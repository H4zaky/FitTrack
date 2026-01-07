package pt.ipp.estg.fittrack.core.recognition

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition

object ActivityRecognitionController {

    private const val INTERVAL_MS = 5_000L

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ActivityRecognitionReceiver::class.java).apply {
            action = "pt.ipp.estg.fittrack.ACTIVITY_RECOGNITION"
        }
        return PendingIntent.getBroadcast(
            context,
            5001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun start(context: Context) {
        if (Build.VERSION.SDK_INT >= 29) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        try {
            ActivityRecognition.getClient(context)
                .requestActivityUpdates(INTERVAL_MS, pendingIntent(context))
        } catch (_: SecurityException) {
        }
    }

    fun stop(context: Context) {
        try {
            ActivityRecognition.getClient(context)
                .removeActivityUpdates(pendingIntent(context))
        } catch (_: SecurityException) {
        }
    }
}
