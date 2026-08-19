package com.choice.app.data

import com.choice.app.data.local.CoinDao
import com.choice.app.data.local.CoinEntity
import com.choice.app.data.local.CoinWithChoicesRelation
import com.choice.app.data.local.ChoiceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinRepositoryImplQuickAccessTest {

    private fun fakeDao(coins: List<CoinWithChoicesRelation>): CoinDao =
        object : CoinDao {
            override fun observeCoins(): Flow<List<CoinWithChoicesRelation>> = flowOf(coins)

            override fun observeCoinsWithInteractions(): Flow<List<CoinWithChoicesRelation>> =
                flowOf(coins.filter { it.coin.interactionCount > 0 })

            override fun observeCoin(coinId: Long): Flow<CoinWithChoicesRelation?> =
                flowOf(coins.firstOrNull { it.coin.id == coinId })

            override suspend fun insertCoin(coin: CoinEntity): Long = coin.id

            override suspend fun insertChoices(choices: List<ChoiceEntity>) {}

            override suspend fun deleteChoicesByCoinId(coinId: Long) {}

            override suspend fun recordInteraction(coinId: Long, now: Long) {}

            override suspend fun deleteCoinById(coinId: Long) {}
        }

    private fun coin(
        id: Long,
        name: String,
        interactionCount: Int,
        lastInteractionAt: Long?,
    ): CoinWithChoicesRelation =
        CoinWithChoicesRelation(
            coin = CoinEntity(
                id = id,
                name = name,
                interactionCount = interactionCount,
                lastInteractionAt = lastInteractionAt,
            ),
            choices = listOf(
                ChoiceEntity(coinId = id, text = "A", position = 0),
                ChoiceEntity(coinId = id, text = "B", position = 1),
            ),
        )

    @Test
    fun quickAccess_excludesCoinsWithZeroInteractions() = runTest {
        val now = System.currentTimeMillis()
        val dao = fakeDao(
            listOf(
                coin(1, "No Interactions", 0, null),
                coin(2, "With Interactions", 3, now),
            ),
        )
        val repo = CoinRepositoryImpl(dao)

        val result = repo.observeQuickAccessCoins().first()

        assertTrue(result.none { it.coin.name == "No Interactions" })
        assertTrue(result.any { it.coin.name == "With Interactions" })
    }

    @Test
    fun quickAccess_ordersByScoreDescending() = runTest {
        val now = System.currentTimeMillis()
        val dao = fakeDao(
            listOf(
                coin(1, "Coin A", 3, now),
                coin(2, "Coin B", 1, now),
            ),
        )
        val repo = CoinRepositoryImpl(dao)

        val result = repo.observeQuickAccessCoins().first()

        assertEquals(2, result.size)
        assertEquals("Coin A", result[0].coin.name)
        assertEquals("Coin B", result[1].coin.name)
    }

    @Test
    fun quickAccess_respectsLimit() = runTest {
        val now = System.currentTimeMillis()
        val dao = fakeDao(
            listOf(
                coin(1, "Coin 1", 5, now),
                coin(2, "Coin 2", 4, now),
                coin(3, "Coin 3", 3, now),
            ),
        )
        val repo = CoinRepositoryImpl(dao)

        val result = repo.observeQuickAccessCoins(limit = 2).first()

        assertEquals(2, result.size)
    }

    @Test
    fun quickAccess_recencyBeatsFrequency() = runTest {
        val msPerHour = 1000L * 60 * 60
        val now = System.currentTimeMillis()
        // 10 interactions 300h ago: 10 * 2^(-300/72) ≈ 0.54
        //  1 interaction now:       1  * 2^0          = 1.0
        val dao = fakeDao(
            listOf(
                coin(1, "FrequentButOld", 10, now - 300 * msPerHour),
                coin(2, "RareButRecent", 1, now),
            ),
        )
        val repo = CoinRepositoryImpl(dao)

        val result = repo.observeQuickAccessCoins().first()

        assertEquals("RareButRecent", result[0].coin.name)
    }
}
