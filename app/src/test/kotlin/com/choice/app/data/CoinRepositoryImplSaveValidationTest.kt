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
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinRepositoryImplSaveValidationTest {

    private fun fakeDao(): CoinDao = object : CoinDao {
        override fun observeCoins(): Flow<List<CoinWithChoicesRelation>> = flowOf(emptyList())

        override fun observeQuickCoins(): Flow<List<CoinWithChoicesRelation>> =
            flowOf(emptyList())

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

            override suspend fun updateChoicePosition(choiceId: Long, position: Int) {}
            override suspend fun setWeightedEnabled(coinId: Long, enabled: Boolean) {}
            override suspend fun setAvoidLastResultEnabled(coinId: Long, enabled: Boolean) {}
            override suspend fun updateChoiceWeight(choiceId: Long, weight: Int?) {}
        }

    private val validChoices = listOf("Ful", "Eggs")

    private suspend fun expectIllegalArgumentException(block: suspend () -> Unit): IllegalArgumentException {
        try {
            block()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            return e
        }
    }

    @Test
    fun saveCoin_rejectsBlankName() = runTest {
        val repo = CoinRepositoryImpl(fakeDao())
        val exception = expectIllegalArgumentException {
            repo.saveCoin(null, "   ", validChoices)
        }
        assertTrue(exception.message!!.contains("name", ignoreCase = true))
    }

    @Test
    fun saveCoin_rejectsBlankChoice() = runTest {
        val repo = CoinRepositoryImpl(fakeDao())
        val exception = expectIllegalArgumentException {
            repo.saveCoin(null, "Breakfast", listOf("Ful", "  "))
        }
        assertTrue(exception.message!!.contains("choice", ignoreCase = true))
    }

    @Test
    fun saveCoin_rejectsNameOver40Chars() = runTest {
        val repo = CoinRepositoryImpl(fakeDao())
        val longName = "x".repeat(41)
        val exception = expectIllegalArgumentException {
            repo.saveCoin(null, longName, validChoices)
        }
        assertTrue(exception.message!!.contains("40"))
    }

    @Test
    fun saveCoin_rejectsChoiceOver60Chars() = runTest {
        val repo = CoinRepositoryImpl(fakeDao())
        val longChoice = "x".repeat(61)
        val exception = expectIllegalArgumentException {
            repo.saveCoin(null, "Breakfast", listOf("Ful", longChoice))
        }
        assertTrue(exception.message!!.contains("60"))
    }

    @Test
    fun saveCoin_rejectsFewerThan2Choices() = runTest {
        val repo = CoinRepositoryImpl(fakeDao())
        val exception = expectIllegalArgumentException {
            repo.saveCoin(null, "Breakfast", listOf("Ful"))
        }
        assertTrue(exception.message!!.contains("2"))
    }

    @Test
    fun saveCoin_acceptsValidInput() = runTest {
        val repo = CoinRepositoryImpl(fakeDao())
        val id = repo.saveCoin(null, "Breakfast", listOf("Ful", "Eggs", "Falafel"))
        assertEquals(0L, id)
    }

    @Test
    fun saveCoin_acceptsNameExactly40Chars() = runTest {
        val repo = CoinRepositoryImpl(fakeDao())
        val name = "x".repeat(40)
        repo.saveCoin(null, name, validChoices)
    }

    @Test
    fun saveCoin_acceptsChoiceExactly60Chars() = runTest {
        val repo = CoinRepositoryImpl(fakeDao())
        val choice = "x".repeat(60)
        repo.saveCoin(null, "Breakfast", listOf("Ful", choice))
    }
}
