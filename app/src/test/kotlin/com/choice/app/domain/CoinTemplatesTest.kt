package com.choice.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinTemplatesTest {

    @Test
    fun coinTemplates_hasFourTemplates() {
        assertEquals(4, CoinTemplates.templates.size)
    }

    @Test
    fun coinTemplates_hasBreakfastTemplate() {
        val template = CoinTemplates.templates.find { it.name == "Breakfast" }
        assertEquals(true, template != null)
        assertEquals(listOf("Ful", "Eggs", "Falafel", "Cheese"), template!!.choices)
    }

    @Test
    fun coinTemplates_hasLunchTemplate() {
        val template = CoinTemplates.templates.find { it.name == "Lunch" }
        assertEquals(true, template != null)
        assertEquals(listOf("Rice", "Pasta", "Chicken", "Lentils"), template!!.choices)
    }

    @Test
    fun coinTemplates_hasWorkoutTemplate() {
        val template = CoinTemplates.templates.find { it.name == "Workout" }
        assertEquals(true, template != null)
        assertEquals(listOf("Chest", "Back", "Legs", "Full Body"), template!!.choices)
    }

    @Test
    fun coinTemplates_hasMovieTemplate() {
        val template = CoinTemplates.templates.find { it.name == "Movie" }
        assertEquals(true, template != null)
        assertEquals(listOf("Interstellar", "Dune", "Batman", "The Matrix"), template!!.choices)
    }

    @Test
    fun coinTemplates_allTemplatesHaveAtLeastTwoChoices() {
        assertTrue(CoinTemplates.templates.all { it.choices.size >= 2 })
    }
}
