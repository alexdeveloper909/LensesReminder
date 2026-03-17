package com.alex.lensesreminder.domain.scheduler

/**
 * Alarm types used by the reminder engine.
 */
enum class ReminderAlarmType {
    DAILY_START,
    PLANNED_START,
    WEAR_END,
    OVERDUE_REPEAT,
    FINAL_ALERT,
}
