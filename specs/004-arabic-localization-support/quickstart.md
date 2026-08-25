# Quickstart: Validating Choice Localization & Multilingual Support

**Feature**: `004-arabic-localization-support` | **Branch**: `004-arabic-localization-support`

This is a **validation guide**, not an implementation guide. It describes how to prove the feature
works end to end once implemented. Implementation detail lives in
[plan.md](./plan.md), [data-model.md](./data-model.md), and [contracts/](./contracts/).

---

## Prerequisites

| Requirement | Value |
|---|---|
| JDK | 17 |
| Android SDK | API 35 (`compileSdk`/`targetSdk`), `minSdk 26` |
| Emulators / devices | **Two** are needed: one API 33+ (`LocaleManager` path) and one API 26–32 (backport path) |
| Launcher | Any home screen that supports widget placement (for US5) |

Both API tiers are mandatory. The per-app language mechanism takes a completely different code path
below API 33 ([localization-contract §4](./contracts/localization-contract.md)), so passing on one
tier says nothing about the other.

```bash
./gradlew assembleDebug
./gradlew installDebug
```

---

## 1. Automated gates

Run these first — they are fast and catch most regressions before any manual work.

```bash
# JVM unit tests: locale resolution, date/percentage formatting, search, template identity
./gradlew testDebugUnitTest

# Resource completeness: every values/ key has a values-ar/ counterpart (FR-004, SC-001)
./gradlew lintDebug

# Instrumented: per-screen localized rendering, RTL, data preservation, a11y
./gradlew connectedDebugAndroidTest
```

### Expected results

| Gate | Pass condition | Proves |
|---|---|---|
| `testDebugUnitTest` | all green, including the new `LocalePreference`, `DecisionTimeFormatter`, `CoinSearch`, and `CoinTemplates` cases | FR-027, FR-015, FR-012, FR-010 |
| `lintDebug` | **zero** `MissingTranslation` and zero `ExtraTranslation` | FR-004, SC-001 |
| `connectedDebugAndroidTest` | all green — including the **14 pre-existing screen tests**, updated to resolve strings via `context.getString(R.string.…)` | no English-wording regression (FR-003, SC-001) |

> The pre-existing `androidTest` suite asserts against hardcoded English literals today. If those
> tests still pass **unmodified** after the string-externalization pass, externalization is
> incomplete — some literal is still in Kotlin.

### Key unit assertions to expect

```
resolveLanguageTag(stored = "de", system = ["ar-EG"])  == "en"   // FR-027 — the easy one to get wrong
resolveLanguageTag(stored = null, system = ["fr","ar"]) == "ar"   // first supported entry wins
formattingLocale("ar").toLanguageTag()                  == "ar-u-nu-latn"
DecisionTimeFormatter.format(t, ar, zone)  contains only 0-9 digits   // SC-006
DecisionTimeFormatter.format(t, ar, zone) != format(t, en, zone)      // a real switch, not a fallback
matchesSearchQuery("قهوة أم شاي", "شاي")   == true    // FR-012
matchesSearchQuery("Coffee أم شاي", "أم")   == true    // mixed script
```

---

## 2. Manual validation by user story

Each block is independently runnable and maps to one prioritized story in
[spec.md](./spec.md). Run every block on **both** API tiers.

### US1 (P1) — Full Arabic UI with correct RTL

1. Launch Choice → **Settings** (top bar) → **Language** → **العربية**.
2. Walk: home → create coin → decision (flip) flow → per-coin settings → history → statistics.

| Check | Expected | Requirement |
|---|---|---|
| Text | every visible string is Arabic; no English leftovers, no `%1$s`, no resource-name placeholders | FR-004, SC-001 |
| Direction | lists, top bars, and content flow right-to-left | FR-005, SC-002 |
| Alignment | labels and helper text align to the RTL start edge | FR-006, FR-007 |
| Back arrow | points **right** | FR-008 |
| Add / Edit / Search / Star / Info icons | **not** mirrored | FR-008 |
| Design | Catppuccin Mocha colors and Material 3 components identical to English | FR-028 |

Repeat the walk at **~320dp width** (`adb shell wm size 320x640`, or a small-screen AVD): no
clipping, no overlap, no horizontal scroll (SC-002).

### US2 (P2) — Switch languages without losing data

1. Starting in English, create ≥3 coins (one Arabic-named, one English-named, one mixed), favorite
   one, and accept several decisions so history is non-empty.
2. Record the exact state: coin names, choice text, favorite flags, history entries and timestamps.
3. Switch English → Arabic → English → **System default**, via the in-app selector **and** via
   *Settings → System → Languages → App languages* (API 33+).

| Check | Expected | Requirement |
|---|---|---|
| Data | every coin, choice, decision record, and favorite is byte-for-byte identical after every switch | FR-025, SC-004 |
| UI | all chrome relabels; **user content does not** — an Arabic coin name stays Arabic in the English UI and vice versa | FR-009, SC-004 |
| English wording | matches the app's pre-feature wording exactly — no regressions from externalization | FR-003 |
| Stability | switching while a screen or dialog is open never crashes, freezes, or shows a placeholder | FR-030, SC-001 |
| System list (API 33+) | Choice appears under *App languages* with English and العربية | FR-024 |
| Unsupported language | set the device to French with "System default" selected → Choice renders **English**, not broken or partial | FR-027 |

### US3 (P3) — Localized templates, fixed created coins

1. In Arabic, open the template picker.
2. Create a coin from a template. Note its exact name and choices.
3. Switch to English. Reopen the created coin, then reopen the template picker.

| Check | Expected | Requirement |
|---|---|---|
| Picker in Arabic | template names and default choices are Arabic | FR-010, SC-005 |
| Created coin after switch | name and choices **unchanged** — still exactly as generated | FR-011, SC-005 |
| Picker after switch | now shows English template content | FR-010 |
| Reverse direction | repeat starting in English → switch to Arabic; same result | SC-005 |
| Choice count | a template's choice list has the same item count in both languages | VR-11 |

### US4 (P4) — Search, history, statistics in either language

| Check | Expected | Requirement |
|---|---|---|
| Arabic query, Arabic coin | matches | FR-012, SC-003 |
| Arabic query, English UI | matches — search is independent of UI language | FR-012 |
| English query, Arabic UI | matches | FR-012 |
| Mixed-script coin name, either substring | matches | FR-013 |
| History / statistics labels in Arabic | fully Arabic, including empty states | FR-014 |
| Timestamps in Arabic | Arabic-locale element order, separators, and connector wording | FR-015, SC-006 |
| **Digits everywhere** | Western Arabic **0-9**, never ٠-٩ — in timestamps, counts, and percentages | FR-015, SC-006 |
| Timestamps differ between languages | English and Arabic render the same instant differently | DF-3 |
| Mixed-script list rendering | Arabic and English coin names both read correctly in the same list | Edge Cases, VR-16 |

### US5 (P5) — Widgets, accessibility, share/import

**Widgets**

1. With the app in Arabic, add a **Single Coin** and a **Quick Coins** widget.

| Check | Expected | Requirement |
|---|---|---|
| Config activity | fully Arabic and RTL | FR-016, WL-4 |
| Widget body | title, flip action, empty/unavailable states in Arabic | FR-016, SC-007 |
| Widget layout | right-to-left | FR-017, SC-002 |
| Choice count | correct Arabic **plural** form, not an English-shaped singular/plural | SR-8 |

2. With widgets placed, flip each so it shows a result. Then switch the app language.

| Check | Expected | Requirement |
|---|---|---|
| Relabel timing | widget labels change **immediately** — no remove/re-add, no app relaunch, no tap | **FR-031**, SC-007, WR-1/WR-2 |
| Recorded result | the displayed decision result is **unchanged** | FR-018, WR-4, SC-007 |
| Association | the widget still points at the same coin | FR-025, WR-3 |

3. Change the **device system language** while the in-app override is set to العربية.

| Check | Expected | Requirement |
|---|---|---|
| Widgets | relabel to the **override** (Arabic), not the new system language | **WR-6**, FR-026 |

4. Force-stop Choice, change the device system language with "System default" selected, then look at
   the home screen **without launching the app**.

| Check | Expected | Requirement |
|---|---|---|
| Widgets | still relabel — the manifest-declared `ACTION_LOCALE_CHANGED` receiver fires with the process dead | **WR-2**, FR-031 |

**Accessibility** — enable TalkBack.

| Check | Expected | Requirement |
|---|---|---|
| Arabic UI | every announcement is Arabic, app **and** widgets | FR-022, SC-009 |
| English UI | every announcement is English | FR-023, SC-009 |
| Cross-language leftovers | **none** in either direction | FR-023, SC-009 |
| Every interactive control | has a meaningful description — no unlabeled buttons | FR-022 |
| Mixed-script descriptions | an Arabic coin name inside an English description reads in the correct visual order | VR-16 |

**Share / import**

| Check | Expected | Requirement |
|---|---|---|
| Export English → import Arabic | imported name and choices identical to the original | FR-021, SC-008 |
| Export Arabic → import English | identical | SC-008 |
| Payload text | byte-identical regardless of export-time language | FR-019, VR-23 |
| Flow chrome | prompts, confirmations, and errors localized | FR-020 |
| Pre-localization payload | a payload from an older build still imports and preserves content | Edge Cases, VR-26 |

---

## 3. Cross-cutting checks

### Missing-translation fallback (FR-030)

Temporarily delete one key from `res/values-ar/strings.xml`, rebuild, run in Arabic.

**Expected**: that one string renders in **English**; the app does not crash and shows no
resource-name placeholder. `lintDebug` reports the gap.

**Then restore the key** — FR-004 requires zero gaps in the shipped build. This test proves the
safety net exists, not that gaps are acceptable.

### Extensibility (FR-029, SC-010)

Confirm by inspection, without writing code, that adding a third language would require **only**:

1. one new tag in `SupportedLanguages.TAGS`,
2. one new `res/values-<tag>/strings.xml`,
3. one new `<locale>` line in `res/xml/locales_config.xml`.

**Fail condition**: any decision logic, navigation route, formatting code, or widget code would need
to change. If it would, FR-029 is not met.

### Constitution spot-checks

| Principle | Check |
|---|---|
| III — Local First | validate the whole guide in airplane mode; nothing here needs a network |
| V — Consistent Design | Catppuccin Mocha and Material 3 identical in both languages (FR-028) |
| VII — Minimal Dependencies | `git diff` on `app/build.gradle.kts` shows **no new dependency** |
| VI — Testable Behavior | locale resolution, formatting, and search are covered by JVM tests, not only by UI tests |

---

## 4. Definition of done

- [ ] `./gradlew testDebugUnitTest` green, including new locale/formatting/search/template cases
- [ ] `./gradlew lintDebug` reports zero `MissingTranslation` / `ExtraTranslation`
- [ ] `./gradlew connectedDebugAndroidTest` green on **both** an API 33+ and an API 26–32 device
- [ ] Pre-existing screen tests updated to resolve strings from resources — and passing
- [ ] US1 – US5 manual blocks pass on both API tiers
- [ ] Narrow-width (~320dp) pass in Arabic with no clipping or overflow
- [ ] Widgets relabel immediately on language change, with recorded results intact
- [ ] Missing-translation fallback verified, then the deleted key restored
- [ ] `app/build.gradle.kts` diff shows no new dependency
- [ ] No Room migration was added (this feature requires none — see [data-model.md](./data-model.md))
