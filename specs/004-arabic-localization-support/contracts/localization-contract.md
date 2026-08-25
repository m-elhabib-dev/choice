# Contract: Localization Resource & Locale Resolution

**Feature**: `004-arabic-localization-support` | **Type**: internal API + resource contract

**Consumers**: every Compose screen, every Glance widget, both widget config activities,
`ChoiceApplication`, translators.

---

## 1. Supported languages

```kotlin
object SupportedLanguages {
    const val ENGLISH = "en"
    const val ARABIC  = "ar"
    const val DEFAULT = ENGLISH

    /** Ordered; first entry is the default/fallback. */
    val TAGS: List<String> = listOf(ENGLISH, ARABIC)
}
```

| Tag | Resource folder | Direction | Numbering system (formatting) |
|---|---|---|---|
| `en` | `res/values/` (default) | LTR | `latn` |
| `ar` | `res/values-ar/` | RTL | `latn` — **forced**, not CLDR's default `arab` |

**FR-029 rule**: adding a language means appending a tag to `TAGS`, adding one
`res/values-<tag>/strings.xml`, and adding one `<locale>` line to `res/xml/locales_config.xml`.
**No other file may need to change.** Any implementation that requires editing decision logic,
navigation, or formatting code to add a third language violates this contract.

---

## 2. Locale resolution

```kotlin
/** Pure. No Android dependency. Unit-tested in app/src/test. */
fun resolveLanguageTag(
    storedTag: String?,          // null = "follow system"
    systemLanguageTags: List<String>,  // e.g. ["fr-FR", "en-US"], highest priority first
): String
```

### Behaviour table

| # | `storedTag` | `systemLanguageTags` | Result | Requirement |
|---|---|---|---|---|
| 1 | `"ar"` | `["en-US"]` | `"ar"` | FR-026 — explicit override beats system |
| 2 | `"en"` | `["ar-EG"]` | `"en"` | FR-026 |
| 3 | `null` | `["ar-EG", "en-US"]` | `"ar"` | FR-024 — follows system |
| 4 | `null` | `["en-GB"]` | `"en"` | FR-024 |
| 5 | `null` | `["fr-FR"]` | `"en"` | FR-027 — unsupported system language → English |
| 6 | `null` | `["fr-FR", "ar-SA"]` | `"ar"` | FR-027 — first *supported* entry wins |
| 7 | `null` | `[]` | `"en"` | FR-027 |
| 8 | `"de"` | `["ar-EG"]` | `"en"` | FR-027 — unsupported *stored* tag → English, **not** system |
| 9 | `""` | `["ar-EG"]` | `"ar"` | Blank treated as absent |

Matching is on the **language subtag only** and is case-insensitive: `"ar-EG"`, `"ar_SA"`, and
`"AR"` all match `"ar"` (the spec's Assumptions scope Arabic to Modern Standard Arabic, with no
regional variants).

Row 8 is the one that is easy to get wrong: a stored-but-unsupported tag (a downgrade leftover, or a
future language removed from `TAGS`) must resolve to **English**, not fall through to the system
language. FR-027 says the fallback is English, unconditionally.

### Derived locales

```kotlin
/** Resource resolution + layout direction. */
fun uiLocale(tag: String): Locale = Locale.forLanguageTag(tag)

/** Dates, times, percentages. ALWAYS carries nu-latn (FR-015, SC-006). */
fun formattingLocale(tag: String): Locale =
    Locale.Builder()
        .setLanguage(tag)
        .setUnicodeLocaleKeyword("nu", "latn")
        .build()
```

| Input tag | `uiLocale().toLanguageTag()` | `formattingLocale().toLanguageTag()` |
|---|---|---|
| `"en"` | `en` | `en-u-nu-latn` |
| `"ar"` | `ar` | `ar-u-nu-latn` |

---

## 3. Language preference store

```kotlin
interface LanguagePreferenceStore {
    /** Synchronous. Safe to call before the first Activity attaches. Returns null = follow system. */
    fun read(): String?

    /** Synchronous commit. Passing null clears the override (back to "follow system"). */
    fun write(tag: String?)
}
```

**Guarantees**

| ID | Guarantee | Requirement |
|---|---|---|
| LP-1 | `read()` never blocks on I/O beyond a `SharedPreferences` load and never throws | research.md §1a |
| LP-2 | `read()` after `write(t)` returns `t` in the same process, immediately | — |
| LP-3 | The value survives process death and app restart | FR-026 |
| LP-4 | `write()` performs **no** Room access and **no** Glance state access | FR-025, SC-004 |
| LP-5 | An unrecognized stored value is not "repaired" on read — it is passed to `resolveLanguageTag`, which applies row 8 | FR-027 |

**Storage**: `SharedPreferences("choice_app_prefs", MODE_PRIVATE)`, key `"language_tag"`.
Deliberately a different store from `ChoiceDatabase` and from `WidgetConfigStore`, so LP-4 is
structurally impossible to violate.

---

## 4. Locale application

```kotlin
object LocaleApplier {
    /** Called from ChoiceApplication.onCreate and after every preference change. */
    fun apply(context: Context, tag: String)

    /** Returns a Context whose resources resolve against the app's language. */
    fun localizedContext(context: Context): Context
}
```

| API level | `apply` | `localizedContext` |
|---|---|---|
| 33+ | `LocaleManager.applicationLocales = LocaleList.forLanguageTags(tag)` — system-wide for the app; recreates activities and re-renders widgets automatically | Returns `context` behaviour-identically (the wrap is a no-op) |
| 26–32 | Persists the tag; the caller recreates the visible activity | `context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocales(LocaleList(uiLocale(tag))) })` |

**Call sites** (all four are mandatory — missing any one leaves a surface un-localized on API < 33):

| Call site | Purpose |
|---|---|
| `MainActivity.attachBaseContext` | App UI |
| `SingleCoinWidgetConfigActivity.attachBaseContext` | Widget config UI (FR-016) |
| `QuickCoinsWidgetConfigActivity.attachBaseContext` | Widget config UI (FR-016) |
| `GlanceAppWidget.provideGlance(context, id)` — wrap before any `getString` | Widget rendering (FR-016, FR-017) |

**Guarantees**

| ID | Guarantee | Requirement |
|---|---|---|
| LA-1 | `localizedContext` is idempotent — wrapping twice equals wrapping once | — |
| LA-2 | The wrap is applied on **every** API level; on 33+ it is a no-op | research.md §1 |
| LA-3 | `apply` never mutates Room or Glance widget state | FR-025 |
| LA-4 | Layout direction is **never** set explicitly; it is derived by the platform from the locale | FR-005 |

---

## 5. String resource contract

### 5.1 File layout

```
app/src/main/res/
├── values/strings.xml       # English — the complete default set (FR-003)
├── values-ar/strings.xml    # Arabic  — complete overlay,   zero gaps (FR-004)
└── xml/locales_config.xml   # <locale android:name="en"/> <locale android:name="ar"/>
```

`AndroidManifest.xml` gains `android:localeConfig="@xml/locales_config"` on `<application>`.
`android:supportsRtl="true"` is already present.

### 5.2 Rules

| ID | Rule | Requirement |
|---|---|---|
| SR-1 | No user-facing literal remains in Kotlin — app **or** widget | FR-001, FR-002 |
| SR-2 | Every `values/` key exists in `values-ar/` | FR-004, SC-001 |
| SR-3 | Composed sentences use positional placeholders (`%1$s`, `%2$d`); translated fragments are **never** concatenated in Kotlin | FR-029 |
| SR-4 | No key holds a date/time/number **format pattern** — formats come from CLDR | FR-015 |
| SR-5 | Every interactive control and meaningful visual has a localized content description | FR-022, FR-023 |
| SR-6 | Existing `widget_*` keys keep their current names | backward compatibility with `003-android-widgets` |
| SR-7 | A missing key falls back to `values/` — never a crash, never a placeholder | FR-030 |
| SR-8 | Quantity-varying text uses `<plurals>`, not manual `if (n == 1)` | FR-029 — Arabic has 6 plural categories |

**SR-8 is not optional.** `MainScreen.kt`'s `"$choiceCount choices"` and
`SingleCoinWidgetConfigActivity.kt:187`'s `"${…size} choices"` must become
`<plurals name="coin_choice_count">`. Arabic distinguishes `zero`/`one`/`two`/`few`/`many`/`other`;
an English-shaped singular/plural branch cannot express it, and hardcoding one would violate
FR-029 as well as reading as broken Arabic.

**SR-3 in practice** — the existing widget code violates it today:

```kotlin
// BEFORE — SingleCoinWidget.kt:202, QuickCoinsWidget.kt:277
contentDescription = "${state.coinName} result: ${state.resultText}"

// AFTER
// <string name="cd_widget_coin_result">%1$s result: %2$s</string>
// <string name="cd_widget_coin_result" (ar)>%1$s: النتيجة %2$s</string>
contentDescription = context.getString(
    R.string.cd_widget_coin_result,
    bidi.unicodeWrap(state.coinName),
    bidi.unicodeWrap(state.resultText),
)
```

### 5.3 Bidi wrapping

`BidiFormatter.getInstance().unicodeWrap(text)` (from the already-present `androidx.core:core-ktx`)
is applied to user content **only where it is interpolated** into a localized string or a
content description.

| Situation | Wrap? |
|---|---|
| `Text(coin.name)` standalone | **No** — Compose resolves paragraph direction from the text itself |
| `getString(R.string.x, coin.name)` | **Yes** |
| `contentDescription` built from user content | **Yes** |
| Text that is entirely a static resource | **No** |

---

## 6. Formatting contract

```kotlin
object DecisionTimeFormatter {
    /** Explicit locale + zone: no Locale.getDefault(), so this is unit-testable on the JVM. */
    fun format(epochMillis: Long, locale: Locale, zone: ZoneId): String
}
```

Implementation: `DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)`.

**Guarantees**

| ID | Guarantee | Requirement |
|---|---|---|
| DF-1 | Element order, separators, and connector wording come from CLDR for `locale` | FR-015 |
| DF-2 | Rendered digits are always `0`-`9`, in every supported language | FR-015, SC-006 |
| DF-3 | The same instant formats differently in `en` vs `ar` (asserting a real locale switch, not a fallback to English) | SC-006 |
| DF-4 | No hardcoded pattern string exists anywhere in the codebase | FR-015 |
| DF-5 | Percentages use `NumberFormat.getPercentInstance(formattingLocale)` | FR-015 |

`Locale.getDefault()` MUST NOT appear in formatting code. Both current call sites
(`HistoryScreen.kt:126`, `StatisticsScreen.kt:224`) use it and both are replaced.

---

## 7. Layout direction contract

| ID | Rule | Requirement |
|---|---|---|
| LD-1 | Direction is resolved by the platform from the locale; never set manually | FR-005 |
| LD-2 | Use logical `Modifier.padding(start =/end =)`; **never** `absolutePadding` | FR-007 |
| LD-3 | Use `Alignment.Start/End`, `Arrangement.Start/End`; never `CenterStart`-as-left assumptions | FR-007 |
| LD-4 | Use `TextAlign.Start/End`; never `TextAlign.Left/Right` | FR-007 |
| LD-5 | Directional icons use `Icons.AutoMirrored.*`; non-directional icons must **not** | FR-008 |
| LD-6 | Glance widgets follow LD-2 … LD-5 with the `androidx.glance.*` equivalents | FR-017 |

### LD-5 icon classification (complete, current codebase)

| Icon | Auto-mirror? | Used in |
|---|---|---|
| `ArrowBack` | **Yes** — `Icons.AutoMirrored.Filled.ArrowBack` | 7 screens; **already correct** |
| `Add` | No | `MainScreen` |
| `Edit` | No | `MainScreen` |
| `Search` | No | `MainScreen` |
| `Star` / `Star` (outlined) | No | `MainScreen` |
| `Info` | No | `HistoryScreen` |

The app is already compliant with LD-1 … LD-6. This section is a **regression guard** for the
string-externalization pass, not a defect list.
