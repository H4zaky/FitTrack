package pt.ipp.estg.fittrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ActivitySessionEntity)

    @Query("SELECT * FROM activity_sessions ORDER BY startTs DESC")
    suspend fun getAll(): List<ActivitySessionEntity>

    @Query("SELECT * FROM activity_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ActivitySessionEntity?
}
