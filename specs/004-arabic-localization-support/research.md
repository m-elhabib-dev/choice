# Phase 0 Research: Choice Localization and Multilingual Support

**Feature**: `004-arabic-localization-support` | **Date**: 2026-08-24

**Purpose**: Resolve every NEEDS CLARIFICATION from the plan's Technical Context and record the
technology decisions that Phase 1 design and the eventual implementation depend on.

All decisions below were validated against the current codebase state (`app/src/main/kotlin/...`,
`app/src/main/res/values/strings.xml`, `app/build.gradle.kts`, `AndroidManifest.xml`) and against
the Choice Constitution v1.0.0.

---

## Codebase baseline (established, not a decision)

Facts the decisions below build on:

- **Stack**: Kotlin, Jetpack Compose + Material 3, Navigation Compose, Room, Glance 1.1.1
  (`app/build.gradle.kts`). `minSdk = 26`, `compileSdk`/`targetSdk = 35`.
- **Activities**: `MainActivity`, `SingleCoinWidgetConfigActivity`, `QuickCoinsWidgetConfigActivity`
  — all `ComponentActivity`. No AppCompat dependency; host theme is
  `android:Theme.Material.Light.NoActionBar` (`app/src/main/res/values/themes.xml`).
- **String state**: `strings.xml` holds **only** 18 keys — `app_name` plus 17 widget strings. Roughly **79** user-facing
  literals remain hardcoded across the Compose screens and widget composables
  (`text = "…"`, `contentDescription = "…"`, `Text("…")`).
- **Manifest**: `android:supportsRtl="true"` is already set. No `android:localeConfig`, no
  `res/values-ar/`, no `res/xml/locales_config.xml`.
- **Templates**: `CoinTemplates.templates` is a hardcoded `List<CoinTemplate(name, choices)>`.
  Navigation passes the template's **name** as a route argument
  (`coinedit?templateName=$name`) and `MainActivity` looks the template back up by that name.
- **Formatting**: two identical private `formatTimestamp()` helpers
  (`HistoryScreen.kt:126`, `StatisticsScreen.kt:224`) using
  `SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())`.
- **Settings**: there is **no app-level settings screen**. `CoinSettingsScreen` is per-coin
  (weighting / avoid-last-result / share / import). `MainScreen` has no settings entry point.
- **Search**: `matchesSearchQuery()` is `coinName.contains(query, ignoreCase = true)`.
- **Widget refresh**: `WidgetRefreshCoordinator` already exists, started once from
  `ChoiceApplication.onCreate`, and calls `SingleCoinWidget().updateAll(context)` +
  `QuickCoinsWidget().updateAll(context)` on every `observeCoins()` emission.

---

## 1. Per-app language mechanism (FR-024, FR-026, FR-027)

**Decision**: Platform `android.app.LocaleManager` on API 33+, plus a small in-repo backport for
API 26–32. The user's choice is stored by Choice itself in `SharedPreferences` as a BCP-47 language
tag (`"en"`, `"ar"`, or absent = "follow system"). No new Gradle dependency.

Shape:

- `LocalePreference` (pure Kotlin, no Android types in the decision logic) — owns the supported-tag
  list and `resolveLocale(storedTag: String?, systemLocales: List<String>): Locale`, which returns
  the app locale, falling back to English for any unsupported tag (FR-027).
- `LocaleApplier` — the only place that touches platform APIs:
  - **API 33+**: `getSystemService(LocaleManager::class.java).applicationLocales =
    LocaleList.forLanguageTags(tag)`. The system then applies the locale to every context in the
    app process, including Glance widget rendering, and it survives process death.
  - **API 26–32**: `createConfigurationContext(Configuration().apply { setLocales(…) })`, applied in
    `attachBaseContext` of `MainActivity`, `SingleCoinWidgetConfigActivity`,
    `QuickCoinsWidgetConfigActivity`, and applied to the `Context` handed to
    `GlanceAppWidget.provideGlance` before any `getString` call.
  - The widget-context wrap is applied on **all** API levels. On 33+ it is a harmless no-op
    (the context already carries the app locale); keeping it unconditional removes an API-level
    branch from widget code and makes widget behaviour identical across versions.

**Rationale**:
- Adds zero dependencies, satisfying Principle VII (Minimal Dependencies), and keeps
  `ComponentActivity` + the existing `Theme.Choice` host theme untouched — no design-system churn
  (Principle V).
- `LocaleManager` is the current, officially supported platform API for per-app languages, so the
  primary path satisfies Principle IV (Modern Android); the backport is a thin, documented shim for
  the versions that predate it.
- The tag→`Locale` resolution — the part that carries the FR-027 fallback rule and the
  supported-language list — is a pure function with no Android dependency, so it is unit-testable
  in `app/src/test` (Principle VI).

**Alternatives considered**:
- **`androidx.appcompat` + `AppCompatDelegate.setApplicationLocales()`** — the official Jetpack
  backport. Rejected: it requires adding the AppCompat dependency, converting all three activities
  to `AppCompatActivity`, and re-parenting `Theme.Choice` to a `Theme.AppCompat.*` parent, and it
  *still* needs an explicit context wrap for Glance widgets below API 33. Higher blast radius and a
  new dependency for no additional coverage. (Confirmed with the user, 2026-08-24.)
- **System language only, no in-app selector** — rejected: FR-026 and the 2026-08-24 clarification
  explicitly require an in-app override.

### 1a. Why SharedPreferences and not DataStore

**Decision**: `SharedPreferences` for the language tag.

**Rationale**: the tag must be read **synchronously** before the first Activity attaches its base
context and before the first widget render, both of which happen with no coroutine scope available.
DataStore is async-only and would require a `runBlocking` at process start. `SharedPreferences` is a
platform API (zero dependency) with a synchronous read.

**Alternatives considered**: DataStore Preferences (already present transitively via Glance, and
used by `WidgetConfigStore`) — rejected for the synchronous-read requirement above. Room — rejected:
this is an app setting, deliberately not user content, and must not live in the user-data database
(FR-025, keeps the "language never touches user data" boundary structurally obvious).

---

## 2. Western Arabic digits under an Arabic locale (FR-015, SC-006)

**Decision**: Build the formatting `Locale` with the Unicode `nu-latn` numbering-system extension:

```kotlin
Locale.Builder().setLanguage("ar").setUnicodeLocaleKeyword("nu", "latn").build()
```

All date/time and percentage formatting uses this derived *formatting locale*, while the *UI
locale* stays plain `ar` for resource resolution.

**Rationale**: CLDR's default numbering system for `ar` is `arab` (٠-٩). The `-u-nu-latn` extension
is the standard, ICU-supported way to keep Arabic-locale ordering, separators, and wording while
forcing Western Arabic digits (0-9) — exactly the 2026-08-24 clarification. It is honoured by both
`java.time.format.DateTimeFormatter` and `java.text.NumberFormat` on API 26+.

**Alternatives considered**:
- Post-processing formatted strings to transliterate Arabic-Indic digits back to ASCII — rejected:
  fragile, and it would also rewrite digits inside user-entered content.
- `DecimalFormatSymbols` with an explicit zero digit — rejected: it only covers `NumberFormat`, not
  the digits embedded by date/time formatters.

---

## 3. Locale-aware date/time and percentage formatting (FR-015, SC-006)

**Decision**: Replace both `SimpleDateFormat("MMM d, yyyy 'at' h:mm a")` helpers with a single
shared `DecisionTimeFormatter` built on `java.time`:

```kotlin
DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    .withLocale(formattingLocale)   // §2
    .withZone(ZoneId.systemDefault())
```

Percentages use `NumberFormat.getPercentInstance(formattingLocale)`.

**Rationale**: `java.time` is available unconditionally at `minSdk = 26` (no desugaring needed).
`ofLocalizedDateTime` delegates the entire pattern — element order, separators, and the
"at"/"في"-style connector wording — to CLDR data for the active locale, which is what FR-015
requires; a hand-written pattern string cannot. One shared formatter also removes the existing
duplication between `HistoryScreen` and `StatisticsScreen`.

**Testability**: the formatter takes an explicit `Locale` and `ZoneId` parameter rather than
reading `Locale.getDefault()`, so `app/src/test` can assert English and Arabic output
deterministically without an emulator (Principle VI).

**Alternatives considered**:
- `android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)` — works, but returns a
  pattern string that then needs a second formatter, and it is an Android API, forcing these tests
  onto an emulator.
- Keeping `SimpleDateFormat` with a localized pattern per language via `strings.xml` — rejected:
  it puts format patterns in translator-editable resources and re-introduces per-language logic,
  violating FR-029 (adding a language must require content only).

---

## 4. RTL layout in the app (FR-005, FR-006, FR-007, FR-008)

**Decision**: Rely entirely on the platform/Compose layout-direction mechanism. Concretely:

- `android:supportsRtl="true"` is already present — no manifest change needed for the app.
- Compose reads `LocalLayoutDirection` from the configuration automatically; no `CompositionLocal`
  override and no manual mirroring anywhere.
- Audit and fix the directional modifiers found in the codebase: `Modifier.padding(start = …)` /
  `padding(end = …)` are already logical (RTL-aware) in Compose and stay as they are;
  `Modifier.absolutePadding`, `Alignment.CenterStart/CenterEnd`, and `TextAlign.Left/Right` are the
  things to avoid. The current code uses none of the forbidden forms — the audit is a guard against
  regressions during the string-externalization pass, not a known defect list.
- Directional icons: the codebase already uses `Icons.AutoMirrored.Filled.ArrowBack` in all seven
  screens that have a back button (`CoinFlipScreen`, `StatisticsScreen`, `ImportCoinScreen`,
  `TemplatePickerScreen`, `CoinSettingsScreen`, `CoinEditScreen`, `HistoryScreen`).
  `Icons.AutoMirrored.*` mirrors itself under RTL, which is exactly FR-008. Non-directional icons
  (`Add`, `Edit`, `Search`, `Star`, `Info`) must stay on the non-auto-mirrored variants.

**Rationale**: FR-005 explicitly forbids per-component manual mirroring, and the existing code is
already ~90% compliant. The work is verification plus the `AutoMirrored` rule being applied to any
new directional icon.

**Alternatives considered**: forcing `LocalLayoutDirection` from the stored language preference —
rejected: it duplicates state the configuration already carries and would desynchronize from the
system on API 33+.

---

## 5. RTL layout in Glance widgets (FR-017, SC-007)

**Decision**: Glance emits `RemoteViews`, and RTL resolution happens in the launcher's host process
against the widget's layout direction. Two things are required:

1. The `<receiver>` host app already declares `android:supportsRtl="true"` at the `<application>`
   level, which is what governs RTL resolution for its `RemoteViews`.
2. Widget composables must use Glance's direction-aware primitives:
   `androidx.glance.layout.Row` with `horizontalAlignment = Alignment.Start/End` and
   `GlanceModifier.padding(start =/end =)` — never `Alignment.Left/Right` or absolute padding.
   `androidx.glance.text.TextAlign.Start/End` replaces `TextAlign.Left/Right`.

**Known constraint**: `QuickCoinsWidget.kt:256` currently uses `TextAlign.Start`, and
`QuickCoinsWidgetConfigActivity.kt:220` uses logical `padding(start = 8.dp)` — both already correct.
`SingleCoinWidget.kt:145,199` use `TextAlign.Center`, which is direction-neutral. So the widget
layer needs no structural rework for RTL; the work is string externalization plus the localized
context wrap from §1.

**Rationale**: `TextAlign.Start` in Glance resolves against the rendering configuration's layout
direction, so once the widget's context carries the Arabic locale (§1), the RemoteViews are
generated with `layoutDirection = RTL` and the launcher lays them out right-to-left.

**Alternatives considered**: forcing `View.LAYOUT_DIRECTION_RTL` on the generated RemoteViews via
`setInt(…, "setLayoutDirection", …)` — rejected: manual mirroring, explicitly forbidden by FR-005,
and unnecessary once the context is localized.

---

## 6. Immediate widget refresh on language change (FR-031, SC-007)

**Decision**: Extend the existing `WidgetRefreshCoordinator` with a second trigger, so it reacts to
**both** coin-data changes (existing behaviour) and language changes (new):

- **In-app selector change**: after `LocaleApplier` persists and applies the new tag, invoke the
  coordinator's refresh path directly (`SingleCoinWidget().updateAll(ctx)` +
  `QuickCoinsWidget().updateAll(ctx)`).
- **Device system-language change**: register a receiver for
  `Intent.ACTION_LOCALE_CHANGED` that calls the same refresh path.

**Rationale**: `updateAll` re-invokes `provideGlance`, which re-reads every string through the
(now Arabic) localized context, so labels change without the user removing and re-adding the
widget. Reusing `WidgetRefreshCoordinator` avoids a second, parallel refresh mechanism
(Principle I) and keeps the "one place that refreshes widgets" invariant the existing code
documents. Declaring `ACTION_LOCALE_CHANGED` in the manifest (rather than a runtime receiver) is
required because the app process may not be alive when the system language changes.

**Alternatives considered**:
- Letting widgets pick up the new language at their next natural refresh — rejected by the
  2026-08-24 clarification, which explicitly requires an immediate refresh.
- A periodic refresh timer — rejected: the existing widget spec's constraint is that reactive Flow
  collection is the only non-user-initiated trigger; a locale-change broadcast is event-driven and
  preserves that.

---

## 7. Localized coin templates with stable identity (FR-010, FR-011, SC-005)

**Decision**: Give each template a **stable, non-localized string ID** and move its display name and
default choices into string resources:

```kotlin
data class CoinTemplate(
    val id: String,                    // "breakfast" — stable, never translated, never shown
    @StringRes val nameRes: Int,
    @ArrayRes val choicesRes: Int,
)
```

`strings.xml` gains `template_breakfast_name` plus a `<string-array name="template_breakfast_choices">`,
mirrored in `values-ar/`. `TemplatePickerScreen` resolves `stringResource(nameRes)` and
`stringArrayResource(choicesRes)` at render time.

**Navigation change (required)**: `MainActivity` currently routes `coinedit?templateName=$name` and
looks the template up with `CoinTemplates.templates.find { it.name == name }`. Once `name` is
localized, that lookup breaks the moment the language changes mid-flow. The route argument becomes
`templateId` and the lookup becomes `find { it.id == id }`. `CoinEditViewModel` then receives the
*already-resolved localized* name and choice strings, exactly as it does today.

**Materialization boundary (FR-011)**: `CoinEditViewModel` already receives plain `String` name and
`List<String>` choices and persists them verbatim. Because the strings are resolved to plain text
**before** the ViewModel and stored as ordinary user content, a coin created from a template is
structurally incapable of re-translating later. No new guard code is needed — the existing data
flow enforces FR-011 by construction.

**Rationale**: A stable ID is the only thing that survives a language switch, and keeping it out of
the UI means it never needs translating. Using `string-array` keeps a template's choices editable by
a translator as a single unit and keeps template content out of Kotlin (FR-029).

**Alternatives considered**:
- Keeping English names as IDs — rejected: the ID would then be a user-visible English string, and a
  translator edit would silently break the lookup.
- A `Map<Locale, CoinTemplate>` in Kotlin — rejected: it puts translated content in code, violating
  FR-001 and FR-029.

---

## 8. Arabic and mixed-script search (FR-012, FR-013, SC-003)

**Decision**: Keep `matchesSearchQuery` as `coinName.contains(query, ignoreCase = true)`, and add
unit tests proving it works for Arabic, English, and mixed-script names/queries. Explicitly do
**not** add locale-sensitive case mapping.

**Rationale**: Kotlin's `contains(ignoreCase = true)` uses locale-independent case folding over the
full Unicode range, not an ASCII-only comparison, so FR-013 is already satisfied. Arabic script is
caseless, so case folding is a no-op for Arabic text; substring matching therefore behaves
identically to the current English behaviour, which is the SC-003 bar. Adding
`toLowerCase(Locale.getDefault())` would actively *introduce* a bug — under a Turkish locale it
would break dotted-I matching for English coin names.

**Explicitly out of scope** (per the spec's Assumptions): Arabic diacritic-insensitive matching,
alef/hamza normalization (أ/إ/آ → ا), and taa-marbuta/haa folding (ة/ه). These are not part of
existing search behaviour and would change match semantics for English users too.

**Alternatives considered**: ICU `Collator` with `PRIMARY` strength for diacritic-insensitive
matching — rejected as out of scope; noted here as the natural extension point if it is ever
requested.

---

## 9. Bidirectional text in mixed-script content (Edge Cases)

**Decision**: Wrap user-entered content that is interpolated into a localized sentence or placed
adjacent to UI chrome with `androidx.core.text.BidiFormatter.getInstance().unicodeWrap(text)`.
`androidx.core:core-ktx` is already a direct dependency, so this adds nothing.

Applies to: coin names and choice text in accessibility `contentDescription`s built by string
interpolation (e.g. `SingleCoinWidget.kt:227`, `QuickCoinsWidget.kt:247,269,277,302`), and to the
`"$coinName result: …"`-style composites once they move to `%1$s`-style resource placeholders.

**Rationale**: An Arabic coin name inside an English content description (or vice versa) renders
with the wrong visual order without an isolate. `unicodeWrap` inserts the Unicode directional
isolate characters the Bidi algorithm needs. Standalone `Text(coin.name)` composables need no
wrapping — Compose already resolves the paragraph direction from the text's own first strong
character.

**Alternatives considered**: manual `⁨`/`⁩` (FSI/PDI) insertion — rejected: `BidiFormatter`
already does exactly this and handles the estimation heuristics.

---

## 10. Placement of the in-app language selector (FR-026)

**Decision**: Add a new app-level **Settings** screen (`ui/settings/`), reachable from a settings
icon in `MainScreen`'s `TopAppBar`, containing the language selector (System default / English /
العربية). This is a genuinely new screen — `CoinSettingsScreen` is per-coin and is the wrong home
for an app-wide preference.

**Rationale**: FR-026 says "within Choice's settings", and no app-level settings surface exists.
Per Principle I (Simplicity First) and Principle VIII (Incremental Development), the screen ships
with *only* the language selector — no other settings are added speculatively. Putting an app-wide
preference inside a per-coin settings screen would be an obvious category error for users.

**Alternatives considered**:
- A language item inside `CoinSettingsScreen` — rejected: wrong scope, per-coin screen.
- A dialog straight off the `MainScreen` overflow — rejected: a settings *screen* is the
  conventional Android home for this and gives future settings somewhere to land, at the same
  implementation cost.

---

## 11. System-settings integration: `locales_config.xml` (FR-024)

**Decision**: Add `app/src/main/res/xml/locales_config.xml` listing `en` and `ar`, and reference it
from the manifest via `android:localeConfig="@xml/locales_config"`.

**Rationale**: On API 33+, this is what makes Choice appear under *Settings → System → Languages →
App languages*, giving users the system-level route to change the app's language that FR-024
describes. Without it the app is absent from that list.

**Alternatives considered**: AGP's `androidResources { generateLocaleConfig = true }` — rejected:
it requires an App Bundle build and generates the file at build time, which makes the supported-
language list invisible in the source tree; an explicit two-line XML file is clearer for a
two-language app.

---

## 12. Translation fallback behaviour (Edge Cases, FR-030, SC-001)

**Decision**: Rely on Android's built-in resource fallback: `res/values/strings.xml` is the default
(English) set, `res/values-ar/strings.xml` is the Arabic overlay. Any key missing from `values-ar`
resolves to the English default rather than throwing.

**Verification**: enable `android { lint { … } }` checks for `MissingTranslation` and
`ExtraTranslation` so a gap is caught at build time rather than shipping as a silently-English
string in an Arabic UI (FR-004 requires *no* visible gaps, so the fallback is a crash-safety net,
not the intended steady state).

**Rationale**: This is the platform's own mechanism — nothing to build. `MissingTranslation` is an
existing Android Lint check and needs only to be surfaced, not written.

**Alternatives considered**: a runtime "missing string" assertion in debug builds — rejected as
redundant with the Lint check, which catches the same problem earlier and without shipping code.

---

## 13. Testing strategy for localization

**Decision**: Three tiers, deliberately weighted toward the JVM tier.

| Tier | Location | Covers |
|------|----------|--------|
| **Unit (JVM)** | `app/src/test/kotlin/` | `LocalePreference.resolveLocale` (incl. FR-027 fallback), `DecisionTimeFormatter` output for `en` and `ar-u-nu-latn`, percentage formatting, `matchesSearchQuery` with Arabic/English/mixed input, `CoinTemplates` ID stability |
| **Instrumented (device)** | `app/src/androidTest/kotlin/` | Per-screen localized rendering and RTL, via a locale-setting test rule; language switch preserves data; template picker localization; `contentDescription` language |
| **Resource-level (build)** | Android Lint | `MissingTranslation` / `ExtraTranslation` — the FR-004 completeness gate |

**Rationale**: Principle VI requires core logic to be testable independent of UI. Every rule with
real branching (fallback, digit system, date shape, search matching) is extracted into a pure
function precisely so it lands in the fast JVM tier; the instrumented tier is reserved for what
genuinely needs a rendered frame (RTL geometry, screen-reader descriptions).

**Existing tests that must keep passing**: the 14 `androidTest` screen tests currently assert against
hardcoded English literals (e.g. `MainScreenSearchTest`, `HistoryScreenTest`,
`TemplatePickerScreenTest`). Externalizing strings will break them unless they are updated to
resolve strings via `context.getString(R.string.…)` — this is a required, non-optional part of the
externalization work, not a follow-up.

**Alternatives considered**: screenshot-diff testing for RTL — rejected: adds a dependency and a
baseline-image maintenance burden for a two-language app (Principle VII).

---

## Resolved unknowns summary

| Technical Context item | Resolution |
|---|---|
| Per-app locale API for `minSdk 26` | §1 — `LocaleManager` (33+) + in-repo `createConfigurationContext` backport; no new dependency |
| Language preference storage | §1a — `SharedPreferences` (synchronous read required at process start) |
| Western digits under `ar` | §2 — `Locale.Builder().setUnicodeLocaleKeyword("nu", "latn")` |
| Locale-aware date/time API | §3 — `java.time.DateTimeFormatter.ofLocalizedDateTime`, available at API 26 |
| App RTL mechanism | §4 — platform layout direction; `Icons.AutoMirrored.*` already in use |
| Widget RTL mechanism | §5 — localized Glance context + `Alignment.Start/End`, `TextAlign.Start/End` |
| Immediate widget refresh on language change | §6 — extend `WidgetRefreshCoordinator`; manifest `ACTION_LOCALE_CHANGED` receiver |
| Template localization + identity | §7 — stable `id` + `@StringRes`/`@ArrayRes`; nav arg `templateName` → `templateId` |
| Arabic / mixed-script search | §8 — existing `contains(ignoreCase = true)` is already correct; add tests, no normalization |
| Bidi in interpolated content | §9 — `BidiFormatter.unicodeWrap` (via existing `core-ktx`) |
| Language selector location | §10 — new app-level Settings screen, language-only |
| System settings integration | §11 — explicit `res/xml/locales_config.xml` + `android:localeConfig` |
| Missing-translation behaviour | §12 — platform fallback to `values/`, gated by Lint `MissingTranslation` |
| Test strategy | §13 — JVM-first, instrumented for RTL/a11y, Lint for coverage |
