package pt.ipp.estg.fittrack.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import pt.ipp.estg.fittrack.data.local.dao.ActivityDao
import pt.ipp.estg.fittrack.data.local.dao.FriendDao
import pt.ipp.estg.fittrack.data.local.dao.LeaderboardSnapshotDao
import pt.ipp.estg.fittrack.data.local.dao.TrackPointDao
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import pt.ipp.estg.fittrack.data.local.entity.FriendEntity
import pt.ipp.estg.fittrack.data.local.entity.LeaderboardSnapshotEntity
import pt.ipp.estg.fittrack.data.local.entity.TrackPointEntity

@Database(
    entities = [
        ActivitySessionEntity::class,
        TrackPointEntity::class,
        FriendEntity::class,
        LeaderboardSnapshotEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class FitTrackDb : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun friendDao(): FriendDao
    abstract fun leaderboardSnapshotDao(): LeaderboardSnapshotDao
}
