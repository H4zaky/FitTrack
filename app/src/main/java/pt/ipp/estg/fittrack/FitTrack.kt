package pt.ipp.estg.fittrack

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import pt.ipp.estg.fittrack.core.rankings.RankingWatchWorker
import java.util.concurrent.TimeUnit

class FitTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val req = PeriodicWorkRequestBuilder<RankingWatchWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ranking_watch",
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }
}
