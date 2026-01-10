package pt.ipp.estg.fittrack.core.recognition

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import pt.ipp.estg.fittrack.core.tracking.TrackingPrefs

class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = ActivityRecognitionResult.extractResult(intent) ?: return
        val best = result.probableActivities.maxByOrNull { it.confidence } ?: return

        val type = when (best.type) {
            DetectedActivity.WALKING -> "Walking"
            DetectedActivity.RUNNING -> "Running"
            DetectedActivity.ON_FOOT -> "On_foot"
            DetectedActivity.IN_VEHICLE -> "In_vehicle"
            DetectedActivity.STILL -> "Still"
            else -> "Unknown"
        }

        TrackingPrefs.setLastDetected(
            context = context,
            type = type,
            confidence = best.confidence,
            ts = System.currentTimeMillis()
        )
    }
}