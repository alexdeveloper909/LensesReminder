package com.alex.lensesreminder.feature.onboarding

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.lensesreminder.R
import com.alex.lensesreminder.feature.settings.SettingsEditorScreen
import com.alex.lensesreminder.feature.settings.SettingsEvent
import com.alex.lensesreminder.feature.settings.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * First-run onboarding flow that captures the initial lens profile.
 */
@Composable
fun OnboardingRoute(
    onSaved: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                SettingsEvent.ProfileSaved -> onSaved()
                is SettingsEvent.ValidationError -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(event.messageId)
                    )
                }
            }
        }
    }

    SettingsEditorScreen(
        title = stringResource(R.string.screen_setup_title),
        actionLabel = stringResource(R.string.action_save_and_continue),
        showBackAction = false,
        onBack = {},
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onMaxWearHoursChanged = viewModel::onMaxWearHoursChanged,
        onMaxWearMinutesChanged = viewModel::onMaxWearMinutesChanged,
        onRemindersEnabledChanged = viewModel::onRemindersEnabledChanged,
        onFinalAlertTimeChanged = viewModel::onFinalAlertTimeChanged,
        onDailyStartReminderTimeChanged = viewModel::onDailyStartReminderTimeChanged,
        onSaveClick = { viewModel.saveProfile(completeOnboarding = true) },
    )
}
