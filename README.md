# Lenses Reminder

Android app for tracking daily contact lens wear time and delivering reminders before the safe wear window ends and before sleep.

The product direction lives in [docs/vision/vision.md](docs/vision/vision.md) and the implementation plan lives in [docs/spec/technical-spec.md](docs/spec/technical-spec.md). This README focuses on the codebase as it exists after Phase 1: app foundation.

## Current status

Phase 1 is implemented:

- Compose app shell with onboarding, home, and settings navigation
- Persistent lens profile/settings storage
- DataStore-backed app preferences
- Room database and schema export
- Notification channel bootstrap
- Notification permission entry point on the home screen
- Hilt dependency injection foundation
- Clock abstraction for later scheduling logic

Not implemented yet:

- session lifecycle actions such as `Start now` and `Lenses off`
- planned sessions
- reminder scheduling engine
- notification actions
- reboot/time-change recovery

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
- [docs/code/ui-and-navigation.md](docs/code/ui-and-navigation.md)
- [docs/code/build-and-tooling.md](docs/code/build-and-tooling.md)

## Notes

The project now builds on the AGP 9 default built-in Kotlin/new DSL path. Hilt and KSP were updated to versions that compile cleanly in that setup, so no AGP compatibility opt-out flags are currently needed in `gradle.properties`.
