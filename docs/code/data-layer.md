# Data Layer

This document explains the persistence and repository shape after Phase 2.

## Storage split

The app uses two persistence mechanisms:

### Room

Used for structured app data that is likely to evolve:

- `LensProfileEntity`
- `WearSessionEntity`

Room is defined in `data/local/db/`.

The database class is `LensesReminderDatabase`, which currently contains:

- `lens_profiles`
- `wear_sessions`

Schema export is enabled and written to `app/schemas/`.

### DataStore Preferences

Used for lightweight app-level flags in `data/local/datastore/AppPreferences.kt`:

- `has_completed_onboarding`
- `notifications_permission_requested`
- `exact_alarm_warning_dismissed`

These are UI/application preferences rather than primary product data, so DataStore is a better fit than Room.

## Entities and domain models

The code intentionally separates storage entities from domain models.

### Domain models

Defined in `core/model/`:

- `LensProfile`
- `WearSession`
- `LensType`
- `SessionStatus`
- `SessionSource`

These are the types feature code should depend on.

### Room entities

Defined in `data/local/db/`:

- `LensProfileEntity`
- `WearSessionEntity`

These represent how the same information is stored in SQLite.

## Mappers

`EntityMappers.kt` converts between Room entities and domain models:

- `LensProfileEntity.toDomain()`
- `LensProfile.toEntity()`
- `WearSessionEntity.toDomain()`
- `WearSession.toEntity()`

This prevents UI and business logic from depending directly on Room-specific classes.

## DAOs

### `LensProfileDao`

Responsibilities:

- observe the single stored profile
- upsert that profile

The app currently assumes a single active profile row with `id = 1`.

### `WearSessionDao`

Responsibilities:

- observe the current non-completed session
- upsert session snapshots

The query already reflects the intended MVP rule that only one planned/active/overdue session should matter at a time.

## Repositories

Repositories provide the interface the rest of the app uses.

### `LensProfileRepository`

- exposes a `Flow<LensProfile>`
- returns a default profile when the database is empty
- saves the active profile

### `WearSessionRepository`

- exposes the current session as `Flow<WearSession?>`
- exposes the current session synchronously for lifecycle rules
- saves session snapshots

Phase 2 uses this repository directly from `SessionLifecycleManager` and `HomeViewModel`.

### `AppPreferencesRepository`

- exposes `Flow<AppPreferences>`
- stores onboarding and permission-flow flags

## Time handling

The data layer stores session timestamps in UTC-friendly types using `Instant`.

User-facing time calculations should use `LensClock.zoneId()` so scheduling, session timing, and display code stay testable and timezone-aware.
