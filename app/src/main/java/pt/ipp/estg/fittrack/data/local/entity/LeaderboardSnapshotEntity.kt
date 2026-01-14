package pt.ipp.estg.fittrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "leaderboard_snapshot",
    primaryKeys = ["month", "uid"],
    indices = [
        Index(value = ["month"]),
        Index(value = ["uid"])
    ]
)
data class LeaderboardSnapshotEntity(
    val month: String,
    val uid: String,
    val name: String,
    val distanceKm: Double,
    val steps: Int,
    val sessions: Int,
    val updatedAt: Long
)
