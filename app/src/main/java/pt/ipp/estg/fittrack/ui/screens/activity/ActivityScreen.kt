package pt.ipp.estg.fittrack.ui.screens.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipp.estg.fittrack.core.media.LocalImageStore
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
    CYCLING("Cycling"),
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
    val scope = rememberCoroutineScope()

    // ---- Sensor luz (mantém a tua lógica) ----
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    var lightLow by remember { mutableStateOf(TrackingPrefs.isLightLow(context)) }
    var batteryLow by remember { mutableStateOf(TrackingPrefs.isBatteryLow(context)) }

    DisposableEffect(sensorManager) {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (sensor == null) {
            TrackingPrefs.setLightLow(context, false)
            lightLow = false
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val lux = event.values.firstOrNull() ?: return
                    val nextLow = if (lightLow) lux < 20f else lux < 10f
                    if (nextLow != lightLow) {
                        lightLow = nextLow
                        TrackingPrefs.setLightLow(context, nextLow)
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    // ---- Estado / permissões ----
    var hasLocation by remember { mutableStateOf(false) }
    var hasNotif by remember { mutableStateOf(Build.VERSION.SDK_INT < 33) }
    var hasRecognition by remember { mutableStateOf(Build.VERSION.SDK_INT < 29) }

    var selectedType by rememberSaveable { mutableStateOf(ActivityType.WALKING) }
    var expanded by remember { mutableStateOf(false) }

    var autoEnabled by rememberSaveable { mutableStateOf(TrackingPrefs.isAutoEnabled(context)) }

    var activeSessionId by remember { mutableStateOf<String?>(TrackingPrefs.getActiveSessionId(context)) }
    var startTs by remember { mutableLongStateOf(TrackingPrefs.getActiveStartTs(context)) }

    var isPublic by rememberSaveable { mutableStateOf(TrackingPrefs.getActiveIsPublic(context)) }

    var elapsedSec by remember { mutableLongStateOf(0L) }
    var distanceKm by remember { mutableDoubleStateOf(0.0) }
    var speedKmh by remember { mutableDoubleStateOf(0.0) }
    var steps by remember { mutableStateOf(0) }

    val isTracking = activeSessionId != null

    // ---- Fotos ----
    val pickBefore = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        scope.launch {
            val stored = if (uri == null) {
                null
            } else {
                withContext(Dispatchers.IO) {
                    LocalImageStore.copyToLocal(context, uri, "before")
                }
            }
            TrackingPrefs.setPhotoBefore(context, stored)
        }
    }

    val pickAfter = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        scope.launch {
            val stored = if (uri == null) {
                null
            } else {
                withContext(Dispatchers.IO) {
                    LocalImageStore.copyToLocal(context, uri, "after")
                }
            }
            TrackingPrefs.setPhotoAfter(context, stored)
        }
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

            if (Build.VERSION.SDK_INT >= 29) add(Manifest.permission.ACTIVITY_RECOGNITION)
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
        TrackingPrefs.setActiveIsPublic(context, isPublic)
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
            putExtra(TrackingService.EXTRA_SESSION_ID, sid)
            putExtra(TrackingService.EXTRA_TYPE, selectedType.label)
            putExtra(TrackingService.EXTRA_MODE, if (autoEnabled) "AUTO" else "MANUAL")
            putExtra(TrackingService.EXTRA_TITLE, "${selectedType.label} ($userName)")
            putExtra(TrackingService.EXTRA_IS_PUBLIC, isPublic)
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

    // 2) auto mode on/off
    LaunchedEffect(autoEnabled, hasRecognition) {
        if (autoEnabled && hasRecognition) ActivityRecognitionController.start(context)
        if (!autoEnabled) ActivityRecognitionController.stop(context)
    }

    // 3) loop UI (prefs)
    LaunchedEffect(Unit) {
        while (true) {
            activeSessionId = TrackingPrefs.getActiveSessionId(context)
            startTs = TrackingPrefs.getActiveStartTs(context)
            batteryLow = TrackingPrefs.isBatteryLow(context)

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

    val canStart = !isTracking && hasLocation && hasNotif // se quiseres obrigar passos: && hasRecognition

    val pageShape = remember { RoundedCornerShape(18.dp) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Surface(
                shape = pageShape,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Olá, $userName", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {},
                            label = { Text(if (isTracking) "A gravar" else "Parado") },
                            leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(if (autoEnabled) "Auto ON" else "Auto OFF") },
                            leadingIcon = { Icon(Icons.Outlined.AutoMode, contentDescription = null) }
                        )
                        if (lightLow) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Poupança") },
                                leadingIcon = { Icon(Icons.Outlined.WbSunny, contentDescription = null) }
                            )
                        }
                        if (batteryLow) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Bateria baixa") },
                                leadingIcon = { Icon(Icons.Outlined.BatteryAlert, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        }

        if (!hasLocation || !hasNotif || !hasRecognition) {
            item {
                Surface(
                    shape = pageShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Faltam permissões para tracking",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Localização") },
                                leadingIcon = { Icon(Icons.Outlined.LocationOn, null) }
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text("Notificações") },
                                leadingIcon = { Icon(Icons.Outlined.NotificationsActive, null) }
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text("Movimento") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, null) }
                            )
                        }
                        Button(
                            onClick = { requestPerms() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Permitir agora") }
                    }
                }
            }
        }

        item {
            Surface(
                shape = pageShape,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Resumo", style = MaterialTheme.typography.titleMedium)

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            title = "Tempo",
                            value = formatElapsed(elapsedSec),
                            icon = { Icon(Icons.Outlined.Timer, null) },
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            title = "Distância",
                            value = "%.2f km".format(distanceKm),
                            icon = { Icon(Icons.Outlined.Flag, null) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            title = "Velocidade",
                            value = "%.1f km/h".format(speedKmh),
                            icon = { Icon(Icons.Outlined.Speed, null) },
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            title = "Passos",
                            value = steps.toString(),
                            icon = { Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, null) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = pageShape,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Modo automático", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Deteta Walking/Running em background",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoEnabled,
                            onCheckedChange = { checked ->
                                TrackingPrefs.setAutoEnabled(context, checked)
                                autoEnabled = checked
                                refreshPermsState()
                                if (checked && Build.VERSION.SDK_INT >= 29 && !hasRecognition) {
                                    requestPerms()
                                }
                            }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Última: $lastType") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.DirectionsRun, null) }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("Conf.: $lastConf%") },
                            leadingIcon = { Icon(Icons.Outlined.Speed, null) }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(formatClock(lastTs)) },
                            leadingIcon = { Icon(Icons.Outlined.Timer, null) }
                        )
                    }

                    if (canApplyDetection) {
                        val detectedLabel = detectedType.label
                        FilledTonalButton(
                            onClick = { detectedType.let { selectedType = it } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Usar deteção como tipo manual (${detectedType?.label})")
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = pageShape,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Atividade manual", style = MaterialTheme.typography.titleMedium)

                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedType.label,
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isTracking,
                            label = { Text("Tipo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    enabled = !isTracking).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            ActivityType.entries.forEach { t ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val icon = when (t) {
                                                ActivityType.WALKING -> Icons.AutoMirrored.Outlined.DirectionsWalk
                                                ActivityType.RUNNING -> Icons.AutoMirrored.Outlined.DirectionsRun
                                                ActivityType.OTHER -> Icons.Outlined.Flag
                                            }
                                            Icon(icon, contentDescription = null)
                                            Spacer(Modifier.width(10.dp))
                                            Text(t.label)
                                        }
                                    },
                                    onClick = { selectedType = t; expanded = false }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Público", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (isPublic) "Sessão visível no histórico público."
                                else "Sessão apenas visível para ti.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isPublic,
                            onCheckedChange = { isPublic = it },
                            enabled = !isTracking
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = pageShape,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Fotos (opcional)", style = MaterialTheme.typography.titleMedium)
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                pickBefore.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isTracking
                        ) { Text("Foto antes") }

                        OutlinedButton(
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
            Surface(
                shape = pageShape,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Controlo", style = MaterialTheme.typography.titleMedium)

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (!hasLocation || !hasNotif || !hasRecognition) requestPerms()
                                else startTracking()
                            },
                            enabled = canStart,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) { Text("Start") }

                        Button(
                            onClick = { stopTracking() },
                            enabled = isTracking,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Stop") }
                    }

                    if (lightLow) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.WbSunny, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Modo poupança ativo", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Pouca luz detetada → updates menos frequentes para poupar bateria.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (batteryLow) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.BatteryAlert, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Bateria baixa detetada", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Tracking ajustado para poupança: menos precisão e menos frequência.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun formatElapsed(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
