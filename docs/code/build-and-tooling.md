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

## Hilt, KSP, and AGP 9 compatibility

The current project template uses AGP 9, which defaults to built-in Kotlin and the new Android DSL.

The project now works on that default path without compatibility opt-out flags.

The working combination is:

- Hilt `2.59.1`
- KSP `2.3.6`
- AGP `9.1.0`

The earlier AGP opt-out flags were removed after updating Hilt and KSP. If this area breaks again during future upgrades, check Hilt and KSP release notes first before reintroducing Gradle property workarounds.

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

Other useful commands:

```bash
./gradlew :app:lint
./gradlew :app:assembleDebug
```

## Current notes

- the project is still a single-module Android app (`:app`)
- Room schema export is committed under `app/schemas/` and should stay updated when storage changes
- the build uses Java 17 compatibility plus core library desugaring so Java time APIs remain available on `minSdk 24`
- if AGP/Hilt/KSP compatibility breaks during upgrades, inspect the version catalog in `gradle/libs.versions.toml` first
