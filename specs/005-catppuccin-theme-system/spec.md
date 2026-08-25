# Feature Specification: Catppuccin Theme System

**Feature Branch**: `005-catppuccin-theme-system`

**Created**: 2026-08-25

**Status**: Draft

**Input**: User description: "Choice Catppuccin Theme System — Extend the existing Choice Android application with support for all four official Catppuccin color flavors (Latte, Frappé, Macchiato, Mocha), selectable in Settings, persisted locally, applied at runtime across the app and widgets, with Mocha remaining the default for existing and new users, without changing typography, layout, decision behavior, or existing local data."

## Clarifications

### Session 2026-08-25

- Q: Should the Appearance screen show an actual color swatch for each theme, or is a text-only descriptor enough? → A: Small color swatch/chips per theme, shown alongside its name and light/dark label.
- Q: At cold app start (or process recreation), is a brief flash of the Mocha default before switching to a previously-selected theme acceptable, or must the correct theme render on the very first frame with no flash? → A: A brief flash is acceptable, as long as the app settles on the correct theme almost immediately.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Choose an appearance theme (Priority: P1)

A user opens Choice's settings and finds an "Appearance" section listing the four Catppuccin flavors (Latte, Frappé, Macchiato, Mocha), each with a short name and a one-line description of its character (e.g., "Light Catppuccin" / "Dark Catppuccin"). The user picks a flavor and the entire app updates its colors immediately, without restarting the app.

**Why this priority**: This is the core value of the feature — without theme selection and immediate visual feedback, nothing else in this feature matters. It is also the smallest slice that is fully demonstrable on its own.

**Independent Test**: Can be fully tested by opening Settings, selecting each of the four themes in turn, and confirming the visible color scheme of the current screen changes to match the selected flavor with no app restart required.

**Acceptance Scenarios**:

1. **Given** the app is freshly installed (or upgraded from a version without this feature) and no theme has ever been chosen, **When** the user opens the app, **Then** the app displays using the Mocha theme, matching its current visual appearance.
2. **Given** the user is on the Settings screen, **When** they select "Latte", **Then** the app immediately switches to a light background with dark, readable text and Latte's accent colors, and "Latte" is shown as the selected option.
3. **Given** the user is on the Settings screen with any theme selected, **When** they select a different theme (Frappé, Macchiato, or Mocha), **Then** the app immediately re-renders every visible element (backgrounds, surfaces, text, buttons, borders) in the newly selected theme's colors.
4. **Given** the user has selected a theme, **When** they navigate to any other screen in the app (coin screens, history, statistics, templates, dialogs, bottom sheets, forms), **Then** that screen renders using the same selected theme.

---

### User Story 2 - Retain my chosen theme across app sessions (Priority: P2)

A user who has selected a non-default theme closes the app, rotates their device, or has the app's process killed by the operating system and later restarted. When they return to Choice, their previously selected theme is still active — they never have to re-select it.

**Why this priority**: Theme selection without durable persistence would be a frustrating, unusable feature; persistence is required for the feature to deliver ongoing value, but it depends on User Story 1 (selection) already existing.

**Independent Test**: Can be fully tested by selecting a non-default theme, force-stopping or restarting the app (and separately, rotating the device), and confirming the same theme is active on return in both cases.

**Acceptance Scenarios**:

1. **Given** the user has selected "Macchiato", **When** they fully close and reopen the app, **Then** the app launches directly in the Macchiato theme.
2. **Given** the user has selected "Frappé", **When** they rotate their device, **Then** the app remains in the Frappé theme with no visible flash or reset to another theme.
3. **Given** the user has selected a theme and the app's process is later recreated by the operating system (e.g., after being backgrounded for a long time), **When** the app resumes, **Then** the previously selected theme is restored automatically.
4. **Given** the user has selected a theme, **When** they change other settings (such as language) or use other app features (creating coins, flipping, editing history), **Then** the previously selected theme remains unchanged and none of that other data is affected by the theme setting.

---

### User Story 3 - See widgets reflect my chosen theme (Priority: P3)

A user who has placed a Choice widget (Single Coin or Quick Coins) on their home screen changes their in-app theme. The widget's colors update to reflect the newly selected Catppuccin flavor, remaining clearly readable, so the widget and the app feel like one consistent product.

**Why this priority**: Widgets are a secondary surface that depends on the theme selection mechanism from User Story 1 already existing; they add polish and consistency but the app is fully usable without this working perfectly on day one.

**Independent Test**: Can be fully tested by placing a widget on the home screen, changing the in-app theme, returning to the home screen (or waiting for the next widget refresh), and confirming the widget's colors match the newly selected flavor and remain readable.

**Acceptance Scenarios**:

1. **Given** a Single Coin or Quick Coins widget is on the home screen showing the current theme's colors, **When** the user changes the in-app theme, **Then** the widget updates to the new theme's colors within the normal widget refresh cycle, without requiring the widget to be removed and re-added.
2. **Given** any of the four themes is active, **When** the widget displays a decision result or coin label, **Then** the text remains clearly readable against its background in that theme.
3. **Given** a widget cannot exactly reproduce a particular flavor's color or visual treatment due to platform limitations, **When** the widget renders, **Then** it uses the closest available representation that still visually identifies as that flavor and preserves text readability.

---

### Edge Cases

- What happens when a user selects a theme while a dialog or bottom sheet is open? The open dialog/sheet must update to the new theme's colors along with the rest of the app, without closing or breaking.
- What happens on the very first launch of a new install, before the user ever visits Settings? The app must display Mocha by default, matching the existing product's current appearance.
- What happens if the stored theme preference is missing, unreadable, or contains an unrecognized value (e.g., due to a corrupted preference store)? The app must fall back to Mocha rather than failing to launch or showing an undefined/blank appearance.
- What happens during a cold app start or process recreation, before the persisted theme preference has been read? A brief flash of the Mocha default theme before switching to the stored theme is acceptable, as long as the app settles on the correct theme almost immediately without any user action; device rotation (a configuration change, not a cold start) must not exhibit this flash, since the theme is already held in memory.
- What happens when the app's language is set to Arabic (RTL)? The theme selection list, radio indicators, and selected-state marker must mirror correctly for RTL, and all theme names/descriptions must display in Arabic without truncation or garbled text.
- What happens when a user has multiple widgets of different types on their home screen? All widgets must reflect the same selected theme consistently after a theme change.
- What happens if a user changes the theme, then immediately changes it again to a different flavor before the first change has visibly settled? The app must end up showing only the most recently selected theme, with no intermediate or mixed-color state left behind.
- What happens to a widget while the host app is not running (e.g., after a device reboot) when the user later changes the theme from within the app? The widget must reflect the new theme the next time it is able to refresh, without requiring the user to reconfigure the widget.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST let users select one of exactly four color themes — Latte, Frappé, Macchiato, Mocha — from an "Appearance" section in Settings.
- **FR-002**: System MUST show each theme option with its name, a short descriptive label (e.g., light/dark character), and a small color swatch/chip sample of that flavor's palette, so users can visually distinguish the four options without switching between them repeatedly.
- **FR-003**: System MUST visually indicate which theme is currently selected within the theme selection interface.
- **FR-004**: System MUST persist the selected theme locally so that it survives an app restart, an activity recreation, a device rotation, and a process recreation by the operating system.
- **FR-005**: System MUST default to the Mocha theme both for users upgrading from a version of the app without this feature and for new installations, so no user experiences an unrequested appearance change.
- **FR-006**: System MUST fall back to the Mocha theme if no valid stored theme preference can be found (e.g., missing or corrupted preference data).
- **FR-007**: System MUST apply a newly selected theme to the running app immediately, without requiring the user to restart the app or navigate away and back.
- **FR-008**: System MUST apply the currently selected theme consistently across all application surfaces, including primary screens, dialogs, bottom sheets, forms, coin screens, history, statistics, settings, and template screens.
- **FR-009**: Each of the four themes MUST use the official Catppuccin color values for that flavor, mapped to a complete, consistent set of color roles (at minimum: background, surface, elevated surface, primary text, secondary text, disabled text, primary accent, secondary accent, border, divider, success, warning, error, informational state, and interactive state) so the four themes are functionally equivalent to one another.
- **FR-010**: System MUST maintain sufficient color contrast for primary text, secondary text, buttons, inputs, interactive states, error messages, success messages, and disabled controls in every one of the four themes.
- **FR-011**: Theme switching MUST NOT change the app's typography — font family, sizes, weights, line heights, and text hierarchy MUST remain identical across all four themes.
- **FR-012**: Changing the selected theme MUST NOT modify, delete, or otherwise affect coins, choices, favorites, decision history, statistics, templates, widget configuration, or any user setting unrelated to appearance (such as language).
- **FR-013**: System MUST apply the selected theme to home-screen widgets (Single Coin and Quick Coins), so that widget colors reflect the app's currently selected theme.
- **FR-014**: When the selected theme changes, affected widgets MUST update to the new theme's colors within the normal widget refresh behavior, without requiring the user to remove and re-add the widget or reconfigure it.
- **FR-015**: Widget content, including decision results, MUST remain clearly readable against its background in every one of the four themes, using the closest valid representation of the flavor's colors when platform limitations prevent an exact match.
- **FR-016**: Theme names and descriptions MUST be presented through the app's existing localization system (not hardcoded text) and MUST display correctly, including proper right-to-left mirroring of the selection interface, when the app is running in Arabic.
- **FR-017**: The theme selection mechanism MUST be structured so that adding a future theme does not require changes to individual screens, dialogs, or components that already consume the current four themes.

### Key Entities

- **Theme Preference**: The user's currently chosen appearance setting for the app. Holds exactly one of the four supported flavor values at any time, defaults to Mocha, is stored independently of all other user data (coins, choices, history, statistics, templates, widget configuration, language), and is read whenever the app or a widget needs to render.
- **Color Theme (Flavor)**: One of the four selectable appearance options — Latte, Frappé, Macchiato, or Mocha. Each flavor provides a complete, coordinated set of colors covering backgrounds, surfaces, text, borders, accents, and status indicators (success/warning/error/informational), consistent in structure with the other three flavors so that any flavor can be substituted for another without other parts of the app needing to change.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can switch from any theme to any other theme in under 5 seconds, entirely from within the Settings screen.
- **SC-002**: 100% of the app's visible screens, dialogs, bottom sheets, and forms reflect a newly selected theme immediately, with no manual app restart.
- **SC-003**: The selected theme is retained with full consistency across app restart, device rotation, and process recreation in all tested cases.
- **SC-004**: All four themes meet standard accessible-contrast expectations for primary text, secondary text, buttons, inputs, error states, and success states (at least a 4.5:1 contrast ratio for normal-sized text and 3:1 for large text).
- **SC-005**: Every existing app feature (creating and managing coins, choices, and templates; flipping/deciding; viewing history and statistics; using widgets) is fully usable, with no functional differences, in each of the four themes.
- **SC-006**: No coins, choices, favorites, decision history, statistics, templates, widget configuration, or unrelated user settings are altered or lost as a result of selecting or switching themes, verified across all twelve possible theme-to-theme transitions.
- **SC-007**: Home-screen widgets visually reflect the selected theme after the next normal widget refresh following an in-app theme change, with text and decision results remaining readable in all four themes.
- **SC-008**: Theme names and descriptions display correctly, legibly, and in properly mirrored right-to-left layout when the app language is Arabic, with no truncated or garbled text.
- **SC-009**: A future fifth theme can be added to the selection list without requiring changes to any individual screen, dialog, or widget that already supports the existing four themes.

## Assumptions

- The app already has a working mechanism for persisting a simple, independent user preference (the existing language preference) outside of the main data store used for coins/choices/history; the theme preference is assumed to use an equivalent, independent local-persistence approach so that it does not interact with or risk that other data.
- "Immediately" for in-app theme changes means the change is visible within the same screen redraw/recomposition after the user taps a theme option; "immediately" for widgets is understood as "at the next normal widget refresh," since Android widget updates are inherently asynchronous and not instantly redrawn like in-app UI.
- Accessibility contrast targets follow standard accessible-text-contrast guidance (4.5:1 for normal text, 3:1 for large text) as no specific numeric target was provided by the requester.
- An optional system-controlled (device light/dark) appearance mode is not required for this feature; the four explicit Catppuccin flavors, defaulting to Mocha, satisfy all stated goals without introducing a fifth, potentially ambiguous "system" mode.
- The existing English/Arabic localization infrastructure will be extended with new string resources for theme names and descriptions; no new localization infrastructure is required.
- The existing Mocha color values currently used by the app are treated as the canonical Catppuccin Mocha flavor for this feature and are not altered in appearance, only reorganized so they are one of four selectable options instead of the only option.
