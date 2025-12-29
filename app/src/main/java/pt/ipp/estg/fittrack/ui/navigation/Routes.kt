package pt.ipp.estg.fittrack.ui.navigation

sealed class Route(val path: String, val title: String) {
    data object Activity : Route("activity", "Atividade")
    data object History : Route("history", "Histórico")
    data object Social : Route("social", "Rankings & Amigos")
}

val drawerItems = listOf(Route.Activity, Route.History, Route.Social)
