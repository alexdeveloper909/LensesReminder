# Lenses Reminder: Product Improvements & Enhancements Specification

## Overview
Based on the current state of the Lenses Reminder app, it successfully handles the core daily lens wear workflow (starting, stopping, reminders, and persistent state). However, to transition from a single-purpose utility into a comprehensive eye-health companion, the app can be expanded across several dimensions: Feature Richness, UI/UX, and Platform Integration. 

This specification outlines genuine feedback and actionable ideas to elevate the project, providing a roadmap for future development.

---

## 1. Feature Richness & Core Capabilities

### 1.1. Lens Inventory & Supply Tracking
**Problem:** Users often forget when they are about to run out of lenses and end up without a fresh pair when they need it most.
**Solution:** 
- Add a "Supply" feature where users can input how many lenses (or boxes) they have purchased.
- Automatically decrement the inventory each time a new pair is used (e.g., starting a session for daily lenses).
- **Notification:** Send a "Low Supply" reminder when the inventory drops below a customizable threshold (e.g., 5 pairs remaining).

### 1.2. Extended Wear Schedules (Bi-weekly & Monthly Lenses)
**Problem:** The current app focuses on daily wear duration (e.g., 12 hours a day), but many users wear bi-weekly or monthly lenses.
**Solution:**
- Introduce a "Lens Type" in the user profile (Daily, Bi-weekly, Monthly, Custom).
- Track both the **daily wear time** AND the **lifespan of the physical lens pair**.
- **Notification:** Alert the user on the day they need to throw away their old lenses and open a fresh pair.

### 1.3. Distinct Left/Right Eye Tracking
**Problem:** Users often have different prescriptions for each eye, or they might lose/tear a lens in one eye, misaligning their replacement schedule.
**Solution:**
- Allow users to split their profile into Left and Right eyes.
- Track inventory and lifespan independently if the user enables this setting.

### 1.4. History, Statistics, and Insights
**Problem:** Users cannot see their past behavior or verify if they are consistently over-wearing their lenses.
**Solution:**
- Create a "History" or "Insights" tab.
- Display a calendar view showing days lenses were worn.
- Provide statistics: "Average daily wear time", "Days overworn this month", etc.
- **Health Log:** Allow users to add a note or select symptoms (e.g., "Dry eyes", "Redness") for a specific day to share with their optometrist.

---

## 2. UI/UX Enhancements

### 2.1. Home Screen Widget
**Problem:** Users have to open the app to check how much wear time is left or to stop the session.
**Solution:**
- Create an Android App Widget using Jetpack Glance (Compose for Widgets).
- The widget should display the current session status (Active/Overdue), remaining time, and provide a quick action button to "End Session".

### 2.2. App Shortcuts & Quick Settings Tile
**Problem:** Friction in starting/stopping a session.
**Solution:**
- **App Shortcuts:** Long-pressing the app icon should reveal options like "Start Session Now" or "End Session".
- **Quick Settings Tile:** Add a tile to the Android notification shade drop-down to toggle the lens session on/off instantly.

### 2.3. Material 3 & Animation Polish
**Solution:**
- Ensure full support for **Dynamic Color (Monet)** so the app blends seamlessly with the user's OS theme.
- Add **Shared Element Transitions** between the Home screen and the Planning screen to make the app feel more fluid.
- Add a visual, animating progress ring on the Home screen for the active session, visually filling up as the safe-wear window closes.

---

## 3. Platform Integrations & Technical Debt

### 3.1. Wear OS Companion
**Solution:**
- Develop a lightweight Wear OS companion app or ensure notifications are highly optimized for wearables.
- Users should be able to tap "Lenses Off" or "Snooze" directly from their watch without pulling out their phone.

### 3.2. Cloud Backup / Export
**Solution:**
- Eye health data is valuable. Implement a simple JSON/CSV export feature.
- Optionally integrate with Google Drive API or Android's Auto Backup to ensure users don't lose their history when changing devices.

---

## Task List / Implementation Roadmap

### Phase 1: Quick Wins & UX Polish
- [ ] **Task 1.1:** Implement Material 3 Dynamic Color across the app.
- [ ] **Task 1.2:** Add App Shortcuts for "Start Session" and "End Session".
- [ ] **Task 1.3:** Enhance Home screen with a circular progress indicator for active wear time.

### Phase 2: History & Insights
- [ ] **Task 2.1:** Create Room DAO queries to fetch historical `WearSession` data by month/week.
- [ ] **Task 2.2:** Build the `HistoryScreen` composable with a calendar layout.
- [ ] **Task 2.3:** Calculate and display basic stats (Average wear time, total sessions).
- [ ] **Task 2.4:** Allow users to attach a "Note" or "Symptom" to a completed session.

### Phase 3: Lens Types & Bi-Weekly/Monthly Support
- [ ] **Task 3.1:** Update `LensProfile` entity to include `lensType` (enum) and `lifespanDays`.
- [ ] **Task 3.2:** Update Onboarding to ask for Lens Type.
- [ ] **Task 3.3:** Add a new scheduled reminder for "Lens Replacement Day" in the domain layer.
- [ ] **Task 3.4:** Display "Days until replacement" on the Home screen for non-daily lenses.

### Phase 4: Inventory Tracking
- [ ] **Task 4.1:** Add `inventoryCount` (Left/Right/Both) to `LensProfile`.
- [ ] **Task 4.2:** Create UI in Settings/Profile to "Add boxes/lenses".
- [ ] **Task 4.3:** Hook into `SessionLifecycleManager` to decrement inventory when a new pair is opened.
- [ ] **Task 4.4:** Trigger local notification when inventory is low.

### Phase 5: Widgets & Advanced Integrations
- [ ] **Task 5.1:** Add Jetpack Glance dependency.
- [ ] **Task 5.2:** Build a resizable Home Screen Widget showing current state and a Stop button.
- [ ] **Task 5.3:** Create a Quick Settings Tile service.
- [ ] **Task 5.4:** Implement data export (JSON/CSV) to local storage.
