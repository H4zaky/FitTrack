package pt.ipp.estg.fittrack.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationTracker(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun start(
        intervalMs: Long = 2000L,
        minDistanceM: Float = 5f,
        onLocation: (lat: Double, lon: Double, accuracyM: Float?) -> Unit
    ) {
        stop()

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateDistanceMeters(minDistanceM)
            .build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                onLocation(loc.latitude, loc.longitude, loc.accuracy)
            }
        }

        client.requestLocationUpdates(request, callback!!, Looper.getMainLooper())
    }

    fun stop() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
    }
}
