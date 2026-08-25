package com.choice.app.locale

/**
 * The set of languages Choice ships resources for.
 *
 * FR-029 extension point: adding a language means appending a tag here, adding one
 * `res/values-<tag>/strings.xml`, and adding one `<locale>` line to
 * `res/xml/locales_config.xml`. No other file should need to change — see
 * contracts/localization-contract.md §1.
 */
object SupportedLanguages {
    const val ENGLISH = "en"
    const val ARABIC = "ar"
    const val DEFAULT = ENGLISH

    /** Ordered; first entry is the default/fallback. */
    val TAGS: List<String> = listOf(ENGLISH, ARABIC)
}
