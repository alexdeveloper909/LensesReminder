package com.alex.lensesreminder.domain.session

import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.SessionSource
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.core.time.LensClock
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
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
    private val clock: LensClock,
) {

    suspend fun startNow(): SessionLifecycleResult<WearSession> {
        val currentSession = wearSessionRepository.getCurrentSession()
        if (currentSession != null) {
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.EXISTING_OPEN_SESSION)
        }

        val profile = lensProfileRepository.profile.first()
        val actualStartAt = clock.now()
        val session = WearSession(
            actualStartAt = actualStartAt,
            expectedEndAt = actualStartAt.plus(Duration.ofMinutes(profile.maxWearMinutes.toLong())),
            status = SessionStatus.ACTIVE,
            source = SessionSource.MANUAL_START,
            finalAlertScheduledFor = computeFinalAlert(
                actualStartAt = actualStartAt,
                profile = profile
            )
        )

        val sessionId = wearSessionRepository.saveSession(session)
        return SessionLifecycleResult.Success(session.copy(id = sessionId))
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
            status = SessionStatus.PLANNED,
            source = SessionSource.PLANNED
        )

        val sessionId = wearSessionRepository.saveSession(plannedSession)
        return SessionLifecycleResult.Success(plannedSession.copy(id = sessionId))
    }

    suspend fun activatePlannedSession(): SessionLifecycleResult<WearSession> {
        val plannedSession = wearSessionRepository.getCurrentSession()
        if (plannedSession?.status != SessionStatus.PLANNED) {
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.PLANNED_SESSION_NOT_FOUND)
        }

        val profile = lensProfileRepository.profile.first()
        val actualStartAt = clock.now()
        val activeSession = plannedSession.copy(
            actualStartAt = actualStartAt,
            expectedEndAt = actualStartAt.plus(Duration.ofMinutes(profile.maxWearMinutes.toLong())),
            status = SessionStatus.ACTIVE,
            finalAlertScheduledFor = computeFinalAlert(
                actualStartAt = actualStartAt,
                profile = profile
            )
        )

        wearSessionRepository.saveSession(activeSession)
        return SessionLifecycleResult.Success(activeSession)
    }

    suspend fun completeCurrentSession(): SessionLifecycleResult<WearSession> {
        val currentSession = wearSessionRepository.getCurrentSession()
        if (currentSession == null || currentSession.status !in completableStatuses) {
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.ACTIVE_SESSION_NOT_FOUND)
        }

        val completedSession = currentSession.copy(
            completedAt = clock.now(),
            status = SessionStatus.COMPLETED
        )
        wearSessionRepository.saveSession(completedSession)
        return SessionLifecycleResult.Success(completedSession)
    }

    suspend fun cancelPlannedSession(): SessionLifecycleResult<WearSession> {
        val plannedSession = wearSessionRepository.getCurrentSession()
        if (plannedSession?.status != SessionStatus.PLANNED) {
            return SessionLifecycleResult.Failure(SessionLifecycleFailure.PLANNED_SESSION_NOT_FOUND)
        }

        val cancelledSession = plannedSession.copy(status = SessionStatus.CANCELLED)
        wearSessionRepository.saveSession(cancelledSession)
        return SessionLifecycleResult.Success(cancelledSession)
    }

    suspend fun refreshCurrentSessionStatus(): WearSession? {
        val currentSession = wearSessionRepository.getCurrentSession() ?: return null
        val expectedEndAt = currentSession.expectedEndAt ?: return currentSession

        if (
            currentSession.status == SessionStatus.ACTIVE &&
            !clock.now().isBefore(expectedEndAt)
        ) {
            val overdueSession = currentSession.copy(status = SessionStatus.OVERDUE)
            wearSessionRepository.saveSession(overdueSession)
            return overdueSession
        }

        return currentSession
    }

    private fun computeFinalAlert(
        actualStartAt: Instant,
        profile: LensProfile,
    ): Instant? {
        if (!profile.remindersEnabled) {
            return null
        }

        val zoneId = clock.zoneId()
        val startDate = actualStartAt.atZone(zoneId).toLocalDate()
        val candidate = startDate.atTime(profile.finalAlertTime).atZone(zoneId).toInstant()
        return candidate.takeIf { it.isAfter(actualStartAt) }
    }

    private companion object {
        val completableStatuses = setOf(SessionStatus.ACTIVE, SessionStatus.OVERDUE)
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
    INVALID_PLANNED_TIME,
    PLANNED_SESSION_NOT_FOUND,
    ACTIVE_SESSION_NOT_FOUND,
}
