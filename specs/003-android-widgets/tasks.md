---

description: "Task list template for feature implementation"
---

# Tasks: Android Home-Screen Widgets

**Input**: Design documents from `/specs/003-android-widgets/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Unit tests are explicitly called for in plan.md/quickstart.md for the plain-Kotlin logic
underneath the widgets (`WidgetConfigStoreTest`, `QuickCoinsDefaultSelectionTest`,
`WidgetFlipGuardTest`). Glance `GlanceAppWidget` composition itself is not unit-testable and is
validated manually via `quickstart.md` — no test tasks are generated for widget UI composition.

**Organization**: Tasks are grouped by user story (US1–US6, per spec.md priorities) to enable
independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Single existing Android app module (`app/`). All new widget code lives under
`app/src/main/kotlin/com/choice/app/widget/`; new tests under
`app/src/test/kotlin/com/choice/app/widget/`; new resources under `app/src/main/res/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add the Glance dependency and create the `widget/` package skeleton.

- [X] T001 Add `implementation("androidx.glance:glance-appwidget:1.1.1")` to the `dependencies` block
      in `app/build.gradle` (alongside the existing `androidx.compose`/`androidx.navigation`
      entries), then sync/build to confirm resolution.
- [X] T002 Create the empty `app/src/main/kotlin/com/choice/app/widget/` package directory (no
      files yet — placeholder for Phase 2/3 sources) and the empty
      `app/src/test/kotlin/com/choice/app/widget/` test package directory.
- [X] T003 [P] Add widget label/description string resources to
      `app/src/main/res/values/strings.xml`: `widget_single_coin_label`,
      `widget_single_coin_description`, `widget_quick_coins_label`,
      `widget_quick_coins_description`, plus shared state-message strings used across both widgets
      (`widget_state_ready`, `widget_state_unavailable`, `widget_state_no_coins_selected`,
      `widget_state_too_few_choices`, `widget_state_no_coins_at_all`, `widget_action_flip`,
      `widget_action_reconfigure`, `widget_action_open_app`, `widget_action_create_coin`).

**Checkpoint**: Dependency resolves, package skeleton exists, string resources are available for
every subsequent phase to reference.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared theme, config storage, and refresh/concurrency infrastructure that every user
story's widget code depends on. No user story can be implemented until this phase is complete.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 [P] Create `app/src/main/kotlin/com/choice/app/widget/WidgetGlanceTheme.kt` — a
      `androidx.glance.color.ColorProvider`-based mirror of the Catppuccin Mocha constants in
      `app/src/main/kotlin/com/choice/app/ui/theme/Color.kt` (per research.md §9), exposing named
      color providers (background, surface, text-primary, text-secondary, accent) for use in
      Glance `GlanceModifier`/text styles.
- [X] T005 [P] Create `app/src/main/kotlin/com/choice/app/widget/WidgetConfigStore.kt` implementing
      read/write for `SingleCoinWidgetConfig` (`coinId: Long?`) and `QuickCoinsWidgetConfig`
      (`useFavorites: Boolean`, `explicitCoinIds: List<Long>`) via Glance's
      `PreferencesGlanceStateDefinition`, per data-model.md's "New per-widget-instance
      configuration" section. Include a decode-failure path that surfaces as "no config" (mapped to
      Unavailable by callers) rather than throwing, per FR-015 / contracts/widget-configuration-
      contract.md §3.
- [X] T006 [P] Create the in-memory per-`GlanceId` (and per-`(GlanceId, coinId)` for Quick Coins)
      flip mutex singleton in `app/src/main/kotlin/com/choice/app/widget/WidgetFlipGuard.kt`, per
      research.md §6: a process-wide `Map` of keys to `kotlinx.coroutines.sync.Mutex`, with a
      `tryFlip(key, block)`-style suspend function that runs `block` only if the mutex is
      immediately acquirable and otherwise returns without running it (dropped tap).
- [X] T007 Create `app/src/main/kotlin/com/choice/app/widget/WidgetRefreshCoordinator.kt`: a
      singleton started from `ChoiceApplication.onCreate` (wire the start call into
      `app/src/main/kotlin/com/choice/app/ChoiceApplication.kt`) that collects
      `appContainer.coinRepository.observeCoins()` in an application-scoped `CoroutineScope` and, on
      every emission, calls `SingleCoinWidget().updateAll(context)` and
      `QuickCoinsWidget().updateAll(context)` (per research.md §3). Forward-references
      `SingleCoinWidget`/`QuickCoinsWidget`, which are created in Phase 3/4 — this task creates the
      coordinator class and wiring; it compiles once those classes exist.
- [X] T008 [P] Create `app/src/main/kotlin/com/choice/app/widget/WidgetActions.kt` with the shared
      `OpenCoinInAppAction : ActionCallback` (per contracts/widget-action-contract.md) that starts
      `MainActivity` with an explicit `EXTRA_OPEN_COIN_ID: Long` intent extra when a resolvable
      `coinId` parameter is present, and no extra otherwise (fallback to `main`).
- [X] T009 Add the `EXTRA_OPEN_COIN_ID` handling to
      `app/src/main/kotlin/com/choice/app/MainActivity.kt`'s `ChoiceNavHost`/`onCreate`: on launch,
      if `intent.hasExtra(EXTRA_OPEN_COIN_ID)`, navigate to `coinflip/{coinId}` (mirroring the
      existing `ACTION_SEND` `LaunchedEffect` pattern at `MainActivity.kt:49-54`); otherwise keep
      the existing `main` start destination behavior.

**Checkpoint**: Theme, config storage, flip-guard, refresh coordinator, and the shared open-in-app
action all exist and compile. User story implementation can now begin.

---

## Phase 3: User Story 1 - Make a decision from a Single Coin Widget (Priority: P1) 🎯 MVP

**Goal**: A user can add a Single Coin Widget, pick one saved coin for it, and flip it to see a
settled result directly on the home screen — with no app launch.

**Independent Test**: With ≥1 saved coin having ≥2 choices, add a Single Coin Widget, select that
coin during configuration, confirm a "ready" state, tap flip, confirm a choice displays immediately
with no app launch.

### Tests for User Story 1

- [X] T010 [P] [US1] Unit test `WidgetConfigStoreTest` in
      `app/src/test/kotlin/com/choice/app/widget/WidgetConfigStoreTest.kt`: covers writing/reading
      a `SingleCoinWidgetConfig(coinId)`, reading an unset/never-written config, and reading a
      corrupted/undecodable value — asserting each maps to "no config" rather than throwing.
- [X] T011 [P] [US1] Unit test `WidgetFlipGuardTest` in
      `app/src/test/kotlin/com/choice/app/widget/WidgetFlipGuardTest.kt`: launches two concurrent
      `tryFlip` calls on the same key and asserts exactly one runs its block to completion (the
      other is dropped), per the rapid-tap edge case and research.md §6.

### Implementation for User Story 1

- [X] T012 [US1] Create `app/src/main/res/xml/single_coin_widget_info.xml`
      (`AppWidgetProviderInfo`) per contracts/widget-provider-contract.md: 1-cell min size, 2x1
      target cell size, `resizeMode="horizontal|vertical"`, `widgetCategory="home_screen"`,
      `updatePeriodMillis="0"`, `configure="com.choice.app.widget.SingleCoinWidgetConfigActivity"`,
      referencing the `widget_single_coin_label`/`widget_single_coin_description` strings from T003.
- [X] T013 [US1] Create `app/src/main/kotlin/com/choice/app/widget/SingleCoinWidget.kt`: a
      `GlanceAppWidget` that resolves its `SingleCoinWidgetConfig` (T005) against
      `CoinRepository.observeCoin(coinId)`, renders the derived state from data-model.md's "Derived
      render states" table (Unavailable / Too Few Choices / Ready / Result), shows the coin name as
      a tap target wired to `OpenCoinInAppAction` (T008), and shows a primary flip control (Ready)
      or a settled result plus visually secondary flip control (Result) per FR-008/FR-009. Uses
      `WidgetGlanceTheme` (T004) for styling.
- [X] T014 [US1] Create `app/src/main/kotlin/com/choice/app/widget/SingleCoinWidgetReceiver.kt`
      (`GlanceAppWidgetReceiver`) exposing `SingleCoinWidget()` as its `glanceAppWidget`.
- [X] T015 [US1] Create `app/src/main/kotlin/com/choice/app/widget/SingleCoinWidgetConfigActivity.kt`
      implementing the system-initiated configuration handshake from
      contracts/widget-configuration-contract.md §1: read `EXTRA_APPWIDGET_ID`
      (finish immediately if missing), default `setResult(RESULT_CANCELED)`, show the "no coins yet"
      state with an open-app action when `observeCoins()` is empty (FR-013), otherwise render a coin
      picker; on selection, write `SingleCoinWidgetConfig(coinId)` via `WidgetConfigStore` (T005),
      call `setResult(RESULT_OK, ...)`, and `finish()`.
- [X] T016 [US1] Implement `FlipSingleCoinAction : ActionCallback` in `WidgetActions.kt` per
      contracts/widget-action-contract.md: guarded by `WidgetFlipGuard` (T006) keyed on `glanceId`;
      resolves config + `CoinWithChoices`; on unavailable/too-few-choices just triggers a re-render;
      otherwise calls `selectChoice(...)`, `CoinRepository.recordDecision(...)`,
      `recordInteraction(...)`, then `SingleCoinWidget().update(context, glanceId)`.
- [X] T017 [US1] Add the `<receiver>` for `SingleCoinWidgetReceiver` and the `<activity>` for
      `SingleCoinWidgetConfigActivity` to `app/src/main/AndroidManifest.xml` exactly as specified in
      contracts/widget-provider-contract.md's "AndroidManifest.xml additions" section.
- [X] T018 [US1] Add a simple `res/drawable` preview image (or reuse the existing app icon) and
      reference it via `android:previewImage` in `single_coin_widget_info.xml` (T012).

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently —
add the widget, configure it, flip it, see a settled result, survive process death (Glance state
persistence is automatic via `PreferencesGlanceStateDefinition`).

---

## Phase 4: User Story 2 - Make decisions for several coins from one Quick Coins Widget (Priority: P2)

**Goal**: A user can add a Quick Coins Widget showing several favorited/selected coins, each with
its own flip action, adapting to available home-screen space.

**Independent Test**: With ≥2 favorited coins, add a Quick Coins Widget, confirm it lists those
coins with a flip action each, and confirm tapping one coin's flip only affects that coin.

### Tests for User Story 2

- [X] T019 [P] [US2] Unit test `QuickCoinsDefaultSelectionTest` in
      `app/src/test/kotlin/com/choice/app/widget/QuickCoinsDefaultSelectionTest.kt`: covers
      `useFavorites = true` resolving against `observeQuickCoins()`, `useFavorites = false`
      resolving `explicitCoinIds` against `observeCoins()` with stale/deleted IDs dropped (not shown
      as broken rows), and both cases producing an empty list when nothing resolves.

### Implementation for User Story 2

- [X] T020 [US2] Create `app/src/main/res/xml/quick_coins_widget_info.xml`
      (`AppWidgetProviderInfo`) per contracts/widget-provider-contract.md: ~2x2 cell min size, 3x2
      target cell size, `resizeMode="horizontal|vertical"`, `widgetCategory="home_screen"`,
      `updatePeriodMillis="0"`,
      `configure="com.choice.app.widget.QuickCoinsWidgetConfigActivity"`, referencing the
      `widget_quick_coins_label`/`widget_quick_coins_description` strings from T003.
- [X] T021 [US2] Create `app/src/main/kotlin/com/choice/app/widget/QuickCoinsWidget.kt`: a
      `GlanceAppWidget` that resolves its `QuickCoinsWidgetConfig` (T005) — favorites via
      `observeQuickCoins()` or explicit IDs via `observeCoins()` filtering per data-model.md — and
      renders a per-coin row (name tappable via `OpenCoinInAppAction`, flip control wired to
      `FlipQuickCoinAction` keyed by that row's `coinId`), adapting the number of visible rows to
      `LocalSize` (Glance's size-aware composition) so it shows as many coins as fit without
      clipping/overlap. Renders the "No Coins Selected" state (data-model.md) when the resolved set
      is empty.
- [X] T022 [US2] Create `app/src/main/kotlin/com/choice/app/widget/QuickCoinsWidgetReceiver.kt`
      (`GlanceAppWidgetReceiver`) exposing `QuickCoinsWidget()` as its `glanceAppWidget`.
- [X] T023 [US2] Create
      `app/src/main/kotlin/com/choice/app/widget/QuickCoinsWidgetConfigActivity.kt` implementing the
      configuration handshake from contracts/widget-configuration-contract.md §1: same
      `EXTRA_APPWIDGET_ID`/`RESULT_CANCELED` default and "no coins yet" (FR-013) handling as T015;
      otherwise render a multi-select list pre-checked to current favorites with a "use my favorites
      (auto-updating)" toggle — toggle on writes `QuickCoinsWidgetConfig(useFavorites = true)`;
      toggle off with an explicit subset writes
      `QuickCoinsWidgetConfig(useFavorites = false, explicitCoinIds = selection)` via
      `WidgetConfigStore` (T005); `setResult(RESULT_OK, ...)` and `finish()`.
- [X] T024 [US2] Implement `FlipQuickCoinAction : ActionCallback` in `WidgetActions.kt` per
      contracts/widget-action-contract.md: takes a `coinId: Long` `ActionParameters.Key`, guarded by
      `WidgetFlipGuard` (T006) keyed on `(glanceId, coinId)` so different coins in the same instance
      can flip concurrently while the same coin cannot double-fire; same
      resolve/select/record/re-render logic as `FlipSingleCoinAction`, updating only via
      `QuickCoinsWidget().update(context, glanceId)`.
- [X] T025 [US2] Add the `<receiver>` for `QuickCoinsWidgetReceiver` and the `<activity>` for
      `QuickCoinsWidgetConfigActivity` to `app/src/main/AndroidManifest.xml` per
      contracts/widget-provider-contract.md.
- [X] T026 [P] [US2] Add a `res/drawable` preview image (or reuse the app icon) and reference it via
      `android:previewImage` in `quick_coins_widget_info.xml` (T020).

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently — Single Coin
Widget and Quick Coins Widget both flip correctly, and Quick Coins adapts its row count to size.

---

## Phase 5: User Story 3 - Widget decisions follow each coin's configured rules (Priority: P2)

**Goal**: Widget-made decisions for a coin follow that coin's exact uniform/weighted/avoid-last-
result rules, identical to app-made decisions.

**Independent Test**: Flip a heavily-weighted coin ~20 times from its widget and confirm the
distribution matches in-app flips; flip an avoid-last-result coin twice from its widget and confirm
no immediate repeat.

**Note**: `selectChoice`/`selectWeighted`/`flipCoin`/`applyAvoidLastResult` already exist and are
already unit-tested (per research.md §4) — this story is satisfied by construction because
`FlipSingleCoinAction` (T016) and `FlipQuickCoinAction` (T024) both call `selectChoice(...)`
directly with the coin's live `weightedEnabled`/`avoidLastResultEnabled` flags and live choices
read at flip time. No new production code is needed beyond T016/T024; this phase only adds the
regression test that pins that reuse down.

### Tests for User Story 3

- [X] T027 [US3] Add a unit test to
      `app/src/test/kotlin/com/choice/app/widget/WidgetFlipGuardTest.kt` (or a new
      `WidgetDecisionParityTest.kt` in the same directory) asserting that the widget flip path calls
      `selectChoice` with the coin's current `weightedEnabled`/`avoidLastResultEnabled` values and
      the most recent decision's `choiceId` as `lastChoiceId` — i.e. that no widget-local selection
      logic exists and the exact same function/arguments the app uses are used here, guarding
      against future duplication.

**Checkpoint**: Decision-rule parity between widget and app flips is explicit and pinned by test,
not just implied by shared code.

---

## Phase 6: User Story 4 - Widgets stay consistent with changes made in the app (Priority: P3)

**Goal**: Renaming, editing choices on, favoriting/unfavoriting, or deleting a coin in the app is
reflected by every widget representing that coin, without removing/re-adding it.

**Independent Test**: Configure a widget for a coin, then rename/edit/delete it in-app; after each
change, confirm the widget updates or shows a clear Unavailable state.

### Implementation for User Story 4

- [X] T028 [US4] Wire `WidgetRefreshCoordinator` (T007) into `ChoiceApplication.onCreate` if not
      already fully wired by T007 (verify the coordinator's `CoroutineScope` is started exactly
      once and stays alive for the process lifetime) — this task is the integration checkpoint that
      makes rename/edit/favorite/delete reflect on all widgets per FR-011/FR-012/SC-005/SC-006,
      since `SingleCoinWidget`/`QuickCoinsWidget` (T013/T021) now exist for `updateAll()` to target.
- [X] T029 [US4] In `SingleCoinWidget.kt` (T013), confirm/adjust the Too-Few-Choices render path so
      that a coin whose choices drop to 0 or 1 (edited down in-app) renders the "can't decide yet"
      state (data-model.md, edge case) rather than attempting a flip — add the corresponding branch
      if not already covered by T013's state derivation.
- [X] T030 [P] [US4] Add a unit test in `app/src/test/kotlin/com/choice/app/widget/` (e.g.
      `QuickCoinsDefaultSelectionTest.kt`, extending T019) covering: a favorites-mode Quick Coins
      Widget's resolved set changes when the underlying `observeQuickCoins()` favorites set changes
      (US4 AS4), and an explicit-mode widget silently drops (not errors on) an ID that no longer
      resolves after a delete (US4 AS3-equivalent, per-coin).

**Checkpoint**: All user stories 1–4 independently functional; app-side changes propagate to
widgets without manual widget removal/re-add.

---

## Phase 7: User Story 5 - Open a coin in the app directly from a widget (Priority: P3)

**Goal**: Tapping a coin's name/header on either widget type opens that coin directly in the app;
tapping the same affordance on an Unavailable widget falls back to the main coin list.

**Independent Test**: Tap a Single Coin Widget's header and a Quick Coins Widget row's name; confirm
each opens the app directly to that coin. Tap the action on an Unavailable widget; confirm it opens
to the main list, not a failure.

### Implementation for User Story 5

- [X] T031 [US5] Verify/finish `OpenCoinInAppAction` (T008) is wired as the tap target on: the coin
      name/header in `SingleCoinWidget.kt`'s Ready/Result states (with `coinId` from its config),
      every row's name in `QuickCoinsWidget.kt` (with that row's own `coinId`), and the "open app"
      affordance shown in both widgets' Unavailable state (with `coinId = null`, i.e. fallback to
      `main`).
- [X] T032 [US5] Confirm `MainActivity`'s `coinflip/{coinId}` route (T009's navigation target)
      handles a `coinId` that no longer resolves (deleted between widget render and tap) via its
      existing not-found handling in
      `app/src/main/kotlin/com/choice/app/ui/coinflip/CoinFlipScreen.kt` / its ViewModel, per
      contracts/widget-action-contract.md ("no new fallback UI is introduced") — add a fallback only
      if none currently exists.

**Checkpoint**: Deep-linking from both widget types into the app works for both the happy path and
the deleted-coin path.

---

## Phase 8: User Story 6 - Add multiple independently configured widgets (Priority: P4)

**Goal**: Multiple Single Coin Widget instances, each configured for a different coin, operate
fully independently — flipping or deleting one does not affect another.

**Independent Test**: Add two Single Coin Widgets for different coins; flip one; confirm only it
updates. Delete one's coin; confirm only that instance goes Unavailable.

**Note**: Per-instance independence is already structural — `WidgetConfigStore` (T005) keys by
`GlanceId`/App Widget ID, `WidgetFlipGuard` (T006) keys by `GlanceId`, and
`ActionCallback.onAction` always receives the specific `glanceId` being acted on. This phase adds
the test that confirms that structural independence holds, rather than new production code.

### Tests for User Story 6

- [X] T033 [US6] Add a unit test in
      `app/src/test/kotlin/com/choice/app/widget/WidgetConfigStoreTest.kt` (extending T010) that
      writes two distinct `SingleCoinWidgetConfig`s under two different `GlanceId`s and asserts
      reading each back returns its own `coinId`, unaffected by writes to the other — pinning the
      per-instance isolation multiple simultaneous widgets rely on.

**Checkpoint**: All 6 user stories independently functional and covered per their Independent Test
criteria.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Accessibility, offline validation, and final manual acceptance across all stories.

- [X] T034 [P] Add Glance `contentDescription`/semantics text (via `GlanceModifier.semantics` or the
      equivalent Glance accessibility API) to every widget element in `SingleCoinWidget.kt` (T013)
      and `QuickCoinsWidget.kt` (T021): coin name, current result/state text, and each control
      (flip, reconfigure, open-app) per FR-020/SC-009.
- [X] T035 Run `./gradlew :app:testDebugUnitTest` and confirm all new widget tests (T010, T011,
      T019, T027, T030, T033) and all existing tests pass.
- [ ] T036 Execute `quickstart.md` Scenarios 1–6 plus the Offline check, Accessibility check, and
      Rapid-tap check manually on a device/emulator per its instructions, and record results.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup (T001–T003) — BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) completion. No dependency on other
  stories. **MVP — deliver first.**
- **User Story 2 (Phase 4)**: Depends on Foundational (Phase 2). Independent of US1's widget code,
  but shares `WidgetConfigStore`/`WidgetFlipGuard`/`WidgetGlanceTheme`/`WidgetActions` from Phase 2.
- **User Story 3 (Phase 5)**: Depends on US1 (T016) and US2 (T024) existing (it tests their
  behavior) — sequence after Phases 3–4.
- **User Story 4 (Phase 6)**: Depends on US1 (T013) and US2 (T021)'s widget classes existing (for
  `WidgetRefreshCoordinator.updateAll()` to target) and on Phase 2's T007 — sequence after Phases
  3–4.
- **User Story 5 (Phase 7)**: Depends on US1 (T013) and US2 (T021) widget classes and Phase 2's T008
  (`OpenCoinInAppAction`) and T009 (`MainActivity` handling) — sequence after Phases 3–4.
- **User Story 6 (Phase 8)**: Depends on Phase 2's T005/T006 (config store, flip guard) — can run
  any time after Phase 2, but is ordered last per spec priority (P4).
- **Polish (Phase 9)**: Depends on all desired user stories (Phases 3–8) being complete.

### Within Each User Story

- Tests (where included) before/alongside their implementation tasks.
- Config/model resolution before widget composition; widget composition before receiver; receiver +
  config Activity before manifest registration.
- Story complete before moving to next priority (if working sequentially).

### Parallel Opportunities

- T003 (strings) can run in parallel with T001–T002.
- T004, T005, T006, T008 (Phase 2) touch different files and can run in parallel; T007 and T009
  depend on classes/patterns from other files and should follow.
- T010 and T011 (US1 tests) can run in parallel with each other and with T012 (resource file).
- T019 (US2 test) can run in parallel with T020 (resource file).
- T026 can run in parallel with T021–T025 (different file).
- T030 and T033 (test-only tasks in Phases 6/8) can run in parallel with each other.
- Once Phase 2 is complete, Phase 3 (US1) and Phase 4 (US2) can be worked on in parallel by
  different developers, since they touch disjoint file sets (`SingleCoinWidget*` vs.
  `QuickCoinsWidget*`) aside from the shared `WidgetActions.kt` (T016 vs. T024 — coordinate if
  working simultaneously in the same file).

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Launch independent foundational tasks together:
Task: "Create WidgetGlanceTheme.kt Catppuccin Mocha color mirror"
Task: "Create WidgetConfigStore.kt (Single + Quick Coins config read/write)"
Task: "Create WidgetFlipGuard.kt (per-GlanceId mutex map)"
Task: "Create WidgetActions.kt's OpenCoinInAppAction"
```

## Parallel Example: User Story 1

```bash
# Launch US1 tests together:
Task: "WidgetConfigStoreTest — write/read/corrupt-decode for SingleCoinWidgetConfig"
Task: "WidgetFlipGuardTest — concurrent tryFlip on the same key runs exactly once"

# In parallel with the above, the resource file:
Task: "Create single_coin_widget_info.xml"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories).
3. Complete Phase 3: User Story 1 (Single Coin Widget core flow).
4. **STOP and VALIDATE**: Run `quickstart.md` Scenario 1 end-to-end on a device/emulator.
5. Deploy/demo if ready — this alone satisfies SC-001/SC-002/SC-003 for a single coin.

### Incremental Delivery

1. Setup + Foundational → foundation ready.
2. Add US1 (Single Coin Widget) → validate via Scenario 1 → MVP.
3. Add US2 (Quick Coins Widget) → validate via Scenario 2.
4. Add US3 (decision-rule parity regression test) → validate via Scenario 3.
5. Add US4 (app-sync) → validate via Scenario 4.
6. Add US5 (open-in-app) → validate via Scenario 5.
7. Add US6 (multi-instance) → validate via Scenario 6.
8. Phase 9 Polish → accessibility + offline + full quickstart pass.

### Parallel Team Strategy

With two developers, after Phase 2 is complete: Developer A takes Phase 3 (US1) then Phase 5 (US3)
then Phase 7 (US5); Developer B takes Phase 4 (US2) then Phase 6 (US4) then Phase 8 (US6);
coordinate on shared `WidgetActions.kt` edits. Both converge on Phase 9 Polish.

---

## Notes

- [P] tasks = different files, no dependencies.
- [Story] label maps task to specific user story for traceability.
- US3 and US6 are satisfied largely "by construction" from Phase 2/3/4 design choices (shared
  `selectChoice` call, per-`GlanceId` keying); their phases mainly add pinning regression tests —
  this is intentional per research.md §4 and data-model.md, not a shortcut.
- Verify tests fail before implementing (where a test task precedes its implementation task in the
  same phase, e.g. T010/T011 before T013/T016).
- Commit after each task or logical group.
- Stop at any checkpoint to validate a story independently via the matching `quickstart.md`
  scenario.
- Avoid: vague tasks, same-file conflicts (`WidgetActions.kt` is touched by both US1 and US2 —
  coordinate), cross-story dependencies that break independence.
