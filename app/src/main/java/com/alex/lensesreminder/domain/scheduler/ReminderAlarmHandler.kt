package com.alex.lensesreminder.domain.scheduler

import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.core.time.LensClock
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Handles fired alarms idempotently and advances the reminder chain.
 */
@Singleton
class ReminderAlarmHandler @Inject constructor(
    private val lensProfileRepository: LensProfileRepository,
    private val wearSessionRepository: WearSessionRepository,
    private val reminderScheduleCoordinator: ReminderScheduleCoordinator,
    private val dailyStartReminderCoordinator: DailyStartReminderCoordinator,
    private val reminderNotificationPublisher: ReminderNotificationPublisher,
    private val clock: LensClock,
) {

    suspend fun handle(
        sessionId: Long,
        type: ReminderAlarmType,
        scheduledAt: Instant,
    ) {
        if (type == ReminderAlarmType.DAILY_START && sessionId == DAILY_START_REMINDER_SESSION_ID) {
            handleDailyStart(scheduledAt)
            return
        }

        val session = wearSessionRepository.getSession(sessionId) ?: return
        val profile = lensProfileRepository.profile.first()

        when (type) {
            ReminderAlarmType.DAILY_START -> Unit
            ReminderAlarmType.PLANNED_START -> handlePlannedStart(session, scheduledAt)
            ReminderAlarmType.WEAR_END -> handleWearEnd(session, scheduledAt)
            ReminderAlarmType.OVERDUE_REPEAT -> handleOverdueRepeat(session, profile.repeatReminderMinutes, scheduledAt)
            ReminderAlarmType.FINAL_ALERT -> handleFinalAlert(session, scheduledAt)
        }
    }

    private suspend fun handleDailyStart(
        scheduledAt: Instant,
    ) {
        dailyStartReminderCoordinator.sync(skipToday = true)

        if (wearSessionRepository.getCurrentSession() != null) {
            return
        }

        reminderNotificationPublisher.show(
            WearSession(
                id = DAILY_START_REMINDER_SESSION_ID,
                lastReminderSentAt = scheduledAt
            ),
            ReminderAlarmType.DAILY_START
        )
    }

    private suspend fun handlePlannedStart(
        session: WearSession,
        scheduledAt: Instant,
    ) {
        if (
            session.status != SessionStatus.PLANNED ||
            session.plannedStartAt != scheduledAt ||
            session.lastReminderSentAt == scheduledAt
        ) {
            return
        }

        val updatedSession = session.copy(lastReminderSentAt = scheduledAt)
        wearSessionRepository.saveSession(updatedSession)
        reminderScheduleCoordinator.sync(updatedSession)
        reminderNotificationPublisher.show(updatedSession, ReminderAlarmType.PLANNED_START)
    }

    private suspend fun handleWearEnd(
        session: WearSession,
        scheduledAt: Instant,
    ) {
        if (
            session.expectedEndAt != scheduledAt ||
            session.completedAt != null ||
            session.finalAlertSentAt != null
        ) {
            return
        }

        if (shouldPromoteToFinalAlert(session)) {
            handleFinalAlert(session, session.finalAlertScheduledFor ?: scheduledAt)
            return
        }

        if (session.status == SessionStatus.OVERDUE && session.reminderCount > 0) {
            return
        }

        if (session.status != SessionStatus.ACTIVE && session.status != SessionStatus.OVERDUE) {
            return
        }

        val reminderSentAt = clock.now()
        val updatedSession = session.copy(
            status = SessionStatus.OVERDUE,
            lastReminderSentAt = reminderSentAt,
            reminderCount = 1
        )

        wearSessionRepository.saveSession(updatedSession)
        reminderScheduleCoordinator.sync(updatedSession)
        reminderNotificationPublisher.show(updatedSession, ReminderAlarmType.WEAR_END)
    }

    private suspend fun handleOverdueRepeat(
        session: WearSession,
        repeatReminderMinutes: Int,
        scheduledAt: Instant,
    ) {
        if (
            session.status != SessionStatus.OVERDUE ||
            session.completedAt != null ||
            session.finalAlertSentAt != null
        ) {
            return
        }

        val expectedScheduledAt = session.lastReminderSentAt
            ?.plusSeconds(repeatReminderMinutes.toLong() * 60)
            ?: return

        if (scheduledAt != expectedScheduledAt) {
            return
        }

        if (shouldPromoteToFinalAlert(session)) {
            handleFinalAlert(session, session.finalAlertScheduledFor ?: scheduledAt)
            return
        }

        val reminderSentAt = clock.now()
        val updatedSession = session.copy(
            lastReminderSentAt = reminderSentAt,
            reminderCount = session.reminderCount + 1
        )

        wearSessionRepository.saveSession(updatedSession)
        reminderScheduleCoordinator.sync(updatedSession)
        reminderNotificationPublisher.show(updatedSession, ReminderAlarmType.OVERDUE_REPEAT)
    }

    private suspend fun handleFinalAlert(
        session: WearSession,
        scheduledAt: Instant,
    ) {
        if (
            session.completedAt != null ||
            session.finalAlertScheduledFor != scheduledAt ||
            session.finalAlertSentAt != null ||
            (session.status != SessionStatus.ACTIVE && session.status != SessionStatus.OVERDUE)
        ) {
            return
        }

        val updatedSession = session.copy(finalAlertSentAt = clock.now())
        wearSessionRepository.saveSession(updatedSession)
        reminderScheduleCoordinator.sync(updatedSession)
        reminderNotificationPublisher.show(updatedSession, ReminderAlarmType.FINAL_ALERT)
    }

    private fun shouldPromoteToFinalAlert(session: WearSession): Boolean {
        val finalAlertAt = session.finalAlertScheduledFor ?: return false
        return session.finalAlertSentAt == null && !finalAlertAt.isAfter(clock.now())
    }
}
