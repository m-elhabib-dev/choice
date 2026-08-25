package com.choice.app.ui.format

import com.choice.app.locale.formattingLocale
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionTimeFormatterTest {

    private val zone = ZoneId.of("UTC")
    // 2026-08-24T15:30:00Z — an arbitrary, fixed instant.
    private val epochMillis = 1787664600000L

    @Test
    fun format_englishVsArabicFormattingLocale_rendersDifferently() {
        val english = DecisionTimeFormatter.format(epochMillis, formattingLocale("en"), zone)
        val arabic = DecisionTimeFormatter.format(epochMillis, formattingLocale("ar"), zone)

        // DF-3: a real locale switch, not a fallback to English.
        assertNotEquals(english, arabic)
    }

    @Test
    fun format_arabicFormattingLocale_usesOnlyWesternArabicDigits() {
        val arabic = DecisionTimeFormatter.format(epochMillis, formattingLocale("ar"), zone)

        // DF-2/SC-006: rendered digits are always 0-9, never the Arabic-Indic digit block.
        val digits = arabic.filter { it.isDigit() }
        assertTrue(digits.isNotEmpty())
        assertTrue(digits.all { it in '0'..'9' })
    }

    @Test
    fun formattingLocale_arabic_carriesLatinNumberingExtension() {
        assertEquals("ar-u-nu-latn", formattingLocale("ar").toLanguageTag())
    }

    @Test
    fun format_englishFormattingLocale_usesOnlyWesternArabicDigits() {
        val english = DecisionTimeFormatter.format(epochMillis, formattingLocale("en"), zone)

        val digits = english.filter { it.isDigit() }
        assertTrue(digits.isNotEmpty())
        assertTrue(digits.all { it in '0'..'9' })
    }

    @Test
    fun format_sameInstantAndZone_isDeterministic() {
        val first = DecisionTimeFormatter.format(epochMillis, formattingLocale("en"), zone)
        val second = DecisionTimeFormatter.format(epochMillis, formattingLocale("en"), zone)

        assertEquals(first, second)
    }
}
