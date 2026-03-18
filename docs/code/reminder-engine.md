# Reminder Engine

This document explains the current reminder engine implementation.

## Scope

The current implementation covers:

- daily `put lenses on` reminder alarms
- planned start reminder alarms
- wear-end reminder alarms
- repeated overdue reminders
- final-alert cutoff behavior
- background notification actions for `Lenses on`, `Snooze 15 min`, and `Lenses off`
- reminder recovery after boot, package replace, time changes, timezone changes, and exact-alarm permission changes

Still out of scope:

- manual start correction
- full exact-alarm permission state persistence or dismissal UX

## Main components

### Domain scheduler package

`app/src/main/java/com/alex/lensesreminder/domain/scheduler/`

- `ReminderScheduleCoordinator`: recomputes alarms from the persisted session state
- `ReminderAlarmHandler`: validates fired alarms, updates the session, posts notifications, and advances the reminder chain
- `ReminderAlarmType`: planned start, wear end, overdue repeat, and final alert
- `DailyStartReminderCoordinator`: manages the recurring daily reminder that prompts the user to put lenses on
- `ReminderStateRescheduler`: rebuilds reminder state after system events
- `ProfileReminderReconciler`: adjusts current reminder state after profile edits
- `ReminderAlarmScheduler` / `ReminderNotificationPublisher`: interfaces used to keep the reminder rules unit-testable

### Android integrations

- `AlarmManagerReminderScheduler`: schedules alarms with `setExactAndAllowWhileIdle()` when exact alarms are available; otherwise falls back to `setAndAllowWhileIdle()`
- `SystemReminderNotificationPublisher`: builds and posts the notification UI and actions
- `ReminderAlarmReceiver`: single alarm receiver entry point
- `ReminderActionReceiver`: background action receiver for notification buttons
- `ReminderSystemEventReceiver`: system-event entry point for reminder resynchronization

## Alarm types

The engine currently uses these alarm types:

- `DAILY_START`
- `PLANNED_START`
- `WEAR_END`
- `OVERDUE_REPEAT`
- `FINAL_ALERT`

The daily reminder uses a synthetic session id (`DAILY_START_REMINDER_SESSION_ID`) so it can share the same alarm/notification infrastructure without colliding with real wear sessions.

## Scheduling rules

The app stores session/profile state as source-of-truth and recomputes alarms from that state.

### Daily reminder

- the daily reminder time comes from `LensProfile.dailyStartReminderTime`
- one daily reminder is scheduled when reminders are enabled
- it is skipped for the current day when there is already an open session or when the app explicitly requests `skipToday`
- when the daily reminder fires, the next day's reminder is scheduled immediately
- if there is already an open session when it fires, the notification is suppressed

### Planned session

- schedule one planned-start alarm at `plannedStartAt`
- when that alarm fires, send the notification once and do not schedule another reminder unless the user snoozes

### Active session

- schedule the wear-end alarm at `expectedEndAt`
- schedule the final alert if `finalAlertScheduledFor` is still in the future

### Overdue session

- if no reminder has been sent yet, schedule an immediate wear-end alarm so the first overdue notification is still emitted
- otherwise schedule the next overdue reminder from `lastReminderSentAt + repeatReminderMinutes`
- skip the repeat reminder if the final alert would happen first

### Final alert

- when the final alert fires, the session remains `ACTIVE` or `OVERDUE`
- `finalAlertSentAt` is recorded
- no further reminder alarms are scheduled for that session

## Recovery and reconciliation

Reminder state is rebuilt from persisted data in several situations:

- after boot
- after app/package replacement
- after manual time changes
- after timezone changes
- after exact-alarm permission state changes
- after profile edits that affect reminder settings

Recovery does not depend on cached in-memory state. The app reloads the current session, profile, and onboarding state, then recomputes alarms from those persisted sources.

## Idempotency

The alarm handler is designed to ignore stale or duplicate alarms by validating:

- session id
- alarm type
- scheduled timestamp
- current session status
- whether the relevant reminder or final alert was already recorded

This lets old alarms no-op after snoozes, plan edits, activation, completion, or delayed delivery.

## Current decisions

The current implementation makes these explicit choices:

- notification actions stay background-driven and do not require opening the app
- exact alarms are used when the platform already allows them; otherwise the app falls back to inexact alarms
- the home screen exposes an exact-alarm access entry point, but the broader UX around that state is still lightweight
