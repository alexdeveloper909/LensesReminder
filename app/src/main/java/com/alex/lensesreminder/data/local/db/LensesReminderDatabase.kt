package com.alex.lensesreminder.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Main local database for profiles and wear sessions.
 */
@Database(
    entities = [
        LensProfileEntity::class,
        WearSessionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomTypeConverters::class)
abstract class LensesReminderDatabase : RoomDatabase() {
    abstract fun lensProfileDao(): LensProfileDao
    abstract fun wearSessionDao(): WearSessionDao
}
