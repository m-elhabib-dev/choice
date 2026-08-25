package com.choice.app.locale

import java.util.Locale

/**
 * Pure locale-resolution logic. No Android dependency, so this is unit-tested on the JVM
 * (app/src/test) rather than requiring a device — see contracts/localization-contract.md §2.
 */

/**
 * Resolves the language tag Choice should actually use.
 *
 * @param storedTag the user's explicit in-app override, or `null`/blank for "follow system"
 * @param systemLanguageTags the device's language preference list, highest priority first
 *   (e.g. `["fr-FR", "en-US"]`)
 * @return one of [SupportedLanguages.TAGS]
 *
 * Behaviour table (contracts/localization-contract.md §2):
 * - An explicit, supported [storedTag] always wins over the system (FR-026).
 * - An explicit but *unsupported* [storedTag] falls back to [SupportedLanguages.DEFAULT]
 *   unconditionally — it does NOT fall through to the system language (FR-027, row 8).
 * - With no override, the first *supported* entry in [systemLanguageTags] wins (FR-024).
 * - If nothing supported is found anywhere, [SupportedLanguages.DEFAULT] is returned (FR-027).
 *
 * Matching is on the language subtag only, case-insensitively: `"ar-EG"`, `"ar_SA"`, and `"AR"`
 * all match `"ar"`.
 */
fun resolveLanguageTag(storedTag: String?, systemLanguageTags: List<String>): String {
    if (!storedTag.isNullOrBlank()) {
        return matchSupportedTag(storedTag) ?: SupportedLanguages.DEFAULT
    }
    for (systemTag in systemLanguageTags) {
        val match = matchSupportedTag(systemTag)
        if (match != null) return match
    }
    return SupportedLanguages.DEFAULT
}

/** Returns the canonical supported tag matching [tag]'s language subtag, or null if unsupported. */
private fun matchSupportedTag(tag: String): String? {
    val languageSubtag = tag.substringBefore('-').substringBefore('_').lowercase(Locale.ROOT)
    return SupportedLanguages.TAGS.firstOrNull { it == languageSubtag }
}

/** Resource resolution + layout direction. */
fun uiLocale(tag: String): Locale = Locale.forLanguageTag(tag)

/**
 * Dates, times, percentages. ALWAYS carries the `nu-latn` Unicode extension, forcing Western
 * Arabic digits (0-9) regardless of language — FR-015, SC-006.
 */
fun formattingLocale(tag: String): Locale =
    Locale.Builder()
        .setLanguage(tag)
        .setUnicodeLocaleKeyword("nu", "latn")
        .build()
