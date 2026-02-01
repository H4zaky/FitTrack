package pt.ipp.estg.fittrack.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.ConcurrentHashMap

object DbProvider {

    private val instances = ConcurrentHashMap<String, FitTrackDb>()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
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
            db.execSQL("CREATE INDEX IF NOT EXISTS index_track_points_sessionId ON track_points(sessionId)")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN mode TEXT NOT NULL DEFAULT 'MANUAL'")
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN startLat REAL")
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN startLon REAL")
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN endLat REAL")
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN endLon REAL")
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN avgSpeedMps REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN elevationGainM REAL NOT NULL DEFAULT 0")

            db.execSQL("ALTER TABLE track_points ADD COLUMN speedMps REAL")
            db.execSQL("ALTER TABLE track_points ADD COLUMN altitudeM REAL")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS friends (
                    phone TEXT NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(phone)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN steps INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN photoBeforeUri TEXT")
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN photoAfterUri TEXT")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN weatherTempC REAL")
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN weatherWindKmh REAL")
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN weatherCode INTEGER")
            db.execSQL("ALTER TABLE friends ADD COLUMN uid TEXT")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS leaderboard_snapshot (
                    month TEXT NOT NULL,
                    uid TEXT NOT NULL,
                    distanceKm REAL NOT NULL,
                    steps INTEGER NOT NULL,
                    sessions INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(month, uid)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_leaderboard_snapshot_month ON leaderboard_snapshot(month)")
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_sessions_userId ON activity_sessions(userId)")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS friends_new (
                    ownerUid TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    uid TEXT,
                    PRIMARY KEY(ownerUid, phone)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO friends_new(ownerUid, phone, name, createdAt, uid)
                SELECT '' as ownerUid, phone, name, createdAt, uid
                FROM friends
                """.trimIndent()
            )

            db.execSQL("DROP TABLE friends")
            db.execSQL("ALTER TABLE friends_new RENAME TO friends")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_friends_ownerUid ON friends(ownerUid)")
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE leaderboard_snapshot ADD COLUMN name TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_leaderboard_snapshot_uid ON leaderboard_snapshot(uid)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_leaderboard_snapshot_month ON leaderboard_snapshot(month)")
        }
    }

    // ✅ No-op para forçar upgrade e atualizar room_master_table identity hash
    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // sem alterações
        }
    }

    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE activity_sessions ADD COLUMN isPublic INTEGER NOT NULL DEFAULT 0")
        }
    }

    fun get(context: Context, userId: String? = null): FitTrackDb {
        val safeKey = userId
            ?.takeIf { it.isNotBlank() }
            ?.replace(Regex("[^a-zA-Z0-9_]"), "_")
            ?: "shared"

        return instances[safeKey] ?: synchronized(this) {
            instances[safeKey] ?: buildDb(context, safeKey).also { instances[safeKey] = it }
        }
    }

    private fun buildDb(context: Context, key: String): FitTrackDb {
        val dbName = if (key == "shared") "fittrack.db" else "fittrack_$key.db"

        return Room.databaseBuilder(
            context.applicationContext,
            FitTrackDb::class.java,
            dbName
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12
            )
            .build()
    }
}
