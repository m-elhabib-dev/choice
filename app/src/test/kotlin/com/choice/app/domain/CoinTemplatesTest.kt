package com.choice.app.domain

import com.choice.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinTemplatesTest {

    @Test
    fun coinTemplates_hasFourTemplates() {
        assertEquals(4, CoinTemplates.templates.size)
    }

    @Test
    fun coinTemplates_idsAreStableAndUnique() {
        val ids = CoinTemplates.templates.map { it.id }
        assertEquals(listOf("breakfast", "lunch", "workout", "movie"), ids)
        assertEquals(ids.toSet().size, ids.size)
    }

    @Test
    fun coinTemplates_hasBreakfastTemplate() {
        val template = CoinTemplates.templates.find { it.id == "breakfast" }
        assertEquals(true, template != null)
        assertEquals(R.string.template_breakfast_name, template!!.nameRes)
        assertEquals(R.array.template_breakfast_choices, template.choicesRes)
    }

    @Test
    fun coinTemplates_hasLunchTemplate() {
        val template = CoinTemplates.templates.find { it.id == "lunch" }
        assertEquals(true, template != null)
        assertEquals(R.string.template_lunch_name, template!!.nameRes)
        assertEquals(R.array.template_lunch_choices, template.choicesRes)
    }

    @Test
    fun coinTemplates_hasWorkoutTemplate() {
        val template = CoinTemplates.templates.find { it.id == "workout" }
        assertEquals(true, template != null)
        assertEquals(R.string.template_workout_name, template!!.nameRes)
        assertEquals(R.array.template_workout_choices, template.choicesRes)
    }

    @Test
    fun coinTemplates_hasMovieTemplate() {
        val template = CoinTemplates.templates.find { it.id == "movie" }
        assertEquals(true, template != null)
        assertEquals(R.string.template_movie_name, template!!.nameRes)
        assertEquals(R.array.template_movie_choices, template.choicesRes)
    }

    @Test
    fun coinTemplates_noTemplateIdIsBlank() {
        assertTrue(CoinTemplates.templates.all { it.id.isNotBlank() })
    }
}
