package com.alex.lensesreminder.feature.home

import android.Manifest
import android.app.DatePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alex.lensesreminder.R
import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.notification.ExactAlarmPermissionManager
import com.alex.lensesreminder.core.notification.NotificationPermissionManager
import com.alex.lensesreminder.ui.component.MaterialTimePickerDialog
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeRoute(
    onEditSettings: () -> Unit,
    onPlanSession: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    var hasNotificationPermission by remember {
        mutableStateOf(NotificationPermissionManager.hasNotificationPermission(context))
    }
    var hasExactAlarmPermission by remember {
        mutableStateOf(ExactAlarmPermissionManager.canScheduleExactAlarms(context))
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
    val exactAlarmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasExactAlarmPermission = ExactAlarmPermissionManager.canScheduleExactAlarms(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = NotificationPermissionManager.hasNotificationPermission(context)
                hasExactAlarmPermission = ExactAlarmPermissionManager.canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    HomeScreen(
        uiState = uiState,
        hasNotificationPermission = hasNotificationPermission,
        hasExactAlarmPermission = hasExactAlarmPermission,
        snackbarHostState = snackbarHostState,
        onRequestPermission = {
            viewModel.onNotificationPermissionRequestLaunched()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                hasNotificationPermission = true
            }
        },
        onRequestExactAlarmAccess = {
            ExactAlarmPermissionManager.createRequestIntent(context)?.let(exactAlarmLauncher::launch)
        },
        onStartNow = viewModel::onStartNowClick,
        onStartAt = viewModel::onStartAtClick,
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
    hasExactAlarmPermission: Boolean,
    snackbarHostState: SnackbarHostState,
    onRequestPermission: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onStartNow: () -> Unit,
    onStartAt: (Instant) -> Unit,
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
    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
    val localTimeFormatter = remember {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    }
    val zoneId = remember { ZoneId.systemDefault() }
    val currentTime by rememberCurrentTime()
    var showStartSessionSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = onEditSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.action_edit_settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                ),
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.profile.remindersEnabled && !hasNotificationPermission) {
                NotificationBanner(
                    wasRequested = uiState.notificationsPermissionRequested,
                    onRequestPermission = onRequestPermission,
                )
            }

            if (uiState.profile.remindersEnabled && !hasExactAlarmPermission) {
                ExactAlarmBanner(
                    onRequestExactAlarmAccess = onRequestExactAlarmAccess,
                )
            }

            SessionHeroCard(
                session = uiState.session,
                maxWearMinutes = uiState.profile.maxWearMinutes,
                currentTime = currentTime,
                zoneId = zoneId,
                dateTimeFormatter = dateTimeFormatter,
                onStartNow = { showStartSessionSheet = true },
                onActivatePlannedSession = onActivatePlannedSession,
                onEditPlan = onEditPlan,
                onCancelPlan = onCancelPlan,
                onCompleteSession = onCompleteSession,
            )

            ProfileSummaryCard(
                profile = uiState.profile,
                timeFormatter = timeFormatter,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showStartSessionSheet) {
        StartSessionBottomSheet(
            currentTime = currentTime,
            maxWearMinutes = uiState.profile.maxWearMinutes,
            zoneId = zoneId,
            dateFormatter = dateFormatter,
            timeFormatter = localTimeFormatter,
            onDismiss = { showStartSessionSheet = false },
            onStartNow = {
                showStartSessionSheet = false
                onStartNow()
            },
            onStartAt = { actualStartAt ->
                showStartSessionSheet = false
                onStartAt(actualStartAt)
            },
        )
    }
}

// ── Notification Banner ─────────────────────────────────────────────────────

@Composable
private fun NotificationBanner(
    wasRequested: Boolean,
    onRequestPermission: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(top = 2.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_permission_warning),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = stringResource(
                        if (wasRequested) R.string.home_permission_requested_hint
                        else R.string.home_permission_not_requested_hint
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                FilledTonalButton(
                    onClick = onRequestPermission,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(text = stringResource(R.string.action_enable_reminders))
                }
            }
        }
    }
}

@Composable
private fun ExactAlarmBanner(
    onRequestExactAlarmAccess: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 2.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_exact_alarm_warning),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(R.string.home_exact_alarm_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                FilledTonalButton(
                    onClick = onRequestExactAlarmAccess,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(text = stringResource(R.string.action_enable_exact_alarms))
                }
            }
        }
    }
}

// ── Session Hero Card ───────────────────────────────────────────────────────

@Composable
private fun SessionHeroCard(
    session: HomeSessionUiState,
    maxWearMinutes: Int,
    currentTime: Instant,
    zoneId: ZoneId,
    dateTimeFormatter: DateTimeFormatter,
    onStartNow: () -> Unit,
    onActivatePlannedSession: () -> Unit,
    onEditPlan: () -> Unit,
    onCancelPlan: () -> Unit,
    onCompleteSession: () -> Unit,
) {
    val displaySession = remember(session, currentTime) {
        session.toDisplayState(currentTime)
    }

    when (displaySession.status) {
        null, SessionStatus.COMPLETED, SessionStatus.CANCELLED -> {
            IdleSessionContent(onStartNow = onStartNow)
        }
        SessionStatus.PLANNED -> {
            PlannedSessionContent(
                displaySession = displaySession,
                zoneId = zoneId,
                dateTimeFormatter = dateTimeFormatter,
                onActivate = onActivatePlannedSession,
                onEdit = onEditPlan,
                onCancel = onCancelPlan,
            )
        }
        SessionStatus.ACTIVE -> {
            ActiveSessionContent(
                displaySession = displaySession,
                maxWearMinutes = maxWearMinutes,
                zoneId = zoneId,
                dateTimeFormatter = dateTimeFormatter,
                onComplete = onCompleteSession,
            )
        }
        SessionStatus.OVERDUE -> {
            OverdueSessionContent(
                displaySession = displaySession,
                zoneId = zoneId,
                dateTimeFormatter = dateTimeFormatter,
                onComplete = onCompleteSession,
            )
        }
    }
}

// ── Idle State ──────────────────────────────────────────────────────────────

@Composable
private fun IdleSessionContent(
    onStartNow: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                text = stringResource(R.string.label_lenses_out),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.home_empty_state),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.helper_track_daily_lenses),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.helper_log_earlier_start),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onStartNow,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.action_start_now))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun StartSessionBottomSheet(
    currentTime: Instant,
    maxWearMinutes: Int,
    zoneId: ZoneId,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    onDismiss: () -> Unit,
    onStartNow: () -> Unit,
    onStartAt: (Instant) -> Unit,
) {
    val context = LocalContext.current
    val localNow = remember(currentTime, zoneId) { currentTime.atZone(zoneId) }
    var selectedDate by remember(localNow) { mutableStateOf(localNow.toLocalDate()) }
    var selectedTime by remember(localNow) {
        mutableStateOf(localNow.toLocalTime().withSecond(0).withNano(0))
    }
    var showTimePicker by remember { mutableStateOf(false) }

    val selectedStartAt = remember(selectedDate, selectedTime, zoneId) {
        selectedDate.atTime(selectedTime).atZone(zoneId).toInstant()
    }
    val expectedEndAt = remember(selectedStartAt, maxWearMinutes) {
        selectedStartAt.plus(Duration.ofMinutes(maxWearMinutes.toLong()))
    }
    val isFutureSelection = selectedStartAt.isAfter(currentTime)
    val startsOverdue = !expectedEndAt.isAfter(currentTime)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.home_start_session_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.home_start_session_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = onStartNow,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                Text(text = stringResource(R.string.action_start_now))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.home_started_earlier_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickStartOptionChip(
                        label = stringResource(R.string.home_start_option_15_min_ago),
                        onClick = {
                            onStartAt(currentTime.minus(Duration.ofMinutes(15)))
                        },
                    )
                    QuickStartOptionChip(
                        label = stringResource(R.string.home_start_option_30_min_ago),
                        onClick = {
                            onStartAt(currentTime.minus(Duration.ofMinutes(30)))
                        },
                    )
                    QuickStartOptionChip(
                        label = stringResource(R.string.home_start_option_1_hour_ago),
                        onClick = {
                            onStartAt(currentTime.minus(Duration.ofHours(1)))
                        },
                    )
                }

                Text(
                    text = stringResource(R.string.home_custom_start_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                            },
                            selectedDate.year,
                            selectedDate.monthValue - 1,
                            selectedDate.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            R.string.home_custom_start_date_value,
                            selectedDate.format(dateFormatter)
                        ),
                    )
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            R.string.home_custom_start_time_value,
                            selectedTime.format(timeFormatter)
                        ),
                    )
                }
                if (isFutureSelection) {
                    Text(
                        text = stringResource(R.string.home_custom_start_invalid_future_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (startsOverdue) {
                    Text(
                        text = stringResource(R.string.home_custom_start_overdue_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = { onStartAt(selectedStartAt) },
                    enabled = !isFutureSelection,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    Text(text = stringResource(R.string.action_start_from_selected_time))
                }
            }
        }
    }

    if (showTimePicker) {
        MaterialTimePickerDialog(
            initialTime = selectedTime,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
            title = stringResource(R.string.home_start_session_title),
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                showTimePicker = false
                selectedTime = time.withSecond(0).withNano(0)
            }
        )
    }
}

@Composable
private fun QuickStartOptionChip(
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(text = label) },
    )
}

// ── Planned State ───────────────────────────────────────────────────────────

@Composable
private fun PlannedSessionContent(
    displaySession: DisplaySessionUiState,
    zoneId: ZoneId,
    dateTimeFormatter: DateTimeFormatter,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Text(
                text = stringResource(R.string.label_session_planned),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.state_session_planned),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SessionDetailRow(
                label = stringResource(R.string.label_planned_start),
                value = displaySession.plannedStartAt.format(zoneId, dateTimeFormatter),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onActivate,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text(text = stringResource(R.string.action_lenses_on))
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text(text = stringResource(R.string.action_edit_plan))
                }
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    }
}

// ── Active State ────────────────────────────────────────────────────────────

@Composable
private fun ActiveSessionContent(
    displaySession: DisplaySessionUiState,
    maxWearMinutes: Int,
    zoneId: ZoneId,
    dateTimeFormatter: DateTimeFormatter,
    onComplete: () -> Unit,
) {
    val maxWearDuration = Duration.ofMinutes(maxWearMinutes.toLong())
    val progress = if (maxWearDuration.isZero || displaySession.elapsed == null) {
        0f
    } else {
        displaySession.elapsed.toMillis().toFloat() / maxWearDuration.toMillis().toFloat()
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.label_lenses_in),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.state_session_active),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProgressRing(
                progress = progress,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                progressColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(192.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displaySession.remaining.formatDuration(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.label_remaining_short),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            SessionDetailRow(
                label = stringResource(R.string.label_started_at),
                value = displaySession.actualStartAt.format(zoneId, dateTimeFormatter),
                contentColor = contentColor,
            )
            SessionDetailRow(
                label = stringResource(R.string.label_expected_removal),
                value = displaySession.expectedEndAt.format(zoneId, dateTimeFormatter),
                contentColor = contentColor,
            )
            SessionDetailRow(
                label = stringResource(R.string.label_elapsed_time),
                value = displaySession.elapsed.formatDuration(),
                contentColor = contentColor,
            )
            displaySession.finalAlertScheduledFor?.let { finalAlert ->
                SessionDetailRow(
                    label = stringResource(R.string.label_final_alert_scheduled),
                    value = finalAlert.format(zoneId, dateTimeFormatter),
                    contentColor = contentColor,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                Text(text = stringResource(R.string.action_lenses_off))
            }
        }
    }
}

// ── Overdue State ───────────────────────────────────────────────────────────

@Composable
private fun OverdueSessionContent(
    displaySession: DisplaySessionUiState,
    zoneId: ZoneId,
    dateTimeFormatter: DateTimeFormatter,
    onComplete: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = stringResource(R.string.label_removal_overdue),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = stringResource(R.string.state_session_overdue),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProgressRing(
                progress = 1f,
                trackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                progressColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(192.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displaySession.overdueBy.formatDuration(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(R.string.label_overdue_short),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val contentColor = MaterialTheme.colorScheme.onErrorContainer
            SessionDetailRow(
                label = stringResource(R.string.label_elapsed_time),
                value = displaySession.elapsed.formatDuration(),
                contentColor = contentColor,
            )
            SessionDetailRow(
                label = stringResource(R.string.label_started_at),
                value = displaySession.actualStartAt.format(zoneId, dateTimeFormatter),
                contentColor = contentColor,
            )
            displaySession.finalAlertScheduledFor?.let { finalAlert ->
                SessionDetailRow(
                    label = stringResource(R.string.label_final_alert_scheduled),
                    value = finalAlert.format(zoneId, dateTimeFormatter),
                    contentColor = contentColor,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                Text(text = stringResource(R.string.action_lenses_off))
            }
        }
    }
}

// ── Progress Ring ───────────────────────────────────────────────────────────

@Composable
private fun ProgressRing(
    progress: Float,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    content: @Composable () -> Unit = {},
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900),
        label = "ring_progress",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = minOf(size.width, size.height) - stroke
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

// ── Shared Detail Row ───────────────────────────────────────────────────────

@Composable
private fun SessionDetailRow(
    label: String,
    value: String,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
        )
    }
}

// ── Profile Summary ─────────────────────────────────────────────────────────

@Composable
private fun ProfileSummaryCard(
    profile: LensProfile,
    timeFormatter: DateTimeFormatter,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_profile_summary_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProfileStat(
                    value = profile.maxWearMinutes.formatDuration(),
                    label = stringResource(R.string.label_max_wear),
                )
                ProfileStat(
                    value = profile.dailyStartReminderTime.format(timeFormatter),
                    label = stringResource(R.string.label_daily_start_reminder_time),
                )
                ProfileStat(
                    value = profile.finalAlertTime.format(timeFormatter),
                    label = stringResource(R.string.label_final_alert_time),
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(
    value: String,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

// ── Helpers & data classes ──────────────────────────────────────────────────

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
