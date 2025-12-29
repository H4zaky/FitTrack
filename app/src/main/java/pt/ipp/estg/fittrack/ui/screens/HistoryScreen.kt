package pt.ipp.estg.fittrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.ipp.estg.fittrack.data.local.ActivitySessionEntity
import pt.ipp.estg.fittrack.data.local.DbProvider
import pt.ipp.estg.fittrack.ui.components.ActivityDetailsCard
import pt.ipp.estg.fittrack.ui.components.ActivityDetailsUi
import java.util.UUID

private enum class HistoryMode { LIST, MAP }

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val dao = remember { DbProvider.get(context).activityDao() }
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(HistoryMode.LIST) }
    var sessions by remember { mutableStateOf<List<ActivitySessionEntity>>(emptyList()) }
    var selected by remember { mutableStateOf<ActivitySessionEntity?>(null) }

    fun toDetails(s: ActivitySessionEntity) = ActivityDetailsUi(
        title = s.title,
        subtitle = "${s.type} • ${s.durationMin} min",
        distanceKm = s.distanceKm,
        durationMin = s.durationMin
    )

    suspend fun refresh() {
        sessions = dao.getAll()
        if (selected == null) selected = sessions.firstOrNull()
    }

    LaunchedEffect(Unit) {
        if (dao.getAll().isEmpty()) {
            val now = System.currentTimeMillis()
            val seed = listOf(
                ActivitySessionEntity(UUID.randomUUID().toString(), "Caminhada no parque", "Walking", now - 3600_000, now - 3300_000, 2.4, 50),
                ActivitySessionEntity(UUID.randomUUID().toString(), "Corrida leve", "Running", now - 86_400_000, now - 86_000_000, 5.2, 66),
                ActivitySessionEntity(UUID.randomUUID().toString(), "Passeio ao final do dia", "Walking", now - 172_800_000, now - 172_500_000, 3.1, 50)
            )
            seed.forEach { dao.upsert(it) }
        }
        refresh()
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        val tabIndex = if (mode == HistoryMode.LIST) 0 else 1
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

        Spacer(Modifier.height(12.dp))

        when (mode) {
            HistoryMode.LIST -> {
                LazyColumn(Modifier.weight(1f)) {
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
            HistoryMode.MAP -> {
                Box(Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                    Text("Mapa (placeholder) — ligamos Google Maps Compose no próximo passo.")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ActivityDetailsCard(
            details = selected?.let(::toDetails),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                scope.launch {
                    val now = System.currentTimeMillis()
                    dao.upsert(
                        ActivitySessionEntity(
                            id = UUID.randomUUID().toString(),
                            title = "Atividade manual (teste)",
                            type = "Walking",
                            startTs = now - 1800_000,
                            endTs = now,
                            distanceKm = 1.2,
                            durationMin = 30
                        )
                    )
                    refresh()
                }
            }) { Text("Adicionar teste") }
        }
    }
}
