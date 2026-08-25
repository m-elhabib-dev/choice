# Contract: Theme Selection, Persistence & Application

**Feature**: `005-catppuccin-theme-system` | **Type**: internal API + design-system contract

**Consumers**: every Compose screen (via `MaterialTheme.colorScheme`/`LocalChoiceExtendedColors`),
`MainActivity`, `ChoiceApplication`, `SettingsScreen`/`SettingsViewModel`,
`SingleCoinWidgetConfigActivity`, `QuickCoinsWidgetConfigActivity`.

**Builds on**: the `003-android-widgets` and `004-arabic-localization-support` preference/refresh
patterns, extended without changing their behavior.

---

## 1. Supported flavors

```kotlin
enum class ThemeFlavor(
    val id: String,
    val isLight: Boolean,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
) {
    LATTE(...), FRAPPE(...), MACCHIATO(...), MOCHA(...);
    val colorScheme: ColorScheme
    val extendedColors: ChoiceExtendedColors
    val swatchColors: List<Color>
    companion object {
        val DEFAULT: ThemeFlavor = MOCHA
        fun fromId(id: String?): ThemeFlavor?
    }
}
```

**FR-017 rule**: adding a flavor means appending one `ThemeFlavor` entry (with its own
`CatppuccinPalette` instance and two new string resources) — see
[data-model.md §1-2](../data-model.md). **No screen, dialog, widget composable, or navigation code
may need to change.** Any implementation that requires touching an individual screen to support a
fifth flavor violates this contract.

Full per-flavor hex values: [data-model.md §1](../data-model.md#1-catppuccinpalette).

---

## 2. Theme resolution

```kotlin
/** Pure. No Android dependency. Unit-tested in app/src/test. */
fun resolveThemeFlavor(storedId: String?): ThemeFlavor
```

### Behaviour table

| # | `storedId` | Result | Requirement |
|---|---|---|---|
| 1 | `"latte"` | `LATTE` | FR-001 |
| 2 | `"frappe"` | `FRAPPE` | FR-001 |
| 3 | `"macchiato"` | `MACCHIATO` | FR-001 |
| 4 | `"mocha"` | `MOCHA` | FR-001 |
| 5 | `null` | `MOCHA` | FR-005 — never-set default |
| 6 | `""` | `MOCHA` | FR-006 — blank treated as absent |
| 7 | `"dracula"` | `MOCHA` | FR-006 — unrecognized value |
| 8 | `"Latte"` | `MOCHA` | FR-006 — `id` matching is exact, not case-folded (see [data-model.md §5](../data-model.md#5-themepreferencestore--resolvethemeflavor) row 6 for rationale) |

Full detail: [data-model.md §5](../data-model.md#5-themepreferencestore--resolvethemeflavor).

---

## 3. Theme preference store

```kotlin
interface ThemePreferenceStore {
    /** Synchronous. Safe to call before the first Activity attaches. */
    fun read(): String?

    /** Synchronous commit. */
    fun write(id: String)
}
```

**Guarantees**

| ID | Guarantee | Requirement |
|---|---|---|
| TP-1 | `read()` never blocks on I/O beyond a `SharedPreferences` load and never throws | FR-004 |
| TP-2 | `read()` after `write(id)` returns `id` in the same process, immediately | — |
| TP-3 | The value survives process death, activity recreation, and device rotation | FR-004 |
| TP-4 | `write()` performs **no** Room access and **no** `WidgetConfigStore` access | FR-012 |
| TP-5 | An unrecognized stored value is not "repaired" on read — it is passed to `resolveThemeFlavor`, which applies row 7/8 of §2 | FR-006 |

**Storage**: `SharedPreferences("choice_app_prefs", MODE_PRIVATE)`, new key `"theme_flavor"` — the
same file the `004` language preference already uses (see
[research.md §3](../research.md#3-persistence-mechanism-precedent-language-preference)), still a
different file from `ChoiceDatabase` and `WidgetConfigStore`, so TP-4 is structurally guaranteed.

---

## 4. App-wide application (no restart)

```kotlin
class AppThemeState(initial: ThemeFlavor) {
    val flavor: StateFlow<ThemeFlavor>
    fun set(flavor: ThemeFlavor)
}

@Composable
fun ChoiceTheme(flavor: ThemeFlavor, content: @Composable () -> Unit)
```

| Call site | Responsibility |
|---|---|
| `ChoiceApplication.onCreate` | Construct `themePreferenceStore`; construct `themeState = AppThemeState(resolveThemeFlavor(themePreferenceStore.read()))` |
| `MainActivity.setContent` | `val flavor by application.themeState.flavor.collectAsState(); ChoiceTheme(flavor) { ChoiceNavHost(...) }` |
| `SingleCoinWidgetConfigActivity` / `QuickCoinsWidgetConfigActivity` | One-shot: `ChoiceTheme(resolveThemeFlavor(themePreferenceStore.read())) { ... }` — these activities render once and close; they do not need to observe live changes made from elsewhere in the same session |
| `SettingsViewModel.selectTheme(flavor)` | `if (flavor == current) return` (no-op reselect) → `store.write(flavor.id)` → `themeState.set(flavor)` → `refreshWidgets()` |

**Guarantees**

| ID | Guarantee | Requirement |
|---|---|---|
| TA-1 | Selecting a theme recomposes every screen, dialog, and bottom sheet currently on screen within the same frame — no activity recreate, no navigation change | FR-007, FR-008, SC-002 |
| TA-2 | Re-selecting the already-active flavor is a no-op — no store write, no state update, no widget refresh | Principle I, Edge Case (rapid re-selection) |
| TA-3 | A theme change performs no Room access and no `WidgetConfigStore` access | FR-012, SC-006 |
| TA-4 | Device rotation reads the same live `AppThemeState` instance (held on `ChoiceApplication`, not per-Activity) — no re-resolution, no flash | US2 Acceptance Scenario 2 |
| TA-5 | Typography (`ChoiceTypography`) is passed to `MaterialTheme` unchanged regardless of `flavor` | FR-011 |
| TA-6 | Two rapid selections leave `AppThemeState.flavor` holding only the most recent value — no intermediate/mixed-color state is observable | Edge Case (rapid re-selection) |

---

## 5. Extended color roles

```kotlin
data class ChoiceExtendedColors(
    val success: Color, val onSuccess: Color, val successContainer: Color, val onSuccessContainer: Color,
    val warning: Color, val onWarning: Color, val warningContainer: Color, val onWarningContainer: Color,
    val info: Color, val onInfo: Color, val infoContainer: Color, val onInfoContainer: Color,
)

val LocalChoiceExtendedColors: ProvidableCompositionLocal<ChoiceExtendedColors>
```

`ChoiceTheme` provides this alongside `MaterialTheme` so any current or future screen can read
`LocalChoiceExtendedColors.current.success` (etc.) the same way it reads
`MaterialTheme.colorScheme.primary`. "Disabled text" is **not** part of this set — use
`MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)` (standard Material3 convention).

Full role table: [data-model.md §4](../data-model.md#4-choiceextendedcolors).

---

## 6. Color-role → contrast coverage (FR-009 / FR-010 / SC-004)

Every role FR-009 names, and where it lives:

| FR-009 role | Token | Contrast pairing checked |
|---|---|---|
| Background | `colorScheme.background` | — (base surface) |
| Surface | `colorScheme.surface` | vs. `onSurface` |
| Elevated surface | `colorScheme.surfaceVariant` | vs. `onSurfaceVariant` |
| Primary text | `colorScheme.onBackground` / `onSurface` | vs. `background` / `surface` |
| Secondary text | `colorScheme.onSurfaceVariant` | vs. `background` / `surface` |
| Disabled text | `colorScheme.onSurface` @ 38% alpha | vs. `background` (3:1 large-text-equivalent target) |
| Primary accent | `colorScheme.primary` | vs. `onPrimary` |
| Secondary accent | `colorScheme.secondary` | vs. `onSecondary` |
| Border | `colorScheme.outline` | vs. `background` |
| Divider | `colorScheme.outlineVariant` | vs. `background` |
| Success | `extendedColors.success` | vs. `onSuccess` |
| Warning | `extendedColors.warning` | vs. `onWarning` |
| Error | `colorScheme.error` | vs. `onError` |
| Informational | `extendedColors.info` | vs. `onInfo` |
| Interactive state | `colorScheme.primary` + M3 standard state-layer opacities (8/12/16%) | inherits primary/onPrimary contrast |

**Guarantee CR-1**: a JVM unit test computes the WCAG contrast ratio for every row above, for all
four flavors, and asserts ≥ 4.5:1 (normal text roles) or ≥ 3:1 (large-text/graphical roles:
disabled text, border, divider) — see [research.md §7](../research.md#7-contrast-validation-sc-004)
and [quickstart.md](../quickstart.md).

---

## 7. Settings "Appearance" section

Not a new route — an added section inside the existing `SettingsScreen` (`route = "settings"`).

| Element | Content | String key |
|---|---|---|
| Section label | "Appearance" | `settings_appearance_label` |
| Option row × 4 | name + description + swatch + `RadioButton` | `theme_<id>_name`, `theme_<id>_description` |

**Row content**:

| Flavor | Name key | Description key |
|---|---|---|
| Latte | `theme_latte_name` | `theme_latte_description` (e.g. "Light Catppuccin") |
| Frappé | `theme_frappe_name` | `theme_frappe_description` (e.g. "Dark Catppuccin") |
| Macchiato | `theme_macchiato_name` | `theme_macchiato_description` |
| Mocha | `theme_mocha_name` | `theme_mocha_description` |

**Guarantees**

| ID | Guarantee | Requirement |
|---|---|---|
| ST-1 | The currently active flavor is visibly indicated (selected `RadioButton`) | FR-003 |
| ST-2 | Each row shows name, description, and a swatch (`ThemeFlavor.swatchColors`) without switching | FR-002 |
| ST-3 | All four row texts render via `stringResource(...)`; none is a hardcoded literal | FR-016 |
| ST-4 | The section mirrors correctly (row content, radio indicator position, selected-state marker) when the app locale is Arabic — achieved by using only logical (`start`/`end`) layout, per the app's existing RTL contract (see `004`'s `localization-contract.md §7`) — no theme-specific RTL code is needed | FR-016, SC-008 |
| ST-5 | Selecting a row triggers exactly the flow in §4 — no Room access, no navigation | FR-012 |
