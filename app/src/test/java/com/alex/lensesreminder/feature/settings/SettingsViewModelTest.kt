package com.alex.lensesreminder.feature.settings

import app.cash.turbine.test
import com.alex.lensesreminder.R
import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.data.local.db.toEntity
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.domain.scheduler.DailyStartReminderCoordinator
import com.alex.lensesreminder.testutil.FakeLensProfileDao
import com.alex.lensesreminder.testutil.FakeLensClock
import com.alex.lensesreminder.testutil.FakeReminderAlarmScheduler
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
            dailyStartReminderCoordinator = DailyStartReminderCoordinator(
                LensProfileRepository(profileDao),
                WearSessionRepository(FakeWearSessionDao()),
                FakeReminderAlarmScheduler(),
                FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
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
            dailyStartReminderCoordinator = DailyStartReminderCoordinator(
                LensProfileRepository(FakeLensProfileDao()),
                WearSessionRepository(FakeWearSessionDao()),
                FakeReminderAlarmScheduler(),
                FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
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
            dailyStartReminderCoordinator = DailyStartReminderCoordinator(
                profileRepository,
                WearSessionRepository(FakeWearSessionDao()),
                FakeReminderAlarmScheduler(),
                FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
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
            dailyStartReminderCoordinator = DailyStartReminderCoordinator(
                profileRepository,
                WearSessionRepository(FakeWearSessionDao()),
                FakeReminderAlarmScheduler(),
                FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
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
            dailyStartReminderCoordinator = DailyStartReminderCoordinator(
                profileRepository,
                WearSessionRepository(FakeWearSessionDao()),
                FakeReminderAlarmScheduler(),
                FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
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
}
