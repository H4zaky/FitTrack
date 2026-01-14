package pt.ipp.estg.fittrack.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipp.estg.fittrack.core.sync.FirebaseSync
import pt.ipp.estg.fittrack.data.local.dao.ActivityDao
import pt.ipp.estg.fittrack.data.local.dao.TrackPointDao
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import pt.ipp.estg.fittrack.ui.components.ActivityDetailsCard
import pt.ipp.estg.fittrack.ui.components.ActivityDetailsUi
import pt.ipp.estg.fittrack.ui.history.HistoryMap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionDetailScreen(
    sessionId: String,
    userId: String,
    activityDao: ActivityDao,
    trackPointDao: TrackPointDao,
    onDeleted: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<ActivitySessionEntity?>(null) }

    LaunchedEffect(userId, sessionId) {
        session = withContext(Dispatchers.IO) {
            activityDao.getByIdForUser(userId = userId, id = sessionId)
        }
    }

    val s = session

    Column(
        Modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Detalhe", style = MaterialTheme.typography.headlineSmall)

        if (s == null) {
            Text("Sessão não encontrada (ou não pertence a esta conta).")
            return@Column
        }

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
            start = s.startLat?.let { "(${s.startLat}, ${s.startLon})" },
            end = s.endLat?.let { "(${s.endLat}, ${s.endLon})" },
            weather = weatherText
        )

        ActivityDetailsCard(details = details, modifier = Modifier.fillMaxWidth())

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mapa", style = MaterialTheme.typography.titleMedium)
                HistoryMap(
                    sessionId = sessionId,
                    trackPointDao = trackPointDao,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        trackPointDao.deleteBySession(sessionId)

                        activityDao.deleteByIdForUser(userId = userId, id = sessionId)
                        runCatching { FirebaseSync.deleteSession(userId, sessionId) }
                    }
                    onDeleted()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Apagar") }
    }
}
