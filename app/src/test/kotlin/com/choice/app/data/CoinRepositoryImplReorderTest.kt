package com.choice.app.data

import com.choice.app.data.local.CoinDao
import com.choice.app.data.local.CoinEntity
import com.choice.app.data.local.CoinWithChoicesRelation
import com.choice.app.data.local.ChoiceEntity
import com.choice.app.data.local.DecisionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CoinRepositoryImplReorderTest {

    private var updatedPositions = mutableListOf<Pair<Long, Int>>()

    private fun fakeDao(): CoinDao = object : CoinDao {
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
        override fun observeDecisionsByCoinId(coinId: Long): Flow<List<DecisionEntity>> = flowOf(emptyList())
        override suspend fun getLastDecisionByCoinId(coinId: Long): DecisionEntity? = null
        override suspend fun updateChoicePosition(choiceId: Long, position: Int) {
            updatedPositions.add(choiceId to position)
        }
        override suspend fun setWeightedEnabled(coinId: Long, enabled: Boolean) {}
        override suspend fun setAvoidLastResultEnabled(coinId: Long, enabled: Boolean) {}
        override suspend fun updateChoiceWeight(choiceId: Long, weight: Int?) {}
    }

    @Test
    fun reorderChoices_updatesEachChoicePosition() = runTest {
        updatedPositions = mutableListOf()
        val repo = CoinRepositoryImpl(fakeDao())

        repo.reorderChoices(1L, listOf(10L, 20L, 30L))

        assertEquals(3, updatedPositions.size)
        assertEquals(10L to 0, updatedPositions[0])
        assertEquals(20L to 1, updatedPositions[1])
        assertEquals(30L to 2, updatedPositions[2])
    }

    @Test
    fun reorderChoices_appliesNewOrderSequentially() = runTest {
        updatedPositions = mutableListOf()
        val repo = CoinRepositoryImpl(fakeDao())

        repo.reorderChoices(1L, listOf(30L, 10L, 20L))

        assertEquals(3, updatedPositions.size)
        assertEquals(30L to 0, updatedPositions[0])
        assertEquals(10L to 1, updatedPositions[1])
        assertEquals(20L to 2, updatedPositions[2])
    }

    @Test
    fun reorderChoices_handlesEmptyList() = runTest {
        updatedPositions = mutableListOf()
        val repo = CoinRepositoryImpl(fakeDao())

        repo.reorderChoices(1L, emptyList())

        assertEquals(0, updatedPositions.size)
    }

    @Test
    fun reorderChoices_handlesSingleChoice() = runTest {
        updatedPositions = mutableListOf()
        val repo = CoinRepositoryImpl(fakeDao())

        repo.reorderChoices(1L, listOf(10L))

        assertEquals(1, updatedPositions.size)
        assertEquals(10L to 0, updatedPositions[0])
    }
}
