# Implementation Plan: Coin Flip Decisions

**Branch**: `001-coin-flip-decisions` | **Date**: 2026-08-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-coin-flip-decisions/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Build "Choice," a native Android app (Kotlin + Jetpack Compose) that lets a user create reusable
"coins" — a name plus 2+ choices — and flip one to get a single, clearly-displayed, final decision.
Coins and choices persist locally via Room with no network dependency; the main screen surfaces a
dedicated Quick Access section ranked by a recency-weighted usage score, and the flip result view
deliberately avoids offering a low-friction re-flip, per the "Decision First" principle. Technical
approach (full detail in [research.md](./research.md)): MVVM architecture, Compose/Material 3 UI
themed with a single shared Catppuccin Mocha token layer, manual (non-framework) dependency
injection, and unit + Compose instrumented tests covering decision logic and the four user-story
flows.

## Technical Context

**Language/Version**: Kotlin 2.0+, JVM target 17

**Primary Dependencies**: Jetpack Compose (Material 3), Room, Navigation-Compose, Kotlin
Coroutines/Flow, Lifecycle-ViewModel-Compose — all AndroidX/Jetpack, no third-party libraries
(see research.md §2–§5)

**Storage**: Room (SQLite), on-device only — two tables (`coins`, `choices`); no server, no sync

**Testing**: JUnit4 + `kotlinx-coroutines-test` + Room in-memory database (unit tests for
repository, validation, flip distribution, Quick Access ranking); `androidx.compose.ui.test.junit4`
instrumented tests for the four user-story UI flows (research.md §9)

**Target Platform**: Android, `minSdk` 26 (Android 8.0+), `targetSdk`/`compileSdk` 35

**Project Type**: Mobile app — single Android application module

**Performance Goals**: 60fps UI/animations; flip outcome computed and displayed with no
perceptible delay before the flip animation completes; cold start to interactive main screen well
under 1s on a mid-range device

**Constraints**: Fully offline — no network permission requested, no core flow may require
connectivity (validated in airplane mode, SC-006); local storage only; coin name ≤40 chars, choice
text ≤60 chars (FR-018, research.md §8)

**Scale/Scope**: Single local user, no accounts; realistically tens of saved coins with no hard
cap; unbounded choices per coin (UI scrolls, no cap); ~4-5 screens (Main w/ Quick Access, Coin
Flip/Result, Create/Edit Coin, Delete confirmation dialog)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
|---|---|---|
| I. Simplicity First | Every screen has one clear purpose; no framework/pattern adopted beyond what ~4-5 screens need (e.g., manual DI over Hilt, MVVM over MVI) | ✅ PASS |
| II. Decision First | Flip result view has no low-friction re-flip; re-flipping requires a deliberate, separate navigation action (FR-013, SC-007) | ✅ PASS |
| III. Local First | Room-only persistence, no network permission, all core flows validated in airplane mode | ✅ PASS |
| IV. Modern Android | Compose/Material 3, current Jetpack architecture (MVVM + StateFlow), `minSdk` 26 / `targetSdk` 35, no deprecated APIs planned | ✅ PASS |
| V. Consistent Design | Single `ChoiceTheme` (Catppuccin Mocha `ColorScheme`) applied app-wide; no screen defines its own literal colors | ✅ PASS |
| VI. Testable Behavior | `CoinRepository` and `flipCoin` are plain Kotlin/coroutines, unit-tested independent of UI (research.md §9, contracts/repository-contract.md) | ✅ PASS |
| VII. Minimal Dependencies | Only AndroidX/Jetpack libraries; DI framework (Hilt) explicitly rejected as unjustified at this scale (research.md §4) | ✅ PASS |
| VIII. Incremental Development | User stories are independently prioritized/testable (P1–P4 in spec.md); no out-of-scope features (accounts, sync, ads, history log) included | ✅ PASS |

No violations. Complexity Tracking is not needed.

**Post-Phase-1 re-check**: data-model.md, contracts/, and quickstart.md were reviewed against the
same table above — no new dependencies, screens, or patterns were introduced during design that
weren't already accounted for here. All gates remain ✅ PASS.

## Project Structure

### Documentation (this feature)

```text
specs/001-coin-flip-decisions/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md         # Phase 1 output (/speckit-plan command)
├── quickstart.md         # Phase 1 output (/speckit-plan command)
├── contracts/            # Phase 1 output (/speckit-plan command)
│   ├── repository-contract.md
│   └── ui-state-contract.md
└── tasks.md              # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
app/
├── src/
│   ├── main/
│   │   ├── kotlin/com/choice/app/
│   │   │   ├── ChoiceApplication.kt      # Application class, owns AppContainer (manual DI)
│   │   │   ├── MainActivity.kt           # Hosts ChoiceTheme + NavHost
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── ChoiceDatabase.kt # Room database
│   │   │   │   │   ├── CoinEntity.kt
│   │   │   │   │   ├── ChoiceEntity.kt
│   │   │   │   │   └── CoinDao.kt
│   │   │   │   └── CoinRepositoryImpl.kt # implements contracts/repository-contract.md
│   │   │   ├── domain/
│   │   │   │   ├── Coin.kt               # domain model (CoinWithChoices, CoinSummary)
│   │   │   │   ├── FlipCoin.kt           # pure random-selection function
│   │   │   │   └── QuickAccessScore.kt   # recency+frequency scoring (research.md §7)
│   │   │   └── ui/
│   │   │       ├── theme/                # Catppuccin Mocha Color.kt, Theme.kt, Type.kt
│   │   │       ├── main/                 # MainScreen, MainViewModel
│   │   │       ├── coinflip/             # CoinFlipScreen, CoinFlipViewModel
│   │   │       ├── coinedit/             # CoinEditScreen, CoinEditViewModel
│   │   │       └── components/           # shared composables (e.g. delete confirmation dialog)
│   │   └── AndroidManifest.xml            # no network permissions declared
│   ├── test/kotlin/com/choice/app/        # unit tests (data/, domain/)
│   └── androidTest/kotlin/com/choice/app/ # Compose UI tests (ui/)
└── build.gradle.kts
```

**Structure Decision**: Single Android application module (`app/`) — no `backend/`/`frontend/`
split (there is no backend) and no multi-module split (Scale/Scope above does not warrant one;
Principle I). Standard Jetpack package-by-layer structure (`data`/`domain`/`ui`) keeps the
persistence and decision logic in `data`/`domain` independently testable from `ui`, per
Principle VI and contracts/repository-contract.md.

## Complexity Tracking

No Constitution Check violations were found. This section is intentionally empty.
