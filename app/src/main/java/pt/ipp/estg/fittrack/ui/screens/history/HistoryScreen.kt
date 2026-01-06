package pt.ipp.estg.fittrack.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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

private fun formatTime(ts: Long?): String? {
    if (ts == null) return null
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return fmt.format(Date(ts))
}

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }
    val activityDao = remember { db.activityDao() }
    val trackPointDao = remember { db.trackPointDao() }

    var mode by remember { mutableStateOf(HistoryMode.LIST) }
    var sessions by remember { mutableStateOf<List<ActivitySessionEntity>>(emptyList()) }
    var selected by remember { mutableStateOf<ActivitySessionEntity?>(null) }

    val listState = rememberLazyListState()

    fun toDetails(s: ActivitySessionEntity) = ActivityDetailsUi(
        title = s.title,
        subtitle = "${s.type} • ${s.durationMin} min • ${s.mode}",
        distanceKm = s.distanceKm,
        durationMin = s.durationMin,
        avgSpeedKmh = if (s.avgSpeedMps > 0) s.avgSpeedMps * 3.6 else null,
        elevationGainM = if (s.elevationGainM > 0) s.elevationGainM else null,
        start = "Início: ${formatTime(s.startTs)}",
        end = "Fim: ${formatTime(s.endTs) ?: "Em curso"}"
    )

    suspend fun refresh() {
        sessions = activityDao.getAll()
        selected = selected ?: sessions.firstOrNull()
    }

    LaunchedEffect(Unit) { refresh() }

    val tabIndex = if (mode == HistoryMode.LIST) 0 else 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
    ) {
        TabRow(selectedTabIndex = tabIndex) {
            Tab(
                selected = tabIndex == 0,
                onClick = { mode = HistoryMode.LIST },
                text = { Text("Lista") }
            )
            Tab(
                selected = tabIndex == 1,
                onClick = { mode = HistoryMode.MAP },
                text = { Text("Mapa") }
            )
        }

        Spacer(Modifier.height(6.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (mode) {
                HistoryMode.LIST -> {
                    if (sessions.isEmpty()) {
                        Text("Sem atividades ainda. Vai a Atividade e faz Start/Stop.")
                    } else {
                        LazyColumn(
                            state = listState,
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
                    val s = selected
                    if (s == null) {
                        Text("Seleciona uma atividade na Lista.")
                    } else {
                        // títulos com horas no próprio marker
                        val startTitle = "Início ${formatTime(s.startTs) ?: ""}".trim()
                        val endTitle = "Fim ${formatTime(s.endTs) ?: ""}".trim()

                        HistoryMap(
                            sessionId = s.id,
                            trackPointDao = trackPointDao,
                            modifier = Modifier.fillMaxSize(),
                            startTitle = startTitle,
                            endTitle = endTitle
                        )
                    }
                }
            }
        }

        if (mode == HistoryMode.LIST) {
            Spacer(Modifier.height(10.dp))
            ActivityDetailsCard(
                details = selected?.let(::toDetails),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    }
}
