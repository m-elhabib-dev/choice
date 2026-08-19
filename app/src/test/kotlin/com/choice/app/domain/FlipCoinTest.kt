package com.choice.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipCoinTest {

    private val choices = listOf(
        Choice(id = 1, coinId = 1, text = "A", position = 0),
        Choice(id = 2, coinId = 1, text = "B", position = 1),
        Choice(id = 3, coinId = 1, text = "C", position = 2),
    )

    @Test
    fun flipCoin_alwaysReturnsOneOfTheChoices() {
        val result = flipCoin(choices)
        assertTrue(choices.any { it.id == result.id })
    }

    @Test
    fun flipCoin_distributionStaysWithinStatisticalBounds() {
        val iterations = 10_000
        val counts = mutableMapOf<Long, Int>()
        choices.forEach { counts[it.id] = 0 }

        repeat(iterations) {
            val result = flipCoin(choices)
            counts[result.id] = counts[result.id]!! + 1
        }

        val expectedPerChoice = iterations.toDouble() / choices.size
        val tolerance = expectedPerChoice * 0.15

        choices.forEach { choice ->
            val count = counts[choice.id]!!
            assertTrue(
                "Choice '${choice.text}' selected $count times, expected ~$expectedPerChoice ± $tolerance",
                count in (expectedPerChoice - tolerance).toInt()..(expectedPerChoice + tolerance).toInt(),
            )
        }
    }

    @Test
    fun flipCoin_emptyListThrows() {
        try {
            flipCoin(emptyList())
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun flipCoin_deterministicWithSeededRandom() {
        val seed = 42L
        val result = flipCoin(choices, kotlin.random.Random(seed))
        val expected = flipCoin(choices, kotlin.random.Random(seed))
        assertEquals(expected, result)
    }
}
