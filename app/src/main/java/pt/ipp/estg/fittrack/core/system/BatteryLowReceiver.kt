package pt.ipp.estg.fittrack.core.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pt.ipp.estg.fittrack.core.tracking.TrackingPrefs

class BatteryLowReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BATTERY_LOW -> TrackingPrefs.setBatteryLow(context, true)
            Intent.ACTION_BATTERY_OKAY -> TrackingPrefs.setBatteryLow(context, false)
        }
    }
}
