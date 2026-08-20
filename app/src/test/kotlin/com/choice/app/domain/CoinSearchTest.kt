package com.choice.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinSearchTest {

    @Test
    fun matchesSearchQuery_blankQuery_matchesEverything() {
        assertTrue(matchesSearchQuery("Breakfast", ""))
        assertTrue(matchesSearchQuery("Breakfast", "   "))
        assertTrue(matchesSearchQuery("Breakfast", "\t"))
    }

    @Test
    fun matchesSearchQuery_exactMatch() {
        assertTrue(matchesSearchQuery("Breakfast", "Breakfast"))
    }

    @Test
    fun matchesSearchQuery_caseInsensitive() {
        assertTrue(matchesSearchQuery("Breakfast", "breakfast"))
        assertTrue(matchesSearchQuery("Breakfast", "BREAKFAST"))
        assertTrue(matchesSearchQuery("Breakfast", "bReAkFaSt"))
    }

    @Test
    fun matchesSearchQuery_substringMatch() {
        assertTrue(matchesSearchQuery("Breakfast", "break"))
        assertTrue(matchesSearchQuery("Breakfast", "fast"))
        assertTrue(matchesSearchQuery("Breakfast", "ea"))
    }

    @Test
    fun matchesSearchQuery_noMatch() {
        assertFalse(matchesSearchQuery("Breakfast", "Lunch"))
        assertFalse(matchesSearchQuery("Breakfast", "XYZ"))
    }

    @Test
    fun matchesSearchQuery_emptyCoinName() {
        assertFalse(matchesSearchQuery("", "test"))
    }

    @Test
    fun matchesSearchQuery_emptyBoth() {
        assertTrue(matchesSearchQuery("", ""))
    }
}
