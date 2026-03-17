package com.alex.lensesreminder.feature.settings

import androidx.annotation.StringRes
import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.LensType
import java.time.LocalTime

/**
 * Editable settings form state shared by onboarding and settings screens.
 */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val lensType: LensType = LensType.DAILY,
    val maxWearHoursInput: String = "",
    val maxWearMinutesInput: String = "",
    val remindersEnabled: Boolean = true,
    val finalAlertTime: LocalTime = LocalTime.of(22, 0),
    val dailyStartReminderTime: LocalTime = LocalTime.of(8, 0),
    val repeatReminderMinutes: Int = 15,
)

/**
 * One-shot outcomes emitted by the settings flow.
 */
sealed interface SettingsEvent {
    data object ProfileSaved : SettingsEvent
    data class ValidationError(@param:StringRes val messageId: Int) : SettingsEvent
}

/**
 * Creates editable UI state from a persisted profile.
 */
fun LensProfile.toSettingsUiState(): SettingsUiState = SettingsUiState(
    isLoading = false,
    lensType = lensType,
    maxWearHoursInput = (maxWearMinutes / 60).toString(),
    maxWearMinutesInput = (maxWearMinutes % 60).toString(),
    remindersEnabled = remindersEnabled,
    finalAlertTime = finalAlertTime,
    dailyStartReminderTime = dailyStartReminderTime,
    repeatReminderMinutes = repeatReminderMinutes
)
