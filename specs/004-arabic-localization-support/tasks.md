---

description: "Task list for Choice Localization and Multilingual Support"
---

# Tasks: Choice Localization and Multilingual Support

**Input**: Design documents from `/specs/004-arabic-localization-support/`
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: Included — the spec's Success Criteria are measurable outcomes (SC-001…SC-010) that
require automated coverage, and Constitution Principle VI requires core logic to be independently
tested. Every pure decision (locale fallback, digit system, formatting, search) gets a JVM unit
test; every rendered/RTL/accessibility behavior gets an instrumented test; the 14 pre-existing
instrumented screen tests **must** be updated as part of this work (they assert hardcoded English
literals today and will fail once strings move to resources — see quickstart.md §1).

**Organization**: Tasks are grouped by user story (P1–P5, from spec.md), so each story is
independently implementable and testable per the Independent Test in its phase header.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1–US5, mapping to spec.md's five prioritized user stories
- Every task names its exact file path(s)

## Path Conventions

Single Android module. All main sources under `app/src/main/kotlin/com/choice/app/`, resources
under `app/src/main/res/`, JVM tests under `app/src/test/kotlin/com/choice/app/`, instrumented
tests under `app/src/androidTest/kotlin/com/choice/app/` — per plan.md's Project Structure.

---

## Phase 1: Setup

**Purpose**: Resource scaffolding and build configuration that every later phase writes into.

- [X] T001 [P] Create `app/src/main/res/values-ar/strings.xml` with an empty `<resources>` root and a header comment noting it must mirror `values/strings.xml` key-for-key (contracts/localization-contract.md §5.1)
- [X] T002 [P] Create `app/src/main/res/xml/locales_config.xml` declaring `<locale android:name="en"/>` and `<locale android:name="ar"/>` (contracts/localization-contract.md §5.1, FR-024)
- [X] T003 Add `android:localeConfig="@xml/locales_config"` to the `<application>` element in `app/src/main/AndroidManifest.xml` (FR-024)
- [X] T004 [P] Enable Android Lint `MissingTranslation`/`ExtraTranslation` as build-blocking checks in the `android { lint { ... } }` block of `app/build.gradle.kts` (contracts/localization-contract.md §5.2 SR-2/SR-7; quickstart.md §1)
- [X] T005 [P] Mark `app_name` `translatable="false"` in `app/src/main/res/values/strings.xml` — the one intentional exception to "every key has an Arabic counterpart" (contracts/widget-localization-contract.md §2.1)

**Checkpoint**: resource files and build config exist; every later phase can add keys to them.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The locale-resolution/application mechanism — the one piece of genuinely shared
infrastructure in this feature. **US2, US4, and US5 build directly on it and cannot start their
locale-dependent tasks until it lands. US1 and US3 do not depend on it at all** (US1's independent
test only needs the device's system language plus resource coverage; US3 only needs stable
template IDs) and may proceed in parallel with this phase.

- [X] T006 [P] Create `SupportedLanguages` object (`ENGLISH="en"`, `ARABIC="ar"`, `DEFAULT=ENGLISH`, `TAGS=[ENGLISH, ARABIC]`) in `app/src/main/kotlin/com/choice/app/locale/SupportedLanguages.kt` (contracts/localization-contract.md §1 — the FR-029 extension point)
- [X] T007 [P] Create pure functions `resolveLanguageTag(storedTag, systemLanguageTags)`, `uiLocale(tag)`, `formattingLocale(tag)` (with the `nu-latn` Unicode extension) in `app/src/main/kotlin/com/choice/app/locale/LocalePreference.kt` (contracts/localization-contract.md §2, research.md §2; depends on T006)
- [X] T008 [P] Add `app/src/test/kotlin/com/choice/app/locale/LocalePreferenceTest.kt` covering the full 9-row resolution table from contracts/localization-contract.md §2 (including row 8's FR-027 stored-but-unsupported-tag case) plus a `formattingLocale("ar").toLanguageTag() == "ar-u-nu-latn"` assertion (depends on T007)
- [X] T009 [P] Create `LanguagePreferenceStore` — synchronous `read(): String?` / `write(tag: String?)` over `SharedPreferences("choice_app_prefs", MODE_PRIVATE)`, key `"language_tag"` — in `app/src/main/kotlin/com/choice/app/locale/LanguagePreferenceStore.kt` (contracts/localization-contract.md §3)
- [X] T010 Create `LocaleApplier` — `apply(context, tag)` using `LocaleManager.applicationLocales` on API 33+ and persisting for a caller-driven recreate on API 26–32; `localizedContext(context)` using `createConfigurationContext` on API 26–32 and returning `context` unchanged on API 33+ — in `app/src/main/kotlin/com/choice/app/locale/LocaleApplier.kt` (contracts/localization-contract.md §4; depends on T006, T007)
- [X] T011 [P] Add `app/src/androidTest/kotlin/com/choice/app/locale/LocaleApplierTest.kt` asserting `localizedContext` resolves Arabic resources correctly and is idempotent (LA-1) — run on both an API 33+ and an API 26–32 device per quickstart.md prerequisites (depends on T010)
- [X] T012 Wire `LocaleApplier.apply(context, resolveLanguageTag(LanguagePreferenceStore.read(), systemTags))` into `ChoiceApplication.onCreate`, before `AppContainer` is constructed, in `app/src/main/kotlin/com/choice/app/ChoiceApplication.kt` (depends on T009, T010)

**Checkpoint**: locale mechanism ready. US2, US4, US5 can now proceed; US1/US3 need nothing from
here and may already be underway.

---

## Phase 3: User Story 1 - Use Choice Fully in Arabic with Correct RTL Layout (Priority: P1) 🎯 MVP

**Goal**: Home, coin creation/editing, the decision (flip) flow, and per-coin settings render fully
in Arabic with correct right-to-left layout, using only the device's system language — no in-app
selector required yet.

**Independent Test**: With the device language set to Arabic, navigate home → coin creation/editing
→ decision (flip) flow → coin settings, and confirm all visible text is Arabic and layout,
alignment, and directional icons are correctly mirrored — independent of templates, search,
widgets, or history (spec.md US1).

### Implementation for User Story 1

- [X] T013 [P] [US1] Externalize `MainScreen.kt` strings — reuse `app_name` for the title; add `cd_create_coin`, `main_empty_title`, `main_empty_subtitle`, `main_action_create_coin`, `main_search_placeholder`, `cd_search`, `main_section_quick_coins`, `main_section_all_coins`, `main_no_results_title`, `main_no_results_subtitle`, `cd_add_to_quick_coins`, `cd_remove_from_quick_coins`, `cd_edit_coin`, and a `<plurals name="coin_choice_count">` for `"$choiceCount choices"` (SR-8) — into `app/src/main/res/values/strings.xml` + `app/src/main/res/values-ar/strings.xml`; update `app/src/main/kotlin/com/choice/app/ui/main/MainScreen.kt` to use `stringResource`/`pluralStringResource`
- [X] T014 [P] [US1] Externalize `DeleteCoinConfirmationDialog.kt` strings (`delete_coin_title`, `delete_coin_message` with a `%1$s` placeholder for the coin name, `delete_coin_confirm`, `delete_coin_cancel`) into both resource files; update `app/src/main/kotlin/com/choice/app/ui/components/DeleteCoinConfirmationDialog.kt`, wrapping the interpolated coin name with `BidiFormatter.getInstance().unicodeWrap(...)` (contracts/localization-contract.md §5.3)
- [X] T015 [P] [US1] Externalize `CoinEditScreen.kt` strings (`coin_edit_title_create`, `coin_edit_title_edit`, `cd_back`, `coin_edit_name_label`, `coin_edit_choice_label` with a `%1$d` index placeholder, `cd_move_choice_up`, `cd_move_choice_down`, `cd_remove_choice`, `coin_edit_add_choice`, `coin_edit_save`) into both resource files; update `app/src/main/kotlin/com/choice/app/ui/coinedit/CoinEditScreen.kt`
- [X] T016 [US1] Replace `CoinEditViewModel.kt`'s hardcoded validation messages (`"At least 2 choices are required"`, `"Name cannot be blank"`, `"Choice cannot be blank"`) with a `CoinEditError` enum/sealed type on `nameError`/`formError`/`removeError`/`ChoiceFieldState.error`, keeping validation logic untouched (Principle VI — the ViewModel stays free of Android resources); add matching `coin_edit_error_*` keys to both resource files and resolve them via `stringResource` at the call sites in `app/src/main/kotlin/com/choice/app/ui/coinedit/CoinEditScreen.kt`; update `app/src/main/kotlin/com/choice/app/ui/coinedit/CoinEditViewModel.kt`
- [X] T017 [P] [US1] Externalize `CoinFlipScreen.kt` strings (`cd_back`, `cd_share_coin`, `share_chooser_title`, `cd_decision_history`, `cd_coin_settings`, `cd_edit_coin`, `coin_flip_action_flip`, `coin_flip_your_decision`, `coin_flip_decision_accepted`, `coin_flip_action_accept`, `coin_flip_action_flip_again`) into both resource files; update `app/src/main/kotlin/com/choice/app/ui/coinflip/CoinFlipScreen.kt`
- [X] T018 [P] [US1] Externalize `CoinSettingsScreen.kt` strings (`coin_settings_title`, `cd_back`, `coin_settings_weighted_title`, `coin_settings_weighted_subtitle`, `coin_settings_avoid_last_title`, `coin_settings_avoid_last_subtitle`, `coin_settings_choice_weights_title`, `coin_settings_weight_label`, `coin_settings_weight_hint`, `coin_settings_save`, `coin_settings_share_coin`, `coin_settings_import_coin`, reusing `share_chooser_title` from T017) into both resource files; update `app/src/main/kotlin/com/choice/app/ui/coinsettings/CoinSettingsScreen.kt`
- [X] T019 [US1] Replace `CoinSettingsViewModel.kt`'s hardcoded `"Weight must be a positive whole number"` with an error-code field on `choiceWeights[i].error`, same pattern as T016; add `coin_settings_error_weight_positive` to both resource files and resolve it in `app/src/main/kotlin/com/choice/app/ui/coinsettings/CoinSettingsScreen.kt`; update `app/src/main/kotlin/com/choice/app/ui/coinsettings/CoinSettingsViewModel.kt`
- [X] T020 [US1] RTL/icon audit across the five screens touched by T013–T019: confirm every back icon still uses `Icons.AutoMirrored.Filled.ArrowBack` (already true — LD-5) and confirm no `Modifier.absolutePadding` / `Alignment.CenterStart`/`CenterEnd` / `TextAlign.Left`/`Right` was introduced (LD-2–LD-4); fix anything found
- [X] T021 [P] [US1] Update `app/src/androidTest/kotlin/com/choice/app/ui/main/MainScreenSearchTest.kt`, `MainScreenDeleteTest.kt`, `MainScreenQuickCoinsTest.kt` to resolve expected text via `context.getString(R.string.…)`/`resources.getQuantityString` instead of hardcoded English literals
- [X] T022 [P] [US1] Update `app/src/androidTest/kotlin/com/choice/app/ui/coinedit/CoinEditScreenCreateTest.kt`, `CoinEditScreenEditTest.kt`, `CoinEditScreenReorderTest.kt` to resolve expected text (including the T016 error messages) via `context.getString(R.string.…)`
- [X] T023 [P] [US1] Update `app/src/androidTest/kotlin/com/choice/app/ui/coinflip/CoinFlipScreenTest.kt`, `CoinFlipScreenAcceptOverrideTest.kt` to resolve expected text via `context.getString(R.string.…)`
- [X] T024 [P] [US1] Update `app/src/androidTest/kotlin/com/choice/app/ui/coinsettings/CoinSettingsWeightingTest.kt`, `CoinSettingsAvoidLastResultTest.kt` to resolve expected text (including the T019 error message) via `context.getString(R.string.…)`
- [ ] T025 [US1] Manually validate User Story 1 per quickstart.md §2 "US1" block on both an API 33+ and an API 26–32 device/emulator with the device system language set to Arabic, including the ~320dp narrow-width pass

**Checkpoint**: User Story 1 is fully functional and independently testable.

---

## Phase 4: User Story 2 - Switch Between English and Arabic Without Losing Data (Priority: P2)

**Goal**: An in-app language selector (Settings screen) lets a user override Choice's display
language independent of the device; every existing coin, choice, decision, and favorite survives
every language switch unchanged.

**Independent Test**: Starting from an app with existing coins, choices, favorites, and history,
switch the language via the device setting and via the new in-app option, and verify all UI text
updates while every piece of existing data is byte-for-byte unchanged (spec.md US2).

### Implementation for User Story 2

- [X] T026 [P] [US2] Create `SettingsScreen` (language-only: "System default" / "English" / "العربية", each option's own-language label per contracts/navigation-contract.md §2) in `app/src/main/kotlin/com/choice/app/ui/settings/SettingsScreen.kt` (depends on Foundational T006–T010)
- [X] T027 [P] [US2] Create `SettingsViewModel` wrapping `LanguagePreferenceStore.write` + `LocaleApplier.apply`, exposing a `refreshWidgets: () -> Unit` hook (wired to the real coordinator in T058) in `app/src/main/kotlin/com/choice/app/ui/settings/SettingsViewModel.kt` (depends on T009, T010)
- [X] T028 [US2] Add `settings_title`, `settings_language_label`, `settings_language_system`, `settings_language_english`, `settings_language_arabic`, `cd_back` to both resource files
- [X] T029 [US2] Add the `"settings"` route to `ChoiceNavHost` and a settings `IconButton` entry point in `MainScreen.kt`'s `TopAppBar`, in `app/src/main/kotlin/com/choice/app/MainActivity.kt` + `app/src/main/kotlin/com/choice/app/ui/main/MainScreen.kt` (depends on T026)
- [X] T030 [US2] Override `attachBaseContext` in `MainActivity` to apply `LocaleApplier.localizedContext` (no-op on API 33+) in `app/src/main/kotlin/com/choice/app/MainActivity.kt` (contracts/localization-contract.md §4; depends on T010)
- [X] T031 [US2] On API 26–32, recreate the current activity after a language change; rely on `LocaleManager`'s automatic recreation on API 33+ — implement in `app/src/main/kotlin/com/choice/app/ui/settings/SettingsScreen.kt` (depends on T027, T030)
- [X] T032 [P] [US2] Add `app/src/androidTest/kotlin/com/choice/app/ui/settings/SettingsScreenTest.kt`: selecting a language persists it (SN-2), re-selecting the already-active language is a no-op (SN-4), selection survives activity recreation
- [X] T033 [P] [US2] Add `app/src/androidTest/kotlin/com/choice/app/locale/LocaleSwitchDataIntegrityTest.kt`: seed coins/choices/decisions/favorites, switch languages through `SettingsScreen` (and simulate a device system-language change), assert every row is byte-identical before/after (SC-004, FR-025)
- [ ] T034 [US2] Manually validate User Story 2 per quickstart.md §2 "US2" block on both API tiers — in-app selector, device *Settings → System → Languages → App languages* (API 33+), and the unsupported-system-language (French) → English fallback case

**Checkpoint**: User Stories 1 AND 2 both work independently.

---

## Phase 5: User Story 3 - Create Coins from Localized Templates (Priority: P3)

**Goal**: Coin templates show a localized name and default choices per active language; a coin
created from a template keeps its content fixed even after a later language change.

**Independent Test**: Open the template picker in each language and confirm localized content;
create a coin from a template, switch languages, and confirm the created coin is unchanged while
the picker itself now shows the other language (spec.md US3).

### Implementation for User Story 3

- [X] T035 [P] [US3] Add per-template `<string>` + `<string-array>` resources (`template_breakfast_name`/`template_breakfast_choices`, and the same pattern for `template_lunch_*`, `template_workout_*`, `template_movie_*`) to both resource files, each array having identical item count in both languages (VR-11), per data-model.md §3
- [X] T036 [US3] Refactor `CoinTemplate` to `data class CoinTemplate(val id: String, @StringRes val nameRes: Int, @ArrayRes val choicesRes: Int)` and `CoinTemplates.templates` to the four stable IDs (`"breakfast"`, `"lunch"`, `"workout"`, `"movie"`) in `app/src/main/kotlin/com/choice/app/domain/CoinTemplates.kt` (depends on T035)
- [X] T037 [P] [US3] Update `app/src/test/kotlin/com/choice/app/domain/CoinTemplatesTest.kt` to assert on `id`/`nameRes`/`choicesRes` instead of `name`/`choices` (depends on T036)
- [X] T038 [US3] Update `TemplatePickerScreen.kt` to resolve `stringResource(nameRes)`/`stringArrayResource(choicesRes)` per template, change `onTemplateSelected` to carry `templateId: String` only, and externalize the screen's own strings (`templates_title`, `cd_back`, `templates_choose_template`, `templates_start_blank_title`, `templates_start_blank_subtitle`) in `app/src/main/kotlin/com/choice/app/ui/templates/TemplatePickerScreen.kt` (depends on T036)
- [X] T039 [US3] Change the `coinedit` route's template argument from `templateName` to `templateId` in `ChoiceNavHost`; resolve `stringResource`/`stringArrayResource` at the navigation call site (before constructing `CoinViewModelFactory`, whose signature stays unchanged) in `app/src/main/kotlin/com/choice/app/MainActivity.kt` (contracts/navigation-contract.md §1; depends on T038)
- [X] T040 [P] [US3] Update `app/src/androidTest/kotlin/com/choice/app/ui/templates/TemplatePickerScreenTest.kt` to resolve expected text via `context.getString`/`context.resources.getStringArray` and to assert the picker navigates with `templateId`
- [ ] T041 [US3] Manually validate User Story 3 per quickstart.md §2 "US3" block — create a coin from a template in Arabic, switch to English, confirm the coin's name/choices are unchanged while the picker now shows English; repeat in the opposite direction

**Checkpoint**: User Stories 1–3 all independently functional.

---

## Phase 6: User Story 4 - Search, Review History, and View Statistics in Either Language (Priority: P4)

**Goal**: Search works correctly regardless of script; history and statistics labels, empty states,
and date/time formatting are locale-appropriate with Western Arabic digits always.

**Independent Test**: With coins named in both Arabic and English, search in each app language and
confirm correct matches; open history and statistics in each language and confirm labels,
empty-states, and date/number formatting are locale-appropriate (spec.md US4).

### Implementation for User Story 4

- [X] T042 [P] [US4] Add Arabic and mixed-script cases to `app/src/test/kotlin/com/choice/app/domain/CoinSearchTest.kt` proving `matchesSearchQuery` already satisfies FR-012/FR-013 — no production code change expected (research.md §8)
- [X] T043 [P] [US4] Create shared `DecisionTimeFormatter.format(epochMillis: Long, locale: Locale, zone: ZoneId): String` using `DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)` in `app/src/main/kotlin/com/choice/app/ui/format/DecisionTimeFormatter.kt` (contracts/localization-contract.md §6; depends on Foundational T007 for `formattingLocale`)
- [X] T044 [P] [US4] Add `app/src/test/kotlin/com/choice/app/ui/format/DecisionTimeFormatterTest.kt`: English vs. `formattingLocale("ar")` render the same instant differently (DF-3), and Arabic output contains only `0`-`9` digits (DF-2/SC-006) (depends on T043)
- [X] T045 [US4] Replace `HistoryScreen.kt`'s private `formatTimestamp`/`SimpleDateFormat` with `DecisionTimeFormatter.format(decidedAt, currentFormattingLocale, ZoneId.systemDefault())`, resolving the current locale via `LocalConfiguration.current.locales[0]` mapped through `formattingLocale(tag)`; externalize `history_title`, `cd_back`, `cd_statistics`, `history_empty_title`, `history_empty_subtitle` in `app/src/main/kotlin/com/choice/app/ui/history/HistoryScreen.kt` (depends on T043)
- [X] T046 [US4] Apply the same timestamp-formatter replacement to `StatisticsScreen.kt`; externalize `statistics_title`, `cd_back`, `statistics_empty_title`, `statistics_empty_subtitle`, `statistics_total_decisions`, `statistics_per_choice_breakdown`, `statistics_most_frequent_suffix`, `statistics_least_frequent_suffix`, `statistics_most_and_least_frequent_suffix`, `statistics_most_recent_decision` in `app/src/main/kotlin/com/choice/app/ui/statistics/StatisticsScreen.kt` (depends on T043)
- [X] T047 [P] [US4] Update `app/src/androidTest/kotlin/com/choice/app/ui/history/HistoryScreenTest.kt` to resolve expected text via `context.getString(R.string.…)`
- [X] T048 [P] [US4] Add `app/src/androidTest/kotlin/com/choice/app/locale/RtlLayoutTest.kt`: assert `LocalLayoutDirection` resolves to RTL under Arabic across Main/History/Statistics/CoinEdit/CoinFlip, and assert no clipping/overflow at ~320dp width (SC-002)
- [ ] T049 [US4] Manually validate User Story 4 per quickstart.md §2 "US4" block, including the Western-Arabic-digit check in Arabic-locale timestamps

> **Note — percentage formatting (FR-015/SC-006)**: Statistics currently shows only per-choice
> counts; no percentage is displayed anywhere in the app today. This clause of FR-015/SC-006 is
> satisfied **vacuously** — no task here adds a percentage UI, since that would be new
> functionality beyond this feature's localization scope (Principle I/VIII). Decided with the
> user, 2026-08-24. If a future feature adds a percentage display, it MUST use
> `NumberFormat.getPercentInstance(formattingLocale)` per contracts/localization-contract.md §6 DF-5.

**Checkpoint**: User Stories 1–4 all independently functional.

---

## Phase 7: User Story 5 - Use Widgets, Accessibility Features, and Share/Import in Either Language (Priority: P5)

**Goal**: Widgets, TalkBack announcements, and share/import are fully localized (or, for the share
payload, unaffected by language) in both English and Arabic; widgets relabel immediately on any
language change without losing recorded results.

**Independent Test**: Add a widget in Arabic and confirm localized RTL text; enable a screen reader
in Arabic and confirm Arabic announcements; export a coin in one language and import in the other,
confirming identical content (spec.md US5).

### Implementation for User Story 5

**Widget strings, bidi, localized context**

- [X] T050 [P] [US5] Add `cd_widget_open_app` (`%1$s`), `cd_widget_coin_ready` (`%1$s`), `cd_widget_coin_result` (`%1$s`, `%2$s`), `cd_widget_flip_coin` (`%1$s`), `cd_widget_action_for_coin` (`%1$s`, `%2$s`) to both resource files (contracts/widget-localization-contract.md §2.2)
- [X] T051 [US5] Rewrite `SingleCoinWidget.kt`'s five string-concatenated `contentDescription`s (lines ~165, 202, 227, 245, plus the reconfigure/open-app pair) to use the T050 placeholder resources, wrapping interpolated coin names/results with `BidiFormatter.getInstance().unicodeWrap(...)`, in `app/src/main/kotlin/com/choice/app/widget/SingleCoinWidget.kt` (depends on T050)
- [X] T052 [US5] Rewrite `QuickCoinsWidget.kt`'s four string-concatenated `contentDescription`s (lines ~247, 269, 277, 302) the same way, in `app/src/main/kotlin/com/choice/app/widget/QuickCoinsWidget.kt` (depends on T050)
- [X] T053 [P] [US5] Externalize `SingleCoinWidgetConfigActivity.kt`'s `"${choices.size} choices"` into the `coin_choice_count` `<plurals>` (added in T013), in `app/src/main/kotlin/com/choice/app/widget/SingleCoinWidgetConfigActivity.kt`
- [X] T054 [P] [US5] Override `attachBaseContext` in `app/src/main/kotlin/com/choice/app/widget/SingleCoinWidgetConfigActivity.kt` and `app/src/main/kotlin/com/choice/app/widget/QuickCoinsWidgetConfigActivity.kt` to apply `LocaleApplier.localizedContext` (depends on Foundational T010)
- [X] T055 [P] [US5] Wrap the `Context` in `SingleCoinWidget.provideGlance` and `QuickCoinsWidget.provideGlance` with `LocaleApplier.localizedContext` before any `getString` call, in `app/src/main/kotlin/com/choice/app/widget/SingleCoinWidget.kt` and `app/src/main/kotlin/com/choice/app/widget/QuickCoinsWidget.kt` (depends on Foundational T010)

**Immediate widget refresh on language change**

- [X] T056 [US5] Extract a callable `WidgetRefreshCoordinator.refreshAll()` (the existing `SingleCoinWidget().updateAll(context)` + `QuickCoinsWidget().updateAll(context)` pair) out of the `observeCoins()` collector so it can be invoked directly, in `app/src/main/kotlin/com/choice/app/widget/WidgetRefreshCoordinator.kt` (contracts/widget-localization-contract.md §4)
- [X] T057 [US5] Create `LocaleChangedReceiver` (`ACTION_LOCALE_CHANGED` → `WidgetRefreshCoordinator.refreshAll()`) in `app/src/main/kotlin/com/choice/app/widget/LocaleChangedReceiver.kt`; declare it manifest-registered (not runtime-registered, so it fires even with the process dead) in `app/src/main/AndroidManifest.xml` (depends on T056)
- [X] T058 [US5] Wire `SettingsViewModel`'s `refreshWidgets` hook (T027) to `WidgetRefreshCoordinator.refreshAll()` so an in-app language change also triggers an immediate widget refresh, in `app/src/main/kotlin/com/choice/app/ui/settings/SettingsViewModel.kt` (depends on T027, T056)

**Share / import chrome and error localization**

- [X] T059 [P] [US5] Externalize `ImportCoinScreen.kt` strings (`share_import_title`, `cd_back`, `share_import_instructions`, `share_import_placeholder`, `share_import_action`) in `app/src/main/kotlin/com/choice/app/ui/share/ImportCoinScreen.kt`
- [X] T060 [US5] Give `InvalidSharePayloadException` a structured `reason: SharePayloadError` (sealed class/enum covering every validation branch currently hardcoded as an English message, e.g. missing/unsupported schema version, blank/oversized name or choice text, too-few-choices, non-positive weight, malformed JSON) in `app/src/main/kotlin/com/choice/app/domain/SharePayload.kt`, keeping `message` only as a non-user-facing developer fallback so `app/src/test/kotlin/com/choice/app/domain/SharePayloadTest.kt` (which asserts exception type only) keeps passing unmodified (Principle VI — domain code stays free of Android resource dependencies)
- [X] T061 [US5] Add one `share_error_*` resource key per `SharePayloadError` case (with `%1$d`/`%1$s` placeholders for the schema-version/length-limit cases) to both resource files; map `SharePayloadError → @StringRes Int` and localize the `"Please paste a coin payload"` / `"Import failed: …"` cases in `app/src/main/kotlin/com/choice/app/ui/share/ImportCoinViewModel.kt` (depends on T060)
- [X] T062 [P] [US5] Confirm `CoinFlipScreen.kt` (T017) and `CoinSettingsScreen.kt` (T018) both reuse the same `share_chooser_title` key for their `Intent.createChooser` title rather than duplicating it
- [X] T063 [P] [US5] Update `app/src/androidTest/kotlin/com/choice/app/ui/share/ImportCoinScreenTest.kt` to resolve expected text (including the T061 error messages) via `context.getString(R.string.…)`

**Accessibility sweep, remaining test updates, validation**

- [X] T064 [US5] Sweep every screen and widget surface (`main`, `coinedit`, `coinflip`, `coinsettings`, `history`, `statistics`, `templates`, `settings`, both `GlanceAppWidget`s, both widget config activities) confirming every interactive control has a non-null, localized `contentDescription` (FR-022) and none is left in a language different from the active UI language (FR-023); fix any gap found
- [X] T065 [P] [US5] Check `app/src/test/kotlin/com/choice/app/widget/WidgetDecisionParityTest.kt`, `WidgetConfigStoreTest.kt`, `WidgetFlipGuardTest.kt`, `QuickCoinsDefaultSelectionTest.kt` for assertions on hardcoded English widget text and update any that exist
- [ ] T066 [US5] Manually validate User Story 5 per quickstart.md §2 "US5" block in full: widget config + body Arabic/RTL; immediate relabel on an in-app language switch with recorded results intact; immediate relabel on a device system-language change with the app process dead (manifest receiver, WR-2); the in-app override winning over a subsequent system-language change (WR-6); TalkBack announcements fully Arabic/English with no cross-language leftovers; export↔import round trip in both language directions

**Checkpoint**: All five user stories are independently functional.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final gates confirming the whole feature together, per quickstart.md's Definition of Done.

- [X] T067 [P] Run `./gradlew lintDebug` and resolve any remaining `MissingTranslation`/`ExtraTranslation` findings across `app/src/main/res/values/strings.xml` and `app/src/main/res/values-ar/strings.xml`
- [X] T068 [P] Run `./gradlew testDebugUnitTest` and fix any failures
- [ ] T069 Run `./gradlew connectedDebugAndroidTest` on both an API 33+ and an API 26–32 device/emulator and fix any failures
- [X] T070 [P] Confirm no new dependency was added — `git diff app/build.gradle.kts` shows no new entries in `dependencies { }` (Principle VII, research.md §1)
- [X] T071 [P] Confirm no Room migration or schema file was added under `app/schemas/` — this feature requires none (data-model.md summary)
- [ ] T072 Execute the full quickstart.md "Definition of done" checklist end-to-end and record results

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies — start immediately
- **Foundational (Phase 2)**: depends on Setup (needs `values-ar/strings.xml` to exist) — **blocks US2, US4, US5**; does **not** block US1 or US3
- **US1 (Phase 3)**: depends only on Setup — can start immediately, in parallel with Phase 2
- **US2 (Phase 4)**: depends on Foundational (Phase 2)
- **US3 (Phase 5)**: depends only on Setup — can start immediately, in parallel with Phases 2/3/4
- **US4 (Phase 6)**: depends on Foundational (Phase 2) for `formattingLocale`
- **US5 (Phase 7)**: depends on Foundational (Phase 2) for `LocaleApplier`; T058 additionally depends on US2's T027 and US2's `WidgetRefreshCoordinator` extraction in T056
- **Polish (Phase 8)**: depends on every user story phase being complete

### User Story Dependencies

- **US1 (P1)**: independent of every other story
- **US2 (P2)**: independent of US1/US3/US4 in content, but shares the Foundational locale mechanism
- **US3 (P3)**: independent of every other story
- **US4 (P4)**: independent of US1/US2/US3 in content, but shares the Foundational locale mechanism
- **US5 (P5)**: independent in content, but T058 specifically reuses US2's `SettingsViewModel` hook and US5's own `WidgetRefreshCoordinator` extraction — sequence T056 before T058

### Within Each User Story

- Resource/string tasks before the Kotlin file that consumes them (e.g., T035 before T036)
- Error-code refactors (T016, T019, T060) before the resource-key tasks that map them (T061) where applicable
- Implementation before the instrumented-test update for the same screen
- Story complete (all tasks + manual validation) before moving to the next priority, if working sequentially

### Parallel Opportunities

- All Setup tasks marked [P] run in parallel
- Within Foundational, T006/T007/T009 run in parallel; T008 waits on T007; T010 waits on T006+T007; T011 waits on T010; T012 waits on T009+T010
- Once Foundational completes, US2/US4/US5 can start in parallel with each other and with US1/US3 (already running)
- Within US1, T013–T019 (all different files) run in parallel; T020 waits on all of them; T021–T024 (test updates, different files) run in parallel after their corresponding implementation task
- Within US5, the widget-string tasks (T050–T055), the refresh tasks (T056–T058), and the share/import tasks (T059–T063) are three largely independent tracks that can proceed in parallel, converging at T064

---

## Parallel Example: User Story 1

```bash
# Launch all five screen-externalization tasks together (different files, no shared state):
Task: "Externalize MainScreen.kt strings in app/src/main/kotlin/com/choice/app/ui/main/MainScreen.kt"
Task: "Externalize DeleteCoinConfirmationDialog.kt strings in app/src/main/kotlin/com/choice/app/ui/components/DeleteCoinConfirmationDialog.kt"
Task: "Externalize CoinEditScreen.kt strings in app/src/main/kotlin/com/choice/app/ui/coinedit/CoinEditScreen.kt"
Task: "Externalize CoinFlipScreen.kt strings in app/src/main/kotlin/com/choice/app/ui/coinflip/CoinFlipScreen.kt"
Task: "Externalize CoinSettingsScreen.kt strings in app/src/main/kotlin/com/choice/app/ui/coinsettings/CoinSettingsScreen.kt"
```

## Parallel Example: Foundational locale mechanism

```bash
Task: "Create SupportedLanguages.kt in app/src/main/kotlin/com/choice/app/locale/SupportedLanguages.kt"
Task: "Create LanguagePreferenceStore.kt in app/src/main/kotlin/com/choice/app/locale/LanguagePreferenceStore.kt"
# (LocalePreference.kt depends on SupportedLanguages.kt landing first)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 3: User Story 1 (Foundational/Phase 2 is **not** required for US1)
3. **STOP and VALIDATE**: run quickstart.md's US1 block on a device set to Arabic
4. This alone delivers the spec's stated central value: "Arabic support and correct RTL behavior
   are what make Choice usable for Arabic-speaking users at all" (spec.md US1 rationale)

### Incremental Delivery

1. Setup → US1 → validate → demo (Arabic UI works via device language, MVP)
2. Foundational → US2 → validate → demo (in-app selector, data survives every switch)
3. US3 → validate → demo (localized templates, materialization boundary holds)
4. US4 → validate → demo (search, history, statistics, locale-aware formatting)
5. US5 → validate → demo (widgets, accessibility, share/import) → Polish

### Parallel Team Strategy

With multiple developers, after Setup (Phase 1):

- Developer A: US1 (Phase 3) — no Foundational dependency, starts immediately
- Developer B: Foundational (Phase 2), then US2 (Phase 4)
- Developer C: US3 (Phase 5) — no Foundational dependency, starts immediately
- Once Foundational lands: Developer D picks up US4 (Phase 6), Developer B continues to US5 (Phase 7)
- Phase 8 (Polish) only after all five stories are merged

---

## Notes

- [P] tasks touch different files with no completed-task dependency between them
- [Story] labels trace every task back to its spec.md user story
- The 14 pre-existing instrumented tests (T021–T024, T040, T047, T063) are **required** updates,
  not optional cleanup — if any still passes unmodified after its screen's strings move to
  resources, externalization for that screen is incomplete
- T016/T019/T060 deliberately keep ViewModels and domain code free of Android resource
  dependencies (Principle VI) by moving to error-code types, with the *Composable* resolving the
  final localized string — never let a hardcoded English string re-enter through a "convenient"
  `Context` reference in a ViewModel
- No task in this list touches `ChoiceDatabase`, any `*Entity`, or `app/schemas/` — confirmed by
  T071. If an agent implementing these tasks finds itself wanting to add a migration, stop and
  re-read data-model.md: this feature needs none
- Commit after each task or logical group; stop at any phase checkpoint to validate that story
  independently before continuing
