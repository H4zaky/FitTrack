package pt.ipp.estg.fittrack.core.leaderboard

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pt.ipp.estg.fittrack.core.tracking.TrackingPrefs
import pt.ipp.estg.fittrack.data.local.db.DbProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LeaderboardWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = TrackingPrefs.getUserId(applicationContext).orEmpty()
        if (uid.isBlank()) return Result.success()

        val month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val repo = LeaderboardRepository()

        val entries = runCatching { repo.fetchGlobal(month, 200) }.getOrNull() ?: return Result.success()
        if (entries.isEmpty()) return Result.success()

        val myIndex = entries.indexOfFirst { it.uid == uid }
        if (myIndex < 0) return Result.success()

        val myRank = myIndex + 1

        val overtaker = if (myIndex - 1 >= 0) entries[myIndex - 1] else null

        val friendDao = DbProvider.get(applicationContext).friendDao()

        val friendPhones = friendDao.getAllForUser(uid).map { it.phone }.toSet()

        val overtakerIsFriend = overtaker?.phone
            ?.takeIf { it.isNotBlank() }
            ?.let { it in friendPhones }
            ?: false

        val lastMonth = TrackingPrefs.getLastRankMonth(applicationContext)
        val lastRank = TrackingPrefs.getLastRank(applicationContext)

        val isSameMonth = lastMonth == month
        val rankGotWorse = isSameMonth && lastRank > 0 && myRank > lastRank

        if (rankGotWorse && overtakerIsFriend && overtaker != null) {
            val lastOverUid = TrackingPrefs.ensuringOvertakerUid(applicationContext)
            if (lastOverUid != overtaker.uid) {
                LeaderboardNotifier.notifyOvertaken(applicationContext, overtaker.name, myRank)
            }
            TrackingPrefs.setLastRank(applicationContext, month, myRank, overtaker.uid)
        } else {
            TrackingPrefs.setLastRank(applicationContext, month, myRank, overtaker?.uid)
        }

        return Result.success()
    }
}
