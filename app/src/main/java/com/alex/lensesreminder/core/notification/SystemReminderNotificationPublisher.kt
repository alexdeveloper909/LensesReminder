package com.alex.lensesreminder.core.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alex.lensesreminder.MainActivity
import com.alex.lensesreminder.R
import com.alex.lensesreminder.core.model.WearSession
import com.alex.lensesreminder.domain.scheduler.ReminderAction
import com.alex.lensesreminder.domain.scheduler.ReminderAlarmType
import com.alex.lensesreminder.domain.scheduler.ReminderNotificationPublisher
import com.alex.lensesreminder.receiver.ReminderActionReceiver
import com.alex.lensesreminder.receiver.ReminderContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and posts reminder notifications with background actions.
 */
@Singleton
class SystemReminderNotificationPublisher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ReminderNotificationPublisher {

    override fun show(
        session: WearSession,
        type: ReminderAlarmType,
    ) {
        if (!NotificationPermissionManager.hasNotificationPermission(context)) {
            return
        }

        val builder = NotificationCompat.Builder(context, channelId(type))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(titleRes(type)))
            .setContentText(context.getString(bodyRes(type)))
            .setPriority(priority(type))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())

        when (type) {
            ReminderAlarmType.DAILY_START -> {
                builder.addAction(
                    0,
                    context.getString(R.string.action_lenses_on),
                    actionPendingIntent(session.id, ReminderAction.START_SESSION)
                )
            }

            ReminderAlarmType.PLANNED_START -> {
                builder.addAction(
                    0,
                    context.getString(R.string.action_lenses_on),
                    actionPendingIntent(session.id, ReminderAction.ACTIVATE_SESSION)
                )
                builder.addAction(
                    0,
                    context.getString(R.string.action_snooze_15_min),
                    actionPendingIntent(session.id, ReminderAction.SNOOZE_PLANNED)
                )
            }

            ReminderAlarmType.WEAR_END,
            ReminderAlarmType.OVERDUE_REPEAT,
            ReminderAlarmType.FINAL_ALERT,
            -> {
                builder.addAction(
                    0,
                    context.getString(R.string.action_lenses_off),
                    actionPendingIntent(session.id, ReminderAction.COMPLETE_SESSION)
                )
            }
        }

        NotificationManagerCompat.from(context)
            .notify(ReminderContract.notificationRequestCode(session.id, type), builder.build())
    }

    override fun cancelAll(sessionId: Long) {
        val manager = NotificationManagerCompat.from(context)
        ReminderAlarmType.entries.forEach { type ->
            manager.cancel(ReminderContract.notificationRequestCode(sessionId, type))
        }
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun actionPendingIntent(
        sessionId: Long,
        action: ReminderAction,
    ): PendingIntent {
        val actionName = when (action) {
            ReminderAction.START_SESSION -> ReminderContract.ACTION_START_SESSION
            ReminderAction.ACTIVATE_SESSION -> ReminderContract.ACTION_ACTIVATE_SESSION
            ReminderAction.SNOOZE_PLANNED -> ReminderContract.ACTION_SNOOZE_PLANNED
            ReminderAction.COMPLETE_SESSION -> ReminderContract.ACTION_COMPLETE_SESSION
        }

        return PendingIntent.getBroadcast(
            context,
            ReminderContract.actionRequestCode(sessionId, action),
            Intent(context, ReminderActionReceiver::class.java).apply {
                this.action = actionName
                putExtra(ReminderContract.EXTRA_SESSION_ID, sessionId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun channelId(type: ReminderAlarmType): String = when (type) {
        ReminderAlarmType.FINAL_ALERT -> LensesReminderNotificationChannels.FINAL_ALERTS
        ReminderAlarmType.DAILY_START,
        ReminderAlarmType.PLANNED_START,
        ReminderAlarmType.WEAR_END,
        ReminderAlarmType.OVERDUE_REPEAT,
        -> LensesReminderNotificationChannels.STANDARD_REMINDERS
    }

    private fun titleRes(type: ReminderAlarmType): Int = when (type) {
        ReminderAlarmType.DAILY_START -> R.string.notification_title_daily_start
        ReminderAlarmType.PLANNED_START -> R.string.notification_title_planned_start
        ReminderAlarmType.WEAR_END -> R.string.notification_title_wear_end
        ReminderAlarmType.OVERDUE_REPEAT -> R.string.notification_title_overdue_repeat
        ReminderAlarmType.FINAL_ALERT -> R.string.notification_title_final_alert
    }

    private fun bodyRes(type: ReminderAlarmType): Int = when (type) {
        ReminderAlarmType.DAILY_START -> R.string.notification_body_daily_start
        ReminderAlarmType.PLANNED_START -> R.string.notification_body_planned_start
        ReminderAlarmType.WEAR_END -> R.string.notification_body_wear_end
        ReminderAlarmType.OVERDUE_REPEAT -> R.string.notification_body_overdue_repeat
        ReminderAlarmType.FINAL_ALERT -> R.string.notification_body_final_alert
    }

    private fun priority(type: ReminderAlarmType): Int = when (type) {
        ReminderAlarmType.FINAL_ALERT -> NotificationCompat.PRIORITY_MAX
        ReminderAlarmType.DAILY_START,
        ReminderAlarmType.PLANNED_START,
        ReminderAlarmType.WEAR_END,
        ReminderAlarmType.OVERDUE_REPEAT,
        -> NotificationCompat.PRIORITY_HIGH
    }
}
