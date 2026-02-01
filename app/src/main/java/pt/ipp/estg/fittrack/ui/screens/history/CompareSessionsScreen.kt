@file:OptIn(ExperimentalMaterial3Api::class)

package pt.ipp.estg.fittrack.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
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
import pt.ipp.estg.fittrack.core.`public`.PublicSessionsRepository
import pt.ipp.estg.fittrack.core.`public`.PublicSessionsRepository.PublicSession
import pt.ipp.estg.fittrack.data.local.dao.ActivityDao
import pt.ipp.estg.fittrack.data.local.dao.FriendDao
import pt.ipp.estg.fittrack.data.local.dao.TrackPointDao
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import pt.ipp.estg.fittrack.data.local.entity.FriendEntity
import pt.ipp.estg.fittrack.ui.components.ActivityDetailsCard
import pt.ipp.estg.fittrack.ui.history.CompareHistoryMap
import pt.ipp.estg.fittrack.ui.history.SessionLike
import pt.ipp.estg.fittrack.ui.history.TrackPointLike
import pt.ipp.estg.fittrack.ui.history.toDetails
import pt.ipp.estg.fittrack.ui.history.toSessionLike
import pt.ipp.estg.fittrack.ui.history.toTrackPointLike

@Composable
fun CompareSessionsScreen(
    userId: String,
    firstSessionId: String,
    secondSessionId: String,
    activityDao: ActivityDao,
    friendDao: FriendDao,
    trackPointDao: TrackPointDao
) {
    var localSessions by remember { mutableStateOf<List<ActivitySessionEntity>>(emptyList()) }
    var friends by remember { mutableStateOf<List<FriendEntity>>(emptyList()) }
    var publicSessions by remember { mutableStateOf<List<PublicSession>>(emptyList()) }

    var firstSession by remember { mutableStateOf<SessionLike?>(null) }
    var secondSession by remember { mutableStateOf<SessionLike?>(null) }
    var firstPoints by remember { mutableStateOf<List<TrackPointLike>>(emptyList()) }
    var secondPoints by remember { mutableStateOf<List<TrackPointLike>>(emptyList()) }

    var firstSelectedId by remember { mutableStateOf(firstSessionId) }
    var secondSelectedId by remember { mutableStateOf(secondSessionId) }
    var secondSource by remember { mutableStateOf(SessionSource.LOCAL) }
    var selectedFriend by remember { mutableStateOf<FriendEntity?>(null) }

    LaunchedEffect(userId) {
        val loaded = withContext(Dispatchers.IO) {
            val sessions = activityDao.getAllForUser(userId)
            val friendList = friendDao.getAllForUser(userId)
            sessions to friendList
        }
        localSessions = loaded.first
        friends = loaded.second
        if (selectedFriend == null && friends.isNotEmpty()) {
            selectedFriend = friends.firstOrNull { it.uid != null } ?: friends.first()
        }
        if (firstSelectedId.isBlank() && localSessions.isNotEmpty()) {
            firstSelectedId = localSessions.first().id
        }
        if (secondSource == SessionSource.LOCAL && secondSelectedId.isBlank() && localSessions.size >= 2) {
            secondSelectedId = localSessions.first { it.id != firstSelectedId }.id
        }
    }

    LaunchedEffect(secondSource, localSessions) {
        if (secondSource != SessionSource.LOCAL) return@LaunchedEffect
        if (localSessions.isEmpty()) return@LaunchedEffect
        val valid = localSessions.any { it.id == secondSelectedId }
        if (!valid) {
            secondSelectedId = localSessions.first().id
        }
    }

    LaunchedEffect(firstSelectedId) {
        val result = withContext(Dispatchers.IO) {
            val session = activityDao.getByIdForUser(userId, firstSelectedId)
            val points = trackPointDao.getBySession(firstSelectedId)
            session?.toSessionLike() to points.map { it.toTrackPointLike() }
        }
        firstSession = result.first
        firstPoints = result.second
    }

    LaunchedEffect(secondSource, secondSelectedId, selectedFriend?.uid) {
        if (secondSelectedId.isBlank()) {
            secondSession = null
            secondPoints = emptyList()
            return@LaunchedEffect
        }
        if (secondSource == SessionSource.PUBLIC && selectedFriend?.uid == null) {
            secondSession = null
            secondPoints = emptyList()
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.IO) {
            when (secondSource) {
                SessionSource.LOCAL -> {
                    val session = activityDao.getByIdForUser(userId, secondSelectedId)
                    val points = trackPointDao.getBySession(secondSelectedId)
                    session?.toSessionLike() to points.map { it.toTrackPointLike() }
                }
                SessionSource.PUBLIC -> {
                    val session = PublicSessionsRepository.getPublicSession(secondSelectedId)
                    val points = PublicSessionsRepository.getPublicTrackPoints(secondSelectedId)
                    session?.toSessionLike() to points.map { it.toTrackPointLike() }
                }
            }
        }
        secondSession = result.first
        secondPoints = result.second
    }

    LaunchedEffect(secondSource, selectedFriend?.uid) {
        if (secondSource != SessionSource.PUBLIC) {
            publicSessions = emptyList()
            return@LaunchedEffect
        }
        val friendUid = selectedFriend?.uid ?: run {
            publicSessions = emptyList()
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            PublicSessionsRepository.listPublicSessionsForOwner(friendUid)
        }
        publicSessions = loaded
        if (publicSessions.isNotEmpty()) {
            secondSelectedId = publicSessions.first().id
        }
    }

    Column(
        modifier = Modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Comparar sessões", style = MaterialTheme.typography.headlineSmall)

        CompareSelectionCard(
            title = "Sessão A (local)",
            sessions = localSessions,
            selectedId = firstSelectedId,
            onSelectedId = { firstSelectedId = it },
            modifier = Modifier.fillMaxWidth()
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sessão B", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        RadioButton(
                            selected = secondSource == SessionSource.LOCAL,
                            onClick = { secondSource = SessionSource.LOCAL }
                        )
                        Text("Local")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        RadioButton(
                            selected = secondSource == SessionSource.PUBLIC,
                            onClick = { secondSource = SessionSource.PUBLIC }
                        )
                        Text("Pública")
                    }
                }

                if (secondSource == SessionSource.PUBLIC) {
                    FriendSelector(
                        friends = friends,
                        selected = selectedFriend,
                        onSelected = {
                            selectedFriend = it
                            secondSelectedId = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    PublicSessionSelector(
                        sessions = publicSessions,
                        selectedId = secondSelectedId,
                        onSelectedId = { secondSelectedId = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    CompareSelectionCard(
                        title = "Sessão B (local)",
                        sessions = localSessions,
                        selectedId = secondSelectedId,
                        onSelectedId = { secondSelectedId = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (firstSession == null || secondSession == null) {
            Text("Não foi possível carregar as duas sessões.")
            return@Column
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mapa combinado", style = MaterialTheme.typography.titleMedium)
                CompareHistoryMap(
                    firstPoints = firstPoints,
                    secondPoints = secondPoints,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActivityDetailsCard(
                details = firstSession?.toDetails("A"),
                modifier = Modifier.weight(1f)
            )
            ActivityDetailsCard(
                details = secondSession?.toDetails("B"),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompareSelectionCard(
    title: String,
    sessions: List<ActivitySessionEntity>,
    selectedId: String,
    onSelectedId: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            SessionDropdown(
                sessions = sessions,
                selectedId = selectedId,
                onSelectedId = onSelectedId,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SessionDropdown(
    sessions: List<ActivitySessionEntity>,
    selectedId: String,
    onSelectedId: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = sessions.firstOrNull { it.id == selectedId }
    val display = selected?.title ?: "Selecionar sessão"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sessões") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            sessions.forEach { session ->
                DropdownMenuItem(
                    text = { Text(session.title) },
                    onClick = {
                        onSelectedId(session.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FriendSelector(
    friends: List<FriendEntity>,
    selected: FriendEntity?,
    onSelected: (FriendEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val display = selected?.name ?: "Selecionar amigo"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text("Amigo") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            friends.forEach { friend ->
                val name = friend.name.ifBlank { friend.phone }
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelected(friend)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PublicSessionSelector(
    sessions: List<PublicSession>,
    selectedId: String,
    onSelectedId: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = sessions.firstOrNull { it.id == selectedId }
    val display = selected?.title ?: "Selecionar sessão pública"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sessões públicas") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (sessions.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Sem sessões públicas") },
                    onClick = { expanded = false }
                )
            } else {
                sessions.forEach { session ->
                    DropdownMenuItem(
                        text = { Text(session.title) },
                        onClick = {
                            onSelectedId(session.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private enum class SessionSource {
    LOCAL,
    PUBLIC
}
