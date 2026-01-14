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

    @Query("SELECT * FROM friends ORDER BY name")
    suspend fun getAll(): List<FriendEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(friend: FriendEntity)

    @Query("DELETE FROM friends WHERE phone = :phone")
    suspend fun deleteByPhone(phone: String)

    @Query("SELECT * FROM friends ORDER BY name")
    suspend fun getAllOnce(): List<FriendEntity>

    @Query("""
        SELECT * FROM friends
        WHERE ownerUid = :ownerUid
        ORDER BY createdAt DESC
    """)
    fun observeAllForUser(ownerUid: String): Flow<List<FriendEntity>>
    @Query("""
        SELECT * FROM friends
        WHERE ownerUid = :ownerUid
        ORDER BY createdAt DESC
    """)
    suspend fun getAllForUser(ownerUid: String): List<FriendEntity>
    @Query("DELETE FROM friends WHERE ownerUid = :ownerUid AND phone = :phone")
    suspend fun deleteByPhoneForUser(ownerUid: String, phone: String)

    @Query("DELETE FROM friends WHERE ownerUid = :ownerUid")
    suspend fun deleteAllForUser(ownerUid: String)
}
