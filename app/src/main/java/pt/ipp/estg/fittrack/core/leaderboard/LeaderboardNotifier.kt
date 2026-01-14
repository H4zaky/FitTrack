package pt.ipp.estg.fittrack.core.leaderboard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pt.ipp.estg.fittrack.R

object LeaderboardNotifier {
    private const val CHANNEL_ID = "leaderboard_channel"
    private const val NOTIF_ID = 2201

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Leaderboards", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun notifyOvertaken(context: Context, friendName: String, yourRank: Int) {
        if (Build.VERSION.SDK_INT >= 33) {
            val ok = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (!ok) return
        }

        ensureChannel(context)

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_overtaken_title))
            .setContentText(context.getString(R.string.notif_overtaken_body, friendName, yourRank))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
    }
}
