# Feature Specification: Choice Localization and Multilingual Support

**Feature Branch**: `[004-arabic-localization-support]`

**Created**: 2026-08-24

**Status**: Draft

**Input**: User description: "Add comprehensive multilingual support to the existing Choice Android application. The application must support multiple UI languages while preserving the existing functionality, architecture, design system, widgets, and local-first behavior. Initial supported languages are English and Arabic, with an architecture that makes adding future languages straightforward without changing application logic. All user-facing text must be localizable and use Android's standard localization mechanisms. Arabic must use correct RTL layout via native layout-direction mechanisms, not manual mirroring. User-created content (coin names, choices) must never be auto-translated; templates may provide localized content. Search, history, statistics, widgets, share/import, and accessibility must all be fully localized and correct in both languages, with locale-aware date/number formatting. Language changes must never corrupt or alter local data."

## Clarifications

### Session 2026-08-24

- Q: Should Choice build its own in-app language selector (separate from the device's system language setting), or should it rely solely on the device's system language? → A: Build an in-app language selector in Settings that overrides the device language for Choice only; persists as an app-level setting, independent of user content.
- Q: When Choice is in Arabic, should numbers in history/statistics and dates display using Western Arabic digits (0-9) or Arabic-Indic digits (٠-٩)? → A: Always use Western Arabic digits (0-9), even when the UI is Arabic; only date/time ordering, separators, and percentage conventions follow Arabic-locale formatting.
- Q: When a user changes Choice's language, should already-placed home-screen widgets update their labels immediately, or is it acceptable for them to pick up the new language on their next natural refresh? → A: Force all placed widgets to refresh immediately when the app's language changes.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Use Choice Fully in Arabic with Correct RTL Layout (Priority: P1)

An Arabic-speaking user sets their language to Arabic and expects every core screen of Choice — home, coin creation/editing, the decision flow, and settings — to display in Arabic with a right-to-left layout that feels natural, not mirrored or broken.

**Why this priority**: This is the central deliverable of the feature. Arabic support and correct RTL behavior are what make Choice usable for Arabic-speaking users at all; every other story builds on this foundation working correctly.

**Independent Test**: With the app/device language set to Arabic, navigate through home, coin creation/editing, the decision (flip) flow, and settings, and confirm all visible text is Arabic and the layout direction, alignment, and directional icons are correctly mirrored — independent of whether templates, search, widgets, or history have been verified.

**Acceptance Scenarios**:

1. **Given** the device or app language is set to Arabic, **When** the user opens Choice, **Then** every screen's text appears in Arabic and navigation, lists, and content flow right-to-left.
2. **Given** the app is displaying in Arabic, **When** the user opens a form or dialog (e.g., creating or editing a coin), **Then** all field labels, buttons, and helper text are in Arabic and are positioned using right-to-left start/end alignment.
3. **Given** the app is displaying in Arabic, **When** the user encounters an icon with directional meaning (e.g., a back arrow or a "next" chevron), **Then** the icon is mirrored to point in the RTL-correct direction, while icons without directional meaning (e.g., a coin or settings icon) remain unchanged.

---

### User Story 2 - Switch Between English and Arabic Without Losing Data (Priority: P2)

A user who already has coins, choices, and decision history switches Choice's language (via the device setting or an in-app language option) and expects the entire UI to relabel itself in the new language while every piece of their existing data stays exactly as it was.

**Why this priority**: This establishes the trust guarantee at the heart of the feature: language is purely a presentation concern. It also validates that English itself has been fully moved into the same localization mechanism as Arabic, so there is no regression to existing users.

**Independent Test**: Starting from an app that already has existing coins, choices, favorites, and decision history, switch the language and verify all UI text updates correctly while every piece of existing data is byte-for-byte unchanged, independent of template, search, or widget behavior.

**Acceptance Scenarios**:

1. **Given** a user has existing coins, choices, decision history, and favorites, **When** they switch the app's language from English to Arabic (or Arabic to English), **Then** all UI text updates to the new language and every existing coin, choice, decision record, and favorite remains exactly as it was before the switch.
2. **Given** the app is displaying in English, **When** the user views any screen, **Then** all text matches the app's existing English wording and terminology, with no regressions introduced by moving text into a localization mechanism.
3. **Given** the user switches the language while a screen is open, **When** they return to or reopen that screen, **Then** it renders fully localized in the new language without crashing, freezing, or showing missing-text placeholders.

---

### User Story 3 - Create Coins from Localized Templates (Priority: P3)

A user browsing Choice's built-in coin templates (e.g., "Breakfast") sees each template's name and default choices in their current app language, and once they create a coin from a template, that coin's content stays fixed even if they later change the app's language.

**Why this priority**: Templates are a common entry point for new coins and a visible showcase of localization quality, but they depend on the foundation from Stories 1 and 2. This story also validates the critical boundary between application-provided content (translatable) and user-owned content (never auto-translated).

**Independent Test**: Open the template picker in each supported language and confirm template names/choices are localized; create a coin from a template, switch the app language, and confirm the created coin's name and choices are unchanged while the template picker itself now shows the other language.

**Acceptance Scenarios**:

1. **Given** the app language is Arabic, **When** the user opens the template picker, **Then** template names and their default choices are displayed in Arabic.
2. **Given** a user creates a coin from a template while the app is in English, **When** they later switch the app to Arabic, **Then** the previously created coin's name and choices remain in English exactly as generated, while opening the template picker again shows Arabic template content.

---

### User Story 4 - Search, Review History, and View Statistics in Either Language (Priority: P4)

A user with coin names in Arabic, English, or both searches for their coins and reviews their decision history and statistics, expecting search to work correctly regardless of script and all history/statistics text, dates, and numbers to display correctly for the active language.

**Why this priority**: These are frequently used but secondary flows relative to core navigation and content ownership; they depend on the localization foundation and mainly need correct text handling and locale-aware formatting rather than new interaction patterns.

**Independent Test**: With coins named using both Arabic and English text, search in each app language and confirm correct matches; open history and statistics in each language and confirm labels, empty states, and date/number/percentage formatting are locale-appropriate.

**Acceptance Scenarios**:

1. **Given** the user has coins with Arabic-character names, **When** they search using Arabic text, **Then** matching coins are returned correctly regardless of the app's currently active UI language.
2. **Given** the app is displaying in Arabic, **When** the user opens decision history or statistics, **Then** all labels and empty-state text are in Arabic and dates, times, and percentages follow Arabic-locale formatting conventions (with numeric digits shown as Western Arabic numerals) rather than fixed English formatting.

---

### User Story 5 - Use Widgets, Accessibility Features, and Share/Import in Either Language (Priority: P5)

A user adds a Choice home-screen widget, relies on a screen reader, or shares/imports a coin with another device, and expects all of these surfaces to be correctly localized (or, for shared data, unaffected by language) in both English and Arabic.

**Why this priority**: This rounds out full coverage of the remaining surfaces. It is important for completeness and for users who depend on accessibility tools or widgets, but each of these surfaces can be verified and shipped once the core app experience (Stories 1-2) is solid.

**Independent Test**: Add a Choice widget while the app/device is set to Arabic and confirm its text and layout are localized and RTL-correct; enable a screen reader in Arabic and confirm announcements are in Arabic; export a coin while in one language and import it while set to the other, confirming identical content.

**Acceptance Scenarios**:

1. **Given** the app/device is set to Arabic, **When** a Choice widget is added to the home screen, **Then** its title, flip action, and empty/unavailable states appear in Arabic and the widget lays out right-to-left.
2. **Given** a screen reader is active and the app is displaying in Arabic, **When** the user navigates the app or a widget, **Then** all announced accessibility descriptions are in Arabic, with no leftover English descriptions.
3. **Given** a coin was exported while the app was displaying in English, **When** it is imported on a device/app instance set to Arabic, **Then** the imported coin's name and choices match the original exactly, unaffected by either device's active UI language.

---

### Edge Cases

- What happens when a UI string has no translation available for the active language? The system must fall back to English rather than crash or show a missing-resource placeholder.
- How does the system handle a coin name or choice that mixes Arabic and English text/numerals (bidirectional text) when displayed in a list, search result, or history entry?
- What happens to an open dialog, form, or in-progress coin edit if the device's system language changes while that screen is on screen?
- How does the app display or search a coin whose name/choices were entered in one script (e.g., Arabic) while the current UI language is the other (e.g., English)? The user's own text must display exactly as entered, only the surrounding UI chrome is localized.
- What happens to widgets already placed on the home screen when the app's language changes? They must refresh their labels immediately (see FR-031) without the user needing to remove and re-add them.
- How does the system handle a device set to a language Choice does not yet support (e.g., French)? The app must default to English rather than showing a broken or partially localized UI.
- How does the layout behave on narrow screen widths when Arabic (or English) translated text is significantly longer than the source string — text must wrap or truncate gracefully without clipping or causing horizontal overflow.
- What happens when a user imports a shared coin whose data was produced by an older version of the app, before localization was added? Import must still succeed and preserve the content exactly.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST externalize all user-facing text in the main application into a standard, resource-based localization mechanism rather than embedding it directly in application code.
- **FR-002**: The system MUST externalize all user-facing text in Choice's home-screen widgets into the same localization mechanism used by the main application.
- **FR-003**: The system MUST provide English as a complete default language, preserving the meaning, tone, and terminology of the application's existing English text.
- **FR-004**: The system MUST provide a complete Arabic translation covering every user-facing string that exists in English, with no visible gaps when Arabic is active.
- **FR-005**: The system MUST automatically render the application and its widgets in a right-to-left layout when Arabic is the active language, and in a left-to-right layout for English (and any other LTR language added later), using native layout-direction mechanisms rather than per-component manual mirroring.
- **FR-006**: The system MUST apply layout-direction-aware behavior consistently across navigation, text alignment, lists, buttons, forms, dialogs, search, history, statistics, coin management, settings, and widgets.
- **FR-007**: The system MUST position and space UI elements using logical start/end semantics instead of fixed left/right positioning throughout the application and widgets.
- **FR-008**: The system MUST mirror icons that convey directional meaning (e.g., back/forward, previous/next) when right-to-left layout is active, while leaving icons without directional meaning unchanged.
- **FR-009**: The system MUST preserve user-created coin names and choices exactly as entered, regardless of the application's active language, and MUST NOT translate or otherwise alter this content automatically.
- **FR-010**: The system MUST allow each coin template to define a localized display name and localized default choices for each supported language, and MUST display the version matching the application's currently active language.
- **FR-011**: Once a template is used to create a coin, the resulting coin's name and choices MUST be fixed at the moment of creation and MUST NOT change automatically if the application's language is changed afterward.
- **FR-012**: The system MUST allow users to search their own coins using Arabic text, English text, or a mixture of both, returning correct matches regardless of which script is used.
- **FR-013**: The system's search MUST NOT assume or require ASCII- or English-only input in order to function correctly.
- **FR-014**: The system MUST localize all labels, section titles, descriptions, and empty-state text within the decision history and statistics views for each supported language.
- **FR-015**: The system MUST format dates, times, and percentages shown in history and statistics according to locale-appropriate conventions (ordering, separators, and wording) for the application's active language, rather than a fixed English format, while always rendering the underlying digits using Western Arabic numerals (0-9) in every supported language.
- **FR-016**: The system MUST localize all widget-facing text, including widget titles, flip/decision action labels, empty states, unavailable states, configuration screens, accessibility descriptions, and error or recovery messages.
- **FR-017**: The system MUST render widget layouts using the correct layout direction (right-to-left for Arabic, left-to-right for English) consistent with the main application's handling.
- **FR-018**: The system MUST leave previously recorded widget decision results unchanged when the application's language changes, since these results are user-generated data.
- **FR-019**: The system MUST keep the exported/serialized representation of a coin, used for sharing and importing, independent of the UI language active at the time of export or import.
- **FR-020**: The system MUST localize all UI text surrounding the share and import flows (e.g., prompts, confirmations, success/error messages) using the application's active-language resources.
- **FR-021**: The system MUST preserve imported coin content (names and choices) exactly as represented in the shared data, without translating or otherwise altering it based on the importing device's active language.
- **FR-022**: The system MUST provide a localized, meaningful accessibility label or content description, in the application's active language, for every interactive control and meaningful visual element in the app and its widgets.
- **FR-023**: The system MUST NOT present accessibility descriptions in a language different from the one currently active in the UI (e.g., no English descriptions while the interface is Arabic, or vice versa).
- **FR-024**: The system MUST update its displayed UI language to match a change in the device's system language setting, without requiring reinstallation or causing data loss.
- **FR-025**: The system MUST preserve all existing local user data — coins, choices, decision history, favorites, configurations, and widget associations — unchanged whenever the application's display language changes.
- **FR-026**: The system MUST provide an in-app language selector (within Choice's settings) that lets a user override the application's display language independent of the device-wide system language; the selected preference MUST persist as an app-level setting across restarts, and changing it MUST NOT alter or remove any existing user data.
- **FR-027**: The system MUST default to English whenever the device's or user's selected language does not match a currently supported application language.
- **FR-028**: The system MUST preserve Choice's existing visual design (Catppuccin Mocha color scheme, Material 3 components, and overall layout style) across all supported languages without introducing added visual complexity.
- **FR-029**: The system MUST be structured so that adding support for an additional language in the future requires only adding new localized content, without any change to the application's decision logic, navigation logic, or other business behavior.
- **FR-030**: The system MUST NOT crash or display a missing-resource or placeholder error when the application is switched between any of its supported languages.
- **FR-031**: The system MUST immediately refresh the labels of every placed home-screen widget to reflect Choice's active language whenever that language changes (via the in-app selector or a device system-language change), without requiring the user to remove and re-add the widget.

### Key Entities

- **Language Preference**: The user's active display-language setting for Choice, whether derived from the device's system language or set explicitly within the app; independent from any user content and determines which localized text set and layout direction (LTR/RTL) is used.
- **Localized String Resource Set**: The collection of translated user-facing text, one set per supported language, used to render all in-app and widget text; identified by a stable key rather than tied to any specific user's data.
- **Coin Template**: An application-provided starting point for creating a coin; carries a localized display name and a set of localized default choices per supported language. Distinct from a coin instance a user has actually created.
- **Coin (user-owned)**: A user-created decision object consisting of a name and a set of choices, entered exactly as typed by the user; never auto-translated and unaffected by application language changes once created.
- **Decision/History Record**: A record of a past decision outcome, including its timestamp and selected result. The recorded values are never altered by a language change; only their on-screen date/number formatting adapts to the active language.
- **Widget Association**: The link between a home-screen widget instance and the coin/configuration it displays. Widget-displayed labels are localized, but any decision result already shown on or recorded by the widget is user data and unaffected by language changes.
- **Shared Coin Package**: The exported/serialized representation of a coin used for sharing and importing between devices or app instances; contains only the coin's user-entered data and is independent of any UI language.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of user-facing text in the main application and its widgets displays correctly in both English and Arabic, with no missing-text placeholders, garbled characters, or language-related crashes.
- **SC-002**: When Arabic is active, all screens and widgets render in a right-to-left layout with no clipped, overlapping, or incorrectly mirrored content, verified on both standard and narrow (e.g., ~320dp-wide) screen sizes.
- **SC-003**: Users can find their own coins via search using Arabic text, English text, or a mix, with match accuracy equivalent to the app's existing English-only search behavior.
- **SC-004**: 100% of existing local data (coins, choices, decision history, favorites, configurations, and widget associations) remains unchanged before and after a language switch, verified for every available way of changing the language (device setting and any in-app option).
- **SC-005**: Coin templates display names and default choices fully translated to match the active language, while any coin already created from a template remains exactly as it was when the application's language is later changed.
- **SC-006**: All dates, times, and percentages shown in history and statistics render using the ordering, separator, and wording conventions appropriate to the active language (with zero instances of hardcoded English-only formatting), while numeric digits consistently use Western Arabic numerals (0-9) in every supported language.
- **SC-007**: Every home-screen widget's title, actions, empty/unavailable states, and accessibility descriptions are fully localized and correctly laid out in RTL when Arabic is active, refresh immediately when the language changes, and previously recorded widget decision results remain unchanged.
- **SC-008**: A coin exported while the app is in one language and imported while the app is set to the other language produces content (name and choices) identical to the original, with zero variance.
- **SC-009**: Screen reader users navigating the app in Arabic hear accessibility announcements entirely in Arabic, and entirely in English when the app is in English, with no cross-language leftovers in either direction.
- **SC-010**: Support for an additional language beyond English and Arabic can be added by supplying new localized content alone, with no change to how the application behaves for existing users in either currently supported language.

## Assumptions

- Any device or user-selected language that Choice does not yet support (i.e., anything other than English or Arabic at launch) falls back to English as the default.
- The initial Arabic translation targets Modern Standard Arabic; distinct regional Arabic dialect variants are not required at launch.
- The existing Choice widget types (as established by prior widget work) inherit the same localization and RTL treatment described here; this feature does not introduce new widget types.
- Search matching for Arabic (and mixed-script) text is expected to behave consistently with the app's current English search matching (e.g., substring matching); advanced text normalization (such as diacritic-insensitive matching) is out of scope unless it is already part of existing search behavior.
- Existing coin templates (e.g., "Breakfast") receive curated Arabic translations of their names and default choices as part of this feature, rather than being left English-only.
