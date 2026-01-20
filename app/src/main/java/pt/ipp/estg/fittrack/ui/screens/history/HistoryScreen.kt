package pt.ipp.estg.fittrack.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.ipp.estg.fittrack.data.local.dao.ActivityDao
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    activityDao: ActivityDao,
    userId: String,
    onOpenSession: (String) -> Unit,
    onCompareSessions: (String, String) -> Unit
) {
    var items by remember { mutableStateOf<List<ActivitySessionEntity>>(emptyList()) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(userId) {
        items = withContext(Dispatchers.IO) {
            activityDao.getAllForUser(userId)
        }
    }

    val fmt = remember { SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Histórico", style = MaterialTheme.typography.headlineSmall)
        }

        if (items.isEmpty()) {
            item {
                Text("Ainda não tens sessões.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            item {
                val selectedSessions = items.filter { it.id in selectedIds }
                val selectedLabel = when (selectedSessions.size) {
                    0 -> "Seleciona duas sessões para comparar."
                    1 -> "Selecionada: ${selectedSessions.first().title}"
                    else -> "Selecionadas: ${selectedSessions[0].title} vs ${selectedSessions[1].title}"
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Comparar sessões", style = MaterialTheme.typography.titleMedium)
                        Text(selectedLabel, style = MaterialTheme.typography.bodySmall)
                        TextButton(
                            enabled = selectedIds.size == 2,
                            onClick = {
                                val ids = selectedIds.toList()
                                onCompareSessions(ids[0], ids[1])
                            }
                        ) { Text("Abrir comparação") }
                    }
                }
            }
            items(items, key = { it.id }) { s ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(s.title, style = MaterialTheme.typography.titleMedium)
                        Text("${s.type} • ${fmt.format(Date(s.startTs))}", style = MaterialTheme.typography.bodySmall)
                        Text("Distância: %.2f km".format(s.distanceKm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(
                                    checked = selectedIds.contains(s.id),
                                    onCheckedChange = {
                                        selectedIds = selectedIds.toMutableSet().apply {
                                            if (contains(s.id)) {
                                                remove(s.id)
                                            } else {
                                                if (size == 2) {
                                                    remove(first())
                                                }
                                                add(s.id)
                                            }
                                        }
                                    }
                                )
                                Text("Comparar", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onOpenSession(s.id) }) { Text("Detalhes") }
                        }
                    }
                }
            }
        }
    }
}
