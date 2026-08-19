package com.choice.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectChoiceTest {

    private val choices = listOf(
        Choice(id = 1, coinId = 1, text = "A", position = 0),
        Choice(id = 2, coinId = 1, text = "B", position = 1),
        Choice(id = 3, coinId = 1, text = "C", position = 2),
    )

    @Test
    fun selectChoice_uniformWhenWeightedDisabled() {
        val iterations = 10_000
        val counts = mutableMapOf<Long, Int>()
        choices.forEach { counts[it.id] = 0 }

        repeat(iterations) {
            val result = selectChoice(
                choices = choices,
                weightedEnabled = false,
                avoidLastResultEnabled = false,
                lastChoiceId = null,
            )
            counts[result.id] = counts[result.id]!! + 1
        }

        val expectedPerChoice = iterations.toDouble() / choices.size
        val tolerance = expectedPerChoice * 0.15
        choices.forEach { choice ->
            val count = counts[choice.id]!!.toDouble()
            assertTrue(
                "Choice '${choice.text}' selected $count times, expected ~$expectedPerChoice ± $tolerance",
                count in (expectedPerChoice - tolerance)..(expectedPerChoice + tolerance),
            )
        }
    }

    @Test
    fun selectChoice_weightedWhenEnabled() {
        val weightedChoices = listOf(
            Choice(id = 1, coinId = 1, text = "Heavy", position = 0, weight = 8),
            Choice(id = 2, coinId = 1, text = "Light1", position = 1, weight = 1),
            Choice(id = 3, coinId = 1, text = "Light2", position = 2, weight = 1),
        )

        val iterations = 10_000
        val counts = mutableMapOf<Long, Int>()
        weightedChoices.forEach { counts[it.id] = 0 }

        repeat(iterations) {
            val result = selectChoice(
                choices = weightedChoices,
                weightedEnabled = true,
                avoidLastResultEnabled = false,
                lastChoiceId = null,
            )
            counts[result.id] = counts[result.id]!! + 1
        }

        val totalWeight = weightedChoices.sumOf { it.weight ?: 1 }
        val expectedHeavy = iterations * 8.0 / totalWeight
        val tolerance = expectedHeavy * 0.20
        val heavyCount = counts[1]!!.toDouble()
        assertTrue(
            "Choice 'Heavy' selected $heavyCount times, expected ~$expectedHeavy ± $tolerance",
            heavyCount in (expectedHeavy - tolerance)..(expectedHeavy + tolerance),
        )
    }

    @Test
    fun selectChoice_avoidLastResultApplied() {
        val result = selectChoice(
            choices = choices,
            weightedEnabled = false,
            avoidLastResultEnabled = true,
            lastChoiceId = 1L,
        )
        assertTrue(result.id != 1L)
    }

    @Test
    fun selectChoice_avoidLastResultExcludesLastChoice() {
        val twoChoices = choices.take(2)
        val iterations = 100
        val lastChoiceResults = mutableSetOf<Long>()

        repeat(iterations) {
            val result = selectChoice(
                choices = twoChoices,
                weightedEnabled = false,
                avoidLastResultEnabled = true,
                lastChoiceId = 1L,
            )
            lastChoiceResults.add(result.id)
        }

        // With 2 choices and excluding id=1, only id=2 remains — always selected
        assertEquals(setOf(2L), lastChoiceResults)
    }

    @Test
    fun selectChoice_avoidLastResultFallsBackWhenOnlyOneChoice() {
        val singleChoice = choices.take(1)
        val iterations = 100
        val results = mutableSetOf<Long>()

        repeat(iterations) {
            val result = selectChoice(
                choices = singleChoice,
                weightedEnabled = false,
                avoidLastResultEnabled = true,
                lastChoiceId = 1L,
            )
            results.add(result.id)
        }

        // Falls back to full pool — id=1 is still selectable
        assertTrue(results.contains(1L))
    }

    @Test
    fun selectChoice_emptyListThrows() {
        try {
            selectChoice(
                choices = emptyList(),
                weightedEnabled = false,
                avoidLastResultEnabled = false,
                lastChoiceId = null,
            )
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun selectChoice_deterministicWithSeededRandom() {
        val seed = 42L
        val result1 = selectChoice(
            choices = choices,
            weightedEnabled = true,
            avoidLastResultEnabled = false,
            lastChoiceId = null,
            random = kotlin.random.Random(seed),
        )
        val result2 = selectChoice(
            choices = choices,
            weightedEnabled = true,
            avoidLastResultEnabled = false,
            lastChoiceId = null,
            random = kotlin.random.Random(seed),
        )
        assertEquals(result1, result2)
    }
}
