# Lenses Reminder MVP Technical Specification

## Purpose

This document converts the product vision into an implementable Android MVP plan for the current repository.

The codebase is currently a minimal single-module Android app using Kotlin and Jetpack Compose. This specification assumes we continue with that stack and add only the dependencies needed to support persistence, scheduling, notifications, and testability.

## MVP outcome

The first release should let a user:

1. Configure daily lens preferences and a final pre-sleep alert time.
2. Start a lens session now or plan one for later.
3. Receive a put-on reminder for planned sessions.
4. Track elapsed time, remaining time, and expected removal time while lenses are in.
5. Receive repeated take-off reminders every 15 minutes after the wear window ends.
6. Receive one final stronger alert at the configured bedtime.
7. Stop reminders only by confirming `Lenses off` or after the final alert fires.
8. Backfill a missed session start time.

## Non-goals for v1

- Multi-lens replacement schedules beyond daily lenses
- Wear history analytics dashboards
- Calendar sync
- Sleep integration
- Health journaling
- Adaptive reminder timing

## Recommended technical approach

## Platform and architecture

- Android app, single `:app` module for MVP
- UI with Jetpack Compose and Material 3
- MVVM with unidirectional state flow
- Room for local persistence
- DataStore for simple user preferences
- AlarmManager for time-sensitive reminders
- BroadcastReceivers for notification actions and alarm handling
- Foreground UI updates driven by `ViewModel` + `StateFlow`
- Dependency injection with Hilt

This is the minimum stack that still handles exact-time reminder behavior reliably. WorkManager alone is not strong enough for the core overdue and final-alert requirements because reminder timing can drift.

## Proposed packages

```text
com.alex.lensesreminder
  app/
  core/
    model/
    time/
    notification/
  data/
    local/
      db/
      datastore/
    repository/
  domain/
    session/
    settings/
    scheduler/
  feature/
    onboarding/
    home/
    plan_session/
    active_session/
    session_result/
    settings/
  receiver/
  service/
```

## Dependency additions

Add these libraries before feature work:

- `androidx.navigation:navigation-compose`
- `androidx.lifecycle:lifecycle-viewmodel-compose`
- `androidx.room:room-runtime`
- `androidx.room:room-ktx`
- `androidx.room:room-compiler` via KSP
- `androidx.datastore:datastore-preferences`
- `androidx.hilt:hilt-navigation-compose`
- `com.google.dagger:hilt-android`
- `com.google.dagger:hilt-compiler`
- `org.jetbrains.kotlinx:kotlinx-datetime` or Java time desugaring
- `androidx.core:core-splashscreen` optional
- test libs for coroutines, Room, and Flow testing

## Permissions and manifest changes

The MVP will require:

- `POST_NOTIFICATIONS`
- `RECEIVE_BOOT_COMPLETED`
- `VIBRATE`

Evaluate exact alarm behavior on Android 12+:

- Preferred: support exact alarms for planned start, wear-end, repeat reminders, and final alert.
- If the app needs `SCHEDULE_EXACT_ALARM`, include a product decision on whether to request it in MVP.
- If exact alarms are denied, fallback behavior should still work but may be inexact. The UI should communicate that reminders may be delayed.

## Domain model

## Lens profile

One active profile is enough for MVP.

```kotlin
data class LensProfile(
    val id: Long = 1,
    val lensType: LensType = LensType.DAILY,
    val maxWearMinutes: Int,
    val remindersEnabled: Boolean,
    val finalAlertTime: LocalTime,
    val repeatReminderMinutes: Int = 15
)
```

`LensType` should be an enum with `DAILY` only in v1, but structured for future extension.

## Wear session

```kotlin
data class WearSession(
    val id: Long,
    val plannedStartAt: Instant?,
    val actualStartAt: Instant?,
    val expectedEndAt: Instant?,
    val completedAt: Instant?,
    val status: SessionStatus,
    val source: SessionSource,
    val finalAlertScheduledFor: Instant?,
    val finalAlertSentAt: Instant?,
    val lastReminderSentAt: Instant?,
    val reminderCount: Int
)
```

### Session status state machine

- `PLANNED`: future session exists, lenses not yet confirmed on
- `ACTIVE`: lenses confirmed on, wear timer running
- `OVERDUE`: wear window ended, lenses not yet confirmed off
- `COMPLETED`: user confirmed lenses off
- `CANCELLED`: optional internal state if planned session is dismissed before start

State transitions:

1. `PLANNED -> ACTIVE` when user taps `Lenses on`
2. `ACTIVE -> OVERDUE` when current time reaches `expectedEndAt`
3. `ACTIVE -> COMPLETED` when user confirms `Lenses off` before end time
4. `OVERDUE -> COMPLETED` when user confirms `Lenses off`
5. `PLANNED -> CANCELLED` if user deletes planned session before activation

## Storage design

## Room entities

Create:

- `wear_sessions`
- `scheduled_notifications` optional table for debugability and recovery

`wear_sessions` should store timestamps in UTC and derive display values in the user timezone.

## DataStore keys

Store:

- `has_completed_onboarding`
- `notifications_permission_requested`
- `exact_alarm_warning_dismissed`
- lightweight UI preferences if needed

Lens settings can live in Room or DataStore. Prefer Room if profile evolution is expected; otherwise DataStore is acceptable. For MVP consistency, keep `LensProfile` in Room with `WearSession`.

## Scheduling and notification behavior

## Notification types

1. Planned start reminder
2. Wear end reminder
3. Repeating overdue reminder
4. Final alert before sleep

## Scheduling rules

### Planned start reminder

- Scheduled when user creates a future session and reminders are enabled
- Fires at `plannedStartAt`
- Notification actions:
  - `Lenses on`
  - `Snooze 15 min`

### Session activation

When a session becomes active:

- set `actualStartAt`
- calculate `expectedEndAt = actualStartAt + maxWearMinutes`
- schedule wear-end reminder
- schedule final alert for the next matching local time on the same day if it occurs after `actualStartAt`

If the configured final alert time is already in the past when the session starts, do not schedule a final alert for that session.

### Wear end reminder

- Fires at `expectedEndAt`
- Changes session to `OVERDUE` if not already completed
- Sends a high-priority notification with `Lenses off`
- Immediately schedules the next overdue reminder for `+15 min` unless blocked by final alert timing

### Repeating overdue reminders

- Only valid while session is `OVERDUE`
- Repeat every 15 minutes
- Stop when:
  - user taps `Lenses off`
  - final alert fires
  - session becomes invalid or cancelled

Before each repeat, recompute whether the final alert time occurs first. If yes, schedule the final alert and skip any reminder beyond that point.

### Final alert

- Fires once at user-configured `finalAlertTime` if session is still `ACTIVE` or `OVERDUE`
- Uses stronger channel and sound than standard reminders
- After firing, no more reminders are scheduled for that session
- Session remains `ACTIVE` or `OVERDUE` until user explicitly confirms `Lenses off`

This distinction matters: notification cadence ends after the final alert, but session truth does not change automatically.

## Alarm handling implementation

Use one `BroadcastReceiver` entry point for alarms, with payload fields:

- `sessionId`
- `notificationType`
- `scheduledAt`

Receiver flow:

1. Load session and profile.
2. Validate the session is still eligible for the event.
3. Update session state if needed.
4. Post the notification.
5. Schedule the next event if rules require it.

This must be idempotent because alarms can be duplicated after process death, reboot recovery, or manual clock changes.

## Reboot and time-change recovery

Implement receivers for:

- device reboot
- app update completed if needed
- timezone changed
- time changed

On recovery:

1. Reload the active or planned session.
2. Recompute upcoming reminder timestamps from source-of-truth session data.
3. Reschedule only the next required event.

## UI specification

## Screens

### 1. Onboarding / first setup

Purpose:

- collect maximum wear duration
- enable reminders
- collect final alert time
- explain notification permissions

Primary action:

- `Save and continue`

### 2. Home screen

Purpose:

- show whether lenses are currently `Out`, `Planned`, `In`, or `Overdue`
- show elapsed, remaining, and expected removal time
- provide the main session actions

Primary actions by state:

- no session: `Start now`, `Plan for later`
- planned: `Lenses on`, `Edit plan`, `Cancel`
- active: `Lenses off`, `Correct start time`
- overdue: `Lenses off`

### 3. Plan session screen

Fields:

- date
- time
- reminders toggle inherited from settings but visible

Actions:

- `Save plan`

### 4. Correct start time screen

Fields:

- date
- time

Rules:

- can only set a start time in the past
- must be same-day in MVP unless product later expands cross-midnight support
- recalculates `expectedEndAt`, reminders, and final alert timing

### 5. Settings screen

Fields:

- lens type
- maximum wear duration
- reminders enabled
- final alert time
- overdue interval shown as fixed `Every 15 minutes`

## Navigation

Use a simple `NavHost`:

- `home`
- `setup`
- `planSession`
- `correctStartTime`
- `settings`

No deep linking is required in MVP.

## UX and validation rules

- There can be at most one non-completed session at a time.
- Starting a new session while another is active or overdue is blocked.
- Planned and active session cards should always show the exact timestamp of the next important event.
- Overdue state must be visually distinct from active state.
- Notification actions should complete the most common task without opening the app when possible.
- If notification permission is denied, the home screen should show a clear warning banner.

## Copy and phrases

These phrases should be implemented as string resources from the start.

## Buttons and actions

- `Start now`
- `Plan for later`
- `Lenses on`
- `Lenses off`
- `Snooze 15 min`
- `Correct start time`
- `Save plan`
- `Save and continue`
- `Done`
- `Cancel`
- `Edit settings`

## Status labels

- `Lenses out`
- `Session planned`
- `Lenses in`
- `Removal overdue`
- `Reminders off`
- `Final alert scheduled`

## Helper text

- `Track how long your daily lenses have been in.`
- `Choose the maximum safe wear time recommended for your lenses.`
- `Set a final alert time for the last warning before sleep.`
- `You can correct the start time if you forgot to log it earlier.`
- `Repeated reminders stop only after you confirm lenses are off or the final alert is sent.`

## Notification titles

- Planned start: `Time to put in your lenses`
- Wear end: `Time to take your lenses off`
- Overdue repeat: `Your lenses are still in`
- Final alert: `Final alert: remove your lenses now`

## Notification body examples

- Planned start: `Your planned lens session starts now.`
- Wear end: `Your recommended wear time has ended.`
- Overdue repeat: `You still have not confirmed that your lenses are out.`
- Final alert: `This is your last reminder before sleep. Remove your lenses now.`

## Empty and confirmation states

- `No lens session for today yet.`
- `Your lens session is active.`
- `Your lens session is complete.`
- `Lenses marked as off.`
- `Plan updated.`
- `Start time corrected.`

## Error messages

- `Please choose a valid wear duration.`
- `Final alert time is required when reminders are enabled.`
- `Only one active lens session is allowed at a time.`
- `That start time is not valid for the current session.`
- `Notification permission is off, so reminders may not arrive.`

## Acceptance criteria

## Settings

- User can save lens type, wear duration, reminders enabled, and final alert time.
- Settings persist across app restarts.

## Planned sessions

- User can create a future session.
- Notification fires at planned start when permissions allow.
- `Lenses on` action converts planned session into active session.

## Active sessions

- Home screen updates elapsed and remaining time while app is open.
- Expected removal time is visible.

## Overdue reminders

- At wear end, user receives a high-priority removal reminder.
- If user does not confirm `Lenses off`, reminders repeat every 15 minutes.
- Repeat chain stops after final alert has fired.

## Final alert

- Final alert fires once at configured time if session still active or overdue.
- Final alert uses separate notification channel and stronger sound if supported.
- No later reminders are scheduled for that session.

## Backfill / correction

- User can manually correct a missed start time.
- Session times and future reminders are recalculated from the new start time.

## Technical task list

## Foundation

- [ ] Add architecture dependencies: Room, Hilt, Navigation, DataStore, test libs
- [ ] Enable KSP and Room schema export
- [ ] Add notification permission flow
- [ ] Add time abstraction (`Clock`) for testability
- [ ] Add app-level package structure

## Data layer

- [ ] Create `LensProfileEntity`
- [ ] Create `WearSessionEntity`
- [ ] Create DAOs and Room database
- [ ] Implement repositories for settings and sessions
- [ ] Add mappers between entities and domain models

## Scheduling engine

- [ ] Define notification event types
- [ ] Implement alarm scheduler wrapper around `AlarmManager`
- [ ] Implement alarm receiver and action receiver
- [ ] Implement notification builder and channels
- [ ] Implement reboot/time-change rescheduling
- [ ] Add idempotency checks for duplicate alarms

## Domain logic

- [ ] Implement create planned session use case
- [ ] Implement start session now use case
- [ ] Implement activate planned session use case
- [ ] Implement complete session use case
- [ ] Implement correct start time use case
- [ ] Implement recompute reminders use case
- [ ] Implement current session observe use case

## UI

- [ ] Replace sample `MainActivity` greeting flow with app navigation shell
- [ ] Build onboarding/settings screen
- [ ] Build home screen with state cards and timer UI
- [ ] Build plan session screen
- [ ] Build correct start time screen
- [ ] Add permission and reminder warning banners

## Testing

- [ ] Unit-test session state transitions
- [ ] Unit-test final alert scheduling edge cases
- [ ] Unit-test overdue repeat logic
- [ ] DAO tests for session persistence
- [ ] Receiver tests for event handling
- [ ] Compose UI tests for key home-screen states

## Delivery plan

## Phase 1: App foundation

Goal:

Create project structure, persistence, navigation, notification channels, and settings storage.

Deliverables:

- working app shell
- persistent profile/settings
- notification permission flow

## Phase 2: Session lifecycle

Goal:

Support `Start now`, planned sessions, and active-session tracking.

Deliverables:

- home screen
- plan session flow
- start/stop actions
- active timer UI

## Phase 3: Reminder engine

Goal:

Implement exact scheduling, overdue loop, final alert behavior, and notification actions.

Deliverables:

- planned start reminder
- wear-end reminder
- repeated overdue reminders
- final alert cutoff logic

## Phase 4: Correction and hardening

Goal:

Add manual start correction, reboot recovery, and edge-case coverage.

Deliverables:

- correct start time flow
- rescheduling after reboot/time changes
- improved test coverage

## Edge cases to implement explicitly

- User taps `Lenses off` from notification while app is killed
- User taps `Lenses on` from planned reminder after the planned time passed
- Final alert time is earlier than planned or actual start time
- Session becomes overdue close to final alert time
- User corrects start time after overdue reminders already began
- Timezone changes during an active session
- Device reboots with an active overdue session
- Notification permission denied after session creation

## Open technical decisions

These should be resolved before implementation starts:

1. Whether MVP requests exact alarm permission on Android 12+ or accepts inexact fallback.
2. Whether the app supports cross-midnight sessions in v1 or restricts to same-day flows.
3. Whether notification actions should open the app after completion or stay fully background-driven.
4. Whether custom reminder sounds are bundled in MVP or deferred until the first polished release.

## Suggested implementation order

1. Replace the placeholder UI with app navigation and a real home screen shell.
2. Add persistence and session domain models.
3. Implement settings and `Start now`.
4. Implement planned sessions and session activation.
5. Implement notification channels and wear-end reminders.
6. Implement repeated overdue reminders.
7. Implement final alert cutoff.
8. Implement correction flow and recovery receivers.
9. Finish tests and UX polish.
