package com.choice.app.widget

import com.choice.app.domain.Choice
import com.choice.app.domain.Coin
import com.choice.app.domain.CoinWithChoices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [resolveQuickCoinsCoins] (T019): `useFavorites = true` resolving against
 * `observeQuickCoins()`'s result, `useFavorites = false` resolving `explicitCoinIds` against
 * `observeCoins()`'s result with stale/deleted IDs dropped (not shown as broken rows), and both
 * cases producing an empty list when nothing resolves.
 */
class QuickCoinsDefaultSelectionTest {

    private fun coinWithChoices(id: Long, name: String, isFavorite: Boolean = false) = CoinWithChoices(
        coin = Coin(
            id = id,
            name = name,
            createdAt = 0L,
            interactionCount = 0,
            lastInteractionAt = null,
            isFavorite = isFavorite,
        ),
        choices = listOf(
            Choice(id = id * 10, coinId = id, text = "A", position = 0),
            Choice(id = id * 10 + 1, coinId = id, text = "B", position = 1),
        ),
    )

    @Test
    fun useFavorites_resolvesAgainstTheFavoritesList() {
        val favorites = listOf(
            coinWithChoices(1L, "Lunch", isFavorite = true),
            coinWithChoices(2L, "Movie", isFavorite = true),
        )
        val config = QuickCoinsWidgetConfig(useFavorites = true)

        val result = resolveQuickCoinsCoins(config, favorites = favorites, allCoins = favorites)

        assertEquals(listOf(1L, 2L), result.map { it.coin.id })
    }

    @Test
    fun useFavorites_noFavorites_resolvesToEmptyList() {
        val config = QuickCoinsWidgetConfig(useFavorites = true)

        val result = resolveQuickCoinsCoins(
            config,
            favorites = emptyList(),
            allCoins = listOf(coinWithChoices(1L, "Lunch")),
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun explicitCoinIds_resolvesAgainstAllCoins_inStoredOrder() {
        val allCoins = listOf(
            coinWithChoices(1L, "Lunch"),
            coinWithChoices(2L, "Movie"),
            coinWithChoices(3L, "Workout"),
        )
        val config = QuickCoinsWidgetConfig(useFavorites = false, explicitCoinIds = listOf(3L, 1L))

        val result = resolveQuickCoinsCoins(config, favorites = emptyList(), allCoins = allCoins)

        assertEquals(listOf(3L, 1L), result.map { it.coin.id })
    }

    @Test
    fun explicitCoinIds_staleDeletedId_isDroppedNotShownAsBroken() {
        val allCoins = listOf(coinWithChoices(1L, "Lunch"))
        val config = QuickCoinsWidgetConfig(useFavorites = false, explicitCoinIds = listOf(1L, 99L))

        val result = resolveQuickCoinsCoins(config, favorites = emptyList(), allCoins = allCoins)

        assertEquals(listOf(1L), result.map { it.coin.id })
    }

    @Test
    fun explicitCoinIds_allStale_resolvesToEmptyList() {
        val config = QuickCoinsWidgetConfig(useFavorites = false, explicitCoinIds = listOf(404L))

        val result = resolveQuickCoinsCoins(config, favorites = emptyList(), allCoins = emptyList())

        assertTrue(result.isEmpty())
    }
}
