package pt.ipp.estg.fittrack.ui.screens.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import pt.ipp.estg.fittrack.core.tracking.TrackingPrefs
import pt.ipp.estg.fittrack.core.tracking.TrackingService
import java.util.UUID

@Composable
fun ActivityScreen(userName: String) {
    val context = LocalContext.current

    var activeSessionId by remember { mutableStateOf(TrackingPrefs.getActiveSessionId(context)) }
    var startTs by remember { mutableLongStateOf(TrackingPrefs.getActiveStartTs(context)) }
    var selectedType by remember { mutableStateOf(TrackingPrefs.getActiveType(context)) }

    var isTracking by remember { mutableStateOf(activeSessionId != null) }

    // “agora” para atualizar o tempo na UI
    var nowTs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isTracking, startTs) {
        while (isTracking && startTs > 0L) {
            nowTs = System.currentTimeMillis()
            delay(1000)
        }
    }

    // lê métricas persistidas pelo service
    val distanceM = TrackingPrefs.getActiveDistanceM(context)
    val speedMps = TrackingPrefs.getActiveSpeedMps(context)
    val speedKmh = speedMps * 3.6f

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* user clica iniciar de novo */ }

    fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        permissionLauncher.launch(perms.toTypedArray())
    }

    fun formatElapsed(sec: Long): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    fun startTracking() {
        if (!hasLocationPermission() || !hasNotificationPermission()) {
            requestPermissions()
            return
        }

        val sid = UUID.randomUUID().toString()
        val st = System.currentTimeMillis()

        TrackingPrefs.setActiveSessionId(context, sid)
        TrackingPrefs.setActiveStartTs(context, st)
        TrackingPrefs.setActiveType(context, selectedType)

        activeSessionId = sid
        startTs = st
        isTracking = true

        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
            putExtra(TrackingService.EXTRA_SESSION_ID, sid)
            putExtra(TrackingService.EXTRA_TYPE, selectedType)
            putExtra(TrackingService.EXTRA_MODE, "MANUAL")
            putExtra(TrackingService.EXTRA_TITLE, "Atividade manual")
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopTracking() {
        context.startService(
            Intent(context, TrackingService::class.java).apply {
                action = TrackingService.ACTION_STOP
            }
        )
        TrackingPrefs.clear(context)

        activeSessionId = null
        startTs = 0L
        isTracking = false
    }

    val elapsedSec = if (isTracking && startTs > 0L) ((nowTs - startTs) / 1000L).coerceAtLeast(0) else 0L
    val types = listOf("Running", "Walking", "Other")

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Olá, $userName", style = MaterialTheme.typography.titleLarge)

        Text("Escolhe a atividade:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { t ->
                FilterChip(
                    selected = selectedType == t,
                    onClick = { if (!isTracking) selectedType = t },
                    enabled = !isTracking,
                    label = { Text(t) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(if (isTracking) "Tracking ativo" else "Parado", style = MaterialTheme.typography.titleMedium)

        if (isTracking) {
            Text("Tempo: ${formatElapsed(elapsedSec)}")
            Text("Distância: ${"%.2f".format(distanceM / 1000.0)} km")
            Text("Velocidade: ${"%.1f".format(speedKmh)} km/h")
        } else {
            Text("Tempo: 00:00")
            Text("Distância: 0.00 km")
            Text("Velocidade: 0.0 km/h")
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { startTracking() },
                enabled = !isTracking,
                modifier = Modifier.weight(1f)
            ) { Text("Start") }

            OutlinedButton(
                onClick = { stopTracking() },
                enabled = isTracking,
                modifier = Modifier.weight(1f)
            ) { Text("Stop") }
        }

        if (!hasLocationPermission() || !hasNotificationPermission()) {
            Text("Permissões necessárias para tracking em background.")
            OutlinedButton(onClick = { requestPermissions() }) { Text("Pedir permissões") }
        }
    }
}
