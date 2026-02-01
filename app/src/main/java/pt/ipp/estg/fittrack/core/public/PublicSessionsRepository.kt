package pt.ipp.estg.fittrack.core.`public`

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object PublicSessionsRepository {
    private val fs = FirebaseFirestore.getInstance()

    data class PublicSession(
        val id: String,
        val ownerUid: String,
        val title: String,
        val type: String,
        val mode: String,
        val startTs: Long,
        val endTs: Long?,
        val distanceKm: Double,
        val durationMin: Int,
        val avgSpeedMps: Double,
        val elevationGainM: Double,
        val steps: Long,
        val startLat: Double?,
        val startLon: Double?,
        val endLat: Double?,
        val endLon: Double?,
        val weatherTempC: Double?,
        val weatherWindKmh: Double?,
        val weatherCode: Int?
    )

    data class PublicTrackPoint(
        val ts: Long,
        val lat: Double,
        val lon: Double,
        val accuracyM: Double?,
        val speedMps: Double?,
        val altitudeM: Double?
    )

    suspend fun listPublicSessionsForOwner(ownerUid: String): List<PublicSession> {
        if (ownerUid.isBlank()) return emptyList()

        val snap = fs.collection("public_sessions")
            .whereEqualTo("ownerUid", ownerUid)
            .whereEqualTo("isPublic", true)
            .get()
            .await()

        return snap.documents
            .mapNotNull { it.toPublicSession() }
            .sortedByDescending { it.startTs }
    }

    suspend fun getPublicSession(sessionId: String): PublicSession? {
        if (sessionId.isBlank()) return null

        val doc = fs.collection("public_sessions")
            .document(sessionId)
            .get()
            .await()

        return doc.toPublicSession()
    }

    suspend fun getPublicTrackPoints(sessionId: String): List<PublicTrackPoint> {
        if (sessionId.isBlank()) return emptyList()

        val snap = fs.collection("public_sessions")
            .document(sessionId)
            .collection("points")
            .orderBy("ts")
            .get()
            .await()

        return snap.documents.mapNotNull { doc ->
            val ts = doc.getLong("ts") ?: return@mapNotNull null
            val lat = doc.getDouble("lat") ?: return@mapNotNull null
            val lon = doc.getDouble("lon") ?: return@mapNotNull null
            PublicTrackPoint(
                ts = ts,
                lat = lat,
                lon = lon,
                accuracyM = doc.getDouble("accuracyM"),
                speedMps = doc.getDouble("speedMps"),
                altitudeM = doc.getDouble("altitudeM")
            )
        }
    }

    private fun DocumentSnapshot.toPublicSession(): PublicSession? {
        if (!exists()) return null

        val ownerUid = getString("ownerUid") ?: return null
        val title = getString("title") ?: "(sem título)"
        val type = getString("type") ?: "Atividade"
        val mode = getString("mode") ?: "MANUAL"
        val startTs = getLong("startTs") ?: return null
        val endTs = getLong("endTs")
        val distanceKm = getDouble("distanceKm") ?: 0.0
        val durationMin = (getLong("durationMin") ?: 0L).toInt()
        val avgSpeedMps = getDouble("avgSpeedMps") ?: 0.0
        val elevationGainM = getDouble("elevationGainM") ?: 0.0
        val steps = getLong("steps") ?: 0L
        val weatherCode = getLong("weatherCode")?.toInt()

        return PublicSession(
            id = getString("id") ?: id,
            ownerUid = ownerUid,
            title = title,
            type = type,
            mode = mode,
            startTs = startTs,
            endTs = endTs,
            distanceKm = distanceKm,
            durationMin = durationMin,
            avgSpeedMps = avgSpeedMps,
            elevationGainM = elevationGainM,
            steps = steps,
            startLat = getDouble("startLat"),
            startLon = getDouble("startLon"),
            endLat = getDouble("endLat"),
            endLon = getDouble("endLon"),
            weatherTempC = getDouble("weatherTempC"),
            weatherWindKmh = getDouble("weatherWindKmh"),
            weatherCode = weatherCode
        )
    }
}
