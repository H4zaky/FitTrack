package pt.ipp.estg.fittrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_points",
    indices = [Index("sessionId")],
    foreignKeys = [
        ForeignKey(
            entity = ActivitySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ]
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val ts: Long,
    val lat: Double,
    val lon: Double,
    val accuracyM: Float? = null,
    val speedMps: Float? = null,
    val altitudeM: Double? = null
)