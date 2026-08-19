package com.choice.app.data

import com.choice.app.data.local.CoinDao
import com.choice.app.data.local.CoinEntity
import com.choice.app.data.local.CoinWithChoicesRelation
import com.choice.app.data.local.ChoiceEntity
import com.choice.app.data.local.DecisionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinRepositoryImplQuickCoinsTest {

    private fun fakeDao(coins: List<CoinWithChoicesRelation>): CoinDao =
        object : CoinDao {
            override fun observeCoins(): Flow<List<CoinWithChoicesRelation>> = flowOf(coins)
            override fun observeQuickCoins(): Flow<List<CoinWithChoicesRelation>> =
                flowOf(coins.filter { it.coin.isFavorite }.sortedByDescending { it.coin.lastInteractionAt })
            override fun observeCoin(coinId: Long): Flow<CoinWithChoicesRelation?> =
                flowOf(coins.firstOrNull { it.coin.id == coinId })
            override suspend fun insertCoin(coin: CoinEntity): Long = coin.id
            override suspend fun insertChoices(choices: List<ChoiceEntity>) {}
            override suspend fun deleteChoicesByCoinId(coinId: Long) {}
            override suspend fun recordInteraction(coinId: Long, now: Long) {}
            override suspend fun deleteCoinById(coinId: Long) {}
            override suspend fun setFavorite(coinId: Long, isFavorite: Boolean) {}
            override suspend fun insertDecision(decision: DecisionEntity): Long = decision.id
            override fun observeDecisionsByCoinId(coinId: Long): Flow<List<DecisionEntity>> = flowOf(emptyList())
            override suspend fun getLastDecisionByCoinId(coinId: Long): DecisionEntity? = null
            override suspend fun updateChoicePosition(choiceId: Long, position: Int) {}
            override suspend fun setWeightedEnabled(coinId: Long, enabled: Boolean) {}
            override suspend fun setAvoidLastResultEnabled(coinId: Long, enabled: Boolean) {}
            override suspend fun updateChoiceWeight(choiceId: Long, weight: Int?) {}
        }

    private fun coin(
        id: Long,
        name: String,
        isFavorite: Boolean,
        lastInteractionAt: Long?,
    ): CoinWithChoicesRelation =
        CoinWithChoicesRelation(
            coin = CoinEntity(
                id = id,
                name = name,
                isFavorite = isFavorite,
                lastInteractionAt = lastInteractionAt,
            ),
            choices = listOf(
                ChoiceEntity(coinId = id, text = "A", position = 0),
                ChoiceEntity(coinId = id, text = "B", position = 1),
            ),
        )

    @Test
    fun quickCoins_returnsOnlyFavoritedCoins() = runTest {
        val now = System.currentTimeMillis()
        val dao = fakeDao(
            listOf(
                coin(1, "Not Favorite", false, now),
                coin(2, "Favorite", true, now),
            ),
        )
        val repo = CoinRepositoryImpl(dao)

        val result = repo.observeQuickCoins().first()

        assertEquals(1, result.size)
        assertEquals("Favorite", result[0].coin.name)
    }

    @Test
    fun quickCoins_ordersByLastInteractionAtDescending() = runTest {
        val now = System.currentTimeMillis()
        val msPerHour = 1000L * 60 * 60
        val dao = fakeDao(
            listOf(
                coin(1, "Recent", true, now),
                coin(2, "Older", true, now - 3 * msPerHour),
            ),
        )
        val repo = CoinRepositoryImpl(dao)

        val result = repo.observeQuickCoins().first()

        assertEquals(2, result.size)
        assertEquals("Recent", result[0].coin.name)
        assertEquals("Older", result[1].coin.name)
    }

    @Test
    fun quickCoins_excludesNonFavoritedCoins() = runTest {
        val now = System.currentTimeMillis()
        val dao = fakeDao(
            listOf(
                coin(1, "Fav A", true, now),
                coin(2, "Not Fav", false, now),
                coin(3, "Fav B", true, now - 1000),
            ),
        )
        val repo = CoinRepositoryImpl(dao)

        val result = repo.observeQuickCoins().first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.coin.isFavorite })
    }

    @Test
    fun quickCoins_returnsEmptyWhenNoFavorites() = runTest {
        val now = System.currentTimeMillis()
        val dao = fakeDao(
            listOf(
                coin(1, "A", false, now),
                coin(2, "B", false, now),
            ),
        )
        val repo = CoinRepositoryImpl(dao)

        val result = repo.observeQuickCoins().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun quickCoins_ordersFavoritedByRecencyNotFrequency() = runTest {
        val now = System.currentTimeMillis()
        val msPerHour = 1000L * 60 * 60
        val dao = fakeDao(
            listOf(
                coin(1, "RareButRecent", true, now),
                coin(2, "FrequentButOld", true, now - 100 * msPerHour),
            ),
        )
        val repo = CoinRepositoryImpl(dao)

        val result = repo.observeQuickCoins().first()

        assertEquals("RareButRecent", result[0].coin.name)
    }
}
