package com.alex.lensesreminder.domain.scheduler

import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Rebuilds the app's alarm state from persisted sources after system events.
 */
@Singleton
class ReminderStateRescheduler @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val wearSessionRepository: WearSessionRepository,
    private val reminderScheduleCoordinator: ReminderScheduleCoordinator,
    private val dailyStartReminderCoordinator: DailyStartReminderCoordinator,
) {

    suspend fun syncAll() {
        if (!appPreferencesRepository.preferences.first().hasCompletedOnboarding) {
            dailyStartReminderCoordinator.clear()
            wearSessionRepository.getCurrentSession()?.let { reminderScheduleCoordinator.clear(it.id) }
            return
        }

        wearSessionRepository.getCurrentSession()
            ?.takeIf { it.status in openStatuses }
            ?.let { reminderScheduleCoordinator.sync(it) }

        dailyStartReminderCoordinator.sync()
    }

    private companion object {
        val openStatuses = setOf(SessionStatus.PLANNED, SessionStatus.ACTIVE, SessionStatus.OVERDUE)
    }
}
