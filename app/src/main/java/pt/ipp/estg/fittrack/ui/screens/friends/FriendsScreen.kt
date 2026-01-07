package pt.ipp.estg.fittrack.ui.screens.friends

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Message
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pt.ipp.estg.fittrack.core.contacts.ContactsUtil
import pt.ipp.estg.fittrack.data.local.db.DbProvider
import pt.ipp.estg.fittrack.data.local.entity.FriendEntity

@Composable
fun FriendsScreen() {
    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }
    val dao = remember { db.friendDao() }
    val scope = remember { CoroutineScope(Dispatchers.IO) }

    val friends by dao.observeAll().collectAsState(initial = emptyList())

    val pickContact = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val picked = ContactsUtil.readPickedContact(context, uri) ?: return@rememberLauncherForActivityResult
        scope.launch {
            dao.upsert(FriendEntity(phone = picked.phone, name = picked.name))
        }
    }

    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Amigos", style = MaterialTheme.typography.titleLarge)

        Button(
            onClick = { pickContact.launch(null) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Adicionar amigo pelos Contactos") }

        if (friends.isEmpty()) {
            Text("Sem amigos ainda. Adiciona pelos contactos.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(friends) { f ->
                    FriendRow(
                        name = f.name,
                        phone = f.phone,
                        onCall = {
                            val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${f.phone}"))
                            context.startActivity(i)
                        },
                        onSms = {
                            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${f.phone}"))
                            context.startActivity(i)
                        },
                        onDelete = {
                            scope.launch { dao.deleteByPhone(f.phone) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendRow(
    name: String,
    phone: String,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(phone, style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onCall) { Icon(Icons.Default.Call, contentDescription = "Ligar") }
                IconButton(onClick = onSms) { Icon(Icons.Default.Message, contentDescription = "SMS") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Remover") }
            }
        }
    }
}
