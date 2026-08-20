package com.choice.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CoinStatisticsTest {

    private fun choice(id: Long, text: String, position: Int) = Choice(
        id = id, coinId = 1L, text = text, position = position,
    )

    private fun decision(id: Long, choiceId: Long?, choiceTextSnapshot: String, decidedAt: Long) = Decision(
        id = id, coinId = 1L, choiceId = choiceId, choiceTextSnapshot = choiceTextSnapshot, decidedAt = decidedAt,
    )

    @Test
    fun computeStatistics_emptyDecisions_returnsZeroTotalAndNulls() {
        val choices = listOf(choice(1L, "A", 0), choice(2L, "B", 1))
        val result = computeStatistics(choices, emptyList())

        assertEquals(0, result.totalDecisions)
        assertEquals(2, result.perChoiceCounts.size)
        assertEquals(0, result.perChoiceCounts[0].count)
        assertEquals(0, result.perChoiceCounts[1].count)
        assertNull(result.mostFrequent)
        assertNull(result.leastFrequent)
        assertNull(result.mostRecentDecision)
    }

    @Test
    fun computeStatistics_countsPerChoice() {
        val choices = listOf(choice(1L, "Eggs", 0), choice(2L, "Ful", 1), choice(3L, "Cheese", 2))
        val decisions = listOf(
            decision(1L, 1L, "Eggs", 1000L),
            decision(2L, 1L, "Eggs", 2000L),
            decision(3L, 2L, "Ful", 3000L),
        )

        val result = computeStatistics(choices, decisions)

        assertEquals(3, result.totalDecisions)
        assertEquals(2, result.perChoiceCounts[0].count) // Eggs
        assertEquals(1, result.perChoiceCounts[1].count) // Ful
        assertEquals(0, result.perChoiceCounts[2].count) // Cheese
    }

    @Test
    fun computeStatistics_mostFrequent_isCorrect() {
        val choices = listOf(choice(1L, "A", 0), choice(2L, "B", 1))
        val decisions = listOf(
            decision(1L, 1L, "A", 1000L),
            decision(2L, 1L, "A", 2000L),
            decision(3L, 1L, "A", 3000L),
            decision(4L, 2L, "B", 4000L),
        )

        val result = computeStatistics(choices, decisions)

        assertEquals("A", result.mostFrequent?.choice?.text)
        assertEquals(3, result.mostFrequent?.count)
    }

    @Test
    fun computeStatistics_leastFrequent_isCorrect() {
        val choices = listOf(choice(1L, "A", 0), choice(2L, "B", 1))
        val decisions = listOf(
            decision(1L, 1L, "A", 1000L),
            decision(2L, 1L, "A", 2000L),
            decision(3L, 1L, "A", 3000L),
            decision(4L, 2L, "B", 4000L),
        )

        val result = computeStatistics(choices, decisions)

        assertEquals("B", result.leastFrequent?.choice?.text)
        assertEquals(1, result.leastFrequent?.count)
    }

    @Test
    fun computeStatistics_tieBreakByPosition() {
        val choices = listOf(choice(1L, "First", 0), choice(2L, "Second", 1), choice(3L, "Third", 2))
        val decisions = listOf(
            decision(1L, 1L, "First", 1000L),
            decision(2L, 2L, "Second", 2000L),
        )

        val result = computeStatistics(choices, decisions)

        assertEquals("First", result.mostFrequent?.choice?.text)
        assertEquals("Third", result.leastFrequent?.choice?.text)
    }

    @Test
    fun computeStatistics_mostRecentDecision_isNewest() {
        val choices = listOf(choice(1L, "A", 0), choice(2L, "B", 1))
        val decisions = listOf(
            decision(1L, 1L, "A", 1000L),
            decision(2L, 2L, "B", 5000L),
            decision(3L, 1L, "A", 3000L),
        )

        val result = computeStatistics(choices, decisions)

        assertNotNull(result.mostRecentDecision)
        assertEquals(2L, result.mostRecentDecision?.id)
        assertEquals("B", result.mostRecentDecision?.choiceTextSnapshot)
    }

    @Test
    fun computeStatistics_handlesEditedChoiceSnapshots() {
        val choices = listOf(choice(1L, "NewName", 0), choice(2L, "B", 1))
        val decisions = listOf(
            decision(1L, 1L, "OldName", 1000L),
            decision(2L, 1L, "OldName", 2000L),
        )

        val result = computeStatistics(choices, decisions)

        assertEquals(2, result.perChoiceCounts[0].count)
        assertEquals(0, result.perChoiceCounts[1].count)
        assertEquals("NewName", result.mostFrequent?.choice?.text)
    }

    @Test
    fun computeStatistics_allSameCount_tieBreaksByPosition() {
        val choices = listOf(choice(1L, "A", 0), choice(2L, "B", 1), choice(3L, "C", 2))
        val decisions = listOf(
            decision(1L, 1L, "A", 1000L),
            decision(2L, 2L, "B", 2000L),
            decision(3L, 3L, "C", 3000L),
        )

        val result = computeStatistics(choices, decisions)

        assertEquals("A", result.mostFrequent?.choice?.text)
        assertEquals("A", result.leastFrequent?.choice?.text)
    }
}
