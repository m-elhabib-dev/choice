# Contract: Widget Localization & Refresh

**Feature**: `004-arabic-localization-support` | **Type**: internal widget contract

**Consumers**: `SingleCoinWidget`, `QuickCoinsWidget`, `SingleCoinWidgetConfigActivity`,
`QuickCoinsWidgetConfigActivity`, `WidgetRefreshCoordinator`, `ChoiceApplication`.

**Builds on**: the `003-android-widgets` widget contracts, which this feature extends without
changing any of their behaviour.

---

## 1. Localized rendering context

Every widget render resolves its strings through a **localized context**, on every API level.

```kotlin
override suspend fun provideGlance(context: Context, id: GlanceId) {
    val localized = LocaleApplier.localizedContext(context)   // §4 of localization-contract.md
    val state = resolveState(context, id)                     // data lookup: unlocalized context is fine
    provideContent { WidgetContent(state, localized) }
}
```

| ID | Guarantee | Requirement |
|---|---|---|
| WL-1 | Every `getString`/`getQuantityString` in widget code uses the localized context | FR-016 |
| WL-2 | The wrap is unconditional; on API 33+ it is a behavioural no-op | research.md §1 |
| WL-3 | `resolveState` (repository + `WidgetConfigStore` access) is unaffected by locale — it returns IDs and user content only | FR-018, VR-20 |
| WL-4 | Widget config activities localize via `attachBaseContext`, the same as `MainActivity` | FR-016 |

---

## 2. Complete widget string inventory

### 2.1 Already externalized — keep these key names (SR-6)

The 18 keys already in `res/values/strings.xml` need Arabic counterparts in `values-ar/` and
**no rename**:

`app_name`, `widget_single_coin_label`, `widget_single_coin_description`,
`widget_quick_coins_label`, `widget_quick_coins_description`, `widget_state_ready`,
`widget_state_unavailable`, `widget_state_no_coins_selected`, `widget_state_too_few_choices`,
`widget_state_no_coins_at_all`, `widget_action_flip`, `widget_action_reconfigure`,
`widget_action_open_app`, `widget_action_create_coin`, `widget_config_choose_coin_title`,
`widget_config_use_favorites_title`, `widget_config_use_favorites_description`,
`widget_config_confirm_selection`.

**`app_name` is not translated** — the product name "Choice" stays "Choice" in both files
(a brand name, not UI text). It stays in `values/` and is deliberately absent from `values-ar/`;
this is the one intentional exception to SR-2, and it must be marked
`<string name="app_name" translatable="false">` so Lint's `MissingTranslation` does not flag it.

### 2.2 New keys — currently hardcoded in widget Kotlin

| Current literal | Location | New key | Kind |
|---|---|---|---|
| `"${…choices.size} choices"` | `SingleCoinWidgetConfigActivity.kt:187` | `coin_choice_count` | **`<plurals>`** (SR-8) |
| `"$coinName, ${…open_app}"` | `SingleCoinWidget.kt:227`, `QuickCoinsWidget.kt:247` | `cd_widget_open_app` | `%1$s` |
| `"$coinName: ${…state_ready}"` | `SingleCoinWidget.kt:165`, `QuickCoinsWidget.kt:269` | `cd_widget_coin_ready` | `%1$s` |
| `"$coinName result: ${resultText}"` | `SingleCoinWidget.kt:202`, `QuickCoinsWidget.kt:277` | `cd_widget_coin_result` | `%1$s`, `%2$s` |
| `"${…action_flip} $coinName"` | `QuickCoinsWidget.kt:302` | `cd_widget_flip_coin` | `%1$s` |
| `"$label $coinName"` | `SingleCoinWidget.kt:245` | `cd_widget_action_for_coin` | `%1$s`, `%2$s` |

**Every one of these is an SR-3 violation today** — a translated fragment concatenated in Kotlin.
Each becomes a single resource with positional placeholders, so Arabic can reorder the parts:

```xml
<!-- values/strings.xml -->
<string name="cd_widget_coin_result">%1$s result: %2$s</string>
<!-- values-ar/strings.xml -->
<string name="cd_widget_coin_result">%1$s: النتيجة %2$s</string>
```

```kotlin
val bidi = BidiFormatter.getInstance()
contentDescription = localized.getString(
    R.string.cd_widget_coin_result,
    bidi.unicodeWrap(state.coinName),      // user content → wrapped (§5.3)
    bidi.unicodeWrap(state.resultText),
)
```

---

## 3. Widget layout direction

| ID | Rule | Requirement |
|---|---|---|
| WD-1 | `android:supportsRtl="true"` on `<application>` governs RemoteViews RTL resolution — already present | FR-017 |
| WD-2 | Use `androidx.glance.layout.Alignment.Start/End`; never `Left/Right` | FR-017 |
| WD-3 | Use `androidx.glance.text.TextAlign.Start/End/Center`; never `Left/Right` | FR-017 |
| WD-4 | Use `GlanceModifier.padding(start =/end =)`; never absolute padding | FR-007 |
| WD-5 | Never set `layoutDirection` on generated RemoteViews — manual mirroring is forbidden | FR-005 |

**Current state**: `QuickCoinsWidget.kt:256` uses `TextAlign.Start`,
`QuickCoinsWidgetConfigActivity.kt:220` uses `padding(start = 8.dp)`, and
`SingleCoinWidget.kt:145,199` use direction-neutral `TextAlign.Center`. The widget layer is
**already compliant**; WD-1 … WD-5 are a regression guard for the string-externalization pass.

---

## 4. Immediate refresh on language change (FR-031)

`WidgetRefreshCoordinator` gains a second trigger alongside its existing `observeCoins()`
collection. Its existing behaviour is unchanged.

```kotlin
class WidgetRefreshCoordinator(private val context: Context, private val repo: CoinRepository) {
    fun start() { /* existing: collect repo.observeCoins() -> refreshAll() */ }

    /** New. Idempotent, safe to call from any thread. */
    fun refreshAll() {
        scope.launch {
            SingleCoinWidget().updateAll(context)
            QuickCoinsWidget().updateAll(context)
        }
    }
}
```

### Triggers

| Trigger | Source | Path |
|---|---|---|
| Coin data change | `repo.observeCoins()` | **existing**, unchanged |
| In-app language change | `SettingsScreen` selection | step 4 of the selection flow ([navigation-contract §2](./navigation-contract.md)) |
| Device system-language change | `Intent.ACTION_LOCALE_CHANGED` | **new** manifest-declared `<receiver>` → `refreshAll()` |

The `ACTION_LOCALE_CHANGED` receiver must be **manifest-declared**, not runtime-registered: the app
process may not be alive when the user changes the system language, and a runtime receiver would
miss the broadcast entirely, leaving widgets stale in the old language.

```xml
<receiver android:name=".widget.LocaleChangedReceiver" android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.LOCALE_CHANGED" />
    </intent-filter>
</receiver>
```

### Guarantees

| ID | Guarantee | Requirement |
|---|---|---|
| WR-1 | After any language change, every placed widget instance of both types re-renders in the new language | FR-031, SC-007 |
| WR-2 | No user action (remove/re-add, tap, app launch) is needed for WR-1 | FR-031 |
| WR-3 | A refresh re-renders; it never re-configures. `WidgetConfigStore` is not written | FR-025, VR-20 |
| WR-4 | A widget's last decision result survives the refresh unchanged — `provideGlance` re-reads it from the repository | FR-018, VR-21, SC-007 |
| WR-5 | `refreshAll()` is a cheap no-op when zero instances of a widget type are placed | existing `updateAll` semantics |
| WR-6 | An `ACTION_LOCALE_CHANGED` broadcast while the in-app override is set relabels widgets to the **override** language, not the new system language | FR-026 |

WR-6 is the subtle one: the receiver's job is to re-render, and `provideGlance` resolves the
language through `LocaleApplier.localizedContext`, which consults the stored preference. So an
override is honoured automatically — the receiver must **not** read the system locale itself.

---

## 5. Widget behaviour that is explicitly unchanged

| Behaviour | Status |
|---|---|
| `WidgetConfigStore` keys and encoding | unchanged |
| Widget → coin association semantics | unchanged (FR-025) |
| `FlipSingleCoinAction` / flip decision logic | unchanged — no localization involvement |
| `WidgetFlipGuard` debounce | unchanged |
| `OpenCoinInAppAction` / `EXTRA_OPEN_COIN_ID` deep link | unchanged |
| Derived render states (`Unavailable` / `TooFewChoices` / `Ready` / `Result`) | unchanged — only their **labels** localize |
| `WidgetGlanceTheme` (Catppuccin Mocha) | unchanged (FR-028) |
| Widget provider XML (`single_coin_widget_info.xml`, `quick_coins_widget_info.xml`) | unchanged — already reference `@string/` labels |
