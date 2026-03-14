package com.alex.lensesreminder.domain.scheduler

import com.alex.lensesreminder.core.model.WearSession

/**
 * Posts and clears reminder notifications independently from scheduling.
 */
interface ReminderNotificationPublisher {
    fun show(
        session: WearSession,
        type: ReminderAlarmType,
    )

    fun cancelAll(sessionId: Long)
}
