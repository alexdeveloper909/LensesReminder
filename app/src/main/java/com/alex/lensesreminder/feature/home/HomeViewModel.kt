package com.alex.lensesreminder.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
import com.alex.lensesreminder.domain.session.SessionLifecycleFailure
import com.alex.lensesreminder.domain.session.SessionLifecycleManager
import com.alex.lensesreminder.domain.session.SessionLifecycleResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * View-model backing the home screen session lifecycle.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    lensProfileRepository: LensProfileRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    wearSessionRepository: WearSessionRepository,
    private val sessionLifecycleManager: SessionLifecycleManager,
) : ViewModel() {
    private val mutableEvents = MutableSharedFlow<HomeEvent>()

    val events = mutableEvents.asSharedFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        lensProfileRepository.profile,
        appPreferencesRepository.preferences,
        wearSessionRepository.currentSession,
    ) { profile, preferences, session ->
        HomeUiState(
            profile = profile,
            notificationsPermissionRequested = preferences.notificationsPermissionRequested,
            session = session.toUiState()
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

    fun onStartNowClick() {
        viewModelScope.launch {
            when (val result = sessionLifecycleManager.startNow()) {
                is SessionLifecycleResult.Success -> {
                    emitStartSuccess(result.value)
                }
                is SessionLifecycleResult.Failure -> {
                    emitFailure(result.reason)
                }
            }
        }
    }

    fun onStartAtClick(
        actualStartAt: Instant,
    ) {
        viewModelScope.launch {
            when (val result = sessionLifecycleManager.startAt(actualStartAt)) {
                is SessionLifecycleResult.Success -> {
                    emitStartSuccess(result.value)
                }
                is SessionLifecycleResult.Failure -> {
                    emitFailure(result.reason)
                }
            }
        }
    }

    fun onActivatePlannedSessionClick() {
        viewModelScope.launch {
            when (val result = sessionLifecycleManager.activatePlannedSession()) {
                is SessionLifecycleResult.Success -> {
                    mutableEvents.emit(HomeEvent.Message(com.alex.lensesreminder.R.string.state_session_active))
                }
                is SessionLifecycleResult.Failure -> emitFailure(result.reason)
            }
        }
    }

    fun onCompleteSessionClick() {
        viewModelScope.launch {
            when (val result = sessionLifecycleManager.completeCurrentSession()) {
                is SessionLifecycleResult.Success -> {
                    mutableEvents.emit(
                        HomeEvent.Message(com.alex.lensesreminder.R.string.state_lenses_marked_off)
                    )
                }
                is SessionLifecycleResult.Failure -> emitFailure(result.reason)
            }
        }
    }

    fun onCancelPlannedSessionClick() {
        viewModelScope.launch {
            when (val result = sessionLifecycleManager.cancelPlannedSession()) {
                is SessionLifecycleResult.Success -> {
                    mutableEvents.emit(
                        HomeEvent.Message(com.alex.lensesreminder.R.string.state_plan_cancelled)
                    )
                }
                is SessionLifecycleResult.Failure -> emitFailure(result.reason)
            }
        }
    }

    private suspend fun emitFailure(
        reason: SessionLifecycleFailure,
    ) {
        val messageId = when (reason) {
            SessionLifecycleFailure.EXISTING_OPEN_SESSION -> {
                com.alex.lensesreminder.R.string.error_only_one_active_session
            }
            SessionLifecycleFailure.INVALID_ACTUAL_START -> {
                com.alex.lensesreminder.R.string.error_invalid_actual_start
            }
            SessionLifecycleFailure.INVALID_PLANNED_TIME -> {
                com.alex.lensesreminder.R.string.error_invalid_planned_start
            }
            SessionLifecycleFailure.PLANNED_SESSION_NOT_FOUND,
            SessionLifecycleFailure.ACTIVE_SESSION_NOT_FOUND,
            -> com.alex.lensesreminder.R.string.error_session_action_unavailable
        }
        mutableEvents.emit(HomeEvent.Message(messageId))
    }

    private suspend fun emitStartSuccess(
        session: WearSession,
    ) {
        val messageId = if (session.status == com.alex.lensesreminder.core.model.SessionStatus.OVERDUE) {
            com.alex.lensesreminder.R.string.state_session_overdue
        } else {
            com.alex.lensesreminder.R.string.state_session_active
        }
        mutableEvents.emit(HomeEvent.Message(messageId))
    }
}

/**
 * Home screen state for the app foundation phase.
 */
data class HomeUiState(
    val profile: LensProfile = LensProfile(),
    val notificationsPermissionRequested: Boolean = false,
    val session: HomeSessionUiState = HomeSessionUiState(),
)

data class HomeSessionUiState(
    val status: com.alex.lensesreminder.core.model.SessionStatus? = null,
    val plannedStartAt: Instant? = null,
    val actualStartAt: Instant? = null,
    val expectedEndAt: Instant? = null,
    val finalAlertScheduledFor: Instant? = null,
)

sealed interface HomeEvent {
    data class Message(
        @param:StringRes val messageId: Int,
    ) : HomeEvent
}

private fun WearSession?.toUiState(): HomeSessionUiState {
    if (this == null) {
        return HomeSessionUiState()
    }

    return HomeSessionUiState(
        status = status,
        plannedStartAt = plannedStartAt,
        actualStartAt = actualStartAt,
        expectedEndAt = expectedEndAt,
        finalAlertScheduledFor = finalAlertScheduledFor,
    )
}
