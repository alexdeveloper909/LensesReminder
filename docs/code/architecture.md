# Code Architecture

This document explains how the current codebase is organized and how the major layers interact after Phase 2.

## High-level flow

The app currently starts in `LensesReminderApplication`, creates notification channels, and then launches `MainActivity`.

From there:

1. `MainActivity` renders `LensesReminderApp`
2. `LensesReminderApp` asks `RootViewModel` whether onboarding is complete
3. `LensesReminderNavHost` chooses between onboarding, home, and settings
4. Feature view-models load persisted state from repositories and domain services
5. Domain services enforce session lifecycle rules
6. Repositories read from Room or DataStore

## Packages

### `app/`

Contains app-wide wiring:

- `LensesReminderApp.kt`: root Compose entry point
- `RootViewModel.kt`: startup state used to select the first screen
- `navigation/`: route definitions and `NavHost`
- `di/AppModule.kt`: Hilt providers for database, DAOs, DataStore, and clock

This package should stay focused on application assembly, not feature business logic.

### `core/`

Contains reusable types that are not tied to a specific screen or storage implementation:

- `model/`: `LensProfile`, `WearSession`, enums such as `SessionStatus`
- `time/`: `LensClock` abstraction and `SystemLensClock`
- `notification/`: channel registration and permission checks

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

This package is where the app now centralizes behavior that must stay consistent across screens and, later, receivers/notifications.

### `feature/`

Contains screen-specific UI and state holders:

- `onboarding/`
- `home/`
- `settings/`

Each feature package currently keeps its screen and view-model together because the Phase 1 surface area is still small.

## Dependency direction

The intended dependency direction is:

`feature -> data/core`

`feature -> domain`

`domain -> data/core`

`data -> core`

`app -> feature/domain/data/core`

`core -> no app-specific dependencies`

This keeps feature UI dependent on repositories and models, while keeping storage and shared logic out of composables.

## Phase boundaries

The current implementation now includes the session lifecycle, but still stops before the reminder engine and recovery flows. That means:

- the home screen is a real session dashboard with plan/start/stop actions
- `SessionLifecycleManager` owns the Phase 2 rules for session state transitions
- notification channels exist, but actual reminder notifications do not

This keeps Phase 3 focused on scheduling and notification orchestration instead of reworking session state handling.
