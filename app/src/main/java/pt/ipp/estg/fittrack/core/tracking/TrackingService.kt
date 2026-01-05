package pt.ipp.estg.fittrack.core.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pt.ipp.estg.fittrack.MainActivity
import pt.ipp.estg.fittrack.R
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import pt.ipp.estg.fittrack.data.local.db.DbProvider
import pt.ipp.estg.fittrack.data.local.entity.TrackPointEntity
import kotlin.math.max

class TrackingService : Service() {

    companion object {
        const val ACTION_START = "pt.ipp.estg.fittrack.TRACKING_START"
        const val ACTION_STOP = "pt.ipp.estg.fittrack.TRACKING_STOP"

        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_TYPE = "type"
        const val EXTRA_MODE = "mode"
        const val EXTRA_TITLE = "title"

        private const val CHANNEL_ID = "tracking_channel"
        private const val NOTIF_ID = 1001
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var callback: LocationCallback? = null

    private val db by lazy { DbProvider.get(this) }
    private val activityDao by lazy { db.activityDao() }
    private val pointDao by lazy { db.trackPointDao() }

    private var sessionId: String? = null
    private var startTs: Long = 0L

    private var lastLoc: Location? = null
    private var lastPointTs: Long = 0L

    private var distanceM: Double = 0.0
    private var elevationGainM: Double = 0.0
    private var startSet: Boolean = false
    private var currentSpeedMps: Float = 0f

    private var tickerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onDestroy() {
        stopTicker()
        stopLocationUpdates()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ✅ Se o sistema reiniciar o service com intent null, tentamos retomar
        if (intent == null || intent.action == null) {
            resumeIfNeeded()
            return START_STICKY
        }

        when (intent.action) {
            ACTION_START -> startTracking(intent)
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun resumeIfNeeded() {
        val sid = TrackingPrefs.getActiveSessionId(this) ?: run {
            stopSelf()
            return
        }
        val st = TrackingPrefs.getActiveStartTs(this)
        val type = TrackingPrefs.getActiveType(this)

        sessionId = sid
        startTs = if (st > 0L) st else System.currentTimeMillis().also { TrackingPrefs.setActiveStartTs(this, it) }

        // restaurar métricas mínimas (não perdes UI ao reabrir)
        distanceM = TrackingPrefs.getActiveDistanceM(this)
        currentSpeedMps = TrackingPrefs.getActiveSpeedMps(this)

        startForeground(NOTIF_ID, buildNotification(buildContent()))
        startTicker()

        if (!hasLocationPermission()) {
            stopTracking()
            return
        }
        startLocationUpdates()
    }

    private fun startTracking(intent: Intent) {
        val sid = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Atividade"
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "Walking"
        val mode = intent.getStringExtra(EXTRA_MODE) ?: "MANUAL"

        sessionId = sid
        startTs = System.currentTimeMillis()

        // ✅ persistir estado
        TrackingPrefs.setActiveSessionId(this, sid)
        TrackingPrefs.setActiveStartTs(this, startTs)
        TrackingPrefs.setActiveType(this, type)
        TrackingPrefs.setActiveDistanceM(this, 0.0)
        TrackingPrefs.setActiveSpeedMps(this, 0f)

        lastLoc = null
        lastPointTs = 0L
        distanceM = 0.0
        elevationGainM = 0.0
        startSet = false
        currentSpeedMps = 0f

        serviceScope.launch {
            val existing = activityDao.getById(sid)
            if (existing == null) {
                activityDao.upsert(
                    ActivitySessionEntity(
                        id = sid,
                        title = title,
                        type = type,
                        mode = mode,
                        startTs = startTs,
                        endTs = null,
                        distanceKm = 0.0,
                        durationMin = 0,
                        startLat = null,
                        startLon = null,
                        endLat = null,
                        endLon = null,
                        avgSpeedMps = 0.0,
                        elevationGainM = 0.0
                    )
                )
            }
        }

        startForeground(NOTIF_ID, buildNotification(buildContent()))
        startTicker()

        if (!hasLocationPermission()) {
            stopTracking()
            return
        }
        startLocationUpdates()
    }

    private fun stopTracking() {
        val sid = sessionId ?: run {
            stopSelf()
            return
        }

        stopTicker()
        stopLocationUpdates()

        val endTs = System.currentTimeMillis()
        val durationMin = ((endTs - startTs) / 60000L).toInt().coerceAtLeast(0)
        val distanceKm = distanceM / 1000.0
        val avgSpeedMps = if (endTs > startTs) distanceM / max(1.0, (endTs - startTs) / 1000.0) else 0.0

        val endLat = lastLoc?.latitude
        val endLon = lastLoc?.longitude

        serviceScope.launch {
            activityDao.finalizeSession(
                id = sid,
                endTs = endTs,
                distanceKm = distanceKm,
                durationMin = durationMin,
                endLat = endLat,
                endLon = endLon,
                avgSpeedMps = avgSpeedMps,
                elevationGainM = elevationGainM
            )
        }

        TrackingPrefs.clear(this)
        sessionId = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateDistanceMeters(3f)
            .build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                onNewLocation(loc)
            }
        }

        fused.requestLocationUpdates(request, callback!!, mainLooper)
    }

    private fun stopLocationUpdates() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
    }

    private fun onNewLocation(loc: Location) {
        val sid = sessionId ?: return
        val now = System.currentTimeMillis()

        val prev = lastLoc
        if (prev != null) {
            distanceM += prev.distanceTo(loc).toDouble()

            // elevação acumulada só se ambos tiverem altitude
            if (prev.hasAltitude() && loc.hasAltitude()) {
                val dAlt = loc.altitude - prev.altitude
                if (!dAlt.isNaN() && dAlt > 0) elevationGainM += dAlt
            }

            // velocidade: preferir a do GPS, senão calcular
            currentSpeedMps = if (loc.hasSpeed()) {
                loc.speed
            } else {
                val dt = (now - lastPointTs).coerceAtLeast(1L) / 1000f
                val d = prev.distanceTo(loc)
                (d / dt)
            }
        } else {
            currentSpeedMps = if (loc.hasSpeed()) loc.speed else 0f
        }

        lastLoc = loc
        lastPointTs = now

        if (!startSet) {
            startSet = true
            serviceScope.launch {
                activityDao.setStartLocation(sid, loc.latitude, loc.longitude)
            }
        }

        // ✅ persistir “ao vivo” para UI não perder ao fechar
        TrackingPrefs.setActiveDistanceM(this, distanceM)
        TrackingPrefs.setActiveSpeedMps(this, currentSpeedMps)

        serviceScope.launch {
            pointDao.insert(
                TrackPointEntity(
                    sessionId = sid,
                    ts = now,
                    lat = loc.latitude,
                    lon = loc.longitude,
                    accuracyM = if (loc.hasAccuracy()) loc.accuracy else null,
                    speedMps = currentSpeedMps,
                    altitudeM = if (loc.hasAltitude()) loc.altitude else null
                )
            )
        }
        // notificação é atualizada pelo ticker
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive && sessionId != null) {
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIF_ID, buildNotification(buildContent()))
                delay(1000)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun buildContent(): String {
        val km = distanceM / 1000.0
        val elapsedSec = ((System.currentTimeMillis() - startTs) / 1000L).coerceAtLeast(0)
        val speedKmh = currentSpeedMps * 3.6f
        return "A gravar… ${"%.2f".format(km)} km • ${formatElapsed(elapsedSec)} • ${"%.1f".format(speedKmh)} km/h"
    }

    private fun formatElapsed(sec: Long): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Tracking", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotification(content: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("FitTrack")
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .addAction(0, "Parar", stopPending)
            .build()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}
