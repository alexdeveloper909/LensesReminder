package com.alex.lensesreminder.app

import app.cash.turbine.test
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
import com.alex.lensesreminder.domain.scheduler.DailyStartReminderCoordinator
import com.alex.lensesreminder.domain.scheduler.ReminderScheduleCoordinator
import com.alex.lensesreminder.domain.scheduler.ReminderStateRescheduler
import com.alex.lensesreminder.testutil.FakeLensClock
import com.alex.lensesreminder.testutil.FakeLensProfileDao
import com.alex.lensesreminder.testutil.FakeReminderAlarmScheduler
import com.alex.lensesreminder.testutil.FakeWearSessionDao
import com.alex.lensesreminder.testutil.MainDispatcherRule
import com.alex.lensesreminder.testutil.createTestPreferencesDataStore
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RootViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `ui state reflects onboarding completion from preferences`() = runTest {
        val preferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore())
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val scheduler = FakeReminderAlarmScheduler()
        val clock = FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
        val viewModel = RootViewModel(
            preferencesRepository,
            DailyStartReminderCoordinator(
                profileRepository,
                sessionRepository,
                scheduler,
                clock
            ),
            ReminderStateRescheduler(
                preferencesRepository,
                sessionRepository,
                ReminderScheduleCoordinator(profileRepository, scheduler, clock),
                DailyStartReminderCoordinator(profileRepository, sessionRepository, scheduler, clock)
            ),
        )

        viewModel.uiState.test {
            assertEquals(RootUiState(), awaitItem())
            assertEquals(
                RootUiState(isLoading = false, hasCompletedOnboarding = false),
                awaitItem()
            )

            preferencesRepository.setHasCompletedOnboarding(true)
            advanceUntilIdle()

            assertEquals(
                RootUiState(isLoading = false, hasCompletedOnboarding = true),
                awaitItem()
            )
        }
    }

    @Test
    fun `foreground sync reschedules the current session`() = runTest {
        val preferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore())
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val scheduler = FakeReminderAlarmScheduler()
        val clock = FakeLensClock(Instant.parse("2026-03-14T20:00:00Z"))
        preferencesRepository.setHasCompletedOnboarding(true)
        sessionRepository.saveSession(
            WearSession(
                id = 4,
                actualStartAt = clock.now().minusSeconds(60 * 60 * 8),
                expectedEndAt = clock.now().plusSeconds(60 * 30),
                status = SessionStatus.ACTIVE
            )
        )
        val viewModel = RootViewModel(
            preferencesRepository,
            DailyStartReminderCoordinator(
                profileRepository,
                sessionRepository,
                scheduler,
                clock
            ),
            ReminderStateRescheduler(
                preferencesRepository,
                sessionRepository,
                ReminderScheduleCoordinator(profileRepository, scheduler, clock),
                DailyStartReminderCoordinator(profileRepository, sessionRepository, scheduler, clock)
            ),
        )

        advanceUntilIdle()
        scheduler.scheduledAlarms.clear()

        viewModel.onAppForegrounded()
        advanceUntilIdle()

        assertEquals(2, scheduler.scheduledAlarms.size)
    }
}
