package com.alex.lensesreminder.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.alex.lensesreminder.receiver.ReminderAlarmReceiver
import com.alex.lensesreminder.receiver.ReminderContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlarmManager-backed reminder scheduler with exact alarms when available.
 */
@Singleton
class AlarmManagerReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
) : ReminderAlarmScheduler {

    override fun schedule(
        sessionId: Long,
        type: ReminderAlarmType,
        triggerAt: Instant,
    ) {
        val pendingIntent = pendingIntent(sessionId, type, triggerAt)
        val triggerAtMillis = triggerAt.toEpochMilli()

        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    override fun cancel(
        sessionId: Long,
        type: ReminderAlarmType,
    ) {
        val pendingIntent = pendingIntent(sessionId, type, Instant.EPOCH)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    override fun cancelAll(sessionId: Long) {
        ReminderAlarmType.entries.forEach { type ->
            cancel(sessionId, type)
        }
    }

    private fun pendingIntent(
        sessionId: Long,
        type: ReminderAlarmType,
        triggerAt: Instant,
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        ReminderContract.alarmRequestCode(sessionId, type),
        Intent(context, ReminderAlarmReceiver::class.java)
            .putExtra(ReminderContract.EXTRA_SESSION_ID, sessionId)
            .putExtra(ReminderContract.EXTRA_ALARM_TYPE, type.name)
            .putExtra(ReminderContract.EXTRA_SCHEDULED_AT, triggerAt.toEpochMilli()),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun canScheduleExactAlarms(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager.canScheduleExactAlarms()
    } else {
        true
    }
}
