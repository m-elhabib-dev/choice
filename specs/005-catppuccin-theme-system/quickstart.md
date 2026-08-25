# Quickstart: Validating the Catppuccin Theme System

**Feature**: `005-catppuccin-theme-system`

This is a validation guide, not an implementation guide — it assumes the design in
[data-model.md](./data-model.md) and [contracts/](./contracts/) is implemented, and walks through
proving each user story and success criterion actually holds. Run automated checks first; the
manual steps are for what automation can't observe (visual mirroring, on-device flash timing).

## Prerequisites

- Android Studio / a configured JDK 17 toolchain (existing project setup — no new tooling).
- A device or emulator at API 26 (`minSdk`) and one at API 33+ (locale/RTL behavior differs by
  tier per the `004` locale contract, though this feature's own theme-switch path does not branch
  by API level — verify both anyway since Settings now hosts two independent preferences).
- Build variant: debug is sufficient; no signing or release config needed.

```bash
./gradlew assembleDebug
```

---

## 1. Automated checks (run first)

```bash
# Unit tests — pure JVM, no device needed
./gradlew testDebugUnitTest --tests "com.choice.app.ui.theme.*"

# Full unit suite (regression guard — theme change must not break existing pure-logic tests)
./gradlew testDebugUnitTest

# Instrumented tests — needs a connected device/emulator
./gradlew connectedDebugAndroidTest --tests "com.choice.app.ui.settings.*"
./gradlew connectedDebugAndroidTest --tests "com.choice.app.locale.RtlLayoutTest"

# Translation-completeness lint gate — fails the build if any theme string lacks an Arabic counterpart
./gradlew lintDebug
```

**Expected new/updated test files** (see [research.md §7](./research.md#7-contrast-validation-sc-004)
and [contracts/theme-contract.md](./contracts/theme-contract.md)):

| Test | Proves |
|---|---|
| `ui/theme/ThemeFlavorTest.kt` (`resolveThemeFlavor` table, [theme-contract.md §2](./contracts/theme-contract.md#2-theme-resolution)) | FR-001, FR-005, FR-006 |
| `ui/theme/ThemeContrastTest.kt` (WCAG ratio per role × 4 flavors, [theme-contract.md §6](./contracts/theme-contract.md#6-color-role--contrast-coverage-fr-009--fr-010--sc-004)) | FR-010, SC-004 |
| `widget/WidgetGlanceThemeTest.kt` (`colorsFor` mapping table) | FR-013, FR-015 |
| `ui/settings/SettingsScreenTest.kt` (extended: theme selection persists; reselect is no-op) | FR-003, FR-004, SC-001 |
| `locale/RtlLayoutTest.kt` (extended: Appearance section added to the screen list) | SC-008 |

If any of these files don't exist yet, that test coverage is missing — implementation is not done.

---

## 2. Manual validation — User Story 1 (choose a theme, P1)

1. Install a fresh debug build (or clear app data) on a device/emulator.
2. Launch the app. **Expect**: Mocha colors, matching the app's current look — no visible
   difference from before this feature (Acceptance Scenario 1).
3. Open Settings → Appearance. **Expect**: four rows (Latte, Frappé, Macchiato, Mocha), each with a
   name, one-line description, small color swatch, and Mocha's row showing as selected
   (`RadioButton` checked).
4. Tap "Latte". **Expect**: within the same frame, the Settings screen itself switches to a light
   background with dark text and Latte's accents; the "Latte" row now shows selected. No spinner,
   no navigation, no restart (Acceptance Scenario 2, SC-001 — should feel instant, well under 5s).
5. Navigate to Main, a coin's flip screen, History, Statistics, Templates, and open a dialog/bottom
   sheet (e.g. the delete-coin confirmation). **Expect**: every one of these renders in Latte's
   colors (Acceptance Scenario 4, SC-002, SC-005 — confirm no functional difference in any screen).
6. Repeat step 4 for Frappé and Macchiato. **Expect**: full-app re-render each time, no leftover
   colors from the previous flavor anywhere (Acceptance Scenario 3).
7. **Edge case** — with a dialog open (e.g. delete-coin confirmation), select a different theme
   from... (not directly possible with a modal dialog open; instead) open the dialog, then use
   Settings from a second navigation path if available, or: select a theme, then before the
   screen settles, tap a second theme rapidly. **Expect**: the app ends on exactly the
   last-tapped theme, with no visible flicker between two different color sets (Edge Case: rapid
   re-selection).

---

## 3. Manual validation — User Story 2 (persistence, P2)

1. Select "Macchiato". Fully close the app (remove from recents) and reopen. **Expect**: app
   launches directly in Macchiato — no flash of any other theme beyond the brief-flash allowance
   from the spec's clarification, and it must settle on Macchiato almost immediately (Acceptance
   Scenario 1).
2. Select "Frappé". Rotate the device. **Expect**: no flash at all, no reset — rotation is a
   configuration change, not a cold start, and the in-memory `AppThemeState` survives it
   (Acceptance Scenario 2).
3. Select a non-Mocha theme. Force-stop the app via Android Settings → Apps → Choice → Force stop.
   Relaunch from the home screen. **Expect**: the same theme is restored (Acceptance Scenario 3 —
   simulates OS process recreation).
4. With a non-default theme active, create a coin, flip it, check History/Statistics, and switch
   the app language (Settings → Language). **Expect**: the theme is unaffected by any of this, and
   none of that data is affected by the theme (Acceptance Scenario 4, FR-012, SC-006).
5. **Corrupted-preference simulation**: `adb shell run-as com.choice.app rm -rf
   /data/data/com.choice.app/shared_prefs/choice_app_prefs.xml` (or edit the file to set an
   unrecognized `theme_flavor` value), then relaunch. **Expect**: the app falls back to Mocha, does
   not crash, does not show a blank/undefined appearance (FR-006, Edge Case).

---

## 4. Manual validation — User Story 3 (widgets, P3)

1. Place both a Single Coin widget and a Quick Coins widget on the home screen.
2. In-app, select a non-Mocha theme (e.g. Latte). Return to the home screen and wait for (or
   trigger) the next normal widget refresh. **Expect**: both widgets' colors update to Latte,
   remaining clearly readable (Acceptance Scenario 1, SC-007).
3. Repeat for each of the four themes, checking text-on-background readability each time
   (Acceptance Scenario 2).
4. **Edge case — theme changed while host app not running**: force-stop the app, change nothing
   (can't change theme without the app running — this validates the receiving side instead): place
   a widget, then from a fresh app launch change the theme and immediately background the app
   without further interaction. **Expect**: the widget still updates at its next normal refresh
   without needing to be removed/re-added or reconfigured.
5. With multiple widgets of both types placed, change the theme once. **Expect**: all widget
   instances converge on the same new theme (Edge Case: multiple widgets stay consistent).

---

## 5. Manual validation — RTL / Arabic (FR-016, SC-008)

1. Switch the app language to Arabic (Settings → Language).
2. Open Settings → Appearance (label should now read in Arabic, mirrored layout — the section
   should have moved to the RTL-appropriate side along with the rest of the Settings screen).
3. **Expect**: all four theme names and descriptions render in Arabic, fully, with no truncation
   and no garbled/mojibake text; the `RadioButton` and selected-state marker appear on the
   RTL-correct side, matching the same mirroring already exhibited by the Language section above
   it.
4. Select a theme while in Arabic. **Expect**: identical immediate-apply behavior as in English —
   language and theme are independent axes.

---

## 6. Sign-off checklist

- [ ] All automated checks in §1 pass, including `lintDebug`'s `MissingTranslation`/
      `ExtraTranslation` gate for the eight new `theme_*_name`/`theme_*_description` keys.
- [ ] §2–§5 manual walkthroughs completed on one API 26–32 device/emulator and one API 33+
      device/emulator.
- [ ] `ThemeContrastTest` passes for all four flavors — no manual contrast judgment substitutes for
      this.
- [ ] No Room schema file under `app/schemas/` changed as part of this feature (confirms FR-012 was
      not violated at the persistence layer).
