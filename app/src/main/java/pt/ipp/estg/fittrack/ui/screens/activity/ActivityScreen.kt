package pt.ipp.estg.fittrack.ui.screens.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import pt.ipp.estg.fittrack.core.recognition.ActivityRecognitionController
import pt.ipp.estg.fittrack.core.tracking.TrackingPrefs
import pt.ipp.estg.fittrack.core.tracking.TrackingService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

private enum class ActivityType(val label: String) {
    WALKING("Walking"),
    RUNNING("Running"),
    OTHER("Other")
}

private fun detectedToActivityType(detected: String?): ActivityType? {
    val d = detected?.trim()?.lowercase() ?: return null
    return when (d) {
        "walking", "on_foot", "on foot" -> ActivityType.WALKING
        "running" -> ActivityType.RUNNING
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(userName: String) {
    val context = LocalContext.current

    var hasLocation by remember { mutableStateOf(false) }
    var hasNotif by remember { mutableStateOf(Build.VERSION.SDK_INT < 33) }
    var hasRecognition by remember { mutableStateOf(Build.VERSION.SDK_INT < 29) }

    var selectedType by rememberSaveable { mutableStateOf(ActivityType.WALKING) }
    var expanded by remember { mutableStateOf(false) }

    var autoEnabled by rememberSaveable { mutableStateOf(TrackingPrefs.isAutoEnabled(context)) }

    var activeSessionId by remember { mutableStateOf<String?>(TrackingPrefs.getActiveSessionId(context)) }
    var startTs by remember { mutableLongStateOf(TrackingPrefs.getActiveStartTs(context)) }

    var elapsedSec by remember { mutableLongStateOf(0L) }
    var distanceKm by remember { mutableDoubleStateOf(0.0) }
    var speedKmh by remember { mutableDoubleStateOf(0.0) }
    var steps by remember { mutableStateOf(0) }

    val isTracking = activeSessionId != null

    // ✅ Foto pickers (galeria) — sem "Request" (isso causava o teu erro)
    val pickBefore = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        TrackingPrefs.setPhotoBefore(context, uri?.toString())
    }

    val pickAfter = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        TrackingPrefs.setPhotoAfter(context, uri?.toString())
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocation = fine || coarse

        hasNotif = if (Build.VERSION.SDK_INT >= 33) {
            result[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true

        hasRecognition = if (Build.VERSION.SDK_INT >= 29) {
            result[Manifest.permission.ACTIVITY_RECOGNITION] == true
        } else true

        if (autoEnabled && hasRecognition) ActivityRecognitionController.start(context)
    }

    fun requestPerms(forAuto: Boolean = false) {
        val perms = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            if (forAuto && Build.VERSION.SDK_INT >= 29) add(Manifest.permission.ACTIVITY_RECOGNITION)
        }.toTypedArray()
        permLauncher.launch(perms)
    }

    fun refreshPermsState() {
        hasLocation =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        hasNotif = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        hasRecognition = if (Build.VERSION.SDK_INT >= 29) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun startTracking() {
        val sid = UUID.randomUUID().toString()
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
            putExtra(TrackingService.EXTRA_SESSION_ID, sid)
            putExtra(TrackingService.EXTRA_TYPE, selectedType.label)
            putExtra(TrackingService.EXTRA_MODE, if (autoEnabled) "AUTO" else "MANUAL")
            putExtra(TrackingService.EXTRA_TITLE, "${selectedType.label} ($userName)")
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopTracking() {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP
        }
        context.startService(intent)
    }

    // 1) check inicial
    LaunchedEffect(Unit) { refreshPermsState() }

    // 2) se autoEnabled e há permissão, liga (e desliga ao desativar)
    LaunchedEffect(autoEnabled, hasRecognition) {
        if (autoEnabled && hasRecognition) ActivityRecognitionController.start(context)
        if (!autoEnabled) ActivityRecognitionController.stop(context)
    }

    // 3) loop UI (prefs)
    LaunchedEffect(Unit) {
        while (true) {
            activeSessionId = TrackingPrefs.getActiveSessionId(context)
            startTs = TrackingPrefs.getActiveStartTs(context)

            val now = System.currentTimeMillis()
            if (activeSessionId != null && startTs > 0L) {
                elapsedSec = max(0L, (now - startTs) / 1000L)
                distanceKm = TrackingPrefs.getActiveDistanceM(context) / 1000.0
                speedKmh = TrackingPrefs.getActiveSpeedMps(context).toDouble() * 3.6
                steps = TrackingPrefs.getActiveSteps(context)
            } else {
                elapsedSec = 0L
                distanceKm = 0.0
                speedKmh = 0.0
                steps = 0
            }
            delay(1000)
        }
    }

    fun formatClock(ts: Long): String {
        if (ts <= 0L) return "—"
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return fmt.format(Date(ts))
    }

    val lastType = TrackingPrefs.getLastDetectedType(context)
    val lastConf = TrackingPrefs.getLastDetectedConfidence(context)
    val lastTs = TrackingPrefs.getLastDetectedTs(context)

    val detectedType = detectedToActivityType(lastType)
    val canApplyDetection = autoEnabled && !isTracking && detectedType != null && lastConf >= 70

    val canStart = !isTracking && hasLocation && hasNotif

    // ✅ Scroll: troca Column por LazyColumn
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Olá, $userName", style = MaterialTheme.typography.titleMedium)
        }

        if (!hasLocation || !hasNotif) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Faltam permissões para tracking (Localização e/ou Notificações).")
                        Button(onClick = { requestPerms(forAuto = autoEnabled) }) { Text("Permitir agora") }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                androidx.compose.foundation.layout.Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        androidx.compose.foundation.layout.Column {
                            Text("Modo automático", style = MaterialTheme.typography.titleMedium)
                            Text("Deteta Walking/Running em background", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = autoEnabled,
                            onCheckedChange = { checked ->
                                TrackingPrefs.setAutoEnabled(context, checked)
                                autoEnabled = checked
                                refreshPermsState()
                                if (checked && Build.VERSION.SDK_INT >= 29 && !hasRecognition) {
                                    requestPerms(forAuto = true)
                                }
                            }
                        )
                    }
                    Text("Última deteção: $lastType")
                    Text("Confiança: $lastConf%")
                    Text("Hora: ${formatClock(lastTs)}")
                }
            }
        }

        if (canApplyDetection) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Sugestão: $lastType ($lastConf%)", style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = { selectedType = detectedType!! },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Usar deteção como tipo manual") }
                    }
                }
            }
        }

        item {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedType.label,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isTracking,
                    label = { Text("Tipo de atividade (manual)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ActivityType.entries.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.label) },
                            onClick = { selectedType = t; expanded = false }
                        )
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                androidx.compose.foundation.layout.Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Fotos (opcional)", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                pickBefore.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isTracking
                        ) { Text("Foto antes") }

                        Button(
                            onClick = {
                                pickAfter.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isTracking
                        ) { Text("Foto depois") }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                androidx.compose.foundation.layout.Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Estado: ${if (isTracking) "A gravar" else "Parado"}")
                    Text("Tempo: ${formatElapsed(elapsedSec)}")
                    Text("Distância: ${"%.2f".format(distanceKm)} km")
                    Text("Velocidade: ${"%.1f".format(speedKmh)} km/h")
                    Text("Passos: $steps")
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (!hasLocation || !hasNotif) requestPerms(forAuto = autoEnabled) else startTracking()
                    },
                    enabled = canStart
                ) { Text("Start") }

                Button(
                    onClick = { stopTracking() },
                    enabled = isTracking
                ) { Text("Stop") }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                "Fecha a app e volta — as métricas continuam (Foreground Service + prefs).",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatElapsed(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
