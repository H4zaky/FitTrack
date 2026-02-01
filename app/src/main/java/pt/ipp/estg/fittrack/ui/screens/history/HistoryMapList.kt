package pt.ipp.estg.fittrack.ui.screens.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryMapList(
    items: List<ActivitySessionEntity>,
    onOpenSession: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sessionsWithCoords = remember(items) {
        items.filter { it.startLat != null && it.startLon != null }
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
        Column(
            modifier = modifier.padding(12.dp)
        ) {
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
