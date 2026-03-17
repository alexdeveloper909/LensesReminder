package com.alex.lensesreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alex.lensesreminder.domain.scheduler.ReminderStateRescheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reschedules reminders after system events that clear or invalidate alarms.
 */
@AndroidEntryPoint
class ReminderSystemEventReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderStateRescheduler: ReminderStateRescheduler

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                reminderStateRescheduler.syncAll()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
