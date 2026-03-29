package com.alex.lensesreminder.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.alex.lensesreminder.R
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.time.format
import com.alex.lensesreminder.core.time.formatDuration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal data class HomeOverviewContent(
    val key: SessionHeroContentKey,
    val headline: String,
    val supportingText: String,
    val accentColor: Color,
    val contentColor: Color,
)

@Composable
internal fun resolveHomeOverviewContent(
    displaySession: DisplaySessionUiState,
    completionSummary: HomeCompletionSummaryUiState?,
    zoneId: ZoneId,
    timeFormatter: DateTimeFormatter,
): HomeOverviewContent {
    return when (displaySession.status) {
        SessionStatus.PLANNED -> HomeOverviewContent(
            key = SessionHeroContentKey.PLANNED,
            headline = stringResource(R.string.state_session_planned),
            supportingText = displaySession.plannedStartAt?.let {
                stringResource(R.string.label_planned_start) + " " + it.format(zoneId, timeFormatter)
            } ?: stringResource(R.string.home_empty_state),
            accentColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        SessionStatus.ACTIVE -> HomeOverviewContent(
            key = SessionHeroContentKey.ACTIVE,
            headline = stringResource(R.string.state_session_active),
            supportingText = stringResource(R.string.label_remaining_time) + " " + displaySession.remaining.formatDuration(),
            accentColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        SessionStatus.OVERDUE -> HomeOverviewContent(
            key = SessionHeroContentKey.OVERDUE,
            headline = stringResource(R.string.state_session_overdue),
            supportingText = stringResource(R.string.label_overdue_by) + " " + displaySession.overdueBy.formatDuration(),
            accentColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
        SessionStatus.COMPLETED, SessionStatus.CANCELLED, null -> {
            if (completionSummary != null) {
                val supportingText = if (completionSummary.removedOnTime) {
                    stringResource(R.string.home_completion_message_on_time)
                } else {
                    stringResource(
                        R.string.home_completion_message_overdue,
                        completionSummary.overdueBy.formatDuration(),
                    )
                }
                HomeOverviewContent(
                    key = SessionHeroContentKey.COMPLETION_SUMMARY,
                    headline = stringResource(R.string.home_completion_summary_title),
                    supportingText = supportingText,
                    accentColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                HomeOverviewContent(
                    key = SessionHeroContentKey.IDLE,
                    headline = stringResource(R.string.home_empty_state),
                    supportingText = stringResource(R.string.helper_track_daily_lenses),
                    accentColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
