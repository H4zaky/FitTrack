package pt.ipp.estg.fittrack.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import pt.ipp.estg.fittrack.ui.screens.activity.ActivityScreen
import pt.ipp.estg.fittrack.ui.screens.history.HistoryScreen
import pt.ipp.estg.fittrack.ui.screens.settings.SettingsScreen
import pt.ipp.estg.fittrack.ui.screens.social.SocialScreen

private data class BottomItem(
    val routePath: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(userName: String = "User") {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    // ✅ Bottom bar só com 3
    val bottomItems = listOf(
        BottomItem(Route.Activity.path, "Atividade", Icons.Default.FitnessCenter),
        BottomItem(Route.History.path, "Histórico", Icons.Default.History),
        BottomItem(Route.Social.path, "Amigos", Icons.Default.Group),
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(8.dp))

                // ✅ Drawer só com Settings
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = destination?.hierarchy?.any { it.route == Route.Settings.path } == true,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Route.Settings.path) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("FitTrack") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = destination?.hierarchy?.any { it.route == item.routePath } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.routePath) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(Route.Activity.path) { saveState = true }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Route.Activity.path,
                modifier = Modifier.padding(padding)
            ) {
                composable(Route.Activity.path) { ActivityScreen(userName = userName) }
                composable(Route.History.path) { HistoryScreen() }
                composable(Route.Social.path) { SocialScreen() }

                // ✅ Settings
                composable(Route.Settings.path) { SettingsScreen() }
            }
        }
    }
}
