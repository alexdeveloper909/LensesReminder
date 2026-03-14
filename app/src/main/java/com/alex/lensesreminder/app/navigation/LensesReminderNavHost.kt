package com.alex.lensesreminder.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.alex.lensesreminder.feature.home.HomeRoute
import com.alex.lensesreminder.feature.onboarding.OnboardingRoute
import com.alex.lensesreminder.feature.settings.SettingsRoute

/**
 * Navigation graph for the foundation phase.
 */
@Composable
fun LensesReminderNavHost(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = AppDestination.Setup.route) {
            OnboardingRoute(
                onSaved = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Setup.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(route = AppDestination.Home.route) {
            HomeRoute(
                onEditSettings = {
                    navController.navigate(AppDestination.Settings.route)
                }
            )
        }
        composable(route = AppDestination.Settings.route) {
            SettingsRoute(
                onDone = {
                    navController.popBackStack()
                }
            )
        }
    }
}
