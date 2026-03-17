package com.alex.lensesreminder.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.alex.lensesreminder.app.navigation.AppDestination
import com.alex.lensesreminder.app.navigation.LensesReminderNavHost

/**
 * Root Compose entry point that decides whether the user lands in onboarding or home.
 */
@Composable
fun LensesReminderApp(
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val uiState by rootViewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, rootViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                rootViewModel.onAppForegrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (uiState.hasCompletedOnboarding) {
        AppDestination.Home.route
    } else {
        AppDestination.Setup.route
    }

    val navController = rememberNavController()
    LensesReminderNavHost(
        navController = navController,
        startDestination = startDestination
    )
}
