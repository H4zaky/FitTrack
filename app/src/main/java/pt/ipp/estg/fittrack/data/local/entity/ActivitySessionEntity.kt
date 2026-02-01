package pt.ipp.estg.fittrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_sessions")
data class ActivitySessionEntity(
    @PrimaryKey val id: String,

    val userId: String,
    val isPublic: Boolean = false,

    val title: String,
    val type: String,
    val startTs: Long,
    val endTs: Long?,

    val distanceKm: Double,
    val durationMin: Int,

    val mode: String = "MANUAL",
    val startLat: Double? = null,
    val startLon: Double? = null,
    val endLat: Double? = null,
    val endLon: Double? = null,
    val avgSpeedMps: Double = 0.0,
    val elevationGainM: Double = 0.0,

    val steps: Int = 0,
    val photoBeforeUri: String? = null,
    val photoAfterUri: String? = null,

    val weatherTempC: Double? = null,
    val weatherWindKmh: Double? = null,
    val weatherCode: Int? = null
)
