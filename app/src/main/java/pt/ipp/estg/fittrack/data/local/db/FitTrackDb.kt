package pt.ipp.estg.fittrack.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import pt.ipp.estg.fittrack.data.local.dao.ActivityDao
import pt.ipp.estg.fittrack.data.local.dao.TrackPointDao
import pt.ipp.estg.fittrack.data.local.entity.ActivitySessionEntity
import pt.ipp.estg.fittrack.data.local.entity.TrackPointEntity

@Database(
    entities = [ActivitySessionEntity::class, TrackPointEntity::class],
    version = 3,
    exportSchema = true
)
abstract class FitTrackDb : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun trackPointDao(): TrackPointDao
}