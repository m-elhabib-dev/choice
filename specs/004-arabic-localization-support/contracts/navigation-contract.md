# Contract: Navigation & Template Identity

**Feature**: `004-arabic-localization-support` | **Type**: internal navigation contract

**Consumers**: `MainActivity` (`ChoiceNavHost`), `TemplatePickerScreen`, `CoinEditViewModel`,
`MainScreen`, the new `SettingsScreen`.

This contract covers the **two** navigation changes this feature requires. Every other route in
`ChoiceNavHost` is unchanged.

---

## 1. Breaking change: template route argument

### The defect being fixed

`ChoiceNavHost` currently routes by the template's **display name**:

```kotlin
// MainActivity.kt — TemplatePickerScreen
onTemplateSelected = { name, _ -> navController.navigate("coinedit?templateName=$name") }

// MainActivity.kt — coinedit destination
val templateName = backStackEntry.arguments?.getString("templateName")
val template = templateName?.let { name ->
    CoinTemplates.templates.find { it.name == name }
}
```

Once `name` is localized this silently breaks:

- A user in Arabic taps "الإفطار" → the route carries `templateName=الإفطار`.
- If the language changes before the destination resolves (a system language change, a locale-driven
  activity recreation, or a restore from a saved back stack), the lookup runs against the English
  template list and `find` returns `null` → the user lands on a **blank** coin editor with no name
  and no choices, silently losing the template they picked.
- The route also carries translated text through a URL, requiring encoding for non-ASCII.

### Required change

| | Before | After |
|---|---|---|
| Route pattern | `coinedit?coinId={coinId}&templateName={templateName}` | `coinedit?coinId={coinId}&templateId={templateId}` |
| Argument name | `templateName` | `templateId` |
| Argument value | localized display name (`"Breakfast"` / `"الإفطار"`) | stable ID (`"breakfast"`) |
| Argument type | `NavType.StringType`, nullable, default `null` | unchanged |
| Lookup | `templates.find { it.name == name }` | `templates.find { it.id == id }` |
| Passed to VM | `template.name`, `template.choices` | `stringResource(nameRes)`, `stringArrayResource(choicesRes).toList()` |

### Resolved shape

```kotlin
// TemplatePickerScreen — callback now carries identity, not display text
onTemplateSelected: (templateId: String) -> Unit

// ChoiceNavHost
onTemplateSelected = { templateId -> navController.navigate("coinedit?templateId=$templateId") }

// coinedit destination
val templateId = backStackEntry.arguments?.getString("templateId")
val template = templateId?.let { id -> CoinTemplates.templates.find { it.id == id } }
val templateName = template?.let { stringResource(it.nameRes) }
val templateChoices = template?.let { stringArrayResource(it.choicesRes).toList() }
val factory = CoinViewModelFactory(
    appContainer.coinRepository, coinId, templateName, templateChoices,
)
```

`CoinViewModelFactory` and `CoinEditViewModel` signatures are **unchanged** — they still receive
`String?` and `List<String>?`. Only the point where those values are produced moves.

### Guarantees

| ID | Guarantee | Requirement |
|---|---|---|
| NV-1 | Route arguments carry no translated text — IDs only | FR-029 |
| NV-2 | `templateId` values are ASCII, so no URL encoding is needed | — |
| NV-3 | A template resolves correctly even if the language changes between picker and editor | FR-010, SC-005 |
| NV-4 | An unknown `templateId` (e.g. a template removed in a later release, restored from a saved back stack) resolves to `null` → a blank editor, exactly as `onStartBlank` does. No crash. | FR-030 |
| NV-5 | Template name/choices are resolved to plain `String`s **before** `CoinEditViewModel`, so a created coin can never re-translate | FR-011, SC-005 |

NV-5 is the materialization boundary from [data-model.md §4](../data-model.md). It needs no guard
code — the resolution point *is* the guarantee.

---

## 2. New destination: app Settings

There is no app-level settings screen today (`CoinSettingsScreen` is per-coin). FR-026 requires
one to host the language selector.

### Route

| Property | Value |
|---|---|
| Route | `"settings"` |
| Arguments | none |
| Entry point | a settings `IconButton` in `MainScreen`'s `TopAppBar`, beside the existing "Create Coin" `Add` action |
| Exit | `onBack = { navController.popBackStack() }` |

```kotlin
composable("settings") {
    SettingsScreen(onBack = { navController.popBackStack() })
}
```

### Screen contract

`SettingsScreen` contains **only** the language selector (Principle I, Principle VIII — no other
settings are added speculatively):

| Element | Content | String key |
|---|---|---|
| Top bar title | "Settings" | `settings_title` |
| Back icon | `Icons.AutoMirrored.Filled.ArrowBack` | `cd_back` |
| Section label | "Language" | `settings_language_label` |
| Option 1 | "System default" → `write(null)` | `settings_language_system` |
| Option 2 | "English" | `settings_language_english` |
| Option 3 | "العربية" | `settings_language_arabic` |

**Option-label rule**: each language's own name is rendered in **that language** (English as
"English", Arabic as "العربية") in **both** resource files — a user who has landed in a language
they cannot read must still be able to find their own. Only `settings_title`,
`settings_language_label`, and `settings_language_system` are translated normally.

### Selection flow

```
user taps a language option
   │
   ├─1─► LanguagePreferenceStore.write(tag)      // tag, or null for "System default"
   ├─2─► LocaleApplier.apply(context, resolved)  // LocaleManager (33+) / persist (26–32)
   ├─3─► API 26–32 only: activity.recreate()     // 33+ recreates automatically
   └─4─► WidgetRefreshCoordinator.refreshAll()   // FR-031 — immediate widget relabel
```

### Guarantees

| ID | Guarantee | Requirement |
|---|---|---|
| SN-1 | The currently active language is visibly indicated as selected | — |
| SN-2 | The selection persists across app restart and process death | FR-026 |
| SN-3 | The flow performs no Room access; coins, choices, history, favorites, and widget associations are untouched | FR-025, SC-004 |
| SN-4 | Re-selecting the already-active language is a no-op (no recreate, no widget churn) | Principle I |
| SN-5 | After step 3 the whole app renders in the new language with the correct layout direction, with no restart and no missing-text placeholder | FR-030, SC-001 |
| SN-6 | Step 4 runs on **every** language change, including a device system-language change while "System default" is selected | FR-031, SC-007 |

---

## 3. Unchanged routes

For the avoidance of doubt, these are **not** modified by this feature — only the literals they
render move into resources:

| Route | Status |
|---|---|
| `main` | unchanged |
| `coinflip/{coinId}` | unchanged |
| `templates` | unchanged |
| `coinsettings/{coinId}` | unchanged |
| `history/{coinId}` | unchanged |
| `statistics/{coinId}` | unchanged |
| `import?payload={payload}` | unchanged — the payload is language-independent (FR-019) |
| `ACTION_SEND` deep link | unchanged |
| `EXTRA_OPEN_COIN_ID` widget deep link | unchanged |
