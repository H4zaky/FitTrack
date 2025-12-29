package pt.ipp.estg.fittrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_sessions")
data class ActivitySessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,      // "Walking" / "Running" / ...
    val startTs: Long,
    val endTs: Long?,
    val distanceKm: Double,
    val durationMin: Int
)
