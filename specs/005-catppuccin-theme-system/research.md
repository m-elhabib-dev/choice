# Research: Catppuccin Theme System

**Feature**: `005-catppuccin-theme-system` | **Input**: [spec.md](./spec.md)

No `NEEDS CLARIFICATION` markers remain in the spec (both clarification questions were resolved
2026-08-25). This document records the technical decisions needed to move from spec to design,
based on direct inspection of the current codebase (Compose app, single hardcoded Mocha theme).

---

## 1. How the four flavors are represented in code

**Decision**: One `data class CatppuccinPalette` holding the 26 named Catppuccin colors (the same
26 the app already has in `Color.kt` for Mocha) plus an `isLight: Boolean` flag. Four `val`
instances — `LattePalette`, `FrappePalette`, `MacchiatoPalette`, `MochaPalette` — each populated
with that flavor's official hex values. A `ThemeFlavor` enum (`LATTE`, `FRAPPE`, `MACCHIATO`,
`MOCHA`) carries a stable `id` string (for persistence), `@StringRes` name/description, and a
reference to its `CatppuccinPalette`.

**Rationale**:
- The app's *existing* Mocha `Color.kt` is already just 26 raw `Color(0xFF...)` vals with no
  flavor abstraction — there is nothing to preserve structurally, only the exact hex values (which
  carry over unchanged into `MochaPalette`).
- A `data class` of named colors (rather than four parallel top-level color files) means one
  formula builds a `ColorScheme` from *any* palette — see §2 — so adding a fifth flavor later
  (FR-017) is "add one palette instance + one enum entry," never a new formula.
- Four flavors share 26 color *names* (`Mauve`, `Red`, `Base`, …) with four different hex values
  each; top-level `val`s can't have four same-named-but-different constants in one file, so they
  must be scoped (record fields, not top-level vals).

**Alternatives considered**:
- *Four `object`s of top-level `val`s* (e.g. `object LattePalette { val Mauve = ... }`) — rejected:
  same information, but no shared type means the `ColorScheme`-building formula would need to be
  copy-pasted four times (or reflection), which is exactly the duplication FR-017 rules out.
- *XML color resources per flavor* (`values/colors_mocha.xml`, …) — rejected: the app is 100%
  Compose; XML colors would need to be re-imported into Compose `Color` anyway, adding a layer with
  no benefit, and Glance widget colors need `ColorProvider(Color)` either way.

---

## 2. Turning a palette into Material3 `ColorScheme` + app-specific roles

**Decision**: Keep the *exact* role-mapping formula the app already uses for Mocha in `Theme.kt`
(`primary = Mauve, onPrimary = Base, background = Base, surface = Mantle, surfaceVariant =
Surface0, onSurfaceVariant = Overlay2, error = Red, outline = Overlay1, outlineVariant =
Surface2, …` — see `Theme.kt:7-36`), but parameterize it as `buildColorScheme(palette:
CatppuccinPalette): ColorScheme` and call it once per flavor. `isLight` selects
`lightColorScheme(...)` vs `darkColorScheme(...)` (both take the identical named-parameter list,
so the same call works through a function reference).

FR-009 requires five roles the app's current `ColorScheme` doesn't carry at all today —
**success, warning, informational state, disabled text** — because nothing in the app currently
renders them (confirmed: no screen references `Green`/`Yellow` and no "success"/"warning" UI
exists yet). These are added as a small extension, not stuffed into Material3's fixed role set:

- **Success / Warning / Info**: a new `ChoiceExtendedColors` data class (`success`, `onSuccess`,
  `successContainer`, `onSuccessContainer`, and the same trio for `warning`/`info`), built by the
  same `buildExtendedColors(palette)` formula (`success = Green`, `warning = Yellow`, `info =
  Sky`, each `onX = Base`/`onXContainer = Text`, mirroring how `error`/`onError` already work),
  exposed via a `CompositionLocal` alongside `MaterialTheme`.
- **Disabled text**: not a stored color at all — Material3's own convention (`onSurface` at the
  standard 38% disabled-content alpha) is used, exactly as any other M3-idiomatic Compose app
  would. This satisfies FR-009's "disabled text" role without inventing a non-standard token, and
  it automatically tracks whatever `onSurface` is per flavor.
- **Border / Divider**: already exist as `outline` / `outlineVariant` — no new role needed.
- **Elevated surface**: already exists as `surfaceVariant` — no new role needed.

**Rationale — the mapping formula transfers unchanged across all four flavors**: every Catppuccin
flavor's own `Base`/`Text` pair (and `Mauve`, `Red`, etc.) is independently designed by upstream
Catppuccin for mutual contrast *within that flavor* — Latte's `Base` is light and `Text` is dark;
Mocha/Frappé/Macchiato's `Base` is dark and `Text` is light. Because the existing formula always
pairs a role with its flavor's own contrasting counterpart (`onPrimary = Base`, `onBackground =
Text`, …) rather than a hardcoded light/dark literal, running the identical formula over any of the
four palettes yields a self-consistent, readable `ColorScheme` with no per-flavor special-casing.
This is the load-bearing insight that makes FR-009 ("functionally equivalent" themes) and SC-004
(contrast in all four) tractable without four hand-tuned mappings.

**Alternatives considered**:
- *Per-flavor hand-picked `ColorScheme` literals* (no shared formula) — rejected: four independent
  26-role mappings to maintain and re-verify is exactly the "changes to individual
  screens/components" duplication FR-017 forbids at the palette layer, and quadruples the review
  surface for a future contrast fix.
- *Stuffing success/warning/info into Material3's `tertiary`/`secondary` slots* — rejected:
  `secondary` and `tertiary` are already assigned (`Flamingo`, `Teal`) and reused elsewhere;
  overloading them would make a future genuine secondary-accent use ambiguous with a status color.

---

## 3. Persistence mechanism (precedent: language preference)

**Decision**: A new `ThemePreferenceStore` interface + `SharedPreferencesThemeStore`
implementation, structurally identical to `LanguagePreferenceStore` /
`SharedPreferencesLanguageStore` (`read(): String?`, `write(id: String)`), storing the flavor's
`id` string under a **new key** (`"theme_flavor"`) in the **same** `"choice_app_prefs"`
`SharedPreferences` file the language preference already uses.

A pure `resolveThemeFlavor(storedId: String?): ThemeFlavor` function (no Android dependency, JVM
unit-testable) mirrors `resolveLanguageTag`: blank/null/unrecognized → `ThemeFlavor.DEFAULT`
(Mocha), otherwise the matching flavor by `id`.

**Rationale**: The spec's own Assumptions section names the language-preference mechanism as the
precedent to mirror, specifically for being "independent... outside of the main data store used
for coins/choices/history." That isolation requirement is about **not touching `ChoiceDatabase` or
`WidgetConfigStore`**, not about having a dedicated file per preference — `choice_app_prefs`
already holds exactly one other key (`language_tag`) and is not, and never has been, read or
written by Room or by widget-instance config. Adding a second key to the same small prefs file is
simpler (Principle I) than introducing a second `SharedPreferences` file for the same purpose, and
preserves the exact isolation guarantee FR-012 requires (a theme change makes zero Room calls, zero
`WidgetConfigStore` calls — call-site auditable, not just file-separation-implied).

**Alternatives considered**:
- *A dedicated `choice_theme_prefs` file* — rejected: no isolation benefit over a second key in the
  existing small file (both are equally independent of Room/widget config); adds a second
  `SharedPreferences.getSharedPreferences(...)` call site and a second file to reason about for no
  behavioral gain.
- *DataStore (`androidx.datastore`)* — rejected: introduces an async read where a synchronous one
  is needed at the very first frame (see §5), and the codebase's own precedent for this exact kind
  of preference (independent, tiny, app-level, needed before first paint) is `SharedPreferences`,
  not DataStore. DataStore is already used, correctly, for genuinely async/reactive concerns
  (`WidgetConfigStore`'s per-instance Glance state) — that is a different problem shape.

---

## 4. Applying the selected theme to the running app without restart

**Decision**: `ChoiceTheme` becomes `ChoiceTheme(flavor: ThemeFlavor, content: @Composable () ->
Unit)`, driven by a `StateFlow<ThemeFlavor>` (`AppThemeState`, held on `ChoiceApplication`, seeded
by `resolveThemeFlavor(themePreferenceStore.read())` at process start). `MainActivity.setContent`
collects it (`val flavor by application.themeState.flavor.collectAsState()`) and passes it into
`ChoiceTheme`. `SettingsViewModel.selectTheme(flavor)` writes the preference, updates the
`StateFlow`, and calls the existing `refreshWidgets()` lambda — no activity recreate, anywhere.

**Rationale**: `ChoiceTheme` is the single top-level wrap around the entire `NavHost`
(`MainActivity.kt`), and every screen already reads colors through `MaterialTheme.colorScheme.*`
(confirmed — no screen imports the raw `Color.kt` constants directly). Changing the `ColorScheme`
instance one level above `NavHost` therefore recomposes every screen, dialog, and bottom sheet
underneath it for free — this is a *simpler* mechanism than the language feature's, which needs
`activity.recreate()` on API 26–32 only because Android's locale-driven resource resolution
(`Configuration`, `Resources`) has no Compose-level recomposition hook. Theme color is pure Compose
state; it needs none of that.

Because the `StateFlow` is seeded synchronously (a `SharedPreferences` read, same cost as the
language store's) before `MainActivity.setContent` runs, the very first composed frame already
carries the correct flavor in the overwhelming majority of cases. The spec's clarification that "a
brief flash... is acceptable" covers the theoretical edge (e.g., an unusually slow first
`SharedPreferences` load) without requiring extra design work to eliminate it — no splash-screen
gating or preload step is added, matching Principle I (Simplicity First).

**Alternatives considered**:
- *Activity recreate on theme change* (mirroring the language flow) — rejected: unnecessary here
  (colors need no `Configuration`/`Resources` re-resolution), would reset navigation/scroll state
  for no benefit, and directly conflicts with FR-007's "without requiring the user to restart the
  app or navigate away and back."
- *`CompositionLocal` alone, no `StateFlow`* — rejected: a bare mutable `CompositionLocal` written
  from a non-Composable call site (the ViewModel) isn't a supported pattern; `StateFlow` +
  `collectAsState()` is the standard, already-used-elsewhere (widget refresh, coin repository)
  idiom for "state produced outside Compose, consumed inside it."

---

## 5. Widget color application (Glance)

**Decision**: `WidgetGlanceTheme` (currently six hardcoded `ColorProvider`s) becomes
`WidgetGlanceTheme.colorsFor(flavor: ThemeFlavor): WidgetThemeColors`, mapping the same six roles
(`background = Base, surface = Surface0, surfaceVariant = Surface1, textPrimary = Text,
textSecondary = Subtext0, accent = Blue`) from whichever palette is selected. Each widget's
`provideGlance(context, id)` reads the flavor once per render
(`resolveThemeFlavor(SharedPreferencesThemeStore(context).read())`) and provides the resulting
`WidgetThemeColors` via a `staticCompositionLocalOf` (Glance composables run on the Compose
runtime, so `CompositionLocalProvider` works identically to app-side Compose) — no per-composable
parameter threading needed inside `SingleCoinWidget.kt` / `QuickCoinsWidget.kt`. Refresh reuses the
**existing** `WidgetRefreshCoordinator.refreshAll()` call already wired into
`SettingsViewModel`'s `refreshWidgets` lambda (currently used for language changes) — `selectTheme`
calls the identical lambda, so `updateAll()` → `provideGlance` → re-read → re-render happens with
no new plumbing.

**Rationale**: The six-role subset and its exact mapping already exist for Mocha
(`WidgetGlanceTheme.kt`'s own doc comment explains *why* it's a second, hand-mirrored palette:
Glance cannot consume a Compose `MaterialTheme` instance). That constraint is unchanged by this
feature — widgets still can't read `MaterialTheme.colorScheme` — so the fix is to parameterize the
existing small palette by flavor, not to unify it with the app-side `ColorScheme` (which would
require Glance to consume Material3 theming, a much larger, unrelated change outside this
feature's scope). The refresh trigger is a straight reuse of the mechanism the `004` localization
feature already proved works for "an app-level preference changed, relabel every placed widget
instance without requiring removal/reconfiguration" — exactly FR-014's requirement.

**Alternatives considered**:
- *Widgets read `SharedPreferences` fresh inside every composable* — rejected: one read per
  `provideGlance` call, shared via `CompositionLocalProvider`, is simpler and avoids repeated I/O
  inside the render tree for no benefit.
- *A `WorkManager` periodic refresh* — rejected: unnecessary: `updateAll()` triggered by the
  existing `refreshWidgets()` call site fires exactly when the theme actually changes, matching
  FR-014's "within the normal widget refresh behavior" without adding a dependency or a poll loop
  (Principle VII, Principle I).

---

## 6. Settings UI structure (Appearance section)

**Decision**: Extend the existing single `SettingsScreen.kt` (`route = "settings"`) with a second
section, "Appearance," below the existing "Language" section — **not** a new screen/route. Each of
the four rows shows the flavor's name (`stringResource(nameRes)`), description
(`stringResource(descriptionRes)`), a small swatch (three `Box`es or a single `Row` of color chips
drawn directly from `ThemeFlavor.swatchColors`, no image assets), and a `RadioButton` following the
exact `Modifier.selectable(selected, role = Role.RadioButton, onClick = ...)` pattern the language
options already use.

**Rationale**: The spec frames this explicitly as "an Appearance section in Settings" (FR-001), not
a new destination, and the existing `SettingsScreen.kt` doc comment ("Choice's only app-level
settings screen... and nothing else") already anticipates being the single home for app-level
prefs — updating that comment is a doc change, not a structural one. The existing selectable-row
pattern is inline (not yet its own composable); factoring a small `ThemeOptionRow`-style composable
for the four theme rows (richer than the plain radio+label language rows: it needs a description
line and swatch) is a natural, low-risk extraction that leaves the language rows untouched.

**Alternatives considered**:
- *A separate "Appearance" screen/route* — rejected: adds a navigation destination and a back-stack
  entry for a single, small selection list, contradicting Principle I and the spec's own framing
  ("an Appearance section in Settings," not "an Appearance screen").

---

## 7. Contrast validation (SC-004)

**Decision**: Add a JVM unit test that computes the WCAG relative-luminance contrast ratio for
every FR-010-named pair (primary text/background, secondary text/background, button/background,
error/background, success/background, disabled-text/background, …) across all four flavors, and
asserts each meets 4.5:1 (normal text) or 3:1 (large text/graphical elements) per SC-004. This runs
as ordinary automated test code, not a manual design review step.

**Rationale**: Every color value going into this feature is a fixed, known hex literal (§1) — the
contrast ratio for every pair is fully determined at compile time and is exactly the kind of "core
data operation" Principle VI (Testable Behavior) asks to be covered by automated tests rather than
verified by eyeballing four color swatches. A regression (e.g., a future flavor with a poorly
chosen pairing) is caught the same way any other unit test regression is.

**Alternatives considered**:
- *Manual/visual accessibility review only* — rejected: not repeatable, not enforced on every
  change, and unnecessary given the pairs are static literals with a well-defined formula (WCAG
  contrast ratio) — a textbook case for a pure unit test.

---

## Summary of resolved unknowns

| Topic | Resolution |
|---|---|
| Language/tooling | No change — Kotlin 2.0.21, Compose BOM 2024.12.01, Material3, Glance 1.1.1, all already present |
| New dependency | **None** |
| Persistence | New key (`theme_flavor`) in the existing `choice_app_prefs` `SharedPreferences` file |
| App-wide apply | `StateFlow<ThemeFlavor>` + `collectAsState()`, no activity recreate |
| Widget apply | Parameterize existing `WidgetGlanceTheme` by flavor; reuse existing `refreshAll()` |
| New color roles (FR-009) | `ChoiceExtendedColors` (success/warning/info); disabled text via M3 alpha convention |
| Settings UI | Extend existing Settings screen with an "Appearance" section; no new route |
| Contrast verification | Automated JVM unit test over fixed color-pair literals, not manual review |
