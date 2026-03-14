package com.alex.lensesreminder.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.alex.lensesreminder.R
import com.alex.lensesreminder.feature.settings.SettingsEditorScreen
import com.alex.lensesreminder.feature.settings.SettingsViewModel

/**
 * First-run onboarding flow that captures the initial lens profile.
 */
@Composable
fun OnboardingRoute(
    onSaved: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    SettingsEditorScreen(
        title = stringResource(R.string.screen_setup_title),
        actionLabel = stringResource(R.string.action_save_and_continue),
        showBackAction = false,
        onBack = {},
        onSaveSuccess = onSaved,
        completeOnboarding = true,
        viewModel = viewModel
    )
}
