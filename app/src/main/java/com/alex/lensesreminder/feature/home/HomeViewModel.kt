package com.alex.lensesreminder.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.alex.lensesreminder.R
import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.data.repository.AppPreferencesRepository
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
import com.alex.lensesreminder.domain.session.SessionLifecycleFailure
import com.alex.lensesreminder.domain.session.SessionLifecycleManager
import com.alex.lensesreminder.domain.session.SessionLifecycleResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val mutableEvents = Channel<HomeEvent>(Channel.BUFFERED)
    private val completionSummary = MutableStateFlow<HomeCompletionSummaryUiState?>(null)

    val events = mutableEvents.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        lensProfileRepository.profile,
        appPreferencesRepository.preferences,
        wearSessionRepository.currentSession,
        completionSummary,
    ) { profile, preferences, session, summary ->
        HomeUiState(
            profile = profile,
            notificationsPermissionRequested = preferences.notificationsPermissionRequested,
            session = session.toUiState(),
            completionSummary = if (session == null) summary else null,
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
                    clearCompletionSummary()
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
                    clearCompletionSummary()
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
                    clearCompletionSummary()
                    mutableEvents.send(HomeEvent.Message(R.string.state_session_active))
                }
                is SessionLifecycleResult.Failure -> emitFailure(result.reason)
            }
        }
    }

    fun onCompleteSessionClick() {
        viewModelScope.launch {
            when (val result = sessionLifecycleManager.completeCurrentSession()) {
                is SessionLifecycleResult.Success -> {
                    completionSummary.value = result.value.toCompletionSummaryUiState()
                    mutableEvents.send(
                        HomeEvent.Message(R.string.state_lenses_marked_off)
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
                    clearCompletionSummary()
                    mutableEvents.send(
                        HomeEvent.Message(R.string.state_plan_cancelled)
                    )
                }
                is SessionLifecycleResult.Failure -> emitFailure(result.reason)
            }
        }
    }

    fun onCompletionSummaryDismissed() {
        clearCompletionSummary()
    }

    private suspend fun emitFailure(
        reason: SessionLifecycleFailure,
    ) {
        val messageId = when (reason) {
            SessionLifecycleFailure.EXISTING_OPEN_SESSION -> {
                R.string.error_only_one_active_session
            }
            SessionLifecycleFailure.INVALID_ACTUAL_START -> {
                R.string.error_invalid_actual_start
            }
            SessionLifecycleFailure.INVALID_PLANNED_TIME -> {
                R.string.error_invalid_planned_start
            }
            SessionLifecycleFailure.PLANNED_SESSION_NOT_FOUND,
            SessionLifecycleFailure.ACTIVE_SESSION_NOT_FOUND,
            -> R.string.error_session_action_unavailable
        }
        mutableEvents.send(HomeEvent.Message(messageId))
    }

    private suspend fun emitStartSuccess(
        session: WearSession,
    ) {
        val messageId = if (session.status == SessionStatus.OVERDUE) {
            R.string.state_session_overdue
        } else {
            R.string.state_session_active
        }
        mutableEvents.send(HomeEvent.Message(messageId))
    }

    private fun clearCompletionSummary() {
        completionSummary.value = null
    }
}

/**
 * Home screen state for the app foundation phase.
 */
data class HomeUiState(
    val profile: LensProfile = LensProfile(),
    val notificationsPermissionRequested: Boolean = false,
    val session: HomeSessionUiState = HomeSessionUiState(),
    val completionSummary: HomeCompletionSummaryUiState? = null,
)

data class HomeSessionUiState(
    val status: SessionStatus? = null,
    val plannedStartAt: Instant? = null,
    val actualStartAt: Instant? = null,
    val expectedEndAt: Instant? = null,
    val finalAlertScheduledFor: Instant? = null,
)

data class HomeCompletionSummaryUiState(
    val wearDuration: Duration,
    val removedOnTime: Boolean,
    val overdueBy: Duration? = null,
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

private fun WearSession.toCompletionSummaryUiState(): HomeCompletionSummaryUiState? {
    val startedAt = actualStartAt ?: return null
    val completedAt = completedAt ?: return null
    val effectiveDeadlineAt = expectedEndAt?.let { expectedEnd ->
        finalAlertScheduledFor?.let { finalAlert ->
            minOf(expectedEnd, finalAlert)
        } ?: expectedEnd
    } ?: finalAlertScheduledFor
    val wearDuration = Duration.between(startedAt, completedAt).coerceAtLeast(Duration.ZERO)
    val overdueBy = effectiveDeadlineAt?.let { deadline ->
        Duration.between(deadline, completedAt).coerceAtLeast(Duration.ZERO)
    }?.takeUnless(Duration::isZero)

    return HomeCompletionSummaryUiState(
        wearDuration = wearDuration,
        removedOnTime = overdueBy == null,
        overdueBy = overdueBy,
    )
}
