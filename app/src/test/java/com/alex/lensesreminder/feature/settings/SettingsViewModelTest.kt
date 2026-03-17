package com.alex.lensesreminder.feature.settings

import app.cash.turbine.test
import com.alex.lensesreminder.R
import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.data.local.db.toEntity
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.domain.scheduler.DailyStartReminderCoordinator
import com.alex.lensesreminder.domain.scheduler.ProfileReminderReconciler
import com.alex.lensesreminder.domain.scheduler.ReminderAlarmType
import com.alex.lensesreminder.domain.scheduler.ReminderScheduleCoordinator
import com.alex.lensesreminder.testutil.FakeLensProfileDao
import com.alex.lensesreminder.testutil.FakeLensClock
import com.alex.lensesreminder.testutil.FakeReminderAlarmScheduler
import com.alex.lensesreminder.testutil.FakeReminderNotificationPublisher
import com.alex.lensesreminder.testutil.FakeWearSessionDao
import com.alex.lensesreminder.testutil.MainDispatcherRule
import com.alex.lensesreminder.testutil.createTestPreferencesDataStore
import com.alex.lensesreminder.data.repository.WearSessionRepository
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `ui state loads persisted profile on startup`() = runTest {
        val profileDao = FakeLensProfileDao()
        profileDao.upsert(
            LensProfile(
                maxWearMinutes = 960,
                remindersEnabled = false,
                finalAlertTime = LocalTime.of(21, 45)
            ).toEntity()
        )
        val viewModel = SettingsViewModel(
            lensProfileRepository = LensProfileRepository(profileDao),
            appPreferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore()),
            profileReminderReconciler = createReminderReconciler(
                profileRepository = LensProfileRepository(profileDao),
                sessionRepository = WearSessionRepository(FakeWearSessionDao()),
                scheduler = FakeReminderAlarmScheduler(),
                publisher = FakeReminderNotificationPublisher(),
                clock = FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
            )
        )

        advanceUntilIdle()

        assertEquals(
            SettingsUiState(
                isLoading = false,
                maxWearHoursInput = "16",
                maxWearMinutesInput = "0",
                remindersEnabled = false,
                finalAlertTime = LocalTime.of(21, 45),
                dailyStartReminderTime = LocalTime.of(8, 0),
                repeatReminderMinutes = 15
            ),
            viewModel.uiState.value
        )
    }

    @Test
    fun `max wear inputs keep only digits and cap length`() = runTest {
        val viewModel = SettingsViewModel(
            lensProfileRepository = LensProfileRepository(FakeLensProfileDao()),
            appPreferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore()),
            profileReminderReconciler = createReminderReconciler(
                profileRepository = LensProfileRepository(FakeLensProfileDao()),
                sessionRepository = WearSessionRepository(FakeWearSessionDao()),
                scheduler = FakeReminderAlarmScheduler(),
                publisher = FakeReminderNotificationPublisher(),
                clock = FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
            )
        )

        advanceUntilIdle()
        viewModel.onMaxWearHoursChanged("12a34b5")
        viewModel.onMaxWearMinutesChanged("6x789")

        assertEquals("123", viewModel.uiState.value.maxWearHoursInput)
        assertEquals("67", viewModel.uiState.value.maxWearMinutesInput)
    }

    @Test
    fun `save profile emits validation error for invalid duration`() = runTest {
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val viewModel = SettingsViewModel(
            lensProfileRepository = profileRepository,
            appPreferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore()),
            profileReminderReconciler = createReminderReconciler(
                profileRepository = profileRepository,
                sessionRepository = WearSessionRepository(FakeWearSessionDao()),
                scheduler = FakeReminderAlarmScheduler(),
                publisher = FakeReminderNotificationPublisher(),
                clock = FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
            )
        )

        advanceUntilIdle()
        viewModel.onMaxWearHoursChanged("0")
        viewModel.onMaxWearMinutesChanged("0")

        viewModel.events.test {
            viewModel.saveProfile(completeOnboarding = false)
            advanceUntilIdle()

            assertEquals(
                SettingsEvent.ValidationError(R.string.error_invalid_wear_duration),
                awaitItem()
            )
        }

        assertEquals(LensProfile(), profileRepository.profile.first())
    }

    @Test
    fun `save profile emits validation error when minutes exceed 59`() = runTest {
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val viewModel = SettingsViewModel(
            lensProfileRepository = profileRepository,
            appPreferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore()),
            profileReminderReconciler = createReminderReconciler(
                profileRepository = profileRepository,
                sessionRepository = WearSessionRepository(FakeWearSessionDao()),
                scheduler = FakeReminderAlarmScheduler(),
                publisher = FakeReminderNotificationPublisher(),
                clock = FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
            )
        )

        advanceUntilIdle()
        viewModel.onMaxWearHoursChanged("12")
        viewModel.onMaxWearMinutesChanged("60")

        viewModel.events.test {
            viewModel.saveProfile(completeOnboarding = false)
            advanceUntilIdle()

            assertEquals(
                SettingsEvent.ValidationError(R.string.error_invalid_wear_duration),
                awaitItem()
            )
        }
    }

    @Test
    fun `save profile persists changes and completes onboarding when requested`() = runTest {
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val preferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore())
        val viewModel = SettingsViewModel(
            lensProfileRepository = profileRepository,
            appPreferencesRepository = preferencesRepository,
            profileReminderReconciler = createReminderReconciler(
                profileRepository = profileRepository,
                sessionRepository = WearSessionRepository(FakeWearSessionDao()),
                scheduler = FakeReminderAlarmScheduler(),
                publisher = FakeReminderNotificationPublisher(),
                clock = FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
            )
        )

        advanceUntilIdle()
        viewModel.onMaxWearHoursChanged("14")
        viewModel.onRemindersEnabledChanged(false)
        viewModel.onFinalAlertTimeChanged(LocalTime.of(23, 30))
        viewModel.onDailyStartReminderTimeChanged(LocalTime.of(7, 15))

        viewModel.events.test {
            viewModel.saveProfile(completeOnboarding = true)
            advanceUntilIdle()

            assertEquals(SettingsEvent.ProfileSaved, awaitItem())
        }

        assertEquals(
            LensProfile(
                maxWearMinutes = 840,
                remindersEnabled = false,
                finalAlertTime = LocalTime.of(23, 30),
                dailyStartReminderTime = LocalTime.of(7, 15)
            ),
            profileRepository.profile.first()
        )
        assertEquals(true, preferencesRepository.preferences.first().hasCompletedOnboarding)
    }

    @Test
    fun `save profile reconciles active session final alert`() = runTest {
        val clock = FakeLensClock(Instant.parse("2026-03-14T10:00:00Z"))
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        profileRepository.saveProfile(
            LensProfile(
                finalAlertTime = LocalTime.of(23, 0),
                dailyStartReminderTime = LocalTime.of(8, 0)
            )
        )
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val scheduler = FakeReminderAlarmScheduler()
        val publisher = FakeReminderNotificationPublisher()
        val viewModel = SettingsViewModel(
            lensProfileRepository = profileRepository,
            appPreferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore()),
            profileReminderReconciler = createReminderReconciler(
                profileRepository = profileRepository,
                sessionRepository = sessionRepository,
                scheduler = scheduler,
                publisher = publisher,
                clock = clock
            )
        )
        sessionRepository.saveSession(
            WearSession(
                actualStartAt = Instant.parse("2026-03-14T08:00:00Z"),
                expectedEndAt = Instant.parse("2026-03-14T18:00:00Z"),
                status = SessionStatus.ACTIVE,
                finalAlertScheduledFor = Instant.parse("2026-03-14T22:00:00Z")
            )
        )

        advanceUntilIdle()
        viewModel.onFinalAlertTimeChanged(LocalTime.of(21, 30))
        viewModel.saveProfile(completeOnboarding = false)
        advanceUntilIdle()

        assertEquals(
            Instant.parse("2026-03-14T20:30:00Z"),
            sessionRepository.getCurrentSession()?.finalAlertScheduledFor
        )
        assertEquals(
            Instant.parse("2026-03-14T20:30:00Z"),
            scheduler.scheduledAlarms.first { it.type == ReminderAlarmType.FINAL_ALERT }.triggerAt
        )
    }

    @Test
    fun `save profile disables and clears current session reminders`() = runTest {
        val clock = FakeLensClock(Instant.parse("2026-03-14T10:00:00Z"))
        val profileRepository = LensProfileRepository(FakeLensProfileDao())
        val sessionRepository = WearSessionRepository(FakeWearSessionDao())
        val scheduler = FakeReminderAlarmScheduler()
        val publisher = FakeReminderNotificationPublisher()
        val viewModel = SettingsViewModel(
            lensProfileRepository = profileRepository,
            appPreferencesRepository = AppPreferencesRepository(createTestPreferencesDataStore()),
            profileReminderReconciler = createReminderReconciler(
                profileRepository = profileRepository,
                sessionRepository = sessionRepository,
                scheduler = scheduler,
                publisher = publisher,
                clock = clock
            )
        )
        val sessionId = sessionRepository.saveSession(
            WearSession(
                actualStartAt = Instant.parse("2026-03-14T08:00:00Z"),
                expectedEndAt = Instant.parse("2026-03-14T18:00:00Z"),
                status = SessionStatus.ACTIVE,
                finalAlertScheduledFor = Instant.parse("2026-03-14T22:00:00Z")
            )
        )
        ReminderScheduleCoordinator(profileRepository, scheduler, clock).sync(
            sessionRepository.getCurrentSession()!!
        )

        advanceUntilIdle()
        viewModel.onRemindersEnabledChanged(false)
        viewModel.saveProfile(completeOnboarding = false)
        advanceUntilIdle()

        assertTrue(scheduler.scheduledAlarms.none { it.sessionId == sessionId })
        assertEquals(listOf(sessionId), publisher.cancelledSessionIds)
    }

    private fun createReminderReconciler(
        profileRepository: LensProfileRepository,
        sessionRepository: WearSessionRepository,
        scheduler: FakeReminderAlarmScheduler,
        publisher: FakeReminderNotificationPublisher,
        clock: FakeLensClock,
    ): ProfileReminderReconciler {
        return ProfileReminderReconciler(
            wearSessionRepository = sessionRepository,
            reminderScheduleCoordinator = ReminderScheduleCoordinator(
                profileRepository,
                scheduler,
                clock
            ),
            dailyStartReminderCoordinator = DailyStartReminderCoordinator(
                profileRepository,
                sessionRepository,
                scheduler,
                clock
            ),
            reminderNotificationPublisher = publisher,
            clock = clock
        )
    }
}
