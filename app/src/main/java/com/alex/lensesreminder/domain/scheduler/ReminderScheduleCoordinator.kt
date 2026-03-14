package com.alex.lensesreminder.domain.scheduler

import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.core.time.LensClock
import com.alex.lensesreminder.data.repository.LensProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Recomputes the full alarm set for the current session from source-of-truth data.
 */
@Singleton
class ReminderScheduleCoordinator @Inject constructor(
    private val lensProfileRepository: LensProfileRepository,
    private val reminderAlarmScheduler: ReminderAlarmScheduler,
    private val clock: LensClock,
) {

    suspend fun sync(session: WearSession) {
        reminderAlarmScheduler.cancelAll(session.id)

        val profile = lensProfileRepository.profile.first()
        if (!profile.remindersEnabled) {
            return
        }

        when (session.status) {
            SessionStatus.PLANNED -> {
                session.plannedStartAt
                    ?.takeIf { it.isAfter(clock.now()) }
                    ?.let { reminderAlarmScheduler.schedule(session.id, ReminderAlarmType.PLANNED_START, it) }
            }

            SessionStatus.ACTIVE -> {
                scheduleFinalAlertIfNeeded(session)
                session.expectedEndAt?.let { expectedEndAt ->
                    val triggerAt = if (expectedEndAt.isAfter(clock.now())) {
                        expectedEndAt
                    } else {
                        clock.now()
                    }
                    reminderAlarmScheduler.schedule(session.id, ReminderAlarmType.WEAR_END, triggerAt)
                }
            }

            SessionStatus.OVERDUE -> {
                if (scheduleFinalAlertIfNeeded(session)) {
                    return
                }
                when {
                    session.finalAlertSentAt != null -> Unit
                    session.reminderCount == 0 -> {
                        reminderAlarmScheduler.schedule(
                            session.id,
                            ReminderAlarmType.WEAR_END,
                            clock.now()
                        )
                    }

                    else -> {
                        nextOverdueReminderAt(session, profile)?.let { triggerAt ->
                            reminderAlarmScheduler.schedule(
                                session.id,
                                ReminderAlarmType.OVERDUE_REPEAT,
                                triggerAt
                            )
                        }
                    }
                }
            }

            SessionStatus.COMPLETED,
            SessionStatus.CANCELLED,
            -> Unit
        }
    }

    fun clear(sessionId: Long) {
        reminderAlarmScheduler.cancelAll(sessionId)
    }

    private fun scheduleFinalAlertIfNeeded(session: WearSession): Boolean {
        if (session.finalAlertSentAt != null) {
            return false
        }

        session.finalAlertScheduledFor
            ?.let { finalAlertAt ->
                val triggerAt = if (finalAlertAt.isAfter(clock.now())) {
                    finalAlertAt
                } else {
                    clock.now()
                }
                reminderAlarmScheduler.schedule(session.id, ReminderAlarmType.FINAL_ALERT, triggerAt)
                return !finalAlertAt.isAfter(clock.now())
            }

        return false
    }

    private fun nextOverdueReminderAt(
        session: WearSession,
        profile: LensProfile,
    ) = session.lastReminderSentAt
        ?.plusSeconds(profile.repeatReminderMinutes.toLong() * 60)
        ?.takeUnless { reminderAt ->
            val finalAlertAt = session.finalAlertScheduledFor
            finalAlertAt != null && !finalAlertAt.isAfter(reminderAt)
        }
}
