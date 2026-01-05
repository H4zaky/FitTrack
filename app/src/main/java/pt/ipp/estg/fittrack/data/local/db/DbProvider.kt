package pt.ipp.estg.fittrack.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DbProvider {
    @Volatile private var db: FitTrackDb? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS track_points (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sessionId TEXT NOT NULL,
                    ts INTEGER NOT NULL,
                    lat REAL NOT NULL,
                    lon REAL NOT NULL,
                    accuracyM REAL,
                    FOREIGN KEY(sessionId) REFERENCES activity_sessions(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS index_track_points_sessionId ON track_points(sessionId)")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE activity_sessions ADD COLUMN mode TEXT NOT NULL DEFAULT 'MANUAL'")
            database.execSQL("ALTER TABLE activity_sessions ADD COLUMN startLat REAL")
            database.execSQL("ALTER TABLE activity_sessions ADD COLUMN startLon REAL")
            database.execSQL("ALTER TABLE activity_sessions ADD COLUMN endLat REAL")
            database.execSQL("ALTER TABLE activity_sessions ADD COLUMN endLon REAL")
            database.execSQL("ALTER TABLE activity_sessions ADD COLUMN avgSpeedMps REAL NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE activity_sessions ADD COLUMN elevationGainM REAL NOT NULL DEFAULT 0")

            database.execSQL("ALTER TABLE track_points ADD COLUMN speedMps REAL")
            database.execSQL("ALTER TABLE track_points ADD COLUMN altitudeM REAL")
        }
    }

    fun get(context: Context): FitTrackDb =
        db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                FitTrackDb::class.java,
                "fittrack.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { db = it }
        }
}