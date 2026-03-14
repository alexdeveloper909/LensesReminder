# Reminder Engine

This document explains the Phase 3 reminder engine as implemented in the app.

## Scope

Phase 3 now covers:

- planned start reminder alarms
- wear-end reminder alarms
- repeated overdue reminders
- final-alert cutoff behavior
- background notification actions for `Lenses on`, `Snooze 15 min`, and `Lenses off`

Still out of scope:

- reboot or time-change rescheduling
- manual start correction
- exact-alarm permission request UI

## Main components

### Domain scheduler package

`app/src/main/java/com/alex/lensesreminder/domain/scheduler/`

- `ReminderScheduleCoordinator`: recomputes alarms from the persisted session state
- `ReminderAlarmHandler`: validates fired alarms, updates the session, posts notifications, and advances the reminder chain
- `ReminderAlarmType`: planned start, wear end, overdue repeat, and final alert
- `ReminderAlarmScheduler` / `ReminderNotificationPublisher`: interfaces used to keep the reminder rules unit-testable

### Android integrations

- `AlarmManagerReminderScheduler`: schedules alarms with `setExactAndAllowWhileIdle()` when exact alarms are available; otherwise falls back to `setAndAllowWhileIdle()`
- `SystemReminderNotificationPublisher`: builds and posts the notification UI and actions
- `ReminderAlarmReceiver`: single alarm receiver entry point
- `ReminderActionReceiver`: background action receiver for notification buttons

## Scheduling rules

The app stores the session as source-of-truth and recomputes alarms from that state.

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

## Idempotency

The alarm handler is designed to ignore stale or duplicate alarms by validating:

- session id
- alarm type
- scheduled timestamp
- current session status
- whether the relevant reminder or final alert was already recorded

This lets old alarms no-op after snoozes, plan edits, activation, completion, or delayed delivery.

## Phase 3 decisions

The implementation resolves two open spec decisions for now:

- notification actions stay background-driven and do not require opening the app
- exact alarms are used when the platform already allows them; otherwise the app falls back to inexact alarms instead of prompting for permission in Phase 3
