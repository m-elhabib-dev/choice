# Contract: Domain decision/data functions

**Feature**: `002-coin-decision-suite`

Choice has no external/network API (Local First, Principle III). As in `001`, the seam that
matters is between the UI/ViewModel layer and plain-Kotlin `domain/` logic — this is what
Principle VI (Testable Behavior) requires to be independently unit-testable, and what
`001-coin-flip-decisions/contracts/repository-contract.md`'s `flipCoin` already established. This
document covers only the functions added for this feature.

## Selection

```kotlin
/**
 * Selects one choice for [choices], honoring [weightedEnabled] and [avoidLastResultEnabled].
 *
 * Order of operations:
 * 1. If [avoidLastResultEnabled] and [lastChoiceId] is non-null, [choices] is filtered via
 *    [applyAvoidLastResult] first.
 * 2. The (possibly filtered) pool is then selected from: [selectWeighted] if [weightedEnabled],
 *    otherwise the existing uniform [flipCoin].
 *
 * @throws IllegalArgumentException if [choices] is empty.
 */
fun selectChoice(
    choices: List<Choice>,
    weightedEnabled: Boolean,
    avoidLastResultEnabled: Boolean,
    lastChoiceId: Long?,
    random: Random = Random.Default,
): Choice

/**
 * Removes the choice matching [lastChoiceId] from [choices] — unless doing so would leave the
 * result empty, in which case the original, unfiltered [choices] is returned instead (FR-018).
 * A null [lastChoiceId], or one that matches no choice in [choices], returns [choices] unchanged.
 */
fun applyAvoidLastResult(choices: List<Choice>, lastChoiceId: Long?): List<Choice>

/**
 * Picks one choice from [choices] with probability proportional to each choice's `weight`
 * (a null/absent weight defaults to the baseline `1` — spec Assumptions), using a cumulative-sum
 * draw (research.md §3).
 *
 * @throws IllegalArgumentException if [choices] is empty.
 */
fun selectWeighted(choices: List<Choice>, random: Random = Random.Default): Choice
```

- `flipCoin` (uniform pick) is unchanged from `001-coin-flip-decisions/contracts/repository-contract.md`
  and remains the selector used when `weightedEnabled == false`.
- `random` stays an injectable parameter on every function here, for the same reason as `001`:
  deterministic unit tests and unseeded statistical distribution tests (SC-002, SC-003).

## Statistics

```kotlin
data class CoinStatistics(
    val totalDecisions: Int,
    val perChoiceCounts: List<ChoiceCount>, // one entry per current choice, in choice order
    val mostFrequent: ChoiceCount?,          // null when totalDecisions == 0
    val leastFrequent: ChoiceCount?,         // null when totalDecisions == 0
    val mostRecentDecision: Decision?,       // null when totalDecisions == 0
)

data class ChoiceCount(val choice: Choice, val count: Int)

/**
 * Folds [decisions] (a coin's full decision history) against its current [choices] into
 * [CoinStatistics]. Decisions are matched to [choices] by `choiceTextSnapshot`, so past decisions
 * are still counted correctly even after choices are edited, reordered, or removed (FR-026).
 * When two or more choices tie for most/least frequent, the tie is broken by whichever tied
 * choice appears first in [choices] (its current `position`) — Clarifications.
 */
fun computeStatistics(choices: List<Choice>, decisions: List<Decision>): CoinStatistics
```

- An empty `decisions` list yields `totalDecisions == 0` and `null` for every "most/least/recent"
  field — the ViewModel maps this directly to the empty-state UI (FR-027), no separate empty-check
  needed at the call site.

## Search

```kotlin
/**
 * True if [query] is blank (matches everything) or [coinName] contains [query] as a
 * case-insensitive substring (FR-028–FR-031).
 */
fun matchesSearchQuery(coinName: String, query: String): Boolean
```

- Applied by the Home ViewModel as `coins.filter { matchesSearchQuery(it.name, query) }` against
  the already-observed coin list — no separate DAO query (research.md — carried from the "single
  `CoinRepository` seam" decision).

## Sharing

```kotlin
data class SharedCoin(
    val schemaVersion: Int,
    val name: String,
    val choices: List<SharedChoice>,
    val weightedEnabled: Boolean,
    val avoidLastResultEnabled: Boolean,
)

data class SharedChoice(val text: String, val weight: Int?)

const val CURRENT_SHARE_SCHEMA_VERSION = 1

/** Serializes [coin] to the versioned JSON string used as share-intent `EXTRA_TEXT`. */
fun encodeSharedCoin(coin: SharedCoin): String

/**
 * Parses [json] into a [SharedCoin].
 *
 * @throws InvalidSharePayloadException if [json] is not valid JSON, is missing a required field,
 *   has a `schemaVersion` other than a version this app understands, has fewer than 2 choices, or
 *   has any blank name/choice text or non-positive weight (FR-039, spec Edge Cases).
 */
fun decodeSharedCoin(json: String): SharedCoin

class InvalidSharePayloadException(message: String) : Exception(message)
```

- See contracts/share-payload-contract.md for the exact JSON shape and the full validation table.
- `decodeSharedCoin` never partially constructs a `SharedCoin` on invalid input — it throws before
  returning anything, so the caller (Import screen ViewModel) can never persist a broken coin
  (FR-039, SC-009).

## Error / Validation Contract (additions to `001`'s table)

| Condition | Behavior |
|---|---|
| `selectChoice`/`selectWeighted`/`flipCoin` called with 0 choices | Throws `IllegalArgumentException` — defensive invariant, unreachable in practice since a coin can never be saved with fewer than 2 choices. |
| Avoid-last-result would exclude every remaining choice | `applyAvoidLastResult` returns the full, unfiltered list instead (FR-018) — `selectChoice` never receives an empty pool because of this filter. |
| Weight entered as blank/zero/negative/non-numeric/non-whole | Rejected before it reaches `Choice.weight` — UI blocks save and explains a positive whole number is required (FR-014). |
| `decodeSharedCoin` given malformed/incomplete/wrong-version JSON | Throws `InvalidSharePayloadException`; Import screen shows a clear message and creates nothing (FR-039). |
| `decodeSharedCoin` given a payload with < 2 choices | Same as above — a `SharedCoin` can never violate the coin's own minimum-choices rule. |
