# Build And Tooling

This document explains the current build setup and the non-obvious tooling choices in the project.

## Main Gradle additions

The app module now includes support for:

- Compose UI
- Navigation Compose
- Room
- DataStore Preferences
- Hilt
- KSP
- Java time desugaring
- coroutine and Room test dependencies

The main build file to inspect is `app/build.gradle.kts`.

## Room schema export

Room schema export is enabled through KSP:

- `room.schemaLocation` points to `app/schemas`
- `room.generateKotlin` is enabled

The exported schema is useful for:

- tracking database evolution
- reviewing migrations later
- validating storage changes in code review

## Hilt and AGP 9 compatibility

The current project template uses AGP 9, which defaults to built-in Kotlin and the new Android DSL.

Hilt in this project currently compiles successfully only with compatibility flags in `gradle.properties`:

- `android.builtInKotlin=false`
- `android.newDsl=false`

These settings are a tooling workaround, not an architectural preference.

They should be revisited later when:

- the Android Gradle Plugin version changes
- Hilt/plugin compatibility improves
- the project is ready for build-system cleanup

## Java and Kotlin target

The project currently builds with Java 17 compatibility and core library desugaring enabled. This is needed so the code can safely use Java time APIs such as:

- `Instant`
- `LocalTime`
- `ZoneId`

## Useful commands

Build the app:

```bash
./gradlew :app:assembleDebug
```

Run unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

## What to document when Phase 2 starts

This file should be extended once the session lifecycle is implemented. The next useful additions will be:

- how session state transitions are modeled
- how feature and domain code are separated
- how tests are organized for use cases and view-models
