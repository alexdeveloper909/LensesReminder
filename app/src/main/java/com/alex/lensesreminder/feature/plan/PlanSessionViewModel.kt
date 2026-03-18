package com.alex.lensesreminder.feature.plan

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.lensesreminder.R
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.time.LensClock
import com.alex.lensesreminder.data.repository.LensProfileRepository
import com.alex.lensesreminder.data.repository.WearSessionRepository
import com.alex.lensesreminder.domain.session.SessionLifecycleFailure
import com.alex.lensesreminder.domain.session.SessionLifecycleManager
import com.alex.lensesreminder.domain.session.SessionLifecycleResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * View-model for creating or editing the single planned session.
 */
@HiltViewModel
class PlanSessionViewModel @Inject constructor(
    lensProfileRepository: LensProfileRepository,
    wearSessionRepository: WearSessionRepository,
    private val sessionLifecycleManager: SessionLifecycleManager,
    private val clock: LensClock,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PlanSessionUiState())
    private val mutableEvents = MutableSharedFlow<PlanSessionEvent>()
    private var hasLoadedDraft = false

    val uiState = mutableUiState.asStateFlow()
    val events = mutableEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                lensProfileRepository.profile,
                wearSessionRepository.currentSession
            ) { profile, session ->
                profile to session
            }.collectLatest { (profile, session) ->
                val existingPlan = session?.takeIf { it.status == SessionStatus.PLANNED }
                if (!hasLoadedDraft) {
                    val localStart = (existingPlan?.plannedStartAt ?: defaultPlannedStart())
                        .atZone(clock.zoneId())
                    mutableUiState.value = PlanSessionUiState(
                        isLoading = false,
                        selectedDate = localStart.toLocalDate(),
                        selectedTime = localStart.toLocalTime().withSecond(0).withNano(0),
                        remindersEnabled = profile.remindersEnabled,
                        finalAlertTime = profile.finalAlertTime,
                        isEditingExistingPlan = existingPlan != null
                    )
                    hasLoadedDraft = true
                } else {
                    mutableUiState.update {
                        it.copy(
                            isLoading = false,
                            remindersEnabled = profile.remindersEnabled,
                            finalAlertTime = profile.finalAlertTime,
                            isEditingExistingPlan = existingPlan != null
                        )
                    }
                }
            }
        }
    }

    fun onDateChanged(
        date: LocalDate,
    ) {
        mutableUiState.update { it.copy(selectedDate = date) }
    }

    fun onTimeChanged(
        time: LocalTime,
    ) {
        mutableUiState.update { it.copy(selectedTime = time.withSecond(0).withNano(0)) }
    }

    fun savePlan() {
        viewModelScope.launch {
            val currentState = mutableUiState.value
            val plannedStartAt = currentState.selectedDate
                .atTime(currentState.selectedTime)
                .atZone(clock.zoneId())
                .toInstant()

            when (val result = sessionLifecycleManager.savePlannedSession(plannedStartAt)) {
                is SessionLifecycleResult.Success -> {
                    mutableEvents.emit(PlanSessionEvent.PlanSaved)
                }
                is SessionLifecycleResult.Failure -> {
                    mutableEvents.emit(
                        PlanSessionEvent.ValidationError(
                            messageId = result.reason.toMessageId()
                        )
                    )
                }
            }
        }
    }

    private fun defaultPlannedStart(): Instant {
        val localNow = clock.now().atZone(clock.zoneId())
        val roundedMinutes = ((localNow.minute / 15) + 1) * 15
        val roundedStart = localNow
            .withSecond(0)
            .withNano(0)
            .withMinute(0)
            .plusHours((roundedMinutes / 60).toLong())
            .plusMinutes((roundedMinutes % 60).toLong())

        return roundedStart.toInstant()
    }
}

data class PlanSessionUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTime: LocalTime = LocalTime.of(9, 0),
    val remindersEnabled: Boolean = true,
    val finalAlertTime: LocalTime = LocalTime.of(22, 0),
    val isEditingExistingPlan: Boolean = false,
)

sealed interface PlanSessionEvent {
    data object PlanSaved : PlanSessionEvent

    data class ValidationError(
        @param:StringRes val messageId: Int,
    ) : PlanSessionEvent
}

private fun SessionLifecycleFailure.toMessageId(): Int = when (this) {
    SessionLifecycleFailure.EXISTING_OPEN_SESSION -> R.string.error_only_one_active_session
    SessionLifecycleFailure.INVALID_ACTUAL_START -> R.string.error_invalid_actual_start
    SessionLifecycleFailure.INVALID_PLANNED_TIME -> R.string.error_invalid_planned_start
    SessionLifecycleFailure.PLANNED_SESSION_NOT_FOUND,
    SessionLifecycleFailure.ACTIVE_SESSION_NOT_FOUND,
    -> R.string.error_session_action_unavailable
}
