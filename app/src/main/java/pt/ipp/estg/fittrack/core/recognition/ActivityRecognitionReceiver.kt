package pt.ipp.estg.fittrack.core.recognition

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityRecognitionResult.hasResult(intent)) return
        val result = ActivityRecognitionResult.extractResult(intent) ?: return

        val best = result.probableActivities.maxByOrNull { it.confidence } ?: return

        val detected = when (best.type) {
            DetectedActivity.WALKING, DetectedActivity.ON_FOOT -> "walking"
            DetectedActivity.RUNNING -> "running"
            DetectedActivity.STILL -> "still"
            DetectedActivity.IN_VEHICLE -> "in_vehicle"
            else -> "unknown"
        }

        ActivityRecognitionController.handleDetection(
            context = context.applicationContext,
            detected = detected,
            confidence = best.confidence,
            ts = System.currentTimeMillis()
        )
    }
}
