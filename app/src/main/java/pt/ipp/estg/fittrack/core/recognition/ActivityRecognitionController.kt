package pt.ipp.estg.fittrack.core.recognition

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import pt.ipp.estg.fittrack.core.tracking.TrackingPrefs
import pt.ipp.estg.fittrack.core.tracking.TrackingService
import java.util.UUID

object ActivityRecognitionController {

    private const val INTERVAL_MS = 5_000L
    private const val REQ_CODE = 5001

    private const val CONF_START = 75
    private const val CONF_STOP = 70

    private const val COOLDOWN_START_MS = 60_000L      // evita começar várias vezes
    private const val STOP_AFTER_STILL_MS = 90_000L    // para quando estiver parado tempo suficiente

    private fun hasRecognitionPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 29) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ActivityRecognitionReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (!hasRecognitionPermission(context)) return
        runCatching {
            ActivityRecognition.getClient(context)
                .requestActivityUpdates(INTERVAL_MS, pendingIntent(context))
        }
    }

    @SuppressLint("MissingPermission")
    fun stop(context: Context) {
        runCatching {
            ActivityRecognition.getClient(context)
                .removeActivityUpdates(pendingIntent(context))
        }
    }

    /**
     * ✅ Aqui está o “cérebro” que te faltava:
     * - guarda lastDetected (UI)
     * - decide auto-start
     * - decide auto-stop
     */
    fun handleDetection(context: Context, detected: String, confidence: Int, ts: Long) {
        // 1) guardar para a UI
        TrackingPrefs.setLastDetected(context, detected, confidence, ts)

        // 2) só corre se AUTO estiver ON
        if (!TrackingPrefs.isAutoEnabled(context)) return

        val uid = TrackingPrefs.getUserId(context).orEmpty()
        if (uid.isBlank()) return

        val isTracking = TrackingPrefs.getActiveSessionId(context) != null
        val activeMode = TrackingPrefs.getActiveMode(context) // "AUTO" ou "MANUAL"

        val isMoving = (detected == "walking" || detected == "running")
        val isStill = (detected == "still")

        // 3) atualiza “último movimento” (para stop)
        if (isMoving && confidence >= CONF_STOP) {
            TrackingPrefs.setAutoLastMovingTs(context, ts)
        }

        // 4) AUTO START
        if (!isTracking && isMoving && confidence >= CONF_START) {
            val lastStart = TrackingPrefs.getAutoLastStartTs(context)
            if (ts - lastStart < COOLDOWN_START_MS) return

            TrackingPrefs.setAutoLastStartTs(context, ts)

            val sid = UUID.randomUUID().toString()
            val typeLabel = if (detected == "running") "Running" else "Walking"

            val i = Intent(context, TrackingService::class.java).apply {
                action = TrackingService.ACTION_START
                putExtra(TrackingService.EXTRA_SESSION_ID, sid)
                putExtra(TrackingService.EXTRA_TYPE, typeLabel)
                putExtra(TrackingService.EXTRA_MODE, "AUTO")
                putExtra(TrackingService.EXTRA_TITLE, "$typeLabel (AUTO)")
            }

            ContextCompat.startForegroundService(context, i)
            return
        }

        // 5) AUTO STOP (só pára sessões AUTO, nunca as manuais)
        if (isTracking && activeMode == "AUTO" && isStill && confidence >= CONF_STOP) {
            val lastMoving = TrackingPrefs.getAutoLastMovingTs(context)
            if (lastMoving > 0L && ts - lastMoving >= STOP_AFTER_STILL_MS) {
                val stop = Intent(context, TrackingService::class.java).apply {
                    action = TrackingService.ACTION_STOP
                }
                context.startService(stop)
            }
        }
    }
}
