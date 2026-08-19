# Implementation Plan: Coin Decision Suite

**Branch**: `002-coin-decision-suite` | **Date**: 2026-08-19 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-coin-decision-suite/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Extend the existing "Choice" Android app (built in `001-coin-flip-decisions`) with ten additive
capabilities on top of its current create/edit/delete/flip MVP: reorderable choices, manual
favoriting with a recency-ordered "Quick Coins" section (replacing the prior automatic
recency+frequency score), optional per-coin weighted selection, optional per-coin "avoid last
result," local decision history, per-coin statistics, local name search, four bundled coin
templates, and versioned native share/import. Technical approach (full detail in
[research.md](./research.md)): the existing MVVM + Room + Compose/Material 3 architecture is
extended rather than replaced — a Room migration (`v1 → v2`) adds favorite/weighting/avoid-last
columns and a new `decisions` table; a small set of new pure `domain` functions (weighted
selection, avoid-last filtering, statistics, search matching, share-payload encode/decode) keep
all new decision/data logic independently unit-testable per Principle VI; sharing uses Android's
native `ACTION_SEND` share sheet plus an in-app paste-to-import fallback, with the payload encoded
via the already-bundled `org.json` (no new dependency).

## Technical Context

**Language/Version**: Kotlin 2.0+, JVM target 17 (unchanged from `001-coin-flip-decisions`)

**Primary Dependencies**: Jetpack Compose (Material 3), Room, Navigation-Compose, Kotlin
Coroutines/Flow, Lifecycle-ViewModel-Compose — all already present from `001`; no new dependencies
added (research.md §1, §6)

**Storage**: Room (SQLite), on-device only — extends the existing `coins`/`choices` tables with new
columns and adds a `decisions` table via an explicit, non-destructive migration (`v1 → v2`,
data-model.md §Migration); no server, no sync

**Testing**: JUnit4 + `kotlinx-coroutines-test` + Room in-memory database and `MigrationTestHelper`
(unit tests for the new domain functions, repository operations, and the schema migration);
`androidx.compose.ui.test.junit4` instrumented tests for the new/changed screens — same toolchain
as `001`, no new test frameworks

**Target Platform**: Android, `minSdk` 26 (Android 8.0+), `targetSdk`/`compileSdk` 35 (unchanged)

**Project Type**: Mobile app — single Android application module (unchanged)

**Performance Goals**: 60fps UI/animations (unchanged); weighted/avoid-last selection adds only
O(n) work over a coin's choice list (n realistically < 100) with no perceptible delay; search
filtering updates the visible list with no perceptible delay per keystroke, computed in-memory
against the already-observed coin list

**Constraints**: Fully offline — no network permission requested, no core flow (including sharing
and import) may require connectivity (research.md §7); local storage only; existing character
limits (coin name ≤40, choice text ≤60) unchanged and applied consistently to template- and
import-sourced values (spec Assumptions)

**Scale/Scope**: Single local user, no accounts (unchanged); realistically tens of coins, no hard
cap; unbounded choices per coin; realistically up to a few thousand decision records per coin over
the app's lifetime, computed in-memory for statistics with no pagination/aggregation SQL needed at
this scale (research.md §8); ~7 new/changed screens (Home w/ Quick Coins + search, Coin Flip/Result
w/ accept-override, Create/Edit Coin w/ reorder, Coin Settings, History, Statistics, Import Coin) on
top of `001`'s existing screens

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
|---|---|---|
| I. Simplicity First | Weighting and avoid-last-result stay opt-in per coin and out of the default create/coin flow (FR-012); reorder uses simple up/down controls, not a custom drag gesture (research.md §2); sharing reuses the OS share sheet plus a plain paste-to-import screen instead of a custom transport | ✅ PASS |
| II. Decision First | User Story 1 (highest priority) keeps the accept action primary and the override action secondary and explicit; decisions are recorded to history only on accept (FR-010) | ✅ PASS |
| III. Local First | No network permission added; sharing/import both work entirely via local intents and local text, validated in airplane mode (SC-006 carryover, research.md §7) | ✅ PASS |
| IV. Modern Android | Continues Compose/Material 3, Jetpack Navigation, Room with an explicit typed `Migration`, no deprecated APIs | ✅ PASS |
| V. Consistent Design | All new screens (Coin Settings, History, Statistics, Import) consume the existing single `ChoiceTheme`; no new colors/styling introduced | ✅ PASS |
| VI. Testable Behavior | Weighted selection, avoid-last-result, statistics, search matching, and share encode/decode are all plain Kotlin functions in `domain/`, unit-tested independent of UI/Room (contracts/domain-api.md) | ✅ PASS |
| VII. Minimal Dependencies | No new dependency added — sharing uses `org.json` (already part of the Android SDK) and `android.content.Intent` (research.md §6); reorder avoids a third-party drag-and-drop library (research.md §2) | ✅ PASS |
| VIII. Incremental Development | Ten user stories, independently prioritized/testable (P1–P10 in spec.md), each shippable on its own on top of the existing `001` MVP | ✅ PASS |

No violations. Complexity Tracking is not needed.

**Post-Phase-1 re-check**: data-model.md, contracts/, and quickstart.md were reviewed against the
same table above — the migration, new domain functions, and new screens all fit within the
approach already accounted for here; no new dependency, pattern, or screen was introduced during
design that wasn't already anticipated. All gates remain ✅ PASS.

## Project Structure

### Documentation (this feature)

```text
specs/002-coin-decision-suite/
├── plan.md                        # This file (/speckit-plan command output)
├── research.md                    # Phase 0 output (/speckit-plan command)
├── data-model.md                  # Phase 1 output (/speckit-plan command)
├── quickstart.md                  # Phase 1 output (/speckit-plan command)
├── contracts/                     # Phase 1 output (/speckit-plan command)
│   ├── domain-api.md
│   ├── repository-contract.md
│   └── share-payload-contract.md
└── tasks.md                       # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
app/
├── src/
│   ├── main/
│   │   ├── kotlin/com/choice/app/
│   │   │   ├── ChoiceApplication.kt        # unchanged shape; AppContainer gains no new deps
│   │   │   ├── MainActivity.kt             # NavHost gains new routes; handles incoming ACTION_SEND
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── ChoiceDatabase.kt   # version 2 + Migration(1, 2)
│   │   │   │   │   ├── CoinEntity.kt       # + isFavorite, weightedEnabled, avoidLastResultEnabled
│   │   │   │   │   ├── ChoiceEntity.kt     # + weight: Int?
│   │   │   │   │   ├── DecisionEntity.kt   # new: history rows
│   │   │   │   │   └── CoinDao.kt          # + favorite/weight/avoid-last/decision queries
│   │   │   │   └── CoinRepositoryImpl.kt   # implements the extended contracts/repository-contract.md
│   │   │   ├── domain/
│   │   │   │   ├── Coin.kt                 # + isFavorite/weightedEnabled/avoidLastResultEnabled/weight
│   │   │   │   ├── Decision.kt             # new: coinId, choiceId?, choiceText, decidedAt
│   │   │   │   ├── FlipCoin.kt             # unchanged: uniform pick, used when weighting is off
│   │   │   │   ├── WeightedSelection.kt    # new: weighted pick (research.md §3)
│   │   │   │   ├── AvoidLastResult.kt      # new: exclude-with-fallback filter (research.md §4)
│   │   │   │   ├── CoinStatistics.kt       # new: total/per-choice/most-least-frequent/most-recent
│   │   │   │   ├── CoinSearch.kt           # new: local name-match filter
│   │   │   │   ├── CoinTemplates.kt        # new: bundled Breakfast/Lunch/Workout/Movie templates
│   │   │   │   ├── SharePayload.kt         # new: versioned encode/decode (research.md §6)
│   │   │   │   └── QuickAccessScore.kt     # REMOVED — superseded by favorite+recency ordering
│   │   │   └── ui/
│   │   │       ├── theme/                  # unchanged Catppuccin Mocha theme
│   │   │       ├── main/                   # MainScreen/MainViewModel: + Quick Coins, + search
│   │   │       ├── coinflip/               # CoinFlipScreen/ViewModel: + accept/override, weighting/avoid-last
│   │   │       ├── coinedit/               # CoinEditScreen/ViewModel: + up/down choice reorder
│   │   │       ├── coinsettings/           # new: favorite, weighting + per-choice weights, avoid-last toggles
│   │   │       ├── history/                # new: HistoryScreen/ViewModel
│   │   │       ├── statistics/             # new: StatisticsScreen/ViewModel
│   │   │       ├── templates/              # new: template picker (from the "create coin" entry point)
│   │   │       ├── share/                  # new: ImportCoinScreen/ViewModel (paste/receive text)
│   │   │       └── components/             # shared composables (existing + new confirmation/empty states)
│   │   └── AndroidManifest.xml              # + ACTION_SEND (text/plain) intent-filter on MainActivity
│   ├── test/kotlin/com/choice/app/          # unit tests (data/, domain/) — extended
│   └── androidTest/kotlin/com/choice/app/   # Compose UI tests (ui/) — extended
└── build.gradle.kts                          # unchanged (no new dependencies)
```

**Structure Decision**: Continue the single Android application module (`app/`) established in
`001-coin-flip-decisions` — this feature is additive to the same app, not a new product, so no new
module/package-by-feature split is warranted (Principle I). The existing `data`/`domain`/`ui`
package-by-layer structure is preserved and extended: all new decision/data logic (weighting,
avoid-last-result, statistics, search, share encode/decode) lives in `domain/` as plain Kotlin,
independently testable per Principle VI and contracts/domain-api.md; all persistence changes are
isolated to `data/local/` behind the existing single `CoinRepository` seam
(contracts/repository-contract.md).

## Complexity Tracking

No Constitution Check violations were found. This section is intentionally empty.
