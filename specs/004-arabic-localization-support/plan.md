# Implementation Plan: Choice Localization and Multilingual Support

**Branch**: `004-arabic-localization-support` | **Date**: 2026-08-24 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-arabic-localization-support/spec.md`

## Summary

Make Choice fully usable in English and Arabic by moving **every** user-facing string — app and
widgets alike — into Android's standard resource mechanism, adding a complete Arabic translation,
and letting the platform derive right-to-left layout from the active locale rather than mirroring
anything by hand.

Technical approach, from [research.md](./research.md):

- **Language selection** uses the platform `LocaleManager` on API 33+ with a small in-repo
  `createConfigurationContext` backport for API 26–32. The tag lives in `SharedPreferences` (a
  synchronous read is required before the first Activity attaches). **No new Gradle dependency.**
- **A new app-level Settings screen** hosts the in-app language selector — Choice has no app-wide
  settings surface today (`CoinSettingsScreen` is per-coin).
- **Coin templates** gain stable, non-localized IDs and move their name/choices into
  `<string>`/`<string-array>` resources. This forces one breaking navigation change: the
  `coinedit` route argument becomes `templateId` instead of the now-localized `templateName`.
- **Dates and percentages** move from a hardcoded `SimpleDateFormat` pattern to CLDR-driven
  `java.time` formatting on a locale carrying the `nu-latn` Unicode extension — Arabic ordering and
  wording, Western Arabic digits, per the 2026-08-24 clarification.
- **Widgets** render through a localized `Context` and relabel immediately on any language change,
  driven by the existing `WidgetRefreshCoordinator` plus a manifest-declared
  `ACTION_LOCALE_CHANGED` receiver.

**No Room migration and no schema change.** Language is presentation; it never touches persisted
user content. That separation is what makes FR-025 and SC-004 structurally true rather than
carefully coded.

## Technical Context

**Language/Version**: Kotlin 2.0.21, JVM target 17

**Primary Dependencies**: Jetpack Compose (BOM 2024.12.01) + Material 3, Navigation Compose 2.8.5,
Room 2.6.1 (KSP), Glance AppWidget 1.1.1, `androidx.core:core-ktx` 1.15.0, kotlinx-coroutines 1.9.0.
**This feature adds none** — `BidiFormatter` comes from the existing `core-ktx`; `LocaleManager`,
`SharedPreferences`, and `java.time` are platform APIs available at `minSdk 26`.

**Storage**: Room (`ChoiceDatabase`) for coins/choices/decisions — **unchanged**; Glance
`PreferencesGlanceStateDefinition` for widget config — **unchanged**; one new `SharedPreferences`
key (`choice_app_prefs` / `language_tag`) for the language preference.

**Testing**: JUnit 4 + `kotlinx-coroutines-test` + `room-testing` (JVM, `app/src/test`);
Compose UI Test + Espresso + AndroidX Test (instrumented, `app/src/androidTest`); Android Lint
`MissingTranslation` / `ExtraTranslation` as the translation-completeness gate.

**Target Platform**: Android, `minSdk 26`, `compileSdk`/`targetSdk 35`. Both API tiers must be
validated — the locale mechanism takes a different path above and below API 33.

**Project Type**: Native Android application, single Gradle module (`:app`).

**Performance Goals**: No measurable regression on app start or screen render. Locale resolution is
one synchronous `SharedPreferences` read at process start. A language change triggers one activity
recreation plus one `updateAll` per widget type — no polling, no periodic refresh.

**Constraints**: Fully offline (Principle III) — nothing here touches the network. No new
dependency (Principle VII). Existing Catppuccin Mocha visuals preserved unchanged in both languages
(FR-028, Principle V). No manual RTL mirroring (FR-005). Adding a third language must require new
content only, never a logic change (FR-029).

**Scale/Scope**: 2 languages; 9 Compose screens (8 existing + 1 new Settings) and 2 Glance widgets
plus 2 widget config activities; 18 existing string keys plus ~79 currently-hardcoded literals to
externalize (~95 keys total, mirrored in `values-ar/`); 4 coin templates; 14 existing instrumented
screen tests to update.

**No unresolved NEEDS CLARIFICATION items.** The three spec clarifications (2026-08-24) settled
in-app selector, digit system, and widget refresh timing; the locale-API choice was confirmed with
the user on 2026-08-24 and is recorded in [research.md §1](./research.md).

## Constitution Check

*GATE: evaluated against Choice Constitution v1.0.0 before Phase 0, and re-evaluated after Phase 1
design (both results recorded below).*

| # | Principle | Pre-Phase 0 | Post-Phase 1 | Notes |
|---|---|---|---|---|
| I | Simplicity First | ✅ PASS | ✅ PASS | Adds exactly one screen, carrying exactly one setting. No speculative settings, no new user-facing concepts. Language selection is a single-purpose screen a user can describe in one sentence. |
| II | Decision First | ✅ PASS | ✅ PASS | No change to decision flow, outcome finality, or re-roll semantics. Purely presentational. |
| III | Local First | ✅ PASS | ✅ PASS | Zero network involvement. Translations ship as APK resources; the preference is on-device. Fully functional offline. |
| IV | Modern Android | ✅ PASS | ✅ PASS | Platform `LocaleManager`, `locales_config.xml`, native RTL, `java.time`, `Icons.AutoMirrored.*`, Compose logical modifiers. No deprecated APIs. The API 26–32 backport uses the supported `createConfigurationContext` path. |
| V | Consistent Design | ✅ PASS | ✅ PASS | No color, type, spacing, or component change (FR-028). `WidgetGlanceTheme` untouched. The new Settings screen reuses the existing `Scaffold` + `TopAppBar` + `Card` pattern. |
| VI | Testable Behavior | ✅ PASS | ✅ PASS | Every branching rule — locale fallback, digit system, date shape, search matching, template identity — is a pure function tested on the JVM. Instrumented tests are reserved for what needs a rendered frame. |
| VII | Minimal Dependencies | ⚠️ AT RISK | ✅ PASS | The obvious route (`androidx.appcompat`) would add a dependency and re-parent the app theme. Resolved in [research.md §1](./research.md) with a zero-dependency platform + in-repo-backport design. Confirmed with the user. |
| VIII | Incremental Development | ✅ PASS | ✅ PASS | The five user stories are independently shippable in priority order; US1+US2 form the minimum useful slice. The Settings screen ships language-only. |

**Technology Constraints**: native Android only ✅ · Kotlin ✅ · on-device storage, no core-path
network ✅ · AndroidX/Jetpack default with no third-party addition ✅ · shared Catppuccin Mocha
theme layer untouched ✅.

**Development Workflow**: test-backed core logic ✅ (new pure functions carry unit tests; the 14
existing instrumented tests are updated as part of the work, not deferred) · MVP-first scoping ✅
(story-ordered) · design review ✅ (no visual change to review beyond one new screen following
existing patterns) · principle check ✅ (this section).

**Gate result: PASS — no violations, no justified deviations.** The Complexity Tracking table below
is therefore empty. The one at-risk item (VII) was resolved by design rather than by exception.

### Design decisions worth flagging for review

Not violations, but choices a reviewer should agree with:

1. **A new Settings screen.** FR-026 says "within Choice's settings" and no app-level settings
   surface exists. Adding one is the smallest correct answer; putting an app-wide preference inside
   the per-coin `CoinSettingsScreen` would be a category error for users. ([research.md §10](./research.md))
2. **A breaking navigation-argument change.** `templateName` → `templateId` is required, not
   cosmetic: a name-based lookup silently yields a blank editor once names are localized.
   ([contracts/navigation-contract.md §1](./contracts/navigation-contract.md))
3. **Updating the 14 existing instrumented tests is in scope.** They assert against hardcoded
   English literals and will fail once strings move to resources. If they still pass unmodified,
   externalization is incomplete.
4. **`app_name` stays untranslated** (`translatable="false"`) — a brand name, and the single
   intentional exception to the "every key has an Arabic counterpart" rule.

## Project Structure

### Documentation (this feature)

```text
specs/004-arabic-localization-support/
├── plan.md                                    # This file (/speckit-plan output)
├── spec.md                                    # Feature specification (input)
├── research.md                                # Phase 0 output — 13 resolved decisions
├── data-model.md                              # Phase 1 output — entities + validation rules
├── quickstart.md                              # Phase 1 output — validation guide
├── contracts/                                 # Phase 1 output
│   ├── localization-contract.md               #   locale resolution, resources, formatting, RTL
│   ├── navigation-contract.md                 #   templateId route change + Settings destination
│   └── widget-localization-contract.md        #   widget strings, RTL, immediate refresh
└── tasks.md                                   # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

Single-module native Android app. `[NEW]` / `[MOD]` mark what this feature touches; everything
unmarked is context.

```text
app/src/main/
├── AndroidManifest.xml                        # [MOD] android:localeConfig + LOCALE_CHANGED receiver
├── kotlin/com/choice/app/
│   ├── ChoiceApplication.kt                   # [MOD] apply locale on start; expose refreshAll()
│   ├── MainActivity.kt                        # [MOD] attachBaseContext; templateId route; settings route
│   ├── locale/                                # [NEW] the entire locale layer
│   │   ├── SupportedLanguages.kt              # [NEW] TAGS + DEFAULT — the FR-029 extension point
│   │   ├── LocalePreference.kt                # [NEW] pure resolveLanguageTag / uiLocale / formattingLocale
│   │   ├── LanguagePreferenceStore.kt         # [NEW] SharedPreferences read/write
│   │   └── LocaleApplier.kt                   # [NEW] LocaleManager (33+) / context wrap (26–32)
│   ├── domain/
│   │   ├── CoinTemplates.kt                   # [MOD] id + @StringRes/@ArrayRes
│   │   └── CoinSearch.kt                      #       unchanged — already Unicode-correct
│   ├── ui/
│   │   ├── format/DecisionTimeFormatter.kt    # [NEW] shared, locale-parameterized, replaces 2 copies
│   │   ├── settings/                          # [NEW]
│   │   │   ├── SettingsScreen.kt              # [NEW] language selector (only)
│   │   │   └── SettingsViewModel.kt           # [NEW]
│   │   ├── main/MainScreen.kt                 # [MOD] strings + settings entry point
│   │   ├── coinedit/CoinEditScreen.kt         # [MOD] strings
│   │   ├── coinflip/CoinFlipScreen.kt         # [MOD] strings
│   │   ├── coinsettings/CoinSettingsScreen.kt # [MOD] strings
│   │   ├── history/HistoryScreen.kt           # [MOD] strings + shared formatter
│   │   ├── statistics/StatisticsScreen.kt     # [MOD] strings + shared formatter
│   │   ├── templates/TemplatePickerScreen.kt  # [MOD] strings + templateId callback
│   │   ├── share/ImportCoinScreen.kt          # [MOD] strings
│   │   └── components/                        # [MOD] EmptyState, DeleteCoinConfirmationDialog
│   └── widget/
│       ├── SingleCoinWidget.kt                # [MOD] localized context; %s placeholders; bidi
│       ├── QuickCoinsWidget.kt                # [MOD] localized context; %s placeholders; bidi
│       ├── SingleCoinWidgetConfigActivity.kt  # [MOD] attachBaseContext; plurals
│       ├── QuickCoinsWidgetConfigActivity.kt  # [MOD] attachBaseContext
│       ├── WidgetRefreshCoordinator.kt        # [MOD] extract refreshAll()
│       └── LocaleChangedReceiver.kt           # [NEW] ACTION_LOCALE_CHANGED → refreshAll()
└── res/
    ├── values/strings.xml                     # [MOD] 18 keys → ~95 (English default set)
    ├── values-ar/strings.xml                  # [NEW] complete Arabic overlay
    └── xml/locales_config.xml                 # [NEW] <locale> en, ar

app/src/test/kotlin/com/choice/app/           # JVM — pure logic
├── locale/LocalePreferenceTest.kt             # [NEW] resolution table incl. FR-027 row 8
├── ui/format/DecisionTimeFormatterTest.kt     # [NEW] en vs ar; digits always 0-9
├── domain/CoinSearchTest.kt                   # [MOD] + Arabic / mixed-script cases
└── domain/CoinTemplatesTest.kt                # [MOD] + stable-id assertions

app/src/androidTest/kotlin/com/choice/app/    # Instrumented — needs a rendered frame
├── locale/LocaleSwitchDataIntegrityTest.kt    # [NEW] SC-004: data byte-identical across switches
├── locale/RtlLayoutTest.kt                    # [NEW] SC-002: direction + no clipping at ~320dp
├── ui/settings/SettingsScreenTest.kt          # [NEW] selector persistence + apply
└── ui/**/*Test.kt (14 existing)               # [MOD] resolve strings via getString — required
```

**Structure Decision**: The existing single-module `:app` layout is kept as-is. The only structural
addition is a `locale/` package holding the four new locale-layer files, and a `ui/settings/`
package for the new screen — both following the established
`ui/<feature>/<Screen>.kt` + `<Screen>ViewModel.kt` convention already used by all eight screens.
`DecisionTimeFormatter` goes in `ui/format/` because it is a presentation concern, not domain logic;
placing it there also collapses the duplicated `formatTimestamp()` helpers currently sitting in
`HistoryScreen.kt:126` and `StatisticsScreen.kt:224`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**Not applicable — the Constitution Check passed with no violations.**

The one principle initially flagged at risk (VII, Minimal Dependencies) was resolved by design
rather than by exception: the chosen locale mechanism adds no dependency. See
[research.md §1](./research.md) for the rejected `androidx.appcompat` alternative and why the
zero-dependency path gives identical coverage.
