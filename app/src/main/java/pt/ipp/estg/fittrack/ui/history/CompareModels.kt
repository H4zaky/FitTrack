package pt.ipp.estg.fittrack.ui.history

import pt.ipp.estg.fittrack.core.`public`.PublicSessionsRepository.PublicSession
import pt.ipp.estg.fittrack.core.`public`.PublicSessionsRepository.PublicTrackPoint
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import pt.ipp.estg.fittrack.data.local.entity.TrackPointEntity
import pt.ipp.estg.fittrack.ui.components.ActivityDetailsUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TrackPointLike(
    val lat: Double,
    val lon: Double
)

data class SessionLike(
    val id: String,
    val title: String,
    val type: String,
    val mode: String,
    val startTs: Long,
    val endTs: Long?,
    val distanceKm: Double,
    val durationMin: Int,
    val avgSpeedMps: Double,
    val elevationGainM: Double,
    val steps: Long,
    val startLat: Double?,
    val startLon: Double?,
    val endLat: Double?,
    val endLon: Double?,
    val weatherTempC: Double?,
    val weatherWindKmh: Double?
)

fun TrackPointEntity.toTrackPointLike(): TrackPointLike = TrackPointLike(
    lat = lat,
    lon = lon
)

fun PublicTrackPoint.toTrackPointLike(): TrackPointLike = TrackPointLike(
    lat = lat,
    lon = lon
)

fun ActivitySessionEntity.toSessionLike(): SessionLike = SessionLike(
    id = id,
    title = title,
    type = type,
    mode = mode,
    startTs = startTs,
    endTs = endTs,
    distanceKm = distanceKm,
    durationMin = durationMin,
    avgSpeedMps = avgSpeedMps,
    elevationGainM = elevationGainM,
    steps = steps,
    startLat = startLat,
    startLon = startLon,
    endLat = endLat,
    endLon = endLon,
    weatherTempC = weatherTempC,
    weatherWindKmh = weatherWindKmh
)

fun PublicSession.toSessionLike(): SessionLike = SessionLike(
    id = id,
    title = title,
    type = type,
    mode = mode,
    startTs = startTs,
    endTs = endTs,
    distanceKm = distanceKm,
    durationMin = durationMin,
    avgSpeedMps = avgSpeedMps,
    elevationGainM = elevationGainM,
    steps = steps,
    startLat = startLat,
    startLon = startLon,
    endLat = endLat,
    endLon = endLon,
    weatherTempC = weatherTempC,
    weatherWindKmh = weatherWindKmh
)

fun SessionLike.toDetails(label: String): ActivityDetailsUi {
    val fmt = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    val subtitle = buildString {
        append("Sessão ").append(label).append(" • ")
        append(fmt.format(Date(startTs)))
        endTs?.let { append(" → ").append(fmt.format(Date(it))) }
    }

    val weatherText = run {
        val t = weatherTempC
        val w = weatherWindKmh
        if (t == null && w == null) null
        else buildString {
            if (t != null) append("%.1f°C".format(t))
            if (t != null && w != null) append(" • ")
            if (w != null) append("Vento %.0f km/h".format(w))
        }
    }

    return ActivityDetailsUi(
        title = title,
        subtitle = "$type • $mode • $subtitle",
        distanceKm = distanceKm,
        durationMin = durationMin,
        avgSpeedKmh = if (avgSpeedMps > 0) avgSpeedMps * 3.6 else null,
        elevationGainM = if (elevationGainM > 0) elevationGainM else null,
        steps = steps,
        start = startLat?.let { "($startLat, $startLon)" },
        end = endLat?.let { "($endLat, $endLon)" },
        weather = weatherText
    )
}
