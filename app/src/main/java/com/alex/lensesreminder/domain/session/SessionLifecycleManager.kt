package com.alex.lensesreminder.domain.session

import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.SessionSource
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.core.time.LensClock
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
import com.alex.lensesreminder.domain.scheduler.DAILY_START_REMINDER_SESSION_ID
import com.alex.lensesreminder.domain.scheduler.DailyStartReminderCoordinator
import com.alex.lensesreminder.domain.scheduler.ReminderNotificationPublisher
import com.alex.lensesreminder.domain.scheduler.ReminderScheduleCoordinator
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Implements the Phase 2 session lifecycle rules on top of the repositories.
 */
@Singleton
class SessionLifecycleManager @Inject constructor(
    private val lensProfileRepository: LensProfileRepository,
    private val wearSessionRepository: WearSessionRepository,
    private val reminderScheduleCoordinator: ReminderScheduleCoordinator,
    private val dailyStartReminderCoordinator: DailyStartReminderCoordinator,
    private val reminderNotificationPublisher: ReminderNotificationPublisher,
    private val clock: LensClock,
) {

    suspend fun startNow(): SessionLifecycleResult<WearSession> {
        return startAt(clock.now())
    }

    suspend fun startAt(
        actualStartAt: Instant,
    ): SessionLifecycleResult<WearSession> {
        val currentSession = wearSessionRepository.getCurrentSession()
        if (currentSession != null) {
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.EXISTING_OPEN_SESSION)
        }
        if (actualStartAt.isAfter(clock.now())) {
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.INVALID_ACTUAL_START)
        }

        val profile = lensProfileRepository.profile.first()
        val expectedEndAt = actualStartAt.plus(Duration.ofMinutes(profile.maxWearMinutes.toLong()))
        val session = WearSession(
            actualStartAt = actualStartAt,
            expectedEndAt = expectedEndAt,
            status = if (expectedEndAt.isAfter(clock.now())) {
                SessionStatus.ACTIVE
            } else {
                SessionStatus.OVERDUE
            },
            source = SessionSource.MANUAL_START,
            finalAlertScheduledFor = computeFinalAlertTime(
                actualStartAt = actualStartAt,
                profile = profile,
                clock = clock
            )
        )

        val sessionId = wearSessionRepository.saveSession(session)
        val savedSession = session.copy(id = sessionId)
        reminderScheduleCoordinator.sync(savedSession)
        clearStartReminderNotifications(savedSession.id)
        dailyStartReminderCoordinator.sync(skipToday = true)
        return SessionLifecycleResult.Success(savedSession)
    }

    suspend fun savePlannedSession(
        plannedStartAt: Instant,
    ): SessionLifecycleResult<WearSession> {
        if (!plannedStartAt.isAfter(clock.now())) {
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.INVALID_PLANNED_TIME)
        }

        val currentSession = wearSessionRepository.getCurrentSession()
        if (currentSession != null && currentSession.status != SessionStatus.PLANNED) {
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.EXISTING_OPEN_SESSION)
        }

        val plannedSession = WearSession(
            id = currentSession?.id ?: 0,
            plannedStartAt = plannedStartAt,
            actualStartAt = null,
            expectedEndAt = null,
            completedAt = null,
            status = SessionStatus.PLANNED,
            source = SessionSource.PLANNED,
            finalAlertScheduledFor = null,
            finalAlertSentAt = null,
            lastReminderSentAt = null,
            reminderCount = 0
        )

        val sessionId = wearSessionRepository.saveSession(plannedSession)
        val savedSession = plannedSession.copy(id = sessionId)
        reminderScheduleCoordinator.sync(savedSession)
        reminderNotificationPublisher.cancelAll(savedSession.id)
        dailyStartReminderCoordinator.sync(skipToday = true)
        return SessionLifecycleResult.Success(savedSession)
    }

    suspend fun activatePlannedSession(sessionId: Long? = null): SessionLifecycleResult<WearSession> {
        val plannedSession = wearSessionRepository.getCurrentSession()
        if (
            plannedSession?.status != SessionStatus.PLANNED ||
            (sessionId != null && plannedSession.id != sessionId)
        ) {
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.PLANNED_SESSION_NOT_FOUND)
        }

        val profile = lensProfileRepository.profile.first()
        val actualStartAt = clock.now()
        val activeSession = plannedSession.copy(
            actualStartAt = actualStartAt,
            expectedEndAt = actualStartAt.plus(Duration.ofMinutes(profile.maxWearMinutes.toLong())),
            status = SessionStatus.ACTIVE,
            finalAlertScheduledFor = computeFinalAlertTime(
                actualStartAt = actualStartAt,
                profile = profile,
                clock = clock
            ),
            finalAlertSentAt = null,
            lastReminderSentAt = null,
            reminderCount = 0
        )

        val didActivate = wearSessionRepository.activatePlannedSession(
            sessionId = plannedSession.id,
            source = activeSession.source.name,
            actualStartAt = actualStartAt,
            expectedEndAt = activeSession.expectedEndAt ?: return SessionLifecycleResult.Failure(
                SessionLifecycleFailure.PLANNED_SESSION_NOT_FOUND
            ),
            finalAlertScheduledFor = activeSession.finalAlertScheduledFor
        )
        if (!didActivate) {
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.PLANNED_SESSION_NOT_FOUND)
        }

        reminderScheduleCoordinator.sync(activeSession)
        clearStartReminderNotifications(activeSession.id)
        dailyStartReminderCoordinator.sync(skipToday = true)
        return SessionLifecycleResult.Success(activeSession)
    }

    suspend fun completeCurrentSession(sessionId: Long? = null): SessionLifecycleResult<WearSession> {
        val currentSession = wearSessionRepository.getCurrentSession()
        if (
            currentSession == null ||
            currentSession.status !in completableStatuses ||
            (sessionId != null && currentSession.id != sessionId)
        ) {
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.ACTIVE_SESSION_NOT_FOUND)
        }

        val completedSession = currentSession.copy(
            completedAt = clock.now(),
            status = SessionStatus.COMPLETED
        )
        val didComplete = wearSessionRepository.completeSession(
            sessionId = completedSession.id,
            completedAt = completedSession.completedAt ?: return SessionLifecycleResult.Failure(
                SessionLifecycleFailure.ACTIVE_SESSION_NOT_FOUND
            )
        )
        if (!didComplete) {
            wearSessionRepository.getSession(completedSession.id)
                ?.takeIf { it.completedAt != null }
                ?.let { return SessionLifecycleResult.Success(it) }
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.ACTIVE_SESSION_NOT_FOUND)
        }

        reminderScheduleCoordinator.clear(completedSession.id)
        reminderNotificationPublisher.cancelAll(completedSession.id)
        dailyStartReminderCoordinator.sync(skipToday = true)
        return SessionLifecycleResult.Success(completedSession)
    }

    suspend fun cancelPlannedSession(sessionId: Long? = null): SessionLifecycleResult<WearSession> {
        val plannedSession = wearSessionRepository.getCurrentSession()
        if (
            plannedSession?.status != SessionStatus.PLANNED ||
            (sessionId != null && plannedSession.id != sessionId)
        ) {
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.PLANNED_SESSION_NOT_FOUND)
        }

        val cancelledSession = plannedSession.copy(status = SessionStatus.CANCELLED)
        val didCancel = wearSessionRepository.cancelPlannedSession(cancelledSession.id)
        if (!didCancel) {
            wearSessionRepository.getSession(cancelledSession.id)
                ?.takeIf { it.status == SessionStatus.CANCELLED }
                ?.let { return SessionLifecycleResult.Success(it) }
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.PLANNED_SESSION_NOT_FOUND)
        }

        reminderScheduleCoordinator.clear(cancelledSession.id)
        reminderNotificationPublisher.cancelAll(cancelledSession.id)
        dailyStartReminderCoordinator.sync()
        return SessionLifecycleResult.Success(cancelledSession)
    }

    suspend fun snoozePlannedSession(sessionId: Long? = null): SessionLifecycleResult<WearSession> {
        val plannedSession = wearSessionRepository.getCurrentSession()
        if (
            plannedSession?.status != SessionStatus.PLANNED ||
            (sessionId != null && plannedSession.id != sessionId)
        ) {
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.PLANNED_SESSION_NOT_FOUND)
        }

        val baseline = maxOf(clock.now(), plannedSession.plannedStartAt ?: clock.now())
        val snoozedSession = plannedSession.copy(
            plannedStartAt = baseline.plus(Duration.ofMinutes(SNOOZE_MINUTES.toLong())),
            lastReminderSentAt = null
        )
        val updatedPlannedStartAt = snoozedSession.plannedStartAt
            ?: return SessionLifecycleResult.Failure(SessionLifecycleFailure.PLANNED_SESSION_NOT_FOUND)
        val didSnooze = wearSessionRepository.snoozePlannedSession(
            sessionId = snoozedSession.id,
            plannedStartAt = updatedPlannedStartAt
        )
        if (!didSnooze) {
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.PLANNED_SESSION_NOT_FOUND)
        }

        reminderScheduleCoordinator.sync(snoozedSession)
        reminderNotificationPublisher.cancelAll(snoozedSession.id)
        dailyStartReminderCoordinator.sync(skipToday = true)
        return SessionLifecycleResult.Success(snoozedSession)
    }

    suspend fun refreshCurrentSessionStatus(): WearSession? {
        val currentSession = wearSessionRepository.getCurrentSession() ?: return null
        val expectedEndAt = currentSession.expectedEndAt ?: return currentSession

        if (
            currentSession.status == SessionStatus.ACTIVE &&
            !clock.now().isBefore(expectedEndAt)
        ) {
            val overdueSession = currentSession.copy(status = SessionStatus.OVERDUE)
            val didMarkOverdue = wearSessionRepository.markActiveSessionOverdue(
                sessionId = overdueSession.id,
                expectedEndAt = expectedEndAt
            )
            if (!didMarkOverdue) {
                return wearSessionRepository.getSession(currentSession.id) ?: currentSession
            }
            reminderScheduleCoordinator.sync(overdueSession)
            dailyStartReminderCoordinator.sync(skipToday = true)
            return overdueSession
        }

        return currentSession
    }

    private companion object {
        val completableStatuses = setOf(SessionStatus.ACTIVE, SessionStatus.OVERDUE)
        const val SNOOZE_MINUTES = 15
    }

    private fun clearStartReminderNotifications(sessionId: Long) {
        reminderNotificationPublisher.cancelAll(sessionId)
        reminderNotificationPublisher.cancelAll(DAILY_START_REMINDER_SESSION_ID)
    }
}

sealed interface SessionLifecycleResult<out T> {
    data class Success<T>(
        val value: T,
    ) : SessionLifecycleResult<T>

    data class Failure(
        val reason: SessionLifecycleFailure,
    ) : SessionLifecycleResult<Nothing>
}

enum class SessionLifecycleFailure {
    EXISTING_OPEN_SESSION,
    INVALID_ACTUAL_START,
    INVALID_PLANNED_TIME,
    PLANNED_SESSION_NOT_FOUND,
    ACTIVE_SESSION_NOT_FOUND,
}
