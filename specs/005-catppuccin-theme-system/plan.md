# Implementation Plan: Catppuccin Theme System

**Branch**: `005-catppuccin-theme-system` | **Date**: 2026-08-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-catppuccin-theme-system/spec.md`

## Summary

Replace Choice's single hardcoded Catppuccin Mocha theme with all four official Catppuccin
flavors (Latte, Frappé, Macchiato, Mocha), selectable from a new "Appearance" section in the
existing Settings screen, applied instantly app-wide via Compose recomposition (no activity
recreate), persisted in the same independent `SharedPreferences` file the language preference
already uses, and propagated to both home-screen widgets through the existing widget-refresh
mechanism. Mocha remains the default for all existing and new users, with zero change to
typography, layout, decision behavior, or any Room-persisted data.

Technical approach, from [research.md](./research.md):

- **One color-role formula, four palettes.** The exact `ColorScheme`-building formula the app
  already uses for Mocha (`primary = Mauve, background = Base, surface = Mantle, error = Red, …`)
  is parameterized over a `CatppuccinPalette` and run once per flavor — because each flavor's own
  `Base`/`Text` pair is independently designed by upstream Catppuccin for mutual contrast, the
  identical formula produces a self-consistent, readable scheme for all four with no per-flavor
  special-casing.
- **New roles, not new APIs.** FR-009's success/warning/informational roles become one small
  `ChoiceExtendedColors` extension (mirroring how `error`/`onError` already work); "disabled text"
  uses Material3's standard `onSurface` @ 38%-alpha convention rather than a new stored color.
- **Persistence reuses the existing prefs file.** A new `theme_flavor` key lives in the same
  `choice_app_prefs` `SharedPreferences` file the language preference already uses — independent of
  `ChoiceDatabase` and `WidgetConfigStore`, per FR-012, with no new dependency.
- **App-wide apply is pure Compose state**, simpler than the language feature's activity-recreate
  path: a `StateFlow<ThemeFlavor>` seeded synchronously at process start, collected once above
  `ChoiceNavHost`, so every screen (which already reads only `MaterialTheme.colorScheme.*`)
  recomposes for free.
- **Widgets reuse the existing refresh coordinator.** `WidgetGlanceTheme`'s six hardcoded
  `ColorProvider`s become a per-flavor lookup; the theme-change flow calls the same
  `WidgetRefreshCoordinator.refreshAll()` already wired for language changes — no new refresh
  plumbing.
- **Settings gains a section, not a screen.** The four theme rows extend the existing single
  Settings screen/route, following the same selectable-radio-row pattern the language options
  already use.
- **Contrast is a unit test, not a design review.** Every FR-009 role pair, across all four
  flavors, is fixed at compile time — a JVM test computes WCAG contrast ratios and asserts SC-004's
  thresholds automatically.

**No Room migration, no schema change, no new Gradle dependency.** Theme is presentation state,
structurally isolated from user content — the same separation that makes FR-012/SC-006 true by
construction rather than by careful coding.

## Technical Context

**Language/Version**: Kotlin 2.0.21, JVM target 17

**Primary Dependencies**: Jetpack Compose (BOM 2024.12.01) + Material 3, Navigation Compose 2.8.5,
Room 2.6.1 (KSP), Glance AppWidget 1.1.1, `androidx.core:core-ktx` 1.15.0, kotlinx-coroutines 1.9.0.
**This feature adds none** — everything needed (`SharedPreferences`, `StateFlow`,
`CompositionLocal`, Material3's `lightColorScheme`/`darkColorScheme`, Glance's `ColorProvider`) is
already a transitive or direct dependency.

**Storage**: Room (`ChoiceDatabase`) for coins/choices/decisions — **unchanged**; Glance
`PreferencesGlanceStateDefinition` for per-instance widget config — **unchanged**; one new
`SharedPreferences` key (`choice_app_prefs` / `theme_flavor`) for the theme preference, alongside
the existing `language_tag` key in the same file.

**Testing**: JUnit 4 + `kotlinx-coroutines-test` (JVM, `app/src/test`) for `resolveThemeFlavor`,
the WCAG contrast-ratio table, and `WidgetGlanceTheme.colorsFor` mapping; Compose UI Test +
AndroidX Test (instrumented, `app/src/androidTest`) for Settings selection/persistence and the
existing `RtlLayoutTest` screen sweep, extended to the Appearance section. Android Lint
`MissingTranslation` / `ExtraTranslation` as the translation-completeness gate for the eight new
`theme_*_name`/`theme_*_description` keys.

**Target Platform**: Android, `minSdk 26`, `compileSdk`/`targetSdk 35`. Unlike the `004` locale
feature, theme switching has **no API-level branch** — it is pure Compose recomposition, not a
platform locale API, so both API tiers exercise the identical code path.

**Project Type**: Native Android application, single Gradle module (`:app`).

**Performance Goals**: No measurable regression on app start or screen render. Theme resolution is
one synchronous `SharedPreferences` read at process start (same cost class as the existing language
read). A theme change triggers zero activity recreation and one `updateAll` per widget type — no
polling, no periodic refresh, no additional recomposition beyond the one already required to show
the new colors.

**Constraints**: Fully offline (Principle III) — nothing here touches the network. No new
dependency (Principle VII). Typography, layout, and decision behavior unchanged in all four themes
(FR-011, Principle V). No Room access on the theme-change path (FR-012, Principle III boundary).
Adding a fifth flavor must require new content only — one palette instance, one enum entry, two
string keys — never a logic change (FR-017).

**Scale/Scope**: 4 color flavors; 1 existing Settings screen gains one new section (no new route);
9 existing Compose screens + all dialogs/bottom sheets consume the change automatically via
`MaterialTheme.colorScheme` (no per-screen code change expected); 2 Glance widgets + 2 widget
config activities gain a per-flavor color lookup; 8 new string keys (4 names + 4 descriptions),
mirrored in `values-ar/`; 26 colors × 3 new palettes (Latte, Frappé, Macchiato) added to `Color.kt`
alongside Mocha's existing 26, restructured into a shared `CatppuccinPalette` shape.

**No unresolved NEEDS CLARIFICATION items.** Both spec clarifications (2026-08-25 — swatch/chip
preview, and cold-start flash tolerance) are settled and reflected in
[research.md §4](./research.md#4-applying-the-selected-theme-to-the-running-app-without-restart)
and [contracts/theme-contract.md §7](./contracts/theme-contract.md#7-settings-appearance-section).

## Constitution Check

*GATE: evaluated against Choice Constitution v1.0.0 before Phase 0, and re-evaluated after Phase 1
design (both results recorded below).*

| # | Principle | Pre-Phase 0 | Post-Phase 1 | Notes |
|---|---|---|---|---|
| I | Simplicity First | ✅ PASS | ✅ PASS | One new Settings section, one new concept ("appearance theme"), no speculative UI. Success/warning/info roles are added because FR-009 requires the structure, not because new UI is being built to use them today. |
| II | Decision First | ✅ PASS | ✅ PASS | No change to decision flow, outcome finality, or re-roll semantics. Purely presentational. |
| III | Local First | ✅ PASS | ✅ PASS | Zero network involvement. Theme preference is on-device `SharedPreferences`, resolved synchronously offline. |
| IV | Modern Android | ✅ PASS | ✅ PASS | Material3 `ColorScheme`/`CompositionLocal`, Compose recomposition, Glance `ColorProvider` — all current, non-deprecated APIs. No activity-recreate hack introduced where recomposition suffices. |
| V | Consistent Design | ✅ PASS | ✅ PASS | This feature *is* the shared theming layer becoming more capable, not a departure from it — a single `buildColorScheme`/`buildExtendedColors` formula remains the one source of color for every screen and both widgets, across all four flavors. Typography untouched (FR-011). |
| VI | Testable Behavior | ✅ PASS | ✅ PASS | `resolveThemeFlavor`, the WCAG contrast table, and `WidgetGlanceTheme.colorsFor` are pure functions with JVM unit tests. UI selection/persistence gets an instrumented test mirroring the existing language-selection test. |
| VII | Minimal Dependencies | ✅ PASS | ✅ PASS | No new dependency at any point — confirmed in [research.md](./research.md) (SharedPreferences over DataStore, reused refresh coordinator over WorkManager). |
| VIII | Incremental Development | ✅ PASS | ✅ PASS | The three user stories are independently shippable in priority order; US1 (in-app selection) alone is a complete, demonstrable slice. Widgets (US3) explicitly depend on US1's mechanism and ship last. |

**Technology Constraints**: native Android only ✅ · Kotlin ✅ · on-device storage, no core-path
network ✅ · AndroidX/Jetpack default with no third-party addition ✅ · shared Catppuccin
theme/token layer remains the single source of colors, now covering four flavors instead of one ✅.

**Development Workflow**: test-backed core logic ✅ (resolution + contrast-ratio math are pure and
unit-tested) · MVP-first scoping ✅ (story-ordered: selection → persistence → widgets) · design
review ✅ (no ad hoc per-screen styling introduced; the review surface is the one shared formula) ·
principle check ✅ (this section).

**Gate result: PASS — no violations, no justified deviations.** The Complexity Tracking table
below is therefore empty.

### Design decisions worth flagging for review

Not violations, but choices a reviewer should agree with:

1. **Reusing the `choice_app_prefs` file for the theme key**, rather than a dedicated
   `SharedPreferences` file. The isolation FR-012 actually requires is "never touches
   `ChoiceDatabase` or `WidgetConfigStore`," which holds either way; a second key in the existing
   small prefs file is simpler than a second file for the same purpose.
   ([research.md §3](./research.md#3-persistence-mechanism-precedent-language-preference))
2. **Introducing `ChoiceExtendedColors` for success/warning/info** even though no current screen
   renders any of the three. FR-009 requires the *role structure* to exist across all four flavors
   now, as part of making them "functionally equivalent" — this is a design-system completeness
   requirement, not speculative feature work.
   ([data-model.md §4](./data-model.md#4-choiceextendedcolors))
3. **No activity recreate on theme change**, unlike the language feature's API 26–32 path. Theme
   color is pure Compose state with no `Configuration`/`Resources` involvement, so recomposition
   alone is sufficient and is what FR-007 ("without restarting... or navigating away and back")
   actually asks for.
   ([research.md §4](./research.md#4-applying-the-selected-theme-to-the-running-app-without-restart))
4. **Widget config activities (`SingleCoinWidgetConfigActivity`, `QuickCoinsWidgetConfigActivity`)
   read the theme once, synchronously, rather than observing live changes.** They are short-lived,
   one-shot configuration UIs — a theme change made in the main app while one happens to be open on
   screen is not a scenario the spec's acceptance criteria require handling live, unlike the main
   app's `NavHost`.
   ([contracts/theme-contract.md §4](./contracts/theme-contract.md#4-app-wide-application-no-restart))

## Project Structure

### Documentation (this feature)

```text
specs/005-catppuccin-theme-system/
├── plan.md                                # This file (/speckit-plan command output)
├── spec.md                                # Feature specification (input)
├── research.md                            # Phase 0 output — 7 resolved decisions
├── data-model.md                          # Phase 1 output — entities + validation rules
├── quickstart.md                          # Phase 1 output — validation guide
├── contracts/                             # Phase 1 output
│   ├── theme-contract.md                  #   flavors, resolution, persistence, app-wide apply, Settings section
│   └── widget-theme-contract.md           #   widget color lookup + refresh reuse
├── checklists/
│   └── requirements.md                    # Spec quality checklist (already passed)
└── tasks.md                               # Phase 2 output (/speckit-tasks — NOT created by /speckit-plan)
```

### Source Code (repository root)

Single-module native Android app. `[NEW]` / `[MOD]` mark what this feature touches; everything
unmarked is context.

```text
app/src/main/
├── kotlin/com/choice/app/
│   ├── ChoiceApplication.kt                    # [MOD] construct themePreferenceStore + AppThemeState
│   ├── MainActivity.kt                         # [MOD] collectAsState(); ChoiceTheme(flavor) { ChoiceNavHost }
│   ├── ui/theme/
│   │   ├── Color.kt                            # [MOD] CatppuccinPalette + 4 palette instances (Latte/Frappé/Macchiato new; Mocha migrated, unchanged values)
│   │   ├── Theme.kt                            # [MOD] ChoiceTheme(flavor, content); buildColorScheme(palette)
│   │   ├── ThemeFlavor.kt                      # [NEW] enum + swatchColors + fromId/DEFAULT
│   │   ├── ThemePreference.kt                  # [NEW] pure resolveThemeFlavor(storedId)
│   │   ├── ThemePreferenceStore.kt              # [NEW] interface + SharedPreferencesThemeStore
│   │   ├── AppThemeState.kt                    # [NEW] StateFlow<ThemeFlavor> holder
│   │   ├── ExtendedColors.kt                   # [NEW] ChoiceExtendedColors + buildExtendedColors + CompositionLocal
│   │   └── Type.kt                             #       unchanged (FR-011)
│   ├── ui/settings/
│   │   ├── SettingsScreen.kt                   # [MOD] + "Appearance" section (4 rows: name, description, swatch, radio)
│   │   └── SettingsViewModel.kt                # [MOD] + selectTheme(flavor), same shape as selectLanguage
│   ├── ui/components/
│   │   └── ThemeOptionRow.kt                   # [NEW] shared row composable (name + description + swatch + radio)
│   └── widget/
│       ├── WidgetGlanceTheme.kt                 # [MOD] colorsFor(flavor) instead of 6 static ColorProviders
│       ├── SingleCoinWidget.kt                  # [MOD] resolve flavor in provideGlance; CompositionLocalProvider(LocalWidgetColors)
│       ├── QuickCoinsWidget.kt                  # [MOD] same
│       ├── SingleCoinWidgetConfigActivity.kt    # [MOD] ChoiceTheme(resolveThemeFlavor(...)) instead of ChoiceTheme { }
│       ├── QuickCoinsWidgetConfigActivity.kt    # [MOD] same
│       └── WidgetRefreshCoordinator.kt          #       unchanged — refreshAll() reused as-is
└── res/
    ├── values/strings.xml                       # [MOD] + 8 keys: theme_{latte,frappe,macchiato,mocha}_{name,description}; settings_appearance_label
    └── values-ar/strings.xml                    # [MOD] + same 9 keys, Arabic

app/src/test/kotlin/com/choice/app/            # JVM — pure logic
├── ui/theme/ThemeFlavorTest.kt                  # [NEW] resolveThemeFlavor table (8 rows, contracts/theme-contract.md §2)
├── ui/theme/ThemeContrastTest.kt                # [NEW] WCAG ratio per FR-009 role × 4 flavors (SC-004)
└── widget/WidgetGlanceThemeTest.kt              # [NEW] colorsFor mapping table, incl. Mocha-unchanged assertion

app/src/androidTest/kotlin/com/choice/app/     # Instrumented — needs a rendered frame
├── ui/settings/SettingsScreenTest.kt             # [MOD] + theme selection persists; reselect is no-op (mirrors language tests)
└── locale/RtlLayoutTest.kt                       # [MOD] + Appearance section added to the swept screen list
```

**Structure Decision**: The existing single-module `:app` layout is kept as-is. All new files live
inside the already-established `ui/theme/` package (which currently holds only `Color.kt`,
`Theme.kt`, `Type.kt`) — extending it, not adding a sibling package, since this is squarely the
existing theming layer growing from one flavor to four. `ThemeOptionRow` goes in `ui/components/`
alongside `EmptyState.kt` and `DeleteCoinConfirmationDialog.kt`, the established home for
composables shared across screens. No new Gradle module, no new top-level package, no navigation
graph change.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**Not applicable — the Constitution Check passed with no violations.**
