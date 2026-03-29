package com.alex.lensesreminder.feature.home

import com.alex.lensesreminder.core.model.SessionStatus
import java.time.Duration
import java.time.Instant

internal data class DisplaySessionUiState(
    val status: SessionStatus? = null,
    val plannedStartAt: Instant? = null,
    val actualStartAt: Instant? = null,
    val expectedEndAt: Instant? = null,
    val effectiveDeadlineAt: Instant? = null,
    val finalAlertScheduledFor: Instant? = null,
    val elapsed: Duration? = null,
    val remaining: Duration? = null,
    val overdueBy: Duration? = null,
)

internal enum class SessionHeroContentKey {
    IDLE,
    COMPLETION_SUMMARY,
    PLANNED,
    ACTIVE,
    OVERDUE,
}

internal sealed interface SessionHeroContentModel {
    val key: SessionHeroContentKey

    data object Idle : SessionHeroContentModel {
        override val key: SessionHeroContentKey = SessionHeroContentKey.IDLE
    }

    data class CompletionSummary(
        val summary: HomeCompletionSummaryUiState,
    ) : SessionHeroContentModel {
        override val key: SessionHeroContentKey = SessionHeroContentKey.COMPLETION_SUMMARY
    }

    data class Planned(
        val displaySession: DisplaySessionUiState,
    ) : SessionHeroContentModel {
        override val key: SessionHeroContentKey = SessionHeroContentKey.PLANNED
    }

    data class Active(
        val displaySession: DisplaySessionUiState,
        val maxWearMinutes: Int,
    ) : SessionHeroContentModel {
        override val key: SessionHeroContentKey = SessionHeroContentKey.ACTIVE
    }

    data class Overdue(
        val displaySession: DisplaySessionUiState,
    ) : SessionHeroContentModel {
        override val key: SessionHeroContentKey = SessionHeroContentKey.OVERDUE
    }
}

internal fun resolveSessionHeroContent(
    displaySession: DisplaySessionUiState,
    completionSummary: HomeCompletionSummaryUiState?,
    maxWearMinutes: Int,
): SessionHeroContentModel = when (displaySession.status) {
    null, SessionStatus.COMPLETED, SessionStatus.CANCELLED -> {
        completionSummary?.let { SessionHeroContentModel.CompletionSummary(it) }
            ?: SessionHeroContentModel.Idle
    }
    SessionStatus.PLANNED -> SessionHeroContentModel.Planned(displaySession)
    SessionStatus.ACTIVE -> SessionHeroContentModel.Active(
        displaySession = displaySession,
        maxWearMinutes = maxWearMinutes,
    )
    SessionStatus.OVERDUE -> SessionHeroContentModel.Overdue(displaySession)
}
