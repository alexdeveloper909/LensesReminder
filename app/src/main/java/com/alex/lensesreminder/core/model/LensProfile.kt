package com.alex.lensesreminder.core.model

import java.time.LocalTime

const val DEFAULT_PROFILE_ID = 1L
const val DEFAULT_REPEAT_REMINDER_MINUTES = 15
const val DEFAULT_MAX_WEAR_MINUTES = 12 * 60

/**
 * Stored lens preferences used by the session and reminder engine.
 */
data class LensProfile(
    val id: Long = DEFAULT_PROFILE_ID,
    val lensType: LensType = LensType.DAILY,
    val maxWearMinutes: Int = DEFAULT_MAX_WEAR_MINUTES,
    val remindersEnabled: Boolean = true,
    val finalAlertTime: LocalTime = LocalTime.of(22, 0),
    val repeatReminderMinutes: Int = DEFAULT_REPEAT_REMINDER_MINUTES,
)
