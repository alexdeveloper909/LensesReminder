package com.alex.lensesreminder.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.alex.lensesreminder.R

/**
 * Notification channel registry used by the reminder engine.
 */
object LensesReminderNotificationChannels {

    const val STANDARD_REMINDERS = "standard_reminders"
    const val FINAL_ALERTS = "final_alerts"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = context.getSystemService<NotificationManager>() ?: return
        notificationManager.createNotificationChannels(
            listOf(
                standardReminderChannel(context),
                finalAlertChannel(context)
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun standardReminderChannel(context: Context): NotificationChannel = NotificationChannel(
        STANDARD_REMINDERS,
        context.getString(R.string.channel_standard_reminders_name),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.getString(R.string.channel_standard_reminders_description)
        enableVibration(true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun finalAlertChannel(context: Context): NotificationChannel = NotificationChannel(
        FINAL_ALERTS,
        context.getString(R.string.channel_final_alert_name),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.getString(R.string.channel_final_alert_description)
        enableVibration(true)
    }
}
