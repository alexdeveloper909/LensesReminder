package com.alex.lensesreminder.app

import app.cash.turbine.test
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import com.alex.lensesreminder.testutil.MainDispatcherRule
import com.alex.lensesreminder.testutil.createTestPreferencesDataStore
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
        val viewModel = RootViewModel(preferencesRepository)

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
