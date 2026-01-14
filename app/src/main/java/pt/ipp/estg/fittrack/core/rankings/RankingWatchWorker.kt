package pt.ipp.estg.fittrack.core.rankings

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.ipp.estg.fittrack.core.notifications.NotifUtil
import pt.ipp.estg.fittrack.core.tracking.TrackingPrefs
import pt.ipp.estg.fittrack.data.local.db.DbProvider

class RankingWatchWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork() = withContext(Dispatchers.IO) {
        val ctx = applicationContext
        val myUid = TrackingPrefs.getUserId(ctx).orEmpty()
        if (myUid.isBlank()) return@withContext Result.success()

        val db = DbProvider.get(ctx)
        val friendUids = db.friendDao().getAllOnce()
            .mapNotNull { it.uid?.takeIf { u -> u.isNotBlank() } }
            .toSet()

        if (friendUids.isEmpty()) return@withContext Result.success()

        LeaderboardCacheRepository.refreshForUids(ctx, friendUids + myUid)

        val month = LeaderboardCacheRepository.monthKey()
        val dao = db.leaderboardSnapshotDao()

        val myDist = dao.getOne(month, myUid)?.distanceKm ?: 0.0

        val bestFriend = friendUids
            .mapNotNull { uid -> dao.getOne(month, uid) }
            .maxByOrNull { it.distanceKm }
            ?: return@withContext Result.success()

        val sp = ctx.getSharedPreferences("rank_watch", Context.MODE_PRIVATE)
        val key = "rank_notified_${month}_${bestFriend.uid}"
        val already = sp.getBoolean(key, false)

        if (bestFriend.distanceKm > myDist && !already) {
            sp.edit().putBoolean(key, true).apply()
            NotifUtil.notifyRankOvertaken(
                ctx,
                title = "Ultrapassado no ranking!",
                body = "Um amigo ultrapassou-te em distância este mês."
            )
        } else if (bestFriend.distanceKm <= myDist && already) {
            sp.edit().putBoolean(key, false).apply()
        }

        Result.success()
    }
}
