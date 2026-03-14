package com.alex.lensesreminder.data.repository

import androidx.datastore.preferences.core.Preferences
import com.alex.lensesreminder.data.local.datastore.AppPreferences
import com.alex.lensesreminder.testutil.MainDispatcherRule
import com.alex.lensesreminder.testutil.createTestPreferencesDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppPreferencesRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `preferences default to false flags`() = runTest {
        val repository = AppPreferencesRepository(createTestPreferencesDataStore())

        assertEquals(AppPreferences(), repository.preferences.first())
    }

    @Test
    fun `preferences updates are persisted`() = runTest {
        val repository = AppPreferencesRepository(createTestPreferencesDataStore())

        repository.setHasCompletedOnboarding(true)
        repository.setNotificationsPermissionRequested(true)
        repository.setExactAlarmWarningDismissed(true)

        assertEquals(
            AppPreferences(
                hasCompletedOnboarding = true,
                notificationsPermissionRequested = true,
                exactAlarmWarningDismissed = true
            ),
            repository.preferences.first()
        )
    }
}
