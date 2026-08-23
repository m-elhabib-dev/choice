# Implementation Plan: Android Home-Screen Widgets

**Branch**: `003-android-widgets` | **Date**: 2026-08-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-android-widgets/spec.md`

## Summary

Add two Android home-screen widgets — a Single Coin Widget (one coin, flip action, most-recent
result) and a Quick Coins Widget (several favorited/selected coins, one flip action each) — that
let users make decisions without opening the Choice app. Both widgets are built with Jetpack
Glance so they can be authored in Compose-like Kotlin instead of hand-rolled `RemoteViews`, and
both read/write through the existing `CoinRepository` (backed by the existing Room database) so
there is no separate widget data store, decision engine, or history — only a small per-widget-
instance configuration (which coin, or which coins) persisted via Glance's built-in
`PreferencesGlanceStateDefinition`. Widget flips reuse the existing `selectChoice` domain function
and write through `CoinRepository.recordDecision`, so a widget-made decision is indistinguishable
from an app-made one in the coin's history. Widgets refresh reactively off the same
`observeCoins()`/`observeCoin()` Flows the app UI already uses — no periodic background polling.

## Technical Context

**Language/Version**: Kotlin, JVM target 17 (matches existing `app` module)

**Primary Dependencies**:
- `androidx.glance:glance-appwidget` (new) — Jetpack's current, officially supported way to build
  App Widgets with Compose-like code; replaces hand-written `RemoteViews`/`AppWidgetProvider`
  boilerplate and is the platform-recommended approach for new widget work (Principle IV: Modern
  Android). Brings `datastore-preferences` transitively, used for per-widget config — no separate
  dependency needed.
- Existing: `androidx.room` (via `CoinRepository`/`CoinDao`), `kotlinx-coroutines-android`,
  `androidx.navigation:navigation-compose` (for the widget's "open in app" deep link),
  `androidx.core:core-ktx`. No other new third-party dependency.

**Storage**: Existing Room database (`ChoiceDatabase`) for coins/choices/decisions — unchanged.
Per-widget-instance configuration (which coin, or which coin set) is stored via Glance's
`PreferencesGlanceStateDefinition`, i.e. one small `Preferences` blob per `GlanceId`/App Widget ID,
holding only coin IDs — never a copy of coin/choice/decision data (FR-018).

**Testing**: JUnit4 + `kotlinx-coroutines-test` (existing pattern under `app/src/test`) for all
widget-adjacent business logic: config-to-coin resolution, favorites-default fallback, rapid-tap
single-flight guarding, and reuse of `selectChoice`/`flipCoin` (already tested, unchanged). Glance
`GlanceAppWidget` composition itself is not unit-testable in isolation (renders to `RemoteViews`
via the OS); it is validated manually per `quickstart.md`.

**Target Platform**: Android, minSdk 26 / targetSdk 35 / compileSdk 35 (unchanged; Glance supports
API 21+, so no floor change is required).

**Project Type**: Native Android mobile app — single existing Gradle module (`app`); this feature
adds a new package inside that module rather than a new module.

**Performance Goals**: Widget flip-to-result perceived as immediate (SC-002) — target < 300ms from
tap to updated `RemoteViews` on typical hardware, matching the in-app flip's local, no-network
computation cost.

**Constraints**: Fully offline (no network calls anywhere in the widget code path, Principle III);
no background/periodic refresh timer (`updatePeriodMillis="0"`, per spec Assumptions) — widgets
update only on user interaction or on a relevant local data change; each home-screen tap must
produce at most one recorded decision even under rapid repeated taps (edge case).

**Scale/Scope**: 2 widget types (`SingleCoinWidget`, `QuickCoinsWidget`), each supporting an
unbounded number of independently configured home-screen instances; 2 configuration Activities; no
new screens inside the main app beyond an existing-coin deep link.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Result |
|---|---|---|
| I. Simplicity First | Each widget has one job (Single: show+flip one coin; Quick: show+flip several). No widget-specific settings UI beyond picking coin(s); everything else (weights, avoid-last-result, editing) stays in-app. | PASS |
| II. Decision First | Widget flip is a single, atomic select-and-record action presented as settled (FR-008); a second flip is available but visually secondary (FR-009), mirroring the app's "override is deliberate, not the default path" rule. | PASS |
| III. Local First | No network calls; widget config and all reads/writes go through the existing local Room-backed `CoinRepository`. | PASS |
| IV. Modern Android | Uses Jetpack Glance (current official widget framework), not deprecated `RemoteViews`/`AppWidgetProvider` hand-rolling. | PASS |
| V. Consistent Design | Widgets reuse the Catppuccin Mocha palette via a small Glance color-provider mirror of `ui/theme/Color.kt`; no ad-hoc widget-only colors. | PASS |
| VI. Testable Behavior | Config resolution, favorites fallback, and single-flight flip-guard logic are plain Kotlin, unit-testable outside Glance; decision math is the existing, already-tested `selectChoice`. | PASS |
| VII. Minimal Dependencies | One new Jetpack (AndroidX) dependency (`glance-appwidget`); no other third-party libraries; justified above. | PASS |
| VIII. Incremental Development | Delivery follows the spec's own P1→P4 story order: Single Coin Widget core flip first, then Quick Coins + rule parity, then app sync + deep link, then multi-instance hardening. | PASS |

No violations. Complexity Tracking is not needed.

**Post-Phase 1 re-check**: Design artifacts (`research.md`, `data-model.md`, `contracts/`,
`quickstart.md`) introduce one new Jetpack dependency (`glance-appwidget`, justified above), no new
persistence technology beyond what that dependency already provides, and no new decision logic —
they resolve every open question by reusing existing `CoinRepository`/`domain` code paths. The
table above still holds unchanged; no new violations were introduced during design.

## Project Structure

### Documentation (this feature)

```text
specs/003-android-widgets/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   ├── widget-provider-contract.md
│   ├── widget-configuration-contract.md
│   └── widget-action-contract.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

Single native Android app module (existing `app/`); no new Gradle module. This feature adds one
new package, `widget/`, alongside the existing `data/`, `domain/`, and `ui/` packages, plus a
handful of `res/xml` and `res/values` resources and two `AndroidManifest.xml` entries.

```text
app/src/main/kotlin/com/choice/app/
├── data/                          # unchanged (CoinRepository, Room)
├── domain/                        # unchanged (selectChoice, flipCoin, Coin, Decision, ...)
├── ui/                            # unchanged (existing screens/viewmodels)
└── widget/                        # NEW — this feature
    ├── SingleCoinWidget.kt            # GlanceAppWidget: content for one coin
    ├── SingleCoinWidgetReceiver.kt    # GlanceAppWidgetReceiver
    ├── SingleCoinWidgetConfigActivity.kt
    ├── QuickCoinsWidget.kt            # GlanceAppWidget: content for several coins
    ├── QuickCoinsWidgetReceiver.kt    # GlanceAppWidgetReceiver
    ├── QuickCoinsWidgetConfigActivity.kt
    ├── WidgetConfigStore.kt          # per-instance Preferences read/write (coin id / coin ids)
    ├── WidgetActions.kt              # ActionCallback: flip, reconfigure, open-in-app
    ├── WidgetRefreshCoordinator.kt   # observes CoinRepository, calls updateAll() on change
    └── WidgetGlanceTheme.kt          # Catppuccin Mocha ColorProviders for Glance

app/src/main/res/
├── xml/
│   ├── single_coin_widget_info.xml   # AppWidgetProviderInfo
│   └── quick_coins_widget_info.xml   # AppWidgetProviderInfo
└── values/strings.xml                # + widget labels/descriptions (existing file, extended)

app/src/test/kotlin/com/choice/app/widget/
├── WidgetConfigStoreTest.kt
├── QuickCoinsDefaultSelectionTest.kt
└── WidgetFlipGuardTest.kt
```

**Structure Decision**: Extend the existing single-module Android app (`app/`) with one new
package (`widget/`) that depends on the existing `data`/`domain` layers exactly the way `ui/`
already does, plus a new `CoinViewModelFactory`-style access point is unnecessary — widget code
reaches `CoinRepository` the same way `MainActivity` does today, via
`(context.applicationContext as ChoiceApplication).appContainer.coinRepository`. No new module,
no new persistence technology beyond what Glance already requires.

## Complexity Tracking

*No Constitution Check violations — this section is intentionally empty.*
