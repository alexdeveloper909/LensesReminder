package com.alex.lensesreminder

import android.app.Application
import com.alex.lensesreminder.core.notification.LensesReminderNotificationChannels
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point used to bootstrap dependency injection and notification channels.
 */
@HiltAndroidApp
class LensesReminderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        LensesReminderNotificationChannels.create(this)
    }
}
