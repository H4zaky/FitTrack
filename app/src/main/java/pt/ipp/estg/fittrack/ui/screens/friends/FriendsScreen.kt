package pt.ipp.estg.fittrack.ui.screens.friends

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pt.ipp.estg.fittrack.R
import pt.ipp.estg.fittrack.core.contacts.ContactsUtil
import pt.ipp.estg.fittrack.data.local.dao.FriendDao
import pt.ipp.estg.fittrack.data.local.entity.FriendEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    ownerUid: String,
    friendDao: FriendDao,
    onViewActivities: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fs = remember { FirebaseFirestore.getInstance() }

    val friends by friendDao.observeAllForUser(ownerUid).collectAsState(initial = emptyList())

    var query by rememberSaveable { mutableStateOf("") }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    // Form add
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val filtered = remember(friends, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) friends
        else friends.filter {
            it.name.lowercase().contains(q) || it.phone.lowercase().contains(q)
        }
    }

    val pickContact = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val res = ContactsUtil.readPickedContact(context, uri) ?: return@rememberLauncherForActivityResult
        name = res.name
        phone = res.phone
    }

    suspend fun resolveUidByPhone(phoneNorm: String): String? {
        val snap = fs.collection("users")
            .whereEqualTo("phone", phoneNorm)
            .limit(1)
            .get()
            .await()
        return snap.documents.firstOrNull()?.id
    }

    fun resetAddForm() {
        name = ""
        phone = ""
        saving = false
    }

    suspend fun addFriend() {
        val n = name.trim()
        val p = ContactsUtil.normalizePhone(phone)

        if (ownerUid.isBlank()) return

        if (n.isBlank() || p.isBlank()) {
            Toast.makeText(
                context,
                context.getString(R.string.friends_invalid_name_phone),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        saving = true

        val friendUid = withContext(Dispatchers.IO) {
            runCatching { resolveUidByPhone(p) }.getOrNull()
        }

        // não deixar adicionar a si próprio
        if (friendUid != null && friendUid == ownerUid) {
            saving = false
            Toast.makeText(
                context,
                context.getString(R.string.friends_cannot_add_self),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val entity = FriendEntity(
            ownerUid = ownerUid,
            phone = p,
            name = n,
            createdAt = System.currentTimeMillis(),
            uid = friendUid
        )

        // 1) Room (por user)
        withContext(Dispatchers.IO) { friendDao.upsert(entity) }

        // 2) Firestore (por user): users/{ownerUid}/friends/{docId}
        val docId = friendUid ?: p
        runCatching {
            fs.collection("users").document(ownerUid)
                .collection("friends").document(docId)
                .set(
                    mapOf(
                        "uid" to friendUid,
                        "phone" to p,
                        "name" to n,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
        }

        saving = false
        Toast.makeText(context, context.getString(R.string.friends_added), Toast.LENGTH_SHORT).show()
        showAddSheet = false
        resetAddForm()
    }

    fun removeFriend(f: FriendEntity) {
        scope.launch {
            withContext(Dispatchers.IO) {
                friendDao.deleteByPhoneForUser(ownerUid, f.phone)
            }

            val docId = f.uid ?: f.phone
            runCatching {
                fs.collection("users").document(ownerUid)
                    .collection("friends").document(docId)
                    .delete()
                    .await()
            }

            Toast.makeText(context, context.getString(R.string.friends_removed), Toast.LENGTH_SHORT)
                .show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.friends_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.friends_total, friends.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(
                    Icons.Outlined.PersonAddAlt,
                    contentDescription = stringResource(R.string.friends_add_friend)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                label = { Text(stringResource(R.string.friends_search)) },
                placeholder = { Text(stringResource(R.string.friends_search_hint)) }
            )

            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                EmptyFriendsState(
                    onAdd = { showAddSheet = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.ownerUid + "|" + it.phone }) { f ->
                        FriendRow(
                            name = f.name,
                            phone = f.phone,
                            friendUid = f.uid,
                            onRemove = { removeFriend(f) },
                            onViewActivities = {
                                val uid = f.uid
                                if (uid == null) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.friends_no_account),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    onViewActivities(uid, f.name)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddSheet = false
                resetAddForm()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.friends_add_friend), style = MaterialTheme.typography.titleLarge)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickContact.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.friends_from_contacts)) }

                    OutlinedButton(
                        onClick = { resetAddForm() },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.friends_clear)) }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.field_name)) }
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.field_phone)) }
                )

                Button(
                    onClick = { scope.launch { addFriend() } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving
                ) {
                    if (saving) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.friends_saving))
                    } else {
                        Text(stringResource(R.string.friends_add))
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FriendRow(
    name: String,
    phone: String,
    friendUid: String?,
    onRemove: () -> Unit,
    onViewActivities: () -> Unit
) {
    val context = LocalContext.current
    val unknownInitial = stringResource(R.string.friends_unknown_initial)
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            leadingContent = {
                val initial = name.trim().firstOrNull()?.uppercase() ?: unknownInitial
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
            headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Column {
                    Text(phone, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (friendUid == null) {
                        Text(
                            stringResource(R.string.friends_no_account),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            trailingContent = {
                Row {
                    IconButton(
                        onClick = onViewActivities,
                        enabled = friendUid != null
                    ) {
                        Icon(
                            Icons.Outlined.DirectionsRun,
                            contentDescription = stringResource(R.string.friends_view_activities)
                        )
                    }
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$phone")
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Phone,
                            contentDescription = stringResource(R.string.friends_call)
                        )
                    }
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:$phone")
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Message,
                            contentDescription = stringResource(R.string.friends_sms)
                        )
                    }
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.btn_remove)
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun EmptyFriendsState(
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Group,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.friends_empty_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.friends_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Outlined.PersonAddAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.friends_add_friend))
        }
    }
}
