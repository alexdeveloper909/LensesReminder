package com.alex.lensesreminder.feature.home

import app.cash.turbine.test
import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
import com.alex.lensesreminder.domain.scheduler.DailyStartReminderCoordinator
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
            DailyStartReminderCoordinator(
                profileRepository,
                sessionRepository,
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
            DailyStartReminderCoordinator(
                profileRepository,
                sessionRepository,
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

    @Test
    fun `starting from an earlier active time emits active message`() = runTest {
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        profileRepository.saveProfile(LensProfile(maxWearMinutes = 600))
        val preferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore())
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val clock = FakeLensClock(Instant.parse("2026-03-14T15:00:00Z"))
        val sessionLifecycleManager = SessionLifecycleManager(
            profileRepository,
            sessionRepository,
            ReminderScheduleCoordinator(
                profileRepository,
                FakeReminderAlarmScheduler(),
                clock
            ),
            DailyStartReminderCoordinator(
                profileRepository,
                sessionRepository,
                FakeReminderAlarmScheduler(),
                clock
            ),
            FakeReminderNotificationPublisher(),
            clock
        )
        val viewModel = HomeViewModel(
            profileRepository,
            preferencesRepository,
            sessionRepository,
            sessionLifecycleManager
        )

        viewModel.events.test {
            viewModel.onStartAtClick(Instant.parse("2026-03-14T13:00:00Z"))
            advanceUntilIdle()

            assertEquals(
                HomeEvent.Message(com.alex.lensesreminder.R.string.state_session_active),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `starting from an overdue time emits overdue message`() = runTest {
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        profileRepository.saveProfile(LensProfile(maxWearMinutes = 120))
        val preferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore())
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val clock = FakeLensClock(Instant.parse("2026-03-14T15:00:00Z"))
        val sessionLifecycleManager = SessionLifecycleManager(
            profileRepository,
            sessionRepository,
            ReminderScheduleCoordinator(
                profileRepository,
                FakeReminderAlarmScheduler(),
                clock
            ),
            DailyStartReminderCoordinator(
                profileRepository,
                sessionRepository,
                FakeReminderAlarmScheduler(),
                clock
            ),
            FakeReminderNotificationPublisher(),
            clock
        )
        val viewModel = HomeViewModel(
            profileRepository,
            preferencesRepository,
            sessionRepository,
            sessionLifecycleManager
        )

        viewModel.events.test {
            viewModel.onStartAtClick(Instant.parse("2026-03-14T11:30:00Z"))
            advanceUntilIdle()

            assertEquals(
                HomeEvent.Message(com.alex.lensesreminder.R.string.state_session_overdue),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completing an active session exposes an on time completion summary`() = runTest {
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val preferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore())
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val clock = FakeLensClock(Instant.parse("2026-03-14T16:30:00Z"))
        val sessionLifecycleManager = SessionLifecycleManager(
            profileRepository,
            sessionRepository,
            ReminderScheduleCoordinator(
                profileRepository,
                FakeReminderAlarmScheduler(),
                clock
            ),
            DailyStartReminderCoordinator(
                profileRepository,
                sessionRepository,
                FakeReminderAlarmScheduler(),
                clock
            ),
            FakeReminderNotificationPublisher(),
            clock
        )
        sessionRepository.saveSession(
            WearSession(
                actualStartAt = Instant.parse("2026-03-14T08:00:00Z"),
                expectedEndAt = Instant.parse("2026-03-14T18:00:00Z"),
                finalAlertScheduledFor = Instant.parse("2026-03-14T17:00:00Z"),
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
            awaitItem()

            viewModel.onCompleteSessionClick()
            advanceUntilIdle()

            val updatedState = awaitItem()
            assertEquals(
                HomeCompletionSummaryUiState(
                    wearDuration = Duration.ofHours(8).plusMinutes(30),
                    removedOnTime = true,
                    overdueBy = null
                ),
                updatedState.completionSummary
            )
            assertEquals(HomeSessionUiState(), updatedState.session)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completing an overdue session exposes overdue duration in completion summary`() = runTest {
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val preferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore())
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val clock = FakeLensClock(Instant.parse("2026-03-14T17:45:00Z"))
        val sessionLifecycleManager = SessionLifecycleManager(
            profileRepository,
            sessionRepository,
            ReminderScheduleCoordinator(
                profileRepository,
                FakeReminderAlarmScheduler(),
                clock
            ),
            DailyStartReminderCoordinator(
                profileRepository,
                sessionRepository,
                FakeReminderAlarmScheduler(),
                clock
            ),
            FakeReminderNotificationPublisher(),
            clock
        )
        sessionRepository.saveSession(
            WearSession(
                actualStartAt = Instant.parse("2026-03-14T08:00:00Z"),
                expectedEndAt = Instant.parse("2026-03-14T18:00:00Z"),
                finalAlertScheduledFor = Instant.parse("2026-03-14T17:00:00Z"),
                status = SessionStatus.OVERDUE
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
            awaitItem()

            viewModel.onCompleteSessionClick()
            advanceUntilIdle()

            assertEquals(
                HomeCompletionSummaryUiState(
                    wearDuration = Duration.ofHours(9).plusMinutes(45),
                    removedOnTime = false,
                    overdueBy = Duration.ofMinutes(45)
                ),
                awaitItem().completionSummary
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissing completion summary clears it from ui state`() = runTest {
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val preferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore())
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val clock = FakeLensClock(Instant.parse("2026-03-14T17:45:00Z"))
        val sessionLifecycleManager = SessionLifecycleManager(
            profileRepository,
            sessionRepository,
            ReminderScheduleCoordinator(
                profileRepository,
                FakeReminderAlarmScheduler(),
                clock
            ),
            DailyStartReminderCoordinator(
                profileRepository,
                sessionRepository,
                FakeReminderAlarmScheduler(),
                clock
            ),
            FakeReminderNotificationPublisher(),
            clock
        )
        sessionRepository.saveSession(
            WearSession(
                actualStartAt = Instant.parse("2026-03-14T08:00:00Z"),
                expectedEndAt = Instant.parse("2026-03-14T18:00:00Z"),
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
            awaitItem()

            viewModel.onCompleteSessionClick()
            advanceUntilIdle()
            awaitItem()

            viewModel.onCompletionSummaryDismissed()
            advanceUntilIdle()

            assertEquals(null, awaitItem().completionSummary)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
