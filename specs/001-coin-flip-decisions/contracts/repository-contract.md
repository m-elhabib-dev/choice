# Contract: CoinRepository

**Feature**: `001-coin-flip-decisions`

Choice has no external/network API (Local First, Principle III). The interface boundary that
matters here is the internal contract between the UI layer (ViewModels) and the data layer
(Room). This is the single seam the constitution's Testable Behavior principle (VI) requires to
be independently verifiable, and the seam ViewModels are built against.

## Interface: `CoinRepository`

```kotlin
interface CoinRepository {

    /** All saved coins, ordered by name, with their choices, as a live stream. */
    fun observeCoins(): Flow<List<CoinWithChoices>>

    /**
     * Coins ranked by Quick Access score (research.md §7), highest first, limited to [limit].
     * Coins with interactionCount == 0 are excluded. Recomputed on every emission from the
     * underlying coin/choice tables so scores stay current as time passes and usage occurs.
     */
    fun observeQuickAccessCoins(limit: Int = 5): Flow<List<CoinWithChoices>>

    /** A single coin with its ordered choices, or null if it no longer exists. */
    fun observeCoin(coinId: Long): Flow<CoinWithChoices?>

    /**
     * Creates or updates a coin and its full choice list in one transaction.
     *
     * @throws IllegalArgumentException if [name] is blank or exceeds 40 characters, if any
     *   choice text is blank or exceeds 60 characters, or if [choices] has fewer than 2 entries.
     * @return the persisted coin's id (existing id when updating, new id when creating).
     */
    suspend fun saveCoin(coinId: Long?, name: String, choices: List<String>): Long

    /** Deletes a coin and all its choices (FK cascade). No-op if the coin no longer exists. */
    suspend fun deleteCoin(coinId: Long)

    /**
     * Records that [coinId] was opened or flipped: increments interactionCount by 1 and sets
     * lastInteractionAt to now. Drives Quick Access ranking (research.md §7).
     */
    suspend fun recordInteraction(coinId: Long)
}
```

## Interface: Domain flip function

Pure logic, independent of persistence — this is what SC-003 (statistically even distribution) is
tested against directly, without touching Room.

```kotlin
/**
 * Selects one choice uniformly at random from [choices].
 *
 * @throws IllegalArgumentException if [choices] is empty.
 */
fun flipCoin(choices: List<Choice>, random: Random = Random.Default): Choice
```

- `random` is an injectable parameter (defaults to `Random.Default`) specifically so unit tests can
  supply a seeded `Random` for deterministic assertions, and so distribution tests can run many
  iterations against the real default source.

## Error / Validation Contract

| Condition | Behavior |
|---|---|
| Save with blank name | `saveCoin` throws `IllegalArgumentException`; ViewModel catches and surfaces a field-level error (FR-015). |
| Save with a blank choice | Same as above, attributed to the specific choice field (FR-015). |
| Save with name > 40 chars | Same as above; UI additionally blocks input past 40 chars (FR-018). |
| Save with a choice > 60 chars | Same as above; UI additionally blocks input past 60 chars (FR-018). |
| Save with < 2 choices | `saveCoin` throws `IllegalArgumentException` (FR-002); UI blocks the Save action and explains the minimum. |
| Remove a choice leaving < 2 | Blocked in the UI/ViewModel before `saveCoin` is even called (FR-006) — the in-progress edit state never reaches the repository in an invalid shape. |
| Delete a coin | Requires prior UI confirmation (FR-007); `deleteCoin` itself performs no confirmation — that responsibility belongs to the caller (ViewModel/UI). |
| Flip with 0 choices | Cannot occur in practice because `saveCoin` never persists < 2 choices; `flipCoin` throwing on empty input is a defensive invariant check, not a reachable user-facing error. |
