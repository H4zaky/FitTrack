package pt.ipp.estg.fittrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ActivitySessionEntity)

    @Query("SELECT * FROM activity_sessions WHERE userId = :userId ORDER BY startTs DESC")
    suspend fun getAllForUser(userId: String): List<ActivitySessionEntity>

    @Query("SELECT * FROM activity_sessions WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getByIdForUser(userId: String, id: String): ActivitySessionEntity?

    @Query("DELETE FROM activity_sessions WHERE id = :id AND userId = :userId")
    suspend fun deleteByIdForUser(userId: String, id: String)

    @Query("DELETE FROM activity_sessions WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("""
        UPDATE activity_sessions
        SET startLat = :startLat, startLon = :startLon
        WHERE id = :id AND userId = :userId
    """)
    suspend fun setStartLocationForUser(userId: String, id: String, startLat: Double, startLon: Double)

    @Query("""
        UPDATE activity_sessions
        SET weatherTempC = :tempC,
            weatherWindKmh = :windKmh,
            weatherCode = :code
        WHERE id = :id AND userId = :userId
    """)
    suspend fun updateWeatherForUser(userId: String, id: String, tempC: Double?, windKmh: Double?, code: Int?)

    @Query("""
        UPDATE activity_sessions
        SET endTs = :endTs,
            distanceKm = :distanceKm,
            durationMin = :durationMin,
            endLat = :endLat,
            endLon = :endLon,
            avgSpeedMps = :avgSpeedMps,
            elevationGainM = :elevationGainM,
            steps = :steps,
            photoBeforeUri = :photoBeforeUri,
            photoAfterUri = :photoAfterUri
        WHERE id = :id AND userId = :userId
    """)
    suspend fun finalizeSessionForUser(
        userId: String,
        id: String,
        endTs: Long,
        distanceKm: Double,
        durationMin: Int,
        endLat: Double?,
        endLon: Double?,
        avgSpeedMps: Double,
        elevationGainM: Double,
        steps: Int,
        photoBeforeUri: String?,
        photoAfterUri: String?
    )

    // --- legacy / shared ---
    @Query("SELECT * FROM activity_sessions ORDER BY startTs DESC")
    suspend fun getAll(): List<ActivitySessionEntity>

    @Query("SELECT * FROM activity_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ActivitySessionEntity?

    @Query("DELETE FROM activity_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("""
        UPDATE activity_sessions
        SET startLat = :startLat, startLon = :startLon
        WHERE id = :id
    """)
    suspend fun setStartLocation(id: String, startLat: Double, startLon: Double)

    @Query("""
        UPDATE activity_sessions
        SET weatherTempC = :tempC,
            weatherWindKmh = :windKmh,
            weatherCode = :code
        WHERE id = :id
    """)
    suspend fun updateWeather(id: String, tempC: Double?, windKmh: Double?, code: Int?)

    @Query("""
        UPDATE activity_sessions
        SET endTs = :endTs,
            distanceKm = :distanceKm,
            durationMin = :durationMin,
            endLat = :endLat,
            endLon = :endLon,
            avgSpeedMps = :avgSpeedMps,
            elevationGainM = :elevationGainM,
            steps = :steps,
            photoBeforeUri = :photoBeforeUri,
            photoAfterUri = :photoAfterUri
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
        elevationGainM: Double,
        steps: Int,
        photoBeforeUri: String?,
        photoAfterUri: String?
    )
}
