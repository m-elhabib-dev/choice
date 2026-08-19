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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinRepositoryImplRecordDecisionTest {

    private var insertedDecisions = mutableListOf<DecisionEntity>()

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
        override suspend fun insertDecision(decision: DecisionEntity): Long {
            insertedDecisions.add(decision)
            return decision.id
        }
        override fun observeDecisionsByCoinId(coinId: Long): Flow<List<DecisionEntity>> = flowOf(emptyList())
        override suspend fun getLastDecisionByCoinId(coinId: Long): DecisionEntity? = null
            override suspend fun updateChoicePosition(choiceId: Long, position: Int) {}
            override suspend fun setWeightedEnabled(coinId: Long, enabled: Boolean) {}
            override suspend fun setAvoidLastResultEnabled(coinId: Long, enabled: Boolean) {}
            override suspend fun updateChoiceWeight(choiceId: Long, weight: Int?) {}
        }

    @Test
    fun recordDecision_insertsExactlyOneDecision() = runTest {
        insertedDecisions = mutableListOf()
        val repo = CoinRepositoryImpl(fakeDao())

        repo.recordDecision(1L, 10L, "Eggs")

        assertEquals(1, insertedDecisions.size)
    }

    @Test
    fun recordDecision_storesCorrectChoiceTextSnapshot() = runTest {
        insertedDecisions = mutableListOf()
        val repo = CoinRepositoryImpl(fakeDao())

        repo.recordDecision(1L, 10L, "Falafel")

        assertEquals("Falafel", insertedDecisions[0].choiceTextSnapshot)
    }

    @Test
    fun recordDecision_storesCorrectCoinId() = runTest {
        insertedDecisions = mutableListOf()
        val repo = CoinRepositoryImpl(fakeDao())

        repo.recordDecision(42L, 10L, "Eggs")

        assertEquals(42L, insertedDecisions[0].coinId)
    }

    @Test
    fun recordDecision_storesCorrectChoiceId() = runTest {
        insertedDecisions = mutableListOf()
        val repo = CoinRepositoryImpl(fakeDao())

        repo.recordDecision(1L, 99L, "Eggs")

        assertEquals(99L, insertedDecisions[0].choiceId)
    }

    @Test
    fun recordDecision_setsDecidedAtTimestamp() = runTest {
        insertedDecisions = mutableListOf()
        val repo = CoinRepositoryImpl(fakeDao())
        val before = System.currentTimeMillis()

        repo.recordDecision(1L, 10L, "Eggs")
        val after = System.currentTimeMillis()

        val decidedAt = insertedDecisions[0].decidedAt
        assertTrue("decidedAt ($decidedAt) should be >= before ($before)", decidedAt >= before)
        assertTrue("decidedAt ($decidedAt) should be <= after ($after)", decidedAt <= after)
    }

    @Test
    fun recordDecision_multipleCallsInsertMultipleDecisions() = runTest {
        insertedDecisions = mutableListOf()
        val repo = CoinRepositoryImpl(fakeDao())

        repo.recordDecision(1L, 10L, "Eggs")
        repo.recordDecision(1L, 11L, "Ful")
        repo.recordDecision(1L, 10L, "Eggs")

        assertEquals(3, insertedDecisions.size)
        assertEquals("Eggs", insertedDecisions[0].choiceTextSnapshot)
        assertEquals("Ful", insertedDecisions[1].choiceTextSnapshot)
        assertEquals("Eggs", insertedDecisions[2].choiceTextSnapshot)
    }
}
