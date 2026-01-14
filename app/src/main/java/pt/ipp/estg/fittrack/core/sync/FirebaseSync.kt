package pt.ipp.estg.fittrack.core.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import pt.ipp.estg.fittrack.data.local.entity.TrackPointEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import pt.ipp.estg.fittrack.data.local.db.DbProvider


object FirebaseSync {
    private val fs = FirebaseFirestore.getInstance()
    private const val SYNC_SESSIONS_LIMIT = 50
    private const val SYNC_POINTS_SESSIONS_LIMIT = 10

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
            "photoBeforeUri" to s.photoBeforeUri,
            "photoAfterUri" to s.photoAfterUri,
            "weatherTempC" to s.weatherTempC,
            "weatherWindKmh" to s.weatherWindKmh,
            "weatherCode" to s.weatherCode,
            "updatedAt" to System.currentTimeMillis()
        )

        ref.set(data, SetOptions.merge()).await()
    }

    suspend fun uploadTrackPoints(uid: String, sessionId: String, points: List<TrackPointEntity>) {
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
    }

    suspend fun countSessionIntoLeaderboard(uid: String, session: ActivitySessionEntity, userName: String?) {
        val month = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(session.startTs))
        val ref = fs.collection("leaderboards").document(month)
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

    suspend fun syncDownAndReconcile(context: Context, uid: String) {
        val db = DbProvider.get(context.applicationContext)
        val activityDao = db.activityDao()
        val pointDao = db.trackPointDao()

        // 1) download sessions (recentes)
        val remoteSessions = downloadSessions(uid, limit = SYNC_SESSIONS_LIMIT)
        val remoteIds = remoteSessions.map { it.id }.toSet()

        // 2) upsert no Room
        for (s in remoteSessions) {
            activityDao.upsert(s.copy(userId = uid))
        }

        val forPoints = remoteSessions
            .sortedByDescending { it.startTs }
            .take(SYNC_POINTS_SESSIONS_LIMIT)

        for (s in forPoints) {
            val points = downloadTrackPoints(uid, s.id)
            pointDao.deleteBySession(s.id)
            pointDao.insertAll(points)
        }

        val local = activityDao.getAllForUser(uid)
        val pendingUpload = local.filter { it.endTs != null && it.id !in remoteIds }

        for (s in pendingUpload) {
            val points = runCatching { pointDao.getBySession(s.id) }.getOrNull().orEmpty()
            runCatching {
                uploadSession(uid, s)
                uploadTrackPoints(uid, s.id, points)
            }
        }
    }

    private suspend fun downloadSessions(uid: String, limit: Int): List<ActivitySessionEntity> {
        val snap = fs.collection("users").document(uid)
            .collection("sessions")
            .orderBy("startTs", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        return snap.documents.mapNotNull { d ->
            val id = d.getString("id") ?: d.id
            val title = d.getString("title") ?: "Atividade"
            val type = d.getString("type") ?: "Walking"
            val mode = d.getString("mode") ?: "MANUAL"

            val startTs = d.getLong("startTs") ?: return@mapNotNull null
            val endTs = d.getLong("endTs")

            val durationMin = (d.getLong("durationMin") ?: 0L).toInt()
            val steps = (d.getLong("steps") ?: 0L).toInt()
            val weatherCode = d.getLong("weatherCode")?.toInt()

            ActivitySessionEntity(
                id = id,
                userId = uid,
                title = title,
                type = type,
                startTs = startTs,
                endTs = endTs,
                distanceKm = d.getDouble("distanceKm") ?: 0.0,
                durationMin = durationMin,
                mode = mode,
                startLat = d.getDouble("startLat"),
                startLon = d.getDouble("startLon"),
                endLat = d.getDouble("endLat"),
                endLon = d.getDouble("endLon"),
                avgSpeedMps = d.getDouble("avgSpeedMps") ?: 0.0,
                elevationGainM = d.getDouble("elevationGainM") ?: 0.0,
                steps = steps,
                photoBeforeUri = d.getString("photoBeforeUri"),
                photoAfterUri = d.getString("photoAfterUri"),
                weatherTempC = d.getDouble("weatherTempC"),
                weatherWindKmh = d.getDouble("weatherWindKmh"),
                weatherCode = weatherCode
            )
        }
    }

    private suspend fun downloadTrackPoints(uid: String, sessionId: String): List<TrackPointEntity> {
        val snap = fs.collection("users").document(uid)
            .collection("sessions").document(sessionId)
            .collection("points")
            .orderBy("ts", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get()
            .await()

        return snap.documents.mapNotNull { d ->
            val ts = d.getLong("ts") ?: d.id.toLongOrNull() ?: return@mapNotNull null
            val lat = d.getDouble("lat") ?: return@mapNotNull null
            val lon = d.getDouble("lon") ?: return@mapNotNull null

            TrackPointEntity(
                sessionId = sessionId,
                ts = ts,
                lat = lat,
                lon = lon,
                accuracyM = d.getDouble("accuracyM")?.toFloat(),
                speedMps = d.getDouble("speedMps")?.toFloat(),
                altitudeM = d.getDouble("altitudeM")
            )
        }
    }
}
