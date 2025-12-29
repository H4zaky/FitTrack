package pt.ipp.estg.fittrack.data.local

import android.content.Context
import androidx.room.Room

object DbProvider {
    @Volatile private var db: FitTrackDb? = null

    fun get(context: Context): FitTrackDb =
        db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                FitTrackDb::class.java,
                "fittrack.db"
            ).build().also { db = it }
        }
}
