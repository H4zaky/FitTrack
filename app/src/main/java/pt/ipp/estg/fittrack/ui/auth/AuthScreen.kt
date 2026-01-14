package pt.ipp.estg.fittrack.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onLogin: suspend (email: String, pass: String) -> Unit,
    onRegister: suspend (name: String, email: String, pass: String) -> Unit,
    onResetPassword: suspend (email: String) -> Unit,
) {
    val scope = rememberCoroutineScope()

    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val bg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                msg?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Button(
                    onClick = {
                        scope.launch {
                            msg = null
                            busy = true
                            try {
                                if (isRegister) {
                                    if (name.trim().isEmpty()) {
                                        msg = "Nome é obrigatório."
                                    } else {
                                        onRegister(name, email, pass)
                                    }
                                } else {
                                    onLogin(email, pass)
                                }
                            } catch (e: Exception) {
                                msg = e.message ?: "Erro"
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRegister) "Registar" else "Login")
                }

                TextButton(
                    onClick = { isRegister = !isRegister },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRegister) "Já tenho conta" else "Criar conta")
                }

                TextButton(
                    onClick = {
                        scope.launch {
                            msg = null
                            busy = true
                            try {
                                onResetPassword(email)
                                msg = "Email de recuperação enviado (se o email existir)."
                            } catch (e: Exception) {
                                msg = e.message ?: "Erro"
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && email.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Esqueci-me da password")
                }
            }
        }
    }
}
