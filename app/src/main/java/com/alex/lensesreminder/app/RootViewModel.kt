package com.alex.lensesreminder.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * View-model for app-wide startup state.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    preferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    val uiState = preferencesRepository.preferences
        .map { preferences ->
            RootUiState(
                isLoading = false,
                hasCompletedOnboarding = preferences.hasCompletedOnboarding
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = RootUiState()
        )
}

/**
 * Startup UI contract for deciding the first screen.
 */
data class RootUiState(
    val isLoading: Boolean = true,
    val hasCompletedOnboarding: Boolean = false,
)
