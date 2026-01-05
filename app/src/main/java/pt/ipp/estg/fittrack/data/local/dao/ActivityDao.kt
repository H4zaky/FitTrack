package pt.ipp.estg.fittrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(session: ActivitySessionEntity)

    @Query("SELECT * FROM activity_sessions ORDER BY startTs DESC")
    suspend fun getAll(): List<ActivitySessionEntity>

    @Query("SELECT * FROM activity_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ActivitySessionEntity?

    @Query("""
        UPDATE activity_sessions
        SET startLat = :startLat, startLon = :startLon
        WHERE id = :id
    """)
    suspend fun setStartLocation(id: String, startLat: Double, startLon: Double)

    @Query("""
        UPDATE activity_sessions
        SET endTs = :endTs,
            distanceKm = :distanceKm,
            durationMin = :durationMin,
            endLat = :endLat,
            endLon = :endLon,
            avgSpeedMps = :avgSpeedMps,
            elevationGainM = :elevationGainM
        WHERE id = :id
    """)
    suspend fun finalizeSession(
        id: String,
        endTs: Long,
        distanceKm: Double,
        durationMin: Int,
        endLat: Double?,
        endLon: Double?,
        avgSpeedMps: Double,
        elevationGainM: Double
    )
}