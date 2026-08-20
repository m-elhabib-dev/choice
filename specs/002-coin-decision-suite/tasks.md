---

description: "Task list template for feature implementation"
---

# Tasks: Coin Decision Suite

**Input**: Design documents from `/specs/002-coin-decision-suite/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md (all present)

**Tests**: Included. Constitution Principle VI (Testable Behavior) and quickstart.md's "Automated
checks" / "Definition of done" require the new domain functions, repository operations, and the
schema migration to be independently unit-tested (mirroring `001-coin-flip-decisions`'s existing
per-function test files), plus instrumented Compose tests per changed/new screen.

**Organization**: Tasks are grouped by user story (P1–P10, from spec.md) to enable independent
implementation and testing of each story on top of the existing `001-coin-flip-decisions` MVP.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US10)
- Every task includes an exact file path

## Path Conventions

Single Android application module at repo root: `app/src/main/kotlin/com/choice/app/...`,
`app/src/test/kotlin/com/choice/app/...` (JVM unit tests), `app/src/androidTest/kotlin/com/choice/app/...`
(instrumented Compose tests) — unchanged from `001-coin-flip-decisions`.

---

## Phase 1: Setup

**Purpose**: Establish a clean, verified baseline before extending the shipped MVP

- [X] T001 Run `./gradlew test connectedAndroidTest` from the repo root to confirm the existing
      `001-coin-flip-decisions` unit and instrumented tests pass before any change in this feature
      begins

**Checkpoint**: Baseline confirmed green — safe to start schema/domain changes.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The `v1 → v2` Room schema change and the domain models it backs. Nearly every user
story (US1, US2, US4, US5, US6, US7, US10) reads or writes one of these new columns/tables, and the
migration itself must be authored and tested as a single atomic Room `Migration`, so it is done once
here rather than split across stories.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T002 [P] Add `isFavorite: Boolean = false`, `weightedEnabled: Boolean = false`,
      `avoidLastResultEnabled: Boolean = false` fields to `CoinEntity` in
      `app/src/main/kotlin/com/choice/app/data/local/CoinEntity.kt` (data-model.md Coin extended)
- [X] T003 [P] Add `weight: Int? = null` field to `ChoiceEntity` in
      `app/src/main/kotlin/com/choice/app/data/local/ChoiceEntity.kt` (data-model.md Choice extended)
- [X] T004 [P] Create `DecisionEntity` (`id`, `coinId` FK → `coins.id` CASCADE, `choiceId` FK →
      `choices.id` SET NULL, `choiceTextSnapshot: String`, `decidedAt: Long`) with indices on
      `coinId` and `choiceId`, in `app/src/main/kotlin/com/choice/app/data/local/DecisionEntity.kt`
      (data-model.md Decision entity)
- [X] T005 [P] Add `isFavorite`, `weightedEnabled`, `avoidLastResultEnabled` fields to the domain
      `Coin` data class and `weight: Int?` to the domain `Choice` data class in
      `app/src/main/kotlin/com/choice/app/domain/Coin.kt`
- [X] T006 [P] Create the domain `Decision` data class (`id`, `coinId`, `choiceId: Long?`,
      `choiceTextSnapshot`, `decidedAt`) in `app/src/main/kotlin/com/choice/app/domain/Decision.kt`
- [X] T007 Bump `@Database` version to `2` on `ChoiceDatabase`, add `DecisionEntity::class` to its
      `entities`, and add the explicit `Migration(1, 2)` implementing exactly the SQL in
      data-model.md's "Migration (v1 → v2)" section, in
      `app/src/main/kotlin/com/choice/app/data/local/ChoiceDatabase.kt` (depends on T002, T003, T004)
- [X] T008 [P] Add decision-related members to `CoinDao` — `insertDecision(decision: DecisionEntity): Long`,
      `observeDecisionsByCoinId(coinId: Long): Flow<List<DecisionEntity>>` (newest first), and
      `getLastDecisionByCoinId(coinId: Long): DecisionEntity?` — in
      `app/src/main/kotlin/com/choice/app/data/local/CoinDao.kt` (depends on T004)
- [X] T009 [P] Update the `CoinWithChoicesRelation.toDomain()` mapping (and any other `CoinEntity`/
      `ChoiceEntity` → domain mapping) in
      `app/src/main/kotlin/com/choice/app/data/CoinRepositoryImpl.kt` to carry the new
      `isFavorite`/`weightedEnabled`/`avoidLastResultEnabled`/`weight` fields through to the domain
      `Coin`/`Choice` (depends on T005)
- [X] T010 [P] Write a Room `MigrationTestHelper` test verifying `Migration(1, 2)` preserves
      pre-existing `coins`/`choices` rows and applies the documented column defaults
      (`isFavorite=false`, `weightedEnabled=false`, `avoidLastResultEnabled=false`, `weight=null`),
      in `app/src/androidTest/kotlin/com/choice/app/data/local/ChoiceDatabaseMigrationTest.kt`
      (depends on T007)

**Checkpoint**: Foundation ready — schema and domain models exist; user story implementation can
now begin.

---

## Phase 3: User Story 1 - Accept a decision instead of rerolling it (Priority: P1) 🎯 MVP

**Goal**: The result screen shows one emphasized choice, a primary "accept/finish" action, and a
secondary "flip again" override action; only an accepted decision is recorded to history.

**Independent Test**: Flip any coin with 2+ choices; verify one prominent result, a primary accept
action, and a distinct secondary override action; accepting records history, overriding does not.

### Tests for User Story 1

- [X] T011 [P] [US1] Unit test for `CoinRepositoryImpl.recordDecision` inserting exactly one
      `Decision` row with the given `choiceTextSnapshot` and a current timestamp, in
      `app/src/test/kotlin/com/choice/app/data/CoinRepositoryImplRecordDecisionTest.kt`
- [X] T012 [P] [US1] Compose UI test verifying the result screen shows one emphasized choice, a
      primary "Accept" action, and a visually secondary "Flip again" action that never itself
      re-flips as the primary control, in
      `app/src/androidTest/kotlin/com/choice/app/ui/coinflip/CoinFlipScreenAcceptOverrideTest.kt`

### Implementation for User Story 1

- [X] T013 [US1] Add `recordDecision(coinId: Long, choiceId: Long, choiceTextSnapshot: String)` to
      `CoinRepository` in `app/src/main/kotlin/com/choice/app/data/CoinRepository.kt`
- [X] T014 [US1] Implement `recordDecision` in
      `app/src/main/kotlin/com/choice/app/data/CoinRepositoryImpl.kt` using `CoinDao.insertDecision`
      (depends on T013, T008)
- [X] T015 [US1] Update `CoinFlipUiState`/`CoinFlipViewModel` to distinguish an in-progress result
      from an accepted one, adding `accept()` (calls `recordDecision`, then marks accepted) and
      `flipAgain()` (discards the current result, computes a new one, records nothing) in
      `app/src/main/kotlin/com/choice/app/ui/coinflip/CoinFlipViewModel.kt` (depends on T014)
- [X] T016 [US1] Update `CoinFlipScreen` to render the result with strong visual emphasis, a
      primary-styled "Accept" button, and a visually secondary "Flip again" button, removing any
      control that repeats the flip as a primary action, in
      `app/src/main/kotlin/com/choice/app/ui/coinflip/CoinFlipScreen.kt` (depends on T015)

**Checkpoint**: User Story 1 is fully functional and testable independently.

---

## Phase 4: User Story 2 - Mark favorite coins as Quick Coins (Priority: P2)

**Goal**: Users mark/unmark any coin as a favorite; the home screen shows a "Quick Coins" section of
favorites (most recently used first) above the full, alphabetical coin list.

**Independent Test**: Mark a coin favorite, confirm it appears in Quick Coins; unmark it, confirm it
disappears from Quick Coins but stays in the full list.

### Tests for User Story 2

- [X] T017 [P] [US2] Unit test for `CoinRepositoryImpl.observeQuickCoins` returning only favorited
      coins ordered by `lastInteractionAt` descending, replacing the removed quick-access-score
      test, in `app/src/test/kotlin/com/choice/app/data/CoinRepositoryImplQuickCoinsTest.kt`
      (replaces `CoinRepositoryImplQuickAccessTest.kt`)
- [X] T018 [P] [US2] Compose UI test: favoriting a coin shows it in Quick Coins, unfavoriting removes
      it from Quick Coins while it remains in the full list, and Quick Coins is hidden/empty-stated
      with zero favorites, in
      `app/src/androidTest/kotlin/com/choice/app/ui/main/MainScreenQuickCoinsTest.kt` (replaces
      `MainScreenQuickAccessTest.kt`)

### Implementation for User Story 2

- [X] T019 [US2] Delete `app/src/main/kotlin/com/choice/app/domain/QuickAccessScore.kt` and
      `app/src/test/kotlin/com/choice/app/domain/QuickAccessScoreTest.kt` (superseded — research.md
      §9); delete `app/src/test/kotlin/com/choice/app/data/CoinRepositoryImplQuickAccessTest.kt` and
      `app/src/androidTest/kotlin/com/choice/app/ui/main/MainScreenQuickAccessTest.kt` once T017/T018
      replace their coverage
- [X] T020 [US2] Remove `observeCoinsWithInteractions` from `CoinDao` and add
      `observeQuickCoins(): Flow<List<CoinWithChoicesRelation>>` (`WHERE is_favorite ... ORDER BY
      last_interaction_at DESC`) in
      `app/src/main/kotlin/com/choice/app/data/local/CoinDao.kt` (depends on T019)
- [X] T021 [US2] Remove `observeQuickAccessCoins` and add `observeQuickCoins()` to `CoinRepository`
      (`app/src/main/kotlin/com/choice/app/data/CoinRepository.kt`) and implement it in
      `CoinRepositoryImpl.kt` on top of `CoinDao.observeQuickCoins` (depends on T020)
- [X] T022 [US2] Add `setFavorite(coinId: Long, isFavorite: Boolean)` to
      `app/src/main/kotlin/com/choice/app/data/local/CoinDao.kt`,
      `app/src/main/kotlin/com/choice/app/data/CoinRepository.kt`, and
      `app/src/main/kotlin/com/choice/app/data/CoinRepositoryImpl.kt`
- [X] T023 [US2] Update `MainViewModel` to consume `observeQuickCoins()` instead of
      `observeQuickAccessCoins()`, expose a favorite/unfavorite action calling `setFavorite`, and
      compute a Quick-Coins-empty flag, in
      `app/src/main/kotlin/com/choice/app/ui/main/MainViewModel.kt` (depends on T021, T022)
- [X] T024 [US2] Update `MainScreen` to render the Quick Coins section (or its empty state) above
      the full alphabetical coin list, with a favorite-toggle affordance per coin, in
      `app/src/main/kotlin/com/choice/app/ui/main/MainScreen.kt` (depends on T023)

**Checkpoint**: User Stories 1 AND 2 both work independently.

---

## Phase 5: User Story 3 - Reorder a coin's choices (Priority: P3)

**Goal**: While editing a coin, up/down controls reorder its choices; the new order persists and is
reflected everywhere the coin's choices are shown.

**Independent Test**: Open a coin with 3+ choices, reorder them, save, verify the new order shows on
the coin screen and survives a restart.

### Tests for User Story 3

- [X] T025 [P] [US3] Unit test for `CoinRepositoryImpl.reorderChoices` persisting a new `position`
      order for a given list of choice IDs, in
      `app/src/test/kotlin/com/choice/app/data/CoinRepositoryImplReorderTest.kt`
- [X] T026 [P] [US3] Compose UI test: moving a choice up/down in `CoinEditScreen` and saving shows
      the new order, with move-up/down disabled at the top/bottom edge, in
      `app/src/androidTest/kotlin/com/choice/app/ui/coinedit/CoinEditScreenReorderTest.kt`

### Implementation for User Story 3

- [X] T027 [US3] Add `reorderChoices(coinId: Long, orderedChoiceIds: List<Long>)` (a `@Transaction`
      updating each choice's `position`) to
      `app/src/main/kotlin/com/choice/app/data/local/CoinDao.kt`,
      `app/src/main/kotlin/com/choice/app/data/CoinRepository.kt`, and
      `app/src/main/kotlin/com/choice/app/data/CoinRepositoryImpl.kt`
- [X] T028 [US3] Add `moveChoiceUp(index: Int)` / `moveChoiceDown(index: Int)` to `CoinEditViewModel`,
      swapping adjacent in-memory choices (no-op at the top/bottom edge) and persisting the order via
      `reorderChoices` on save, in
      `app/src/main/kotlin/com/choice/app/ui/coinedit/CoinEditViewModel.kt` (depends on T027)
- [X] T029 [US3] Add up/down reorder buttons to each choice row in `CoinEditScreen`, disabled at the
      top/bottom edges, in `app/src/main/kotlin/com/choice/app/ui/coinedit/CoinEditScreen.kt`
      (depends on T028)

**Checkpoint**: User Stories 1–3 all work independently.

---

## Phase 6: User Story 4 - Enable weighted selection for a coin (Priority: P4)

**Goal**: A coin can opt into weighted selection with a configurable positive-whole-number weight
per choice, without changing any other coin's uniform behavior.

**Independent Test**: An untouched coin still flips uniformly; a separately weighted coin (e.g. 8/1/1)
selects proportionally to its weights over many flips; invalid weights are rejected.

### Tests for User Story 4

- [X] T030 [P] [US4] Unit test for `selectWeighted` — over 500+ iterations, observed frequency per
      choice falls within a predictable margin of its weight's proportional share (SC-002), in
      `app/src/test/kotlin/com/choice/app/domain/WeightedSelectionTest.kt`
- [X] T031 [P] [US4] Unit test for `selectChoice` branching to `selectWeighted` when
      `weightedEnabled == true` and to uniform `flipCoin` otherwise, in
      `app/src/test/kotlin/com/choice/app/domain/SelectChoiceTest.kt`
- [X] T032 [P] [US4] Compose UI test: enabling weighting and assigning weights on Coin Settings
      succeeds; entering a blank/zero/negative/non-whole weight blocks save with an explanation, in
      `app/src/androidTest/kotlin/com/choice/app/ui/coinsettings/CoinSettingsWeightingTest.kt`

### Implementation for User Story 4

- [X] T033 [P] [US4] Create `selectWeighted(choices: List<Choice>, random: Random = Random.Default): Choice`
      using a cumulative-sum ("roulette wheel") draw, defaulting a `null` weight to the baseline `1`,
      in `app/src/main/kotlin/com/choice/app/domain/WeightedSelection.kt` (research.md §3)
- [X] T034 [US4] Create the `selectChoice(choices, weightedEnabled, avoidLastResultEnabled,
      lastChoiceId, random)` orchestrator per contracts/domain-api.md, delegating to `flipCoin` or
      `selectWeighted` based on `weightedEnabled` (avoid-last-result filtering is wired in in US5),
      in `app/src/main/kotlin/com/choice/app/domain/SelectChoice.kt` (depends on T033)
- [X] T035 [US4] Add `setWeightedEnabled(coinId: Long, enabled: Boolean)` and
      `updateChoiceWeights(coinId: Long, weights: List<Pair<Long, Int?>>)` (validating each present
      weight is a positive whole number, throwing `IllegalArgumentException` otherwise) to
      `app/src/main/kotlin/com/choice/app/data/local/CoinDao.kt`,
      `app/src/main/kotlin/com/choice/app/data/CoinRepository.kt`, and
      `app/src/main/kotlin/com/choice/app/data/CoinRepositoryImpl.kt`
- [X] T036 [P] [US4] Create `CoinSettingsUiState` + `CoinSettingsViewModel` with a weighting toggle
      and per-choice weight fields, validating blank/zero/negative/non-whole input before allowing
      save, in `app/src/main/kotlin/com/choice/app/ui/coinsettings/CoinSettingsViewModel.kt`
      (depends on T035)
- [X] T037 [P] [US4] Create `CoinSettingsScreen` with the weighting toggle and per-choice weight
      inputs (showing the validation error inline), in
      `app/src/main/kotlin/com/choice/app/ui/coinsettings/CoinSettingsScreen.kt`
- [X] T038 [US4] Update `CoinFlipViewModel.flip()` to call `selectChoice(choices, coin.weightedEnabled,
      coin.avoidLastResultEnabled, lastChoiceId = null, random)` instead of `flipCoin(...)` directly
      (avoid-last-result's `lastChoiceId` is wired in US5), in
      `app/src/main/kotlin/com/choice/app/ui/coinflip/CoinFlipViewModel.kt` (depends on T034)
- [X] T039 [US4] Register `CoinSettingsViewModel` in `CoinViewModelFactory` and add a
      `"coinsettings/{coinId}"` route reachable from `CoinFlipScreen`/`CoinEditScreen`, in
      `app/src/main/kotlin/com/choice/app/ui/ViewModelFactory.kt` and
      `app/src/main/kotlin/com/choice/app/MainActivity.kt` (depends on T036, T037)

**Checkpoint**: User Stories 1–4 all work independently; uniform coins remain unaffected.

---

## Phase 7: User Story 5 - Enable "avoid last result" for a coin (Priority: P5)

**Goal**: A coin can opt into excluding its immediately previous result from the next flip's pool,
safely falling back to the full pool when exclusion would leave nothing to pick from.

**Independent Test**: With "avoid last result" enabled on a 3+-choice coin, repeated flips never
repeat the immediately previous result; reduced to one available choice, it falls back to the full
set instead of failing.

### Tests for User Story 5

- [X] T040 [P] [US5] Unit test for `applyAvoidLastResult` — excludes the matching `lastChoiceId`,
      returns the choices unchanged for a `null`/non-matching ID, and falls back to the full,
      unfiltered list when exclusion would leave it empty, in
      `app/src/test/kotlin/com/choice/app/domain/AvoidLastResultTest.kt`
- [X] T041 [P] [US5] Unit test verifying `selectChoice` applies `applyAvoidLastResult` before
      handing the pool to uniform/weighted selection, and never repeats the previous result across
      500+ iterations (SC-003), extending
      `app/src/test/kotlin/com/choice/app/domain/SelectChoiceTest.kt`
- [X] T042 [P] [US5] Compose UI test: enabling "avoid last result" on Coin Settings and flipping
      repeatedly never shows the immediately previous result on the next flip, in
      `app/src/androidTest/kotlin/com/choice/app/ui/coinsettings/CoinSettingsAvoidLastResultTest.kt`

### Implementation for User Story 5

- [X] T043 [P] [US5] Create
      `applyAvoidLastResult(choices: List<Choice>, lastChoiceId: Long?): List<Choice>` in
      `app/src/main/kotlin/com/choice/app/domain/AvoidLastResult.kt` (research.md §4)
- [X] T044 [US5] Wire `applyAvoidLastResult` into the `selectChoice` orchestrator, filtering the pool
      first when `avoidLastResultEnabled && lastChoiceId != null`, in
      `app/src/main/kotlin/com/choice/app/domain/SelectChoice.kt` (depends on T034, T043)
- [X] T045 [US5] Add `setAvoidLastResultEnabled(coinId: Long, enabled: Boolean)` to
      `app/src/main/kotlin/com/choice/app/data/local/CoinDao.kt`,
      `app/src/main/kotlin/com/choice/app/data/CoinRepository.kt`, and
      `app/src/main/kotlin/com/choice/app/data/CoinRepositoryImpl.kt`
- [X] T046 [US5] Add `getLastDecision(coinId: Long): Decision?` to
      `app/src/main/kotlin/com/choice/app/data/CoinRepository.kt`/
      `app/src/main/kotlin/com/choice/app/data/CoinRepositoryImpl.kt`, reading the most recent
      `Decision` via `CoinDao.getLastDecisionByCoinId` (depends on T008)
- [X] T047 [P] [US5] Add an avoid-last-result toggle to `CoinSettingsUiState`/`CoinSettingsViewModel`
      and `CoinSettingsScreen`, calling `setAvoidLastResultEnabled`, in
      `app/src/main/kotlin/com/choice/app/ui/coinsettings/CoinSettingsViewModel.kt` and
      `app/src/main/kotlin/com/choice/app/ui/coinsettings/CoinSettingsScreen.kt` (depends on T036,
      T037, T045)
- [X] T048 [US5] Update `CoinFlipViewModel.flip()` to resolve `lastChoiceId` via `getLastDecision(coinId)`
      and pass it into `selectChoice(...)`, in
      `app/src/main/kotlin/com/choice/app/ui/coinflip/CoinFlipViewModel.kt` (depends on T038, T044,
      T046)

**Checkpoint**: User Stories 1–5 all work independently.

---

## Phase 8: User Story 6 - Review a coin's decision history (Priority: P6)

**Goal**: A coin's History screen lists its accepted decisions newest-first, surviving later edits
or removal of the referenced choice.

**Independent Test**: Flip-and-accept a coin several times, open its history, verify newest-first
entries with choice + timestamp; a never-flipped coin shows a clear empty state.

### Tests for User Story 6

- [X] T049 [P] [US6] Unit test for `observeDecisionHistory` returning newest-first decisions whose
      `choiceTextSnapshot` remains intact after the referenced choice is edited/removed, in
      `app/src/test/kotlin/com/choice/app/data/CoinRepositoryImplHistoryTest.kt`
- [X] T050 [P] [US6] Compose UI test: `HistoryScreen` lists decisions newest-first with choice text
      and timestamp, and shows a clear empty state with no decisions, in
      `app/src/androidTest/kotlin/com/choice/app/ui/history/HistoryScreenTest.kt`

### Implementation for User Story 6

- [X] T051 [US6] Add `observeDecisionHistory(coinId: Long): Flow<List<Decision>>` (newest first) to
      `app/src/main/kotlin/com/choice/app/data/CoinRepository.kt`/
      `app/src/main/kotlin/com/choice/app/data/CoinRepositoryImpl.kt` on top of
      `CoinDao.observeDecisionsByCoinId` (depends on T008)
- [X] T052 [P] [US6] Create `HistoryUiState` + `HistoryViewModel` (decision list + empty-state flag)
      in `app/src/main/kotlin/com/choice/app/ui/history/HistoryViewModel.kt` (depends on T051)
- [X] T053 [P] [US6] Create `HistoryScreen` rendering decisions newest-first with an empty state, in
      `app/src/main/kotlin/com/choice/app/ui/history/HistoryScreen.kt`
- [X] T054 [US6] Register `HistoryViewModel` in `CoinViewModelFactory` and add a `"history/{coinId}"`
      route reachable from `CoinFlipScreen`, in
      `app/src/main/kotlin/com/choice/app/ui/ViewModelFactory.kt` and
      `app/src/main/kotlin/com/choice/app/MainActivity.kt` (depends on T052, T053)

**Checkpoint**: User Stories 1–6 all work independently.

---

## Phase 9: User Story 7 - View a coin's statistics (Priority: P7)

**Goal**: A coin's Statistics screen shows total decisions, a per-choice breakdown, most/least
frequent choice (tie-broken by current choice position), and the most recent decision.

**Independent Test**: Record an uneven decision distribution, open statistics, verify totals/
per-choice/most-least-frequent/most-recent match the history; a coin with no history shows an
empty state.

### Tests for User Story 7

- [X] T055 [P] [US7] Unit test for `computeStatistics` — counts, most/least frequent with
      position-based tie-break, and `totalDecisions == 0` empty-state output, in
      `app/src/test/kotlin/com/choice/app/domain/CoinStatisticsTest.kt`
- [X] T056 [P] [US7] Compose UI test: `StatisticsScreen` shows total/per-choice/most-least-frequent/
      most-recent matching a fixture history, and an empty state with no history, in
      `app/src/androidTest/kotlin/com/choice/app/ui/statistics/StatisticsScreenTest.kt`

### Implementation for User Story 7

- [X] T057 [P] [US7] Create `CoinStatistics`/`ChoiceCount` data classes and
      `computeStatistics(choices: List<Choice>, decisions: List<Decision>): CoinStatistics` in
      `app/src/main/kotlin/com/choice/app/domain/CoinStatistics.kt` (per contracts/domain-api.md)
- [X] T058 [P] [US7] Create `StatisticsViewModel` combining `observeCoin` and
      `observeDecisionHistory` through `computeStatistics(...)`, in
      `app/src/main/kotlin/com/choice/app/ui/statistics/StatisticsViewModel.kt` (depends on T057,
      T051)
- [X] T059 [P] [US7] Create `StatisticsScreen` rendering the total/per-choice breakdown/most-least-
      frequent/most-recent with an empty state, in
      `app/src/main/kotlin/com/choice/app/ui/statistics/StatisticsScreen.kt`
- [X] T060 [US7] Register `StatisticsViewModel` in `CoinViewModelFactory` and add a
      `"statistics/{coinId}"` route reachable from `CoinFlipScreen`/`HistoryScreen`, in
      `app/src/main/kotlin/com/choice/app/ui/ViewModelFactory.kt` and
      `app/src/main/kotlin/com/choice/app/MainActivity.kt` (depends on T058, T059)

**Checkpoint**: User Stories 1–7 all work independently.

---

## Phase 10: User Story 8 - Search saved coins by name (Priority: P8)

**Goal**: A home-screen search field narrows the full coin list live as the user types, entirely
against local data.

**Independent Test**: Type a partial name, verify the list narrows live; clear it, verify the full
list returns; search for no match, verify a clear "no matches" state.

### Tests for User Story 8

- [X] T061 [P] [US8] Unit test for `matchesSearchQuery` — case-insensitive substring match, blank
      query matches everything, in `app/src/test/kotlin/com/choice/app/domain/CoinSearchTest.kt`
- [X] T062 [P] [US8] Compose UI test: typing narrows the visible list live, clearing restores the
      full list, and a no-match query shows a clear "no matches" state, in
      `app/src/androidTest/kotlin/com/choice/app/ui/main/MainScreenSearchTest.kt`

### Implementation for User Story 8

- [X] T063 [P] [US8] Create `matchesSearchQuery(coinName: String, query: String): Boolean` in
      `app/src/main/kotlin/com/choice/app/domain/CoinSearch.kt`
- [X] T064 [US8] Add a search-query state and a `matchesSearchQuery`-filtered coin list (plus a
      no-matches flag) to `MainViewModel`, in
      `app/src/main/kotlin/com/choice/app/ui/main/MainViewModel.kt` (depends on T063, T023)
- [X] T065 [US8] Add a search field and a "no matches" empty state to `MainScreen`, in
      `app/src/main/kotlin/com/choice/app/ui/main/MainScreen.kt` (depends on T064, T024)

**Checkpoint**: User Stories 1–8 all work independently.

---

## Phase 11: User Story 9 - Create a coin from a bundled template (Priority: P9)

**Goal**: A user picks a bundled template (Breakfast, Lunch, Workout, Movie) to pre-fill a new,
otherwise fully normal, editable coin.

**Independent Test**: Create a coin from "Breakfast", verify it's pre-filled and saves quickly, then
edit/add/remove/reorder its choices exactly like any other coin.

### Tests for User Story 9

- [X] T066 [P] [US9] Unit test verifying `CoinTemplates` provides Breakfast/Lunch/Workout/Movie with
      the spec's example starter choices, in
      `app/src/test/kotlin/com/choice/app/domain/CoinTemplatesTest.kt`
- [X] T067 [P] [US9] Compose UI test: picking the "Breakfast" template pre-fills `CoinEditScreen`'s
      name and choices, and the saved coin is subsequently editable like any other, in
      `app/src/androidTest/kotlin/com/choice/app/ui/templates/TemplatePickerScreenTest.kt`

### Implementation for User Story 9

- [X] T068 [P] [US9] Create the `CoinTemplate` data class and a `CoinTemplates` object listing
      Breakfast/Lunch/Workout/Movie with their starter choices, in
      `app/src/main/kotlin/com/choice/app/domain/CoinTemplates.kt` (research.md §8)
- [X] T069 [P] [US9] Create `TemplatePickerScreen` listing the four templates plus a "start blank"
      option, in `app/src/main/kotlin/com/choice/app/ui/templates/TemplatePickerScreen.kt` (depends
      on T068)
- [X] T070 [US9] Add an optional template pre-fill parameter to `CoinEditViewModel`'s blank-coin
      initialization path (pre-populating name + starter choices), in
      `app/src/main/kotlin/com/choice/app/ui/coinedit/CoinEditViewModel.kt` (depends on T068)
- [X] T071 [US9] Add a `"templates"` route in `MainActivity`, change `MainScreen`'s "create coin"
      action to navigate there first, and pass the chosen template's name/choices as nav arguments
      into the `"coinedit"` route, in `app/src/main/kotlin/com/choice/app/MainActivity.kt` (depends
      on T069, T070)

**Checkpoint**: User Stories 1–9 all work independently.

---

## Phase 12: User Story 10 - Share a coin and import a shared coin (Priority: P10)

**Goal**: A coin's name/choices/weighting/avoid-last-result configuration can be shared via the
native Android share sheet and imported elsewhere as a new, independent coin, using a versioned JSON
payload.

**Independent Test**: Share a weighted/avoid-last coin, import the payload on another install (or
after a data clear), verify it reproduces the same name/choices/configuration as an independent coin;
malformed/unsupported-version payloads are rejected without crashing.

### Tests for User Story 10

- [X] T072 [P] [US10] Unit test for `encodeSharedCoin`/`decodeSharedCoin` — an exact round trip, and
      rejection (via `InvalidSharePayloadException`) of malformed JSON, a missing required field, an
      unsupported `schemaVersion`, and a non-positive/non-whole weight, per
      contracts/share-payload-contract.md, in
      `app/src/test/kotlin/com/choice/app/domain/SharePayloadTest.kt`
- [X] T073 [P] [US10] Unit test for `CoinRepositoryImpl.importSharedCoin` always inserting a new,
      independent coin — including when an existing coin already has the same name/choices — in
      `app/src/test/kotlin/com/choice/app/data/CoinRepositoryImplImportTest.kt`
- [X] T074 [P] [US10] Compose UI test: sharing a coin launches `ACTION_SEND` with the expected JSON
      payload; pasting a valid payload into the Import screen creates a new coin, and pasting an
      invalid one shows a clear rejection message without crashing, in
      `app/src/androidTest/kotlin/com/choice/app/ui/share/ImportCoinScreenTest.kt`

### Implementation for User Story 10

- [X] T075 [P] [US10] Create `SharedCoin`/`SharedChoice` data classes, `CURRENT_SHARE_SCHEMA_VERSION`,
      `encodeSharedCoin`/`decodeSharedCoin`, and `InvalidSharePayloadException` using `org.json`, per
      contracts/share-payload-contract.md's validation rules, in
      `app/src/main/kotlin/com/choice/app/domain/SharePayload.kt`
- [X] T076 [US10] Add `importSharedCoin(payload: SharedCoin): Long` to
      `app/src/main/kotlin/com/choice/app/data/CoinRepository.kt`/
      `app/src/main/kotlin/com/choice/app/data/CoinRepositoryImpl.kt`, always inserting a brand-new
      `Coin` (`isFavorite = false`) + `Choice` rows with no existing-coin lookup (depends on T075)
- [X] T077 [P] [US10] Create `ImportCoinViewModel` handling pasted/received text →
      `decodeSharedCoin` → `importSharedCoin`, surfacing the decode error message on failure, in
      `app/src/main/kotlin/com/choice/app/ui/share/ImportCoinViewModel.kt` (depends on T075, T076)
- [X] T078 [P] [US10] Create `ImportCoinScreen` with a paste-text field, an import action, and error
      display, in `app/src/main/kotlin/com/choice/app/ui/share/ImportCoinScreen.kt`
- [X] T079 [US10] Add a "Share" action to `CoinFlipScreen`/`CoinSettingsScreen` launching
      `Intent(ACTION_SEND).setType("text/plain")` with `EXTRA_TEXT = encodeSharedCoin(...)` via
      `Intent.createChooser`, in
      `app/src/main/kotlin/com/choice/app/ui/coinflip/CoinFlipScreen.kt` and
      `app/src/main/kotlin/com/choice/app/ui/coinsettings/CoinSettingsScreen.kt` (depends on T075)
- [X] T080 [US10] Add an `ACTION_SEND` (`text/plain`) intent-filter to `MainActivity` in
      `app/src/main/AndroidManifest.xml`, route any incoming `EXTRA_TEXT` to the Import screen,
      register `ImportCoinViewModel` in `CoinViewModelFactory`, and add an
      `"import?sharedText={sharedText}"` route, in
      `app/src/main/kotlin/com/choice/app/ui/ViewModelFactory.kt` and
      `app/src/main/kotlin/com/choice/app/MainActivity.kt` (depends on T077, T078)

**Checkpoint**: All 10 user stories are now independently functional.

---

## Phase 13: Polish & Cross-Cutting Concerns

**Purpose**: Consistency and final validation across all ten stories

- [X] T081 [P] Review the new History/Statistics/Quick-Coins/Search empty states for a single shared
      empty-state composable and consistent Catppuccin Mocha styling, extracting one into
      `app/src/main/kotlin/com/choice/app/ui/components/` if duplicated
- [X] T082 [P] Update `README.md` at the repo root to reflect the ten new capabilities, if its
      feature list is user-facing documentation rather than only describing the `001` MVP
- [ ] T083 Run all manual validation scenarios (1–10) from
      `specs/002-coin-decision-suite/quickstart.md` with the device/emulator in airplane mode
- [X] T084 Run `./gradlew test connectedAndroidTest` from the repo root and confirm every unit and
      instrumented test passes, including the `Migration(1, 2)` test (FR-043, SC-005)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories (the `v1 → v2` schema and
  domain model changes almost every story reads or writes).
- **User Stories (Phase 3–12)**: All depend on Foundational completion.
  - US1–US10 are otherwise mutually independent at the code level, but US4 and US5 both extend the
    same `selectChoice` orchestrator (T034) and the same `CoinSettingsScreen`/`ViewModel` (T036/
    T037) — implement US4 before US5 if working sequentially, since US5's tasks (T044, T047) name
    T034/T036/T037 as dependencies. US8 similarly extends US2's `MainViewModel`/`MainScreen` (T023/
    T024).
  - Recommended sequential order matches priority: US1 → US2 → US3 → US4 → US5 → US6 → US7 → US8 →
    US9 → US10.
- **Polish (Phase 13)**: Depends on all desired user stories being complete.

### User Story Dependencies

- **US1 (P1)**: Foundational only.
- **US2 (P2)**: Foundational only.
- **US3 (P3)**: Foundational only.
- **US4 (P4)**: Foundational only; US5 builds on US4's `selectChoice` orchestrator and Coin Settings
  screen (see above) but US4 itself does not depend on US5.
- **US5 (P5)**: Foundational + US4's `selectChoice` (T034) and Coin Settings screen (T036/T037).
- **US6 (P6)**: Foundational only.
- **US7 (P7)**: Foundational + US6's `observeDecisionHistory` (T051).
- **US8 (P8)**: Foundational + US2's `MainViewModel`/`MainScreen` (T023/T024).
- **US9 (P9)**: Foundational only.
- **US10 (P10)**: Foundational only (does not require US4/US5 to be implemented first, though its
  payload carries the `weightedEnabled`/`avoidLastResultEnabled`/`weight` fields those stories
  introduce).

### Within Each User Story

- Tests are written first and should fail before implementation.
- Repository/DAO methods before ViewModel logic; ViewModel logic before screen UI.
- Domain (pure Kotlin) functions before the repository/ViewModel code that calls them.

### Parallel Opportunities

- All Foundational tasks marked `[P]` (T002, T003, T004, T005, T006, T008, T009, T010) touch
  different files and can run in parallel once their own listed dependencies are met.
- Once Foundational is done, US1, US2, US3, US6, and US9 have no cross-story dependencies and can be
  staffed fully in parallel; US4 can start in parallel too, with US5/US8 following once US4/US2's
  shared files are in place.
- Within any story, all `[P]`-marked test tasks can run together, and all `[P]`-marked domain/model
  tasks can run together.

---

## Parallel Example: User Story 1

```bash
Task: "Unit test for CoinRepositoryImpl.recordDecision in app/src/test/kotlin/com/choice/app/data/CoinRepositoryImplRecordDecisionTest.kt"
Task: "Compose UI test for accept/override in app/src/androidTest/kotlin/com/choice/app/ui/coinflip/CoinFlipScreenAcceptOverrideTest.kt"
```

## Parallel Example: User Story 4

```bash
Task: "Unit test for selectWeighted distribution in app/src/test/kotlin/com/choice/app/domain/WeightedSelectionTest.kt"
Task: "Unit test for selectChoice branching in app/src/test/kotlin/com/choice/app/domain/SelectChoiceTest.kt"
Task: "Compose UI test for Coin Settings weighting in app/src/androidTest/kotlin/com/choice/app/ui/coinsettings/CoinSettingsWeightingTest.kt"
Task: "Create selectWeighted in app/src/main/kotlin/com/choice/app/domain/WeightedSelection.kt"
```

## Parallel Example: User Story 10

```bash
Task: "Unit test for encodeSharedCoin/decodeSharedCoin round-trip in app/src/test/kotlin/com/choice/app/domain/SharePayloadTest.kt"
Task: "Unit test for importSharedCoin always inserting a new coin in app/src/test/kotlin/com/choice/app/data/CoinRepositoryImplImportTest.kt"
Task: "Compose UI test for share/import in app/src/androidTest/kotlin/com/choice/app/ui/share/ImportCoinScreenTest.kt"
Task: "Create SharedCoin/SharePayload encode/decode in app/src/main/kotlin/com/choice/app/domain/SharePayload.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories).
3. Complete Phase 3: User Story 1.
4. **STOP and VALIDATE**: run quickstart.md scenario 1 in airplane mode.
5. Deploy/demo if ready — this alone delivers the product's central "Choose → Decide → Continue"
   principle on top of the existing `001` MVP.

### Incremental Delivery

1. Setup + Foundational → schema/domain foundation ready.
2. Add US1 → validate → MVP demo (accept-vs-override).
3. Add US2 → validate → Quick Coins demo.
4. Add US3 → validate → reorder demo.
5. Add US4 → validate → weighted-selection demo.
6. Add US5 → validate → avoid-last-result demo.
7. Add US6 → validate → history demo.
8. Add US7 → validate → statistics demo.
9. Add US8 → validate → search demo.
10. Add US9 → validate → templates demo.
11. Add US10 → validate → share/import demo.
12. Polish → final cross-cutting validation (Phase 13).

Each story adds value without breaking previously delivered stories, per spec.md's independent-test
criteria.

### Parallel Team Strategy

With multiple developers, after Foundational is done:

- Developer A: US1 → US6 → US7 (result flow, then history/statistics that build on it)
- Developer B: US2 → US8 (Quick Coins, then search on the same Home screen)
- Developer C: US3, then US9 (reorder, then templates — both touch `CoinEditScreen`/`ViewModel`)
- Developer D: US4 → US5 → US10 (weighting, then avoid-last-result on the same Coin Settings screen,
  then sharing)

---

## Notes

- `[P]` tasks touch different files with no unmet dependency.
- `[Story]` labels map every user-story-phase task back to spec.md's US1–US10 for traceability.
- Each user story is independently completable and testable per its "Independent Test" line above.
- Commit after each task or logical group; stop at any checkpoint to validate a story independently.
- The `v1 → v2` migration (Phase 2) is the one piece of infrastructure every later story implicitly
  relies on even when its own functional scope doesn't touch the new columns/table directly (e.g.
  the domain `Coin`/`Choice` data classes it updates are shared by every screen).
