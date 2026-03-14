package com.alex.lensesreminder.testutil

import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.domain.scheduler.ReminderAlarmScheduler
import com.alex.lensesreminder.domain.scheduler.ReminderAlarmType
import com.alex.lensesreminder.domain.scheduler.ReminderNotificationPublisher
import java.time.Instant

data class ScheduledAlarm(
    val sessionId: Long,
    val type: ReminderAlarmType,
    val triggerAt: Instant,
)

/**
 * In-memory reminder scheduler used by unit tests.
 */
class FakeReminderAlarmScheduler : ReminderAlarmScheduler {
    val scheduledAlarms = mutableListOf<ScheduledAlarm>()
    val cancelledSessionIds = mutableListOf<Long>()

    override fun schedule(
        sessionId: Long,
        type: ReminderAlarmType,
        triggerAt: Instant,
    ) {
        scheduledAlarms.removeAll { it.sessionId == sessionId && it.type == type }
        scheduledAlarms += ScheduledAlarm(sessionId, type, triggerAt)
    }

    override fun cancel(
        sessionId: Long,
        type: ReminderAlarmType,
    ) {
        scheduledAlarms.removeAll { it.sessionId == sessionId && it.type == type }
    }

    override fun cancelAll(sessionId: Long) {
        cancelledSessionIds += sessionId
        scheduledAlarms.removeAll { it.sessionId == sessionId }
    }
}

/**
 * In-memory notification publisher used by unit tests.
 */
class FakeReminderNotificationPublisher : ReminderNotificationPublisher {
    val shownNotifications = mutableListOf<Pair<Long, ReminderAlarmType>>()
    val cancelledSessionIds = mutableListOf<Long>()

    override fun show(
        session: WearSession,
        type: ReminderAlarmType,
    ) {
        shownNotifications += session.id to type
    }

    override fun cancelAll(sessionId: Long) {
        cancelledSessionIds += sessionId
    }
}
