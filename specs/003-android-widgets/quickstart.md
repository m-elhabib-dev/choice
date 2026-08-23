# Quickstart: Validating Android Home-Screen Widgets

These are manual, end-to-end validation scenarios that prove the feature works, mapped to the
spec's user stories. Widget-surface behavior (`RemoteViews` rendered by Glance) cannot be driven
by unit tests, so this guide is the primary acceptance check; unit tests cover the plain-Kotlin
logic underneath it (see "Unit tests" at the end).

## Prerequisites

- Android device or emulator, API 26+ (project `minSdk`), with a home-screen launcher that
  supports widgets (stock AOSP launcher and Pixel Launcher both do).
- The `app` module builds and installs: `./gradlew :app:installDebug`.
- At least one saved coin with 2+ choices exists in the app for the "happy path" scenarios (create
  one in-app first if starting from a clean install).

## Setup

```bash
./gradlew :app:installDebug
adb shell am start -n com.choice.app/.MainActivity   # confirm app itself still launches normally
```

Add a widget: long-press the home screen → **Widgets** → find **Choice** → drag either **Single
Coin** or **Quick Coins** onto the home screen. This always launches that widget's configuration
Activity first (per `contracts/widget-provider-contract.md`).

## Scenario 1 — Single Coin Widget core flow (User Story 1, P1)

1. Create (or confirm) a coin with ≥ 2 choices in-app.
2. Add a Single Coin Widget; in configuration, select that coin; confirm.
   - **Expect**: widget appears on the home screen showing the coin's name and a "ready" state
     (no result yet).
3. Tap the widget's flip action.
   - **Expect**: a result appears immediately on the widget, no app launch occurs.
4. Look at the widget again.
   - **Expect**: the result reads as settled (prominent); any further flip control is visually
     smaller/secondary, not the dominant element.
5. Reboot the device (or force-stop the launcher: `adb shell am force-stop
   com.android.launcher3` on emulators using that launcher, then reopen the launcher).
   - **Expect**: the widget still shows the same coin and the same most-recent result.

## Scenario 2 — Quick Coins Widget (User Story 2, P2)

1. Favorite 2+ coins in-app (each with ≥ 2 choices).
2. Add a Quick Coins Widget without customizing selection.
   - **Expect**: widget shows the favorited coins by default, each with its own flip action.
3. Reconfigure it (via its config entry point) to a specific different subset of coins.
   - **Expect**: widget now shows exactly that subset.
4. Tap one coin's flip action.
   - **Expect**: only that coin's row updates; the others are unchanged.
5. Resize the widget smaller, then larger, via the launcher's resize handles.
   - **Expect**: fewer/more coins shown as space allows; no clipped or overlapping content at any
     size.
6. Un-favorite all coins, then add a fresh Quick Coins Widget with no custom selection.
   - **Expect**: widget clearly states no coins are selected and offers a way to choose some.

## Scenario 3 — Decision rule parity (User Story 3, P2)

1. On one coin, enable weighted selection and set one choice's weight much higher than the others
   (in-app, via existing coin settings).
2. Flip that coin's widget ~20 times, tapping the flip action each time; separately flip the same
   coin in-app ~20 times.
   - **Expect**: the widget's observed distribution of results looks like the app's — the heavily
     weighted choice dominates in both.
3. On a different coin (or the same one, weighting removed), enable "avoid last result."
4. Flip its widget twice in a row.
   - **Expect**: the second result never repeats the first (when the coin has ≥ 2 choices).
5. Change that coin's weighted/avoid-last-result setting in-app, then flip its widget again.
   - **Expect**: the widget's next decision follows the newly changed setting.

## Scenario 4 — Staying in sync with app-side changes (User Story 4, P3)

1. Configure a widget for a coin. Rename that coin in-app.
   - **Expect**: widget shows the new name without removing/re-adding it.
2. Edit that coin's choices in-app (add/remove/edit text).
   - **Expect**: the widget's next flip draws from the updated choice list.
3. Delete that coin in-app.
   - **Expect**: the widget shows a clear "unavailable" state (not a crash, not stale data) with a
     reconfigure/open-app action.
4. Remove a coin's choices down to zero (edge case) on a coin represented by a widget.
   - **Expect**: the widget shows a "can't decide yet" explanatory state, not a broken flip.
5. Change a coin's favorite status while it is shown by a favorites-mode Quick Coins Widget.
   - **Expect**: the widget's displayed set updates to match.

## Scenario 5 — Open in app from a widget (User Story 5, P3)

1. Tap a Single Coin Widget's coin name/header.
   - **Expect**: the app opens directly to that coin's screen, no extra navigation needed.
2. Tap one coin's name inside a Quick Coins Widget.
   - **Expect**: the app opens directly to that specific coin.
3. Tap the "open app" action on an Unavailable-state widget (deleted coin).
   - **Expect**: the app opens to a reasonable fallback (the main coin list), not a failure.

## Scenario 6 — Multiple independent widgets (User Story 6, P4)

1. Add a second Single Coin Widget configured for a different coin than the first.
2. Flip one of them.
   - **Expect**: only that widget's shown result changes; the other is untouched.
3. Delete the coin represented by only one of the two widgets.
   - **Expect**: only that widget enters the Unavailable state; the other keeps working normally.

## Offline check (SC-008)

Enable Airplane Mode (`adb shell cmd connectivity airplane-mode enable` or device Quick Settings)
and repeat Scenario 1 end-to-end (add, configure, flip, view result). Everything must work
identically with connectivity off.

## Accessibility check (SC-009)

Enable TalkBack (Settings → Accessibility → TalkBack). For both widget types, swipe through the
widget's elements and confirm TalkBack announces: the represented coin's name, the current
result (or ready/unavailable/explanatory state), and what each control (flip, open-app,
reconfigure) does — per `FR-020`.

## Rapid-tap check (edge case)

Rapidly tap a widget's flip action several times in quick succession, then open the app's History
screen for that coin.
- **Expect**: exactly one new history entry per settled tap sequence — never a duplicate entry
  from a single intended tap, and the widget never shows a result that isn't in history.

## Unit tests

Business logic that underlies the above (independent of Glance rendering) is covered by:

```bash
./gradlew :app:testDebugUnitTest
```

Relevant new test classes (see `plan.md` Project Structure): `WidgetConfigStoreTest` (config
read/write/decode-failure → Unavailable mapping), `QuickCoinsDefaultSelectionTest` (favorites vs.
explicit resolution, including stale/deleted IDs being dropped), `WidgetFlipGuardTest` (concurrent
flip calls on the same key produce exactly one selection+record). Existing tests for
`selectChoice`/`flipCoin`/`applyAvoidLastResult`/`selectWeighted` are unchanged and continue to be
the source of truth for decision-rule correctness (research.md §4).
