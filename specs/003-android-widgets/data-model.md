# Phase 1 Data Model: Android Home-Screen Widgets

This feature introduces no new durable app entities and no changes to the Room schema. It adds two
small, per-OS-widget-instance configuration records, stored outside Room via Glance's
`PreferencesGlanceStateDefinition` (see `research.md` §2), plus in-memory-only coordination state.
All decision-relevant data (coins, choices, decisions) continues to live exclusively in the
existing Room database, read/written exclusively through the existing `CoinRepository`.

## Existing entities reused unchanged

| Entity | Source | Used by widgets for |
|---|---|---|
| `Coin` (`domain/Coin.kt`) | Room, via `CoinRepository` | name, favorite flag, weighted/avoid-last-result flags |
| `Choice` (`domain/Coin.kt`) | Room, via `CoinRepository` | option text + optional weight, at decision time |
| `CoinWithChoices` (`domain/Coin.kt`) | `CoinRepository.observeCoin`/`observeCoins`/`observeQuickCoins` | full resolved state for a widget's configured coin(s) |
| `Decision` (`domain/Decision.kt`) | Room, via `CoinRepository.getLastDecision`/`recordDecision` | most-recent result display; write target for widget flips |

No widget code reads or writes these tables directly — all access goes through `CoinRepository`,
per FR-018.

## New per-widget-instance configuration

### `SingleCoinWidgetConfig`

One record per Single Coin Widget App Widget ID (`GlanceId`).

| Field | Type | Notes |
|---|---|---|
| `coinId` | `Long?` | The one coin this widget instance represents. `null`/absent = unconfigured (should not normally persist past the configuration Activity's `setResult(RESULT_OK)`). |

**Validation / resolution rules**:
- At render time, `coinId` is resolved against `CoinRepository.observeCoin(coinId)`.
  - Resolves to `null` (coin deleted, or ID never valid) → widget renders the **Unavailable** state
    (FR-012).
  - Resolves, but `choices.size < 2` → widget renders the **Too Few Choices** state (FR-014).
  - Resolves with ≥ 2 choices → widget renders the **Ready** or **Result** state depending on
    whether a decision has been made since configuration (see State Transitions below).
- Configuration is written exactly once per instance at setup time, and again only if the user
  explicitly reconfigures (FR-015 recovery path); it is never silently rewritten by a refresh.

### `QuickCoinsWidgetConfig`

One record per Quick Coins Widget App Widget ID (`GlanceId`).

| Field | Type | Notes |
|---|---|---|
| `useFavorites` | `Boolean` | `true` (default) = coin list is always the live favorites set. `false` = use `explicitCoinIds`. |
| `explicitCoinIds` | `List<Long>` | Only meaningful when `useFavorites == false`. An ordered, user-chosen set of coin IDs. |

**Validation / resolution rules**:
- `useFavorites == true` → resolve against `CoinRepository.observeQuickCoins()` (existing
  favorites query) every render (FR-002, US4 AS4).
- `useFavorites == false` → resolve each ID in `explicitCoinIds` against `CoinRepository
  .observeCoins()`; IDs that no longer resolve (coin deleted) are dropped from display, not shown
  as broken rows — the remaining valid coins still render normally (US4 AS3 applies per-coin, not
  to the whole widget, since a Quick Coins Widget represents a *set*, not a single required coin).
- If the resolved list (favorites or explicit) is empty → widget renders the **No Coins Selected**
  state (US1 AS5 equivalent for this widget type / FR-002).
- Display count adapts to available Glance size (`LocalSize`) — this is a rendering concern, not a
  stored field; nothing about "how many fit" is persisted.

## Derived render states (both widget types)

These are not stored — they are computed at each composition from the resolved coin(s) and the
existing `CoinRepository` data:

| State | Condition | Shown as |
|---|---|---|
| Unconfigured | Config record missing/never completed | Should not be reachable post-setup; treated identically to Unavailable if seen. |
| Unavailable | Configured `coinId` no longer resolves (Single), or every explicit ID no longer resolves and `useFavorites == false` with an empty resulting set (Quick) | Clear "unavailable" message + reconfigure / open-app actions (FR-012, US4 AS3) |
| No Coins Selected | Quick Coins Widget: resolved set (favorites or explicit) is empty | Message + action to pick coins / open app (US1 AS5) |
| Too Few Choices | Coin resolves but has < 2 choices | Explanatory "can't decide yet" message, no flip control offered (FR-014, edge case) |
| Ready | Coin resolves, ≥ 2 choices, no decision recorded via this widget path yet this "session" (i.e. `getLastDecision` is null, or simply always show name + primary flip control when no result is being displayed) | Coin name + primary flip action |
| Result | A decision exists for the coin (from `getLastDecision`, whether made via widget or app) | Coin name + most recent choice text as settled result + visually secondary flip control (FR-008/FR-009) |

Note: "Ready" vs. "Result" is derived from `CoinRepository.getLastDecision(coinId)` at render
time — exactly the same source of truth the in-app coin screen would show as its last recorded
decision — so a widget freshly configured for a coin that already has history immediately shows
that history's latest entry as its Result state (spec's US1 AS2 concerns a *freshly created* coin
with no history yet, which is consistent: `getLastDecision` returns `null`, hence Ready).

## In-memory-only coordination state (not persisted)

| State | Scope | Purpose |
|---|---|---|
| Per-`GlanceId` flip mutex | Process-lifetime singleton map | Guarantees at most one in-flight select+record per widget instance at a time (research.md §6). Reset for free on process death — never persisted, never leaks a stuck "busy" state. |

## State Transitions

```text
[Add widget to home screen]
        │
        ▼
[Configuration Activity] ──(no saved coins)──▶ [Show "create a coin first" + open-app action]
        │ (user picks coin/coins, or accepts default favorites)
        ▼
[Config record written] ──▶ Ready / No-Coins-Selected / Too-Few-Choices (per resolution rules)
        │ (user taps flip)
        ▼
[select via selectChoice()] → [recordDecision() + recordInteraction()] → Result state
        │ (user taps flip again — explicit override)
        ▼
[select again] → [record again] → Result state (new result replaces prior)

At any point, independent of widget interaction:
[Coin renamed/edited/favorited/deleted in app] → CoinRepository Flow emits
        → WidgetRefreshCoordinator calls updateAll()
        → every affected widget instance re-resolves its config → new state as above
```
