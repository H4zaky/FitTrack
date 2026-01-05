package pt.ipp.estg.fittrack.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import pt.ipp.estg.fittrack.data.local.db.DbProvider
import pt.ipp.estg.fittrack.ui.components.ActivityDetailsCard
import pt.ipp.estg.fittrack.ui.components.ActivityDetailsUi

private enum class HistoryMode { LIST, MAP }

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val dao = remember { DbProvider.get(context).activityDao() }

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
        start = s.startLat?.let { lat -> "${"%.5f".format(lat)}, ${"%.5f".format(s.startLon)}" },
        end = s.endLat?.let { lat -> "${"%.5f".format(lat)}, ${"%.5f".format(s.endLon)}" }
    )

    suspend fun refresh() {
        sessions = dao.getAll()
        selected = selected ?: sessions.firstOrNull()
    }

    LaunchedEffect(Unit) { refresh() }

    val tabIndex = if (mode == HistoryMode.LIST) 0 else 1

    Scaffold(
        bottomBar = {
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
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { mode = HistoryMode.LIST }, text = { Text("Lista") })
                Tab(selected = tabIndex == 1, onClick = { mode = HistoryMode.MAP }, text = { Text("Mapa") })
            }

            Spacer(Modifier.height(12.dp))

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
                    Text("Mapa (placeholder) — ligamos Google Maps Compose no próximo passo.")
                }
            }
        }
    }
}
