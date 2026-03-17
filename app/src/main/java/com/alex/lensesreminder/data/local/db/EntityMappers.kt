package com.alex.lensesreminder.data.local.db

import com.alex.lensesreminder.core.model.LensProfile
import com.alex.lensesreminder.core.model.LensType
import com.alex.lensesreminder.core.model.SessionSource
import com.alex.lensesreminder.core.model.SessionStatus
import com.alex.lensesreminder.core.model.WearSession

/**
 * Mapping helpers that keep persistence details out of the rest of the app.
 */
fun LensProfileEntity.toDomain(): LensProfile = LensProfile(
    id = id,
    lensType = LensType.valueOf(lensType),
    maxWearMinutes = maxWearMinutes,
    remindersEnabled = remindersEnabled,
    finalAlertTime = finalAlertTime,
    dailyStartReminderTime = dailyStartReminderTime,
    repeatReminderMinutes = repeatReminderMinutes
)

fun LensProfile.toEntity(): LensProfileEntity = LensProfileEntity(
    id = id,
    lensType = lensType.name,
    maxWearMinutes = maxWearMinutes,
    remindersEnabled = remindersEnabled,
    finalAlertTime = finalAlertTime,
    dailyStartReminderTime = dailyStartReminderTime,
    repeatReminderMinutes = repeatReminderMinutes
)

fun WearSessionEntity.toDomain(): WearSession = WearSession(
    id = id,
    plannedStartAt = plannedStartAt,
    actualStartAt = actualStartAt,
    expectedEndAt = expectedEndAt,
    completedAt = completedAt,
    status = SessionStatus.valueOf(status),
    source = SessionSource.valueOf(source),
    finalAlertScheduledFor = finalAlertScheduledFor,
    finalAlertSentAt = finalAlertSentAt,
    lastReminderSentAt = lastReminderSentAt,
    reminderCount = reminderCount
)

fun WearSession.toEntity(): WearSessionEntity = WearSessionEntity(
    id = id,
    plannedStartAt = plannedStartAt,
    actualStartAt = actualStartAt,
    expectedEndAt = expectedEndAt,
    completedAt = completedAt,
    status = status.name,
    source = source.name,
    finalAlertScheduledFor = finalAlertScheduledFor,
    finalAlertSentAt = finalAlertSentAt,
    lastReminderSentAt = lastReminderSentAt,
    reminderCount = reminderCount
)
