package pt.ipp.estg.fittrack.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import pt.ipp.estg.fittrack.data.local.db.DbProvider
import pt.ipp.estg.fittrack.ui.detail.SessionDetailScreen
import pt.ipp.estg.fittrack.ui.screens.activity.ActivityScreen
import pt.ipp.estg.fittrack.ui.screens.friends.FriendsScreen
import pt.ipp.estg.fittrack.ui.screens.history.CompareSessionsScreen
import pt.ipp.estg.fittrack.ui.screens.history.HistoryScreen
import pt.ipp.estg.fittrack.ui.screens.public.FriendPublicSessionDetailScreen
import pt.ipp.estg.fittrack.ui.screens.public.FriendPublicSessionsScreen
import pt.ipp.estg.fittrack.ui.screens.settings.SettingsScreen
import pt.ipp.estg.fittrack.ui.screens.social.SocialScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    userId: String,
    currentName: String,
    userEmail: String,
    currentPhone: String,
    onLogout: () -> Unit,
    onSaveName: suspend (String) -> Unit,
    onSavePhone: suspend (String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }
    val activityDao = remember { db.activityDao() }
    val trackPointDao = remember { db.trackPointDao() }
    val friendDao = remember { db.friendDao() }

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomRoutes = remember { bottomScreens.map { it.route }.toSet() }
    val showBottomBar = (currentRoute == null) || (currentRoute in bottomRoutes)

    val isDetailRoute = currentRoute == Screen.Detail.routePattern || (currentRoute?.startsWith("detail/") == true)
    val isCompareRoute = currentRoute == Screen.Compare.routePattern || (currentRoute?.startsWith("compare/") == true)
    val isPublicSessionsRoute = currentRoute == Screen.FriendPublicSessions.routePattern ||
        (currentRoute?.startsWith("friend-public/") == true)
    val isPublicDetailRoute = currentRoute == Screen.FriendPublicSessionDetail.routePattern ||
        (currentRoute?.startsWith("friend-public-detail/") == true)
    val showBack = (currentRoute == Screen.Settings.route) || isDetailRoute || isCompareRoute ||
        isPublicSessionsRoute || isPublicDetailRoute

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(8.dp))
                drawerScreens.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(item.route) { launchSingleTop = true }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("FitTrack") },
                    navigationIcon = {
                        if (showBack) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomScreens.forEach { screen ->
                            val selected = (currentRoute ?: Screen.Activity.route) == screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Activity.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Icon(screen.icon!!, contentDescription = screen.label) },
                                label = { Text(screen.label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Activity.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Screen.Activity.route) {
                    ActivityScreen(userName = currentName)
                }

                composable(Screen.History.route) {
                    HistoryScreen(
                        activityDao = activityDao,
                        userId = userId,
                        onOpenSession = { sid -> navController.navigate(Screen.Detail.route(sid)) },
                        onCompareSessions = { firstId, secondId ->
                            navController.navigate(Screen.Compare.route(firstId, secondId))
                        }
                    )
                }

                composable(Screen.Friends.route) {
                    FriendsScreen(
                        ownerUid = userId,
                        friendDao = friendDao,
                        onViewActivities = { friendUid, friendName ->
                            navController.navigate(Screen.FriendPublicSessions.route(friendUid, friendName))
                        }
                    )
                }

                composable(Screen.Social.route) {
                    SocialScreen()
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        initialName = currentName,
                        initialEmail = userEmail,
                        initialPhone = currentPhone,
                        onSaveName = onSaveName,
                        onSavePhone = onSavePhone,
                        onLogout = onLogout
                    )
                }


                composable(Screen.Detail.routePattern) { backStack ->
                    val sid = backStack.arguments?.getString("id") ?: return@composable
                    SessionDetailScreen(
                        sessionId = sid,
                        userId = userId,
                        activityDao = activityDao,
                        trackPointDao = trackPointDao,
                        onDeleted = { navController.popBackStack() }
                    )
                }

                composable(Screen.Compare.routePattern) { backStack ->
                    val firstId = backStack.arguments?.getString("firstId") ?: return@composable
                    val secondId = backStack.arguments?.getString("secondId") ?: return@composable
                    CompareSessionsScreen(
                        userId = userId,
                        firstSessionId = firstId,
                        secondSessionId = secondId,
                        activityDao = activityDao,
                        friendDao = friendDao,
                        trackPointDao = trackPointDao
                    )
                }

                composable(Screen.FriendPublicSessions.routePattern) { backStack ->
                    val friendUid = backStack.arguments?.getString("uid") ?: return@composable
                    val friendName = backStack.arguments?.getString("name").orEmpty()
                    FriendPublicSessionsScreen(
                        friendUid = friendUid,
                        friendName = friendName,
                        onOpenSession = { sessionId ->
                            navController.navigate(
                                Screen.FriendPublicSessionDetail.route(sessionId, friendName)
                            )
                        }
                    )
                }

                composable(Screen.FriendPublicSessionDetail.routePattern) { backStack ->
                    val sessionId = backStack.arguments?.getString("sessionId") ?: return@composable
                    val friendName = backStack.arguments?.getString("name").orEmpty()
                    FriendPublicSessionDetailScreen(
                        sessionId = sessionId,
                        friendName = friendName
                    )
                }
            }
        }
    }
}
