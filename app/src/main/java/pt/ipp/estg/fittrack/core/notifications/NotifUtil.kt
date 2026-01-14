package pt.ipp.estg.fittrack.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import pt.ipp.estg.fittrack.R

object NotifUtil {
    private const val CHANNEL_ID = "rank_channel"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Rankings", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun notifyRankOvertaken(context: Context, title: String, body: String) {
        ensureChannel(context)
        val nm = context.getSystemService(NotificationManager::class.java)

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        nm.notify(2201, notif)
    }
}
