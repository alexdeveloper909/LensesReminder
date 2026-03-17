package com.alex.lensesreminder.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.alex.lensesreminder.R
import com.alex.lensesreminder.domain.scheduler.ReminderAlarmType

/**
 * Notification channel registry used by the reminder engine.
 */
object LensesReminderNotificationChannels {

    const val STANDARD_REMINDERS = "standard_reminders_v2"
    const val FINAL_ALERTS = "final_alerts_v2"

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
        setSound(
            ReminderNotificationSound.uri(context, ReminderAlarmType.WEAR_END),
            ReminderNotificationSound.audioAttributes(ReminderAlarmType.WEAR_END)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun finalAlertChannel(context: Context): NotificationChannel = NotificationChannel(
        FINAL_ALERTS,
        context.getString(R.string.channel_final_alert_name),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.getString(R.string.channel_final_alert_description)
        enableVibration(true)
        setSound(
            ReminderNotificationSound.uri(context, ReminderAlarmType.FINAL_ALERT),
            ReminderNotificationSound.audioAttributes(ReminderAlarmType.FINAL_ALERT)
        )
    }
}
