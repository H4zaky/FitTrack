package pt.ipp.estg.fittrack.core.leaderboard

data class LeaderboardEntry(
    val uid: String,
    val name: String,
    val phone: String,
    val distanceKm: Double,
    val steps: Long,
    val sessions: Long
)
