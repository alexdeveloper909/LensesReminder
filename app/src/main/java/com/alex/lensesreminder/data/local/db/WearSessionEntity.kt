package com.alex.lensesreminder.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity for session history and the currently open session.
 */
@Entity(tableName = "wear_sessions")
data class WearSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "planned_start_at_utc")
    val plannedStartAt: Instant? = null,
    @ColumnInfo(name = "actual_start_at_utc")
    val actualStartAt: Instant? = null,
    @ColumnInfo(name = "expected_end_at_utc")
    val expectedEndAt: Instant? = null,
    @ColumnInfo(name = "completed_at_utc")
    val completedAt: Instant? = null,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "final_alert_scheduled_for_utc")
    val finalAlertScheduledFor: Instant? = null,
    @ColumnInfo(name = "final_alert_sent_at_utc")
    val finalAlertSentAt: Instant? = null,
    @ColumnInfo(name = "last_reminder_sent_at_utc")
    val lastReminderSentAt: Instant? = null,
    @ColumnInfo(name = "reminder_count")
    val reminderCount: Int = 0,
)
