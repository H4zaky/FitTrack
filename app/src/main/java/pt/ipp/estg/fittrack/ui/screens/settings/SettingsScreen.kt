package pt.ipp.estg.fittrack.ui.screens.settings

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.ipp.estg.fittrack.R
import java.util.Locale

@Composable
fun SettingsScreen(
    initialName: String,
    initialEmail: String,
    initialPhone: String,
    onSaveName: suspend (String) -> Unit,
    onSavePhone: suspend (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estado editável
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    var phone by remember { mutableStateOf(initialPhone) }

    // Flags de edição (para não “pisar” o que o utilizador está a escrever)
    var nameDirty by remember { mutableStateOf(false) }
    var phoneDirty by remember { mutableStateOf(false) }

    var saving by remember { mutableStateOf(false) }

    // Quando o AuthGate atualizar o profile e reenviar initial*, atualiza o UI
    LaunchedEffect(initialName) {
        if (!nameDirty) name = initialName
    }
    LaunchedEffect(initialEmail) {
        email = initialEmail
    }
    LaunchedEffect(initialPhone) {
        if (!phoneDirty) phone = initialPhone
    }

    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = stringResource(R.string.settings_title))
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameDirty = true
            },
            label = { Text(stringResource(R.string.field_name)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { /* email é readonly */ },
            label = { Text(stringResource(R.string.field_email)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = {
                phone = it
                phoneDirty = true
            },
            label = { Text(stringResource(R.string.field_phone)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving
        )

        Spacer(modifier = Modifier.height(16.dp))

        LanguageSelector()

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val n = name.trim()
                val p = phone.trim()

                scope.launch {
                    saving = true
                    val ok = runCatching {
                        // guarda os dois (podes guardar só o que mudou, mas assim é simples)
                        onSaveName(n)
                        onSavePhone(p)
                    }.isSuccess
                    saving = false

                    if (ok) {
                        nameDirty = false
                        phoneDirty = false
                        toast(context.getString(R.string.msg_saved))
                    } else {
                        toast("Falha ao guardar (ver logs).")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving
        ) {
            if (saving) CircularProgressIndicator()
            else Text(text = stringResource(R.string.btn_save_name))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onLogout() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving
        ) {
            Text(text = stringResource(R.string.btn_logout))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector() {
    val context = LocalContext.current
    val activity = context as? Activity
    val languages = listOf(
        stringResource(R.string.language_english),
        stringResource(R.string.language_portuguese)
    )
    val currentLocale = context.resources.configuration.locales[0]
    val initialLanguage = if (currentLocale.language == "pt") languages[1] else languages[0]
    var expanded by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(initialLanguage) }

    Text(
        text = stringResource(R.string.language),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedLanguage,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language) },
                    onClick = {
                        selectedLanguage = language
                        expanded = false
                        val newLocale = if (language == languages[1]) "pt" else "en"
                        if (newLocale != currentLocale.language) {
                            activity?.let { setLocale(it, newLocale) }
                        }
                    }
                )
            }
        }
    }
}

private fun setLocale(activity: Activity, languageCode: String) {
    val locale = Locale.forLanguageTag(languageCode)
    Locale.setDefault(locale)
    val resources = activity.resources
    val config = resources.configuration
    config.setLocale(locale)
    activity.applyOverrideConfiguration(config)
    activity.recreate()
}
