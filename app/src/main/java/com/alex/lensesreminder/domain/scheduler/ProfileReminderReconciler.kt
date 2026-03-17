package com.alex.lensesreminder.domain.scheduler

import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.time.LensClock
import com.alex.lensesreminder.data.repository.WearSessionRepository
import com.alex.lensesreminder.domain.session.computeFinalAlertTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reconciles the current session and daily reminder after profile edits.
 *
 * The wear-end timestamp of an already-started session stays fixed; profile wear-duration
 * changes only affect future sessions.
 */
@Singleton
class ProfileReminderReconciler @Inject constructor(
    private val wearSessionRepository: WearSessionRepository,
    private val reminderScheduleCoordinator: ReminderScheduleCoordinator,
    private val dailyStartReminderCoordinator: DailyStartReminderCoordinator,
    private val reminderNotificationPublisher: ReminderNotificationPublisher,
    private val clock: LensClock,
) {

    suspend fun reconcile(profile: LensProfile) {
        val currentSession = wearSessionRepository.getCurrentSession()
        if (currentSession != null) {
            if (!profile.remindersEnabled) {
                reminderScheduleCoordinator.clear(currentSession.id)
                reminderNotificationPublisher.cancelAll(currentSession.id)
            } else {
                reconcileSessionFinalAlert(profile, currentSession.id)
                wearSessionRepository.getSession(currentSession.id)
                    ?.takeIf { it.status in openStatuses }
                    ?.let { reminderScheduleCoordinator.sync(it) }
            }
        }

        dailyStartReminderCoordinator.sync()
    }

    private suspend fun reconcileSessionFinalAlert(
        profile: LensProfile,
        sessionId: Long,
    ) {
        val currentSession = wearSessionRepository.getSession(sessionId) ?: return
        if (currentSession.status !in setOf(SessionStatus.ACTIVE, SessionStatus.OVERDUE)) {
            return
        }

        val actualStartAt = currentSession.actualStartAt ?: return
        if (currentSession.finalAlertSentAt != null) {
            return
        }

        val updatedFinalAlert = computeFinalAlertTime(
            actualStartAt = actualStartAt,
            profile = profile,
            clock = clock
        )

        if (updatedFinalAlert == currentSession.finalAlertScheduledFor) {
            return
        }

        wearSessionRepository.updateFinalAlertSchedule(
            sessionId = currentSession.id,
            actualStartAt = actualStartAt,
            expectedFinalAlertScheduledFor = currentSession.finalAlertScheduledFor,
            updatedFinalAlertScheduledFor = updatedFinalAlert
        )
    }

    private companion object {
        val openStatuses = setOf(SessionStatus.PLANNED, SessionStatus.ACTIVE, SessionStatus.OVERDUE)
    }
}
