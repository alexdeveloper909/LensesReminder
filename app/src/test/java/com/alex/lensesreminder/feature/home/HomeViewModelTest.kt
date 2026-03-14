package com.alex.lensesreminder.feature.home

import app.cash.turbine.test
import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
import com.alex.lensesreminder.domain.scheduler.ReminderScheduleCoordinator
import com.alex.lensesreminder.domain.session.SessionLifecycleManager
import com.alex.lensesreminder.testutil.FakeLensProfileDao
import com.alex.lensesreminder.testutil.FakeLensClock
import com.alex.lensesreminder.testutil.MainDispatcherRule
import com.alex.lensesreminder.testutil.FakeReminderAlarmScheduler
import com.alex.lensesreminder.testutil.FakeReminderNotificationPublisher
import com.alex.lensesreminder.testutil.FakeWearSessionDao
import com.alex.lensesreminder.testutil.createTestPreferencesDataStore
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `ui state combines profile and permission request state`() = runTest {
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val preferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore())
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val clock = FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
        val sessionLifecycleManager = SessionLifecycleManager(
            profileRepository,
            sessionRepository,
            ReminderScheduleCoordinator(
                profileRepository,
                FakeReminderAlarmScheduler(),
                clock
            ),
            FakeReminderNotificationPublisher(),
            clock
        )
        val expectedProfile = LensProfile(maxWearMinutes = 600, remindersEnabled = false)
        profileRepository.saveProfile(expectedProfile)

        val viewModel = HomeViewModel(
            profileRepository,
            preferencesRepository,
            sessionRepository,
            sessionLifecycleManager
        )

        viewModel.uiState.test {
            assertEquals(HomeUiState(), awaitItem())

            advanceUntilIdle()

            assertEquals(
                HomeUiState(
                    profile = expectedProfile,
                    notificationsPermissionRequested = false
                ),
                awaitItem()
            )

            viewModel.onNotificationPermissionRequestLaunched()
            advanceUntilIdle()

            assertEquals(
                HomeUiState(
                    profile = expectedProfile,
                    notificationsPermissionRequested = true
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ui state exposes the current session snapshot`() = runTest {
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val preferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore())
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val clock = FakeLensClock(Instant.parse("2026-03-14T12:00:00Z"))
        val sessionLifecycleManager = SessionLifecycleManager(
            profileRepository,
            sessionRepository,
            ReminderScheduleCoordinator(
                profileRepository,
                FakeReminderAlarmScheduler(),
                clock
            ),
            FakeReminderNotificationPublisher(),
            clock
        )
        sessionRepository.saveSession(
            WearSession(
                actualStartAt = clock.now().minus(Duration.ofHours(8)),
                expectedEndAt = clock.now().minus(Duration.ofMinutes(30)),
                status = SessionStatus.ACTIVE
            )
        )

        val viewModel = HomeViewModel(
            profileRepository,
            preferencesRepository,
            sessionRepository,
            sessionLifecycleManager
        )

        viewModel.uiState.test {
            assertEquals(HomeUiState(), awaitItem())

            advanceUntilIdle()

            val updatedState = awaitItem()
            assertEquals(SessionStatus.ACTIVE, updatedState.session.status)
            assertEquals(clock.now().minus(Duration.ofHours(8)), updatedState.session.actualStartAt)
            assertEquals(clock.now().minus(Duration.ofMinutes(30)), updatedState.session.expectedEndAt)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
