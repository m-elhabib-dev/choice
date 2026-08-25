package com.choice.app.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Locale-aware decision timestamp formatting — contracts/localization-contract.md §6.
 *
 * Pure and explicit about [locale]/[zone] (never `Locale.getDefault()`), so it is unit-testable on
 * the JVM (app/src/test) rather than requiring a device. Callers pass `formattingLocale(tag)`
 * (see `com.choice.app.locale.LocalePreference`), never `uiLocale(tag)` directly, so rendered
 * digits are always Western Arabic (`0`-`9`) regardless of UI language (FR-015, SC-006).
 */
object DecisionTimeFormatter {

    /**
     * Element order, separators, and connector wording come from CLDR for [locale] (DF-1). No
     * hardcoded pattern string is used (DF-4) — [DateTimeFormatter.ofLocalizedDateTime] derives
     * the medium-date/short-time style CLDR defines for the locale.
     */
    fun format(epochMillis: Long, locale: Locale, zone: ZoneId): String {
        val formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
        return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))
    }
}
