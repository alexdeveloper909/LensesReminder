# UI Code Review — Specification & Task List

Findings from reviewing Jetpack Compose UI, ViewModels, navigation, and theme code.

---

## 1. File Organization & Readability

### 1.1 HomeScreen.kt is 1 930+ lines — split into separate files

- [ ] Extract `DisplaySessionUiState`, `SessionHeroContentKey`, `SessionHeroContentModel`, and
  `resolveSessionHeroContent` into a dedicated `HomeScreenDisplayState.kt` file.
- [ ] Extract `HomeOverviewContent` and `resolveHomeOverviewContent` into `HomeOverviewContent.kt`.
- [ ] Extract helper composables that are not screen-specific (`ProgressRing`, `StatusBadge`,
  `SessionDetailRow`, `OverviewMetric`, `ProfileMetricTile`, `StaggeredVisibility`) into
  `ui/component/` or a `feature/home/components/` package.
- [ ] Extract `formatDuration()` extension functions and `Instant?.format()` into a shared
  `core/time/` or `ui/util/` formatting file so they can be reused outside the home feature.

### 1.2 Fully-qualified references in HomeViewModel

`HomeViewModel` uses fully-qualified names for `R.string.*` and `SessionStatus` instead of
imports. Every other ViewModel in the project uses imports.

- [ ] Add `import com.alex.lensesreminder.R` and
  `import com.alex.lensesreminder.core.model.SessionStatus` to `HomeViewModel.kt` and replace
  all FQN usages.

---

## 2. Performance & Recomposition

### 2.1 Duplicate `displaySession` computation every tick

`HomeScreen` computes `displaySession` at line 238 and passes it to `HomeOverviewCard`.
`SessionHeroCard` independently recomputes `displaySession` from the same inputs at line 634.
Because `currentTime` ticks every second, the same `toDisplayState()` mapping runs twice per
second.

- [ ] Compute `displaySession` once in `HomeScreen` and pass it to both `HomeOverviewCard` and
  `SessionHeroCard` as a parameter, removing the redundant `remember` block inside
  `SessionHeroCard`.

### 2.2 Date string reformatted every second

Inside `HomeOverviewCard`, the date label is formatted from `currentTime` every second:

```kotlin
currentTime.atZone(zoneId).format(fullDateFormatter)
```

The output only changes once per day.

- [ ] Derive the formatted date with `remember(currentTime) { ... }` keyed on
  `currentTime.atZone(zoneId).toLocalDate()` so the format call runs only when the date actually
  changes.

### 2.3 Duplicate DateTimeFormatter instances

`timeFormatter` (line 220) and `localTimeFormatter` (line 232) in `HomeScreen` are identical —
both are `DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)`.

- [ ] Remove `localTimeFormatter` and pass the single `timeFormatter` to
  `StartSessionBottomSheet`.

---

## 3. State Management

### 3.1 Non-atomic state update in SettingsViewModel

`SettingsViewModel` uses `mutableUiState.value = mutableUiState.value.copy(...)` for every
input handler. This is a read-modify-write sequence that is not thread-safe.
`PlanSessionViewModel` in the same project correctly uses `mutableUiState.update { ... }`.

- [ ] Replace all `.value = .value.copy(...)` calls in `SettingsViewModel` with
  `mutableUiState.update { it.copy(...) }`.

### 3.2 One-shot events use SharedFlow — consider Channel

`HomeViewModel`, `SettingsViewModel`, and `PlanSessionViewModel` all use `MutableSharedFlow`
for one-shot UI events (snackbar messages, navigation signals). `SharedFlow` with default
`replay = 0` will silently drop events emitted when no collector is active (e.g., during a
configuration change or before the `LaunchedEffect` collector starts).

`Channel(capacity = Channel.BUFFERED)` with `receiveAsFlow()` guarantees that events are
buffered and delivered once a collector appears.

- [ ] In `HomeViewModel`, `SettingsViewModel`, and `PlanSessionViewModel`: replace
  `MutableSharedFlow<*>()` with `Channel<*>(Channel.BUFFERED)` and expose via
  `.receiveAsFlow()`.

### 3.3 Missing permission-state refresh on resume in SettingsScreen

`HomeRoute` uses a `DisposableEffect` + `ON_RESUME` observer to re-read notification and
exact-alarm permission states when the user returns from system settings.
`SettingsEditorContent` reads `ExactAlarmPermissionManager.canScheduleExactAlarms(context)` only
once (at composition) and via the activity-result launcher callback — but not on resume.

If the user navigates to the system exact-alarm settings screen via the device's recent-apps
switcher instead of the launcher, the banner won't update.

- [ ] Add a `DisposableEffect`/`ON_RESUME` observer in `SettingsEditorContent` (or hoist the
  check to `SettingsEditorScreen`) to refresh `hasExactAlarmPermission` when the screen
  resumes, matching the pattern in `HomeRoute`.

---

## 4. Architectural Issues

### 4.1 SettingsEditorScreen receives ViewModel directly

`SettingsEditorScreen` accepts `viewModel: SettingsViewModel` as a parameter and calls
`collectAsStateWithLifecycle()` internally. This couples the composable to the concrete
ViewModel, making it impossible to preview with `@Preview` or test in isolation.

`HomeScreen` correctly follows the state-hoisting pattern: `HomeRoute` collects state and passes
primitive/data-class parameters down to `HomeScreen`.

- [ ] Refactor `SettingsEditorScreen` to accept `uiState: SettingsUiState`,
  individual event callbacks (`onMaxWearHoursChanged`, `onSaveClick`, etc.), and
  `snackbarHostState: SnackbarHostState` as parameters.
- [ ] Move `collectAsStateWithLifecycle()`, event collection, and ViewModel interaction into
  `SettingsRoute` (and `OnboardingRoute`), mirroring the HomeRoute/HomeScreen split.

### 4.2 Legacy View-based DatePickerDialog in PlanSessionScreen

`PlanSessionScreen` uses `android.app.DatePickerDialog` — the framework View-based dialog.
`HomeScreen` already uses the Compose `androidx.compose.material3.DatePickerDialog`. The
inconsistency introduces:

- Mixed View/Compose lifecycle ownership.
- The legacy dialog doesn't respect Material 3 theming.
- `DatePickerDialog(context, ...).show()` inside an `onClick` lambda is a side-effect outside
  Compose's state management.

- [ ] Replace `android.app.DatePickerDialog` usage in `PlanSessionScreen` with Compose Material 3
  `DatePickerDialog` + `rememberDatePickerState`, following the same pattern used in
  `StartSessionBottomSheet` in `HomeScreen.kt`.

### 4.3 String-based navigation routes

`AppDestination` is an enum with string routes, and `LensesReminderNavHost` uses the
`composable(route = ...)` API. Since Navigation Compose 2.8+, the recommended approach is
type-safe navigation using `@Serializable` data objects/classes with the `composable<T>` API.

- [ ] Migrate `AppDestination` to `@Serializable` data objects (e.g.,
  `@Serializable data object Home` ) and update `LensesReminderNavHost` to use
  `composable<AppDestination.Home>` instead of string routes.

---

## 5. Internationalization

### 5.1 Hardcoded duration format strings

`formatDuration()` (both the `Duration?` and `Int` overloads at the bottom of `HomeScreen.kt`)
uses hardcoded `"h"` and `"m"` suffixes:

```kotlin
"${hours}h ${minutes}m"
```

These are not translatable and won't render correctly in locales where unit abbreviations differ.

- [ ] Replace with `pluralStringResource` / `stringResource` using parameterized string
  resources (e.g., `@string/duration_hours_minutes`) or use `android.icu.text.MeasureFormat`
  for locale-aware duration formatting.

---

## 6. Minor Code Quality

### 6.1 Redundant `isLoading = false` in SettingsViewModel input handlers

Every input handler in `SettingsViewModel` (`onMaxWearHoursChanged`, `onMaxWearMinutesChanged`,
`onRemindersEnabledChanged`, etc.) sets `isLoading = false` in the copy. The loading flag is
already set to `false` after the initial profile load and never goes back to `true`.

- [ ] Remove `isLoading = false` from all input-handler `copy()` calls in `SettingsViewModel`.

### 6.2 `PlanSessionUiState.selectedDate` defaults to `LocalDate.now()` at construction time

`PlanSessionUiState` has `val selectedDate: LocalDate = LocalDate.now()`. The default is
evaluated at class instantiation, which means the initial state created in `MutableStateFlow`
captures the date at ViewModel creation time. If the ViewModel is retained overnight, the
default date is stale. In practice the value is immediately overwritten by the `init` block, so
this is cosmetic — but using a sentinel like `LocalDate.MIN` or making the field nullable would
make the intent clearer.

- [ ] Consider making `selectedDate` nullable or documenting that the default is always
  overwritten before the UI reads it.
