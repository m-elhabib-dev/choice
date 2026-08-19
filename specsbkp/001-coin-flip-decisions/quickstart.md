# Quickstart: Coin Flip Decisions

**Feature**: `001-coin-flip-decisions` | **Date**: 2026-08-18

Validates that the implemented feature satisfies the acceptance scenarios in
[spec.md](./spec.md). See [data-model.md](./data-model.md) and
[contracts/](./contracts/) for the underlying structures referenced below.

## Prerequisites

- Android Studio (current stable) with an emulator or physical device running Android 8.0
  (API 26) or higher.
- JDK 17.
- No network connection required at any point (Local First) — validating with the device/emulator
  in **airplane mode** is part of this guide.

## Setup

```bash
# From the Android project root (app module directory)
./gradlew assembleDebug
./gradlew installDebug   # installs on a connected device/emulator
```

## Automated checks

```bash
./gradlew test                 # unit tests: repository, validation, flip distribution, ranking
./gradlew connectedAndroidTest # instrumented Compose UI tests for the 4 user stories
```

Expected: all tests pass. Unit tests cover, at minimum:
- `flipCoin` distribution stays within statistical bounds over many iterations (SC-003).
- `saveCoin` rejects blank/over-length name or choice text, and fewer than 2 choices (FR-002,
  FR-015, FR-018).
- Quick Access score/ranking formula (research.md §7) orders coins as expected for known
  `interactionCount`/`lastInteractionAt` fixtures.

## Manual validation scenarios

Run these with the device in **airplane mode** to also confirm SC-006 (fully offline).

### 1. Empty state (first launch)

1. Launch the app with no coins saved.
2. **Expect**: main screen shows an empty-state message guiding the user to create their first
   coin (spec Edge Cases); no Quick Access section is shown.

### 2. Create a coin — User Story 2 (P2)

1. From the main screen, start creating a coin.
2. Name it "Breakfast"; add choices "Ful", "Eggs", "Falafel", "Cheese".
3. Save.
4. **Expect**: the coin appears on the main screen (SC-002: this whole flow takes under 1 minute).
5. Attempt to save a new coin with only 1 choice. **Expect**: save is blocked with an explanation
   (FR-002).
6. Attempt to save a new coin with a blank name or blank choice text. **Expect**: save is blocked,
   indicating what's missing (FR-015).
7. Attempt to type a coin name or choice text past the enforced limit (40 / 60 characters).
   **Expect**: further input is blocked or save is blocked with a clear message (FR-018).

### 3. Flip a saved coin — User Story 1 (P1)

1. From the main screen, open "Breakfast".
2. Flip it.
3. **Expect**: exactly one choice is displayed clearly as the result (FR-011, FR-012), reachable
   in 3 taps or fewer from app open (SC-001).
4. **Expect**: the result view does not offer an immediate low-friction re-flip — leaving and
   deliberately reopening/re-flipping is required (FR-013, SC-007).
5. Force-close and reopen the app, then reopen "Breakfast". **Expect**: no prior result is shown
   (Decision Outcome resets on restart).
6. Open and flip "Breakfast" (and/or other coins) repeatedly. **Expect**: after enough interactions,
   the most-used/most-recent coins surface in a dedicated Quick Access section at the top of the
   main screen, separate from the full list (FR-014); with 3+ used coins, the top 3 are visible
   without scrolling (SC-004).

### 4. Edit an existing coin — User Story 3 (P3)

1. Open "Breakfast" for editing.
2. Rename it, add a new choice, edit an existing choice's text, then save.
3. **Expect**: the new name appears everywhere the coin is shown; the new choice is selectable on
   a future flip; edited text is reflected (US3 Acceptance Scenarios 1–2).
4. With a coin reduced to exactly 2 choices, attempt to remove one more.
   **Expect**: removal is blocked with an explanation (FR-006).
5. Force-close and reopen the app. **Expect**: all edits persisted (SC-005).

### 5. Delete a coin — User Story 4 (P4)

1. From the main screen, choose to delete a coin.
2. Cancel the confirmation. **Expect**: the coin is unchanged and still present.
3. Delete the coin again and confirm.
4. **Expect**: the coin and its choices are gone from the main screen and do not reappear after
   restarting the app (FR-007).

## Definition of done for this feature

- [ ] All automated unit + instrumented tests pass.
- [ ] All 5 manual scenarios above pass with the device in airplane mode.
- [ ] Visual design matches the Catppuccin Mocha theme consistently across all screens (FR-017).
