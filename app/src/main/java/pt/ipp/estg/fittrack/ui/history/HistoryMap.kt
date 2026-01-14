package pt.ipp.estg.fittrack.ui.history

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.ipp.estg.fittrack.data.local.dao.TrackPointDao
import pt.ipp.estg.fittrack.data.local.entity.TrackPointEntity

@Composable
fun HistoryMap(
    sessionId: String?,
    trackPointDao: TrackPointDao,
    modifier: Modifier = Modifier,
    startTitle: String = "Início",
    endTitle: String = "Fim",
) {
    var points by remember { mutableStateOf<List<TrackPointEntity>>(emptyList()) }

    LaunchedEffect(sessionId) {
        points = if (sessionId == null) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) { trackPointDao.getBySession(sessionId) }
        }
    }

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

    GoogleMap(modifier = modifier, cameraPositionState = cameraState) {
        if (startPos != null) Marker(state = startState, title = startTitle)
        if (endPos != null) Marker(state = endState, title = endTitle)
        if (latLngs.size >= 2) Polyline(points = latLngs)
    }
}
