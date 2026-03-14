package com.alex.lensesreminder.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.alex.lensesreminder.data.local.datastore.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for simple UI and onboarding preferences.
 */
@Singleton
class AppPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val preferences: Flow<AppPreferences> = dataStore.data.map { storedPreferences ->
        AppPreferences(
            hasCompletedOnboarding = storedPreferences[HAS_COMPLETED_ONBOARDING] ?: false,
            notificationsPermissionRequested = storedPreferences[NOTIFICATIONS_PERMISSION_REQUESTED]
                ?: false,
            exactAlarmWarningDismissed = storedPreferences[EXACT_ALARM_WARNING_DISMISSED] ?: false
        )
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    suspend fun setNotificationsPermissionRequested(requested: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_PERMISSION_REQUESTED] = requested
        }
    }

    suspend fun setExactAlarmWarningDismissed(dismissed: Boolean) {
        dataStore.edit { preferences ->
            preferences[EXACT_ALARM_WARNING_DISMISSED] = dismissed
        }
    }

    private companion object {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val NOTIFICATIONS_PERMISSION_REQUESTED =
            booleanPreferencesKey("notifications_permission_requested")
        val EXACT_ALARM_WARNING_DISMISSED =
            booleanPreferencesKey("exact_alarm_warning_dismissed")
    }
}
