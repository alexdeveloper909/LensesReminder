package com.alex.lensesreminder.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import com.alex.lensesreminder.data.repository.LensProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * View-model backing the Phase 1 home screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    lensProfileRepository: LensProfileRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        lensProfileRepository.profile,
        appPreferencesRepository.preferences
    ) { profile, preferences ->
        HomeUiState(
            profile = profile,
            notificationsPermissionRequested = preferences.notificationsPermissionRequested
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = HomeUiState()
    )

    fun onNotificationPermissionRequestLaunched() {
        viewModelScope.launch {
            appPreferencesRepository.setNotificationsPermissionRequested(true)
        }
    }
}

/**
 * Home screen state for the app foundation phase.
 */
data class HomeUiState(
    val profile: LensProfile = LensProfile(),
    val notificationsPermissionRequested: Boolean = false,
)
