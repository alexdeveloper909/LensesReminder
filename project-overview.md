# Lenses Reminder Project Overview

This document is the technical starting point for engineers working in this repository. Read it first to understand what the app does, how the main parts fit together, what is already implemented, and which deeper documents to open next.

## Product in One Minute

Lenses Reminder is a safety-focused Android app for people who wear contact lenses and want a simple, reliable way to track lens wear during the day.

The app is built around one core workflow:

1. the user configures their lens profile and reminder settings
2. the user starts a session now or plans one for later
3. the app tracks the active wear session
4. the app reminds the user when the safe wear window ends
5. if the user does not confirm `Lenses off`, the app continues sending overdue reminders
6. the app sends one final stronger alert at the user's configured cutoff time
7. reminders stop when the user confirms `Lenses off` or after the final alert has fired

The product logic is centered on lens wear sessions, overdue state, and persistent reminder behavior until the user explicitly confirms the lenses are out.

## What Exists Today

The current codebase already implements the main MVP reminder flow:

- onboarding for the initial profile
- home dashboard with current lens/session state
- settings screen for profile and reminder configuration
- planned session create, edit, activate, and cancel
- immediate `Start now` session creation
- active and overdue timer UI
- session completion through `Lenses off`
- daily `put lenses on` reminder from Settings
- planned-start reminders
- wear-end reminders
- repeated overdue reminders
- final alert cutoff notification
- background notification actions for `Lenses on`, `Snooze 15 min`, and `Lenses off`
- reminder rescheduling after boot, package replace, time changes, timezone changes, and exact-alarm permission state changes

## Core Domain Concepts

If you are new to the code, these are the most important concepts to keep in mind.

### Lens profile

There is currently one active stored profile. It defines:

- lens type
- maximum wear duration
- whether reminders are enabled
- daily `put lenses on` reminder time
- final alert time
- repeat reminder interval

This profile drives both session calculations and reminder scheduling.

### Wear session

A wear session is the app's main unit of state. It tracks:

- planned start time
- actual start time
- expected end time
- completion time
- session source
- reminder metadata such as last reminder sent and final alert timestamps

The important statuses are:

- `PLANNED`
- `ACTIVE`
- `OVERDUE`
- `COMPLETED`

### Reminder model

The app treats persisted state as the source of truth and recomputes alarms from that state.

- planned sessions can trigger a planned-start reminder
- active sessions schedule a wear-end reminder and possibly a final alert
- overdue sessions schedule repeated reminders until completion or final alert
- stale alarms are ignored by validating the alarm payload against current persisted state

This is why scheduling logic lives in the domain layer instead of directly inside UI code or receivers.

## Runtime Flow

At a high level, the app behaves like this:

1. `LensesReminderApplication` starts and registers notification channels.
2. `MainActivity` renders the Compose app shell.
3. `RootViewModel` decides whether the user should see onboarding or home.
4. Feature view-models load profile, preferences, and session state from repositories.
5. `SessionLifecycleManager` applies session transition rules.
6. Reminder coordinators schedule alarms from persisted session/profile state.
7. Receivers handle fired alarms, notification actions, and system events.
8. The reminder engine updates session state, posts notifications, and schedules the next step in the chain.

## Codebase Map

Main package: `com.alex.lensesreminder`

### `app/`

Application assembly and startup wiring:

- app entry points
- root Compose app
- startup routing
- navigation graph
- Hilt module definitions

### `core/`

Stable cross-cutting types and Android integrations that are shared across the app:

- domain models
- clock abstraction
- notification channel and notification helpers
- permission helpers

### `data/`

Persistence details and repository APIs:

- Room entities, DAOs, database, mappers
- DataStore preference storage
- repositories used by feature and domain layers

### `domain/`

Business logic that should stay independent from screens:

- session lifecycle rules
- reminder scheduling rules
- alarm validation and handling
- recovery/resync logic

### `receiver/`

Android `BroadcastReceiver` entry points:

- reminder alarm receiver
- notification action receiver
- system-event receiver for alarm recovery

### `feature/`

Screen-specific UI and view-models:

- onboarding
- home
- plan session
- settings

### `ui/`

Theme and reusable UI pieces.

## Architecture Rules of Thumb

These are the main architectural expectations in the current code:

- feature code owns UI state and user interactions
- domain code owns session behavior and reminder behavior
- data code owns storage and mapping details
- core code holds reusable models and platform abstractions
- persisted session/profile state is the source of truth for reminders
- Android framework entry points should stay thin and delegate into domain services

Intended dependency direction:

- `feature -> domain/data/core`
- `domain -> data/core`
- `data -> core`
- `app -> feature/domain/data/core`

## Persistence Model

The app uses two storage mechanisms:

### Room

Used for structured product data:

- lens profile
- wear sessions

Schema export is enabled and written to `app/schemas/`.

### DataStore Preferences

Used for lightweight app/UI flags:

- onboarding completion
- notification permission request state
- exact alarm warning dismissal state

## Notifications and Scheduling

The app uses `AlarmManager` for reminder delivery.

Important implementation details:

- exact alarms are used when the platform allows them
- when exact alarms are unavailable, the app falls back to inexact scheduling
- notification actions are background-driven and do not require opening the app
- system-event recovery rebuilds reminder state after events that may invalidate alarms

Relevant manifest permissions:

- `POST_NOTIFICATIONS`
- `SCHEDULE_EXACT_ALARM`
- `RECEIVE_BOOT_COMPLETED`
- `VIBRATE`

## Build and Test Baseline

Current project baseline:

- single-module Android app (`:app`)
- Kotlin
- Jetpack Compose
- Material 3
- Room
- DataStore Preferences
- Hilt
- KSP
- Java 17
- `minSdk = 24`

Useful commands:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Where to Read Next

Use this section as the map from task type to deeper documentation.

### Product intent and MVP boundaries

Read:

- [docs/vision/vision.md](docs/vision/vision.md)
- [docs/spec/technical-spec.md](docs/spec/technical-spec.md)

Use these when you need to understand why the app exists, what user problem it solves, and which behaviors are product-critical.

### Overall code structure

Read:

- [docs/code/architecture.md](docs/code/architecture.md)

Use this when you are orienting yourself in the package layout, startup flow, or dependency boundaries.

### Persistence, entities, repositories

Read:

- [docs/code/data-layer.md](docs/code/data-layer.md)

Use this when changing Room entities, DataStore keys, repositories, or model mapping.

### Reminder engine and alarm behavior

Read:

- [docs/code/reminder-engine.md](docs/code/reminder-engine.md)

Use this when touching alarms, notification actions, scheduling rules, overdue behavior, or resync/recovery paths.

### Screens, navigation, and view-model responsibilities

Read:

- [docs/code/ui-and-navigation.md](docs/code/ui-and-navigation.md)

Use this when changing screen behavior, navigation routes, or view-model ownership.

### Build setup and tooling choices

Read:

- [docs/code/build-and-tooling.md](docs/code/build-and-tooling.md)

Use this when changing Gradle config, Room schema export, Hilt/KSP setup, Java/Kotlin targets, or dependency/tooling upgrades.