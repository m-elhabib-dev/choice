# Feature Specification: Android Home-Screen Widgets

**Feature Branch**: `[003-android-widgets]`

**Created**: 2026-08-23

**Status**: Draft

**Input**: User description: "Add Android home-screen widgets to the existing Choice application so users can make decisions without opening the app. Support a Single Coin Widget (represents one saved coin, shows its name, current/most recent decision, and a flip action) and a Quick Coins Widget (shows several favorite/selected coins with a flip action each). Widgets must be configured by picking a saved coin (or coins), must reuse the app's existing coins, choices, decision rules (uniform, weighted, avoid-last-result), and decision history rather than duplicating them, and every widget-made decision must appear in the app's history. Widgets must preserve the app's Choose → Decide → Continue philosophy (present the result as settled, not as an invitation to keep rerolling), stay in sync when coins are renamed/edited/favorited/deleted in the app, support multiple independently-configured widget instances, remain fully offline with no accounts/cloud/backend, follow the Catppuccin Mocha design, adapt to different home-screen widget sizes, handle no-coins/deleted-coin/no-choices/invalid-configuration states gracefully, and be usable with Android accessibility services."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Make a decision from a Single Coin Widget (Priority: P1)

A user adds a Single Coin Widget to their home screen, picks one of their saved coins for it during setup, and can then flip that coin and see the result directly on the home screen, without opening the app.

**Why this priority**: This is the entire product goal of the feature — reducing a frequent decision (e.g., "what's for breakfast?") to a home-screen tap. Without this, there is no widget feature; every other capability builds on it.

**Independent Test**: With at least one saved coin that has two or more choices, add a Single Coin Widget to the home screen, select that coin during configuration, confirm it shows a "ready" state, tap flip, and confirm a choice is displayed immediately on the widget with no app launch required.

**Acceptance Scenarios**:

1. **Given** a user has at least one saved coin, **When** they add a Single Coin Widget to the home screen, **Then** they are prompted to select exactly one saved coin for that widget.
2. **Given** a Single Coin Widget has just been configured and no decision has been made yet, **When** the widget is displayed, **Then** it shows the coin's name and a "ready" state rather than a stale or blank result.
3. **Given** a configured Single Coin Widget, **When** the user taps its flip action, **Then** the widget performs a decision and immediately displays the selected choice as the current result, without the app opening.
4. **Given** a widget has just displayed a decision, **When** the user views the widget, **Then** the result is shown as the settled decision, and any further flip action is visually secondary rather than a prominent invitation to try again.
5. **Given** the user closes and reopens the home screen (or restarts the device), **When** they view a previously configured widget, **Then** it still shows the coin it was configured for and its most recent decision.

---

### User Story 2 - Make decisions for several coins from one Quick Coins Widget (Priority: P2)

A user adds a Quick Coins Widget to their home screen showing several of their frequently used coins at once, each with its own flip action, so they can make more than one kind of decision without switching widgets or opening the app.

**Why this priority**: Many users have several recurring decisions (breakfast, workout, movies); a single-coin widget per decision does not scale well on limited home-screen space, so a multi-coin widget materially increases the feature's day-to-day value.

**Independent Test**: With two or more favorited (or otherwise selected) coins, add a Quick Coins Widget to the home screen, confirm it lists those coins with a flip action each, and confirm tapping one coin's flip action produces and displays a result for that coin only, leaving the others unaffected.

**Acceptance Scenarios**:

1. **Given** a user has one or more favorited coins, **When** they add a Quick Coins Widget without customizing its coin list, **Then** the widget displays their favorited coins by default.
2. **Given** a Quick Coins Widget configuration screen, **When** the user chooses a specific set of coins to display, **Then** the widget shows that chosen set instead of the default favorites.
3. **Given** a Quick Coins Widget showing multiple coins, **When** the user taps the flip action next to one coin, **Then** only that coin's decision updates; the other listed coins remain unchanged.
4. **Given** the widget is resized smaller or larger by the user on the home screen, **When** the available space changes, **Then** the widget shows as many coins as reasonably fit and remains readable and usable, rather than clipping or breaking its layout.
5. **Given** a user has no favorited coins and has not chosen a custom set, **When** they add a Quick Coins Widget, **Then** the widget clearly communicates that no coins are selected and offers a way to choose some.

---

### User Story 3 - Widget decisions follow each coin's configured rules (Priority: P2)

A user who has enabled weighted selection and/or "avoid last result" on a coin sees widget-made decisions for that coin follow the exact same rules as decisions made inside the app.

**Why this priority**: If widget decisions used different odds or rules than the app, the feature would silently break user trust in the decision outcome — this is a correctness guarantee, not a nice-to-have.

**Independent Test**: Configure a coin with weighted selection enabled (heavily favoring one choice) and flip it repeatedly from its widget; confirm the observed distribution matches flipping the same coin repeatedly inside the app. Separately, enable "avoid last result" on a coin, flip it from the widget twice in a row, and confirm the second result never repeats the first (when more than one choice exists).

**Acceptance Scenarios**:

1. **Given** a coin has weighted selection enabled, **When** it is flipped repeatedly from its widget, **Then** results occur with the same relative likelihood as flipping that coin repeatedly inside the app.
2. **Given** a coin has "avoid last result" enabled and more than one choice, **When** it is flipped from its widget immediately after a previous decision, **Then** the new result is different from the immediately preceding one.
3. **Given** a coin uses plain uniform selection (no weighting, no avoid-last-result), **When** it is flipped from its widget, **Then** the decision is drawn from all of its choices with no special weighting or exclusion.
4. **Given** a coin's weighted or avoid-last-result setting is changed inside the app, **When** the coin is next flipped from a widget, **Then** the widget decision follows the newly changed setting, not the previous one.

---

### User Story 4 - Widgets stay consistent with changes made in the app (Priority: P3)

A user renames a coin, edits its choices, changes its favorite status, or deletes it inside the app, and any widget referencing that coin reflects the change instead of showing outdated information or breaking.

**Why this priority**: Widgets sit alongside an actively used app; without this, widgets would quickly diverge from reality and become confusing or untrustworthy, undermining the rest of the feature.

**Independent Test**: Configure a widget for a coin, then rename the coin, change one of its choices, and finally delete it inside the app — after each change, confirm the widget updates to reflect the rename/edit and, after deletion, shows a clear unavailable state rather than crashing or showing the deleted coin's old data.

**Acceptance Scenarios**:

1. **Given** a widget represents a coin, **When** that coin is renamed in the app, **Then** the widget displays the new name without the user needing to remove and re-add the widget.
2. **Given** a widget represents a coin, **When** the coin's choices are added, edited, or removed in the app, **Then** a subsequent decision from the widget is drawn from the coin's current choices.
3. **Given** a widget represents a coin, **When** that coin is deleted in the app, **Then** the widget shows a clear "unavailable" state and offers a way to reconfigure the widget or open the app, instead of crashing or continuing to show the deleted coin.
4. **Given** a coin's favorite status changes in the app, **When** a Quick Coins Widget is using the default favorites source, **Then** its displayed coins update to match the current favorites.
5. **Given** a coin currently has no choices (all were removed), **When** a widget representing it attempts to show its state, **Then** the widget displays a message that a decision cannot be made yet, instead of attempting an invalid flip.

---

### User Story 5 - Open a coin in the app directly from a widget (Priority: P3)

A user taps a coin's name or header on a widget to jump straight into that coin inside the main app, for cases where they want more detail or control than the widget offers.

**Why this priority**: Widgets intentionally offer a reduced set of controls; a direct path back into the full app is the safety valve that keeps the widget simple without losing access to full functionality.

**Independent Test**: From a configured Single Coin Widget or a coin listed in a Quick Coins Widget, tap the coin's name/header and confirm the app opens directly to that coin, without requiring the user to search or navigate manually.

**Acceptance Scenarios**:

1. **Given** a configured Single Coin Widget, **When** the user taps the coin name/header, **Then** the app opens directly to that coin.
2. **Given** a Quick Coins Widget listing several coins, **When** the user taps one coin's name, **Then** the app opens directly to that specific coin, not a generic home screen.
3. **Given** a widget is in an "unavailable" state because its coin was deleted, **When** the user taps the widget's available action, **Then** the app opens to a reasonable fallback (such as the main coin list) rather than failing silently.

---

### User Story 6 - Add multiple independently configured widgets (Priority: P4)

A user places more than one Single Coin Widget on their home screen, each representing a different coin (for example, one for "Breakfast" and one for "Workout"), and each operates independently of the others.

**Why this priority**: Supporting only a single widget instance would force users to choose one recurring decision to fast-track, which undercuts the feature's value for anyone with multiple frequent decisions; this is lower priority than the single-widget core flow because it is an extension of already-working behavior rather than new capability.

**Independent Test**: Add two Single Coin Widgets to the home screen, configure each with a different coin, flip one of them, and confirm only that widget's displayed result changes while the other remains unaffected and still correctly identifies its own coin.

**Acceptance Scenarios**:

1. **Given** a user adds a second Single Coin Widget, **When** they configure it, **Then** they can select a different coin than any other widget already on the home screen is showing.
2. **Given** two Single Coin Widgets configured for different coins, **When** one is flipped, **Then** only that widget's displayed result updates; the other widget's result and coin remain unchanged.
3. **Given** multiple Single Coin Widgets on the home screen, **When** one of their represented coins is deleted in the app, **Then** only the widget(s) representing that coin enter the unavailable state; widgets representing other coins continue working normally.

---

### Edge Cases

- What happens when a user tries to add or configure a widget while they have no saved coins at all? The configuration flow must say so clearly and offer a way to open the app and create a coin first.
- What happens when a coin referenced by a widget is deleted while the widget is on the home screen? The widget must show a safe "unavailable" state (never crash) and offer reconfiguration or a path into the app.
- What happens when a coin has fewer than two choices (or zero) at the moment a widget tries to display or flip it? The widget must show an explanatory state instead of attempting an invalid decision.
- What happens when a widget's saved configuration is missing, corrupted, or references a coin that can no longer be resolved (e.g., after a backup/restore mismatch)? The widget must recover gracefully and let the user reconfigure it rather than getting stuck.
- What happens when a user resizes a widget to a smaller or larger size than its default? The layout must adapt — the Single Coin Widget keeps the result and flip action visible at small sizes; the Quick Coins Widget shows more or fewer coins as space allows.
- What happens when a Quick Coins Widget has more selected/favorited coins than currently fit in the space given? The widget must show as many as fit and remain usable, without erroring or overlapping content.
- What happens when the same coin is represented by more than one widget (e.g., a Single Coin Widget and a Quick Coins Widget entry)? A decision made through either one must be reflected consistently once both are next updated.
- What happens when a user taps a widget's flip action multiple times in rapid succession? Each tap must result in at most one recorded decision at a time; taps must not corrupt the decision history or leave the widget showing a result that was never actually recorded.
- What happens when a coin has "avoid last result" enabled but only has exactly one choice? The widget must follow the same fallback behavior the app already uses in that situation (a decision is still produced), not fail or show an error.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to add a Single Coin Widget to the Android home screen and, during its setup, select exactly one of their saved coins for that widget instance to represent.
- **FR-002**: Users MUST be able to add a Quick Coins Widget to the Android home screen and choose which saved coins it displays, defaulting to the user's favorited coins when no explicit selection is made.
- **FR-003**: Each widget instance MUST persist and use its own configuration (which coin, or which coins, it represents) independently of every other widget instance, so that multiple widgets can represent different coins at the same time.
- **FR-004**: A configured Single Coin Widget MUST display the represented coin's name, its most recent decision result (or a clear "ready" state when no decision has been made yet), and a primary action to make a new decision.
- **FR-005**: A configured Quick Coins Widget MUST display each selected coin's name with a direct flip action for that coin, and MUST adapt the number of coins it shows to the space available on the home screen.
- **FR-006**: Tapping a widget's flip action MUST produce a decision using the same selection rules the coin uses inside the app — uniform selection, weighted selection, and/or "avoid last result" — without requiring the app to be opened.
- **FR-007**: A decision made from a widget MUST be added to that coin's decision history in the same way as a decision made inside the app, so it appears when the user next opens the app.
- **FR-008**: After a widget produces a decision, the widget MUST present that decision as the current, settled result; the widget MUST NOT make repeated flipping the visually primary or default next action.
- **FR-009**: A widget MUST still allow the user to deliberately trigger another decision (an explicit override), consistent with how the app allows overriding a decision, but this action MUST remain visually secondary to accepting the shown result.
- **FR-010**: Users MUST be able to open the corresponding coin directly in the main app from a widget (from a Single Coin Widget, or from an individual coin entry in a Quick Coins Widget).
- **FR-011**: When a coin represented by a widget is renamed, has its choices changed, has its favorite status changed, or has its weighted-selection or avoid-last-result settings changed in the app, every widget representing that coin MUST reflect the change without the user needing to remove and re-add the widget.
- **FR-012**: When a coin represented by a widget is deleted in the app, the widget MUST show a clear "unavailable" state instead of crashing or displaying the deleted coin's previous data, and MUST offer a way to reconfigure the widget or open the app.
- **FR-013**: If a user attempts to configure a widget while they have no saved coins, the system MUST clearly communicate that a coin must be created first and MUST provide a way to open the app to create one.
- **FR-014**: If a coin represented by a widget currently has too few choices to produce a valid decision, the widget MUST display an explanatory state instead of attempting a decision.
- **FR-015**: If a widget's saved configuration becomes invalid or unreadable, the system MUST let the user reconfigure that widget rather than leaving it in a broken or crashed state.
- **FR-016**: Widget configuration MUST be restored automatically after a device restart or home-screen launcher restart, without requiring the user to reconfigure widgets that were already set up.
- **FR-017**: All widget functionality — configuring a widget, viewing its state, and making a decision from it — MUST work fully without an internet connection and without any user account or sign-in.
- **FR-018**: Widgets MUST NOT duplicate or separately store a coin's choices, weights, or decision rules; a widget MUST always act on the coin's current data and decision history rather than a stored copy.
- **FR-019**: Widget content MUST be limited to what is needed for the coin(s) it represents and their current decision; a widget MUST NOT surface a coin's full decision history or any other coin's data.
- **FR-020**: Widgets MUST provide text descriptions for each coin name, current result, and control sufficient for a screen-reader user to understand what the widget represents and what each action does.
- **FR-021**: Adding multiple Single Coin Widgets to the home screen MUST allow each one to represent a different coin, with each widget's displayed state and decisions remaining independent of the others.

### Key Entities

- **Coin**: An existing saved decision context (name, ordered list of choices, favorite status, optional weighted-selection setting, optional avoid-last-result setting) that a widget references by identity; widgets read a coin's current data rather than storing a separate copy of it.
- **Choice**: One selectable option belonging to a coin. Widgets read a coin's current choices (and weights, if applicable) at the moment of deciding, rather than caching them.
- **Decision (history entry)**: The recorded outcome of a coin being flipped — which coin, which choice was selected, and when. Decisions made through a widget are added to the same history as decisions made in the app.
- **Single Coin Widget Configuration**: Per-widget-instance setting identifying which one saved coin that specific home-screen widget instance represents.
- **Quick Coins Widget Configuration**: Per-widget-instance setting identifying which saved coins that specific home-screen widget instance displays (an explicit set, or "use current favorites").

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can go from placing a new, unconfigured Single Coin Widget on the home screen to seeing a decision result, entirely without opening the main app.
- **SC-002**: A user can complete a decision from an already-configured widget — from tapping flip to seeing the result displayed — in a way that feels immediate, with no noticeable delay.
- **SC-003**: 100% of decisions made through a widget appear in the corresponding coin's decision history the next time the user opens the app.
- **SC-004**: A user can place three or more Single Coin Widgets on the home screen, each representing a different coin, with each widget continuing to display and decide correctly for its own coin independently.
- **SC-005**: After a user renames a coin, edits its choices, or changes its favorite/weighted/avoid-last-result settings in the app, every widget representing that coin reflects the change without the user removing and re-adding the widget.
- **SC-006**: When a user deletes a coin that one or more widgets represent, 100% of those widgets show a clear unavailable state instead of crashing or showing stale data.
- **SC-007**: A user with no saved coins who attempts to add a widget is clearly told a coin must be created first and can reach coin creation without confusion or dead ends.
- **SC-008**: All widget configuration, viewing, and decision-making functions correctly with the device's internet connection turned off.
- **SC-009**: A user relying on a screen reader can determine, for both widget types, which coin a widget represents, its current result, and what each visible control does.
- **SC-010**: Repeated decisions made through a widget for a weighted or avoid-last-result coin show the same behavior (relative likelihood of outcomes, and non-repetition of the immediately previous result) as repeated decisions made inside the app for the same coin.

## Assumptions

- The app already provides the underlying coin/choice data, the uniform/weighted/avoid-last-result decision rules, and the decision history that this feature reuses; this specification adds a new local entry point to existing decision-making capability rather than a new or separate decision engine.
- The app's existing "favorite" coins are the default source of coins for a Quick Coins Widget when the user has not made an explicit, widget-specific selection; users may still choose a different explicit set per widget instance.
- A Single Coin Widget represents exactly one coin per widget instance; a user who wants to track several frequent decisions either places several Single Coin Widgets or uses a Quick Coins Widget.
- Widgets refresh when relevant local data changes (for example, a coin rename, edit, favorite change, deletion, or a decision made via the widget itself); continuous or frequent background refreshing on a timer is out of scope, consistent with the app's local-first, low-overhead design.
- "Restart" in this specification refers to the device or home-screen launcher restarting, after which previously configured widgets restore their configuration automatically; it does not imply any network-based backup or sync.
- Supported widget sizes follow whatever grid sizes the user's Android home-screen launcher already offers; no additional minimum or maximum size constraint beyond the launcher's own is introduced by this feature.
- "Opening the corresponding coin in the main app" from a widget navigates to that coin's existing screen in the app (where the user can flip it, edit it, or view its history/statistics); no new widget-specific app screen is introduced for this purpose.
- Full decision history browsing, statistics, coin sharing/import, and coin templates remain in-app-only capabilities and are not reproduced inside widget UI, consistent with keeping widget content to what's necessary for the represented coin(s).
