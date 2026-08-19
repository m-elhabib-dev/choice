# Contract: CoinRepository (extended)

**Feature**: `002-coin-decision-suite`

Extends `001-coin-flip-decisions/contracts/repository-contract.md`'s `CoinRepository` interface.
The `001` methods (`observeCoins`, `observeCoin`, `saveCoin`, `deleteCoin`, `recordInteraction`)
are unchanged in signature and behavior; `observeQuickAccessCoins` is **removed** (research.md §9)
and replaced by `observeQuickCoins` below.

## Interface: `CoinRepository` (new/changed members)

```kotlin
interface CoinRepository {

    // --- unchanged from 001: observeCoins, observeCoin, saveCoin, deleteCoin, recordInteraction ---

    /**
     * Favorited coins only, ordered by `lastInteractionAt` descending (most recently opened or
     * flipped first) — Clarifications. Replaces 001's score-based `observeQuickAccessCoins`.
     */
    fun observeQuickCoins(): Flow<List<CoinWithChoices>>

    /** Toggles a coin's favorite flag. No-op if the coin no longer exists. */
    suspend fun setFavorite(coinId: Long, isFavorite: Boolean)

    /** Enables/disables weighted selection for a coin, independent of stored per-choice weights. */
    suspend fun setWeightedEnabled(coinId: Long, enabled: Boolean)

    /** Enables/disables "avoid last result" for a coin. */
    suspend fun setAvoidLastResultEnabled(coinId: Long, enabled: Boolean)

    /**
     * Persists an ordered list of (choiceId, weight) pairs for a coin's choices in one
     * transaction.
     *
     * @throws IllegalArgumentException if any [weight] is present and not a positive whole number
     *   (FR-014).
     */
    suspend fun updateChoiceWeights(coinId: Long, weights: List<Pair<Long, Int?>>)

    /**
     * Persists a new choice order for a coin (a full list of that coin's choice IDs in their new
     * order) in one transaction. Used by the up/down reorder controls (research.md §2).
     */
    suspend fun reorderChoices(coinId: Long, orderedChoiceIds: List<Long>)

    /**
     * Records an accepted decision: inserts one `Decision` row for [coinId]/[choiceId] with
     * [choiceTextSnapshot] and the current time. Called only from the result screen's accept
     * action (FR-010) — never for an overridden/rejected flip.
     */
    suspend fun recordDecision(coinId: Long, choiceId: Long, choiceTextSnapshot: String)

    /** A coin's decision history, newest first (FR-021). Empty if the coin has never been flipped-and-accepted. */
    fun observeDecisionHistory(coinId: Long): Flow<List<Decision>>

    /**
     * The most recently recorded decision for a coin, or `null` if none exists yet. Used to
     * resolve `lastChoiceId` for `selectChoice`'s avoid-last-result filtering (research.md §4) —
     * not a separate stored field.
     */
    suspend fun getLastDecision(coinId: Long): Decision?

    /** Creates a new, independent coin from [payload] (import), reusing the same validation as `saveCoin`. */
    suspend fun importSharedCoin(payload: SharedCoin): Long
}
```

## Contract notes

- `observeQuickCoins()` and `observeCoins()` (unchanged, alphabetical) together fully answer
  FR-004/FR-005/FR-006 — the Home ViewModel needs no additional sorting/filtering logic beyond
  applying `matchesSearchQuery` (contracts/domain-api.md) on top of whichever of these two flows is
  currently visible.
- `updateChoiceWeights` and `reorderChoices` are separate methods (rather than folded into
  `saveCoin`) because they're invoked from Coin Settings and Coin Edit respectively — two different
  screens with two different, independently testable concerns, matching FR-001 and FR-013 being
  distinct requirements.
- `recordDecision` takes `choiceTextSnapshot` as an explicit parameter (rather than re-reading the
  live `Choice` row inside the repository) so the snapshot is unambiguously "the text as displayed
  to the user at accept time," matching data-model.md's `Decision.choiceTextSnapshot` definition.
- `importSharedCoin` always inserts a new coin — it performs no lookup against existing coins
  first, per FR-040.

## Error / Validation Contract (additions to `001`'s table)

| Condition | Behavior |
|---|---|
| `updateChoiceWeights` given a non-positive/non-whole weight | Throws `IllegalArgumentException` (FR-014); UI blocks Save and explains before this is ever called. |
| `setFavorite`/`setWeightedEnabled`/`setAvoidLastResultEnabled` on a deleted coin | No-op — consistent with `001`'s `deleteCoin` no-op behavior for an already-gone coin. |
| `recordDecision` for a coin/choice pair where the choice no longer exists | Cannot occur in practice — `recordDecision` is only ever called immediately after a `selectChoice` result computed from the coin's *current* choice list within the same flip. |
| `importSharedCoin` given a payload that fails `decodeSharedCoin`'s validation | Never reached — decoding happens before `importSharedCoin` is called; the Import ViewModel surfaces the decode error directly (FR-039). |
| `importSharedCoin` given a valid payload matching an existing coin's name/choices | Succeeds normally, inserting a new, independent coin (FR-040) — no uniqueness check performed. |
