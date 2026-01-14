package pt.ipp.estg.fittrack.ui.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.ipp.estg.fittrack.core.rankings.LeaderboardCacheRepository
import pt.ipp.estg.fittrack.core.tracking.TrackingPrefs
import pt.ipp.estg.fittrack.data.local.db.DbProvider

@Composable
fun SocialScreen() {
    val context = LocalContext.current
    val myUid = TrackingPrefs.getUserId(context).orEmpty()
    val month = remember { LeaderboardCacheRepository.monthKey() }

    val db = remember(myUid) { DbProvider.get(context, myUid) }
    val friendDao = remember { db.friendDao() }

    var friendUids by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(myUid) {
        if (myUid.isBlank()) return@LaunchedEffect
        friendUids = withContext(Dispatchers.IO) {
            friendDao.getAllForUser(myUid)
                .mapNotNull { it.uid?.takeIf { u -> u.isNotBlank() } }
        }
    }

    val global by LeaderboardCacheRepository
        .observeGlobalTop(context, uidForDb = myUid, month = month, limit = 15)
        .collectAsState(initial = emptyList())

    val friendsTop by LeaderboardCacheRepository
        .observeForUids(
            context,
            uidForDb = myUid,
            month = month,
            uids = (friendUids + myUid).distinct().filter { it.isNotBlank() }
        )
        .collectAsState(initial = emptyList())

    LaunchedEffect(friendUids, myUid) {
        if (myUid.isBlank()) return@LaunchedEffect
        LeaderboardCacheRepository.refreshGlobalTop(context, uidForDb = myUid, limit = 15)
        LeaderboardCacheRepository.refreshForUids(
            context,
            uidForDb = myUid,
            uids = (friendUids + myUid).filter { it.isNotBlank() }.toSet()
        )
    }

    Column(
        Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Rankings", style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Global (Top 15)", style = MaterialTheme.typography.titleMedium)
                if (global.isEmpty()) {
                    Text("Sem cache ainda. Abre com internet 1x para preencher.")
                } else {
                    global.forEachIndexed { idx, e ->
                        val label = e.name.ifBlank { e.uid.take(8) }
                        Text("${idx + 1}. $label • ${"%.2f".format(e.distanceKm)} km • ${e.steps} passos")
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Amigos + Eu", style = MaterialTheme.typography.titleMedium)
                if (friendsTop.isEmpty()) {
                    Text("Sem cache de amigos (ou ainda não tens amigos).")
                } else {
                    friendsTop.forEachIndexed { idx, e ->
                        val me = if (e.uid == myUid) " (eu)" else ""
                        val label = e.name.ifBlank { e.uid.take(8) }
                        Text("${idx + 1}. $label$me • ${"%.2f".format(e.distanceKm)} km • ${e.steps} passos")
                    }
                }
            }
        }
    }
}
