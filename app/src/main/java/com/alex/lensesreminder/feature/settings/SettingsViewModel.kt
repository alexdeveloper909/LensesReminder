package com.alex.lensesreminder.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.lensesreminder.R
import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import com.alex.lensesreminder.data.repository.LensProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * View-model for editing the single lens profile.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val lensProfileRepository: LensProfileRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(SettingsUiState())
    private val mutableEvents = MutableSharedFlow<SettingsEvent>()
    private var hasLoadedProfile = false

    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    val events = mutableEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            lensProfileRepository.profile.collectLatest { profile ->
                if (!hasLoadedProfile) {
                    mutableUiState.value = profile.toSettingsUiState()
                    hasLoadedProfile = true
                }
            }
        }
    }

    fun onMaxWearMinutesChanged(value: String) {
        mutableUiState.value = mutableUiState.value.copy(
            isLoading = false,
            maxWearMinutesInput = value.filter(Char::isDigit).take(4)
        )
    }

    fun onRemindersEnabledChanged(enabled: Boolean) {
        mutableUiState.value = mutableUiState.value.copy(
            isLoading = false,
            remindersEnabled = enabled
        )
    }

    fun onFinalAlertTimeChanged(value: java.time.LocalTime) {
        mutableUiState.value = mutableUiState.value.copy(
            isLoading = false,
            finalAlertTime = value
        )
    }

    fun saveProfile(completeOnboarding: Boolean) {
        val currentState = mutableUiState.value
        val maxWearMinutes = currentState.maxWearMinutesInput.toIntOrNull()

        if (maxWearMinutes == null || maxWearMinutes <= 0) {
            emitValidationError(R.string.error_invalid_wear_duration)
            return
        }

        viewModelScope.launch {
            lensProfileRepository.saveProfile(
                LensProfile(
                    lensType = currentState.lensType,
                    maxWearMinutes = maxWearMinutes,
                    remindersEnabled = currentState.remindersEnabled,
                    finalAlertTime = currentState.finalAlertTime,
                    repeatReminderMinutes = currentState.repeatReminderMinutes
                )
            )
            if (completeOnboarding) {
                appPreferencesRepository.setHasCompletedOnboarding(true)
            }
            mutableEvents.emit(SettingsEvent.ProfileSaved)
        }
    }

    private fun emitValidationError(@androidx.annotation.StringRes messageId: Int) {
        viewModelScope.launch {
            mutableEvents.emit(SettingsEvent.ValidationError(messageId))
        }
    }
}
