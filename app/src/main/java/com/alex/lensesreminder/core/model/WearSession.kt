package com.alex.lensesreminder.core.model

import java.time.Instant

/**
 * Domain model for a single lens wear session.
 */
data class WearSession(
    val id: Long = 0,
    val plannedStartAt: Instant? = null,
    val actualStartAt: Instant? = null,
    val expectedEndAt: Instant? = null,
    val completedAt: Instant? = null,
    val status: SessionStatus = SessionStatus.PLANNED,
    val source: SessionSource = SessionSource.MANUAL_START,
    val finalAlertScheduledFor: Instant? = null,
    val finalAlertSentAt: Instant? = null,
    val lastReminderSentAt: Instant? = null,
    val reminderCount: Int = 0,
)
