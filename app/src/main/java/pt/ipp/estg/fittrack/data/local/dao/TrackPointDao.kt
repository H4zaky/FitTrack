package pt.ipp.estg.fittrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pt.ipp.estg.fittrack.data.local.entity.TrackPointEntity

@Dao
interface TrackPointDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(point: TrackPointEntity)

    @Query("SELECT * FROM track_points WHERE sessionId = :sessionId ORDER BY ts ASC")
    suspend fun getBySession(sessionId: String): List<TrackPointEntity>
}