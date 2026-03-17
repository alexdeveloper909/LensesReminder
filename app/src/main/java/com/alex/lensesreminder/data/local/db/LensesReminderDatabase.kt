package com.alex.lensesreminder.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Main local database for profiles and wear sessions.
 */
@Database(
    entities = [
        LensProfileEntity::class,
        WearSessionEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(RoomTypeConverters::class)
abstract class LensesReminderDatabase : RoomDatabase() {
    abstract fun lensProfileDao(): LensProfileDao
    abstract fun wearSessionDao(): WearSessionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE lens_profiles
                    ADD COLUMN daily_start_reminder_time TEXT NOT NULL DEFAULT '08:00'
                    """.trimIndent()
                )
            }
        }
    }
}
