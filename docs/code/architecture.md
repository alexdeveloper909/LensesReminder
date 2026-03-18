# Code Architecture

This document explains how the current codebase is organized and how the main layers interact in the current implementation.

## High-level flow

The app currently starts in `LensesReminderApplication`, creates notification channels, and then launches `MainActivity`.

From there:

1. `MainActivity` renders `LensesReminderApp`
2. `LensesReminderApp` asks `RootViewModel` whether onboarding is complete
3. `LensesReminderNavHost` chooses between onboarding, home, and settings
4. Feature view-models load persisted state from repositories and domain services
5. Domain services enforce session lifecycle and reminder rules
6. Receivers handle fired alarms, notification actions, and system resync events in the background
7. Repositories read from Room or DataStore

## Packages

### `app/`

Contains app-wide wiring:

- `LensesReminderApp.kt`: root Compose entry point
- `RootViewModel.kt`: startup state used to select the first screen
- `navigation/`: route definitions and `NavHost`
- `di/AppModule.kt`: Hilt providers for database, DAOs, DataStore, and clock
- receiver bindings are still declared in the manifest because they are Android entry points

This package should stay focused on application assembly, not feature business logic.

### `core/`

Contains reusable types that are not tied to a specific screen or storage implementation:

- `model/`: `LensProfile`, `WearSession`, enums such as `SessionStatus`
- `time/`: `LensClock` abstraction and `SystemLensClock`
- `notification/`: channel registration, permission checks, and system notification publishing

The `core` package is intended to stay stable as the rest of the app grows.

### `data/`

Contains persistence and repository code:

- `local/db/`: Room entities, DAOs, database, type converters, entity mappers
- `local/datastore/`: lightweight preference models
- `repository/`: profile, session, and app-preference repositories

This layer owns storage details and should be the only layer that knows about Room table shapes or DataStore keys.

### `domain/`

Contains business rules that sit above repositories but below screen state:

- `session/SessionLifecycleManager.kt`: Phase 2 lifecycle rules for starting, planning, activating, completing, cancelling, and expiring sessions
- `scheduler/`: reminder scheduling, alarm handling, daily start reminders, profile-driven reminder reconciliation, and recovery/resync rules

This package is where the app centralizes behavior that must stay consistent across screens, receivers, and background notification flows.

### `receiver/`

Contains the Android `BroadcastReceiver` entry points for:

- reminder alarms
- notification actions
- system events that require reminder resynchronization

### `feature/`

Contains screen-specific UI and state holders:

- `onboarding/`
- `home/`
- `plan/`
- `settings/`

Each feature package currently keeps its screen and view-model together because the app is still a single-module MVP.

## Dependency direction

The intended dependency direction is:

`feature -> data/core`

`feature -> domain`

`domain -> data/core`

`data -> core`

`app -> feature/domain/data/core`

`core -> no app-specific dependencies`

This keeps feature UI dependent on repositories and models, while keeping storage and shared logic out of composables.

## Current implementation boundaries

The current implementation includes the session lifecycle, reminder engine, and reminder recovery/resync flows. That means:

- the home screen is a real session dashboard with plan/start/stop actions
- `SessionLifecycleManager` owns the Phase 2 rules for session state transitions
- `ReminderScheduleCoordinator` and `ReminderAlarmHandler` own the reminder chain
- `DailyStartReminderCoordinator`, `ProfileReminderReconciler`, and `ReminderStateRescheduler` keep reminder state aligned with profile edits and system events
- reboot, package replace, time-change, timezone-change, and exact-alarm-permission-change recovery are implemented through `ReminderSystemEventReceiver`

Still deferred:

- manual start correction / backfill flow

The current code keeps reminder policy mostly outside Android framework classes, so the reminder chain can be unit-tested without instrumented tests.
