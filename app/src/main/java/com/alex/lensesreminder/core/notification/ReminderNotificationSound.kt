package com.alex.lensesreminder.core.notification

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import com.alex.lensesreminder.R
import com.alex.lensesreminder.domain.scheduler.ReminderAlarmType

/**
 * Resolves the bundled notification sounds used by reminder channels.
 */
object ReminderNotificationSound {

    fun uri(
        context: Context,
        type: ReminderAlarmType,
    ): Uri = Uri.parse(
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${rawResId(type)}"
    )

    fun audioAttributes(type: ReminderAlarmType): AudioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(
            if (type == ReminderAlarmType.FINAL_ALERT) {
                AudioAttributes.USAGE_ALARM
            } else {
                AudioAttributes.USAGE_NOTIFICATION_EVENT
            }
        )
        .build()

    private fun rawResId(type: ReminderAlarmType): Int = if (type == ReminderAlarmType.FINAL_ALERT) {
        R.raw.final_alert_sound
    } else {
        R.raw.minor_reminder_sound
    }
}
