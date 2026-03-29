package com.alex.lensesreminder.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.alex.lensesreminder.core.time.format
import com.alex.lensesreminder.core.time.formatDuration
import com.alex.lensesreminder.feature.home.components.OverviewMetric
import com.alex.lensesreminder.feature.home.components.CountdownRingReadout
import com.alex.lensesreminder.feature.home.components.ProfileMetricTile
import com.alex.lensesreminder.feature.home.components.ProgressRing
import com.alex.lensesreminder.feature.home.components.SessionDetailRow
import com.alex.lensesreminder.feature.home.components.StaggeredVisibility
import com.alex.lensesreminder.feature.home.components.StatusBadge
import com.alex.lensesreminder.ui.component.MaterialTimePickerDialog
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
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
        onDismissCompletionSummary = viewModel::onCompletionSummaryDismissed,
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
    onDismissCompletionSummary: () -> Unit,
    onEditSettings: () -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val timeFormatter = remember {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    }
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    }
    val fullDateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
    }
    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
    val zoneId = remember { ZoneId.systemDefault() }
    val currentTime by rememberCurrentTime()
    var showStartSessionSheet by remember { mutableStateOf(false) }
    val displaySession = remember(uiState.session, currentTime) {
        uiState.session.toDisplayState(currentTime)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lerp(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primaryContainer,
                            if (isDarkTheme) 0.16f else 0.1f,
                        ),
                        lerp(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant,
                            if (isDarkTheme) 0.22f else 0.08f,
                        ),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 72.dp, end = 20.dp)
                .size(if (isDarkTheme) 140.dp else 180.dp)
                .background(
                    color = lerp(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer,
                        if (isDarkTheme) 0.12f else 0.08f,
                    ),
                    shape = CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 160.dp, start = 16.dp)
                .size(if (isDarkTheme) 96.dp else 120.dp)
                .background(
                    color = lerp(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.secondaryContainer,
                        if (isDarkTheme) 0.16f else 0.08f,
                    ),
                    shape = CircleShape,
                ),
        )

        Scaffold(
            containerColor = Color.Transparent,
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
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground,
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
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                HomeOverviewCard(
                    displaySession = displaySession,
                    completionSummary = uiState.completionSummary,
                    profile = uiState.profile,
                    currentTime = currentTime,
                    zoneId = zoneId,
                    fullDateFormatter = fullDateFormatter,
                    timeFormatter = timeFormatter,
                )

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
                    displaySession = displaySession,
                    completionSummary = uiState.completionSummary,
                    maxWearMinutes = uiState.profile.maxWearMinutes,
                    zoneId = zoneId,
                    dateTimeFormatter = dateTimeFormatter,
                    onStartNow = { showStartSessionSheet = true },
                    onPlanForLater = onPlanForLater,
                    onActivatePlannedSession = onActivatePlannedSession,
                    onEditPlan = onEditPlan,
                    onCancelPlan = onCancelPlan,
                    onCompleteSession = onCompleteSession,
                    onDismissCompletionSummary = onDismissCompletionSummary,
                )

                ProfileSummaryCard(
                    profile = uiState.profile,
                    timeFormatter = timeFormatter,
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showStartSessionSheet) {
        StartSessionBottomSheet(
            currentTime = currentTime,
            maxWearMinutes = uiState.profile.maxWearMinutes,
            zoneId = zoneId,
            dateFormatter = dateFormatter,
            timeFormatter = timeFormatter,
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
private fun HomeOverviewCard(
    displaySession: DisplaySessionUiState,
    completionSummary: HomeCompletionSummaryUiState?,
    profile: LensProfile,
    currentTime: Instant,
    zoneId: ZoneId,
    fullDateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
) {
    val overview = resolveHomeOverviewContent(
        displaySession = displaySession,
        completionSummary = completionSummary,
        zoneId = zoneId,
        timeFormatter = timeFormatter,
    )
    val currentDate = remember(currentTime, zoneId) {
        currentTime.atZone(zoneId).toLocalDate()
    }
    val formattedDate = remember(currentDate, fullDateFormatter) {
        currentDate.format(fullDateFormatter)
    }
    val gradientColors = when (overview.key) {
        SessionHeroContentKey.OVERDUE -> {
            listOf(
                lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.errorContainer, 0.62f),
                lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.tertiaryContainer, 0.36f),
            )
        }
        SessionHeroContentKey.ACTIVE -> {
            listOf(
                lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, 0.48f),
                lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.secondaryContainer, 0.22f),
            )
        }
        SessionHeroContentKey.PLANNED -> {
            listOf(
                lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.secondaryContainer, 0.38f),
                MaterialTheme.colorScheme.surface,
            )
        }
        SessionHeroContentKey.COMPLETION_SUMMARY -> {
            listOf(
                lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, 0.34f),
                MaterialTheme.colorScheme.surface,
            )
        }
        SessionHeroContentKey.IDLE -> {
            listOf(
                MaterialTheme.colorScheme.surface,
                lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, 0.2f),
            )
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.linearGradient(gradientColors))
                .padding(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelLarge,
                    color = overview.contentColor.copy(alpha = 0.72f),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_overview_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = overview.accentColor,
                    )
                    Text(
                        text = overview.headline,
                        style = MaterialTheme.typography.headlineSmall,
                        color = overview.contentColor,
                    )
                    Text(
                        text = overview.supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = overview.contentColor.copy(alpha = 0.78f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OverviewMetric(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.label_max_wear),
                        value = profile.maxWearMinutes.formatDuration(),
                        accentColor = overview.accentColor,
                        contentColor = overview.contentColor,
                    )
                    OverviewMetric(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.label_final_alert_time),
                        value = profile.finalAlertTime.format(timeFormatter),
                        accentColor = overview.accentColor,
                        contentColor = overview.contentColor,
                    )
                    OverviewMetric(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.label_reminders_enabled),
                        value = stringResource(
                            if (profile.remindersEnabled) {
                                R.string.label_reminders_on
                            } else {
                                R.string.label_reminders_off
                            }
                        ),
                        accentColor = overview.accentColor,
                        contentColor = overview.contentColor,
                    )
                }
            }
        }
    }
}

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
    displaySession: DisplaySessionUiState,
    completionSummary: HomeCompletionSummaryUiState?,
    maxWearMinutes: Int,
    zoneId: ZoneId,
    dateTimeFormatter: DateTimeFormatter,
    onStartNow: () -> Unit,
    onPlanForLater: () -> Unit,
    onActivatePlannedSession: () -> Unit,
    onEditPlan: () -> Unit,
    onCancelPlan: () -> Unit,
    onCompleteSession: () -> Unit,
    onDismissCompletionSummary: () -> Unit,
) {
    val heroContent = remember(displaySession, completionSummary, maxWearMinutes) {
        resolveSessionHeroContent(
            displaySession = displaySession,
            completionSummary = completionSummary,
            maxWearMinutes = maxWearMinutes,
        )
    }

    AnimatedContent(
        targetState = heroContent,
        transitionSpec = {
            sessionHeroContentTransform(
                initialKey = initialState.key,
                targetKey = targetState.key,
            )
        },
        contentKey = SessionHeroContentModel::key,
        label = "session_hero_content",
    ) { targetContent ->
        when (targetContent) {
            SessionHeroContentModel.Idle -> {
                IdleSessionContent(
                    onStartNow = onStartNow,
                    onPlanForLater = onPlanForLater,
                )
            }
            is SessionHeroContentModel.CompletionSummary -> {
                CompletionSummaryContent(
                    summary = targetContent.summary,
                    onDismiss = onDismissCompletionSummary,
                )
            }
            is SessionHeroContentModel.Planned -> {
                PlannedSessionContent(
                    displaySession = targetContent.displaySession,
                    zoneId = zoneId,
                    dateTimeFormatter = dateTimeFormatter,
                    onActivate = onActivatePlannedSession,
                    onEdit = onEditPlan,
                    onCancel = onCancelPlan,
                )
            }
            is SessionHeroContentModel.Active -> {
                ActiveSessionContent(
                    displaySession = targetContent.displaySession,
                    maxWearMinutes = targetContent.maxWearMinutes,
                    zoneId = zoneId,
                    dateTimeFormatter = dateTimeFormatter,
                    onComplete = onCompleteSession,
                )
            }
            is SessionHeroContentModel.Overdue -> {
                OverdueSessionContent(
                    displaySession = targetContent.displaySession,
                    zoneId = zoneId,
                    dateTimeFormatter = dateTimeFormatter,
                    onComplete = onCompleteSession,
                )
            }
        }
    }
}

private const val COMPLETION_SUMMARY_AUTO_DISMISS_MILLIS = 8_000L

// ── Idle State ──────────────────────────────────────────────────────────────

@Composable
private fun CompletionSummaryContent(
    summary: HomeCompletionSummaryUiState,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(summary) {
        delay(COMPLETION_SUMMARY_AUTO_DISMISS_MILLIS)
        onDismiss()
    }

    val cardColors = if (summary.removedOnTime) {
        CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
    } else {
        CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        )
    }
    val accentColor = if (summary.removedOnTime) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val contentColor = if (summary.removedOnTime) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    val messageText = if (summary.removedOnTime) {
        stringResource(R.string.home_completion_message_on_time)
    } else {
        stringResource(
            R.string.home_completion_message_overdue,
            summary.overdueBy.formatDuration()
        )
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StaggeredVisibility(delayMillis = 0) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = accentColor.copy(alpha = 0.14f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (summary.removedOnTime) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.Warning
                        },
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = accentColor,
                    )
                }
            }

            StaggeredVisibility(delayMillis = 70) {
                Text(
                    text = stringResource(R.string.label_session_complete),
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                )
            }
            StaggeredVisibility(delayMillis = 140) {
                Text(
                    text = stringResource(R.string.home_completion_summary_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                )
            }

            StaggeredVisibility(delayMillis = 210) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SessionDetailRow(
                        label = stringResource(R.string.home_completion_total_wear_label),
                        value = summary.wearDuration.formatDuration(),
                        contentColor = contentColor,
                    )
                    SessionDetailRow(
                        label = stringResource(R.string.home_completion_status_label),
                        value = stringResource(
                            if (summary.removedOnTime) {
                                R.string.home_completion_status_on_time
                            } else {
                                R.string.home_completion_status_overdue
                            }
                        ),
                        contentColor = contentColor,
                    )
                }
            }

            StaggeredVisibility(delayMillis = 280) {
                Text(
                    text = messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                )
            }

            StaggeredVisibility(delayMillis = 350, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    Text(text = stringResource(R.string.action_got_it))
                }
            }
        }
    }
}

@Composable
private fun IdleSessionContent(
    onStartNow: () -> Unit,
    onPlanForLater: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, 0.22f),
                        )
                    ),
                )
                .padding(24.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                StaggeredVisibility(delayMillis = 0) {
                    StatusBadge(
                        text = stringResource(R.string.label_lenses_out),
                        containerColor = lerp(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primaryContainer,
                            0.58f,
                        ),
                        contentColor = MaterialTheme.colorScheme.primary,
                    )
                }
                StaggeredVisibility(delayMillis = 70) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.home_idle_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.home_idle_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                StaggeredVisibility(delayMillis = 150) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.helper_track_daily_lenses),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.helper_log_earlier_start),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                StaggeredVisibility(delayMillis = 230, modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onStartNow,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 16.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(R.string.action_start_now))
                        }
                        OutlinedButton(
                            onClick = onPlanForLater,
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            contentPadding = PaddingValues(vertical = 16.dp),
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(R.string.action_plan_for_later))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val initialSelection = remember(currentTime, zoneId) {
        currentTime.atZone(zoneId).toLocalDateTime().withSecond(0).withNano(0)
    }
    var selectedDate by remember(initialSelection) { mutableStateOf(initialSelection.toLocalDate()) }
    var selectedTime by remember(initialSelection) { mutableStateOf(initialSelection.toLocalTime()) }
    var showDatePicker by remember { mutableStateOf(false) }
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
                Text(
                    text = stringResource(R.string.home_custom_start_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
            initialDisplayMode = DisplayMode.Picker,
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                            selectedDate = Instant
                                .ofEpochMilli(selectedDateMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
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
    val isDarkTheme = isSystemInDarkTheme()
    val maxWearDuration = Duration.ofMinutes(maxWearMinutes.toLong())
    val countdownMetrics = remember(displaySession.remaining) {
        displaySession.remaining.toCountdownRingMetrics()
    }
    val heroCardBrush = if (isDarkTheme) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer,
                lerp(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.surface,
                    0.18f,
                ),
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, 0.42f),
                lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, 0.78f),
                lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.tertiaryContainer, 0.34f),
            ),
        )
    }
    val progress = if (maxWearDuration.isZero || displaySession.elapsed == null) {
        0f
    } else {
        displaySession.elapsed.toMillis().toFloat() / maxWearDuration.toMillis().toFloat()
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.Transparent,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroCardBrush),
        ) {
            if (!isDarkTheme) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 44.dp, y = (-52).dp)
                        .size(196.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.26f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-26).dp, y = 26.dp)
                        .size(148.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
            }

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
                    modifier = Modifier.size(208.dp),
                    strokeWidth = 12.dp,
                ) {
                    CountdownRingReadout(
                        metrics = countdownMetrics,
                        statusLabel = stringResource(R.string.label_remaining_short),
                        accentColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(150.dp),
                    )
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
}

// ── Overdue State ───────────────────────────────────────────────────────────

@Composable
private fun OverdueSessionContent(
    displaySession: DisplaySessionUiState,
    zoneId: ZoneId,
    dateTimeFormatter: DateTimeFormatter,
    onComplete: () -> Unit,
) {
    val countdownMetrics = remember(displaySession.overdueBy) {
        displaySession.overdueBy.toCountdownRingMetrics()
    }

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
                modifier = Modifier.size(208.dp),
                strokeWidth = 12.dp,
            ) {
                CountdownRingReadout(
                    metrics = countdownMetrics,
                    statusLabel = stringResource(R.string.label_overdue_short),
                    accentColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(150.dp),
                )
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

@Composable
private fun ProfileSummaryCard(
    profile: LensProfile,
    timeFormatter: DateTimeFormatter,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant, 0.72f),
                        ),
                    ),
                )
                .padding(18.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.home_profile_summary_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.home_profile_summary_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProfileMetricTile(
                        modifier = Modifier.weight(1f),
                        value = profile.maxWearMinutes.formatDuration(),
                        label = stringResource(R.string.label_max_wear),
                    )
                    ProfileMetricTile(
                        modifier = Modifier.weight(1f),
                        value = profile.dailyStartReminderTime.format(timeFormatter),
                        label = stringResource(R.string.label_daily_start_reminder_time),
                    )
                    ProfileMetricTile(
                        modifier = Modifier.weight(1f),
                        value = profile.finalAlertTime.format(timeFormatter),
                        label = stringResource(R.string.label_final_alert_time),
                    )
                }
            }
        }
    }
}

private fun sessionHeroContentTransform(
    initialKey: SessionHeroContentKey,
    targetKey: SessionHeroContentKey,
): ContentTransform {
    val settleFromLiveSession = initialKey.isLiveSession() && targetKey.isSettledState()
    val enterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = if (settleFromLiveSession) 320 else 220,
            delayMillis = if (settleFromLiveSession) 90 else 40,
        ),
    ) + scaleIn(
        initialScale = if (settleFromLiveSession) 0.96f else 0.99f,
        animationSpec = tween(
            durationMillis = if (settleFromLiveSession) 320 else 220,
            easing = FastOutSlowInEasing,
        ),
    )
    val exitTransition = fadeOut(
        animationSpec = tween(durationMillis = if (settleFromLiveSession) 180 else 160),
    ) + if (settleFromLiveSession) {
        scaleOut(
            targetScale = 0.98f,
            animationSpec = tween(
                durationMillis = 180,
                easing = FastOutSlowInEasing,
            ),
        )
    } else {
        ExitTransition.None
    }

    return enterTransition.togetherWith(exitTransition)
}

private fun SessionHeroContentKey.isLiveSession(): Boolean {
    return this == SessionHeroContentKey.ACTIVE || this == SessionHeroContentKey.OVERDUE
}

private fun SessionHeroContentKey.isSettledState(): Boolean {
    return this == SessionHeroContentKey.IDLE || this == SessionHeroContentKey.COMPLETION_SUMMARY
}

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
