---

description: "Task list for Catppuccin Theme System"
---

# Tasks: Catppuccin Theme System

**Input**: Design documents from `/specs/005-catppuccin-theme-system/`
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: Included — SC-004 (contrast) and FR-006 (fallback) are measurable/testable outcomes
requiring automated coverage, and Constitution Principle VI requires core logic to be
independently tested. Every pure decision (flavor resolution, contrast ratios, widget color
mapping) gets a JVM unit test; every rendered/RTL/persistence behavior gets an instrumented test.

**Organization**: Tasks are grouped by user story (P1–P3, from spec.md), so each story is
independently implementable and testable per the Independent Test in its phase header.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1–US3, mapping to spec.md's three prioritized user stories
- Every task names its exact file path(s)

## Path Conventions

Single Android module. All main sources under `app/src/main/kotlin/com/choice/app/`, resources
under `app/src/main/res/`, JVM tests under `app/src/test/kotlin/com/choice/app/`, instrumented
tests under `app/src/androidTest/kotlin/com/choice/app/` — per plan.md's Project Structure.

---

## Phase 1: Setup

**Purpose**: String resources that the Foundational `ThemeFlavor` enum requires to compile
(`@StringRes` references must resolve to real keys).

- [X] T001 [P] Add `settings_appearance_label` and the eight `theme_{latte,frappe,macchiato,mocha}_{name,description}` keys (English) to `app/src/main/res/values/strings.xml` — descriptions per spec.md's example wording (e.g. "Light Catppuccin" / "Dark Catppuccin") (contracts/theme-contract.md §7)
- [X] T002 [P] Add the same nine keys, translated to Arabic, to `app/src/main/res/values-ar/strings.xml` (FR-016; keeps Android Lint's `MissingTranslation` gate green)

**Checkpoint**: string keys exist; the Foundational enum can reference them.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The shared color/persistence/state layer every user story builds on — the palette
data, the four `ColorScheme`s, the extended status-color roles, flavor resolution, and the
preference store + in-memory state that both the app and widgets read. **No user story can be
implemented, and the app cannot even compile (`ChoiceTheme`'s signature changes), until this phase
is complete.**

- [X] T003 [P] Create `CatppuccinPalette` data class (26 named colors + `isLight: Boolean`) and four instances — `LattePalette`, `FrappePalette`, `MacchiatoPalette`, `MochaPalette` — in `app/src/main/kotlin/com/choice/app/ui/theme/Color.kt`, using the hex tables in data-model.md §1; `MochaPalette`'s 26 values MUST exactly match the current top-level `val`s in this file (migrate them in, don't retype) so Mocha's appearance is byte-for-byte unchanged
- [X] T004 Create `ThemeFlavor` enum (`LATTE`, `FRAPPE`, `MACCHIATO`, `MOCHA` — `id`, `isLight`, `@StringRes nameRes`, `@StringRes descriptionRes`, `internal palette: CatppuccinPalette`) plus `swatchColors` (`listOf(palette.base, palette.mauve, palette.blue)`) and `companion object { val DEFAULT = MOCHA; fun fromId(id: String?): ThemeFlavor? }` in `app/src/main/kotlin/com/choice/app/ui/theme/ThemeFlavor.kt` (data-model.md §2; depends on T001, T002, T003)
- [X] T005 [P] Add `buildColorScheme(palette: CatppuccinPalette): ColorScheme` (the app's existing Mocha `darkColorScheme(...)`/`lightColorScheme(...)` role-mapping formula, parameterized) and change `ChoiceTheme` to `ChoiceTheme(flavor: ThemeFlavor, content: @Composable () -> Unit)` calling `MaterialTheme(colorScheme = flavor.colorScheme, typography = ChoiceTypography, content = content)` in `app/src/main/kotlin/com/choice/app/ui/theme/Theme.kt` (research.md §2; depends on T003, T004)
- [X] T006 [P] Create `ChoiceExtendedColors` data class (success/warning/info + their `on*`/`*Container`/`on*Container` pairs) and `buildExtendedColors(palette: CatppuccinPalette)` (data-model.md §4), plus `LocalChoiceExtendedColors` `CompositionLocal`, provided inside `ChoiceTheme` alongside `MaterialTheme`, in `app/src/main/kotlin/com/choice/app/ui/theme/ExtendedColors.kt` (depends on T003, T005)
- [X] T007 [P] Create pure `resolveThemeFlavor(storedId: String?): ThemeFlavor` (`storedId?.let { ThemeFlavor.fromId(it) } ?: ThemeFlavor.DEFAULT`) in `app/src/main/kotlin/com/choice/app/ui/theme/ThemePreference.kt` (contracts/theme-contract.md §2; depends on T004)
- [X] T008 [P] Add `app/src/test/kotlin/com/choice/app/ui/theme/ThemeFlavorTest.kt` covering the full 8-row resolution table from contracts/theme-contract.md §2 (valid ids, `null`, blank, unrecognized, wrong-case) (depends on T007)
- [X] T009 [P] Add `app/src/test/kotlin/com/choice/app/ui/theme/ThemeContrastTest.kt` computing the WCAG relative-luminance contrast ratio for every FR-009 role pair in contracts/theme-contract.md §6, across all four flavors, asserting ≥4.5:1 for normal-text roles and ≥3:1 for large-text/graphical roles (SC-004) (depends on T003, T005, T006)
- [X] T010 [P] Create `ThemePreferenceStore` interface (`read(): String?`, `write(id: String)`) and `SharedPreferencesThemeStore` over `SharedPreferences("choice_app_prefs", MODE_PRIVATE)`, new key `"theme_flavor"`, in `app/src/main/kotlin/com/choice/app/ui/theme/ThemePreferenceStore.kt` (contracts/theme-contract.md §3)
- [X] T011 Create `AppThemeState(initial: ThemeFlavor)` (`MutableStateFlow`/`StateFlow<ThemeFlavor>` + `set(flavor)`) in `app/src/main/kotlin/com/choice/app/ui/theme/AppThemeState.kt` (data-model.md §6; depends on T004)
- [X] T012 Construct `themePreferenceStore = SharedPreferencesThemeStore(this)` and `themeState = AppThemeState(resolveThemeFlavor(themePreferenceStore.read()))` in `ChoiceApplication.onCreate`, in `app/src/main/kotlin/com/choice/app/ChoiceApplication.kt` (contracts/theme-contract.md §4; depends on T007, T010, T011)
- [X] T013 Update the two remaining `ChoiceTheme { ... }` call sites to the new signature — `ChoiceTheme(resolveThemeFlavor(SharedPreferencesThemeStore(this).read())) { ... }` — a one-shot synchronous read, no live observation needed, in `app/src/main/kotlin/com/choice/app/widget/SingleCoinWidgetConfigActivity.kt` and `app/src/main/kotlin/com/choice/app/widget/QuickCoinsWidgetConfigActivity.kt` (contracts/theme-contract.md §4 table; depends on T005, T007, T010; required for the module to compile)

**Checkpoint**: palette/enum/scheme/store/state all exist; the app compiles; user story
implementation can now begin.

---

## Phase 3: User Story 1 - Choose an appearance theme (Priority: P1) 🎯 MVP

**Goal**: An "Appearance" section in Settings lets the user pick one of the four flavors; the
entire running app updates its colors immediately, on every screen/dialog/bottom sheet, with no
restart.

**Independent Test**: Open Settings, select each of the four themes in turn, and confirm the
visible color scheme of the current screen changes to match the selected flavor with no app
restart required (spec.md US1).

### Implementation for User Story 1

- [X] T014 [US1] In `MainActivity.setContent`, collect `val flavor by application.themeState.flavor.collectAsState()` and pass it to `ChoiceTheme(flavor) { ChoiceNavHost(...) }`, in `app/src/main/kotlin/com/choice/app/MainActivity.kt` (contracts/theme-contract.md §4; depends on Foundational T005, T011, T012)
- [X] T015 [P] [US1] Create `ThemeOptionRow` composable (name via `stringResource(nameRes)`, description via `stringResource(descriptionRes)`, a small swatch row from `ThemeFlavor.swatchColors`, and a `RadioButton` using the existing `Modifier.selectable(selected, role = Role.RadioButton, onClick = ...)` pattern from the language rows) in `app/src/main/kotlin/com/choice/app/ui/components/ThemeOptionRow.kt` (contracts/theme-contract.md §7; depends on Foundational T004)
- [X] T016 [US1] Add `selectTheme(flavor: ThemeFlavor)` to `SettingsViewModel` — no-op if `flavor == current` (mirrors the existing `selectLanguage` reselect guard), else `themePreferenceStore.write(flavor.id)`, `themeState.set(flavor)`, `refreshWidgets()` (the lambda already wired for language changes) — in `app/src/main/kotlin/com/choice/app/ui/settings/SettingsViewModel.kt` (contracts/theme-contract.md §4; depends on Foundational T010, T011, T012)
- [X] T017 [US1] Add an "Appearance" section to `SettingsScreen` below the existing "Language" section — `settings_appearance_label` heading, four `ThemeOptionRow`s over `ThemeFlavor.entries` wired to `viewModel.selectTheme`, current selection shown via `flavor == currentFlavor`; update the screen's stale "language, and nothing else" doc comment — in `app/src/main/kotlin/com/choice/app/ui/settings/SettingsScreen.kt` (contracts/theme-contract.md §7; depends on T015, T016)
- [X] T018 [P] [US1] Extend `app/src/androidTest/kotlin/com/choice/app/ui/settings/SettingsScreenTest.kt`: selecting each theme option updates `MaterialTheme.colorScheme` immediately (assert a color read from the composition changes), and re-selecting the already-active theme triggers zero additional store writes/widget refreshes (mirrors the existing language test's `reselectingActiveLanguage_isNoOp`) (depends on T017)
- [X] T019 [P] [US1] Extend `app/src/androidTest/kotlin/com/choice/app/locale/RtlLayoutTest.kt`'s swept screen list to include the Settings/Appearance section, asserting `LocalLayoutDirection` resolves to RTL and no theme row text clips at ~320dp under Arabic (SC-008) (depends on T017)
- [ ] T020 [US1] Manually validate User Story 1 per quickstart.md §2 on both an API 26–32 and an API 33+ device/emulator, including the rapid-reselection edge case — **N/A this session**: no device/emulator available (`adb devices` empty, no `emulator` binary); run separately before shipping

**Checkpoint**: User Story 1 is fully functional and independently testable.

---

## Phase 4: User Story 2 - Retain my chosen theme across app sessions (Priority: P2)

**Goal**: A selected non-default theme survives an app restart, a device rotation, and a
process recreation by the OS, without ever affecting unrelated data.

**Independent Test**: Select a non-default theme, force-stop or restart the app (and separately,
rotate the device), and confirm the same theme is active on return in both cases (spec.md US2).

> **Note**: the persistence mechanism itself (`ThemePreferenceStore`, `AppThemeState` seeded at
> `ChoiceApplication.onCreate`) was already built in Phase 2 because `SettingsViewModel.selectTheme`
> (Phase 3) writes through it — exactly as the spec frames this story as depending on US1's
> mechanism already existing. This phase's job is proving that mechanism end-to-end, per the four
> acceptance scenarios and two edge cases in spec.md, not building new production code.

### Implementation for User Story 2

- [X] T021 [P] [US2] Extend `app/src/androidTest/kotlin/com/choice/app/ui/settings/SettingsScreenTest.kt`: select a non-default theme, construct a **fresh** `SettingsViewModel`/store instance (simulating process restart, mirroring the existing language persistence test), and assert the same theme is read back; also assert selecting a theme, then changing the language, leaves the theme selection unchanged (FR-012) (depends on Phase 3 T017)
- [X] T022 [P] [US2] Add a rotation test asserting `AppThemeState` (Application-scoped, not Activity-scoped) survives an `Activity` recreation with no flash/reset — either in `SettingsScreenTest.kt` or a new `app/src/androidTest/kotlin/com/choice/app/ui/theme/ThemeRotationTest.kt` (US2 Acceptance Scenario 2; depends on Phase 3 T014)
- [ ] T023 [US2] Manually validate User Story 2 per quickstart.md §3 on both API tiers, including: force-stop/relaunch, device rotation, and the corrupted-`SharedPreferences`-file simulation via `adb shell run-as` (proving FR-006's fallback end-to-end, beyond the pure-function coverage already in T008) — **N/A this session**: no device/emulator available; run separately before shipping

**Checkpoint**: User Stories 1 AND 2 both work independently.

---

## Phase 5: User Story 3 - See widgets reflect my chosen theme (Priority: P3)

**Goal**: Single Coin and Quick Coins home-screen widgets update their colors to the newly
selected flavor within the normal widget refresh cycle, remaining readable, without removal or
reconfiguration.

**Independent Test**: Place a widget, change the in-app theme, return to the home screen (or
trigger the next refresh), and confirm the widget's colors match the new flavor and remain
readable (spec.md US3).

### Implementation for User Story 3

- [X] T024 [P] [US3] Replace `WidgetGlanceTheme`'s six static `ColorProvider`s with `WidgetThemeColors` data class + `WidgetGlanceTheme.colorsFor(flavor: ThemeFlavor): WidgetThemeColors` (`background = base, surface = surface0, surfaceVariant = surface1, textPrimary = text, textSecondary = subtext0, accent = blue` — data-model.md §3), preserving Mocha's exact current values, in `app/src/main/kotlin/com/choice/app/widget/WidgetGlanceTheme.kt` (contracts/widget-theme-contract.md §1; depends on Foundational T003, T004)
- [X] T025 [P] [US3] Add `LocalWidgetColors` (`staticCompositionLocalOf<WidgetThemeColors>`) in `app/src/main/kotlin/com/choice/app/widget/WidgetGlanceTheme.kt` (depends on T024)
- [X] T026 [US3] In `SingleCoinWidget.provideGlance`, resolve `resolveThemeFlavor(SharedPreferencesThemeStore(context).read())`, compute `colorsFor(flavor)`, and wrap `provideContent { ... }` in `CompositionLocalProvider(LocalWidgetColors provides colors)`; replace every `WidgetGlanceTheme.<role>` reference in this file's composables with `LocalWidgetColors.current.<role>`, in `app/src/main/kotlin/com/choice/app/widget/SingleCoinWidget.kt` (contracts/widget-theme-contract.md §2; depends on T024, T025, Foundational T007, T010)
- [X] T027 [US3] Apply the same change to `QuickCoinsWidget.provideGlance` and its composables, in `app/src/main/kotlin/com/choice/app/widget/QuickCoinsWidget.kt` (depends on T024, T025, Foundational T007, T010)
- [X] T028 [P] [US3] Add `app/src/test/kotlin/com/choice/app/widget/WidgetGlanceThemeTest.kt`: `colorsFor` returns the correct six-role mapping for all four flavors, and `colorsFor(ThemeFlavor.MOCHA)` reproduces the pre-feature `WidgetGlanceTheme` constants exactly (FR-015; depends on T024)
- [ ] T029 [US3] Manually validate User Story 3 per quickstart.md §4 — place both widget types, change the in-app theme, confirm both widgets' colors update within the normal refresh cycle with no reconfiguration, text remains readable in all four flavors, and multiple placed widgets converge on the same theme — **N/A this session**: no device/emulator available; run separately before shipping

**Checkpoint**: All three user stories are independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final gates confirming the whole feature together, per quickstart.md's sign-off
checklist.

- [X] T030 [P] Run `./gradlew lintDebug` and resolve any `MissingTranslation`/`ExtraTranslation` findings for the nine new `theme_*`/`settings_appearance_label` keys
- [X] T031 [P] Run `./gradlew testDebugUnitTest` and fix any failures
- [ ] T032 Run `./gradlew connectedDebugAndroidTest` on both an API 26–32 and an API 33+ device/emulator and fix any failures — **N/A this session**: no device/emulator available; run separately before shipping
- [X] T033 [P] Confirm no new dependency was added — `git diff app/build.gradle.kts` shows no new entries in `dependencies { }` (Principle VII, research.md summary)
- [X] T034 [P] Confirm no Room migration or schema file was added under `app/schemas/` (FR-012, data-model.md preamble)
- [ ] T035 Execute the full quickstart.md §6 sign-off checklist end-to-end and record results — **N/A this session**: blocked on T020/T023/T029/T032, all of which need a device/emulator not available here; run separately before shipping

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies — start immediately
- **Foundational (Phase 2)**: depends on Setup (needs the string keys for `ThemeFlavor` to
  compile) — **blocks every user story and the module's own compilation** (`ChoiceTheme`'s
  signature change ripples to the two widget config activities)
- **US1 (Phase 3)**: depends on Foundational
- **US2 (Phase 4)**: depends on Foundational **and** US1 (its tests exercise `selectTheme`, built
  in Phase 3)
- **US3 (Phase 5)**: depends on Foundational; does **not** depend on US1 or US2's *tests*, but does
  reuse `resolveThemeFlavor`/`ThemePreferenceStore` from Foundational and the `refreshWidgets()`
  wiring from US1's T016 to become visible — can be implemented in parallel with US2 once US1's
  T016 lands
- **Polish (Phase 6)**: depends on all three user stories being complete

### Within Each User Story

- US1: components (T015) and ViewModel (T016) before the screen wiring them together (T017);
  screen before its tests (T018, T019); tests before manual validation (T020)
- US2: entirely tests + manual validation, built on Foundational + US1's `selectTheme`
- US3: widget color model (T024, T025) before the two widget files that consume it (T026, T027);
  those before their unit test (T028) and manual validation (T029)

### Parallel Opportunities

- T001/T002 (Setup) — different files
- T003, T005, T006, T007, T010 (Foundational) — once T003 exists, T005/T006/T007/T010 touch
  different files and can proceed together; T008/T009 (tests) are `[P]` once their dependencies
  land
- T015 (US1 component) can proceed in parallel with T016 (US1 ViewModel) — different files
- T018/T019 (US1 tests) — different files
- T021/T022 (US2 tests) — can proceed together once US1's T017 lands
- T024/T025 (US3 widget color model) then T026/T027 in parallel — different widget files
- T030/T031/T033/T034 (Polish) — independent checks, no shared file

---

## Parallel Example: Foundational Phase

```bash
# After T003 (CatppuccinPalette) lands, launch together:
Task: "Create ThemeFlavor enum in app/src/main/kotlin/com/choice/app/ui/theme/ThemeFlavor.kt"
Task: "Add buildColorScheme + ChoiceTheme(flavor) in app/src/main/kotlin/com/choice/app/ui/theme/Theme.kt"
Task: "Create ThemePreferenceStore in app/src/main/kotlin/com/choice/app/ui/theme/ThemePreferenceStore.kt"
```

## Parallel Example: User Story 3

```bash
# After T024/T025 (widget color model) land, launch together:
Task: "Wire SingleCoinWidget.provideGlance to LocalWidgetColors in app/src/main/kotlin/com/choice/app/widget/SingleCoinWidget.kt"
Task: "Wire QuickCoinsWidget.provideGlance to LocalWidgetColors in app/src/main/kotlin/com/choice/app/widget/QuickCoinsWidget.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories and compilation)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: run quickstart.md §2 independently
5. Deploy/demo if ready — a user can already pick any of the four themes and see the whole app
   update instantly; only restart-persistence and widget colors are still pending

### Incremental Delivery

1. Setup + Foundational → the whole color system exists, app compiles, nothing user-visible yet
2. Add US1 → in-app selection works end-to-end → **MVP**
3. Add US2 → prove (mostly via tests) that the selection survives restart/rotation/process death
4. Add US3 → widgets pick up the same selection → all three stories complete
5. Polish → lint, full test suite, dependency/schema audits, final sign-off

### Team Strategy

With multiple developers, after Foundational lands: one developer can take US1 (app UI) while
another starts US3's widget color model (T024/T025 have no dependency on US1's UI, only on
Foundational) — T026/T027's `refreshWidgets()` payoff simply won't be visible until US1's T016
lands, but the widget code itself can be written and unit-tested (T028) independently.
