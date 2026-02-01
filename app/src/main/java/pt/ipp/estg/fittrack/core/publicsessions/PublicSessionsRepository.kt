package pt.ipp.estg.fittrack.core.publicsessions

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import pt.ipp.estg.fittrack.data.local.entity.TrackPointEntity

object PublicSessionsRepository {
    private val fs = FirebaseFirestore.getInstance()

    suspend fun upsertSession(ownerUid: String, session: ActivitySessionEntity) {
        val data = mapOf(
            "id" to session.id,
            "ownerUid" to ownerUid,
            "title" to session.title,
            "type" to session.type,
            "mode" to session.mode,
            "startTs" to session.startTs,
            "endTs" to session.endTs,
            "distanceKm" to session.distanceKm,
            "durationMin" to session.durationMin,
            "avgSpeedMps" to session.avgSpeedMps,
            "elevationGainM" to session.elevationGainM,
            "steps" to session.steps.toLong(),
            "startLat" to session.startLat,
            "startLon" to session.startLon,
            "endLat" to session.endLat,
            "endLon" to session.endLon,
            "isPublic" to session.isPublic,
            "photoBeforeUri" to session.photoBeforeUri,
            "photoAfterUri" to session.photoAfterUri,
            "weatherTempC" to session.weatherTempC,
            "weatherWindKmh" to session.weatherWindKmh,
            "weatherCode" to session.weatherCode,
            "updatedAt" to System.currentTimeMillis()
        )

        fs.collection("public_sessions")
            .document(session.id)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun upsertPoints(sessionId: String, points: List<TrackPointEntity>) {
        if (points.isEmpty()) return

        val base = fs.collection("public_sessions")
            .document(sessionId)
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

    suspend fun deleteSession(sessionId: String) {
        val sessionRef = fs.collection("public_sessions").document(sessionId)
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

        sessionRef.delete().await()
    }
}
