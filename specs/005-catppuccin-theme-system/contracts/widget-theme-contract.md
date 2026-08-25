# Contract: Widget Theme Application & Refresh

**Feature**: `005-catppuccin-theme-system` | **Type**: internal widget contract

**Consumers**: `SingleCoinWidget`, `QuickCoinsWidget`, `WidgetGlanceTheme`,
`WidgetRefreshCoordinator`.

**Builds on**: the `003-android-widgets` widget contracts and the `004`
`widget-localization-contract.md` refresh pattern, extended without changing either's behavior.

---

## 1. Per-flavor widget colors

```kotlin
data class WidgetThemeColors(
    val background: ColorProvider, val surface: ColorProvider, val surfaceVariant: ColorProvider,
    val textPrimary: ColorProvider, val textSecondary: ColorProvider, val accent: ColorProvider,
)

object WidgetGlanceTheme {
    fun colorsFor(flavor: ThemeFlavor): WidgetThemeColors
}

val LocalWidgetColors: ProvidableCompositionLocal<WidgetThemeColors>
```

| Role | Palette source | Requirement |
|---|---|---|
| `background` | `base` | FR-013 |
| `surface` | `surface0` | FR-013 |
| `surfaceVariant` | `surface1` | FR-013 |
| `textPrimary` | `text` | FR-013, FR-015 |
| `textSecondary` | `subtext0` | FR-013, FR-015 |
| `accent` | `blue` | FR-013 |

For `ThemeFlavor.MOCHA`, `colorsFor(MOCHA)` reproduces the current six `WidgetGlanceTheme`
`ColorProvider` values exactly — confirmed against `WidgetGlanceTheme.kt`'s existing constants.

**Guarantees**

| ID | Guarantee | Requirement |
|---|---|---|
| WT-1 | `colorsFor` is a pure function of `flavor` — no I/O, no `Context` | — |
| WT-2 | Text/background pairs use each flavor's own `text`/`subtext0` against `base`/`surface0`/`surface1`, preserving the same contrast relationship Mocha already has today | FR-015 |
| WT-3 | If a platform surface (e.g. a widget preview thumbnail) cannot render the exact `ColorProvider` value, the closest available representation is used — this is a platform rendering concern, not a `WidgetGlanceTheme` API concern; `colorsFor` always returns the flavor's true colors | FR-015 (US3 Acceptance Scenario 3) |

---

## 2. Where the flavor comes from, inside a widget

```kotlin
override suspend fun provideGlance(context: Context, id: GlanceId) {
    val flavor = resolveThemeFlavor(SharedPreferencesThemeStore(context).read())
    val colors = WidgetGlanceTheme.colorsFor(flavor)
    val state = resolveState(context, id)   // unaffected by flavor — data lookup only
    provideContent {
        CompositionLocalProvider(LocalWidgetColors provides colors) {
            WidgetContent(state)             // reads LocalWidgetColors.current.* internally
        }
    }
}
```

| ID | Guarantee | Requirement |
|---|---|---|
| WP-1 | Every widget composable reads colors through `LocalWidgetColors.current`, never a hardcoded literal and never the app-side `MaterialTheme.colorScheme` (Glance cannot consume it) | FR-013 |
| WP-2 | The flavor is read once per `provideGlance` invocation, not once per composable | — |
| WP-3 | `resolveState` (repository + `WidgetConfigStore` access) is unaffected by flavor resolution — it returns coin/decision data only | FR-012 |

---

## 3. Refresh on theme change (FR-014)

`WidgetRefreshCoordinator.refreshAll()` — **unchanged**, reused as-is:

```kotlin
fun refreshAll() {
    scope.launch {
        SingleCoinWidget().updateAll(context)
        QuickCoinsWidget().updateAll(context)
    }
}
```

### Triggers

| Trigger | Source | Path |
|---|---|---|
| Coin data change | `repo.observeCoins()` | existing, unchanged |
| Language change | `SettingsScreen` selection | existing, unchanged (`004`) |
| **Theme change** | `SettingsScreen` Appearance selection | **new** — `SettingsViewModel.selectTheme` calls the same `refreshWidgets()` lambda already wired for language |

No new receiver, no new coroutine scope, no new dependency — this is the identical call already
proven for FR-031 in `004`, invoked from a second call site.

**Guarantees**

| ID | Guarantee | Requirement |
|---|---|---|
| WR-1 | After any theme change, every placed widget instance of both types re-renders in the new flavor's colors at the next normal `updateAll` cycle | FR-014, SC-007 |
| WR-2 | No user action (remove/re-add, tap, app relaunch) is needed for WR-1 | FR-014 |
| WR-3 | A refresh re-renders; it never re-configures — `WidgetConfigStore` (which coin(s) a widget instance shows) is not written | FR-012 |
| WR-4 | A widget's last decision result survives the refresh unchanged — `provideGlance` re-reads it from the repository exactly as before; only the color mapping changed | FR-014 |
| WR-5 | If the host app process is not running when the theme changes from a still-running Settings UI (should not occur in practice, since the change originates in-app), or the widget itself is not currently placed, `refreshAll()` remains a cheap no-op — existing `updateAll` semantics, unchanged | Edge Case (theme changed while host app not running) |
| WR-6 | A widget that has never been able to refresh since a theme change (e.g., placed during a long device-off period) resolves the **current** stored flavor the next time `provideGlance` runs — flavor resolution reads the live preference each time, never a cached value from placement time | Edge Case, FR-014 |

---

## 4. Widget behaviour explicitly unchanged

| Behaviour | Status |
|---|---|
| `WidgetConfigStore` keys and encoding | unchanged |
| Widget → coin association semantics | unchanged |
| `FlipSingleCoinAction` / flip decision logic | unchanged — no theming involvement |
| `WidgetFlipGuard` debounce | unchanged |
| `OpenCoinInAppAction` / deep link | unchanged |
| Derived render states (`Unavailable` / `TooFewChoices` / `Ready` / `Result`) | unchanged — only their **colors** now vary by flavor, exactly as they previously varied by nothing (fixed to Mocha) |
| Widget provider XML (`single_coin_widget_info.xml`, `quick_coins_widget_info.xml`) | unchanged |
| Widget string localization (`004`) | unchanged — orthogonal axis (language vs. color), both read independently |
