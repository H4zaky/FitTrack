package pt.ipp.estg.fittrack.core.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity

object FirebaseSync {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val fs: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun ensureSignedIn(): String {
        val cur = auth.currentUser
        if (cur != null) return cur.uid
        val res = auth.signInAnonymously().await()
        return res.user?.uid ?: ""
    }

    suspend fun uploadSession(uid: String, session: ActivitySessionEntity) {
        if (uid.isBlank()) return
        val doc = fs.collection("users").document(uid)
            .collection("sessions").document(session.id)

        doc.set(
            mapOf(
                "id" to session.id,
                "title" to session.title,
                "type" to session.type,
                "mode" to session.mode,
                "startTs" to session.startTs,
                "endTs" to session.endTs,
                "distanceKm" to session.distanceKm,
                "durationMin" to session.durationMin,
                "startLat" to session.startLat,
                "startLon" to session.startLon,
                "endLat" to session.endLat,
                "endLon" to session.endLon,
                "avgSpeedMps" to session.avgSpeedMps,
                "elevationGainM" to session.elevationGainM
            )
        ).await()

        // Leaderboard mensal global (por utilizador)
        val ym = session.startTs.let { java.text.SimpleDateFormat("yyyy-MM").format(java.util.Date(it)) }
        val lbDoc = fs.collection("leaderboards").document(ym)
            .collection("global").document(uid)

        lbDoc.set(
            mapOf(
                "uid" to uid,
                "distanceKm" to FieldValue.increment(session.distanceKm),
                "sessions" to FieldValue.increment(1)
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }
}
