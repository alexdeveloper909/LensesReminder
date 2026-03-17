package com.alex.lensesreminder.app

import app.cash.turbine.test
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import com.alex.lensesreminder.domain.scheduler.DailyStartReminderCoordinator
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
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
        val viewModel = RootViewModel(
            preferencesRepository,
            DailyStartReminderCoordinator(
                LensProfileRepository(FakeLensProfileDao()),
                WearSessionRepository(FakeWearSessionDao()),
                FakeReminderAlarmScheduler(),
                FakeLensClock(Instant.parse("2026-03-14T08:00:00Z"))
            )
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
}
