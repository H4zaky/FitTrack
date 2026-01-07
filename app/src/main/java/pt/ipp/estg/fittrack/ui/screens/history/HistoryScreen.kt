package pt.ipp.estg.fittrack.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import pt.ipp.estg.fittrack.data.local.db.DbProvider
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import pt.ipp.estg.fittrack.ui.components.ActivityDetailsCard
import pt.ipp.estg.fittrack.ui.components.ActivityDetailsUi
import pt.ipp.estg.fittrack.ui.history.HistoryMap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryMode { LIST, MAP }

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }
    val activityDao = remember { db.activityDao() }
    val trackPointDao = remember { db.trackPointDao() }

    var mode by remember { mutableStateOf(HistoryMode.LIST) }
    var sessions by remember { mutableStateOf<List<ActivitySessionEntity>>(emptyList()) }
    var selected by remember { mutableStateOf<ActivitySessionEntity?>(null) }

    suspend fun refresh() {
        sessions = activityDao.getAll()
        selected = selected ?: sessions.firstOrNull()
    }

    LaunchedEffect(Unit) { refresh() }

    val tabIndex = if (mode == HistoryMode.LIST) 0 else 1

    Scaffold(
        bottomBar = {
            if (mode == HistoryMode.LIST) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    ActivityDetailsCard(
                        details = selected?.let(::toDetails),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { mode = HistoryMode.LIST }, text = { Text("Lista") })
                Tab(selected = tabIndex == 1, onClick = { mode = HistoryMode.MAP }, text = { Text("Mapa") })
            }

            Spacer(Modifier.height(8.dp))

            when (mode) {
                HistoryMode.LIST -> {
                    if (sessions.isEmpty()) {
                        Text("Sem atividades ainda. Vai a Atividade e faz Start/Stop.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(sessions) { s ->
                                ListItem(
                                    headlineContent = { Text(s.title) },
                                    supportingContent = { Text("${s.type} • ${"%.1f".format(s.distanceKm)} km") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selected = s }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }

                HistoryMode.MAP -> {
                    HistoryMap(
                        sessionId = selected?.id,
                        trackPointDao = trackPointDao,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun toDetails(s: ActivitySessionEntity): ActivityDetailsUi {
    fun fmt(ts: Long?): String? {
        if (ts == null) return null
        val f = SimpleDateFormat("HH:mm", Locale.getDefault())
        return f.format(Date(ts))
    }

    val avgKmh = if (s.avgSpeedMps > 0) s.avgSpeedMps * 3.6 else null
    val elev = if (s.elevationGainM > 0) s.elevationGainM else null

    return ActivityDetailsUi(
        title = s.title,
        subtitle = "${s.type} • ${s.durationMin} min • ${s.mode}",
        distanceKm = s.distanceKm,
        durationMin = s.durationMin,
        avgSpeedKmh = avgKmh,
        elevationGainM = elev,
        start = fmt(s.startTs)?.let { "Início: $it" },
        end = fmt(s.endTs)?.let { "Fim: $it" }
    )
}
