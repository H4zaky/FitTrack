package pt.ipp.estg.fittrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pt.ipp.estg.fittrack.data.local.entity.LeaderboardSnapshotEntity

@Dao
interface LeaderboardSnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<LeaderboardSnapshotEntity>)

    @Query("""
        SELECT * FROM leaderboard_snapshot
        WHERE month = :month
        ORDER BY distanceKm DESC
        LIMIT :limit
    """)
    fun observeTop(month: String, limit: Int): Flow<List<LeaderboardSnapshotEntity>>

    @Query("""
        SELECT * FROM leaderboard_snapshot
        WHERE month = :month AND uid IN (:uids)
        ORDER BY distanceKm DESC
    """)
    fun observeForUids(month: String, uids: List<String>): Flow<List<LeaderboardSnapshotEntity>>

    @Query("""
        SELECT * FROM leaderboard_snapshot
        WHERE month = :month AND uid = :uid
        LIMIT 1
    """)
    suspend fun getOne(month: String, uid: String): LeaderboardSnapshotEntity?

    @Query("""
        SELECT * FROM leaderboard_snapshot
        WHERE month = :month
        ORDER BY distanceKm DESC
        LIMIT :limit
    """)
    suspend fun getTopOnce(month: String, limit: Int): List<LeaderboardSnapshotEntity>

    @Query("""
        DELETE FROM leaderboard_snapshot
        WHERE month NOT IN (:keepMonths)
    """)
    suspend fun pruneExceptMonths(keepMonths: List<String>)
}
