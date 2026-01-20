package pt.ipp.estg.fittrack.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.ipp.estg.fittrack.data.local.dao.ActivityDao
import pt.ipp.estg.fittrack.data.local.dao.TrackPointDao
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import pt.ipp.estg.fittrack.ui.components.ActivityDetailsCard
import pt.ipp.estg.fittrack.ui.components.ActivityDetailsUi
import pt.ipp.estg.fittrack.ui.history.CompareHistoryMap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CompareSessionsScreen(
    userId: String,
    firstSessionId: String,
    secondSessionId: String,
    activityDao: ActivityDao,
    trackPointDao: TrackPointDao
) {
    var first by remember { mutableStateOf<ActivitySessionEntity?>(null) }
    var second by remember { mutableStateOf<ActivitySessionEntity?>(null) }

    LaunchedEffect(userId, firstSessionId, secondSessionId) {
        val loaded = withContext(Dispatchers.IO) {
            val a = activityDao.getByIdForUser(userId, firstSessionId)
            val b = activityDao.getByIdForUser(userId, secondSessionId)
            a to b
        }
        first = loaded.first
        second = loaded.second
    }

    Column(
        modifier = Modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Comparar sessões", style = MaterialTheme.typography.headlineSmall)

        if (first == null || second == null) {
            Text("Não foi possível carregar as duas sessões.")
            return@Column
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mapa combinado", style = MaterialTheme.typography.titleMedium)
                CompareHistoryMap(
                    firstSessionId = firstSessionId,
                    secondSessionId = secondSessionId,
                    trackPointDao = trackPointDao,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActivityDetailsCard(
                details = first?.toDetails("A"),
                modifier = Modifier.weight(1f)
            )
            ActivityDetailsCard(
                details = second?.toDetails("B"),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun ActivitySessionEntity.toDetails(label: String): ActivityDetailsUi {
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
        start = startLat?.let { "($startLat, $startLon)" },
        end = endLat?.let { "($endLat, $endLon)" },
        weather = weatherText
    )
}
