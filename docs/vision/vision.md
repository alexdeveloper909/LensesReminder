## Product vision

Lenses Reminder is a safety-focused mobile app for people who wear contact lenses and want a simple way to track active wear time and avoid wearing lenses too long.

The app is not just a timer. Its main value is helping users build a safe daily habit:

- start lens wear intentionally
- know how long lenses have been worn
- get reminded before safe wear time is exceeded
- keep reminding the user to remove lenses until they confirm they are out

## Problem

People who wear daily contact lenses can easily lose track of time, especially on busy days. This creates a real health and comfort risk:

- lenses may be worn longer than recommended
- users may forget to remove lenses before sleeping
- users may not remember exactly when they put lenses on

Most generic reminder apps are not designed for this workflow. They do not understand lens wear duration, active wear sessions, or the need for repeated "take lenses off" alerts until the user confirms completion.

## Product goal

The goal of the application is to help users safely manage daily contact lens wear by combining session tracking with action-oriented reminders.

For the first version, the app should help the user answer three questions:

1. Did I put my lenses on today?
2. How long have I been wearing them?
3. Have I definitely taken them off?

## Target user

The primary user is a person who wears contact lenses regularly and wants a lightweight app that reduces the chance of over-wearing lenses or falling asleep with them on.

This is especially useful for users who:

- have irregular schedules
- often forget when they put lenses on
- want a stronger reminder flow than a normal alarm

## Core product principles

- Safety first: the app should prioritize preventing overuse and forgotten removal.
- Minimal friction: starting and ending a lens session should take one tap whenever possible.
- Clear status: the user should always be able to see whether lenses are currently in or out.
- Persistent reminders: removal reminders should continue until the user explicitly confirms lenses are off.

## MVP scope

The first version of the app should include the following capabilities:

### 1. Lens setup

The user can define:

- lens type
- maximum recommended wear duration for that lens
- whether reminders are enabled

For now, the initial focus should stay on daily lenses, even if the data model is designed to support more lens types later.

### 2. Start a wear session

The user can create a wear session by choosing when lens wear starts.

Possible start options:

- start now
- schedule a planned time slot in advance

The app should treat a wear session as an active state with:

- start time
- expected end time
- current status: planned, active, completed, overdue

### 3. Put-on reminder

If the user plans a future wear session and reminders are enabled, the app sends a notification at the planned start time.

The notification should support a quick action such as:

- `Lenses on`
- `Snooze`

If the user taps `Lenses on`, the active session starts immediately and tracking begins from that moment.

### 4. Active wear tracking

While lenses are in, the app should show:

- elapsed wear time
- remaining recommended time
- expected removal time

### 5. Take-off reminder

When the wear window ends, the app sends a high-priority notification reminding the user to remove the lenses.

This is the most important moment in the product. The reminder should include an explicit action:

- `Lenses off`

### 6. Repeating removal alerts

If the user does not confirm that lenses are off, the app should continue sending reminders at repeated intervals.

This behavior is a core feature, not a nice-to-have.

The rule is:

- once the session reaches its end time, the app enters an overdue state
- while the session is overdue and not confirmed complete, the app keeps reminding the user every 15 minutes
- reminders stop only when the user confirms `Lenses off` or the final alert has been reached

The removal reminder should use a stronger notification style than normal reminders, including a custom alert sound if the platform allows it.

### 7. Final alert before sleep

The user can configure a personal final alert time. This should usually match the time when the user normally falls asleep.

The purpose of the final alert is to act as the last strong warning before sleep if the user still has not confirmed `Lenses off`.

The rule is:

- the final alert is triggered at the user-defined time if there is still an active or overdue session
- the final alert should use a distinct, stronger sound than normal reminders
- after the final alert, the app should not send any more reminders for that session

This makes the final alert the strongest and last notification in the daily reminder chain.

## Core user stories

### Story 1: Set up my lens preferences

As a lens wearer, I want to configure the type of lenses I use and the safe wear duration, so that reminders match my real routine.

### Story 2: Plan lens wear for today

As a lens wearer, I want to schedule when I plan to wear my lenses, so that the app can remind me to put them on at the right time.

### Story 3: Start lens wear quickly

As a lens wearer, I want to confirm with one tap that I have put my lenses on, so that the app can accurately start tracking wear time.

### Story 4: Know my current wear status

As a lens wearer, I want to see whether my lenses are currently in and how long I have been wearing them, so that I can make safe decisions during the day.

### Story 5: Be reminded to remove lenses on time

As a lens wearer, I want the app to alert me when my recommended wear time is over, so that I do not accidentally over-wear my lenses.

### Story 6: Prevent forgetting to remove lenses

As a lens wearer, I want the app to keep reminding me to take my lenses off until I confirm it, so that I do not fall asleep with lenses still in.

### Story 7: Get a final pre-sleep warning

As a lens wearer, I want to define a final alert time with a stronger sound, so that I get one last warning before I fall asleep with lenses still in.

### Story 8: Correct a forgotten session start

As a lens wearer, I want to manually log a session start time after the fact, so that tracking can still reflect when I actually began wearing my lenses.

## Main user flow

1. User selects lens settings and preferred wear duration.
2. User starts a session now or plans one for later.
3. App reminds the user to put lenses on at the planned time.
4. User confirms `Lenses on`.
5. App tracks the active wear session.
6. App sends a removal reminder when the wear duration ends.
7. If the user does not confirm `Lenses off`, the app repeats the reminder every 15 minutes.
8. If the session is still active by the user's configured final alert time, the app sends a stronger final alert.
9. After the final alert, no further reminders are sent for that session.
10. User confirms `Lenses off`.
11. Session is marked complete and reminders stop.

## Success criteria for MVP

The MVP is successful if a user can:

- configure a safe daily wear duration
- configure a final alert time before sleep
- start and stop a lens session with minimal effort
- manually correct a missed session start
- receive reminders at the right moments
- avoid missing the lens removal step because of repeated alerts and a final pre-sleep alert

## Future expansion ideas

These are useful, but should not distract from the first version:

- wear history and analytics
- support for more lens replacement schedules
- health notes such as discomfort or dry eyes
- integration with calendar or sleep schedule
- smart reminder intervals based on user behavior

## Product decisions to clarify later

The following product decisions are currently defined for v1:

- The app supports daily lenses only.
- Overdue removal reminders repeat every 15 minutes.
- The user defines a final alert time, usually based on when they go to sleep.
- The final alert uses a stronger, distinct sound.
- After the final alert, no more reminders are sent for that session.
- Users can manually backfill a forgotten session.
