package com.alex.lensesreminder.data.local.datastore

/**
 * Lightweight user preferences stored in DataStore.
 */
data class AppPreferences(
    val hasCompletedOnboarding: Boolean = false,
    val notificationsPermissionRequested: Boolean = false,
    val exactAlarmWarningDismissed: Boolean = false,
)
