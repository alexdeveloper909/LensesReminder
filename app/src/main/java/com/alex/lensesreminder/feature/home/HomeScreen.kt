package com.alex.lensesreminder.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.lensesreminder.R
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.notification.NotificationPermissionManager
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * Home screen route for the session lifecycle phase.
 */
@Composable
fun HomeRoute(
    onEditSettings: () -> Unit,
    onPlanSession: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var hasNotificationPermission by remember {
        mutableStateOf(NotificationPermissionManager.hasNotificationPermission(context))
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HomeEvent.Message -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(event.messageId)
                    )
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        hasNotificationPermission = NotificationPermissionManager.hasNotificationPermission(context)
    }

    HomeScreen(
        uiState = uiState,
        hasNotificationPermission = hasNotificationPermission,
        snackbarHostState = snackbarHostState,
        onRequestPermission = {
            viewModel.onNotificationPermissionRequestLaunched()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                hasNotificationPermission = true
            }
        },
        onStartNow = viewModel::onStartNowClick,
        onPlanForLater = onPlanSession,
        onActivatePlannedSession = viewModel::onActivatePlannedSessionClick,
        onEditPlan = onPlanSession,
        onCancelPlan = viewModel::onCancelPlannedSessionClick,
        onCompleteSession = viewModel::onCompleteSessionClick,
        onEditSettings = onEditSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    hasNotificationPermission: Boolean,
    snackbarHostState: SnackbarHostState,
    onRequestPermission: () -> Unit,
    onStartNow: () -> Unit,
    onPlanForLater: () -> Unit,
    onActivatePlannedSession: () -> Unit,
    onEditPlan: () -> Unit,
    onCancelPlan: () -> Unit,
    onCompleteSession: () -> Unit,
    onEditSettings: () -> Unit,
) {
    val timeFormatter = remember {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    }
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    }
    val zoneId = remember { ZoneId.systemDefault() }
    val currentTime by rememberCurrentTime()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onEditSettings) {
                        Text(text = stringResource(R.string.action_edit_settings))
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.profile.remindersEnabled && !hasNotificationPermission) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_permission_warning),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(
                                if (uiState.notificationsPermissionRequested) {
                                    R.string.home_permission_requested_hint
                                } else {
                                    R.string.home_permission_not_requested_hint
                                }
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(text = stringResource(R.string.action_enable_reminders))
                        }
                    }
                }
            }

            SessionCard(
                session = uiState.session,
                currentTime = currentTime,
                zoneId = zoneId,
                dateTimeFormatter = dateTimeFormatter,
                onStartNow = onStartNow,
                onPlanForLater = onPlanForLater,
                onActivatePlannedSession = onActivatePlannedSession,
                onEditPlan = onEditPlan,
                onCancelPlan = onCancelPlan,
                onCompleteSession = onCompleteSession
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_profile_summary_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.home_profile_summary_body_phase_2),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    SummaryRow(
                        label = stringResource(R.string.label_lens_type),
                        value = stringResource(R.string.label_daily_lenses)
                    )
                    SummaryRow(
                        label = stringResource(R.string.label_maximum_wear_duration),
                        value = uiState.profile.maxWearMinutes.formatDuration()
                    )
                    SummaryRow(
                        label = stringResource(R.string.label_reminders_enabled),
                        value = if (uiState.profile.remindersEnabled) {
                            stringResource(R.string.label_reminders_on)
                        } else {
                            stringResource(R.string.label_reminders_off)
                        }
                    )
                    SummaryRow(
                        label = stringResource(R.string.label_final_alert_time),
                        value = uiState.profile.finalAlertTime.format(timeFormatter)
                    )
                    SummaryRow(
                        label = stringResource(R.string.label_overdue_interval),
                        value = stringResource(R.string.label_every_15_minutes)
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: HomeSessionUiState,
    currentTime: Instant,
    zoneId: ZoneId,
    dateTimeFormatter: DateTimeFormatter,
    onStartNow: () -> Unit,
    onPlanForLater: () -> Unit,
    onActivatePlannedSession: () -> Unit,
    onEditPlan: () -> Unit,
    onCancelPlan: () -> Unit,
    onCompleteSession: () -> Unit,
) {
    val displaySession = remember(session, currentTime) {
        session.toDisplayState(currentTime)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (displaySession.status) {
                null -> {
                    SessionHeader(
                        status = stringResource(R.string.label_lenses_out),
                        headline = stringResource(R.string.home_empty_state)
                    )
                    Text(
                        text = stringResource(R.string.helper_track_daily_lenses),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = onStartNow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.action_start_now))
                    }
                    OutlinedButton(
                        onClick = onPlanForLater,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.action_plan_for_later))
                    }
                }

                SessionStatus.PLANNED -> {
                    SessionHeader(
                        status = stringResource(R.string.label_session_planned),
                        headline = stringResource(R.string.state_session_planned)
                    )
                    SessionDetailRow(
                        label = stringResource(R.string.label_next_event),
                        value = displaySession.plannedStartAt.format(zoneId, dateTimeFormatter)
                    )
                    SessionDetailRow(
                        label = stringResource(R.string.label_planned_start),
                        value = displaySession.plannedStartAt.format(zoneId, dateTimeFormatter)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onActivatePlannedSession,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(R.string.action_lenses_on))
                        }
                        OutlinedButton(
                            onClick = onEditPlan,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(R.string.action_edit_plan))
                        }
                    }
                    TextButton(
                        onClick = onCancelPlan,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                }

                SessionStatus.ACTIVE -> {
                    SessionHeader(
                        status = stringResource(R.string.label_lenses_in),
                        headline = stringResource(R.string.state_session_active)
                    )
                    SessionTimingSection(
                        session = displaySession,
                        zoneId = zoneId,
                        dateTimeFormatter = dateTimeFormatter
                    )
                    Button(
                        onClick = onCompleteSession,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.action_lenses_off))
                    }
                }

                SessionStatus.OVERDUE -> {
                    SessionHeader(
                        status = stringResource(R.string.label_removal_overdue),
                        headline = stringResource(R.string.state_session_overdue)
                    )
                    SessionTimingSection(
                        session = displaySession,
                        zoneId = zoneId,
                        dateTimeFormatter = dateTimeFormatter
                    )
                    Button(
                        onClick = onCompleteSession,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.action_lenses_off))
                    }
                }

                SessionStatus.COMPLETED,
                SessionStatus.CANCELLED,
                -> Unit
            }
        }
    }
}

@Composable
private fun SessionHeader(
    status: String,
    headline: String,
) {
    Text(
        text = status,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = headline,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SessionTimingSection(
    session: DisplaySessionUiState,
    zoneId: ZoneId,
    dateTimeFormatter: DateTimeFormatter,
) {
    SessionDetailRow(
        label = stringResource(R.string.label_started_at),
        value = session.actualStartAt.format(zoneId, dateTimeFormatter)
    )
    SessionDetailRow(
        label = stringResource(R.string.label_expected_removal),
        value = session.expectedEndAt.format(zoneId, dateTimeFormatter)
    )
    SessionDetailRow(
        label = stringResource(R.string.label_elapsed_time),
        value = session.elapsed.formatDuration()
    )
    if (session.status == SessionStatus.ACTIVE) {
        SessionDetailRow(
            label = stringResource(R.string.label_remaining_time),
            value = session.remaining.formatDuration()
        )
    }
    if (session.status == SessionStatus.OVERDUE) {
        SessionDetailRow(
            label = stringResource(R.string.label_overdue_by),
            value = session.overdueBy.formatDuration()
        )
    }
    session.finalAlertScheduledFor?.let { finalAlert ->
        SessionDetailRow(
            label = stringResource(R.string.label_final_alert_scheduled),
            value = finalAlert.format(zoneId, dateTimeFormatter)
        )
    }
}

@Composable
private fun SessionDetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    SessionDetailRow(label = label, value = value)
}

private fun Instant?.format(
    zoneId: ZoneId,
    formatter: DateTimeFormatter,
): String = this?.atZone(zoneId)?.format(formatter).orEmpty()

private fun Duration?.formatDuration(): String {
    if (this == null) {
        return "--"
    }

    val totalMinutes = toMinutes()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

private fun Int.formatDuration(): String {
    val hours = this / 60
    val minutes = this % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

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

internal fun HomeSessionUiState.toDisplayState(
    currentTime: Instant,
): DisplaySessionUiState {
    val effectiveDeadlineAt = expectedEndAt?.let { expectedEnd ->
        finalAlertScheduledFor?.let { finalAlert ->
            minOf(expectedEnd, finalAlert)
        } ?: expectedEnd
    } ?: finalAlertScheduledFor

    val effectiveStatus = if (
        status == SessionStatus.ACTIVE &&
        effectiveDeadlineAt != null &&
        !currentTime.isBefore(effectiveDeadlineAt)
    ) {
        SessionStatus.OVERDUE
    } else {
        status
    }

    return DisplaySessionUiState(
        status = effectiveStatus,
        plannedStartAt = plannedStartAt,
        actualStartAt = actualStartAt,
        expectedEndAt = effectiveDeadlineAt,
        effectiveDeadlineAt = effectiveDeadlineAt,
        finalAlertScheduledFor = finalAlertScheduledFor,
        elapsed = actualStartAt?.let {
            Duration.between(it, currentTime).coerceAtLeast(Duration.ZERO)
        },
        remaining = if (effectiveStatus == SessionStatus.ACTIVE && effectiveDeadlineAt != null) {
            Duration.between(currentTime, effectiveDeadlineAt).coerceAtLeast(Duration.ZERO)
        } else {
            null
        },
        overdueBy = if (effectiveStatus == SessionStatus.OVERDUE && effectiveDeadlineAt != null) {
            Duration.between(effectiveDeadlineAt, currentTime).coerceAtLeast(Duration.ZERO)
        } else {
            null
        }
    )
}

@Composable
private fun rememberCurrentTime(): State<Instant> = produceState(initialValue = Instant.now()) {
    while (true) {
        value = Instant.now()
        delay(1_000)
    }
}
