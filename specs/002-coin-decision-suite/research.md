# Research: Coin Decision Suite

**Feature**: `002-coin-decision-suite` | **Date**: 2026-08-19

This document resolves every design decision needed beyond what `001-coin-flip-decisions/research.md`
already settled (platform baseline, UI toolkit, architecture pattern, DI, Room, Catppuccin Mocha —
all unchanged and carried forward). It covers only what's new for this feature, per the project
constitution (`.specify/memory/constitution.md`).

## 1. Extending vs. replacing the existing schema/architecture

- **Decision**: Extend the existing `coins`/`choices` Room tables and `CoinRepository` in place;
  add one new `decisions` table. No new module, no new repository class, no architectural pattern
  change.
- **Rationale**: This feature is additive to a shipped MVP (Principle VIII — incremental
  development builds on what exists rather than re-architecting). The existing MVVM +
  Room + Compose structure already isolates persistence and decision logic from UI
  (`001-coin-flip-decisions/research.md` §3, §5), which is exactly what this feature's new logic
  (weighting, avoid-last-result, statistics, search, sharing) also needs.
- **Alternatives considered**: A separate `history`/`stats` module (rejected — over-engineered for
  a single-module app at this scale, violates Simplicity First); a second repository class for
  decisions (rejected — coins, choices, and decisions are all coin-scoped and read together in
  most flows; one `CoinRepository` seam stays simpler to consume from ViewModels, per Principle I).

## 2. Reordering choices

- **Decision**: Up/down move buttons on each choice row in `CoinEditScreen` (move a choice one
  position earlier/later; disabled at the top/bottom edge), backed by updating each affected
  choice's existing `position` column.
- **Rationale**: FR-001 only requires that users can reorder choices and that the order is
  persisted and reflected consistently — it does not require drag-and-drop specifically. Up/down
  buttons are trivially accessible (no gesture to discover), trivially unit/Compose-testable (click
  a button, assert new order), and need no new dependency or custom gesture-detection code
  (Principle VII, Principle I). The `position: Int` column already exists on `ChoiceEntity` from
  `001` specifically to support this.
- **Alternatives considered**: Custom long-press drag-and-drop via `pointerInput` +
  `LazyListState` offset tracking (rejected for v1 — meaningfully more code and edge-case handling
  for a UX refinement the spec doesn't require; can be layered on later without changing the
  underlying `position` data model or `CoinRepository` contract); a third-party reorderable-list
  library (rejected — unjustified dependency for a need already satisfiable with plain Compose,
  Principle VII).

## 3. Weighted selection algorithm

- **Decision**: Cumulative-sum ("roulette wheel") selection: build a running total of each
  available choice's weight (missing/null weight defaults to the baseline `1`, spec Assumptions),
  draw one uniform `Double` in `[0, totalWeight)` via `Random.nextDouble`, and return the first
  choice whose cumulative weight exceeds the draw.
- **Rationale**: Directly satisfies FR-013 (relative weight determines relative chance) and SC-002
  (observed frequency tracks assigned weight proportionally). Weights are positive whole numbers
  with no fixed upper bound (Clarifications), so a simple cumulative-sum scan is O(n) per flip
  against a small `n` (a coin's choice count) — no need for a more sophisticated algorithm (e.g.
  the alias method) at this scale.
- **Alternatives considered**: The alias method (rejected — O(1) selection after O(n) setup only
  matters at large `n`/high flip-rate scales far beyond a personal decision app; added complexity
  fails Simplicity First); repeating a choice `weight` times in a flattened list before picking
  uniformly (rejected — wastes memory/time for large weights and complicates the "no fixed upper
  bound" requirement).

## 4. "Avoid last result" + weighting interaction

- **Decision**: A single pure function, `applyAvoidLastResult(choices, lastChoiceId)`, returns the
  input list with the choice matching `lastChoiceId` removed — unless that removal would leave the
  list empty, in which case it returns the original, unfiltered list (the fallback in FR-018). The
  flip orchestrator (contracts/domain-api.md `selectChoice`) runs this filter *before* handing the
  (possibly-filtered) pool to either the uniform or weighted selector, so weighting always applies
  to whatever pool avoid-last-result leaves behind (User Story 5, Acceptance Scenario 4).
- **Rationale**: Keeping the exclusion step and the selection step as two small, separately-tested
  pure functions (Principle VI) is simpler to reason about and test exhaustively than one combined
  function handling every enabled/disabled permutation inline (Principle I). `lastChoiceId` is
  read from the coin's most recent `Decision` row (data-model.md) rather than a separate "last
  result" column, so there is exactly one source of truth for "the previous decision" and no risk
  of the two going out of sync.
- **Alternatives considered**: Storing a denormalized `lastChoiceId` directly on `Coin` (rejected —
  duplicates information already recoverable from the `decisions` table and would need to be kept
  in sync on every accept *and* on decision-history changes, e.g. if a coin is deleted/recreated;
  querying the latest `Decision` row is simpler and can't drift).

## 5. Statistics computation

- **Decision**: `computeStatistics(choices, decisions)` — a pure function that folds a coin's full
  `Decision` list (already loaded for the History screen) into: total count, a per-choice count map,
  the most/least frequent choice (ties broken by the tied choice's position in `choices`, per
  Clarifications), and the most recent decision. Computed on demand in the ViewModel each time the
  Statistics screen is opened; nothing is cached or denormalized in the database.
- **Rationale**: At this app's realistic scale (Scale/Scope: up to a few thousand decisions per
  coin), an in-memory fold over a `List<Decision>` is sub-millisecond and needs no SQL
  aggregation, no cached counters to keep in sync on every decision/edit, and stays trivially
  unit-testable as a pure function (Principle VI, Principle I) — directly matching FR-026's
  requirement that statistics "remain accurate after choices are added, edited, reordered, or
  removed" with no separate invalidation logic to get wrong.
- **Alternatives considered**: SQL `COUNT`/`GROUP BY` aggregate queries (rejected — adds
  Room query surface for a computation that's simpler and just as fast as a plain Kotlin fold at
  this scale, and harder to unit-test in isolation from the database); maintaining running
  per-choice counters on `Choice`/`Coin` rows (rejected — introduces a second, cache-like source of
  truth that must be kept consistent through choice edits/removals, exactly the corruption risk
  FR-024/FR-026 call out).

## 6. Sharing & import format/transport

- **Decision**: A coin's shareable data is encoded as a single flat JSON object using
  `org.json.JSONObject`/`JSONArray` (part of the Android SDK, not a separate dependency) with a
  top-level `schemaVersion` integer field (contracts/share-payload-contract.md). Sharing out uses
  `Intent(ACTION_SEND).setType("text/plain")` with that JSON string as `EXTRA_TEXT`, opening the
  standard Android share sheet. Importing is supported two ways: (a) `MainActivity` declares an
  `ACTION_SEND` (`text/plain`) intent filter so "Choice" can itself appear as a share target and
  route directly to an Import screen pre-filled with the received text, and (b) that same Import
  screen also accepts manually pasted text, covering cases where the payload arrives through a
  channel that isn't a direct app-to-app share (e.g., a chat message the recipient copies).
- **Rationale**: FR-035–FR-041 require native share/import with no backend, no account, and a
  versioned format. `org.json` avoids adding `kotlinx.serialization` (a new Gradle plugin +
  dependency) for a payload shape small and stable enough not to need a full serialization
  framework (Principle VII). Supporting both direct receive and paste-to-import keeps the feature
  useful regardless of which app the coin passes through on its way to the recipient, without
  requiring file storage, `FileProvider`, or any storage permission (Principle III/I).
- **Alternatives considered**: `kotlinx.serialization` (rejected — new dependency + compiler
  plugin unjustified for one small, stable data shape, Principle VII); sharing as a file via
  `FileProvider` (rejected — adds a content-provider/permission surface for no benefit over a plain
  text payload at this size); a custom URI scheme/deep link (rejected — more manifest/parsing
  complexity than a plain-text share payload for the same result).

## 7. Local-first sharing constraint

- **Decision**: Neither sharing nor importing ever performs a network call; both operate purely on
  an in-memory string handed to/received from Android's `Intent` system.
- **Rationale**: Directly satisfies Principle III and FR-041/SC-006's carryover requirement that
  every core flow work in airplane mode. There is no server-mediated "share link" — the payload
  itself, in the OS share sheet, is the entire transport.
- **Alternatives considered**: A cloud relay/short-link service for sharing (rejected outright —
  directly contradicts Principle III and the spec's Non-Goals).

## 8. Bundled coin templates

- **Decision**: A single `CoinTemplates` object holding a hardcoded `List<CoinTemplate>` (name +
  starter choices) for Breakfast, Lunch, Workout, and Movie, using the exact example choices from
  the spec's Core Concept section (Ful/Eggs/Falafel/Cheese; Rice/Pasta/Chicken/Lentils;
  Chest/Back/Legs/Full Body; Interstellar/Dune/Batman/The Matrix).
- **Rationale**: FR-032 requires templates bundled with the app, available offline, with no backend
  — a compile-time Kotlin list is the simplest possible way to satisfy that (Principle I/VII); no
  assets/resources parsing needed.
- **Alternatives considered**: Templates as a bundled JSON/XML asset file (rejected — adds a
  parsing step and a place for malformed bundled data to break the app, for no benefit over a
  typed Kotlin list at only four templates); a remotely-fetched template catalog (rejected —
  directly contradicts Local First and the spec's explicit "no network connection" requirement for
  templates).

## 9. Retiring the automatic Quick Access score

- **Decision**: Remove `QuickAccessScore.kt` (and its test) entirely. The home screen's Quick Coins
  section now queries favorited coins ordered by the existing `lastInteractionAt` column
  (descending, per the Clarifications), reusing the interaction-tracking already written by
  `recordInteraction` in `001`. The full coin list is ordered alphabetically by name (already the
  existing `observeCoins()` query's order — unchanged). The now-unused `interactionCount` column is
  left in place rather than dropped.
- **Rationale**: Per this feature's Clarifications, Quick Coins is explicit-favorite-driven, not
  score-driven, so the decay-based ranking formula has no remaining caller — keeping dead code
  around would violate Simplicity First. `lastInteractionAt` is exactly what "most recently opened
  or flipped favorite first" needs, so no new column is required for ordering. Dropping
  `interactionCount` would require Room to rebuild the `coins` table during the migration for a
  purely cosmetic cleanup with no functional upside; keeping an unused-but-harmless column is the
  simpler, lower-risk choice (Principle I applied to the migration itself, not just the code).
- **Alternatives considered**: Keeping `QuickAccessScore` as a fallback/tiebreaker within Quick
  Coins (rejected — the Clarifications session explicitly chose plain recency over the combined
  score, and keeping two ranking mechanisms alive at once adds complexity the feature doesn't call
  for); dropping `interactionCount` via a destructive column removal (rejected — meaningfully
  higher migration risk for a cosmetic-only benefit).

## 10. Testing strategy additions

- **Decision**: Extend `001`'s existing test setup with: unit tests for `WeightedSelection`
  (statistical distribution bounds), `AvoidLastResult` (exclusion + fallback), `CoinStatistics`
  (counts, tie-break, empty state), `CoinSearch` (case-insensitive substring match, blank query),
  and `SharePayload` (round-trip encode/decode, malformed input, unsupported version) — all plain
  JUnit4, no Android dependency needed for these. A Room `MigrationTestHelper` test verifies the
  `v1 → v2` migration preserves existing rows and applies correct defaults to new columns.
  Instrumented Compose UI tests cover the new/changed screens' acceptance scenarios, same pattern
  as `001`.
- **Rationale**: Principle VI requires the new decision/data logic to be independently unit-tested
  before being "done"; a migration test is the concrete way to verify FR-043 (existing data
  survives a schema change) rather than relying on manual verification.
- **Alternatives considered**: Skipping a dedicated migration test (rejected — FR-043 and SC-005
  are explicit, testable requirements; only an automated migration test actually proves them across
  future schema changes, not just this one).

## Resolved Technical Context

| Item | Resolution |
|---|---|
| Language/Version | Kotlin 2.0+, JVM target 17 (unchanged) |
| Primary Dependencies | Jetpack Compose (Material 3), Room, Navigation-Compose, Kotlin Coroutines/Flow, Lifecycle-ViewModel-Compose (unchanged); `org.json` (already part of the Android SDK) for share payload encode/decode — no new Gradle dependency |
| Storage | Room (SQLite), on-device only; `v1 → v2` migration adds columns to `coins`/`choices` and a new `decisions` table |
| Testing | JUnit4, kotlinx-coroutines-test, Room in-memory DB + `MigrationTestHelper`, androidx.compose.ui.test (instrumented) — same toolchain as `001` |
| Target Platform | Android, minSdk 26, targetSdk/compileSdk 35 (unchanged) |
| Project Type | Mobile app (single Android application module, unchanged) |
| Performance Goals | 60fps UI/animations; weighted/avoid-last selection and search filtering add only imperceptible, in-memory O(n) work over realistically small lists |
| Constraints | Fully offline, including sharing/import (no network permission); local storage only; existing 40/60-character limits carried forward |
| Scale/Scope | Single local user; tens of coins, unbounded choices per coin (unchanged); up to a few thousand decisions per coin computed in-memory for statistics with no aggregation SQL needed |

All open design questions are resolved; no unknowns remain.
