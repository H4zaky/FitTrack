package pt.ipp.estg.fittrack.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.ipp.estg.fittrack.R

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
    val nameRequiredMessage = stringResource(R.string.auth_name_required)
    val resetSentMessage = stringResource(R.string.auth_reset_sent)
    val genericErrorMessage = stringResource(R.string.auth_error_generic)

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
                    if (isRegister) {
                        stringResource(R.string.auth_create_account)
                    } else {
                        stringResource(R.string.auth_sign_in)
                    },
                    style = MaterialTheme.typography.headlineSmall
                )

                if (isRegister) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.field_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.field_email)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text(stringResource(R.string.auth_password)) },
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
                                        msg = nameRequiredMessage
                                    } else {
                                        onRegister(name, email, pass)
                                    }
                                } else {
                                    onLogin(email, pass)
                                }
                            } catch (e: Exception) {
                                msg = e.message ?: genericErrorMessage
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isRegister) {
                            stringResource(R.string.auth_register)
                        } else {
                            stringResource(R.string.auth_login)
                        }
                    )
                }

                TextButton(
                    onClick = { isRegister = !isRegister },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isRegister) {
                            stringResource(R.string.auth_already_have_account)
                        } else {
                            stringResource(R.string.auth_create_account)
                        }
                    )
                }

                TextButton(
                    onClick = {
                        scope.launch {
                            msg = null
                            busy = true
                            try {
                                onResetPassword(email)
                                msg = resetSentMessage
                            } catch (e: Exception) {
                                msg = e.message ?: genericErrorMessage
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && email.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.auth_forgot_password))
                }
            }
        }
    }
}
