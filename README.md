# Lenses Reminder

Lenses Reminder is an Android app for tracking daily contact lens wear and sending reminders before the safe wear window ends, after the session becomes overdue, and before the user's configured final alert cutoff.

The codebase is built with Kotlin, Jetpack Compose, Room, DataStore, Hilt, and AlarmManager-based reminder scheduling.

## What the App Does

The current app supports the core daily lens workflow:

- onboarding flow for creating the initial lens profile
- home screen dashboard for the current lens state
- `Start now` flow for immediate wear sessions
- delayed manual start flow for logging the real wear start time when the user forgot to start on time
- planned session create, edit, activate, and cancel flows
- active and overdue session timing on the home screen
- `Lenses off` completion flow
- daily `put lenses on` reminder configured from Settings
- wear-end reminders, repeated overdue reminders, and a final alert cutoff
- background notification actions for `Lenses on`, `Snooze 15 min`, and `Lenses off`
- alarm rescheduling after boot, app replacement, time changes, timezone changes, and exact-alarm permission state changes

## Current State

- Compose-based onboarding, home, planning, and settings screens
- persistent lens profile storage
- Room-backed wear session storage with exported schemas
- DataStore-backed app preferences
- Hilt dependency injection
- notification channel setup and notification permission handling
- a domain-layer session lifecycle manager
- a reminder engine that recomputes alarms from persisted state

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Room
- DataStore Preferences
- Hilt
- KSP
- AlarmManager

## Architecture

Main package: `com.alex.lensesreminder`

- `app/`: app entry points, startup routing, DI, navigation
- `core/`: shared models, clock abstraction, notifications
- `data/`: Room entities/DAOs, DataStore models, repositories, mappers
- `domain/`: session lifecycle and reminder scheduling logic
- `receiver/`: BroadcastReceiver entry points for alarms, actions, and system rescheduling
- `feature/`: screen-specific UI and view-models
- `ui/`: theme and reusable UI components

Dependency direction is intentionally layered:

- `feature -> domain/data/core`
- `domain -> data/core`
- `data -> core`
- `app -> everything`

This keeps business rules out of composables and keeps storage concerns out of the UI layer.

## Reminder Model

The reminder engine uses persisted app state as the source of truth and rebuilds alarms from that state.

- planned sessions schedule a single planned-start reminder
- active sessions schedule a wear-end reminder and, when applicable, a final alert
- overdue sessions continue with repeated reminders until the final alert cutoff
- stale or duplicate alarms are ignored through alarm validation and idempotent handling
- system-event recovery resynchronizes reminders after events that can invalidate alarms

## Requirements

- Android Studio with Android SDK support
- JDK 17
- Android device or emulator with API 24+

Project Android targets:

- `minSdk = 24`
- `targetSdk = 36`
- `compileSdk = 36`

## Getting Started

1. Open the project in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on a device or emulator.

If you only want to try the app, download the latest signed APK from the repository's Releases page.

If you prefer the command line, assemble the debug APK with:

```bash
./gradlew :app:assembleDebug
```

## Testing

Run the current JVM/unit test suite:

```bash
./gradlew :app:testDebugUnitTest
```

Useful additional commands:

```bash
./gradlew :app:assembleDebug
./gradlew :app:test
./gradlew :app:lint
```

## Data and Persistence

The app uses two persistence layers:

- Room for structured product data such as lens profiles and wear sessions
- DataStore Preferences for app-level flags such as onboarding completion and permission-related UI state

Room schema export is enabled via KSP and written to `app/schemas/`.

## Notifications and Permissions

The app declares and uses:

- `POST_NOTIFICATIONS`
- `SCHEDULE_EXACT_ALARM`
- `RECEIVE_BOOT_COMPLETED`
- `VIBRATE`

Notification reliability depends on platform capabilities and user settings. When exact alarms are unavailable, reminder delivery falls back to inexact alarms.

## Project Documentation

Product and implementation context lives in:

- [project-overview.md](project-overview.md)
- [docs/vision/vision.md](docs/vision/vision.md)
- [docs/spec/technical-spec.md](docs/spec/technical-spec.md)

Code-oriented documentation:

- [docs/code/architecture.md](docs/code/architecture.md)
- [docs/code/data-layer.md](docs/code/data-layer.md)
- [docs/code/reminder-engine.md](docs/code/reminder-engine.md)
- [docs/code/ui-and-navigation.md](docs/code/ui-and-navigation.md)
- [docs/code/build-and-tooling.md](docs/code/build-and-tooling.md)
- [docs/code/release-apk-ci.md](docs/code/release-apk-ci.md)

## Notes

The project uses the AGP 9 built-in Kotlin/new DSL path and currently documents a working setup around:

- AGP `9.1.0`
- Hilt `2.59.1`
- KSP `2.3.6`

Java 17 compatibility and core library desugaring are enabled so the app can use Java time APIs such as `Instant`, `LocalTime`, and `ZoneId`.
