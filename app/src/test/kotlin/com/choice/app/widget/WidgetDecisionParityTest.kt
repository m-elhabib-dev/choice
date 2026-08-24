package com.choice.app.widget

import com.choice.app.domain.Choice
import com.choice.app.domain.Coin
import com.choice.app.domain.CoinWithChoices
import com.choice.app.domain.selectChoice
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Pins User Story 3 ("widget decisions follow each coin's configured rules", T027): asserts that
 * [resolveFlipChoice] — the single function [FlipSingleCoinAction] and [FlipQuickCoinAction]
 * (WidgetActions.kt) both delegate their selection to — calls the exact same domain [selectChoice]
 * with the coin's current `weightedEnabled`/`avoidLastResultEnabled` values and the supplied
 * `lastChoiceId`, with no widget-local selection logic in between. Verified by asserting identical
 * results to a direct [selectChoice] call given the same seeded [Random], for uniform, weighted,
 * and avoid-last-result configurations.
 */
class WidgetDecisionParityTest {

    private fun choices(vararg weights: Int?) = weights.mapIndexed { index, weight ->
        Choice(
            id = index.toLong() + 1,
            coinId = 1L,
            text = "Choice $index",
            position = index,
            weight = weight,
        )
    }

    private fun coin(weightedEnabled: Boolean, avoidLastResultEnabled: Boolean) = Coin(
        id = 1L,
        name = "Test Coin",
        createdAt = 0L,
        interactionCount = 0,
        lastInteractionAt = null,
        weightedEnabled = weightedEnabled,
        avoidLastResultEnabled = avoidLastResultEnabled,
    )

    @Test
    fun uniform_producesSameResultAsDirectSelectChoiceCall() {
        val choices = choices(null, null, null)
        val coinWithChoices = CoinWithChoices(coin(weightedEnabled = false, avoidLastResultEnabled = false), choices)

        val actual = resolveFlipChoice(coinWithChoices, lastChoiceId = null, random = Random(1))
        val expected = selectChoice(
            choices = choices,
            weightedEnabled = false,
            avoidLastResultEnabled = false,
            lastChoiceId = null,
            random = Random(1),
        )

        assertEquals(expected, actual)
    }

    @Test
    fun weightedEnabled_passesTheCoinsLiveWeightedFlagThrough() {
        val choices = choices(1, 5, 20)
        val coinWithChoices = CoinWithChoices(coin(weightedEnabled = true, avoidLastResultEnabled = false), choices)

        val actual = resolveFlipChoice(coinWithChoices, lastChoiceId = null, random = Random(42))
        val expected = selectChoice(
            choices = choices,
            weightedEnabled = true,
            avoidLastResultEnabled = false,
            lastChoiceId = null,
            random = Random(42),
        )

        assertEquals(expected, actual)
    }

    @Test
    fun avoidLastResultEnabled_usesTheSuppliedLastChoiceIdAndExcludesIt() {
        val choices = choices(null, null, null)
        val coinWithChoices = CoinWithChoices(coin(weightedEnabled = false, avoidLastResultEnabled = true), choices)
        val lastChoiceId = choices[1].id

        // Run many seeds: the avoided choice must never be selected, and every run matches a
        // direct selectChoice call given the same seed and the same lastChoiceId.
        for (seed in 0 until 50) {
            val actual = resolveFlipChoice(coinWithChoices, lastChoiceId = lastChoiceId, random = Random(seed))
            val expected = selectChoice(
                choices = choices,
                weightedEnabled = false,
                avoidLastResultEnabled = true,
                lastChoiceId = lastChoiceId,
                random = Random(seed),
            )

            assertEquals(expected, actual)
            assertNotEquals(lastChoiceId, actual.id)
        }
    }

    @Test
    fun avoidLastResultDisabled_lastChoiceIdIsIgnored_canRepeat() {
        val choices = choices(null, null)
        val coinWithChoices = CoinWithChoices(coin(weightedEnabled = false, avoidLastResultEnabled = false), choices)
        val lastChoiceId = choices[0].id

        val actual = resolveFlipChoice(coinWithChoices, lastChoiceId = lastChoiceId, random = Random(3))
        val expected = selectChoice(
            choices = choices,
            weightedEnabled = false,
            avoidLastResultEnabled = false,
            lastChoiceId = lastChoiceId,
            random = Random(3),
        )

        assertEquals(expected, actual)
    }
}
