package pt.ipp.estg.fittrack.ui.screens.public

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.ipp.estg.fittrack.R
import pt.ipp.estg.fittrack.core.`public`.PublicSessionsRepository
import pt.ipp.estg.fittrack.core.`public`.PublicSessionsRepository.PublicSession
import pt.ipp.estg.fittrack.core.`public`.PublicSessionsRepository.PublicTrackPoint
import pt.ipp.estg.fittrack.ui.components.ActivityDetailsCard
import pt.ipp.estg.fittrack.ui.components.ActivityDetailsUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FriendPublicSessionsScreen(
    friendUid: String,
    friendName: String,
    onOpenSession: (String) -> Unit
) {
    var sessions by remember { mutableStateOf<List<PublicSession>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(friendUid) {
        isLoading = true
        errorMessage = null
        sessions = emptyList()

        val result = withContext(Dispatchers.IO) {
            runCatching { PublicSessionsRepository.listPublicSessionsForOwner(friendUid) }
        }

        sessions = result.getOrDefault(emptyList())
        errorMessage = result.exceptionOrNull()?.localizedMessage
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(
                R.string.public_sessions_title,
                if (friendName.isBlank()) "amigo" else friendName
            ),
            style = MaterialTheme.typography.headlineSmall
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Lista") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Mapa") }
            )
        }

        when {
            isLoading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage ?: stringResource(R.string.public_sessions_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            sessions.isEmpty() -> {
                Text(
                    text = stringResource(R.string.public_sessions_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                if (selectedTab == 0) {
                    PublicSessionsList(
                        sessions = sessions,
                        onOpenSession = onOpenSession
                    )
                } else {
                    PublicSessionsMapList(
                        sessions = sessions,
                        onOpenSession = onOpenSession,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun PublicSessionsList(
    sessions: List<PublicSession>,
    onOpenSession: (String) -> Unit
) {
    val fmt = remember { SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sessions, key = { it.id }) { session ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSession(session.id) }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(session.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${session.type} • ${fmt.format(Date(session.startTs))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Distância: %.2f km".format(session.distanceKm),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Duração: ${session.durationMin} min",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PublicSessionsMapList(
    sessions: List<PublicSession>,
    onOpenSession: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sessionsWithCoords = remember(sessions) {
        sessions.filter { it.startLat != null && it.startLon != null }.take(200)
    }
    val cameraState = rememberCameraPositionState()
    val fmt = remember { SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault()) }

    LaunchedEffect(sessionsWithCoords) {
        val latLngs = sessionsWithCoords.map { LatLng(it.startLat!!, it.startLon!!) }
        when {
            latLngs.size >= 2 -> {
                val bounds = LatLngBounds.builder().apply {
                    latLngs.forEach { include(it) }
                }.build()
                cameraState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 80))
            }
            latLngs.size == 1 -> {
                cameraState.animate(CameraUpdateFactory.newLatLngZoom(latLngs.first(), 14f))
            }
        }
    }

    if (sessionsWithCoords.isEmpty()) {
        Column(modifier = modifier.padding(12.dp)) {
            Text(
                text = "Sem pontos de início para mostrar.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraState
    ) {
        sessionsWithCoords.forEach { session ->
            val position = LatLng(session.startLat!!, session.startLon!!)
            val markerState = rememberMarkerState(position = position)
            Marker(
                state = markerState,
                title = session.title,
                snippet = "${session.type} • ${fmt.format(Date(session.startTs))}",
                onClick = {
                    onOpenSession(session.id)
                    true
                }
            )
        }
    }
}

@Composable
fun FriendPublicSessionDetailScreen(
    sessionId: String,
    friendName: String
) {
    var session by remember { mutableStateOf<PublicSession?>(null) }
    var points by remember { mutableStateOf<List<PublicTrackPoint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionId) {
        isLoading = true
        errorMessage = null

        val result = withContext(Dispatchers.IO) {
            runCatching {
                val s = PublicSessionsRepository.getPublicSession(sessionId)
                val pts = PublicSessionsRepository.getPublicTrackPoints(sessionId)
                s to pts
            }
        }

        if (result.isSuccess) {
            val (s, pts) = result.getOrDefault(null to emptyList())
            session = s
            points = pts
        } else {
            errorMessage = result.exceptionOrNull()?.localizedMessage
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.public_session_detail_title),
            style = MaterialTheme.typography.headlineSmall
        )

        if (friendName.isNotBlank()) {
            Text(friendName, style = MaterialTheme.typography.bodyMedium)
        }

        when {
            isLoading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage ?: stringResource(R.string.public_session_not_found),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            session == null -> {
                Text(
                    text = stringResource(R.string.public_session_not_found),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                val s = session ?: return@Column
                val fmt = remember { SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()) }
                val subtitle = buildString {
                    append(fmt.format(Date(s.startTs)))
                    s.endTs?.let { append(" → ").append(fmt.format(Date(it))) }
                }

                val weatherText = run {
                    val t = s.weatherTempC
                    val w = s.weatherWindKmh
                    if (t == null && w == null) null
                    else buildString {
                        if (t != null) append("%.1f°C".format(t))
                        if (t != null && w != null) append(" • ")
                        if (w != null) append("Vento %.0f km/h".format(w))
                    }
                }

                val details = ActivityDetailsUi(
                    title = s.title,
                    subtitle = "${s.type} • ${s.mode} • $subtitle",
                    distanceKm = s.distanceKm,
                    durationMin = s.durationMin,
                    avgSpeedKmh = if (s.avgSpeedMps > 0) s.avgSpeedMps * 3.6 else null,
                    elevationGainM = if (s.elevationGainM > 0) s.elevationGainM else null,
                    steps = s.steps,
                    start = if (s.startLat != null && s.startLon != null) {
                        "(${s.startLat}, ${s.startLon})"
                    } else {
                        null
                    },
                    end = if (s.endLat != null && s.endLon != null) {
                        "(${s.endLat}, ${s.endLon})"
                    } else {
                        null
                    },
                    weather = weatherText
                )

                ActivityDetailsCard(details = details, modifier = Modifier.fillMaxWidth())

                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.public_session_map_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (points.isEmpty()) {
                            Text(
                                text = stringResource(R.string.public_session_no_points),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            PublicHistoryMap(points = points, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicHistoryMap(
    points: List<PublicTrackPoint>,
    modifier: Modifier = Modifier,
    startTitle: String = "Início",
    endTitle: String = "Fim",
) {
    val latLngs = remember(points) { points.map { LatLng(it.lat, it.lon) } }
    val cameraState = rememberCameraPositionState()

    val startPos = latLngs.firstOrNull()
    val endPos = if (latLngs.size > 1) latLngs.last() else null
    val startState = rememberUpdatedMarkerState(position = startPos ?: LatLng(0.0, 0.0))
    val endState = rememberUpdatedMarkerState(position = endPos ?: LatLng(0.0, 0.0))

    LaunchedEffect(latLngs) {
        when {
            latLngs.size >= 2 -> {
                val bounds = LatLngBounds.builder().apply {
                    latLngs.forEach { include(it) }
                }.build()
                cameraState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 80))
            }
            latLngs.size == 1 -> {
                cameraState.animate(CameraUpdateFactory.newLatLngZoom(latLngs.first(), 16f))
            }
        }
    }

    GoogleMap(modifier = modifier.height(220.dp), cameraPositionState = cameraState) {
        if (startPos != null) Marker(state = startState, title = startTitle)
        if (endPos != null) Marker(state = endState, title = endTitle)
        if (latLngs.size >= 2) Polyline(points = latLngs)
    }
}
