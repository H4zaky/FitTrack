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

object LeaderboardCacheRepository {

    private val fs = FirebaseFirestore.getInstance()

    fun monthKey(): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
        return fmt.format(java.util.Date())
    }

    private fun dao(context: Context) = DbProvider.get(context).leaderboardSnapshotDao()

    fun observeGlobalTop(
        context: Context,
        month: String = monthKey(),
        limit: Int = 15
    ): Flow<List<LeaderboardSnapshotEntity>> = dao(context).observeTop(month, limit)

    fun observeForUids(
        context: Context,
        month: String = monthKey(),
        uids: List<String>
    ): Flow<List<LeaderboardSnapshotEntity>> = dao(context).observeForUids(month, uids)

    suspend fun refreshGlobalTop(context: Context, limit: Int = 15) = withContext(Dispatchers.IO) {
        val month = monthKey()

        val snap = try {
            fs.collection("leaderboards")
                .document(month)
                .collection("users")
                .orderBy("distanceKm", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get(Source.SERVER)
                .await()
        } catch (_: Exception) {
            fs.collection("leaderboards")
                .document(month)
                .collection("users")
                .orderBy("distanceKm", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get(Source.CACHE)
                .await()
        }

        val now = System.currentTimeMillis()

        val rows = snap.documents.map { d ->
            val uid = d.id
            val name = d.getString("name")?.trim().orEmpty()

            val dist = d.getDouble("distanceKm") ?: 0.0
            val steps = (d.getLong("steps") ?: 0L).toInt()
            val sessions = (d.getLong("sessions") ?: 0L).toInt()
            val updatedAt = d.getLong("updatedAt") ?: now

            LeaderboardSnapshotEntity(
                month = month,
                uid = uid,
                name = name,
                distanceKm = dist,
                steps = steps,
                sessions = sessions,
                updatedAt = updatedAt
            )
        }

        dao(context).upsertAll(rows)
        dao(context).pruneExceptMonths(listOf(month))
    }

    suspend fun refreshForUids(ctx: Context, uids: Set<String>) = withContext(Dispatchers.IO) {
        val month = monthKey()
        val dao = DbProvider.get(ctx).leaderboardSnapshotDao()

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

            val name = snap.getString("name")?.trim().orEmpty()
            val dist = snap.getDouble("distanceKm") ?: 0.0
            val steps = (snap.getLong("steps") ?: 0L).toInt()
            val sessions = (snap.getLong("sessions") ?: 0L).toInt()
            val updatedAt = snap.getLong("updatedAt") ?: System.currentTimeMillis()

            dao.upsertAll(
                listOf(
                    LeaderboardSnapshotEntity(
                        month = month,
                        uid = uid,
                        name = name,
                        distanceKm = dist,
                        steps = steps,
                        sessions = sessions,
                        updatedAt = updatedAt
                    )
                )
            )
        }
    }
}
