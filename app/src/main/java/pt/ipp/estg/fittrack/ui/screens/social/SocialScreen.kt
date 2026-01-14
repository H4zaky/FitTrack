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
    val db = remember { DbProvider.get(context) }
    val friendDao = remember { db.friendDao() }

    val myUid = TrackingPrefs.getUserId(context).orEmpty()
    val myName = TrackingPrefs.getUserName(context)?.trim().orEmpty()
    val month = remember { LeaderboardCacheRepository.monthKey() }

    var friendUids by remember { mutableStateOf(emptyList<String>()) }
    var friendNameByUid by remember { mutableStateOf(emptyMap<String, String>()) }

    LaunchedEffect(myUid) {
        if (myUid.isBlank()) return@LaunchedEffect

        val friends = withContext(Dispatchers.IO) {
            // ✅ importante: por user
            friendDao.getAllForUser(myUid)
        }

        friendUids = friends.mapNotNull { it.uid?.takeIf { u -> u.isNotBlank() } }

        friendNameByUid = friends
            .mapNotNull { f -> f.uid?.takeIf { it.isNotBlank() }?.let { it to f.name } }
            .toMap()
    }

    val global by LeaderboardCacheRepository.observeGlobalTop(context, month, 15)
        .collectAsState(initial = emptyList())

    val friendsTop by LeaderboardCacheRepository.observeForUids(
        context,
        month,
        (friendUids + myUid).distinct().filter { it.isNotBlank() }
    ).collectAsState(initial = emptyList())

    LaunchedEffect(friendUids, myUid) {
        LeaderboardCacheRepository.refreshGlobalTop(context, 15)
        LeaderboardCacheRepository.refreshForUids(context, (friendUids + myUid).filter { it.isNotBlank() }.toSet())
    }

    fun displayName(uid: String, cachedName: String): String {
        if (uid == myUid) return if (myName.isNotBlank()) myName else "Eu"
        val friendName = friendNameByUid[uid]
        if (!friendName.isNullOrBlank()) return friendName
        if (cachedName.isNotBlank()) return cachedName
        return uid.take(8) // fallback final (idealmente nunca aparece)
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
                        val name = displayName(e.uid, e.name)
                        Text("${idx + 1}. $name • ${"%.2f".format(e.distanceKm)} km • ${e.steps} passos")
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
                        val name = displayName(e.uid, e.name)
                        val me = if (e.uid == myUid) " (eu)" else ""
                        Text("${idx + 1}. $name$me • ${"%.2f".format(e.distanceKm)} km • ${e.steps} passos")
                    }
                }
            }
        }
    }
}
