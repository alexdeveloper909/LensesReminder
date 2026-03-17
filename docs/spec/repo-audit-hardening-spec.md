# Repo Audit Hardening Spec

## Purpose

This document captures the concrete follow-up work from a repository audit performed against the current Phase 3 implementation.

The existing JVM unit suite passes, but the audit found several correctness and resilience gaps that are either not covered by tests or are currently codified by incorrect tests. The goal of this spec is to harden reminder behavior, session integrity, and UI semantics without broad architectural churn.

## Audit Summary

The highest-value issues found during the audit are:

1. Home screen deadline semantics are incorrect. The UI currently treats `finalAlertScheduledFor` as if it can replace `expectedEndAt`, which can surface an overdue state before the recommended wear window actually ends.
2. Profile changes do not reconcile already scheduled reminders. Toggling reminders off or changing timing-related settings does not immediately reschedule or cancel alarms for the current session.
3. Reminder recovery is incomplete. Alarms are not restored after reboot or other time-related recovery scenarios, and the current overdue refresh path is not robust enough to restart the reminder chain safely.
4. Session writes are vulnerable to stale overwrite behavior. Alarm handling and notification actions update the same session row from separate async receivers using whole-row replacement.
5. Persistence does not enforce the "only one open session" rule at the database boundary.

## Goals

- Align Home UI with the domain meaning of wear end vs final alert.
- Make reminder scheduling respond immediately to settings changes.
- Restore reminder correctness after reboot, time change, timezone change, and app update.
- Prevent stale background writes from regressing session state.
- Enforce single-open-session integrity with repository and database safeguards.
- Add test coverage that would have caught the above issues.

## Non-Goals

- Splitting the app into multiple Gradle modules
- Replacing `AlarmManager` with a different scheduling stack
- Large-scale UI redesign unrelated to reminder correctness
- Building analytics/history features

## Confirmed Findings

## 1. Home UI mis-models final alert as the wear-end deadline

Current behavior:

- `HomeScreen.toDisplayState()` computes an `effectiveDeadlineAt` using `min(expectedEndAt, finalAlertScheduledFor)`.
- An `ACTIVE` session is shown as `OVERDUE` once that earlier timestamp passes.
- The current JVM test suite asserts this behavior, so the test currently protects the bug.

Impact:

- Users can see "recommended wear time has ended" too early.
- The "Expected removal" row can display the final-alert time instead of the true wear-end time.

Files:

- `app/src/main/java/com/alex/lensesreminder/feature/home/HomeScreen.kt`
- `app/src/test/java/com/alex/lensesreminder/feature/home/HomeScreenDisplayStateTest.kt`

## 2. Settings saves do not resync or clear live alarms

Current behavior:

- `SettingsViewModel.saveProfile()` persists the profile only.
- No production path recomputes alarms for the current session after profile edits.
- Already scheduled alarms can still fire after reminders are disabled.
- Changing repeat interval or final-alert timing can make already scheduled alarms stale relative to the newly saved profile.

Impact:

- Reminder behavior diverges from visible settings.
- Users can still receive notifications after disabling reminders.
- Overdue chains can silently stop after timing changes.

Files:

- `app/src/main/java/com/alex/lensesreminder/feature/settings/SettingsViewModel.kt`
- `app/src/main/java/com/alex/lensesreminder/domain/scheduler/ReminderScheduleCoordinator.kt`
- `app/src/main/java/com/alex/lensesreminder/domain/scheduler/ReminderAlarmHandler.kt`

## 3. Recovery and overdue handoff are incomplete

Current behavior:

- The manifest includes `RECEIVE_BOOT_COMPLETED`, but there is no receiver that restores alarms after reboot or related recovery events.
- App startup only creates notification channels and does not reconcile the persisted current session.
- `refreshCurrentSessionStatus()` can flip an expired `ACTIVE` session to `OVERDUE`, but it then schedules an immediate `WEAR_END` alarm at `clock.now()`.
- `ReminderAlarmHandler.handleWearEnd()` only accepts wear-end alarms whose `scheduledAt` exactly equals `session.expectedEndAt`, so the overdue chain does not restart from this refresh path.

Impact:

- Persisted sessions can lose all future reminders after reboot or time-change scenarios.
- The current recovery helper is not safe to use as the backbone of reminder restoration.

Files:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/alex/lensesreminder/LensesReminderApplication.kt`
- `app/src/main/java/com/alex/lensesreminder/domain/session/SessionLifecycleManager.kt`
- `app/src/main/java/com/alex/lensesreminder/domain/scheduler/ReminderAlarmHandler.kt`

## 4. Async receiver writes can overwrite newer session state

Current behavior:

- `ReminderAlarmReceiver` and `ReminderActionReceiver` both call `goAsync()` and mutate session state on background coroutines.
- Session persistence uses whole-row `REPLACE`.
- Handlers read a snapshot, compute `copy(...)`, and save the full row back.

Impact:

- A stale alarm handler can overwrite a newer completion update.
- Concurrent action and alarm delivery can regress state or lose fields.

Files:

- `app/src/main/java/com/alex/lensesreminder/receiver/ReminderAlarmReceiver.kt`
- `app/src/main/java/com/alex/lensesreminder/receiver/ReminderActionReceiver.kt`
- `app/src/main/java/com/alex/lensesreminder/data/local/db/WearSessionDao.kt`
- `app/src/main/java/com/alex/lensesreminder/domain/session/SessionLifecycleManager.kt`
- `app/src/main/java/com/alex/lensesreminder/domain/scheduler/ReminderAlarmHandler.kt`

## 5. Single-open-session integrity is not enforced strongly enough

Current behavior:

- Session creation relies on a read-then-write check against `getCurrentSession()`.
- The database schema and DAO do not enforce uniqueness for open sessions.
- The fake DAO used in tests stores only one session at a time, so unit tests cannot reveal multi-row corruption or ordering bugs.

Impact:

- Concurrent starts can create multiple open sessions.
- The repository can then operate on whichever open row happens to be returned by the query.

Files:

- `app/src/main/java/com/alex/lensesreminder/domain/session/SessionLifecycleManager.kt`
- `app/src/main/java/com/alex/lensesreminder/data/local/db/WearSessionDao.kt`
- `app/src/main/java/com/alex/lensesreminder/data/local/db/WearSessionEntity.kt`
- `app/src/test/java/com/alex/lensesreminder/testutil/FakeDaos.kt`

## Implementation Plan

## Workstream 1: Fix Home deadline semantics

Plan:

- Treat `expectedEndAt` as the only wear-end deadline.
- Keep `finalAlertScheduledFor` as a separate informational/reminder timestamp.
- Derive `OVERDUE` UI state from `expectedEndAt`, not from the final alert.

Tasks:

- Update `HomeSessionUiState.toDisplayState()` to stop collapsing wear end and final alert into one derived deadline.
- Keep the "Expected removal" row bound to `expectedEndAt`.
- Continue showing final alert as a separate detail row when present.
- Rewrite `HomeScreenDisplayStateTest` to reflect the intended domain semantics.

Acceptance criteria:

- An `ACTIVE` session remains active until `expectedEndAt`.
- A final alert earlier than wear end does not make the home screen show `OVERDUE`.
- The displayed removal timestamp matches the persisted `expectedEndAt`.

## Workstream 2: Reconcile alarms after settings changes

Plan:

- Introduce a single production path that recalculates the current session’s reminder schedule after profile edits.
- Profile saves must either cancel alarms or recompute the next valid event immediately.

Tasks:

- Add an application service or domain use case that loads the current session and performs reminder reconciliation after profile changes.
- Call that reconciliation from `SettingsViewModel.saveProfile()` after saving the profile.
- Define behavior for each mutable profile field:
  - `remindersEnabled = false`: cancel all session alarms and active notifications
  - `repeatReminderMinutes` changed: recompute the next overdue reminder from persisted session state
  - `finalAlertTime` changed: recompute `finalAlertScheduledFor` and downstream next event
  - `maxWearMinutes` changed while session is active: explicitly decide whether this affects only future sessions or also the current active one, then implement that rule consistently
- Ensure stale previously scheduled alarms are ignored without breaking the reminder chain.

Acceptance criteria:

- Disabling reminders prevents future notifications for the current session.
- Changing repeat interval or final alert time keeps the current session’s schedule internally consistent.
- A settings edit cannot strand an overdue session with no next reminder.

## Workstream 3: Add reminder recovery

Plan:

- Rebuild the next required reminder from persisted session state after lifecycle disruptions.
- Do not rely on stale `scheduledAt == expectedEndAt` matching for recovered overdue flows.

Tasks:

- Add receiver support for the required recovery events:
  - device reboot
  - timezone change
  - manual time change
  - package replaced, if required for app update recovery
- Register those actions in the manifest.
- Add a recovery use case that:
  - loads the current session
  - refreshes session status when needed
  - recomputes only the next valid event
- Either remove `refreshCurrentSessionStatus()` or fix it so recovered overdue sessions can re-enter the reminder chain safely.
- Adjust wear-end / overdue handling so recovery-triggered immediate catch-up does not get rejected as stale solely because wall-clock time moved past `expectedEndAt`.

Acceptance criteria:

- After reboot or time-related recovery, an open session gets a correct next reminder without user intervention.
- Recovered overdue sessions resume a valid reminder chain.
- Recovery logic remains idempotent if triggered more than once.

## Workstream 4: Harden session persistence against stale writes

Plan:

- Stop using broad read-modify-write flows that replace the entire session row without concurrency protection.
- Narrow writes to explicit state transitions, preferably inside transactions.

Tasks:

- Replace or supplement whole-row `REPLACE` writes with targeted DAO operations for key transitions such as:
  - mark planned reminder sent
  - activate planned session
  - mark overdue
  - increment overdue reminder count
  - mark final alert sent
  - complete session
- Use Room transactions for state transitions that depend on current status.
- Add guards so a completion cannot be overwritten by a stale alarm snapshot.
- Review whether receiver work should be serialized through a single executor/dispatcher or protected entirely by database-level compare-and-update logic.

Acceptance criteria:

- A `COMPLETED` session cannot be regressed to `ACTIVE` or `OVERDUE` by stale alarm delivery.
- Duplicate or reordered alarm deliveries are safely ignored.
- State transitions remain idempotent under repeated receiver invocations.

## Workstream 5: Enforce single-open-session integrity

Plan:

- Move the "at most one open session" rule closer to the database boundary.
- Improve tests so the rule is validated against real multi-row behavior.

Tasks:

- Add a stronger persistence-level safeguard for open sessions.
- Choose one of these implementation approaches and document the decision:
  - unique/indexed open-session marker that the database can enforce
  - dedicated singleton table/reference for the current session
  - transaction-based check-and-insert/update with failure propagation
- Update repository/session lifecycle code to surface constraint failures as domain failures instead of silent corruption.
- Replace or extend fake DAO coverage with real Room DAO tests for multi-row behavior.

Acceptance criteria:

- Concurrent start flows cannot persist multiple open sessions.
- Repository methods fail predictably when the single-open-session invariant would be violated.
- Tests exercise real SQL behavior rather than only single-object fakes.

## Testing Plan

Add or update tests for the following:

- Home display-state tests that separate wear end from final alert
- Settings-change tests that verify alarm resync and cancellation behavior
- Recovery tests for reboot/time-change/startup reconciliation
- Reminder handler tests for recovered overdue sessions and stale-alarm rejection rules
- Concurrency-focused tests around action-vs-alarm ordering
- Room DAO tests that validate single-open-session integrity against real SQL
- At least one instrumentation or integration test path for receiver-driven reminder handling

## Prioritization

## P0

- Fix Home deadline semantics and replace the incorrect tests
- Reconcile alarms after settings changes
- Implement recovery path for reboot/time-change/app restart

## P1

- Harden stale-write behavior in receiver/session persistence flows
- Enforce single-open-session integrity at the persistence boundary

## P2

- Expand instrumentation coverage for receiver and notification integration paths

## Suggested Delivery Order

1. Fix Home semantics and tests so visible state matches the domain model.
2. Add profile-change reconciliation so reminders immediately reflect settings.
3. Implement recovery entry points and rebuild the next reminder from persisted state.
4. Harden DAO/repository transition logic against stale writes.
5. Add database-backed integrity tests and integration coverage.
