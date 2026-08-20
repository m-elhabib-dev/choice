package com.choice.app.data

import com.choice.app.data.local.CoinDao
import com.choice.app.data.local.CoinEntity
import com.choice.app.data.local.CoinWithChoicesRelation
import com.choice.app.data.local.ChoiceEntity
import com.choice.app.data.local.DecisionEntity
import com.choice.app.domain.Decision
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinRepositoryImplHistoryTest {

    private fun fakeDao(
        decisionsForCoin: Map<Long, List<DecisionEntity>> = emptyMap(),
    ): CoinDao = object : CoinDao {
        override fun observeCoins(): Flow<List<CoinWithChoicesRelation>> = flowOf(emptyList())
        override fun observeQuickCoins(): Flow<List<CoinWithChoicesRelation>> = flowOf(emptyList())
        override fun observeCoin(coinId: Long): Flow<CoinWithChoicesRelation?> = flowOf(null)
        override suspend fun insertCoin(coin: CoinEntity): Long = coin.id
        override suspend fun insertChoices(choices: List<ChoiceEntity>) {}
        override suspend fun deleteChoicesByCoinId(coinId: Long) {}
        override suspend fun recordInteraction(coinId: Long, now: Long) {}
        override suspend fun setFavorite(coinId: Long, isFavorite: Boolean) {}
        override suspend fun deleteCoinById(coinId: Long) {}
        override suspend fun insertDecision(decision: DecisionEntity): Long = decision.id
        override fun observeDecisionsByCoinId(coinId: Long): Flow<List<DecisionEntity>> =
            flowOf(decisionsForCoin[coinId] ?: emptyList())
        override suspend fun getLastDecisionByCoinId(coinId: Long): DecisionEntity? = null
        override suspend fun updateChoicePosition(choiceId: Long, position: Int) {}
        override suspend fun setWeightedEnabled(coinId: Long, enabled: Boolean) {}
        override suspend fun setAvoidLastResultEnabled(coinId: Long, enabled: Boolean) {}
        override suspend fun updateChoiceWeight(choiceId: Long, weight: Int?) {}
    }

    @Test
    fun observeDecisionHistory_returnsNewestFirst() = runTest {
        // Fake DAO returns data in the same order as the real Room query (ORDER BY decidedAt DESC)
        val decisions = listOf(
            DecisionEntity(id = 2, coinId = 1, choiceId = 11, choiceTextSnapshot = "Ful", decidedAt = 3000L),
            DecisionEntity(id = 3, coinId = 1, choiceId = 10, choiceTextSnapshot = "Eggs", decidedAt = 2000L),
            DecisionEntity(id = 1, coinId = 1, choiceId = 10, choiceTextSnapshot = "Eggs", decidedAt = 1000L),
        )
        val repo = CoinRepositoryImpl(fakeDao(decisionsForCoin = mapOf(1L to decisions)))

        val result = repo.observeDecisionHistory(1L)
            .first()

        assertEquals(3, result.size)
        assertEquals(3000L, result[0].decidedAt)
        assertEquals(2000L, result[1].decidedAt)
        assertEquals(1000L, result[2].decidedAt)
    }

    @Test
    fun observeDecisionHistory_preservesChoiceTextSnapshot() = runTest {
        val decisions = listOf(
            DecisionEntity(id = 1, coinId = 1, choiceId = 10, choiceTextSnapshot = "Eggs", decidedAt = 1000L),
        )
        val repo = CoinRepositoryImpl(fakeDao(decisionsForCoin = mapOf(1L to decisions)))

        val result = repo.observeDecisionHistory(1L)
            .first()

        assertEquals(1, result.size)
        assertEquals("Eggs", result[0].choiceTextSnapshot)
    }

    @Test
    fun observeDecisionHistory_preservesChoiceTextAfterChoiceRemoved() = runTest {
        val decisions = listOf(
            DecisionEntity(id = 1, coinId = 1, choiceId = null, choiceTextSnapshot = "Falafel", decidedAt = 1000L),
        )
        val repo = CoinRepositoryImpl(fakeDao(decisionsForCoin = mapOf(1L to decisions)))

        val result = repo.observeDecisionHistory(1L)
            .first()

        assertEquals(1, result.size)
        assertEquals("Falafel", result[0].choiceTextSnapshot)
    }

    @Test
    fun observeDecisionHistory_returnsEmptyForCoinWithNoDecisions() = runTest {
        val repo = CoinRepositoryImpl(fakeDao())

        val result = repo.observeDecisionHistory(42L)
            .first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun observeDecisionHistory_returnsCorrectDomainObjects() = runTest {
        val decisions = listOf(
            DecisionEntity(id = 5, coinId = 1, choiceId = 10, choiceTextSnapshot = "Cheese", decidedAt = 5000L),
        )
        val repo = CoinRepositoryImpl(fakeDao(decisionsForCoin = mapOf(1L to decisions)))

        val result = repo.observeDecisionHistory(1L)
            .first()

        assertEquals(1, result.size)
        val decision = result[0]
        assertEquals(5L, decision.id)
        assertEquals(1L, decision.coinId)
        assertEquals(10L, decision.choiceId)
        assertEquals("Cheese", decision.choiceTextSnapshot)
        assertEquals(5000L, decision.decidedAt)
    }
}
