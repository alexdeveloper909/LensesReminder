package com.alex.lensesreminder.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * DAO for reading and storing wear sessions.
 */
@Dao
interface WearSessionDao {

    @Query(
        """
        SELECT * FROM wear_sessions
        WHERE status IN ('PLANNED', 'ACTIVE', 'OVERDUE')
        ORDER BY COALESCE(actual_start_at_utc, planned_start_at_utc) DESC
        LIMIT 1
        """
    )
    fun observeCurrentSession(): Flow<WearSessionEntity?>

    @Query(
        """
        SELECT * FROM wear_sessions
        WHERE id = :sessionId
        LIMIT 1
        """
    )
    suspend fun getSessionById(sessionId: Long): WearSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: WearSessionEntity): Long

    @Query(
        """
        UPDATE wear_sessions
        SET last_reminder_sent_at_utc = :reminderSentAt
        WHERE id = :sessionId
          AND status = 'PLANNED'
          AND planned_start_at_utc = :plannedStartAt
          AND completed_at_utc IS NULL
          AND (last_reminder_sent_at_utc IS NULL OR last_reminder_sent_at_utc != :reminderSentAt)
        """
    )
    suspend fun markPlannedReminderSent(
        sessionId: Long,
        plannedStartAt: Instant,
        reminderSentAt: Instant,
    ): Int

    @Query(
        """
        UPDATE wear_sessions
        SET status = 'OVERDUE',
            last_reminder_sent_at_utc = :reminderSentAt,
            reminder_count = 1
        WHERE id = :sessionId
          AND expected_end_at_utc = :expectedEndAt
          AND completed_at_utc IS NULL
          AND final_alert_sent_at_utc IS NULL
          AND (status = 'ACTIVE' OR (status = 'OVERDUE' AND reminder_count = 0))
        """
    )
    suspend fun markWearEndTriggered(
        sessionId: Long,
        expectedEndAt: Instant,
        reminderSentAt: Instant,
    ): Int

    @Query(
        """
        UPDATE wear_sessions
        SET last_reminder_sent_at_utc = :reminderSentAt,
            reminder_count = reminder_count + 1
        WHERE id = :sessionId
          AND status = 'OVERDUE'
          AND completed_at_utc IS NULL
          AND final_alert_sent_at_utc IS NULL
          AND last_reminder_sent_at_utc = :previousReminderSentAt
        """
    )
    suspend fun markOverdueReminderSent(
        sessionId: Long,
        previousReminderSentAt: Instant,
        reminderSentAt: Instant,
    ): Int

    @Query(
        """
        UPDATE wear_sessions
        SET final_alert_sent_at_utc = :sentAt
        WHERE id = :sessionId
          AND completed_at_utc IS NULL
          AND final_alert_scheduled_for_utc = :scheduledAt
          AND final_alert_sent_at_utc IS NULL
          AND status IN ('ACTIVE', 'OVERDUE')
        """
    )
    suspend fun markFinalAlertSent(
        sessionId: Long,
        scheduledAt: Instant,
        sentAt: Instant,
    ): Int

    @Query(
        """
        UPDATE wear_sessions
        SET actual_start_at_utc = :actualStartAt,
            expected_end_at_utc = :expectedEndAt,
            completed_at_utc = NULL,
            status = 'ACTIVE',
            source = :source,
            final_alert_scheduled_for_utc = :finalAlertScheduledFor,
            final_alert_sent_at_utc = NULL,
            last_reminder_sent_at_utc = NULL,
            reminder_count = 0
        WHERE id = :sessionId
          AND status = 'PLANNED'
        """
    )
    suspend fun activatePlannedSession(
        sessionId: Long,
        source: String,
        actualStartAt: Instant,
        expectedEndAt: Instant,
        finalAlertScheduledFor: Instant?,
    ): Int

    @Query(
        """
        UPDATE wear_sessions
        SET completed_at_utc = :completedAt,
            status = 'COMPLETED'
        WHERE id = :sessionId
          AND status IN ('ACTIVE', 'OVERDUE')
          AND completed_at_utc IS NULL
        """
    )
    suspend fun completeSession(
        sessionId: Long,
        completedAt: Instant,
    ): Int

    @Query(
        """
        UPDATE wear_sessions
        SET status = 'CANCELLED'
        WHERE id = :sessionId
          AND status = 'PLANNED'
        """
    )
    suspend fun cancelPlannedSession(sessionId: Long): Int

    @Query(
        """
        UPDATE wear_sessions
        SET planned_start_at_utc = :plannedStartAt,
            last_reminder_sent_at_utc = NULL
        WHERE id = :sessionId
          AND status = 'PLANNED'
        """
    )
    suspend fun snoozePlannedSession(
        sessionId: Long,
        plannedStartAt: Instant,
    ): Int

    @Query(
        """
        UPDATE wear_sessions
        SET status = 'OVERDUE'
        WHERE id = :sessionId
          AND status = 'ACTIVE'
          AND expected_end_at_utc = :expectedEndAt
        """
    )
    suspend fun markActiveSessionOverdue(
        sessionId: Long,
        expectedEndAt: Instant,
    ): Int

    @Query(
        """
        UPDATE wear_sessions
        SET final_alert_scheduled_for_utc = :updatedFinalAlertScheduledFor
        WHERE id = :sessionId
          AND status IN ('ACTIVE', 'OVERDUE')
          AND actual_start_at_utc = :actualStartAt
          AND final_alert_sent_at_utc IS NULL
          AND final_alert_scheduled_for_utc IS :expectedFinalAlertScheduledFor
        """
    )
    suspend fun updateFinalAlertSchedule(
        sessionId: Long,
        actualStartAt: Instant,
        expectedFinalAlertScheduledFor: Instant?,
        updatedFinalAlertScheduledFor: Instant?,
    ): Int
}
