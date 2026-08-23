# Contract: Widget Action Callbacks & App Deep Link

## Glance `ActionCallback`s

All widget-surface interactivity is implemented as Glance `ActionCallback`s, each invoked by the
system with `(context: Context, glanceId: GlanceId, parameters: ActionParameters)`.

### `FlipSingleCoinAction`

- **Parameters**: none beyond `glanceId` (the widget instance already identifies its one coin via
  its own stored `SingleCoinWidgetConfig`).
- **Behavior**:
  1. Acquire this `glanceId`'s flip mutex (research.md §6); if already held, return immediately
     (dropped tap — at most one in-flight decision per instance).
  2. Resolve `coinId` from config; resolve `CoinWithChoices` via `CoinRepository.observeCoin`.
     - Not resolvable → do nothing but trigger a re-render (`update`) so the widget shows/confirms
       the Unavailable state.
     - Resolvable, `choices.size < 2` → re-render only (Too Few Choices state); no selection
       attempted.
  3. Otherwise: `lastDecision = getLastDecision(coinId)`; `choice = selectChoice(choices,
     coin.weightedEnabled, coin.avoidLastResultEnabled, lastDecision?.choiceId)`;
     `recordDecision(coinId, choice.id, choice.text)`; `recordInteraction(coinId)`.
  4. Release the mutex; update this widget instance (`SingleCoinWidget().update(context,
     glanceId)`); the new render reads the just-recorded decision as its Result state.
- **Postconditions**: Exactly one new row in `decisions` (Room) on success; zero on any early
  return above.

### `FlipQuickCoinAction`

- **Parameters**: `coinId: Long` (`ActionParameters.Key<Long>`) — which row's flip was tapped.
- **Behavior**: Identical to `FlipSingleCoinAction` steps 1–4, keyed by `(glanceId, coinId)` for the
  mutex (so flipping two different coins in the same Quick Coins Widget instance concurrently is
  allowed; flipping the same coin twice rapidly is not) — per the edge case "tapping one coin's
  flip action ... only that coin's decision updates; the other listed coins remain unchanged."
- **Postconditions**: Same as above, scoped to the tapped coin only; other rows in the same widget
  instance are untouched.

### `ReconfigureAction`

- **Parameters**: none beyond `glanceId`.
- **Behavior**: Starts the owning config Activity per `widget-configuration-contract.md` §2.

### `OpenCoinInAppAction`

- **Parameters**: `coinId: Long?` (`null`/absent = fallback).
- **Behavior**: Starts `MainActivity` with an explicit `Intent` extra `EXTRA_OPEN_COIN_ID: Long`
  set to `coinId` when non-null and non-`null`-resolvable; omitted (or unresolvable) → no extra, so
  `MainActivity` falls back to its default `main` (coin list) destination.
- **App-side contract** (`MainActivity`): on launch, if `intent.hasExtra(EXTRA_OPEN_COIN_ID)`, the
  existing `LaunchedEffect`-based startup navigation (already used today for the `ACTION_SEND`
  import case) instead navigates to `coinflip/{coinId}`. If that coin no longer resolves by the
  time `MainActivity` loads it (deleted between widget render and tap), the existing `coinflip`
  screen's own not-found handling applies — no new fallback UI is introduced (per spec Assumptions:
  no new widget-specific app screen).

## Cross-widget consistency (same coin, multiple widget instances)

After any `ActionCallback` above completes a successful `recordDecision`/`recordInteraction`, the
callback also calls `updateAll()` for **both** `SingleCoinWidget` and `QuickCoinsWidget` (not just
`update()` on the tapped instance) — cheap when there are few instances, and it is what guarantees
the edge case "same coin represented by more than one widget ... reflected consistently once both
are next updated" holds immediately rather than only on the next unrelated repository emission.
