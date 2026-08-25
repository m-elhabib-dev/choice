# Phase 1 Data Model: Choice Localization and Multilingual Support

**Feature**: `004-arabic-localization-support` | **Date**: 2026-08-24

**Input**: [spec.md](./spec.md) Key Entities · [research.md](./research.md)

---

## Guiding invariant

> **Language is presentation. It never touches persisted user content.**

This feature adds exactly **one** persisted value (a language tag in `SharedPreferences`) and
**zero** changes to the Room schema. Everything else is either a resource lookup or a derived,
render-time value. This is what makes FR-009, FR-025, and SC-004 structurally true rather than
enforced by careful coding.

| Store | Owns | Changed by this feature? |
|---|---|---|
| Room (`ChoiceDatabase`) | Coins, choices, decisions, favorites, per-coin settings | **No.** No entity, column, or migration change. |
| Glance `PreferencesGlanceStateDefinition` | Per-widget-instance config (`WidgetConfigStore`) | **No.** Widget→coin associations are unaffected by language. |
| `SharedPreferences` (`choice_app_prefs`) | **Language Preference (new)** | **Yes — the only new persisted state.** |
| `res/values*/strings.xml` | Localized String Resource Set, Coin Template content | **Yes — new and expanded.** |

---

## 1. Language Preference *(new, persisted)*

The user's active display-language setting for Choice. Independent of all user content.

**Storage**: `SharedPreferences("choice_app_prefs", MODE_PRIVATE)`, key `"language_tag"`.
Read synchronously at process start and in `Activity.attachBaseContext` (research.md §1a).

| Field | Type | Values | Notes |
|---|---|---|---|
| `languageTag` | `String?` | `"en"`, `"ar"`, or **absent** | Absent = "follow the device's system language". BCP-47 tag. |

**Derived (not persisted)**

| Value | Derivation | Used for |
|---|---|---|
| `uiLocale` | `resolveLocale(languageTag, systemLocales)` | Resource resolution, layout direction |
| `formattingLocale` | `uiLocale` + Unicode extension `nu-latn` | Dates, times, percentages (FR-015) |
| `layoutDirection` | RTL iff `uiLocale.language == "ar"` | Resolved by the platform, never set manually (FR-005) |

**Validation rules**

- **VR-1** (FR-027): a stored tag that is not in `SUPPORTED_TAGS` resolves to English. Applies to a
  stale tag left by a downgrade as well as to an unsupported system language.
- **VR-2** (FR-027): when `languageTag` is absent, the first system locale whose language matches a
  supported tag wins; if none match, English.
- **VR-3** (FR-015 / SC-006): `formattingLocale` **always** carries `nu-latn`, in every supported
  language, so digits are Western Arabic (0-9) regardless of UI language.
- **VR-4** (FR-025 / SC-004): writing `languageTag` MUST NOT read, write, or invalidate any Room
  table or any Glance widget state. Enforced structurally — the preference lives in a separate
  store with no reference to `CoinRepository`.

**State transitions**

```
        ┌────────────── (absent) ──────────────┐
        │         follow system locale         │
        └──┬────────────────────────────────┬──┘
           │ user picks "English"           │ user picks "العربية"
           ▼                                ▼
      languageTag = "en"  ◄────────────►  languageTag = "ar"
           │                                │
           └──── user picks "System" ───────┘
                        │
                        ▼
                    (absent)
```

Every transition triggers, in order:
1. persist the new tag,
2. apply it (`LocaleManager` on API 33+, recreate the activity on API 26–32),
3. refresh all placed widgets (FR-031).

A device system-language change while `languageTag` is present is a **no-op for Choice's UI** — the
explicit in-app override wins (FR-026). While `languageTag` is absent, it re-resolves via VR-2.

---

## 2. Localized String Resource Set *(new)*

The collection of translated user-facing text, keyed by a stable name, one file per language.

| Language | File | Role |
|---|---|---|
| English (default) | `app/src/main/res/values/strings.xml` | Complete default set; the fallback target (FR-003, §12) |
| Arabic | `app/src/main/res/values-ar/strings.xml` | Complete overlay; zero gaps required (FR-004) |

**Scope**: every user-facing literal in the app **and** the widgets — the 18 keys already in
`strings.xml` plus roughly **79** currently-hardcoded literals across the Compose screens and widget
composables (research.md, codebase baseline).

**Key naming convention** — `<surface>_<element>[_<qualifier>]`, so a translator can see which
screen a string belongs to without reading code:

| Prefix | Surface | Examples |
|---|---|---|
| `main_` | `MainScreen` | `main_title`, `main_search_placeholder`, `main_empty_title`, `main_section_quick_coins` |
| `coin_edit_` | `CoinEditScreen` | `coin_edit_title_create`, `coin_edit_error_min_choices` |
| `coin_flip_` | `CoinFlipScreen` | `coin_flip_result_label`, `coin_flip_action_flip` |
| `coin_settings_` | `CoinSettingsScreen` | `coin_settings_weighting_title`, `coin_settings_save` |
| `history_` | `HistoryScreen` | `history_title`, `history_empty_title` |
| `statistics_` | `StatisticsScreen` | `statistics_total_decisions`, `statistics_most_frequent` |
| `templates_` | `TemplatePickerScreen` | `templates_title`, `templates_start_blank` |
| `share_` | `ImportCoinScreen` + share flow | `share_import_title`, `share_error_invalid_payload` |
| `settings_` | **new** app Settings screen | `settings_title`, `settings_language_label`, `settings_language_system` |
| `template_` | Coin Template content (§3) | `template_breakfast_name` |
| `widget_` | Widgets — **already exists, do not rename** | `widget_action_flip`, `widget_state_unavailable` |
| `cd_` | Accessibility content descriptions | `cd_back`, `cd_edit_coin`, `cd_add_to_quick_coins` |

**Validation rules**

- **VR-5** (FR-001, FR-002): no user-facing literal may remain in Kotlin. Both app and widget text
  use the same mechanism and the same file.
- **VR-6** (FR-004 / SC-001): every key in `values/` has a counterpart in `values-ar/`. Gated by
  Android Lint `MissingTranslation` / `ExtraTranslation`.
- **VR-7** (FR-030): a missing key falls back to `values/` (English) — never a crash, never a
  placeholder. Platform behaviour; VR-6 makes it a safety net rather than the normal path.
- **VR-8** (FR-029): composed sentences use positional placeholders (`%1$s`, `%2$d`) so a translator
  can reorder them. **No string concatenation of translated fragments in Kotlin** — the current
  `"$coinName result: ${state.resultText}"` pattern (`SingleCoinWidget.kt:202`,
  `QuickCoinsWidget.kt:277`) becomes a `%1$s`/`%2$s` resource.
- **VR-9** (FR-015 / SC-006): no key holds a date, time, or number **format pattern**. Formats come
  from CLDR via the formatting locale, not from resources.
- **VR-10** (FR-022, FR-023): every interactive control and meaningful visual has a `cd_*` (or
  surface-prefixed) description in the resource set; none is left as a Kotlin literal.

---

## 3. Coin Template *(modified)*

An application-provided starting point for creating a coin. Application-owned content —
translatable, unlike a Coin.

**Before**

```kotlin
data class CoinTemplate(val name: String, val choices: List<String>)
// name = "Breakfast", choices = ["Ful", "Eggs", "Falafel", "Cheese"]
```

**After**

```kotlin
data class CoinTemplate(
    val id: String,                 // stable, never localized, never displayed
    @StringRes val nameRes: Int,
    @ArrayRes val choicesRes: Int,
)
```

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | `"breakfast"`, `"lunch"`, `"workout"`, `"movie"`. The navigation key and lookup key. Stable across languages and releases. |
| `nameRes` | `@StringRes Int` | → `template_breakfast_name` etc. |
| `choicesRes` | `@ArrayRes Int` | → `<string-array name="template_breakfast_choices">` |

**Resource shape** (per template, in both `values/` and `values-ar/`):

```xml
<string name="template_breakfast_name">Breakfast</string>
<string-array name="template_breakfast_choices">
    <item>Ful</item> <item>Eggs</item> <item>Falafel</item> <item>Cheese</item>
</string-array>
```

**Validation rules**

- **VR-11** (FR-010): every template resolves a localized name and choice list in every supported
  language. A template's `string-array` MUST have the same item count in every language — order and
  count are part of the contract, only the text is translated.
- **VR-12**: `id` is never rendered and never translated. It is the only cross-language identity.
- **VR-13** (FR-011 / SC-005): resolution to plain `String`s happens in the **UI layer**, before
  `CoinEditViewModel`. See §4.

**Contract change — navigation**

| | Before | After |
|---|---|---|
| Route | `coinedit?templateName={templateName}` | `coinedit?templateId={templateId}` |
| Lookup | `templates.find { it.name == name }` | `templates.find { it.id == id }` |
| Passed to VM | `template.name`, `template.choices` | `stringResource(nameRes)`, `stringArrayResource(choicesRes).toList()` |

Required, not cosmetic: with a localized `name`, a lookup by name breaks as soon as the language
changes between the picker and the edit screen. Full detail in
[contracts/navigation-contract.md](./contracts/navigation-contract.md).

---

## 4. Coin (user-owned) *(unchanged)*

A user-created decision object: a name and a set of choices, entered exactly as typed.

**Persistence**: `CoinEntity` / `ChoiceEntity` — **unchanged**. No new column, no migration.

**Validation rules**

- **VR-14** (FR-009 / SC-004): coin names and choice text are stored and rendered verbatim. No
  translation, transliteration, normalization, or case-folding is ever applied to stored content.
- **VR-15** (FR-011): once a template materializes into a coin, the coin's name and choices are
  ordinary user content. Because `CoinEditViewModel` receives plain `String`s and persists them
  verbatim, a created coin **cannot** re-translate — this is enforced by the data flow, not by a
  guard.
- **VR-16** (Edge Cases): mixed Arabic/English content renders with correct visual order.
  Standalone `Text(coin.name)` needs nothing (Compose resolves paragraph direction from the first
  strong character); content interpolated into a localized sentence or a `contentDescription` is
  wrapped with `BidiFormatter.unicodeWrap` (research.md §9).

**The materialization boundary** — the single most important line in this data model:

```
  Coin Template (app content, translatable)
        │
        │  stringResource(nameRes) / stringArrayResource(choicesRes)   ← resolved ONCE, in the UI
        ▼
  plain String / List<String>
        │
        ▼
  CoinEditViewModel → CoinRepository → Room
        │
        ▼
  Coin (user content, NEVER translated)
```

---

## 5. Decision / History Record *(unchanged persisted, new formatting)*

A record of a past decision outcome.

**Persistence**: `DecisionEntity` — **unchanged**. `choiceTextSnapshot` and `decidedAt` are stored
exactly as today.

| Field | Persisted as | Rendered as |
|---|---|---|
| `choiceTextSnapshot` | `String`, verbatim | verbatim (user content — never translated) |
| `decidedAt` | `Long` epoch millis | `DateTimeFormatter.ofLocalizedDateTime(MEDIUM, SHORT).withLocale(formattingLocale)` |

**Validation rules**

- **VR-17** (FR-018, FR-025): a language change never rewrites a stored decision. Only the
  on-screen rendering of `decidedAt` changes.
- **VR-18** (FR-015 / SC-006): the timestamp's element order, separators, and connector wording come
  from CLDR for the active locale; the digits are always 0-9 (VR-3).
- **VR-19**: the two duplicated `formatTimestamp()` helpers (`HistoryScreen.kt:126`,
  `StatisticsScreen.kt:224`) collapse into one shared, locale-parameterized formatter that is
  unit-testable without an emulator.

---

## 6. Widget Association *(unchanged)*

The link between a home-screen widget instance and the coin/configuration it displays.

**Persistence**: Glance `PreferencesGlanceStateDefinition` via `WidgetConfigStore` — **unchanged**.
Keys (`single_coin_widget_coin_id`, `quick_coins_widget_use_favorites`,
`quick_coins_widget_explicit_coin_ids`) hold IDs and booleans only — no display text, so nothing in
this store is language-dependent.

**Validation rules**

- **VR-20** (FR-025 / SC-004): a language change never reads or writes widget state. Refresh
  (FR-031) re-*renders* widgets; it does not re-*configure* them.
- **VR-21** (FR-018 / SC-007): a decision result already shown on a widget is user data and survives
  a language change unchanged; only the surrounding labels relabel.
- **VR-22** (FR-016): every widget-facing string — titles, actions, empty/unavailable states, config
  screens, accessibility descriptions, error/recovery messages — comes from the resource set,
  resolved through a **localized context** (research.md §1).

---

## 7. Shared Coin Package *(unchanged)*

The exported/serialized representation used for sharing and importing (`SharePayload`).

**Validation rules**

- **VR-23** (FR-019 / SC-008): the serialized form is byte-identical regardless of the UI language
  at export or import time. `SharePayload` contains no locale field and no localized text.
- **VR-24** (FR-021): imported coin names and choices are written verbatim, never translated to the
  importing device's language.
- **VR-25** (FR-020): only the *chrome* around share/import (prompts, confirmations, success and
  error messages) is localized — the payload itself is not.
- **VR-26** (Edge Cases): a payload produced by a pre-localization build still imports successfully.
  Guaranteed because `SharePayload`'s schema is untouched by this feature.

---

## Entity relationship overview

```
┌──────────────────────┐        selects            ┌───────────────────────────┐
│  Language Preference │───────────────────────────►│ Localized String Resource │
│  (SharedPreferences) │                            │ Set (values/, values-ar/) │
│  languageTag: String?│                            └─────────────┬─────────────┘
└──────────┬───────────┘                                          │ resolves
           │ derives                                              │
           ▼                                                      ▼
   uiLocale ─────────────► layout direction (platform)   ┌──────────────────┐
   formattingLocale ─────► Decision.decidedAt rendering  │  Coin Template   │
                                                         │  id + @StringRes │
                                                         └────────┬─────────┘
                                                                  │ materializes
                                    ══════════ presentation ══════╪═══════════
                                    ══════════ user content ══════╪═══════════
                                                                  ▼
        ┌───────────────┐   1..*   ┌──────────┐   1..*   ┌──────────────────┐
        │ Widget Assoc. │─────────►│   Coin   │─────────►│ Decision Record  │
        │ (Glance prefs)│  displays│  (Room)  │  produces│      (Room)      │
        └───────────────┘          └────┬─────┘          └──────────────────┘
                                        │ exports/imports
                                        ▼
                              ┌──────────────────────┐
                              │ Shared Coin Package  │  language-independent
                              └──────────────────────┘
```

Nothing below the double line is touched by a language change. That is the whole feature's
correctness argument.

---

## Summary of changes

| Entity | Change | Schema migration |
|---|---|---|
| Language Preference | **New** — `SharedPreferences`, one key | n/a (new store) |
| Localized String Resource Set | **New** — `values-ar/`; `values/` expanded from 18 to ~95 keys | n/a (resources) |
| Coin Template | **Modified** — `name`/`choices` → `id` + `@StringRes`/`@ArrayRes`; nav arg renamed | None (not persisted) |
| Coin | Unchanged | **None** |
| Decision / History Record | Unchanged persisted; new locale-aware rendering | **None** |
| Widget Association | Unchanged | **None** |
| Shared Coin Package | Unchanged | **None** |

**No Room migration is required by this feature.** Any implementation step that proposes one has
misread this model.
