# UI And Navigation

This document explains the current Compose UI structure after Phase 1.

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
- `settings`

The route set is intentionally smaller than the full product spec because Phase 1 only implements the foundation screens.

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

Purpose in Phase 1:

- show the empty state shell
- summarize the saved lens profile
- expose notification permission handling
- provide navigation to settings

Current limitations:

- no session card logic yet
- no active timers yet
- no start/stop actions yet

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
- records that a notification permission request was launched

## Notification permission UX

The current notification permission entry point lives on the home screen.

Behavior:

- if reminders are enabled but notification permission is missing, a warning card is shown
- the user can trigger the permission prompt from that card
- the app remembers whether a request has already been attempted

This is groundwork for reminder reliability messaging in later phases.
