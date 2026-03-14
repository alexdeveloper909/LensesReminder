# Lenses Reminder

Android app for tracking daily contact lens wear time and delivering reminders before the safe wear window ends and before sleep.

The product direction lives in [docs/vision/vision.md](docs/vision/vision.md) and the implementation plan lives in [docs/spec/technical-spec.md](docs/spec/technical-spec.md). This README focuses on the codebase as it exists after Phase 3: reminder engine.

## Current status

Phase 1, Phase 2, and Phase 3 are implemented:

- Compose app shell with onboarding, home, and settings navigation
- Persistent lens profile/settings storage
- DataStore-backed app preferences
- Room database and schema export
- Notification channel bootstrap
- Notification permission entry point on the home screen
- Hilt dependency injection foundation
- Clock abstraction for later scheduling logic
- `Start now` session creation
- Planned session create/edit/cancel flow
- Planned-session activation into an active session
- Active and overdue timer UI on the home screen
- Session completion via `Lenses off`
- `AlarmManager` reminder scheduling with exact-alarm fallback behavior
- Planned start reminder notifications
- Wear-end and repeated overdue reminder notifications
- Final alert cutoff notification
- Background notification actions for `Lenses on`, `Snooze 15 min`, and `Lenses off`

Not implemented yet:

- reboot/time-change recovery
- manual start correction

## Tech stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Room
- DataStore Preferences
- Hilt
- KSP

## Project structure

Main package: `com.alex.lensesreminder`

- `app/`: root app entry points, startup state, navigation, DI
- `core/`: domain-neutral models and helpers
- `data/`: Room, DataStore, repositories, mappers
- `feature/`: screen-level UI and view-models
- `ui/theme/`: Compose theme

## Build

Assemble the debug app:

```bash
./gradlew :app:assembleDebug
```

## Quality gate

Run the current JVM test suite:

```bash
./gradlew :app:testDebugUnitTest
```

## Documentation

Code-oriented docs are in `docs/code/`:

- [docs/code/architecture.md](docs/code/architecture.md)
- [docs/code/data-layer.md](docs/code/data-layer.md)
- [docs/code/reminder-engine.md](docs/code/reminder-engine.md)
- [docs/code/ui-and-navigation.md](docs/code/ui-and-navigation.md)
- [docs/code/build-and-tooling.md](docs/code/build-and-tooling.md)

## Notes

The project now builds on the AGP 9 default built-in Kotlin/new DSL path. Hilt and KSP were updated to versions that compile cleanly in that setup, so no AGP compatibility opt-out flags are currently needed in `gradle.properties`.
