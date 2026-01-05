package pt.ipp.estg.fittrack.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import pt.ipp.estg.fittrack.data.local.dao.TrackPointDao
import pt.ipp.estg.fittrack.data.local.entity.TrackPointEntity

@Composable
fun HistoryMap(
    sessionId: String?,
    trackPointDao: TrackPointDao,
    modifier: Modifier = Modifier
) {
    var points by remember { mutableStateOf<List<TrackPointEntity>>(emptyList()) }

    LaunchedEffect(sessionId) {
        points = if (sessionId == null) emptyList() else trackPointDao.getBySession(sessionId)
    }

    val latLngs = remember(points) { points.map { LatLng(it.lat, it.lon) } }
    val cameraState = rememberCameraPositionState()

    // Estados dos markers (atualizam a posição sem recriar estado "à bruta")
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

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraState
    ) {
        if (startPos != null) {
            Marker(
                state = startState,
                title = "Início"
            )
        }

        if (endPos != null) {
            Marker(
                state = endState,
                title = "Fim"
            )
        }

        if (latLngs.size >= 2) {
            Polyline(points = latLngs)
        }
    }
}
