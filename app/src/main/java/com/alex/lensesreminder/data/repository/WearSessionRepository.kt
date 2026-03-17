package com.alex.lensesreminder.data.repository

import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.data.local.db.WearSessionDao
import com.alex.lensesreminder.data.local.db.toDomain
import com.alex.lensesreminder.data.local.db.toEntity
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository for the current session and session history.
 */
@Singleton
class WearSessionRepository @Inject constructor(
    private val wearSessionDao: WearSessionDao,
) {

    val currentSession: Flow<WearSession?> = wearSessionDao.observeCurrentSession()
        .map { entity -> entity?.toDomain() }

    suspend fun getCurrentSession(): WearSession? = currentSession.first()

    suspend fun getSession(sessionId: Long): WearSession? = wearSessionDao.getSessionById(sessionId)?.toDomain()

    suspend fun saveSession(session: WearSession): Long = wearSessionDao.upsert(session.toEntity())

    suspend fun markPlannedReminderSent(
        sessionId: Long,
        plannedStartAt: Instant,
        reminderSentAt: Instant,
    ): Boolean = wearSessionDao.markPlannedReminderSent(
        sessionId = sessionId,
        plannedStartAt = plannedStartAt,
        reminderSentAt = reminderSentAt
    ) > 0

    suspend fun markWearEndTriggered(
        sessionId: Long,
        expectedEndAt: Instant,
        reminderSentAt: Instant,
    ): Boolean = wearSessionDao.markWearEndTriggered(
        sessionId = sessionId,
        expectedEndAt = expectedEndAt,
        reminderSentAt = reminderSentAt
    ) > 0

    suspend fun markOverdueReminderSent(
        sessionId: Long,
        previousReminderSentAt: Instant,
        reminderSentAt: Instant,
    ): Boolean = wearSessionDao.markOverdueReminderSent(
        sessionId = sessionId,
        previousReminderSentAt = previousReminderSentAt,
        reminderSentAt = reminderSentAt
    ) > 0

    suspend fun markFinalAlertSent(
        sessionId: Long,
        scheduledAt: Instant,
        sentAt: Instant,
    ): Boolean = wearSessionDao.markFinalAlertSent(
        sessionId = sessionId,
        scheduledAt = scheduledAt,
        sentAt = sentAt
    ) > 0

    suspend fun activatePlannedSession(
        sessionId: Long,
        source: String,
        actualStartAt: Instant,
        expectedEndAt: Instant,
        finalAlertScheduledFor: Instant?,
    ): Boolean = wearSessionDao.activatePlannedSession(
        sessionId = sessionId,
        source = source,
        actualStartAt = actualStartAt,
        expectedEndAt = expectedEndAt,
        finalAlertScheduledFor = finalAlertScheduledFor
    ) > 0

    suspend fun completeSession(
        sessionId: Long,
        completedAt: Instant,
    ): Boolean = wearSessionDao.completeSession(
        sessionId = sessionId,
        completedAt = completedAt
    ) > 0

    suspend fun cancelPlannedSession(sessionId: Long): Boolean =
        wearSessionDao.cancelPlannedSession(sessionId) > 0

    suspend fun snoozePlannedSession(
        sessionId: Long,
        plannedStartAt: Instant,
    ): Boolean = wearSessionDao.snoozePlannedSession(
        sessionId = sessionId,
        plannedStartAt = plannedStartAt
    ) > 0

    suspend fun markActiveSessionOverdue(
        sessionId: Long,
        expectedEndAt: Instant,
    ): Boolean = wearSessionDao.markActiveSessionOverdue(
        sessionId = sessionId,
        expectedEndAt = expectedEndAt
    ) > 0

    suspend fun updateFinalAlertSchedule(
        sessionId: Long,
        actualStartAt: Instant,
        expectedFinalAlertScheduledFor: Instant?,
        updatedFinalAlertScheduledFor: Instant?,
    ): Boolean = wearSessionDao.updateFinalAlertSchedule(
        sessionId = sessionId,
        actualStartAt = actualStartAt,
        expectedFinalAlertScheduledFor = expectedFinalAlertScheduledFor,
        updatedFinalAlertScheduledFor = updatedFinalAlertScheduledFor
    ) > 0
}
