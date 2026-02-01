package pt.ipp.estg.fittrack.core.sync

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.fittrack.core.publicsessions.PublicSessionsRepository
import pt.ipp.estg.fittrack.data.local.db.DbProvider
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import pt.ipp.estg.fittrack.data.local.entity.TrackPointEntity
import java.time.Instant
import java.time.ZoneId
import pt.ipp.estg.fittrack.core.rankings.LeaderboardCacheRepository
import com.google.firebase.firestore.FieldPath

object FirebaseSync {
    private val fs = FirebaseFirestore.getInstance()

    // ---------------- UPLOAD ----------------

    // users/{uid}/sessions/{sid}
    suspend fun uploadSession(uid: String, s: ActivitySessionEntity) {
        val ref = fs.collection("users").document(uid)
            .collection("sessions").document(s.id)

        val data = mapOf(
            "id" to s.id,
            "title" to s.title,
            "type" to s.type,
            "mode" to s.mode,
            "startTs" to s.startTs,
            "endTs" to s.endTs,
            "distanceKm" to s.distanceKm,
            "durationMin" to s.durationMin,
            "avgSpeedMps" to s.avgSpeedMps,
            "elevationGainM" to s.elevationGainM,
            "steps" to s.steps.toLong(),
            "startLat" to s.startLat,
            "startLon" to s.startLon,
            "endLat" to s.endLat,
            "endLon" to s.endLon,
            "isPublic" to s.isPublic,
            "photoBeforeUri" to s.photoBeforeUri,
            "photoAfterUri" to s.photoAfterUri,
            "weatherTempC" to s.weatherTempC,
            "weatherWindKmh" to s.weatherWindKmh,
            "weatherCode" to s.weatherCode,
            "updatedAt" to System.currentTimeMillis()
        )

        ref.set(data, SetOptions.merge()).await()

        if (s.isPublic) {
            PublicSessionsRepository.upsertSession(uid, s)
        } else {
            runCatching { PublicSessionsRepository.deleteSession(s.id) }
        }
    }
    suspend fun deleteSession(uid: String, sessionId: String) {
        val sessionRef = fs.collection("users")
            .document(uid)
            .collection("sessions")
            .document(sessionId)

        // 1) apagar subcoleção points em batches
        val pointsCol = sessionRef.collection("points")

        while (true) {
            val snap = pointsCol
                .orderBy(FieldPath.documentId())
                .limit(450)
                .get()
                .await()

            if (snap.isEmpty) break

            val batch = fs.batch()
            for (d in snap.documents) {
                batch.delete(d.reference)
            }
            batch.commit().await()
        }

        // 2) apagar o doc da sessão
        sessionRef.delete().await()

        runCatching { PublicSessionsRepository.deleteSession(sessionId) }
    }

    suspend fun uploadTrackPoints(
        uid: String,
        sessionId: String,
        points: List<TrackPointEntity>,
        isPublic: Boolean
    ) {
        if (points.isEmpty()) return

        val base = fs.collection("users").document(uid)
            .collection("sessions").document(sessionId)
            .collection("points")

        val chunks = points.chunked(450)
        for (chunk in chunks) {
            val batch = fs.batch()
            for (p in chunk) {
                val doc = base.document(p.ts.toString())
                batch.set(
                    doc,
                    mapOf(
                        "ts" to p.ts,
                        "lat" to p.lat,
                        "lon" to p.lon,
                        "accuracyM" to p.accuracyM,
                        "speedMps" to ((p.speedMps ?: 0f).toDouble()),
                        "altitudeM" to p.altitudeM
                    ),
                    SetOptions.merge()
                )
            }
            batch.commit().await()
        }

        if (isPublic) {
            PublicSessionsRepository.upsertPoints(sessionId, points)
        } else {
            runCatching { PublicSessionsRepository.deleteSession(sessionId) }
        }
    }

    // leaderboards/{YYYY-Www}/users/{uid}
    suspend fun countSessionIntoLeaderboard(uid: String, session: ActivitySessionEntity, userName: String?) {
        val week = LeaderboardCacheRepository.weekKey(
            now = Instant.ofEpochMilli(session.startTs),
            zoneId = ZoneId.systemDefault()
        )
        val ref = fs.collection("leaderboards").document(week)
            .collection("users").document(uid)

        fs.runTransaction { tx ->
            val snap = tx.get(ref)
            val prevDist = snap.getDouble("distanceKm") ?: 0.0
            val prevSteps = snap.getLong("steps") ?: 0L
            val prevSessions = snap.getLong("sessions") ?: 0L

            val next = mapOf(
                "name" to (userName ?: (snap.getString("name") ?: "User")),
                "distanceKm" to (prevDist + session.distanceKm),
                "steps" to (prevSteps + session.steps.toLong()),
                "sessions" to (prevSessions + 1L),
                "updatedAt" to System.currentTimeMillis()
            )
            tx.set(ref, next, SetOptions.merge())
        }.await()
    }

    // ---------------- DOWNLOAD (SYNC DOWN) ----------------

    suspend fun syncDownAndReplace(context: Context, uid: String) {
        val db = DbProvider.get(context)
        val activityDao = db.activityDao()
        val pointDao = db.trackPointDao()

        // 1) buscar remoto (server → cache)
        val sessionsCol = fs.collection("users").document(uid).collection("sessions")
        val sessionsSnap = try {
            sessionsCol.get(Source.SERVER).await()
        } catch (_: Exception) {
            sessionsCol.get(Source.CACHE).await()
        }

        val remoteSessions = sessionsSnap.documents.mapNotNull { d ->
            val sid = d.getString("id") ?: d.id
            val title = d.getString("title") ?: "Atividade"
            val type = d.getString("type") ?: "Walking"
            val mode = d.getString("mode") ?: "MANUAL"

            val startTs = d.getLong("startTs") ?: return@mapNotNull null
            val endTs = d.getLong("endTs")

            val dist = d.getDouble("distanceKm") ?: 0.0
            val dur = (d.getLong("durationMin") ?: 0L).toInt()

            val startLat = d.getDouble("startLat")
            val startLon = d.getDouble("startLon")
            val endLat = d.getDouble("endLat")
            val endLon = d.getDouble("endLon")

            val isPublic = d.getBoolean("isPublic") ?: false

            val avg = d.getDouble("avgSpeedMps") ?: 0.0
            val elev = d.getDouble("elevationGainM") ?: 0.0
            val steps = (d.getLong("steps") ?: 0L).toInt()

            val pb = d.getString("photoBeforeUri")
            val pa = d.getString("photoAfterUri")

            val wt = d.getDouble("weatherTempC")
            val ww = d.getDouble("weatherWindKmh")
            val wc = d.getLong("weatherCode")?.toInt()

            ActivitySessionEntity(
                id = sid,
                userId = uid,
                title = title,
                type = type,
                startTs = startTs,
                endTs = endTs,
                distanceKm = dist,
                durationMin = dur,
                mode = mode,
                isPublic = isPublic,
                startLat = startLat,
                startLon = startLon,
                endLat = endLat,
                endLon = endLon,
                avgSpeedMps = avg,
                elevationGainM = elev,
                steps = steps,
                photoBeforeUri = pb,
                photoAfterUri = pa,
                weatherTempC = wt,
                weatherWindKmh = ww,
                weatherCode = wc
            )
        }

        // 2) limpar local desse user
        val local = activityDao.getAllForUser(uid)
        for (s in local) {
            pointDao.deleteBySession(s.id)
            activityDao.deleteByIdForUser(uid, s.id)
        }

        // 3) inserir sessões
        for (s in remoteSessions) {
            activityDao.upsert(s)
        }

        for (s in remoteSessions) {
            val pointsCol = sessionsCol.document(s.id).collection("points")

            val pointsSnap = try {
                pointsCol.orderBy("ts").get(Source.SERVER).await()
            } catch (_: Exception) {
                pointsCol.orderBy("ts").get(Source.CACHE).await()
            }

            val points = pointsSnap.documents.mapNotNull { p ->
                val ts = p.getLong("ts") ?: return@mapNotNull null
                val lat = p.getDouble("lat") ?: return@mapNotNull null
                val lon = p.getDouble("lon") ?: return@mapNotNull null

                val acc = p.getDouble("accuracyM")?.toFloat()
                val spd = p.getDouble("speedMps")?.toFloat()
                val alt = p.getDouble("altitudeM")

                TrackPointEntity(
                    id = 0,
                    sessionId = s.id,
                    ts = ts,
                    lat = lat,
                    lon = lon,
                    accuracyM = acc,
                    speedMps = spd,
                    altitudeM = alt
                )
            }

            pointDao.deleteBySession(s.id)
            if (points.isNotEmpty()) pointDao.insertAll(points)
        }
    }

}
