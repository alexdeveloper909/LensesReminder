package com.alex.lensesreminder.domain.scheduler

import java.time.Instant

/**
 * Schedules and cancels reminder alarms for a session.
 */
interface ReminderAlarmScheduler {
    fun schedule(
        sessionId: Long,
        type: ReminderAlarmType,
        triggerAt: Instant,
    )

    fun cancel(
        sessionId: Long,
        type: ReminderAlarmType,
    )

    fun cancelAll(sessionId: Long)
}
