# Feature Specification: Coin Decision Suite

**Feature Branch**: `[002-coin-decision-suite]`

**Created**: 2026-08-19

**Status**: Draft

**Input**: User description: "Extend the existing Choice Android application into a complete local-first decision-making tool. Beyond the existing MVP (create/edit/delete coins, add/edit/remove choices, flip to decide, offline local persistence), add: reorderable choices; a manual favorite/Quick Coins mechanism on the home screen; optional weighted selection per coin; an optional 'avoid last result' rule per coin; local decision history per coin; basic per-coin statistics; local search of coins by name; bundled coin templates (Breakfast, Lunch, Workout, Movie); versioned native sharing/import of coins; and a decision-result experience that clearly separates accepting a decision from deliberately overriding it. Everything must remain fully offline, Room-backed, Catppuccin Mocha themed, and built so the random engine, weighting, and avoid-last-result rules are independently testable. The product principle is Choose → Decide → Continue, not Choose → Rethink → Reroll."

## Clarifications

### Session 2026-08-19

- Q: How should coins in the Quick Coins section be ordered when a user has favorited more than one? → A: By recency of use — the most recently opened or flipped favorite appears first.
- Q: When two or more choices are tied for most (or least) frequently selected in a coin's statistics, how should the app resolve the tie? → A: Deterministically show whichever tied choice currently appears first in the coin's choice order.
- Q: What kind of value should a choice's weight be on a weighted coin? → A: Positive whole numbers only, with no fixed upper bound.
- Q: When a user isn't searching, in what order should the full list of all saved coins be shown? → A: Alphabetical by name.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Accept a decision instead of rerolling it (Priority: P1)

A user flips a coin and is shown a single, clearly emphasized result. The primary, most prominent action lets them finish and move on with that decision. If they truly want a different outcome, they must deliberately choose a separate, less prominent override action rather than being invited to reroll.

**Why this priority**: This is the product's central principle (Choose → Decide → Continue). Every other capability in this suite exists to support fast, committed decisions — if the result screen instead invites endless rerolling, the rest of the feature set doesn't matter.

**Independent Test**: Flip any coin with two or more choices and verify the result screen shows one outcome prominently, offers a clearly primary "finish" action, and requires an explicit, secondary action to flip again — with no single prominent control that just repeats the flip.

**Acceptance Scenarios**:

1. **Given** a coin has just been flipped, **When** the result is shown, **Then** exactly one choice is displayed with strong visual emphasis as the decision.
2. **Given** a decision result is shown, **When** the user looks at the available actions, **Then** the primary/default-styled action finishes the interaction (accepts the decision) and no primary action simply re-flips the same coin.
3. **Given** a decision result is shown, **When** the user wants a different outcome, **Then** they must select a distinct, explicitly-labeled override action (e.g., "Flip again") that is visually secondary to the accept action.
4. **Given** the user accepts a decision, **When** the acceptance is recorded, **Then** the decision is saved to that coin's history with the coin, the selected choice, and a timestamp.
5. **Given** the user overrides a decision, **When** they trigger another flip, **Then** a new independent selection is made and only the decision the user ultimately accepts is what the user is guided toward finishing with.

---

### User Story 2 - Mark favorite coins as Quick Coins (Priority: P2)

A user marks one or more coins as favorites so they appear in a prominent "Quick Coins" section at the top of the home screen, separate from the full coin list, for one-tap access to the decisions they make most often.

**Why this priority**: Frequent, repeated decisions (e.g., daily breakfast) are the app's main use case; without fast access to the coins a user actually relies on, every use of the app pays the full navigation/search cost.

**Independent Test**: Mark a coin as a favorite, return to the home screen, and confirm it appears in a dedicated Quick Coins section; unmark it and confirm it disappears from that section while remaining in the full coin list.

**Acceptance Scenarios**:

1. **Given** a saved coin, **When** the user marks it as a favorite, **Then** it immediately appears in the home screen's Quick Coins section.
2. **Given** a favorited coin, **When** the user unmarks it, **Then** it is immediately removed from Quick Coins but remains visible and usable in the full coin list.
3. **Given** no coins are marked as favorites, **When** the user views the home screen, **Then** the Quick Coins section is hidden or shows a brief empty state, and the rest of the home screen (full list, search, create) remains fully usable.
4. **Given** several coins are favorited, **When** the user views Quick Coins, **Then** all of them are reachable without scrolling past the full coin list.
5. **Given** more than one coin is favorited, **When** the user views Quick Coins, **Then** they are ordered with the most recently opened or flipped favorite first.

---

### User Story 3 - Reorder a coin's choices (Priority: P3)

While editing a coin, a user changes the display order of its choices to match how they think about the options (e.g., putting the most common option first).

**Why this priority**: Choice order affects scanability of the coin screen and clarity of the result; it's a natural extension of editing but not required for the app's core decision loop to function.

**Independent Test**: Open an existing coin with three or more choices, reorder them, save, and verify the new order is reflected on the coin screen and persists after restarting the app.

**Acceptance Scenarios**:

1. **Given** a coin with three or more choices, **When** the user reorders them and saves, **Then** the coin screen displays the choices in the new order.
2. **Given** a reordered coin, **When** the app is restarted, **Then** the saved choice order is preserved.
3. **Given** a coin being reordered, **When** the user leaves the editor without saving, **Then** the previous order is retained and the attempted reorder is discarded.

---

### User Story 4 - Enable weighted selection for a coin (Priority: P4)

A user who wants some choices to be more likely than others (e.g., "Legs" should come up less often than the rest) enables weighted selection on a coin and assigns each choice a relative weight, without affecting the simple, uniform behavior of any other coin.

**Why this priority**: This is an optional, advanced capability that meaningfully improves a subset of use cases but must not complicate the default coin-creation experience, so it's lower priority than the core flows.

**Independent Test**: Create a normal coin (weighting untouched, default uniform behavior unaffected), then separately enable weighting on a coin, assign different weights per choice, flip it many times, and verify the observed selection frequency roughly tracks the assigned weights.

**Acceptance Scenarios**:

1. **Given** a newly created coin, **When** the user does not enable weighting, **Then** the coin behaves exactly as a normal uniform coin with no extra setup required.
2. **Given** a coin with weighting enabled, **When** the user assigns a relative weight to each choice and flips repeatedly, **Then** choices with higher weights are selected proportionally more often than choices with lower weights.
3. **Given** a coin with weighting enabled, **When** the user enters an invalid weight (blank, zero, negative, non-numeric, or a non-whole number) for a choice, **Then** the system blocks saving that weight and explains that a positive whole number is required.
4. **Given** a weighted coin, **When** the user disables weighting, **Then** the coin reverts to uniform random selection across its choices.

---

### User Story 5 - Enable "avoid last result" for a coin (Priority: P5)

A user who dislikes getting the same answer twice in a row enables "avoid last result" on a coin so that, whenever possible, the choice picked last time cannot be picked again on the very next flip.

**Why this priority**: This is a valuable but optional refinement of the core random-selection behavior, applicable to a subset of coins and users; the app must work well without it.

**Independent Test**: Enable "avoid last result" on a coin with three or more choices, flip it repeatedly, and verify the immediately previous result is never selected again on the next flip while other choices remain available; then reduce the coin to a single available choice and verify the setting safely falls back to selecting from the full choice set instead of failing.

**Acceptance Scenarios**:

1. **Given** a coin has "avoid last result" enabled and has more than one choice, **When** the user flips again, **Then** the choice selected on the immediately previous flip is excluded from that flip's selection pool.
2. **Given** "avoid last result" is enabled and the coin currently has only one available choice, **When** the user flips, **Then** the system falls back to selecting from the full choice set rather than failing or refusing to flip.
3. **Given** "avoid last result" is enabled, **When** the user has never flipped that coin before, **Then** the first flip selects normally from the full choice set.
4. **Given** "avoid last result" is enabled on a weighted coin, **When** the previous result is excluded, **Then** the remaining choices' relative weights still determine the selection among themselves.

---

### User Story 6 - Review a coin's decision history (Priority: P6)

A user opens a coin's history to see a chronological record of past decisions for that coin, most recent first.

**Why this priority**: History provides transparency and supports statistics, but the app is fully usable for its core purpose without a user ever opening it.

**Independent Test**: Flip a coin several times (accepting each result), open its history, and verify each accepted decision appears with its selected choice and timestamp, newest first; verify a coin with no decisions yet shows a clear empty state.

**Acceptance Scenarios**:

1. **Given** a coin has one or more recorded decisions, **When** the user opens its history, **Then** entries are listed with the selected choice and timestamp, ordered from newest to oldest.
2. **Given** a coin has no recorded decisions, **When** the user opens its history, **Then** a clear empty state is shown instead of an error or blank screen.
3. **Given** a choice referenced by a past decision has since been edited or removed from the coin, **When** the user views history, **Then** the historical entry still displays meaningfully (showing the choice text as it was at the time of the decision) rather than breaking or disappearing.
4. **Given** the app is restarted, **When** the user reopens a coin's history, **Then** all previously recorded decisions are still present.

---

### User Story 7 - View a coin's statistics (Priority: P7)

A user views simple statistics for a coin: how many decisions have been made, how often each choice has come up, which choice is most and least frequent, and when the last decision happened.

**Why this priority**: Statistics are a lightweight, secondary insight layered on top of history; useful but not required for the core decide-and-move-on loop.

**Independent Test**: Record several decisions across a coin's choices with an uneven distribution, open its statistics, and verify the total count, per-choice counts, most/least frequent choice, and most recent decision all match the recorded history.

**Acceptance Scenarios**:

1. **Given** a coin with recorded decisions, **When** the user opens its statistics, **Then** the total decision count and a per-choice breakdown are shown, matching the coin's history.
2. **Given** a coin's decision history has an uneven distribution across choices, **When** the user views statistics, **Then** the most-selected and least-selected choices are each clearly identified; if two or more choices tie for most (or least) selected, the one appearing first in the coin's current choice order is shown.
3. **Given** a coin has no recorded decisions, **When** the user opens its statistics, **Then** a clear empty state is shown rather than an error or misleading zeroed chart.
4. **Given** a coin's choices are later edited, reordered, or removed, **When** the user views statistics, **Then** past decisions are still counted correctly against the choice text recorded at decision time.

---

### User Story 8 - Search saved coins by name (Priority: P8)

A user with many saved coins types into a search field on the home screen and sees the matching coins update as they type, without needing to scroll through the full list.

**Why this priority**: Search only becomes valuable once a user has accumulated enough coins that scrolling is inconvenient; it doesn't block any core decision flow.

**Independent Test**: With several saved coins, type a partial name into the search field and verify the visible list narrows to matching coins in real time; clear the search and verify the full list returns; search for text that matches nothing and verify a clear empty-results state.

**Acceptance Scenarios**:

1. **Given** several saved coins exist, **When** the user types a partial coin name into search, **Then** the displayed list updates to show only matching coins as each character is typed.
2. **Given** an active search query, **When** the user clears it, **Then** the full coin list is shown again.
3. **Given** a search query matches no saved coin, **When** results are displayed, **Then** a clear "no matches" state is shown instead of an empty blank area.
4. **Given** the device has no network connectivity, **When** the user searches, **Then** search still works exactly as normal, since it runs entirely against local data.

---

### User Story 9 - Create a coin from a bundled template (Priority: P9)

A user creates a new coin by picking a bundled template (Breakfast, Lunch, Workout, or Movie) instead of starting from a blank coin, then freely edits the resulting choices.

**Why this priority**: Templates reduce first-use friction but are a convenience on top of coin creation, which already works without them.

**Independent Test**: Start creating a coin from the "Breakfast" template, verify it is pre-filled with a sensible starter name and choice list, save it, then edit/add/remove/reorder its choices afterward exactly as with any other coin.

**Acceptance Scenarios**:

1. **Given** the user chooses to create a coin from a template, **When** they pick "Breakfast", "Lunch", "Workout", or "Movie", **Then** a new coin is pre-filled with that template's name and starter choices.
2. **Given** a coin was just created from a template, **When** the user saves it, **Then** it behaves as a fully normal, editable coin indistinguishable in capability from a manually created one.
3. **Given** a coin created from a template, **When** the user later adds, edits, removes, or reorders its choices, **Then** those changes are saved exactly as they would be for any other coin.
4. **Given** the device has no network connection, **When** the user creates a coin from a template, **Then** the template is available and works normally, since templates are bundled with the app.

---

### User Story 10 - Share a coin and import a shared coin (Priority: P10)

A user shares a coin (name, choices, and its configuration) with another person using Android's native share mechanism; the recipient imports it into their own copy of the app as a new, independent coin.

**Why this priority**: Sharing extends the app's value across users but is entirely optional — every other capability in this suite functions fully for a single user who never shares anything.

**Independent Test**: Share a coin that has weighting and/or avoid-last-result configured, import the shared data on a separate copy of the app (or after clearing local data), and verify the imported coin reproduces the same name, choices, and configuration as an independent new coin.

**Acceptance Scenarios**:

1. **Given** a saved coin, **When** the user shares it, **Then** the system share sheet opens with a payload containing the coin's name, choices, and relevant configuration (favorite status is not included, since it is personal to the sharer).
2. **Given** a valid shared coin payload, **When** the recipient imports it, **Then** a new coin is created locally with the same name, choices, and configuration as the original.
3. **Given** a shared payload that is malformed, incomplete, or from an unsupported future version, **When** the user attempts to import it, **Then** the app rejects the import with a clear message and does not crash or partially create a broken coin.
4. **Given** the recipient already has a coin that looks identical (same name and choices) to the one being imported, **When** they import it anyway, **Then** the app creates a new, independent coin rather than failing, silently merging, or overwriting the existing one.
5. **Given** the device has no network connectivity, **When** sharing or importing, **Then** both operations complete using only local mechanisms (the OS share sheet and local file/text handling), requiring no backend or account.

---

### Edge Cases

- A coin with fewer than two choices: the system MUST block save/creation and explain the minimum requirement; existing coins can never be edited down below two choices.
- Removing the choice that was most recently selected as a coin's result: the removal MUST succeed (subject to the two-choice minimum), and the on-screen "last result" view MUST no longer reference a choice that no longer exists.
- Deleting a coin that has decision history: the deletion MUST succeed without error; the coin's history is removed along with it, since history is scoped to viewing "the decision history associated with a coin" and there is no standalone history view once the coin is gone.
- Empty search results: the system MUST show a clear "no matches" state rather than a blank or broken screen.
- Empty history or empty statistics: both MUST show a clear empty state rather than an error, a crash, or a misleadingly zeroed display.
- Enabling "avoid last result" when doing so would leave no valid alternative (e.g., only one choice remains available): the system MUST fall back to selecting from the complete choice set for that flip rather than failing.
- Invalid or missing shared coin data (malformed payload, missing required fields, unsupported future version number): the system MUST reject the import with a clear message and MUST NOT crash or create a partially-formed coin.
- Invalid weights (blank, zero, negative, non-numeric, or a non-whole number) on a weighted coin: the system MUST block saving the invalid value and explain that a positive whole number is required; if a weighted coin somehow ends up with no valid positive weights at flip time, the system MUST safely fall back to uniform selection across its choices rather than failing to flip.
- Changing weights or the choice list on a weighted coin (adding, removing, or editing choices): the system MUST continue to produce a valid selection using the current choices and their current weights (defaulting any choice without an explicit weight to a standard baseline weight) without requiring the user to re-enter every weight.
- Importing an already-existing (duplicate-looking) coin: the system MUST create a new, independent coin rather than crashing, silently overwriting, or blocking the import.
- Application restart during normal usage (mid-edit, mid-flip, or right after accepting a decision): only explicitly saved coins/choices/configuration and explicitly accepted decisions MUST survive the restart; anything not yet saved or accepted MUST be safely discarded without corrupting existing data.
- A coin's choice list or weighting configuration changes after decisions were already recorded against it: existing history entries MUST remain intact and display the choice text as it existed at decision time, without being corrupted, silently changed, or causing statistics to error out.

## Requirements *(mandatory)*

### Functional Requirements

**Coin & choice management**

- **FR-001**: Users MUST be able to reorder the choices within a coin, and the saved order MUST be reflected everywhere the coin's choices are displayed (coin screen, editing, sharing).
- **FR-002**: Users MUST be able to mark and unmark any saved coin as a favorite at any time.
- **FR-003**: The system MUST persist each coin's favorite status locally so it survives app restarts.

**Home screen & Quick Coins**

- **FR-004**: The home screen MUST display a dedicated "Quick Coins" section containing exactly the coins currently marked as favorites, positioned above the full coin list, ordered with the most recently opened or flipped favorite first.
- **FR-005**: The home screen MUST hide the Quick Coins section (or show a minimal empty state) when no coins are marked as favorites, without disrupting access to the full coin list, search, or coin creation.
- **FR-006**: The home screen MUST display the full list of all saved coins regardless of favorite status, sorted alphabetically by name.

**Decision flow (accept vs. override)**

- **FR-007**: After a flip, the system MUST display exactly one selected choice with strong, unambiguous visual emphasis as the decision result.
- **FR-008**: The result view's primary, most visually prominent action MUST finish the interaction (accept the decision); no primary action MUST simply repeat the flip.
- **FR-009**: The system MUST provide a distinct, explicitly labeled override action that lets the user deliberately request a new flip; this action MUST be visually secondary to the accept action.
- **FR-010**: The system MUST record a decision to history only when the user accepts it (or otherwise completes the flow), not merely because a random selection occurred.

**Random selection & weighting**

- **FR-011**: By default, every coin MUST use uniform random selection, giving each available choice an equal chance of being selected.
- **FR-012**: Users MUST be able to enable weighted selection on a coin without changing how normal (non-weighted) coins are created or behave.
- **FR-013**: When weighted selection is enabled, each choice MUST have a configurable positive whole-number weight, with no fixed upper bound, that determines its relative chance of being selected.
- **FR-014**: The system MUST reject blank, zero, negative, non-numeric, or non-whole-number weight values with a clear explanation, and MUST NOT save an invalid weight.
- **FR-015**: The random-selection logic (uniform and weighted) MUST be implemented as a self-contained component, separable from UI and persistence code, so it can be tested independently of the rest of the app.

**Avoid last result**

- **FR-016**: Users MUST be able to enable or disable an "avoid last result" setting per coin.
- **FR-017**: When "avoid last result" is enabled and more than one choice is available, the system MUST exclude the immediately previous decision's choice from the current flip's selection pool.
- **FR-018**: When excluding the previous result would leave no valid choices to select from, the system MUST safely fall back to selecting from the coin's complete current choice set.
- **FR-019**: The "avoid last result" logic MUST be implemented as a self-contained, independently testable component, separate from UI and persistence code.

**Decision history**

- **FR-020**: The system MUST record every accepted decision locally, including a reference to the coin, a reference to (or snapshot of) the selected choice, and a timestamp.
- **FR-021**: Users MUST be able to view the decision history for a specific coin, ordered from most recent to oldest.
- **FR-022**: Decision history MUST persist across app restarts.
- **FR-023**: If a coin is deleted, its associated decision history MUST also be removed, without causing errors elsewhere in the app.
- **FR-024**: If a choice referenced by a past decision is later edited or removed from its coin, the historical entry MUST continue to display meaningfully (showing the choice text as recorded at decision time) rather than becoming corrupted or unreadable.

**Coin statistics**

- **FR-025**: Users MUST be able to view, per coin, the total number of recorded decisions, how many times each choice was selected, the most frequently selected choice, the least frequently selected choice, and the most recent decision; when two or more choices tie for most (or least) frequent, the tie MUST be broken deterministically by showing whichever tied choice currently appears first in the coin's choice order.
- **FR-026**: Statistics MUST be derived entirely from that coin's stored decision history and MUST remain accurate after choices are added, edited, reordered, or removed.
- **FR-027**: A coin with no recorded decisions MUST show a clear empty statistics state rather than an error or a misleading zeroed display.

**Search**

- **FR-028**: Users MUST be able to search their saved coins by name from the home screen.
- **FR-029**: Search results MUST update as the user types, without requiring a separate submit action.
- **FR-030**: Search MUST operate entirely against locally stored coin data and MUST require no network connectivity.
- **FR-031**: A search query that matches no coin MUST show a clear "no matches" state.

**Templates**

- **FR-032**: The system MUST provide bundled coin templates for at least Breakfast, Lunch, Workout, and Movie, available without any network connection.
- **FR-033**: Creating a coin from a template MUST produce a normal, fully editable coin pre-filled with that template's name and starter choices.
- **FR-034**: After creation from a template, users MUST be able to add, edit, remove, and reorder its choices exactly as with any manually created coin.

**Sharing & import**

- **FR-035**: Users MUST be able to share a saved coin using the device's native share mechanism.
- **FR-036**: A shared coin's data MUST include its name, its choices, and any configuration needed to reproduce its behavior (e.g., weighting and avoid-last-result settings), but MUST NOT include personal/local-only state such as favorite status or decision history.
- **FR-037**: The shared data format MUST include a version identifier so future versions of the app can evolve the format while still recognizing older payloads.
- **FR-038**: Users MUST be able to import a shared coin payload, creating a new, independent coin locally from valid data.
- **FR-039**: The system MUST reject invalid, incomplete, or unsupported-version shared payloads with a clear message, without crashing and without creating a partially-formed coin.
- **FR-040**: Importing a coin that appears identical to an already-saved coin MUST succeed by creating a new, independent coin rather than failing, merging, or silently overwriting existing data.
- **FR-041**: Sharing and importing MUST both work with no account, no backend service, and no network connectivity.

**Local-first persistence & reliability**

- **FR-042**: All coins, choices, choice ordering, favorite status, avoid-last-result configuration, weighted-selection configuration, and decision history MUST be persisted locally and remain available after the app is closed and reopened.
- **FR-043**: When the app's local data schema evolves to support new capabilities, existing user data (coins, choices, configuration, and history) MUST be preserved through the update rather than lost or reset.
- **FR-044**: The system MUST NOT crash as a result of invalid user input, malformed shared data, or edge-case states (e.g., a coin with insufficient choices, an incompatible import, or interrupted app usage); it MUST instead degrade gracefully with a clear message where appropriate.

### Key Entities

- **Coin**: A reusable, user-named decision context. Holds an ordered list of two or more choices, a favorite flag, an optional weighted-selection setting, and an optional avoid-last-result setting.
- **Choice**: A single labeled option belonging to a coin, with a position within that coin's ordering and, when its coin has weighting enabled, a positive relative weight.
- **Decision Record**: A single completed, accepted decision: which coin it belongs to, which choice was selected (recorded in a way that survives later edits or removal of that choice), and when it happened. Forms the coin's history and feeds its statistics.
- **Coin Template**: A bundled, read-only starting point (Breakfast, Lunch, Workout, Movie) consisting of a suggested name and starter choices, used only to pre-fill a new, otherwise-normal coin at creation time.
- **Shared Coin Payload**: A versioned, portable representation of a coin's name, choices, and reproducible configuration (weighting, avoid-last-result), used to hand a coin from one installation of the app to another via the platform's native share/import mechanism. Excludes personal/local-only state such as favorite status and decision history.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After any flip, users can finish (accept) the decision in a single tap from the result view.
- **SC-002**: Across at least 500 simulated flips of a weighted coin, each choice's observed selection frequency falls within a small, predictable margin of its assigned weight's proportional share.
- **SC-003**: Across at least 500 simulated flips of a coin with "avoid last result" enabled and two or more choices, the immediately previous result is never selected again on the next flip.
- **SC-004**: Users can reach any favorited coin from the home screen in a single tap, regardless of how many total coins are saved.
- **SC-005**: 100% of accepted decisions, coin configuration changes (favorite, weighting, avoid-last-result, choice order), and history entries remain intact and correct after the app is force-closed and reopened.
- **SC-006**: Search results visibly narrow to matching coins as the user types, with no perceptible delay, entirely without network access.
- **SC-007**: A new user can produce a usable, saved coin from a bundled template in under 30 seconds.
- **SC-008**: A coin shared from one installation and imported into another reproduces the same name, choices, and configuration as an independent coin, with the imported coin's behavior (uniform/weighted/avoid-last-result) matching the original.
- **SC-009**: 100% of malformed, incomplete, or unsupported-version import attempts are rejected with a clear message and never crash the app or leave a broken coin behind.
- **SC-010**: Deleting a coin that has decision history, or removing a choice referenced by past decisions, never crashes the app or corrupts the remaining coins' or choices' history and statistics.

## Assumptions

- **Quick Coins replaces automatic ranking**: The home screen's Quick Coins section is driven entirely by the user's manual favorite marking, not by an automatic recency/frequency score. This intentionally supersedes the prior automatic "Quick Access" ranking behavior from the initial MVP release, per this feature's explicit favorite/unfavorite requirement. Within Quick Coins, favorites are ordered by recency of use (most recently opened/flipped first) — see Clarifications.
- **Weight representation**: Weights are positive whole numbers with no fixed upper bound (not required to sum to any fixed total, such as 100) — see Clarifications; a choice's selection probability is its weight divided by the sum of all valid weights among currently available choices.
- **Missing weight on a new/edited choice**: A choice added to a weighted coin without an explicit weight is assigned a standard baseline weight of 1 (equal to a normal, unweighted share) until the user changes it, so weighting never blocks ordinary choice editing.
- **Weighting + avoid-last-result combined**: When both are enabled, the excluded (previous-result) choice is removed from the pool and the remaining choices' relative weights still govern selection among themselves.
- **History scope and lifecycle**: Decision history is viewed per coin only (no standalone/global history screen in this feature); deleting a coin deletes its history with it, since there is no way to view history for a coin that no longer exists.
- **History resilience to edits**: Each history entry stores the selected choice's text as it existed at the moment of the decision, so later edits, reordering, or removal of that choice never corrupt or blank out past entries or statistics.
- **Import duplicate handling**: Because coin names are not required to be unique (carried over from the initial MVP), importing a coin that looks identical to one already saved always creates a new, independent coin rather than attempting to detect, merge, or block duplicates.
- **Sharing payload contents**: Shared/import data intentionally excludes favorite status and decision history, since both are personal to the device/user that created them and not part of the coin's reproducible definition.
- **Templates are starting points only**: Bundled templates are not synced, updated, or versioned independently from the app itself; a coin created from a template becomes a fully independent, ordinary coin immediately upon creation with no ongoing link back to the template.
- **Character/length limits**: Coin names and choice text continue to use the same reasonable maximum length policy established for the initial MVP, applied consistently to values entered directly or brought in via template or import.
- **Single local user**: As with the initial MVP, there are no accounts or multi-user separation; all data (including history and statistics) belongs to the single user of the device.
