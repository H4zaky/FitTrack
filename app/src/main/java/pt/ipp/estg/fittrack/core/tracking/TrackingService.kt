package pt.ipp.estg.fittrack.core.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.*
import pt.ipp.estg.fittrack.MainActivity
import pt.ipp.estg.fittrack.R
import pt.ipp.estg.fittrack.core.steps.StepCounterManager
import pt.ipp.estg.fittrack.core.sync.FirebaseSync
import pt.ipp.estg.fittrack.core.weather.WeatherRepository
import pt.ipp.estg.fittrack.data.local.db.DbProvider
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import pt.ipp.estg.fittrack.data.local.entity.TrackPointEntity
import kotlin.math.max

class TrackingService : Service() {

    companion object {
        const val ACTION_START = "pt.ipp.estg.fittrack.TRACKING_START"
        const val ACTION_STOP  = "pt.ipp.estg.fittrack.TRACKING_STOP"

        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_TYPE = "type"
        const val EXTRA_MODE = "mode"
        const val EXTRA_TITLE = "title"

        private const val CHANNEL_ID = "tracking_channel"
        private const val NOTIF_ID = 1001
        private var batteryReceiver: BroadcastReceiver? = null
        private var lastPowerSave: Boolean? = null
    }

    private var lastWeatherTryTs: Long = 0L
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var callback: LocationCallback? = null

    private val db by lazy { DbProvider.get(this) }
    private val activityDao by lazy { db.activityDao() }
    private val pointDao by lazy { db.trackPointDao() }

    private var sessionId: String? = null
    private var startTs: Long = 0L
    private var activeType: String = "Walking"
    private var activeUserId: String = ""

    private var lastLoc: Location? = null
    private var lastPointTs: Long = 0L
    private var distanceM: Double = 0.0
    private var elevationGainM: Double = 0.0
    private var startSet = false
    private var currentSpeedMps: Float = 0f

    private var lastSavedLoc: Location? = null
    private var lastSavedTs: Long = 0L
    private val pendingPoints = mutableListOf<TrackPointEntity>()
    private var flushJob: Job? = null
    private var tickerJob: Job? = null

    private var stepManager: StepCounterManager? = null
    private var currentSteps: Int = 0

    private var weatherSaved = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startBatteryMonitor()

        stepManager = StepCounterManager(this) { delta ->
            currentSteps = delta
            TrackingPrefs.setActiveSteps(this, delta)
        }
    }

    override fun onDestroy() {
        val sid = sessionId
        val uid = activeUserId.ifBlank { TrackingPrefs.getUserId(this).orEmpty() }

        // Fecha a sessão ativa caso o serviço seja destruído inesperadamente
        runBlocking(Dispatchers.IO) {
            runCatching { flushPendingPoints() }

            if (!sid.isNullOrBlank()) {
                val endTs = System.currentTimeMillis()
                val durationMin = ((endTs - startTs) / 60000L).toInt().coerceAtLeast(0)
                val distanceKm = distanceM / 1000.0
                val avgSpeedMps =
                    if (endTs > startTs) distanceM / max(1.0, (endTs - startTs) / 1000.0) else 0.0

                runCatching {
                    if (uid.isNotBlank()) {
                        activityDao.finalizeSessionForUser(
                            userId = uid,
                            id = sid,
                            endTs = endTs,
                            distanceKm = distanceKm,
                            durationMin = durationMin,
                            endLat = lastLoc?.latitude,
                            endLon = lastLoc?.longitude,
                            avgSpeedMps = avgSpeedMps,
                            elevationGainM = elevationGainM,
                            steps = currentSteps,
                            photoBeforeUri = TrackingPrefs.getPhotoBefore(this@TrackingService),
                            photoAfterUri = TrackingPrefs.getPhotoAfter(this@TrackingService)
                        )
                    } else {
                        activityDao.finalizeSession(
                            id = sid,
                            endTs = endTs,
                            distanceKm = distanceKm,
                            durationMin = durationMin,
                            endLat = lastLoc?.latitude,
                            endLon = lastLoc?.longitude,
                            avgSpeedMps = avgSpeedMps,
                            elevationGainM = elevationGainM,
                            steps = currentSteps,
                            photoBeforeUri = TrackingPrefs.getPhotoBefore(this@TrackingService),
                            photoAfterUri = TrackingPrefs.getPhotoAfter(this@TrackingService)
                        )
                    }
                }

                runCatching { TrackingPrefs.clearActiveSession(this@TrackingService) }
            }
        }

        stopBatteryMonitor()
        stopTicker()
        stopFlushLoop()
        stopLocationUpdates()
        stepManager?.stop()
        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == null) {
            resumeIfNeeded()
            return START_STICKY
        }
        when (intent.action) {
            ACTION_START -> startTracking(intent)
            ACTION_STOP  -> stopTracking()
        }
        return START_STICKY
    }

    private fun resumeIfNeeded() {
        val m = TrackingPrefs.getActiveMode(this)
        TrackingPrefs.setActiveMode(this, m)
        val sid = TrackingPrefs.getActiveSessionId(this) ?: run { stopSelf(); return }

        sessionId = sid
        activeUserId = TrackingPrefs.getUserId(this).orEmpty()

        startTs = TrackingPrefs.getActiveStartTs(this).takeIf { it > 0L }
            ?: System.currentTimeMillis().also { TrackingPrefs.setActiveStartTs(this, it) }

        activeType = TrackingPrefs.getActiveType(this)
        distanceM = TrackingPrefs.getActiveDistanceM(this)
        currentSpeedMps = TrackingPrefs.getActiveSpeedMps(this)
        currentSteps = TrackingPrefs.getActiveSteps(this)

        startForeground(NOTIF_ID, buildNotification(buildContent()))
        startTicker()
        startFlushLoop()
        startLocationUpdates()

        // ✅ passos só se houver permissão
        if (hasActivityPerm()) stepManager?.start()
        else TrackingPrefs.setActiveSteps(this, 0)
    }

    private fun startTracking(intent: Intent) {
        val sid = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Atividade"
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "Walking"
        val mode = intent.getStringExtra(EXTRA_MODE) ?: "MANUAL"

        TrackingPrefs.setActiveMode(this, mode)


        val uid = TrackingPrefs.getUserId(this).orEmpty()
        if (uid.isBlank()) { stopSelf(); return }
        activeUserId = uid

        sessionId = sid
        activeType = type
        startTs = System.currentTimeMillis()

        TrackingPrefs.setActiveSessionId(this, sid)
        TrackingPrefs.setActiveStartTs(this, startTs)
        TrackingPrefs.setActiveType(this, type)
        TrackingPrefs.setActiveDistanceM(this, 0.0)
        TrackingPrefs.setActiveSpeedMps(this, 0f)
        TrackingPrefs.setActiveSteps(this, 0)

        lastLoc = null
        lastPointTs = 0L
        distanceM = 0.0
        elevationGainM = 0.0
        startSet = false
        currentSpeedMps = 0f
        lastSavedLoc = null
        lastSavedTs = 0L
        pendingPoints.clear()
        weatherSaved = false

        currentSteps = 0
        stepManager?.stop()
        if (hasActivityPerm()) stepManager?.start()
        else TrackingPrefs.setActiveSteps(this, 0)

        val beforeUri = TrackingPrefs.getPhotoBefore(this)
        val afterUri = TrackingPrefs.getPhotoAfter(this)

        serviceScope.launch {
            activityDao.upsert(
                ActivitySessionEntity(
                    id = sid,
                    userId = uid,
                    title = title,
                    type = type,
                    startTs = startTs,
                    endTs = null,
                    distanceKm = 0.0,
                    durationMin = 0,
                    mode = mode,
                    startLat = null,
                    startLon = null,
                    endLat = null,
                    endLon = null,
                    avgSpeedMps = 0.0,
                    elevationGainM = 0.0,
                    steps = 0,
                    photoBeforeUri = beforeUri,
                    photoAfterUri = afterUri,
                    weatherTempC = null,
                    weatherWindKmh = null,
                    weatherCode = null
                )
            )
        }

        startForeground(NOTIF_ID, buildNotification(buildContent()))
        startTicker()
        startFlushLoop()
        startLocationUpdates()
    }

    private fun stopTracking() {
        val sid = sessionId ?: run { stopSelf(); return }
        val uid = activeUserId.ifBlank { TrackingPrefs.getUserId(this).orEmpty() }

        stopTicker()
        stopLocationUpdates()
        stepManager?.stop()

        serviceScope.launch { flushPendingPoints() }

        val endTs = System.currentTimeMillis()
        val durationMin = ((endTs - startTs) / 60000L).toInt().coerceAtLeast(0)
        val distanceKm = distanceM / 1000.0
        val avgSpeedMps =
            if (endTs > startTs) distanceM / max(1.0, (endTs - startTs) / 1000.0) else 0.0

        val endLat = lastLoc?.latitude
        val endLon = lastLoc?.longitude

        val beforeUri = TrackingPrefs.getPhotoBefore(this)
        val afterUri = TrackingPrefs.getPhotoAfter(this)

        serviceScope.launch {
            if (uid.isNotBlank()) {
                activityDao.finalizeSessionForUser(
                    userId = uid,
                    id = sid,
                    endTs = endTs,
                    distanceKm = distanceKm,
                    durationMin = durationMin,
                    endLat = endLat,
                    endLon = endLon,
                    avgSpeedMps = avgSpeedMps,
                    elevationGainM = elevationGainM,
                    steps = currentSteps,
                    photoBeforeUri = beforeUri,
                    photoAfterUri = afterUri
                )
            } else {
                activityDao.finalizeSession(
                    id = sid,
                    endTs = endTs,
                    distanceKm = distanceKm,
                    durationMin = durationMin,
                    endLat = endLat,
                    endLon = endLon,
                    avgSpeedMps = avgSpeedMps,
                    elevationGainM = elevationGainM,
                    steps = currentSteps,
                    photoBeforeUri = beforeUri,
                    photoAfterUri = afterUri
                )
            }
        }

        serviceScope.launch {
            if (uid.isBlank()) return@launch

            runCatching { flushPendingPoints() }

            val session = activityDao.getByIdForUser(uid, sid) ?: return@launch
            val points = runCatching { pointDao.getBySession(sid) }.getOrNull().orEmpty()

            runCatching {
                FirebaseSync.uploadSession(uid, session)
                FirebaseSync.uploadTrackPoints(uid, sid, points)

                FirebaseSync.countSessionIntoLeaderboard(
                    uid = uid,
                    session = session,
                    userName = TrackingPrefs.getUserName(this@TrackingService) ?: "User"
                )
            }
        }


        stopFlushLoop()
        TrackingPrefs.clearActiveSession(this)
        sessionId = null
        activeUserId = ""

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            stopTracking()
            return
        }

        val batteryLow = TrackingPrefs.isBatteryLow(this)
        val lightLow = TrackingPrefs.isLightLow(this)
        val powerSave = batteryLow || lightLow

        val isRunning = activeType.equals("Running", ignoreCase = true)

        val priority = if (powerSave) {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        } else {
            if (isRunning) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val interval = if (powerSave) 6000L else 2000L
        val minDist = if (powerSave) {
            if (isRunning) 6f else 10f
        } else {
            if (isRunning) 3f else 5f
        }

        val request = LocationRequest.Builder(priority, interval)
            .setMinUpdateDistanceMeters(minDist)
            .setMaxUpdateDelayMillis(10_000L)
            .build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                onNewLocation(loc)
            }
        }

        try {
            fused.requestLocationUpdates(request, callback!!, mainLooper)
        } catch (_: SecurityException) {
            stopTracking()
        }
    }

    private fun stopLocationUpdates() {
        val cb = callback ?: return
        callback = null
        try { fused.removeLocationUpdates(cb) } catch (_: SecurityException) {}
    }

    private fun onNewLocation(loc: Location) {
        val sid = sessionId ?: return
        val uid = activeUserId.ifBlank { TrackingPrefs.getUserId(this).orEmpty() }
        val now = System.currentTimeMillis()

        val prev = lastLoc
        if (prev != null) {
            distanceM += prev.distanceTo(loc).toDouble()

            if (prev.hasAltitude() && loc.hasAltitude()) {
                val dAlt = loc.altitude - prev.altitude
                if (!dAlt.isNaN() && dAlt > 0) elevationGainM += dAlt
            }

            currentSpeedMps = if (loc.hasSpeed()) loc.speed else run {
                val dt = (now - lastPointTs).coerceAtLeast(1L) / 1000f
                val d = prev.distanceTo(loc)
                d / dt
            }
        } else {
            currentSpeedMps = if (loc.hasSpeed()) loc.speed else 0f
        }

        lastLoc = loc
        lastPointTs = now

        if (!startSet) {
            startSet = true
            serviceScope.launch {
                if (uid.isNotBlank()) activityDao.setStartLocationForUser(uid, sid, loc.latitude, loc.longitude)
                else activityDao.setStartLocation(sid, loc.latitude, loc.longitude)
            }

            if (!weatherSaved) {
                val nowTs = System.currentTimeMillis()
                if (nowTs - lastWeatherTryTs > 60_000L) {
                    lastWeatherTryTs = nowTs

                    serviceScope.launch {
                        val w = runCatching {
                            WeatherRepository.fetchCurrent(loc.latitude, loc.longitude)
                        }.getOrNull() ?: return@launch

                        val ok = runCatching {
                            if (uid.isNotBlank()) {
                                activityDao.updateWeatherForUser(uid, sid, w.tempC, w.windKmh, w.code)
                            } else {
                                activityDao.updateWeather(sid, w.tempC, w.windKmh, w.code)
                            }
                        }.isSuccess

                        if (ok) weatherSaved = true
                    }
                }
            }

        }

        TrackingPrefs.setActiveDistanceM(this, distanceM)
        TrackingPrefs.setActiveSpeedMps(this, currentSpeedMps)

        if (shouldSavePoint(now, loc)) {
            lastSavedLoc = loc
            lastSavedTs = now

            pendingPoints += TrackPointEntity(
                sessionId = sid,
                ts = now,
                lat = loc.latitude,
                lon = loc.longitude,
                accuracyM = if (loc.hasAccuracy()) loc.accuracy else null,
                speedMps = currentSpeedMps,
                altitudeM = if (loc.hasAltitude()) loc.altitude else null
            )

            if (pendingPoints.size >= 10) {
                serviceScope.launch { flushPendingPoints() }
            }
        }
    }

    private fun shouldSavePoint(now: Long, loc: Location): Boolean {
        val last = lastSavedLoc ?: return true
        val dt = now - lastSavedTs
        val dist = last.distanceTo(loc)

        val isRunning = activeType.equals("Running", ignoreCase = true)
        val minTime = if (isRunning) 3000L else 5000L
        val minDist = if (isRunning) 6f else 8f

        return (dt >= minTime) || (dist >= minDist)
    }

    private fun startFlushLoop() {
        flushJob?.cancel()
        flushJob = serviceScope.launch {
            while (isActive && sessionId != null) {
                delay(15_000L)
                flushPendingPoints()
            }
        }
    }

    private fun stopFlushLoop() {
        flushJob?.cancel()
        flushJob = null
    }

    private suspend fun flushPendingPoints() {
        if (pendingPoints.isEmpty()) return
        val copy = pendingPoints.toList()
        pendingPoints.clear()
        pointDao.insertAll(copy)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive && sessionId != null) {
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIF_ID, buildNotification(buildContent()))

                val powerSave = TrackingPrefs.isBatteryLow(this@TrackingService) ||
                        TrackingPrefs.isLightLow(this@TrackingService)

                delay(if (powerSave) 8000L else 4000L)
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
        return "A gravar… ${"%.2f".format(km)} km • ${formatElapsed(elapsedSec)} • ${"%.1f".format(speedKmh)} km/h • $currentSteps passos"
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
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    // ✅ esta é a função que faltava (e estava a dar erro / private local)
    private fun hasActivityPerm(): Boolean {
        if (Build.VERSION.SDK_INT < 29) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startBatteryMonitor() {
        if (batteryReceiver != null) return

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, i: Intent) {
                val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1

                val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val lowBattery = (pct in 0..15) && !charging
                TrackingPrefs.setBatteryLow(this@TrackingService, lowBattery)

                val powerSaveNow = lowBattery || TrackingPrefs.isLightLow(this@TrackingService)

                if (sessionId != null && lastPowerSave != null && lastPowerSave != powerSaveNow) {
                    stopLocationUpdates()
                    startLocationUpdates()
                }
                lastPowerSave = powerSaveNow
            }
        }

        val sticky = ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        sticky?.let { batteryReceiver?.onReceive(this, it) }
    }

    private fun stopBatteryMonitor() {
        batteryReceiver?.let { runCatching { unregisterReceiver(it) } }
        batteryReceiver = null
    }
}
