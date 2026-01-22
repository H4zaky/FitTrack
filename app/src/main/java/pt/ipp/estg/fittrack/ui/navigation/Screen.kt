package pt.ipp.estg.fittrack.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector? = null
) {
    data object Activity : Screen("activity", "Atividade", Icons.AutoMirrored.Filled.DirectionsRun)
    data object History : Screen("history", "Histórico", Icons.Filled.History)
    data object Friends : Screen("friends", "Amigos", Icons.Filled.Group)
    data object Social : Screen("social", "Rankings", Icons.Filled.EmojiEvents)
    data object Settings : Screen("settings", "Definições", Icons.Filled.Settings)

    data object Detail : Screen("detail/{id}", "Detalhe") {
        fun route(id: String) = "detail/$id"
        const val routePattern = "detail/{id}"
    }

    data object Compare : Screen("compare/{firstId}/{secondId}", "Comparar") {
        fun route(firstId: String, secondId: String) = "compare/$firstId/$secondId"
        const val routePattern = "compare/{firstId}/{secondId}"
    }
}

val bottomScreens = listOf(Screen.Activity, Screen.History, Screen.Friends, Screen.Social)
val drawerScreens = listOf(Screen.Settings)
