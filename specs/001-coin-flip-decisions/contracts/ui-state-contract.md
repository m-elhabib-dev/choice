# Contract: Screen UI States

**Feature**: `001-coin-flip-decisions`

Each screen's `ViewModel` exposes a single immutable UI state via `StateFlow`, consumed by a
stateless Composable. This is the contract between ViewModels and Composables — it is what
Compose UI tests assert against and what keeps decision/data logic (Principle VI) out of the UI
layer.

## MainScreen

```kotlin
data class MainScreenUiState(
    val quickAccessCoins: List<CoinSummary> = emptyList(),
    val allCoins: List<CoinSummary> = emptyList(),
    val isEmpty: Boolean = false, // true when allCoins is empty -> show first-run empty state
)

data class CoinSummary(
    val id: Long,
    val name: String,
    val choiceCount: Int,
)
```

- Maps to Acceptance Scenario US1.3 (Quick Access section) and the Edge Case "first time the app
  is opened" (empty state).
- `quickAccessCoins` is always a subset of `allCoins`, ordered by score (highest first, ≤5 entries,
  research.md §7); `allCoins` is ordered by name.

## CoinFlipScreen (opening/flipping a coin)

```kotlin
data class CoinFlipUiState(
    val coinId: Long,
    val coinName: String,
    val choices: List<String>,
    val result: String? = null,   // null until the user flips; cleared on navigating away/back
    val canRemoveChoice: Boolean, // false when choices.size == 2 (FR-006 guard, edit entry point)
)
```

- `result == null` renders the pre-flip state (flip control visible, no outcome shown).
- `result != null` renders the outcome per FR-012/FR-013: prominent result display, and the
  primary action is NOT "flip again" — re-flipping requires leaving this result state via a
  deliberate, separate action (FR-013, SC-007), per Constitution Principle II (Decision First).
- Reconstructed fresh (result = null) every time the screen is (re)entered, satisfying the
  "Reset on restart" clarification and the Decision Outcome entity's transient nature.

## CoinEditScreen (create or edit)

```kotlin
data class CoinEditUiState(
    val coinId: Long?,            // null when creating a new coin
    val name: String,
    val nameError: String? = null,
    val choices: List<ChoiceFieldState>,
    val formError: String? = null, // e.g. "At least 2 choices are required"
    val canSave: Boolean,          // derived: name non-blank/within limit, all choices valid, >=2 choices
)

data class ChoiceFieldState(
    val text: String,
    val error: String? = null,
)
```

- Directly implements Acceptance Scenarios US2.2/US2.3 and US3.1–US3.3: field-level errors for
  blank name/choice (FR-015), a form-level error when attempting to drop below 2 choices (FR-006,
  FR-002), and `canSave` gating the Save action so invalid states are blocked before they ever
  reach `CoinRepository.saveCoin` (see repository-contract.md).
- Leaving this screen without saving discards all state (per spec Assumptions) — there is no
  autosave or draft persistence.

## DeleteCoinConfirmation

```kotlin
data class DeleteCoinConfirmationState(
    val coinId: Long,
    val coinName: String,
)
```

- A modal confirmation (dialog) driven by this minimal state; confirming invokes
  `CoinRepository.deleteCoin(coinId)`, cancelling dismisses with no side effects — implements
  Acceptance Scenarios US4.1/US4.2 and FR-007's confirmation requirement.
