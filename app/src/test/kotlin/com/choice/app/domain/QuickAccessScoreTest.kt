package com.choice.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAccessScoreTest {

    @Test
    fun score_isZero_whenLastInteractionIsNull() {
        val score = QuickAccessScore.compute(interactionCount = 5, lastInteractionAt = null)
        assertEquals(0.0, score, 0.001)
    }

    @Test
    fun score_isZero_whenInteractionCountIsZero() {
        val score = QuickAccessScore.compute(interactionCount = 0, lastInteractionAt = System.currentTimeMillis())
        assertEquals(0.0, score, 0.001)
    }

    @Test
    fun score_isPositive_whenHasInteractions() {
        val score = QuickAccessScore.compute(interactionCount = 3, lastInteractionAt = System.currentTimeMillis())
        assertTrue(score > 0.0)
    }

    @Test
    fun score_isHigher_forMoreInteractions() {
        val now = System.currentTimeMillis()
        val scoreLow = QuickAccessScore.compute(interactionCount = 1, lastInteractionAt = now)
        val scoreHigh = QuickAccessScore.compute(interactionCount = 5, lastInteractionAt = now)
        assertTrue(scoreHigh > scoreLow)
    }

    @Test
    fun score_isHigher_forMoreRecentInteractions() {
        val msPerHour = 1000L * 60 * 60
        val now = System.currentTimeMillis()
        val scoreRecent = QuickAccessScore.compute(interactionCount = 3, lastInteractionAt = now)
        val scoreOld = QuickAccessScore.compute(interactionCount = 3, lastInteractionAt = now - 72 * msPerHour)
        assertTrue(scoreRecent > scoreOld)
    }

    @Test
    fun score_halvingAfterHalfLife() {
        val msPerHour = 1000L * 60 * 60
        val now = System.currentTimeMillis()
        val scoreImmediate = QuickAccessScore.compute(interactionCount = 10, lastInteractionAt = now)
        val scoreAfterHalfLife = QuickAccessScore.compute(interactionCount = 10, lastInteractionAt = now - (72 * msPerHour))
        assertEquals(scoreImmediate / 2.0, scoreAfterHalfLife, 0.01)
    }

    @Test
    fun score_decaysTowardsZero_overVeryLongTime() {
        val msPerHour = 1000L * 60 * 60
        val now = System.currentTimeMillis()
        val scoreOld = QuickAccessScore.compute(interactionCount = 1, lastInteractionAt = now - 720 * msPerHour)
        assertTrue(scoreOld < 0.01)
    }

    @Test
    fun score_ranking_ordersByRecencyAndFrequency() {
        val msPerHour = 1000L * 60 * 60
        val now = System.currentTimeMillis()

        // score = count * 2^(-hours/72):
        //   MostFrequent:  10 * 2^(-100/72) ≈ 3.78
        //   MostRecent:     1 * 2^(0)       = 1.0
        //   OldAndRare:     2 * 2^(-500/72) ≈ 0.017
        val coins = listOf(
            "MostRecent" to QuickAccessScore.compute(1, now),
            "MostFrequent" to QuickAccessScore.compute(10, now - 100 * msPerHour),
            "OldAndRare" to QuickAccessScore.compute(2, now - 500 * msPerHour),
        )

        val ranked = coins.sortedByDescending { it.second }
        assertEquals("MostFrequent", ranked[0].first)
        assertEquals("MostRecent", ranked[1].first)
        assertEquals("OldAndRare", ranked[2].first)
        assertTrue(ranked[0].second > ranked[1].second)
        assertTrue(ranked[1].second > ranked[2].second)
    }
}
