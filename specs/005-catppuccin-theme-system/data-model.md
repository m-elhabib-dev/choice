# Data Model: Catppuccin Theme System

**Feature**: `005-catppuccin-theme-system` | **Input**: [spec.md](./spec.md), [research.md](./research.md)

This feature adds no Room entity and no schema migration — it is a presentation-layer concern, kept
structurally isolated from `ChoiceDatabase` and `WidgetConfigStore` (FR-012). The entities below are
in-memory/preference-layer Kotlin types.

---

## 1. `CatppuccinPalette`

The 26 official Catppuccin named colors for one flavor, plus whether that flavor is visually light.

```kotlin
data class CatppuccinPalette(
    val rosewater: Color, val flamingo: Color, val pink: Color, val mauve: Color,
    val red: Color, val maroon: Color, val peach: Color, val yellow: Color,
    val green: Color, val teal: Color, val sky: Color, val sapphire: Color,
    val blue: Color, val lavender: Color,
    val text: Color, val subtext1: Color, val subtext0: Color,
    val overlay2: Color, val overlay1: Color, val overlay0: Color,
    val surface2: Color, val surface1: Color, val surface0: Color,
    val base: Color, val mantle: Color, val crust: Color,
    val isLight: Boolean,
)
```

**Validation rules**: none at runtime — these are four fixed, compile-time `val` instances (below),
never user input, never persisted as a whole (only the owning flavor's `id` is persisted).

**Four instances** (`app/src/main/kotlin/com/choice/app/ui/theme/Color.kt`), official Catppuccin
hex values, one flavor's `Base`/`Text` pair always contrasting *within that flavor*:

| Role | Latte (light) | Frappé | Macchiato | Mocha *(unchanged)* |
|---|---|---|---|---|
| rosewater | `#DC8A78` | `#F2D5CF` | `#F4DBD6` | `#F5E0DC` |
| flamingo | `#DD7878` | `#EEBEBE` | `#F0C6C6` | `#F2CDCD` |
| pink | `#EA76CB` | `#F4B8E4` | `#F5BDE6` | `#F5C2E7` |
| mauve | `#8839EF` | `#CA9EE6` | `#C6A0F6` | `#CBA6F7` |
| red | `#D20F39` | `#E78284` | `#ED8796` | `#F38BA8` |
| maroon | `#E64553` | `#EA999C` | `#EE99A0` | `#EBA0AC` |
| peach | `#FE640B` | `#EF9F76` | `#F5A97F` | `#FAB387` |
| yellow | `#DF8E1D` | `#E5C890` | `#EED49F` | `#F9E2AF` |
| green | `#40A02B` | `#A6D189` | `#A6DA95` | `#A6E3A1` |
| teal | `#179299` | `#81C8BE` | `#8BD5CA` | `#94E2D5` |
| sky | `#04A5E5` | `#99D1DB` | `#91D7E3` | `#89DCEB` |
| sapphire | `#209FB5` | `#85C1DC` | `#7DC4E4` | `#74C7EC` |
| blue | `#1E66F5` | `#8CAAEE` | `#8AADF4` | `#89B4FA` |
| lavender | `#7287FD` | `#BABBF1` | `#B7BDF8` | `#B4BEFE` |
| text | `#4C4F69` | `#C6D0F5` | `#CAD3F5` | `#CDD6F4` |
| subtext1 | `#5C5F77` | `#B5BFE2` | `#B8C0E0` | `#BAC2DE` |
| subtext0 | `#6C6F85` | `#A5ADCE` | `#A5ADCB` | `#A6ADC8` |
| overlay2 | `#7C7F93` | `#949CBB` | `#939AB7` | `#9399B2` |
| overlay1 | `#8C8FA1` | `#838BA7` | `#8087A2` | `#7F849C` |
| overlay0 | `#9CA0B0` | `#737994` | `#6E738D` | `#6C7086` |
| surface2 | `#ACB0BE` | `#626880` | `#5B6078` | `#585B70` |
| surface1 | `#BCC0CC` | `#51576D` | `#494D64` | `#45475A` |
| surface0 | `#CCD0DA` | `#414559` | `#363A4F` | `#313244` |
| base | `#EFF1F5` | `#303446` | `#24273A` | `#1E1E2E` |
| mantle | `#E6E9EF` | `#292C3C` | `#1E2030` | `#181825` |
| crust | `#DCE0E8` | `#232634` | `#181926` | `#11111B` |
| `isLight` | `true` | `false` | `false` | `false` |

Mocha's column is exactly the current `Color.kt` — confirmed byte-for-byte against
`app/src/main/kotlin/com/choice/app/ui/theme/Color.kt:6-31` — so migrating it into this structure
changes zero visible pixels (Assumption in spec.md, last bullet). **Task-level note**: cross-check
Latte/Frappé/Macchiato hex values against the upstream `catppuccin/palette` source during
implementation before shipping; they are recorded here from the well-established public palette
but were not fetched from a live source in this planning pass.

---

## 2. `ThemeFlavor`

The selectable entity from the spec's Key Entities section ("Color Theme (Flavor)").

```kotlin
enum class ThemeFlavor(
    val id: String,                     // persisted value — stable, never localized, never reused
    val isLight: Boolean,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    internal val palette: CatppuccinPalette,
) {
    LATTE("latte", true, R.string.theme_latte_name, R.string.theme_latte_description, LattePalette),
    FRAPPE("frappe", false, R.string.theme_frappe_name, R.string.theme_frappe_description, FrappePalette),
    MACCHIATO("macchiato", false, R.string.theme_macchiato_name, R.string.theme_macchiato_description, MacchiatoPalette),
    MOCHA("mocha", false, R.string.theme_mocha_name, R.string.theme_mocha_description, MochaPalette),
    ;

    val colorScheme: ColorScheme by lazy { buildColorScheme(palette) }
    val extendedColors: ChoiceExtendedColors by lazy { buildExtendedColors(palette) }
    val swatchColors: List<Color> get() = listOf(palette.base, palette.mauve, palette.blue)

    companion object {
        val DEFAULT: ThemeFlavor = MOCHA
        fun fromId(id: String?): ThemeFlavor? = entries.find { it.id == id }
    }
}
```

**Fields**:

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | Persisted verbatim in `SharedPreferences`. ASCII, lowercase, stable — never shown to users, never translated (mirrors `templateId` from `004`'s stable-ID lesson). |
| `isLight` | `Boolean` | Selects `lightColorScheme(...)` vs `darkColorScheme(...)` when building `colorScheme`. Only `LATTE` is `true`. |
| `nameRes` / `descriptionRes` | `@StringRes Int` | Resolved via `stringResource(...)` — never a hardcoded literal (FR-016). |
| `palette` | `CatppuccinPalette` | `internal` — visible module-wide (app UI *and* widget package), not part of the public surface of this enum. |
| `colorScheme` | `ColorScheme` | Derived, cached per flavor (`by lazy`) — same instance reused across every recomposition and every screen. |
| `extendedColors` | `ChoiceExtendedColors` | Success/warning/info roles (§4) — derived, cached the same way. |
| `swatchColors` | `List<Color>` | Three representative colors (`base`, `mauve`, `blue`) for the FR-002 preview chip; computed, not stored. |

**Validation rules**:
- Exactly four values exist (FR-001) — enforced by the type itself (a `Kotlin enum`, not an open
  set).
- `fromId` returns `null` for anything not matching one of the four `id`s — including `null`,
  blank, or an unrecognized string — which is exactly the input `resolveThemeFlavor` (§5) needs to
  apply the FR-006 fallback.

**State transitions**: A `ThemeFlavor` value itself has no lifecycle — the *selection* (which one
is current) is the only mutable state, modeled by `AppThemeState` (§6) and persisted by
`ThemePreferenceStore` (§5). Transitioning from any flavor to any other (including to itself, which
must be a no-op — Edge Case in spec.md) never mutates a `ThemeFlavor` value; it only changes which
one `AppThemeState.flavor` currently points to.

---

## 3. `WidgetThemeColors`

The six-role subset `WidgetGlanceTheme` already uses today, now parameterized instead of static.

```kotlin
data class WidgetThemeColors(
    val background: ColorProvider,
    val surface: ColorProvider,
    val surfaceVariant: ColorProvider,
    val textPrimary: ColorProvider,
    val textSecondary: ColorProvider,
    val accent: ColorProvider,
)
```

| Role | Source (per flavor's `palette`) |
|---|---|
| `background` | `base` |
| `surface` | `surface0` |
| `surfaceVariant` | `surface1` |
| `textPrimary` | `text` |
| `textSecondary` | `subtext0` |
| `accent` | `blue` |

Produced by `WidgetGlanceTheme.colorsFor(flavor: ThemeFlavor): WidgetThemeColors`, a pure function
of the enum value (no I/O). For Mocha, this reproduces the exact current `WidgetGlanceTheme`
values byte-for-byte (confirmed against `WidgetGlanceTheme.kt`'s current six `ColorProvider`s).

---

## 4. `ChoiceExtendedColors`

The three status-color roles Material3's `ColorScheme` has no slot for, needed to satisfy FR-009's
full role list.

```kotlin
data class ChoiceExtendedColors(
    val success: Color, val onSuccess: Color, val successContainer: Color, val onSuccessContainer: Color,
    val warning: Color, val onWarning: Color, val warningContainer: Color, val onWarningContainer: Color,
    val info: Color, val onInfo: Color, val infoContainer: Color, val onInfoContainer: Color,
)
```

| Role pair | Source | Mirrors existing formula for |
|---|---|---|
| `success` / `onSuccess` / `successContainer` / `onSuccessContainer` | `green` / `base` / `surface0` / `text` | `error`/`onError`/`errorContainer`/`onErrorContainer` |
| `warning` / `onWarning` / `warningContainer` / `onWarningContainer` | `yellow` / `base` / `surface0` / `text` | same shape |
| `info` / `onInfo` / `infoContainer` / `onInfoContainer` | `sky` / `base` / `surface0` / `text` | same shape |

Not currently rendered by any screen (confirmed — no existing success/warning UI exists), so
defining this role set changes zero visible pixels for Mocha today; it exists so a future
success/warning/info UI element has exactly one place to source its color from, in any of the four
flavors, satisfying FR-009 without speculative UI work (Principle VIII).

**"Disabled text" role** (also required by FR-009): intentionally **not** a stored field here.
Represented the standard Material3 way — `MaterialTheme.colorScheme.onSurface.copy(alpha =
0.38f)` — so it always tracks whichever flavor is active with no separate lookup.

---

## 5. `ThemePreferenceStore` / `resolveThemeFlavor`

The persisted preference and its resolution, mirroring `LanguagePreferenceStore` /
`resolveLanguageTag`.

```kotlin
interface ThemePreferenceStore {
    fun read(): String?          // null = never set
    fun write(id: String)
}
```

| Guarantee | Statement | Requirement |
|---|---|---|
| TP-1 | `read()` performs at most one `SharedPreferences` load, never throws, never blocks on network | FR-004 |
| TP-2 | `read()` immediately after `write(id)` in the same process returns `id` | — |
| TP-3 | The value survives process death, activity recreation, and device rotation | FR-004 |
| TP-4 | `write()` makes zero calls into `ChoiceDatabase` or `WidgetConfigStore` | FR-012 |
| TP-5 | An unrecognized stored value is not corrected on `read()` — it is returned as-is; `resolveThemeFlavor` applies the fallback | FR-006 |

```kotlin
/** Pure. No Android dependency. Unit-tested on the JVM. */
fun resolveThemeFlavor(storedId: String?): ThemeFlavor =
    storedId?.let { ThemeFlavor.fromId(it) } ?: ThemeFlavor.DEFAULT
```

| # | `storedId` | Result | Requirement |
|---|---|---|---|
| 1 | `"latte"` | `LATTE` | FR-001 |
| 2 | `"mocha"` | `MOCHA` | FR-001 |
| 3 | `null` (never set) | `MOCHA` (`DEFAULT`) | FR-005 |
| 4 | `""` | `MOCHA` | FR-006 |
| 5 | `"dracula"` (unrecognized) | `MOCHA` | FR-006 |
| 6 | `"Latte"` (wrong case) | `MOCHA` — `id`s are lowercase-exact, not case-folded; a corrupted/foreign value falls back, it is not "repaired" | FR-006 |

Row 6 deliberately does **not** case-fold, unlike the language resolver's tag matching — `id` is an
internal, never-user-typed, never-displayed constant (unlike a BCP-47 language tag, which a system
can legitimately present in mixed case). Any mismatch is corrupt/foreign data, not a
legitimate-but-differently-cased value, so FR-006's "unrecognized value" fallback is the correct
outcome, not a normalization step.

**Storage**: `SharedPreferences("choice_app_prefs", MODE_PRIVATE)`, new key `"theme_flavor"` —
same file the language preference already uses (see [research.md §3](./research.md)), independent
of `ChoiceDatabase` and `WidgetConfigStore`.

---

## 6. `AppThemeState`

The in-memory, app-wide "currently active" pointer that makes the selection visible without a
restart.

```kotlin
class AppThemeState(initial: ThemeFlavor) {
    private val _flavor = MutableStateFlow(initial)
    val flavor: StateFlow<ThemeFlavor> = _flavor.asStateFlow()
    fun set(flavor: ThemeFlavor) { _flavor.value = flavor }
}
```

| Guarantee | Statement | Requirement |
|---|---|---|
| AS-1 | Seeded once, synchronously, from `resolveThemeFlavor(store.read())` at `ChoiceApplication.onCreate` — before `MainActivity.setContent` runs | FR-007, cold-start clarification |
| AS-2 | `set()` is a plain value replace — the last call wins if two selections happen in quick succession (Edge Case in spec.md: no intermediate/mixed state) | Edge Case |
| AS-3 | Read by `MainActivity` via `collectAsState()`; never read by `SettingsViewModel` to decide UI — the ViewModel only *writes* to it and to the store | Unidirectional data flow |
| AS-4 | Holds no reference to `Context`, `ChoiceDatabase`, or `WidgetConfigStore` — theme state cannot leak into or be affected by unrelated data (FR-012) | FR-012 |

Held once on `ChoiceApplication` (same lifecycle pattern as `WidgetRefreshCoordinator`), not
per-Activity — so a config-change-driven Activity re-creation (device rotation) reads the same
live `StateFlow` instance rather than re-resolving the preference, which is exactly why rotation
exhibits no flash (Edge Case / Acceptance Scenario US2.2): the value was never lost, only the
Activity recreated around it.

---

## Entity relationship summary

```
ThemeFlavor (enum, 4 fixed values)
 ├─ palette: CatppuccinPalette ──────────► buildColorScheme()   → ColorScheme      (app-wide, MaterialTheme)
 │                                  └────► buildExtendedColors() → ChoiceExtendedColors (success/warning/info)
 │                                  └────► WidgetGlanceTheme.colorsFor() → WidgetThemeColors (widgets)
 ├─ id: String ───────────────────────────► persisted by ThemePreferenceStore
 └─ nameRes/descriptionRes ───────────────► rendered in Settings "Appearance" section

AppThemeState.flavor: StateFlow<ThemeFlavor>   — drives ChoiceTheme's recomposition (app)
ThemePreferenceStore                            — drives WidgetGlanceTheme.colorsFor() (widgets, next provideGlance)
```

No entity here is a Room `@Entity`; none is written to `ChoiceDatabase`; none appears in a database
migration. This is intentional and load-bearing for FR-012/SC-006.
