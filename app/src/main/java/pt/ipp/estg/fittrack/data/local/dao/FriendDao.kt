package pt.ipp.estg.fittrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pt.ipp.estg.fittrack.data.local.entity.FriendEntity

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY name")
    fun observeAll(): Flow<List<FriendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(friend: FriendEntity)

    @Query("DELETE FROM friends WHERE phone = :phone")
    suspend fun deleteByPhone(phone: String)
}