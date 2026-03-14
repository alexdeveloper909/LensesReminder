package com.alex.lensesreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alex.lensesreminder.domain.session.SessionLifecycleManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles notification actions without requiring the app UI to be open.
 */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionLifecycleManager: SessionLifecycleManager

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val sessionId = intent.getLongExtra(ReminderContract.EXTRA_SESSION_ID, 0L)
        if (sessionId == 0L) {
            return
        }

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                when (intent.action) {
                    ReminderContract.ACTION_ACTIVATE_SESSION -> {
                        sessionLifecycleManager.activatePlannedSession(sessionId)
                    }

                    ReminderContract.ACTION_SNOOZE_PLANNED -> {
                        sessionLifecycleManager.snoozePlannedSession(sessionId)
                    }

                    ReminderContract.ACTION_COMPLETE_SESSION -> {
                        sessionLifecycleManager.completeCurrentSession(sessionId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
