package com.alex.lensesreminder.receiver

import com.alex.lensesreminder.domain.scheduler.ReminderAction
import com.alex.lensesreminder.domain.scheduler.ReminderAlarmType

/**
 * Shared extras, actions, and request-code helpers for reminder broadcasts.
 */
object ReminderContract {
    const val EXTRA_SESSION_ID = "extra_session_id"
    const val EXTRA_ALARM_TYPE = "extra_alarm_type"
    const val EXTRA_SCHEDULED_AT = "extra_scheduled_at"

    const val ACTION_ACTIVATE_SESSION =
        "com.alex.lensesreminder.action.ACTIVATE_SESSION"
    const val ACTION_SNOOZE_PLANNED =
        "com.alex.lensesreminder.action.SNOOZE_PLANNED"
    const val ACTION_COMPLETE_SESSION =
        "com.alex.lensesreminder.action.COMPLETE_SESSION"

    fun alarmRequestCode(
        sessionId: Long,
        type: ReminderAlarmType,
    ): Int = requestCode(sessionId, type.ordinal)

    fun notificationRequestCode(
        sessionId: Long,
        type: ReminderAlarmType,
    ): Int = requestCode(sessionId, 100 + type.ordinal)

    fun actionRequestCode(
        sessionId: Long,
        action: ReminderAction,
    ): Int = requestCode(sessionId, 200 + action.ordinal)

    private fun requestCode(
        sessionId: Long,
        salt: Int,
    ): Int = ((sessionId * 37L) + salt.toLong()).hashCode()
}
