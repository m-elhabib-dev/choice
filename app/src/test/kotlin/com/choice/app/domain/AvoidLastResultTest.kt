package com.choice.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvoidLastResultTest {

    private val choices = listOf(
        Choice(id = 1, coinId = 1, text = "A", position = 0),
        Choice(id = 2, coinId = 1, text = "B", position = 1),
        Choice(id = 3, coinId = 1, text = "C", position = 2),
    )

    @Test
    fun applyAvoidLastResult_excludesMatchingChoice() {
        val result = applyAvoidLastResult(choices, lastChoiceId = 1L)
        assertEquals(2, result.size)
        assertTrue(result.none { it.id == 1L })
    }

    @Test
    fun applyAvoidLastResult_nullLastChoiceIdReturnsUnchanged() {
        val result = applyAvoidLastResult(choices, lastChoiceId = null)
        assertEquals(choices, result)
    }

    @Test
    fun applyAvoidLastResult_nonMatchingIdReturnsUnchanged() {
        val result = applyAvoidLastResult(choices, lastChoiceId = 99L)
        assertEquals(choices, result)
    }

    @Test
    fun applyAvoidLastResult_fallsBackWhenOnlyOneRemaining() {
        val singleChoice = listOf(choices[0])
        val result = applyAvoidLastResult(singleChoice, lastChoiceId = 1L)
        assertEquals(singleChoice, result)
    }

    @Test
    fun applyAvoidLastResult_fallsBackWhenAllWouldBeExcluded() {
        val result = applyAvoidLastResult(choices, lastChoiceId = 1L)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun applyAvoidLastResult_nonFallbackWhenTwoRemain() {
        val result = applyAvoidLastResult(choices, lastChoiceId = 1L)
        assertEquals(2, result.size)
    }

    @Test
    fun applyAvoidLastResult_preservesOrder() {
        val result = applyAvoidLastResult(choices, lastChoiceId = 2L)
        assertEquals(listOf(choices[0], choices[2]), result)
    }

    @Test
    fun applyAvoidLastResult_withTwoChoicesExcludesOne() {
        val twoChoices = choices.take(2)
        val result = applyAvoidLastResult(twoChoices, lastChoiceId = 1L)
        assertEquals(1, result.size)
        assertEquals(choices[1], result[0])
    }

    @Test
    fun applyAvoidLastResult_fallsBackWithTwoChoicesWhenOneRemoved() {
        val singleChoice = listOf(choices[0])
        val result = applyAvoidLastResult(singleChoice, lastChoiceId = 1L)
        assertEquals(singleChoice, result)
    }
}
