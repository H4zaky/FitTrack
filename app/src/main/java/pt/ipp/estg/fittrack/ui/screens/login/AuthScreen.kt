package pt.ipp.estg.fittrack.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val fs = remember { FirebaseFirestore.getInstance() }

    var isRegister by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Card(modifier.padding(16.dp)) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (isRegister) "Criar conta" else "Entrar",
                style = MaterialTheme.typography.headlineSmall
            )

            if (isRegister) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    error = null
                    loading = true

                    scope.launch {
                        try {
                            if (isRegister) {
                                val res = auth.createUserWithEmailAndPassword(email, pass).await()
                                val user = res.user ?: throw IllegalStateException("No user")

                                val req = userProfileChangeRequest {
                                    displayName = name.ifBlank { "User" }
                                }
                                user.updateProfile(req).await()

                                // opcional: guardar perfil no Firestore
                                fs.collection("users").document(user.uid)
                                    .set(
                                        mapOf(
                                            "uid" to user.uid,
                                            "name" to (name.ifBlank { "User" }),
                                            "email" to email,
                                            "createdAt" to System.currentTimeMillis()
                                        )
                                    ).await()
                            } else {
                                auth.signInWithEmailAndPassword(email, pass).await()
                            }
                        } catch (t: Throwable) {
                            error = t.message ?: "Erro"
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading && email.isNotBlank() && pass.isNotBlank() && (!isRegister || name.isNotBlank()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Aguarda..." else if (isRegister) "Registar" else "Entrar")
            }

            TextButton(
                onClick = { isRegister = !isRegister },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isRegister) "Já tenho conta" else "Criar conta")
            }
        }
    }
}
