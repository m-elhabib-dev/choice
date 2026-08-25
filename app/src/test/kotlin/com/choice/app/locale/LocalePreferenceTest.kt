package com.choice.app.locale

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the full resolution table from contracts/localization-contract.md §2, including
 * row 8's FR-027 stored-but-unsupported-tag case.
 */
class LocalePreferenceTest {

    // Row 1: explicit, supported storedTag beats the system (FR-026).
    @Test
    fun `row1 explicit ar beats system en`() {
        assertEquals("ar", resolveLanguageTag("ar", listOf("en-US")))
    }

    // Row 2: explicit, supported storedTag beats the system, other direction.
    @Test
    fun `row2 explicit en beats system ar`() {
        assertEquals("en", resolveLanguageTag("en", listOf("ar-EG")))
    }

    // Row 3: no override, system's first entry (with region) is supported (FR-024).
    @Test
    fun `row3 null storedTag follows first supported system tag`() {
        assertEquals("ar", resolveLanguageTag(null, listOf("ar-EG", "en-US")))
    }

    // Row 4: no override, system entry with region resolves to en (FR-024).
    @Test
    fun `row4 null storedTag follows system en-GB`() {
        assertEquals("en", resolveLanguageTag(null, listOf("en-GB")))
    }

    // Row 5: no override, unsupported system language falls back to English (FR-027).
    @Test
    fun `row5 unsupported system language falls back to English`() {
        assertEquals("en", resolveLanguageTag(null, listOf("fr-FR")))
    }

    // Row 6: no override, first unsupported entry is skipped in favor of the next supported one.
    @Test
    fun `row6 first supported entry wins even if not first overall`() {
        assertEquals("ar", resolveLanguageTag(null, listOf("fr-FR", "ar-SA")))
    }

    // Row 7: no override, empty system list falls back to English (FR-027).
    @Test
    fun `row7 empty system list falls back to English`() {
        assertEquals("en", resolveLanguageTag(null, emptyList()))
    }

    // Row 8: unsupported *stored* tag falls back to English unconditionally, never to the
    // system language (FR-027) — the row that is easy to get wrong.
    @Test
    fun `row8 unsupported stored tag falls back to English not system`() {
        assertEquals("en", resolveLanguageTag("de", listOf("ar-EG")))
    }

    // Row 9: a blank storedTag is treated as absent ("follow system").
    @Test
    fun `row9 blank storedTag treated as absent`() {
        assertEquals("ar", resolveLanguageTag("", listOf("ar-EG")))
    }

    // Matching is on the language subtag only, case-insensitively.
    @Test
    fun `matching is case-insensitive and region-agnostic`() {
        assertEquals("ar", resolveLanguageTag("AR", emptyList()))
        assertEquals("ar", resolveLanguageTag(null, listOf("ar_SA")))
        assertEquals("ar", resolveLanguageTag(null, listOf("AR-eg")))
    }

    @Test
    fun `uiLocale resolves the plain language tag`() {
        assertEquals("en", uiLocale("en").toLanguageTag())
        assertEquals("ar", uiLocale("ar").toLanguageTag())
    }

    // formattingLocale always forces Western Arabic (nu-latn) digits, even for Arabic.
    @Test
    fun `formattingLocale carries the nu-latn extension`() {
        assertEquals("en-u-nu-latn", formattingLocale("en").toLanguageTag())
        assertEquals("ar-u-nu-latn", formattingLocale("ar").toLanguageTag())
    }
}
