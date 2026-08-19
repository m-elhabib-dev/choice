# Feature Specification: Coin Flip Decisions

**Feature Branch**: `[001-coin-flip-decisions]`

**Created**: 2026-08-18

**Status**: Draft

**Input**: User description: "Build a simple Android application that helps users make decisions when they are unable or unwilling to choose between multiple options. Users create reusable 'coins' representing a decision context with a dynamic list of choices (e.g., a 'Breakfast' coin with Ful, Eggs, Falafel, Cheese). Users can create, edit, delete, and save multiple coins; saved coins appear on the main screen for quick access. Opening a coin lets the user flip it to randomly select one of its choices; the result is presented clearly and treated as final, discouraging repeated reconsidering or rerolling. A coin must have two or more choices, dynamically addable/editable/removable. The app targets quick, repeated decisions (e.g., open app, pick 'Breakfast', flip, decide) with minimal interaction, must work fully offline with local persistence, and uses a modern, minimal Catppuccin Mocha visual design. MVP: create/edit/delete coins; add/edit/remove choices; save and display coins locally; flip to select a choice; clearly display the decision; quick access to frequently used coins. Out of scope for MVP: accounts, cloud sync, social features, ads, backend."

## Clarifications

### Session 2026-08-18

- Q: When ranking coins for "quick access" on the main screen, what should determine a coin's rank — how recently it was used, how often it's been used, or a blend of both? → A: Combined score — a blend of recency and frequency (e.g., frequency weighted higher for recent activity)
- Q: Should the main screen show frequently-used coins in a separate "Quick Access" section, or just reorder the single full list so they float to the top? → A: Dedicated "Quick Access" section at the top, separate from the full list below
- Q: Should the most recent flip result (Decision Outcome) still be shown if the user force-closes and reopens the app, or should it reset? → A: Reset on restart — reopening the app clears any shown result; the coin shows no result until flipped again
- Q: Is there a maximum number of choices a single coin can hold, or can users add an unlimited number? → A: No fixed maximum
- Q: Should coin names and choice text have a maximum character length, or should they be unrestricted (aside from not being blank)? → A: A reasonable character cap should be enforced (exact limit determined during design/planning)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Make a quick decision with a saved coin (Priority: P1)

A user who already has a saved coin (e.g., "Breakfast") opens the app, selects that coin from the main screen, flips it, and is shown a single clear decision to act on.

**Why this priority**: This is the core repeated-use value of the product — the app exists to deliver fast, low-friction decisions from coins the user already relies on. Without this, nothing else matters.

**Independent Test**: Seed the app with one existing coin containing at least two choices. Open the app, select the coin, flip it, and verify a single choice is clearly displayed as the outcome, reachable in only a few taps.

**Acceptance Scenarios**:

1. **Given** a saved coin with two or more choices exists, **When** the user selects it from the main screen and flips it, **Then** exactly one of its choices is selected at random and displayed clearly as the decision.
2. **Given** the user has just flipped a coin and seen a result, **When** they look at the screen, **Then** the interface presents that result as the decision rather than inviting an immediate re-flip.
3. **Given** multiple saved coins exist, **When** the user opens the main screen, **Then** the coins the user opens or flips most appear in a dedicated Quick Access section, easy to find without scrolling past unrelated coins.

---

### User Story 2 - Create a new coin with choices (Priority: P2)

A user creates a new coin by giving it a name and a set of two or more choices, then saves it so it appears on the main screen for future use.

**Why this priority**: The app has no value until a user can build their own reusable decision contexts; this is required before Story 1 can happen with real user data.

**Independent Test**: From the main screen, start creating a coin, name it, add several choices, save it, and verify it now appears on the main screen and can be opened.

**Acceptance Scenarios**:

1. **Given** the user is creating a new coin, **When** they provide a name and at least two choices and save, **Then** the coin appears on the main screen.
2. **Given** the user is creating a new coin, **When** they attempt to save it with fewer than two choices, **Then** the system blocks the save and explains that at least two choices are required.
3. **Given** the user is creating a new coin, **When** they attempt to save it with a blank name or a blank choice, **Then** the system blocks the save and indicates what needs to be filled in.

---

### User Story 3 - Edit an existing coin (Priority: P3)

A user updates an existing coin's name and its list of choices — adding, editing, or removing choices — while keeping the coin usable.

**Why this priority**: Decision contexts change over time (e.g., a new breakfast option); editing keeps saved coins useful without forcing users to recreate them.

**Independent Test**: Open an existing saved coin, rename it, add a new choice, edit the text of another choice, remove a third choice, save, and verify all changes persist after reopening the coin.

**Acceptance Scenarios**:

1. **Given** an existing coin, **When** the user renames it and saves, **Then** the new name is shown everywhere the coin appears.
2. **Given** an existing coin, **When** the user adds a new choice and saves, **Then** the new choice can be selected on a future flip.
3. **Given** an existing coin with exactly two choices, **When** the user attempts to remove one of them, **Then** the system prevents the removal and explains that at least two choices are required.

---

### User Story 4 - Delete a coin (Priority: P4)

A user removes a coin they no longer need from their saved list.

**Why this priority**: Keeps the main screen relevant and uncluttered over time; lowest priority because the app is still fully usable without it for an initial MVP demo.

**Independent Test**: From the main screen, delete an existing coin, confirm the deletion, and verify it no longer appears on the main screen or anywhere else in the app.

**Acceptance Scenarios**:

1. **Given** an existing saved coin, **When** the user chooses to delete it and confirms, **Then** the coin and its choices are permanently removed from the main screen and local storage.
2. **Given** an existing saved coin, **When** the user starts to delete it but cancels the confirmation, **Then** the coin remains unchanged and available.

---

### Edge Cases

- What happens when the user tries to save a coin with fewer than two choices? The system MUST block the save and explain the minimum requirement (see FR-002).
- What happens when the user tries to remove a choice that would leave the coin with fewer than two choices? The system MUST prevent the removal (see FR-006).
- What happens when the user tries to save a coin with a blank name, or a choice with blank text? The system MUST block the save and indicate the problem (see FR-015).
- What happens when the user tries to enter a coin name or choice text longer than the allowed maximum length? The system MUST prevent the text from exceeding the limit (e.g., by stopping further input or blocking the save with a clear message) (see FR-018).
- What happens when two or more choices in the same coin have identical text (e.g., "Eggs" listed twice)? The system MUST allow it; each entry remains independently selectable, which increases that outcome's effective chance of being picked.
- What happens the very first time the app is opened, before any coin has been created? The main screen MUST show an empty state that clearly guides the user to create their first coin.
- What happens if the user backs out of creating or editing a coin without saving? Unsaved changes MUST be discarded; only explicitly saved data persists.
- What happens when the device has no network connection at any point? Every core flow (create, edit, delete, flip, view) MUST continue to work normally, since the app is fully offline.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to create a new coin by providing a name and two or more choices.
- **FR-002**: System MUST require every coin to have at least two choices at all times; a coin MUST NOT be created or saved with fewer than two choices.
- **FR-003**: Users MUST be able to edit the name of an existing coin.
- **FR-004**: Users MUST be able to add a new choice to an existing coin at any time.
- **FR-005**: Users MUST be able to edit the text of an existing choice within a coin.
- **FR-006**: Users MUST be able to remove a choice from a coin, provided the coin retains at least two choices afterward; the system MUST prevent removal that would drop a coin below two choices.
- **FR-007**: Users MUST be able to delete an entire coin; the system MUST require the user to confirm before the deletion is finalized.
- **FR-008**: System MUST persist all coins and their choices locally on the device so they remain available after the app is fully closed and reopened, with no network access required.
- **FR-009**: System MUST display all saved coins on the main screen so the user can select one to open.
- **FR-010**: Users MUST be able to open any saved coin and flip it to select one of its choices.
- **FR-011**: System MUST select the outcome of a flip using an unbiased random process that gives every choice in the coin an equal chance of being selected, regardless of how many choices the coin has.
- **FR-012**: System MUST clearly and prominently display the selected choice as the decision outcome immediately after a flip, in a way that is unambiguous about which choice won.
- **FR-013**: The interface MUST treat a flip's result as the decision: once a result is shown, the primary interface MUST NOT offer an immediate, low-friction way to reroll the same flip. Re-flipping a coin MUST require the user to leave the result view and deliberately choose to flip again.
- **FR-014**: The main screen MUST display a dedicated "Quick Access" section, separate from the full list of coins, containing the user's most-used coins ranked by a combined score that blends recency and frequency of being opened or flipped (weighting recent activity more heavily than older activity), so frequently used coins remain quick to find as the number of saved coins grows.
- **FR-015**: System MUST prevent saving a coin with a blank name, and MUST prevent saving any choice with blank text.
- **FR-016**: System MUST provide full core functionality (creating, editing, deleting coins and choices; flipping; viewing results) with no network connection at any time.
- **FR-017**: All screens MUST present a consistent, modern, minimal visual style using the Catppuccin Mocha color palette.
- **FR-018**: System MUST enforce a reasonable maximum character length on coin names and on choice text (the exact limit is a design/planning detail) so that names and choices always display clearly and unambiguously, including in the flip result view (see FR-012).

### Key Entities

- **Coin**: A reusable decision context created and named by the user (e.g., "Breakfast"). Holds an ordered list of two or more choices with no fixed upper limit, and enough usage information (such as last-opened/last-flipped time and flip count) to support quick access to frequently used coins.
- **Choice**: A single option belonging to a coin (e.g., "Eggs"), consisting of a short text label. A coin's choices can be added, edited, or removed at any time, subject to the two-choice minimum.
- **Decision Outcome**: The choice selected the last time a given coin was flipped, shown to the user as the current result of that coin. Not retained as a historical log in the MVP, and not persisted across app restarts — only the most recent result per coin is relevant, and it resets (no result shown) whenever the app is force-closed and reopened.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user with an existing coin can go from opening the app to seeing a decision result in 3 taps or fewer.
- **SC-002**: A new user can create a fully usable coin (a name plus two or more choices) in under 1 minute.
- **SC-003**: Across repeated flips of the same coin, each choice is selected with statistically even frequency proportional to its share of that coin's choices, with no built-in bias toward any particular choice or position.
- **SC-004**: Among a user's saved coins, the 3 highest-ranked coins (by the combined recency/frequency score) are visible in the main screen's Quick Access section without needing to scroll, regardless of total number of saved coins.
- **SC-005**: 100% of created coins and choices remain intact and available after the app is force-closed and reopened, with zero data loss.
- **SC-006**: All core actions (create, edit, delete a coin; add, edit, remove a choice; flip; view result) complete successfully with the device in airplane mode / no network connectivity.
- **SC-007**: After seeing a flip result, a user cannot re-trigger a new flip of the same coin in a single additional tap from the result screen — reconsidering requires a deliberate, separate action.

## Assumptions

- The app is used by a single local user per device; there are no accounts, profiles, or multi-user separation in the MVP.
- No historical log of past decisions is kept in the MVP; only the most recent flip result per coin is shown, and it resets (is not persisted) across app restarts — a force-closed and reopened app shows no result until the coin is flipped again.
- Random selection is uniform across a coin's listed choices; duplicate choice text within a coin is permitted and simply increases that outcome's effective probability rather than being merged or blocked.
- Unsaved changes made while creating or editing a coin are discarded if the user leaves that screen without explicitly saving.
- Coin names are not required to be unique; two coins may share the same name.
- There is no fixed maximum on the number of choices a coin can hold; the UI must accommodate an arbitrarily long choice list (e.g., via scrolling) rather than capping it.
- "Frequently used" for quick access is derived automatically from the user's own usage of each coin via a combined recency+frequency score (recent activity weighted more heavily) rather than requiring manual pinning or favoriting, since no such manual mechanism was requested.
