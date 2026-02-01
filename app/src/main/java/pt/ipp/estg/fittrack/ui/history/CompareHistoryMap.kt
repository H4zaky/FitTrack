package pt.ipp.estg.fittrack.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

@Composable
fun CompareHistoryMap(
    firstPoints: List<TrackPointLike>,
    secondPoints: List<TrackPointLike>,
    modifier: Modifier = Modifier
) {
    val firstLatLngs = remember(firstPoints) { firstPoints.map { LatLng(it.lat, it.lon) } }
    val secondLatLngs = remember(secondPoints) { secondPoints.map { LatLng(it.lat, it.lon) } }
    val allLatLngs = remember(firstLatLngs, secondLatLngs) { firstLatLngs + secondLatLngs }
    val cameraState = rememberCameraPositionState()

    val firstStart = firstLatLngs.firstOrNull()
    val firstEnd = if (firstLatLngs.size > 1) firstLatLngs.last() else null
    val secondStart = secondLatLngs.firstOrNull()
    val secondEnd = if (secondLatLngs.size > 1) secondLatLngs.last() else null

    val firstStartState = rememberUpdatedMarkerState(position = firstStart ?: LatLng(0.0, 0.0))
    val firstEndState = rememberUpdatedMarkerState(position = firstEnd ?: LatLng(0.0, 0.0))
    val secondStartState = rememberUpdatedMarkerState(position = secondStart ?: LatLng(0.0, 0.0))
    val secondEndState = rememberUpdatedMarkerState(position = secondEnd ?: LatLng(0.0, 0.0))

    LaunchedEffect(allLatLngs) {
        when {
            allLatLngs.size >= 2 -> {
                val bounds = LatLngBounds.builder().apply {
                    allLatLngs.forEach { include(it) }
                }.build()
                cameraState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 80))
            }
            allLatLngs.size == 1 -> {
                cameraState.animate(CameraUpdateFactory.newLatLngZoom(allLatLngs.first(), 16f))
            }
        }
    }

    GoogleMap(modifier = modifier, cameraPositionState = cameraState) {
        if (firstStart != null) {
            Marker(
                state = firstStartState,
                title = "Início A",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )
        }
        if (firstEnd != null) {
            Marker(
                state = firstEndState,
                title = "Fim A",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
            )
        }
        if (secondStart != null) {
            Marker(
                state = secondStartState,
                title = "Início B",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
            )
        }
        if (secondEnd != null) {
            Marker(
                state = secondEndState,
                title = "Fim B",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )
        }
        if (firstLatLngs.size >= 2) Polyline(points = firstLatLngs)
        if (secondLatLngs.size >= 2) Polyline(points = secondLatLngs)
    }
}
