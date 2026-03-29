package com.alex.lensesreminder.app.navigation

import kotlinx.serialization.Serializable

/**
 * Supported top-level destinations for the Phase 1 app shell.
 */
sealed interface AppDestination {
    @Serializable
    data object Setup : AppDestination

    @Serializable
    data object Home : AppDestination

    @Serializable
    data object PlanSession : AppDestination

    @Serializable
    data object Settings : AppDestination
}
