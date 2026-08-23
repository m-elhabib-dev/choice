# Phase 0 Research: Android Home-Screen Widgets

## 1. Widget framework: Jetpack Glance vs. classic `AppWidgetProvider`/`RemoteViews`

- **Decision**: Build both widgets with `androidx.glance:glance-appwidget`.
- **Rationale**: Glance is the current, officially supported Jetpack API for App Widgets. It lets
  widget UI be written as composable-style Kotlin (`GlanceAppWidget`, `provideContent { ... }`)
  that compiles down to `RemoteViews`, instead of hand-assembling `RemoteViews` and manually
  diffing view state in an `AppWidgetProvider`. This directly satisfies Principle IV (Modern
  Android: no adoption of deprecated patterns for new work) and keeps the widget code style close
  to the rest of the Compose-based app (Principle I: Simplicity First — one mental model for UI
  code, not two).
- **Alternatives considered**:
  - *Classic `AppWidgetProvider` + hand-built `RemoteViews`*: more boilerplate, more manual state
    management, and is the pattern Glance was built to replace — rejected as not "current,
    officially supported" per Principle IV.
  - *WebView-based widget*: not a supported Android App Widget mechanism; rejected.

## 2. Per-widget-instance configuration storage

- **Decision**: Use Glance's built-in `PreferencesGlanceStateDefinition` — one small `Preferences`
  blob per `GlanceId` (App Widget ID) holding only IDs: a single `coinId` (Single Coin Widget) or
  a coin-ID list plus a `useFavorites` flag (Quick Coins Widget).
- **Rationale**: This is the smallest amount of new persistence possible. It stores identity only
  (never a copy of a coin's name/choices/weights/history — FR-018), is automatically scoped and
  cleaned up per widget instance by the Glance framework (including on widget deletion via
  `onDeleted`), and needs no new explicit dependency (it rides on `datastore-preferences`, already
  pulled in transitively by `glance-appwidget`). This satisfies Principle VII (Minimal
  Dependencies) better than adding a new Room table + DAO for what is effectively a tiny key-value
  mapping.
- **Alternatives considered**:
  - *New Room table (`widget_config`)*: would work, but adds schema/migration overhead for data
    that is inherently per-OS-widget-instance rather than a durable app entity, and duplicates what
    Glance already provides for free — rejected as unnecessary complexity (Principle I, VII).
  - *`SharedPreferences` directly*: functionally similar to `PreferencesGlanceStateDefinition` but
    would require manually keying by App Widget ID and manually wiring cleanup on deletion —
    rejected in favor of the framework-provided, already-scoped equivalent.

## 3. Refresh strategy (staying in sync with app-side changes)

- **Decision**: No periodic/background polling. Two triggers only:
  1. **User interaction** — a widget's own `ActionCallback` (flip, reconfigure) updates that
     widget's own Glance state directly.
  2. **Reactive app-side change** — a single application-scoped `WidgetRefreshCoordinator`,
     started from `ChoiceApplication.onCreate`, collects the existing `CoinRepository
     .observeCoins()` Flow (already used by `MainViewModel`) and, on every emission, calls
     `SingleCoinWidget().updateAll(context)` and `QuickCoinsWidget().updateAll(context)`. Both
     calls are cheap no-ops when there are zero active instances of that widget type.
- **Rationale**: `observeCoins()` already emits on every rename, choice edit, favorite toggle,
  weighted/avoid-last-result setting change, and delete — exactly the change set FR-011/FR-012/
  SC-005/SC-006 require widgets to reflect. Piggy-backing on it means widgets need no new "did
  something change" signal, and there is no timer, matching the spec's own Assumptions ("continuous
  or frequent background refreshing on a timer is out of scope"). `updateAll` re-runs each active
  instance's content composition, which re-resolves the instance's configured coin(s) against
  current repository state, so a rename/edit/delete/favorite-change is picked up without the user
  removing and re-adding the widget.
- **Alternatives considered**:
  - *`WorkManager` periodic refresh*: explicitly rejected by the spec's Assumptions section; would
    also cost battery for no behavioral benefit given a reactive Flow already exists.
  - *Per-widget targeted `update(context, glanceId)` instead of `updateAll`*: more precise, but
    would require tracking which coin ID(s) each instance currently shows just to decide whether a
    given change is relevant — extra bookkeeping for a benefit (skipping a cheap recomposition of
    an idle widget) that doesn't matter at this app's scale (Principle I: simpler wins when outcome
    is the same).

## 4. Decision engine reuse

- **Decision**: Widget flips call the existing `selectChoice(choices, weightedEnabled,
  avoidLastResultEnabled, lastChoiceId)` domain function and `CoinRepository.recordDecision` /
  `recordInteraction` directly — the same functions `CoinFlipViewModel` calls today. No parallel
  selection logic, no duplicated weighting/avoid-last-result code.
- **Rationale**: FR-006, FR-018, User Story 3, and SC-010 all require bit-for-bit identical
  decision behavior between app and widget. The only way to guarantee that without duplicated (and
  potentially divergent) logic is to call the same function. This code is also already unit-tested
  (Principle VI), so no new test burden for the selection math itself.
- **Alternatives considered**: Re-implementing selection inside `widget/` — rejected outright as a
  correctness and maintenance risk with no benefit.

## 5. One-tap flip vs. the app's two-step flip-then-accept

- **Decision**: A widget's flip action performs **select + record in one atomic step** (equivalent
  to the app's `flip()` immediately followed by `accept()`), not the app's two-step "preview, then
  accept" flow.
- **Rationale**: The spec's widget acceptance scenarios (US1 AS3: "the widget performs a decision
  and immediately displays the selected choice as the current result") and FR-008 ("present that
  decision as the current, settled result") describe a single tap producing a settled, recorded
  outcome — there is no described widget "accept" step, and a widget's constrained surface (one
  tap target, no secondary confirmation control expected by the spec) makes a two-step flow poor
  UX. FR-009's "explicit override" (a further, visually secondary flip) is itself just another
  one-tap select+record — consistent with the app's own override affordance (re-flipping is always
  available, just not the default path), satisfying Principle II (Decision First) without
  introducing a widget-only concept of an "unaccepted" result.
- **Alternatives considered**: Mirroring the app's two-step flip/accept inside the widget —
  rejected: it would need a second visible control at all times (contradicting FR-008's "not the
  visually primary action" for re-flipping) and contradicts the acceptance scenario's "immediately
  displays ... as the current result" wording.

## 6. Rapid repeated taps on one flip action

- **Decision**: Guard each widget instance's flip action with an in-memory, per-`GlanceId` mutex
  (`Mutex` held in a process-wide singleton map) so a second tap arriving while the first tap's
  select+record is still in flight is dropped rather than queued or double-recorded.
- **Rationale**: Glance `ActionCallback`s for the same app run in the same process (no separate
  widget process is used by this app), so an in-memory guard is sufficient and needs no persisted
  "isFlipping" flag (which would itself risk getting stuck `true` if the process died mid-flip).
  This satisfies the edge case "each tap must result in at most one recorded decision at a time."
- **Alternatives considered**: Persisting an in-progress flag in the widget's own Preferences state
  — rejected as unnecessary and a source of a possible stuck state after a process death; disabling
  the button visually after tap — not reliably possible mid-flight for a `RemoteViews`-backed
  surface without first awaiting the same in-memory guard anyway, so the guard is the load-bearing
  mechanism regardless.

## 7. Navigating from a widget into the app

- **Decision**: Reuse `MainActivity`'s existing `NavHost` and its `coinflip/{coinId}` route. A
  widget's "open in app" action starts `MainActivity` with an explicit coin ID extra; a small
  `LaunchedEffect` (mirroring the existing `ACTION_SEND` import handling already in
  `MainActivity`) navigates straight to `coinflip/{coinId}` if the extra is present, or to `main`
  (existing coin list) as the fallback when the coin no longer resolves (US5 AS3, unavailable
  state).
- **Rationale**: The spec's Assumptions explicitly say "no new widget-specific app screen is
  introduced"; the app already has exactly the screen and route needed.
- **Alternatives considered**: A dedicated widget-launch screen — explicitly out of scope per the
  spec's Assumptions.

## 8. Quick Coins Widget default coin set

- **Decision**: When a Quick Coins Widget instance's stored config has no explicit coin-ID list
  (or `useFavorites = true`), its content resolves against the existing `CoinRepository
  .observeQuickCoins()` Flow — the same "favorited coins" query `MainViewModel` already uses —
  rather than a widget-specific favorites query.
- **Rationale**: FR-002 and Assumptions both specify favorites as the default source, and
  `observeQuickCoins()` already exists and is already exercised by app code and tests.
- **Alternatives considered**: Duplicating a "favorited coins" query inside `widget/` — rejected as
  needless duplication of an existing, correct query.

## 9. Widget visual theme

- **Decision**: Add a small Glance-specific mirror of the existing Catppuccin Mocha constants
  (`ui/theme/Color.kt`) as `androidx.glance.color.ColorProvider` values in
  `widget/WidgetGlanceTheme.kt`, used directly in each widget's `GlanceModifier`/text styles.
  `glance-material3` (dynamic Material You theming) is not used, since the app does not use
  Android dynamic color anywhere else and Catppuccin Mocha is a fixed palette (Principle V:
  Consistent Design — one shared theme source of truth, mirrored rather than forked in values).
- **Rationale**: Glance's composition model cannot directly consume a Compose `MaterialTheme`
  instance from the main app (Glance has its own theming types), so a values mirror is the
  practical minimum needed to keep both surfaces visually consistent without adding a dependency.
- **Alternatives considered**: `glance-material3` dynamic color — rejected, wrong palette model for
  a fixed brand palette; hard-coded literal colors inline in each widget file — rejected as a
  "one-off styling" duplication that Principle V explicitly disfavors in favor of a shared token
  source.

## Outcome

No unresolved `NEEDS CLARIFICATION` items remain. All Technical Context fields in `plan.md` are
answered from the existing codebase's own established patterns plus one new, justified Jetpack
dependency (`glance-appwidget`).
