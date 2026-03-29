package com.alex.lensesreminder.feature.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.lensesreminder.R
import com.alex.lensesreminder.core.notification.ExactAlarmPermissionManager
import com.alex.lensesreminder.ui.component.MaterialTimePickerDialog
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsRoute(
    onDone: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    SettingsEditorScreen(
        title = stringResource(R.string.screen_settings_title),
        actionLabel = stringResource(R.string.action_done),
        showBackAction = true,
        onBack = onDone,
        onSaveSuccess = onDone,
        completeOnboarding = false,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsEditorScreen(
    title: String,
    actionLabel: String,
    showBackAction: Boolean,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    completeOnboarding: Boolean,
    viewModel: SettingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                SettingsEvent.ProfileSaved -> onSaveSuccess()
                is SettingsEvent.ValidationError -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(event.messageId)
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (showBackAction) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_cancel),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        SettingsEditorContent(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            actionLabel = actionLabel,
            onMaxWearHoursChanged = viewModel::onMaxWearHoursChanged,
            onMaxWearMinutesChanged = viewModel::onMaxWearMinutesChanged,
            onRemindersEnabledChanged = viewModel::onRemindersEnabledChanged,
            onFinalAlertTimeChanged = viewModel::onFinalAlertTimeChanged,
            onDailyStartReminderTimeChanged = viewModel::onDailyStartReminderTimeChanged,
            onSaveClick = { viewModel.saveProfile(completeOnboarding = completeOnboarding) }
        )
    }
}

@Composable
private fun SettingsEditorContent(
    modifier: Modifier = Modifier,
    uiState: SettingsUiState,
    actionLabel: String,
    onMaxWearHoursChanged: (String) -> Unit,
    onMaxWearMinutesChanged: (String) -> Unit,
    onRemindersEnabledChanged: (Boolean) -> Unit,
    onFinalAlertTimeChanged: (java.time.LocalTime) -> Unit,
    onDailyStartReminderTimeChanged: (java.time.LocalTime) -> Unit,
    onSaveClick: () -> Unit,
) {
    val context = LocalContext.current
    var hasExactAlarmPermission by remember {
        mutableStateOf(ExactAlarmPermissionManager.canScheduleExactAlarms(context))
    }
    val timeFormatter = remember {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    }
    var showFinalAlertTimePicker by remember { mutableStateOf(false) }
    var showDailyStartTimePicker by remember { mutableStateOf(false) }
    val exactAlarmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasExactAlarmPermission = ExactAlarmPermissionManager.canScheduleExactAlarms(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.helper_track_daily_lenses),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.helper_choose_max_wear_time),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.helper_notification_permission),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.remindersEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_exact_alarm_warning),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.settings_exact_alarm_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    OutlinedButton(
                        onClick = {
                            ExactAlarmPermissionManager.createRequestIntent(context)
                                ?.let(exactAlarmLauncher::launch)
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(text = stringResource(R.string.action_enable_exact_alarms))
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.label_lens_type),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = lerp(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceVariant,
                    0.72f,
                ),
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LabeledValue(
                    label = stringResource(R.string.label_lens_type),
                    value = stringResource(R.string.label_daily_lenses),
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = stringResource(R.string.label_maximum_wear_duration),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.helper_choose_max_wear_time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.maxWearHoursInput,
                        onValueChange = onMaxWearHoursChanged,
                        label = { Text(text = stringResource(R.string.label_hours)) },
                        placeholder = { Text(text = "12") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.maxWearMinutesInput,
                        onValueChange = onMaxWearMinutesChanged,
                        label = { Text(text = stringResource(R.string.label_minutes)) },
                        placeholder = { Text(text = "0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.label_reminders_enabled),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = lerp(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceVariant,
                    0.72f,
                ),
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.label_reminders_enabled),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.helper_set_final_alert_time),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = uiState.remindersEnabled,
                        onCheckedChange = onRemindersEnabledChanged
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = stringResource(R.string.label_final_alert_time),
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedButton(
                    onClick = { showFinalAlertTimePicker = true },
                    enabled = uiState.remindersEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            R.string.label_time_value,
                            uiState.finalAlertTime.format(timeFormatter)
                        )
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = stringResource(R.string.label_daily_start_reminder_time),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.helper_set_daily_start_reminder_time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { showDailyStartTimePicker = true },
                    enabled = uiState.remindersEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            R.string.label_time_value,
                            uiState.dailyStartReminderTime.format(timeFormatter)
                        )
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                LabeledValue(
                    label = stringResource(R.string.label_overdue_interval),
                    value = stringResource(R.string.label_every_15_minutes),
                )
                Text(
                    text = stringResource(R.string.helper_repeated_reminders_stop),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showFinalAlertTimePicker) {
        MaterialTimePickerDialog(
            initialTime = uiState.finalAlertTime,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
            title = stringResource(R.string.label_final_alert_time),
            onDismiss = { showFinalAlertTimePicker = false },
            onConfirm = { selectedTime ->
                showFinalAlertTimePicker = false
                onFinalAlertTimeChanged(selectedTime)
            }
        )
    }

    if (showDailyStartTimePicker) {
        MaterialTimePickerDialog(
            initialTime = uiState.dailyStartReminderTime,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
            title = stringResource(R.string.label_daily_start_reminder_time),
            onDismiss = { showDailyStartTimePicker = false },
            onConfirm = { selectedTime ->
                showDailyStartTimePicker = false
                onDailyStartReminderTimeChanged(selectedTime)
            }
        )
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
