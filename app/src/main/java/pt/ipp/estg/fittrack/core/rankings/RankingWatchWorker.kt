package pt.ipp.estg.fittrack.core.rankings

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.ipp.estg.fittrack.core.notifications.NotifUtil
import pt.ipp.estg.fittrack.core.tracking.TrackingPrefs
import pt.ipp.estg.fittrack.data.local.db.DbProvider
import pt.ipp.estg.fittrack.data.local.entity.LeaderboardSnapshotEntity

class RankingWatchWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ctx = applicationContext
        val myUid = TrackingPrefs.getUserId(ctx).orEmpty()
        if (myUid.isBlank()) return@withContext Result.success()

        val db = DbProvider.get(ctx)

        val friendUids = db.friendDao()
            .getAllForUser(myUid)
            .mapNotNull { it.uid?.takeIf { u -> u.isNotBlank() } }
            .toSet()

        if (friendUids.isEmpty()) return@withContext Result.success()

        LeaderboardCacheRepository.refreshForUids(
            context = ctx,
            uidForDb = myUid,
            uids = friendUids + myUid
        )

        val week = LeaderboardCacheRepository.weekKey()
        val dao = db.leaderboardSnapshotDao()

        val snapshots = (friendUids + myUid)
            .mapNotNull { uid -> dao.getOne(week, uid) }

        if (snapshots.isEmpty()) return@withContext Result.success()

        val currentTop = selectTopSnapshot(myUid, snapshots)
            ?: return@withContext Result.success()

        val lastTopUid = TrackingPrefs.getRankWatchLastTopUid(ctx, week)
        val shouldNotify = isOvertakeTransition(
            myUid = myUid,
            lastTopUid = lastTopUid,
            currentTopUid = currentTop.uid
        )

        if (shouldNotify) {
            NotifUtil.notifyRankOvertaken(
                ctx,
                title = "Ultrapassado no ranking!",
                body = "${currentTop.name.ifBlank { "Um amigo" }} ultrapassou-te em distância esta semana."
            )
            TrackingPrefs.setRankWatchLastNotifiedUid(ctx, week, currentTop.uid)
        }

        TrackingPrefs.setRankWatchLastTopUid(ctx, week, currentTop.uid)

        Result.success()
    }

    companion object {
        fun isOvertakeTransition(
            myUid: String,
            lastTopUid: String?,
            currentTopUid: String?
        ): Boolean {
            if (currentTopUid.isNullOrBlank() || currentTopUid == myUid) return false
            if (lastTopUid.isNullOrBlank()) return false
            return lastTopUid == myUid
        }

        fun selectTopSnapshot(
            myUid: String,
            snapshots: List<LeaderboardSnapshotEntity>
        ): LeaderboardSnapshotEntity? {
            return snapshots.maxWithOrNull { left, right ->
                when {
                    left.distanceKm > right.distanceKm -> 1
                    left.distanceKm < right.distanceKm -> -1
                    left.uid == myUid && right.uid != myUid -> 1
                    right.uid == myUid && left.uid != myUid -> -1
                    else -> left.uid.compareTo(right.uid)
                }
            }
        }
    }
}
