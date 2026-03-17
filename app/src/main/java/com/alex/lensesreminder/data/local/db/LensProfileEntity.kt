package com.alex.lensesreminder.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

/**
 * Room representation of the single active lens profile.
 */
@Entity(tableName = "lens_profiles")
data class LensProfileEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "lens_type")
    val lensType: String,
    @ColumnInfo(name = "max_wear_minutes")
    val maxWearMinutes: Int,
    @ColumnInfo(name = "reminders_enabled")
    val remindersEnabled: Boolean,
    @ColumnInfo(name = "final_alert_time")
    val finalAlertTime: LocalTime,
    @ColumnInfo(name = "daily_start_reminder_time")
    val dailyStartReminderTime: LocalTime,
    @ColumnInfo(name = "repeat_reminder_minutes")
    val repeatReminderMinutes: Int,
)
