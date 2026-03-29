# Product Improvements Specification — Round 2

## Context

This specification is a follow-up to `product-improvements-spec-1.md`. Where spec-1 focused on feature expansion (inventory, extended wear types, widgets, Wear OS), this document focuses on **UX friction**, **visual polish**, **flow improvements**, and **quality-of-life enhancements** found during a thorough code and design review of the current app.

Every item here comes from reading the actual source code, examining screenshots, and analyzing user flows end to end.

---

## 1. UX Issues Found in Current Build

### 1.1. "Plan for Later" Is Not Reachable from the Home Screen

**Problem:** The idle home screen shows only a `Start now` button. The `onPlanForLater` callback is wired into `HomeScreen` but never attached to any visible UI element in `IdleSessionContent`. A user who wants to plan a session for later has no way to discover this feature from the home screen.

**Evidence:** `HomeScreen.kt` defines `onPlanForLater` as a parameter passed down through `HomeScreen`, but `IdleSessionContent` only receives and renders `onStartNow`. The `onPlanForLater` parameter is unused in the composable body.

**Fix:** Add a secondary `OutlinedButton` or `FilledTonalButton` labeled "Plan for later" directly below the "Start now" button in `IdleSessionContent`. This is the natural place for it — the user has no session and is deciding what to do.

### 1.2. "Lenses Off" Has No Confirmation or Undo

**Problem:** Tapping "Lenses off" immediately ends the session. A single accidental tap (easy on a phone held in one hand) terminates the session with no way to recover. The only feedback is a snackbar that says "Lenses marked as off."

**Fix:** After the user taps "Lenses off", show a `Snackbar` with an **"Undo"** action that persists for 5 seconds. If the user does not tap undo, the completion is finalized. If they tap undo, the session is restored to its previous state (active or overdue). This pattern is standard in Material Design for destructive actions.

### DONE 1.3. Session Completion Feels Anticlimactic

**Problem:** After completing a session, the user is shown the idle state with the generic "No lens session for today yet" message. There is no summary of how long they wore their lenses, whether they removed them on time, or any positive reinforcement.

**Fix:** After completing a session, show a brief **completion summary card** in place of the idle card for one app visit. This card should display:
- Total wear time for the completed session
- Whether lenses were removed on time or overdue
- A congratulatory message if on time ("Great — you removed your lenses within the safe window")
- A gentle nudge if overdue ("Your lenses were in 45 minutes past the recommended time")

The card should have a "Got it" or auto-dismiss behavior that returns to the standard idle state.

### DONE 1.4. DatePickerDialog Uses Android Views Inside a Compose App

**Problem:** The `StartSessionBottomSheet` uses `android.app.DatePickerDialog` (a View-based dialog) inside a fully Compose-based UI. This creates visual inconsistency — the time picker is a Material 3 Compose dialog, but the date picker looks like an older Android dialog.

**Fix:** Replace `DatePickerDialog(context, ...)` with `androidx.compose.material3.DatePickerDialog` and `DatePicker` from the Material 3 Compose library. This ensures consistent visual language across the app.

---

## 2. UI Polish & Modern Android Design

### 2.1. Enable Dynamic Color (Material You)

**Problem:** The `LensesReminderTheme` composable has `dynamicColor: Boolean = false`. On devices running Android 12+, this means the app uses a static teal palette instead of adapting to the user's wallpaper-derived colors. The app feels disconnected from the rest of the system.

**Fix:** Change the default to `dynamicColor = true`. The static teal/slate/amber palette should remain as the fallback for devices below Android 12. This is a single-line change that significantly improves perceived integration on Pixel and Samsung devices.

### 2.2. Add Subtle Animations to the Idle State

**Problem:** The idle state is visually static — a flat green checkmark circle and text. It communicates "nothing is happening" but feels lifeless compared to the active state with its animated progress ring.

**Fix:**
- Add a subtle **breathing animation** (slow scale pulse between 0.95 and 1.0) on the checkmark circle.
- When transitioning from active/overdue to idle (after completing a session), use an `AnimatedContent` transition instead of a hard state swap.
- Stagger the entrance of the card content using `AnimatedVisibility` with slight delays.

### 2.3. Overdue State Should Escalate Visual Urgency

**Problem:** The overdue state looks the same whether the user is 1 minute overdue or 3 hours overdue. The red error container and warning icon are appropriate but static. There is no increasing sense of urgency.

**Fix:**
- Add a **pulsing animation** on the warning icon that starts subtle and becomes more pronounced over time.
- The overdue duration text inside the progress ring should pulse gently.
- Consider adding a very subtle **red glow/shadow** around the overdue card that intensifies with time. Use `animateFloatAsState` tied to the overdue duration.

### 2.4. Typography Is Default System Font

**Problem:** The `Type.kt` file uses `FontFamily.Default` for everything. This means the app uses Roboto everywhere, which is functional but gives the app a generic, template-like feel.

**Fix:** Introduce a distinctive display font from Google Fonts for headline and display text styles (e.g., the session status titles, the countdown timer). Body and label text can remain the default for readability. Candidates:
- **Inter** for body text (excellent legibility, purpose-built for UIs)
- **Plus Jakarta Sans** or **DM Sans** for headlines (modern, slightly rounded, friendly for a health app)

Bundle the fonts as resources and define them in `Type.kt`.

### 2.5. Progress Ring Enhancement

**Problem:** The current progress ring is a simple arc with uniform color and weight. It works but feels plain compared to modern health/fitness app designs.

**Fix:**
- Increase the `strokeWidth` slightly (from 10dp to 12-14dp) for more visual presence.
- Add a **gradient effect** to the progress arc so it transitions from a lighter shade at the start to the full primary color at the current position.
- Add a small **dot indicator** at the leading edge of the progress arc (a filled circle at the end of the sweep).
- Add a subtle **glow/shadow** behind the progress arc for depth.

---

## 3. User Experience Flow Improvements

### 3.1. Contextual Empty State Messaging

**Problem:** The idle state always shows "No lens session for today yet" regardless of time of day or user patterns. This is accurate but not helpful.

**Fix:** Make the idle state message time-aware:
- **Morning (before daily reminder time):** "Good morning — ready to put your lenses in?"
- **Around daily reminder time:** "Time for your lenses! Start whenever you're ready."
- **Evening (after final alert time):** "Rest day for your eyes. See you tomorrow."
- **After completing a session today:** Show the completion summary (see 1.3).

This makes the app feel more personal and aware of the user's routine.

### 3.2. Improved Onboarding Experience

**Problem:** The onboarding screen is a direct reuse of the settings editor (`SettingsEditorScreen`). While this avoids code duplication, the first-run experience feels utilitarian and cold. New users are immediately confronted with a dense settings form titled "First setup" with no explanation of what the app does or why they should care.

**Fix:** Create a proper multi-step onboarding flow:
1. **Welcome screen:** Brief app introduction (1-2 sentences + illustration/icon) explaining the core value: "We'll help you track your daily lens wear and remind you when it's time to take them off."
2. **Lens profile setup:** The existing settings fields, but presented in a cleaner, focused layout with one concept per step (wear duration on one page, reminders on the next).
3. **Permission setup:** Dedicated screen explaining why notification and alarm permissions matter, with clear "Allow" actions.
4. **Confirmation:** "You're all set! Here's a summary of your settings."

The existing `SettingsEditorScreen` should remain for the settings route. Onboarding should be its own flow.

### 3.3. Sound Preview in Settings

**Problem:** The app includes custom notification sounds (`final_alert_sound.wav`, `minor_reminder_sound.wav`) but users cannot preview them from settings. They will only hear these sounds when an actual notification fires, which may be startling.

**Fix:** Add a small speaker/play icon button next to the "Final alert time" and "Overdue interval" settings rows. Tapping it plays a short preview of the associated sound. This sets user expectations and lets them verify their device volume is appropriate.

### 3.4. Settings Screen Organization

**Problem:** The settings screen is a single continuous scroll with two card groups (Lens Type and Reminders Enabled). The reminder settings card contains five distinct settings (toggle, final alert time, put lenses on time, daily start reminder, overdue interval) packed into one card. It is dense and feels overwhelming, especially during onboarding.

**Fix:**
- Split the single reminders card into focused sections with clear visual separation:
  - **Daily Routine:** Put lenses on reminder time
  - **Safety Alerts:** Max wear duration, final alert time, overdue interval
  - **Master toggle:** Reminders enabled/disabled (at the top, controlling all child sections)
- Add section headers with descriptive subtitles.
- Consider using expandable/collapsible sections so disabled reminders collapse their child settings.

---

## 4. New Feature Concepts

### 4.1. Safety Streak Counter

**Problem:** There is no positive feedback loop for consistent safe behavior. The app only shows the current session — no sense of progress or accomplishment.

**Solution:** Track consecutive days of removing lenses within the safe wear window. Display a small streak badge or counter on the home screen (e.g., "7-day safe streak"). When the streak grows, show a congratulatory micro-interaction. If the user breaks the streak (overdue removal), show a supportive message rather than punitive ("Let's get back on track tomorrow").

This adds lightweight gamification that reinforces the core safety mission without being gimmicky.

**Implementation:**
- Add a `removedOnTime: Boolean` field to `WearSession` (computed at completion: `completedAt <= expectedEndAt`).
- Add a repository query to calculate the current streak from completed sessions.
- Display in a small chip or badge on the home screen.

### 4.2. Post-Session Comfort Check

**Problem:** Users have no way to log how their eyes feel during or after wearing lenses. This is a common concern (dry eyes, redness, irritation) and optometrists often ask about wearing patterns and comfort levels.

**Solution:** After the user taps "Lenses off", optionally show a quick comfort prompt:
- Three to five emoji-based options: Comfortable, Slightly Dry, Dry, Irritated, Painful
- A text field for optional notes
- A "Skip" option for users who don't want to log every time

This data gets stored with the `WearSession` and becomes valuable in the history/insights feature (spec-1, section 1.4). Users can share it with their optometrist.

**Implementation:**
- Add `comfortRating: Int?` and `comfortNote: String?` to `WearSessionEntity`.
- Room migration to add the columns.
- A brief bottom sheet or dialog after session completion.
- DataStore preference to disable the comfort prompt for users who never want it.

### 4.3. Weekly Summary Notification

**Problem:** The app is session-focused. Users have no awareness of their weekly patterns unless they mentally track it themselves.

**Solution:** Send a weekly summary notification (configurable day/time, defaulting to Sunday evening) with:
- Number of sessions this week
- Average wear time
- Days lenses were removed on time vs. overdue
- Current safety streak

This encourages engagement without being intrusive (once per week) and reinforces awareness of wearing habits.

**Implementation:**
- Schedule via `AlarmManager` (weekly repeating).
- Query `WearSessionRepository` for sessions in the past 7 days.
- Format as a compact notification with an "Open details" action.
- Add a toggle in settings to enable/disable weekly summaries.

### 4.4. Quick Settings Tile for Session Control

**Problem:** Starting or stopping a session requires unlocking the phone, finding the app, opening it, and tapping the button. For a "minimal friction" app (core principle), this is too many steps.

**Solution:** Add an Android Quick Settings Tile that:
- Shows "Lenses out" / "Lenses in (Xh Ym)" status text
- Tapping it toggles: starts a session if idle, opens a compact "Lenses off" confirmation if active/overdue
- Uses a lens/eye icon that changes between states

**Implementation:**
- Subclass `TileService`.
- Bind to `WearSessionRepository.currentSession` for live tile state.
- Use `SessionLifecycleManager` for start/complete actions.
- Register in `AndroidManifest.xml`.

### 4.5. Persistent Notification During Active Session

**Problem:** When lenses are in, the user has no persistent visual indicator except opening the app. They may check the notification shade and see nothing until a reminder fires.

**Solution:** While a session is active, show a non-dismissible notification with:
- Remaining time (or overdue duration) that updates periodically
- "Lenses off" quick action
- Tapping the notification opens the app to the home screen

This acts as a passive awareness layer. Use a `ForegroundService` with a low-priority notification channel so it doesn't feel intrusive but stays visible in the shade.

**Implementation:**
- Foreground service started when session activates, stopped on completion.
- Notification updates via a periodic handler (every minute).
- Dedicated notification channel with `IMPORTANCE_LOW` (no sound, just visible in shade).

---

## 5. Technical Quality & Code Health

### 5.1. Enable R8/ProGuard for Release Builds

**Problem:** `isMinifyEnabled = false` in the release build type. This means the release APK ships with all debug symbols, unused code, and no obfuscation. The APK is larger than necessary and exposes internal class names.

**Fix:** Enable `isMinifyEnabled = true` and `isShrinkResources = true` for the release build type. Add appropriate ProGuard/R8 rules for Room, Hilt, and Compose.

### 5.2. Compose UI Tests Are Missing

**Problem:** The test suite has strong unit test coverage (18 test files covering ViewModels, repositories, domain logic, and mappers) but zero Compose UI tests. The `androidTest` folder contains only the default `ExampleInstrumentedTest`.

**Fix:** Add Compose UI tests for the key user flows:
- Home screen renders correctly in each state (idle, planned, active, overdue)
- "Start now" flow opens the bottom sheet and completes session creation
- Settings editor validates input and saves correctly
- Notification banners appear when permissions are missing

### 5.3. Accessibility Audit

**Problem:** Several UI elements lack semantic information for screen readers:
- The progress ring has no content description conveying the remaining time
- Icon-only buttons (settings gear) rely on `contentDescription` strings but several decorative icons use `null` without clear semantics on their parent containers
- Touch targets for some elements (profile stats, dividers) may be smaller than the recommended 48dp

**Fix:**
- Add `semantics { contentDescription = "..." }` to the progress ring with the remaining/overdue time as text
- Ensure all interactive elements meet the 48dp minimum touch target
- Test with TalkBack enabled and fix any navigation issues
- Add `Modifier.clearAndSetSemantics` where redundant announcements occur

### 5.4. Edge-to-Edge Display Support

**Problem:** The app does not appear to use edge-to-edge rendering. On Android 15+, the system enforces edge-to-edge by default, which means the app may have visual issues on newer devices if insets are not handled properly.

**Fix:**
- Call `enableEdgeToEdge()` in `MainActivity.onCreate()` before `setContent`.
- Ensure all `Scaffold` instances properly consume `WindowInsets`.
- Test on a device or emulator with gesture navigation to verify no content is obscured by system bars.

### 5.5. Predictive Back Gesture Support

**Problem:** Modern Android (13+) supports predictive back animations that preview where the back gesture will take the user. Without opting in, the app uses the legacy back behavior.

**Fix:**
- Enable `android:enableOnBackInvokedCallback="true"` in `AndroidManifest.xml`.
- Ensure Navigation Compose handles back events correctly with the predictive back API.
- Verify bottom sheets and dialogs animate correctly with the predictive back preview.

---

## 6. Summary of Quick Wins vs. Larger Efforts

### Quick wins (hours, not days)
| # | Item | Effort |
|---|------|--------|
| 1.1 | Add "Plan for later" button to idle state | ~30 min |
| 2.1 | Enable dynamic color | ~5 min |
| 5.1 | Enable R8 for release | ~1-2 hours (including ProGuard rules) |
| 5.4 | Edge-to-edge support | ~1-2 hours |
| 5.5 | Predictive back gesture | ~30 min |
| 1.4 | Replace View DatePicker with Compose DatePicker | ~1-2 hours |

### Medium effort (1-3 days each)
| # | Item | Effort |
|---|------|--------|
| 1.2 | Undo for "Lenses off" | ~1 day |
| 1.3 | Completion summary card | ~1 day |
| 2.2 | Idle state animations | ~1 day |
| 2.3 | Overdue escalating urgency | ~1 day |
| 3.1 | Contextual empty state messages | ~0.5 day |
| 3.3 | Sound preview in settings | ~0.5 day |
| 3.4 | Settings reorganization | ~1-2 days |
| 4.1 | Safety streak counter | ~1-2 days |
| 5.3 | Accessibility audit & fixes | ~1-2 days |

### Larger effort (3+ days each)
| # | Item | Effort |
|---|------|--------|
| 2.4 | Custom typography | ~1-2 days |
| 2.5 | Progress ring enhancement | ~1-2 days |
| 3.2 | Multi-step onboarding | ~3-4 days |
| 4.2 | Post-session comfort check | ~2-3 days |
| 4.3 | Weekly summary notification | ~2-3 days |
| 4.4 | Quick settings tile | ~2-3 days |
| 4.5 | Persistent notification during session | ~2-3 days |
| 5.2 | Compose UI test suite | ~3-5 days |

---

## Task List

### Phase 1: Fix What's Broken (UX bugs)
- [ ] **1.1** Wire `onPlanForLater` to a visible button in `IdleSessionContent`
- [ ] **1.4** Replace `android.app.DatePickerDialog` with Material 3 Compose `DatePickerDialog`
- [ ] **2.1** Set `dynamicColor = true` as the default in `LensesReminderTheme`
- [ ] **5.4** Add `enableEdgeToEdge()` and verify `WindowInsets` handling
- [ ] **5.5** Enable predictive back gesture in manifest

### Phase 2: Polish the Core Experience
- [ ] **1.2** Add "Undo" snackbar to "Lenses off" action
- [ ] **1.3** Build session completion summary card
- [ ] **2.2** Add breathing animation to idle checkmark and `AnimatedContent` for state transitions
- [ ] **2.3** Add pulsing animation and escalating glow to overdue state
- [ ] **3.1** Implement time-aware contextual messaging for idle state

### Phase 3: Elevate Design Quality
- [ ] **2.4** Introduce custom typography (display font for headlines/timer)
- [ ] **2.5** Enhance progress ring with gradient, dot indicator, and glow
- [ ] **3.4** Reorganize settings into focused sections
- [ ] **3.3** Add sound preview buttons to settings
- [ ] **5.3** Conduct accessibility audit and fix touch targets, content descriptions

### Phase 4: Feature Expansion
- [ ] **4.1** Implement safety streak tracking and display
- [ ] **4.2** Add post-session comfort check flow
- [ ] **4.5** Build persistent notification for active sessions
- [ ] **4.4** Implement Quick Settings tile
- [ ] **4.3** Build weekly summary notification

### Phase 5: Quality & Testing
- [ ] **5.1** Enable R8/ProGuard for release builds with appropriate keep rules
- [ ] **5.2** Write Compose UI tests for home screen states and key flows
- [ ] **3.2** Build dedicated multi-step onboarding flow
