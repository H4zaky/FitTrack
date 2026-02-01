package pt.ipp.estg.fittrack.core.rankings

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pt.ipp.estg.fittrack.data.local.db.DbProvider
import pt.ipp.estg.fittrack.data.local.entity.LeaderboardSnapshotEntity
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.WeekFields
import java.util.Locale

object LeaderboardCacheRepository {

    private val fs = FirebaseFirestore.getInstance()

    fun weekKey(
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val weekFields = WeekFields.ISO
        val date = ZonedDateTime.ofInstant(now, zoneId)
        val weekYear = date.get(weekFields.weekBasedYear())
        val week = date.get(weekFields.weekOfWeekBasedYear())
        return "%d-W%02d".format(Locale.US, weekYear, week)
    }

    private fun dao(context: Context, uid: String) =
        DbProvider.get(context, uid).leaderboardSnapshotDao()

    fun observeGlobalTop(
        context: Context,
        uidForDb: String,
        month: String = weekKey(),
        limit: Int = 15
    ): Flow<List<LeaderboardSnapshotEntity>> = dao(context, uidForDb).observeTop(month, limit)

    fun observeForUids(
        context: Context,
        uidForDb: String,
        month: String = weekKey(),
        uids: List<String>
    ): Flow<List<LeaderboardSnapshotEntity>> = dao(context, uidForDb).observeForUids(month, uids)

    suspend fun refreshGlobalTop(context: Context, uidForDb: String, limit: Int = 15) =
        withContext(Dispatchers.IO) {
            val month = weekKey()
            val ref = fs.collection("leaderboards").document(month).collection("users")
                .orderBy("distanceKm", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            val snap = try {
                ref.get(Source.SERVER).await()
            } catch (_: Exception) {
                ref.get(Source.CACHE).await()
            }

            val now = System.currentTimeMillis()
            val rows = snap.documents.map { d ->
                val uid = d.id
                val name = d.getString("name")?.takeIf { it.isNotBlank() } ?: "User"
                val dist = d.getDouble("distanceKm") ?: 0.0
                val steps = (d.getLong("steps") ?: 0L).toInt()
                val sessions = (d.getLong("sessions") ?: 0L).toInt()

                LeaderboardSnapshotEntity(
                    month = month,
                    uid = uid,
                    name = name,
                    distanceKm = dist,
                    steps = steps,
                    sessions = sessions,
                    updatedAt = now
                )
            }

            val dao = dao(context, uidForDb)
            dao.upsertAll(rows)
            dao.pruneExceptMonths(listOf(month))
        }

    suspend fun refreshForUids(context: Context, uidForDb: String, uids: Set<String>) =
        withContext(Dispatchers.IO) {
            val month = weekKey()
            val dao = dao(context, uidForDb)

            val now = System.currentTimeMillis()

            for (uid in uids) {
                val ref = fs.collection("leaderboards")
                    .document(month)
                    .collection("users")
                    .document(uid)

                val snap = try {
                    ref.get(Source.SERVER).await()
                } catch (_: Exception) {
                    ref.get(Source.CACHE).await()
                }

                val dist = snap.getDouble("distanceKm") ?: 0.0
                val steps = (snap.getLong("steps") ?: 0L).toInt()
                val sessions = (snap.getLong("sessions") ?: 0L).toInt()
                val name = snap.getString("name")?.takeIf { it.isNotBlank() } ?: "User"

                dao.upsertAll(
                    listOf(
                        LeaderboardSnapshotEntity(
                            month = month,
                            uid = uid,
                            name = name,
                            distanceKm = dist,
                            steps = steps,
                            sessions = sessions,
                            updatedAt = now
                        )
                    )
                )
            }
        }
}
