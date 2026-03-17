package com.alex.lensesreminder.domain.scheduler

import com.alex.lensesreminder.core.time.LensClock
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

const val DAILY_START_REMINDER_SESSION_ID = -1L

/**
 * Schedules the recurring daily reminder that prompts the user to put lenses on.
 */
@Singleton
class DailyStartReminderCoordinator @Inject constructor(
    private val lensProfileRepository: LensProfileRepository,
    private val wearSessionRepository: WearSessionRepository,
    private val reminderAlarmScheduler: ReminderAlarmScheduler,
    private val clock: LensClock,
) {

    suspend fun sync(
        skipToday: Boolean = false,
    ) {
        reminderAlarmScheduler.cancelAll(DAILY_START_REMINDER_SESSION_ID)

        val profile = lensProfileRepository.profile.first()
        if (!profile.remindersEnabled) {
            return
        }

        val hasOpenSession = wearSessionRepository.getCurrentSession() != null
        val triggerAt = nextTriggerAt(
            reminderTime = profile.dailyStartReminderTime,
            skipToday = skipToday || hasOpenSession
        )
        reminderAlarmScheduler.schedule(
            DAILY_START_REMINDER_SESSION_ID,
            ReminderAlarmType.DAILY_START,
            triggerAt
        )
    }

    fun clear() {
        reminderAlarmScheduler.cancelAll(DAILY_START_REMINDER_SESSION_ID)
    }

    private fun nextTriggerAt(
        reminderTime: java.time.LocalTime,
        skipToday: Boolean,
    ): Instant {
        val zoneId = clock.zoneId()
        val now = clock.now()
        val currentDate = now.atZone(zoneId).toLocalDate()
        val targetDate = if (skipToday) {
            currentDate.plusDays(1)
        } else {
            currentDate
        }
        val candidateAtReminderTime = targetDate
            .atTime(reminderTime)
            .atZone(zoneId)
            .toInstant()

        return when {
            skipToday -> candidateAtReminderTime
            candidateAtReminderTime.isAfter(now) -> candidateAtReminderTime
            else -> currentDate.plusDays(1).atTime(reminderTime).atZone(zoneId).toInstant()
        }
    }
}
