package com.alex.lensesreminder.feature.plan

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.lensesreminder.R
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.collectLatest

/**
 * Route for planning a future lens session.
 */
@Composable
fun PlanSessionRoute(
    onDone: () -> Unit,
    viewModel: PlanSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                PlanSessionEvent.PlanSaved -> onDone()
                is PlanSessionEvent.ValidationError -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(event.messageId)
                    )
                }
            }
        }
    }

    PlanSessionScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onDone,
        onDateChanged = viewModel::onDateChanged,
        onTimeChanged = viewModel::onTimeChanged,
        onSave = viewModel::savePlan
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanSessionScreen(
    uiState: PlanSessionUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onDateChanged: (java.time.LocalDate) -> Unit,
    onTimeChanged: (java.time.LocalTime) -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
    val timeFormatter = remember {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.isEditingExistingPlan) {
                                R.string.screen_edit_plan_title
                            } else {
                                R.string.screen_plan_session_title
                            }
                        )
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.plan_session_intro_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.plan_session_intro_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    onDateChanged(
                                        java.time.LocalDate.of(year, month + 1, dayOfMonth)
                                    )
                                },
                                uiState.selectedDate.year,
                                uiState.selectedDate.monthValue - 1,
                                uiState.selectedDate.dayOfMonth
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(
                                R.string.plan_session_date_value,
                                uiState.selectedDate.format(dateFormatter)
                            )
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    onTimeChanged(java.time.LocalTime.of(hour, minute))
                                },
                                uiState.selectedTime.hour,
                                uiState.selectedTime.minute,
                                DateFormat.is24HourFormat(context)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(
                                R.string.plan_session_time_value,
                                uiState.selectedTime.format(timeFormatter)
                            )
                        )
                    }
                    Text(
                        text = stringResource(
                            if (uiState.remindersEnabled) {
                                R.string.plan_session_reminders_on
                            } else {
                                R.string.plan_session_reminders_off
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(
                            R.string.plan_session_final_alert_value,
                            uiState.finalAlertTime.format(timeFormatter)
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.action_save_plan))
            }
        }
    }
}
