package pt.ipp.estg.fittrack.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector? = null
) {
    data object Activity : Screen("activity", "Atividade", Icons.Filled.DirectionsRun)
    data object History : Screen("history", "Histórico", Icons.Filled.History)
    data object Friends : Screen("friends", "Amigos", Icons.Filled.People)

    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

val bottomScreens = listOf(
    Screen.Activity,
    Screen.History,
    Screen.Friends
)

val drawerScreens = listOf(
    Screen.Settings
)
