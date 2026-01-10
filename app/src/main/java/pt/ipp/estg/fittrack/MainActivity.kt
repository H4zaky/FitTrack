package pt.ipp.estg.fittrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import pt.ipp.estg.fittrack.ui.navigation.AppShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val userName = stringResource(R.string.default_user_name)
                AppShell(userName = userName)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainActivityPreview() {
    MaterialTheme {
        val userName = stringResource(R.string.default_user_name)
        AppShell(userName = userName)
    }
}
