package com.alex.lensesreminder.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.alex.lensesreminder.feature.home.HomeRoute
import com.alex.lensesreminder.feature.onboarding.OnboardingRoute
import com.alex.lensesreminder.feature.plan.PlanSessionRoute
import com.alex.lensesreminder.feature.settings.SettingsRoute

/**
 * Navigation graph for the foundation phase.
 */
@Composable
fun LensesReminderNavHost(
    navController: NavHostController,
    startDestination: AppDestination,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<AppDestination.Setup> {
            OnboardingRoute(
                onSaved = {
                    navController.navigate(AppDestination.Home) {
                        popUpTo<AppDestination.Setup> {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<AppDestination.Home> {
            HomeRoute(
                onPlanSession = {
                    navController.navigate(AppDestination.PlanSession)
                },
                onEditSettings = {
                    navController.navigate(AppDestination.Settings)
                }
            )
        }
        composable<AppDestination.PlanSession> {
            PlanSessionRoute(
                onDone = {
                    navController.popBackStack()
                }
            )
        }
        composable<AppDestination.Settings> {
            SettingsRoute(
                onDone = {
                    navController.popBackStack()
                }
            )
        }
    }
}
