package pt.ipp.estg.fittrack.core.rankings

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.WeekFields
import java.util.Locale

data class LeaderboardEntry(
    val uid: String,
    val distanceKm: Double,
    val steps: Long,
    val sessions: Long
)

object LeaderboardRepository {
    private val fs by lazy { FirebaseFirestore.getInstance() }

    private fun weekKey(
        ts: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val weekFields = WeekFields.ISO
        val date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), zoneId)
        val weekYear = date.get(weekFields.weekBasedYear())
        val week = date.get(weekFields.weekOfWeekBasedYear())
        return "%d-W%02d".format(Locale.getDefault(), weekYear, week)
    }

    suspend fun getTopGlobal(limit: Long = 20, source: Source = Source.DEFAULT): List<LeaderboardEntry> {
        val week = weekKey()
        val snap = fs.collection("leaderboards").document(week)
            .collection("global")
            .orderBy("distanceKm", Query.Direction.DESCENDING)
            .limit(limit)
            .get(source)
            .await()

        return snap.documents.map { d ->
            LeaderboardEntry(
                uid = d.getString("uid") ?: d.id,
                distanceKm = d.getDouble("distanceKm") ?: 0.0,
                steps = d.getLong("steps") ?: 0L,
                sessions = d.getLong("sessions") ?: 0L
            )
        }
    }

    suspend fun getGlobalForUids(uids: Set<String>, source: Source = Source.DEFAULT): List<LeaderboardEntry> {
        val clean = uids.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (clean.isEmpty()) return emptyList()

        val week = weekKey()
        val col = fs.collection("leaderboards").document(week).collection("global")

        val out = mutableListOf<LeaderboardEntry>()
        for (uid in clean) {
            val d = col.document(uid).get(source).await()
            if (d.exists()) {
                out += LeaderboardEntry(
                    uid = uid,
                    distanceKm = d.getDouble("distanceKm") ?: 0.0,
                    steps = d.getLong("steps") ?: 0L,
                    sessions = d.getLong("sessions") ?: 0L
                )
            }
        }
        return out.sortedByDescending { it.distanceKm }
    }
}
