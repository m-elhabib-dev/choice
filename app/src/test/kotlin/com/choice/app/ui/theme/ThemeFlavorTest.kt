package com.choice.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the full 8-row resolution table from contracts/theme-contract.md §2.
 */
class ThemeFlavorTest {

    // Row 1
    @Test
    fun `row1 latte resolves to LATTE`() {
        assertEquals(ThemeFlavor.LATTE, resolveThemeFlavor("latte"))
    }

    // Row 2
    @Test
    fun `row2 frappe resolves to FRAPPE`() {
        assertEquals(ThemeFlavor.FRAPPE, resolveThemeFlavor("frappe"))
    }

    // Row 3
    @Test
    fun `row3 macchiato resolves to MACCHIATO`() {
        assertEquals(ThemeFlavor.MACCHIATO, resolveThemeFlavor("macchiato"))
    }

    // Row 4
    @Test
    fun `row4 mocha resolves to MOCHA`() {
        assertEquals(ThemeFlavor.MOCHA, resolveThemeFlavor("mocha"))
    }

    // Row 5 — never-set default (FR-005)
    @Test
    fun `row5 null resolves to MOCHA default`() {
        assertEquals(ThemeFlavor.MOCHA, resolveThemeFlavor(null))
    }

    // Row 6 — blank treated as absent (FR-006)
    @Test
    fun `row6 blank resolves to MOCHA`() {
        assertEquals(ThemeFlavor.MOCHA, resolveThemeFlavor(""))
    }

    // Row 7 — unrecognized value (FR-006)
    @Test
    fun `row7 unrecognized value resolves to MOCHA`() {
        assertEquals(ThemeFlavor.MOCHA, resolveThemeFlavor("dracula"))
    }

    // Row 8 — id matching is exact, not case-folded (FR-006)
    @Test
    fun `row8 wrong case resolves to MOCHA, not the intended flavor`() {
        assertEquals(ThemeFlavor.MOCHA, resolveThemeFlavor("Latte"))
    }

    @Test
    fun `fromId returns null for anything unrecognized`() {
        assertNull(ThemeFlavor.fromId(null))
        assertNull(ThemeFlavor.fromId(""))
        assertNull(ThemeFlavor.fromId("dracula"))
        assertNull(ThemeFlavor.fromId("Latte"))
    }

    @Test
    fun `fromId returns the matching flavor for each valid id`() {
        assertEquals(ThemeFlavor.LATTE, ThemeFlavor.fromId("latte"))
        assertEquals(ThemeFlavor.FRAPPE, ThemeFlavor.fromId("frappe"))
        assertEquals(ThemeFlavor.MACCHIATO, ThemeFlavor.fromId("macchiato"))
        assertEquals(ThemeFlavor.MOCHA, ThemeFlavor.fromId("mocha"))
    }

    @Test
    fun `DEFAULT is MOCHA`() {
        assertEquals(ThemeFlavor.MOCHA, ThemeFlavor.DEFAULT)
    }
}
