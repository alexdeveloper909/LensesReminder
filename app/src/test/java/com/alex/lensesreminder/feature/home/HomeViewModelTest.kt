package com.alex.lensesreminder.feature.home

import app.cash.turbine.test
import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.testutil.FakeLensProfileDao
import com.alex.lensesreminder.testutil.MainDispatcherRule
import com.alex.lensesreminder.testutil.createTestPreferencesDataStore
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
        val expectedProfile = LensProfile(maxWearMinutes = 600, remindersEnabled = false)
        profileRepository.saveProfile(expectedProfile)

        val viewModel = HomeViewModel(profileRepository, preferencesRepository)

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
        }
    }
}
