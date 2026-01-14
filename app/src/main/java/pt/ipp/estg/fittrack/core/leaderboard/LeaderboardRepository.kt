package pt.ipp.estg.fittrack.core.leaderboard

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LeaderboardRepository(
    private val fs: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun fetchGlobal(month: String, limit: Long = 100): List<LeaderboardEntry> {
        val snap = fs.collection("leaderboards").document(month)
            .collection("global")
            .orderBy("distanceKm", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()

        return snap.documents.mapNotNull { d ->
            val uid = d.getString("uid") ?: d.id
            val name = d.getString("name") ?: "User"
            val phone = d.getString("phone") ?: ""
            val distance = d.getDouble("distanceKm") ?: 0.0
            val steps = (d.getLong("steps") ?: 0L)
            val sessions = (d.getLong("sessions") ?: 0L)
            LeaderboardEntry(uid, name, phone, distance, steps, sessions)
        }
    }
}
