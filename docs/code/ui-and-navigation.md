# UI And Navigation

This document explains the current Compose UI structure after Phase 2.

## Entry points

### `MainActivity`

Hosts the Compose tree and applies the app theme.

### `LensesReminderApp`

Acts as the root composable for startup routing.

It reads `RootViewModel.uiState` and decides whether the first destination should be:

- `setup`
- `home`

## Navigation graph

Navigation is defined in `app/navigation/LensesReminderNavHost.kt`.

Current routes:

- `setup`
- `home`
- `planSession`
- `settings`

The route set is still smaller than the full product spec because correction/recovery flows are deferred to later phases.

## Implemented screens

### Onboarding

Package: `feature/onboarding/`

Purpose:

- collect the initial lens profile
- mark onboarding complete

Implementation notes:

- reuses the same editor UI as settings
- saves through `SettingsViewModel`
- navigates to home on success

### Home

Package: `feature/home/`

Purpose in Phase 2:

- show the current session state: out, planned, active, or overdue
- expose `Start now`, `Plan for later`, `Lenses on`, `Lenses off`, and plan-cancel actions
- render elapsed, remaining, and overdue timing while the app is open
- summarize the saved reminder profile
- expose notification permission handling
- provide navigation to settings and plan editing

Implementation notes:

- `HomeViewModel` combines profile data, app preferences, the current session, and a clock ticker
- overdue state is reflected in the UI as soon as the expected end time passes
- the screen still stops short of scheduling notifications

### Plan session

Package: `feature/plan/`

Purpose:

- create or edit the single planned session
- show the inherited reminder configuration from settings

Implementation notes:

- uses platform date/time picker dialogs
- saves through `PlanSessionViewModel`
- reuses the same route for both create and edit

### Settings

Package: `feature/settings/`

Purpose:

- edit the persisted lens profile after onboarding

Implementation notes:

- shared editor composable between onboarding and settings
- validates wear duration before save
- supports reminder toggle and final alert time selection

## View-model responsibilities

### `RootViewModel`

- observes app preferences
- decides whether onboarding has been completed

### `SettingsViewModel`

- loads the persisted profile
- owns editable form state
- validates and saves profile updates
- optionally marks onboarding complete

### `HomeViewModel`

- combines profile state with app preference state
- combines current session state with a live clock ticker
- records that a notification permission request was launched
- delegates lifecycle actions to `SessionLifecycleManager`

### `PlanSessionViewModel`

- loads an existing planned session when present
- owns the plan date/time form state
- validates and saves planned-session changes

## Notification permission UX

The current notification permission entry point lives on the home screen.

Behavior:

- if reminders are enabled but notification permission is missing, a warning card is shown
- the user can trigger the permission prompt from that card
- the app remembers whether a request has already been attempted

This is groundwork for reminder reliability messaging in later phases, when actual reminder scheduling is added.
