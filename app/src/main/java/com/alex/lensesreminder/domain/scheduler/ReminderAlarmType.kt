package com.alex.lensesreminder.domain.scheduler

/**
 * Alarm types used by the reminder engine.
 */
enum class ReminderAlarmType {
    PLANNED_START,
    WEAR_END,
    OVERDUE_REPEAT,
    FINAL_ALERT,
}
