package com.alex.lensesreminder.testutil

import com.alex.lensesreminder.data.local.db.LensProfileDao
import com.alex.lensesreminder.data.local.db.LensProfileEntity
import com.alex.lensesreminder.data.local.db.WearSessionDao
import com.alex.lensesreminder.data.local.db.WearSessionEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory DAO implementation used by repository and ViewModel unit tests.
 */
class FakeLensProfileDao : LensProfileDao {
    private val profileState = MutableStateFlow<LensProfileEntity?>(null)

    override fun observeProfile(): Flow<LensProfileEntity?> = profileState

    override suspend fun upsert(profile: LensProfileEntity) {
        profileState.value = profile
    }
}

/**
 * In-memory DAO implementation used by repository and ViewModel unit tests.
 */
class FakeWearSessionDao : WearSessionDao {
    private val sessionState = MutableStateFlow<WearSessionEntity?>(null)
    private var nextId = 1L

    override fun observeCurrentSession(): Flow<WearSessionEntity?> = sessionState.map { session ->
        session?.takeIf {
            it.status == "PLANNED" || it.status == "ACTIVE" || it.status == "OVERDUE"
        }
    }

    override suspend fun getSessionById(sessionId: Long): WearSessionEntity? {
        return sessionState.value?.takeIf { it.id == sessionId }
    }

    override suspend fun upsert(session: WearSessionEntity): Long {
        val resolvedId = if (session.id == 0L) nextId++ else session.id
        sessionState.value = session.copy(id = resolvedId)
        return resolvedId
    }

    override suspend fun markPlannedReminderSent(
        sessionId: Long,
        plannedStartAt: Instant,
        reminderSentAt: Instant,
    ): Int = updateSession(sessionId) { session ->
        if (
            session.status == "PLANNED" &&
            session.plannedStartAt == plannedStartAt &&
            session.completedAt == null &&
            session.lastReminderSentAt != reminderSentAt
        ) {
            session.copy(lastReminderSentAt = reminderSentAt)
        } else {
            null
        }
    }

    override suspend fun markWearEndTriggered(
        sessionId: Long,
        expectedEndAt: Instant,
        reminderSentAt: Instant,
    ): Int = updateSession(sessionId) { session ->
        if (
            session.expectedEndAt == expectedEndAt &&
            session.completedAt == null &&
            session.finalAlertSentAt == null &&
            (session.status == "ACTIVE" || (session.status == "OVERDUE" && session.reminderCount == 0))
        ) {
            session.copy(
                status = "OVERDUE",
                lastReminderSentAt = reminderSentAt,
                reminderCount = 1
            )
        } else {
            null
        }
    }

    override suspend fun markOverdueReminderSent(
        sessionId: Long,
        previousReminderSentAt: Instant,
        reminderSentAt: Instant,
    ): Int = updateSession(sessionId) { session ->
        if (
            session.status == "OVERDUE" &&
            session.completedAt == null &&
            session.finalAlertSentAt == null &&
            session.lastReminderSentAt == previousReminderSentAt
        ) {
            session.copy(
                lastReminderSentAt = reminderSentAt,
                reminderCount = session.reminderCount + 1
            )
        } else {
            null
        }
    }

    override suspend fun markFinalAlertSent(
        sessionId: Long,
        scheduledAt: Instant,
        sentAt: Instant,
    ): Int = updateSession(sessionId) { session ->
        if (
            session.completedAt == null &&
            session.finalAlertScheduledFor == scheduledAt &&
            session.finalAlertSentAt == null &&
            (session.status == "ACTIVE" || session.status == "OVERDUE")
        ) {
            session.copy(finalAlertSentAt = sentAt)
        } else {
            null
        }
    }

    override suspend fun activatePlannedSession(
        sessionId: Long,
        source: String,
        actualStartAt: Instant,
        expectedEndAt: Instant,
        finalAlertScheduledFor: Instant?,
    ): Int = updateSession(sessionId) { session ->
        if (session.status == "PLANNED") {
            session.copy(
                actualStartAt = actualStartAt,
                expectedEndAt = expectedEndAt,
                completedAt = null,
                status = "ACTIVE",
                source = source,
                finalAlertScheduledFor = finalAlertScheduledFor,
                finalAlertSentAt = null,
                lastReminderSentAt = null,
                reminderCount = 0
            )
        } else {
            null
        }
    }

    override suspend fun completeSession(
        sessionId: Long,
        completedAt: Instant,
    ): Int = updateSession(sessionId) { session ->
        if (
            (session.status == "ACTIVE" || session.status == "OVERDUE") &&
            session.completedAt == null
        ) {
            session.copy(
                completedAt = completedAt,
                status = "COMPLETED"
            )
        } else {
            null
        }
    }

    override suspend fun cancelPlannedSession(sessionId: Long): Int = updateSession(sessionId) { session ->
        if (session.status == "PLANNED") {
            session.copy(status = "CANCELLED")
        } else {
            null
        }
    }

    override suspend fun snoozePlannedSession(
        sessionId: Long,
        plannedStartAt: Instant,
    ): Int = updateSession(sessionId) { session ->
        if (session.status == "PLANNED") {
            session.copy(
                plannedStartAt = plannedStartAt,
                lastReminderSentAt = null
            )
        } else {
            null
        }
    }

    override suspend fun markActiveSessionOverdue(
        sessionId: Long,
        expectedEndAt: Instant,
    ): Int = updateSession(sessionId) { session ->
        if (session.status == "ACTIVE" && session.expectedEndAt == expectedEndAt) {
            session.copy(status = "OVERDUE")
        } else {
            null
        }
    }

    override suspend fun updateFinalAlertSchedule(
        sessionId: Long,
        actualStartAt: Instant,
        expectedFinalAlertScheduledFor: Instant?,
        updatedFinalAlertScheduledFor: Instant?,
    ): Int = updateSession(sessionId) { session ->
        if (
            (session.status == "ACTIVE" || session.status == "OVERDUE") &&
            session.actualStartAt == actualStartAt &&
            session.finalAlertSentAt == null &&
            session.finalAlertScheduledFor == expectedFinalAlertScheduledFor
        ) {
            session.copy(finalAlertScheduledFor = updatedFinalAlertScheduledFor)
        } else {
            null
        }
    }

    private fun updateSession(
        sessionId: Long,
        block: (WearSessionEntity) -> WearSessionEntity?,
    ): Int {
        val currentSession = sessionState.value ?: return 0
        if (currentSession.id != sessionId) {
            return 0
        }

        val updatedSession = block(currentSession) ?: return 0
        sessionState.value = updatedSession
        return 1
    }
}
