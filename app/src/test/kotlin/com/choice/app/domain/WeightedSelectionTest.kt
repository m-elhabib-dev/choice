package com.choice.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightedSelectionTest {

    private val choices = listOf(
        Choice(id = 1, coinId = 1, text = "Heavy", position = 0, weight = 8),
        Choice(id = 2, coinId = 1, text = "Light1", position = 1, weight = 1),
        Choice(id = 3, coinId = 1, text = "Light2", position = 2, weight = 1),
    )

    @Test
    fun selectWeighted_alwaysReturnsOneOfTheChoices() {
        val result = selectWeighted(choices)
        assertTrue(choices.any { it.id == result.id })
    }

    @Test
    fun selectWeighted_respectsWeightsOverManyIterations() {
        val iterations = 10_000
        val counts = mutableMapOf<Long, Int>()
        choices.forEach { counts[it.id] = 0 }

        repeat(iterations) {
            val result = selectWeighted(choices)
            counts[result.id] = counts[result.id]!! + 1
        }

        val totalWeight = choices.sumOf { it.weight ?: 1 }
        choices.forEach { choice ->
            val expected = (iterations * (choice.weight ?: 1).toDouble() / totalWeight)
            val tolerance = expected * 0.20
            val count = counts[choice.id]!!.toDouble()
            assertTrue(
                "Choice '${choice.text}' selected $count times, expected ~$expected ± $tolerance",
                count in (expected - tolerance)..(expected + tolerance),
            )
        }
    }

    @Test
    fun selectWeighted_nullWeightDefaultsToOne() {
        val choicesWithNull = listOf(
            Choice(id = 1, coinId = 1, text = "A", position = 0, weight = null),
            Choice(id = 2, coinId = 1, text = "B", position = 1, weight = 3),
        )

        val iterations = 10_000
        val counts = mutableMapOf<Long, Int>()
        choicesWithNull.forEach { counts[it.id] = 0 }

        repeat(iterations) {
            val result = selectWeighted(choicesWithNull)
            counts[result.id] = counts[result.id]!! + 1
        }

        val expectedA = iterations * 1.0 / 4.0
        val tolerance = expectedA * 0.20
        val countA = counts[1]!!.toDouble()
        assertTrue(
            "Choice 'A' (null weight) selected $countA times, expected ~$expectedA ± $tolerance",
            countA in (expectedA - tolerance)..(expectedA + tolerance),
        )
    }

    @Test
    fun selectWeighted_equalWeightsDistribution() {
        val equalChoices = listOf(
            Choice(id = 1, coinId = 1, text = "A", position = 0, weight = 5),
            Choice(id = 2, coinId = 1, text = "B", position = 1, weight = 5),
        )

        val iterations = 10_000
        val counts = mutableMapOf<Long, Int>()
        equalChoices.forEach { counts[it.id] = 0 }

        repeat(iterations) {
            val result = selectWeighted(equalChoices)
            counts[result.id] = counts[result.id]!! + 1
        }

        val expected = iterations.toDouble() / 2
        val tolerance = expected * 0.15
        equalChoices.forEach { choice ->
            val count = counts[choice.id]!!.toDouble()
            assertTrue(
                "Choice '${choice.text}' selected $count times, expected ~$expected ± $tolerance",
                count in (expected - tolerance)..(expected + tolerance),
            )
        }
    }

    @Test
    fun selectWeighted_emptyListThrows() {
        try {
            selectWeighted(emptyList())
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun selectWeighted_deterministicWithSeededRandom() {
        val seed = 42L
        val result1 = selectWeighted(choices, kotlin.random.Random(seed))
        val result2 = selectWeighted(choices, kotlin.random.Random(seed))
        assertEquals(result1, result2)
    }
}
