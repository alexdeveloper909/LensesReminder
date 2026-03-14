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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.alex.lensesreminder.core.notification.NotificationPermissionManager
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Home screen route for the foundation phase.
 */
@Composable
fun HomeRoute(
    onEditSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(NotificationPermissionManager.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        hasNotificationPermission = NotificationPermissionManager.hasNotificationPermission(context)
    }

    HomeScreen(
        uiState = uiState,
        hasNotificationPermission = hasNotificationPermission,
        onRequestPermission = {
            viewModel.onNotificationPermissionRequestLaunched()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                hasNotificationPermission = true
            }
        },
        onEditSettings = onEditSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit,
    onEditSettings: () -> Unit,
) {
    val timeFormatter = remember {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    }

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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_lenses_out),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.home_empty_state),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.helper_track_daily_lenses),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

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
                        text = stringResource(R.string.home_profile_summary_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    SummaryRow(
                        label = stringResource(R.string.label_lens_type),
                        value = stringResource(R.string.label_daily_lenses)
                    )
                    SummaryRow(
                        label = stringResource(R.string.label_maximum_wear_duration),
                        value = stringResource(
                            R.string.label_minutes_suffix,
                            uiState.profile.maxWearMinutes
                        )
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
private fun SummaryRow(
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
