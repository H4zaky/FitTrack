package pt.ipp.estg.fittrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ActivitySessionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class FitTrackDb : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
}
