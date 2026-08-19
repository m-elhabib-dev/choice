# Quickstart: Coin Decision Suite

**Feature**: `002-coin-decision-suite` | **Date**: 2026-08-19

Validates that the implemented feature satisfies the acceptance scenarios in
[spec.md](./spec.md), on top of the already-validated `001-coin-flip-decisions` MVP. See
[data-model.md](./data-model.md) and [contracts/](./contracts/) for the underlying structures
referenced below.

## Prerequisites

- Android Studio (current stable) with an emulator or physical device running Android 8.0
  (API 26) or higher; ideally two devices/emulators (or one device plus a second app instance
  after a data clear) to validate sharing end-to-end.
- JDK 17.
- The `001-coin-flip-decisions` MVP already implemented and passing its own quickstart.
- No network connection required at any point (Local First) — validate with the device/emulator
  in **airplane mode**.

## Setup

```bash
# From the Android project root (app module directory)
./gradlew assembleDebug
./gradlew installDebug   # installs on a connected device/emulator
```

## Automated checks

```bash
./gradlew test                 # unit tests: repository, domain functions, migration
./gradlew connectedAndroidTest # instrumented Compose UI tests for the 10 user stories
```

Expected: all tests pass, including (at minimum):
- `selectWeighted` distribution stays within statistical bounds over many iterations (SC-002).
- `applyAvoidLastResult` never returns a pool containing `lastChoiceId` unless removing it would
  leave the pool empty, in which case it returns the full pool (SC-003, FR-018).
- `computeStatistics` counts match a fixture `Decision` list exactly, and ties are broken by
  choice position (Clarifications).
- `matchesSearchQuery` is case-insensitive and treats a blank query as "match everything."
- `encodeSharedCoin`/`decodeSharedCoin` round-trip a coin exactly, and `decodeSharedCoin` rejects
  malformed JSON, a missing field, an unsupported `schemaVersion`, and an invalid weight
  (contracts/share-payload-contract.md).
- The `Migration(1, 2)` test (Room `MigrationTestHelper`) confirms pre-existing coins/choices
  survive with the new columns at their documented defaults (FR-043, SC-005).

## Manual validation scenarios

Run these with the device in **airplane mode** to also confirm continued offline operation.

### 1. Accept vs. override a decision — User Story 1 (P1)

1. Flip any existing coin with 2+ choices.
2. **Expect**: exactly one choice is shown with strong visual emphasis; the primary, most
   prominent action finishes/accepts it — no primary action just repeats the flip (FR-007–FR-009,
   SC-001).
3. Accept the decision. **Expect**: it is now recorded in that coin's history (see scenario 6).
4. Flip again; this time choose the secondary "Flip again" (override) action instead of accepting.
   **Expect**: a new independent selection is shown, and nothing was added to history for the
   overridden result (FR-010).

### 2. Favorite a coin and use Quick Coins — User Story 2 (P2)

1. From the home screen, mark two or more coins as favorites.
2. **Expect**: they appear in a "Quick Coins" section above the full coin list, ordered with the
   most recently opened/flipped favorite first (FR-004, Clarifications).
3. Unmark one favorite. **Expect**: it disappears from Quick Coins immediately but remains in the
   full list (FR-002, FR-005).
4. Unmark all favorites. **Expect**: Quick Coins is hidden/empty, and the rest of the home screen
   (full list, search, create) remains fully usable (FR-005).

### 3. Reorder a coin's choices — User Story 3 (P3)

1. Open an existing coin with 3+ choices for editing.
2. Move a choice up/down using the reorder controls, then save.
3. **Expect**: the coin screen shows the new order; force-close and reopen the app — the order
   persists (FR-001, SC-005).

### 4. Weighted selection — User Story 4 (P4)

1. Create or edit a coin without touching weighting. **Expect**: it flips exactly as a normal
   uniform coin, with no extra setup required (FR-011, FR-012).
2. On a separate coin, enable weighting and assign clearly uneven weights (e.g., 8 vs. 1 vs. 1)
   across its choices.
3. Flip it many times (or run the automated distribution test). **Expect**: the heavily-weighted
   choice is selected noticeably more often, proportionally to its share (FR-013, SC-002).
4. Attempt to enter a blank/zero/negative/non-whole weight. **Expect**: save is blocked with an
   explanation (FR-014).

### 5. Avoid last result — User Story 5 (P5)

1. Enable "avoid last result" on a coin with 3+ choices.
2. Flip and accept repeatedly (or run the automated test). **Expect**: the immediately previous
   accepted choice is never selected again on the very next flip (FR-017, SC-003).
3. Reduce the coin down to a single available choice (e.g., via search/filter in a custom test
   fixture, or reason about it via the unit test) and flip. **Expect**: the system falls back to
   the full choice set rather than failing (FR-018).

### 6. Decision history — User Story 6 (P6)

1. Open a coin that has had several accepted decisions.
2. **Expect**: history lists them newest-first with choice and timestamp (FR-021).
3. Edit or remove a choice referenced by a past decision. **Expect**: that history entry still
   displays the original choice text rather than breaking (FR-024).
4. Open the history of a coin with no accepted decisions yet. **Expect**: a clear empty state, not
   an error (FR-027 pattern applied to history too).
5. Force-close and reopen the app. **Expect**: all history is still present (FR-022).

### 7. Coin statistics — User Story 7 (P7)

1. Open statistics for the coin from scenario 6.
2. **Expect**: total count, per-choice breakdown, most/least frequent, and most recent decision
   all match the history (FR-025).
3. Arrange (or use a fixture) so two choices tie for most frequent. **Expect**: the one appearing
   first in the coin's current choice order is shown (Clarifications).
4. Open statistics for a coin with no history. **Expect**: a clear empty state (FR-027).

### 8. Search coins — User Story 8 (P8)

1. With several saved coins, type a partial name into the home screen search field.
2. **Expect**: the list narrows as each character is typed, with no perceptible delay (FR-029,
   SC-006).
3. Clear the query. **Expect**: the full list returns.
4. Search for text matching nothing. **Expect**: a clear "no matches" state (FR-031).

### 9. Create a coin from a template — User Story 9 (P9)

1. Start creating a coin and pick the "Breakfast" template.
2. **Expect**: it's pre-filled with a sensible name and starter choices (FR-032, FR-033); saving it
   takes well under 30 seconds end-to-end (SC-007).
3. After saving, add/edit/remove/reorder its choices. **Expect**: it behaves exactly like any
   manually created coin (FR-034).

### 10. Share and import a coin — User Story 10 (P10)

1. Configure a coin with weighting and/or avoid-last-result enabled, then share it via the system
   share sheet.
2. **Expect**: the share sheet opens with a payload containing name, choices, and configuration,
   but not favorite status or history (FR-036, contracts/share-payload-contract.md).
3. Import the shared text (either by receiving it directly or pasting it into the Import screen).
   **Expect**: a new, independent coin appears with the same name, choices, and configuration
   (weighted/avoid-last behavior matching the original) — SC-008.
4. Attempt to import malformed text, or text edited to carry an unsupported `schemaVersion`.
   **Expect**: a clear rejection message, no crash, and no partial coin created (FR-039, SC-009).
5. Import a coin that looks identical to one already saved. **Expect**: a second, independent copy
   is created rather than a failure or silent merge (FR-040).

## Definition of done for this feature

- [ ] All automated unit + instrumented tests pass, including the `v1 → v2` migration test.
- [ ] All 10 manual scenarios above pass with the device in airplane mode.
- [ ] Sharing and importing both complete with no network connectivity at any point.
- [ ] Visual design matches the existing Catppuccin Mocha theme consistently across all new/changed
      screens (FR from `001`, Constitution Principle V).
- [ ] Pre-existing coins/choices from a `001`-schema database open correctly after the update, with
      no data loss (FR-043, SC-005).
