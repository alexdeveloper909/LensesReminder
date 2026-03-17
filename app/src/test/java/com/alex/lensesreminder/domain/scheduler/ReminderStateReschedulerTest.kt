package com.alex.lensesreminder.domain.scheduler

import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
import com.alex.lensesreminder.testutil.FakeLensClock
import com.alex.lensesreminder.testutil.FakeLensProfileDao
import com.alex.lensesreminder.testutil.FakeReminderAlarmScheduler
import com.alex.lensesreminder.testutil.FakeWearSessionDao
import com.alex.lensesreminder.testutil.MainDispatcherRule
import com.alex.lensesreminder.testutil.createTestPreferencesDataStore
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderStateReschedulerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `sync all restores session and daily reminders`() = runTest {
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val preferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore())
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val scheduler = FakeReminderAlarmScheduler()
        val clock = FakeLensClock(Instant.parse("2026-03-14T18:00:00Z"))
        preferencesRepository.setHasCompletedOnboarding(true)
        val rescheduler = ReminderStateRescheduler(
            preferencesRepository,
            sessionRepository,
            ReminderScheduleCoordinator(profileRepository, scheduler, clock),
            DailyStartReminderCoordinator(profileRepository, sessionRepository, scheduler, clock)
        )

        sessionRepository.saveSession(
            WearSession(
                id = 8,
                actualStartAt = clock.now().minus(Duration.ofHours(7)),
                expectedEndAt = clock.now().plus(Duration.ofHours(1)),
                status = SessionStatus.ACTIVE,
                finalAlertScheduledFor = clock.now().plus(Duration.ofHours(4))
            )
        )

        rescheduler.syncAll()

        assertEquals(3, scheduler.scheduledAlarms.size)
        assertTrue(scheduler.scheduledAlarms.any { it.type == ReminderAlarmType.WEAR_END })
        assertTrue(scheduler.scheduledAlarms.any { it.type == ReminderAlarmType.FINAL_ALERT })
        assertTrue(
            scheduler.scheduledAlarms.any {
                it.sessionId == DAILY_START_REMINDER_SESSION_ID &&
                    it.type == ReminderAlarmType.DAILY_START
            }
        )
    }
}
