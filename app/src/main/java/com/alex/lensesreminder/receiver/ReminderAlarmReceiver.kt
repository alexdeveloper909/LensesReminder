package com.alex.lensesreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alex.lensesreminder.domain.scheduler.ReminderAlarmHandler
import com.alex.lensesreminder.domain.scheduler.ReminderAlarmType
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Alarm entry point for all reminder events.
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderAlarmHandler: ReminderAlarmHandler

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val sessionId = intent.getLongExtra(ReminderContract.EXTRA_SESSION_ID, 0L)
        val scheduledAtMillis = intent.getLongExtra(ReminderContract.EXTRA_SCHEDULED_AT, Long.MIN_VALUE)
        val type = intent.getStringExtra(ReminderContract.EXTRA_ALARM_TYPE)
            ?.let { value -> runCatching { ReminderAlarmType.valueOf(value) }.getOrNull() }
            ?: return

        if (sessionId == 0L || scheduledAtMillis == Long.MIN_VALUE) {
            return
        }

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                reminderAlarmHandler.handle(
                    sessionId = sessionId,
                    type = type,
                    scheduledAt = Instant.ofEpochMilli(scheduledAtMillis)
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
