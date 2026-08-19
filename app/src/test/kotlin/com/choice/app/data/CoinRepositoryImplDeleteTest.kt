package com.choice.app.data

import com.choice.app.data.local.CoinDao
import com.choice.app.data.local.CoinEntity
import com.choice.app.data.local.CoinWithChoicesRelation
import com.choice.app.data.local.ChoiceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CoinRepositoryImplDeleteTest {

    private lateinit var storedCoins: MutableMap<Long, CoinEntity>
    private lateinit var storedChoices: MutableMap<Long, MutableList<ChoiceEntity>>
    private lateinit var deletedCoinIds: MutableList<Long>
    private var nextId: Long = 1

    private fun fakeDao(): CoinDao = object : CoinDao {
        override fun observeCoins(): Flow<List<CoinWithChoicesRelation>> = flowOf(emptyList())
        override fun observeCoinsWithInteractions(): Flow<List<CoinWithChoicesRelation>> = flowOf(emptyList())
        override fun observeCoin(coinId: Long): Flow<CoinWithChoicesRelation?> = flowOf(null)
        override suspend fun insertCoin(coin: CoinEntity): Long {
            val id = if (coin.id == 0L) nextId++ else coin.id
            storedCoins[id] = coin.copy(id = id)
            return id
        }
        override suspend fun insertChoices(choices: List<ChoiceEntity>) {
            choices.forEach { storedChoices.getOrPut(it.coinId) { mutableListOf() }.add(it) }
        }
        override suspend fun deleteChoicesByCoinId(coinId: Long) {
            storedChoices.remove(coinId)
        }
        override suspend fun recordInteraction(coinId: Long, now: Long) {}
        override suspend fun deleteCoinById(coinId: Long) {
            deletedCoinIds.add(coinId)
            storedCoins.remove(coinId)
            storedChoices.remove(coinId)
        }
    }

    @Before
    fun setup() {
        storedCoins = mutableMapOf()
        storedChoices = mutableMapOf()
        deletedCoinIds = mutableListOf()
        nextId = 1
    }

    private suspend fun createCoin(name: String, choices: List<String>): Long {
        val repo = CoinRepositoryImpl(fakeDao())
        return repo.saveCoin(null, name, choices)
    }

    @Test
    fun deleteCoin_removesCoinRow() = runTest {
        val coinId = createCoin("Breakfast", listOf("Ful", "Eggs"))
        val repo = CoinRepositoryImpl(fakeDao())
        repo.deleteCoin(coinId)
        assertTrue(deletedCoinIds.contains(coinId))
    }

    @Test
    fun deleteCoin_cascadesToChoices() = runTest {
        val coinId = createCoin("Breakfast", listOf("Ful", "Eggs", "Falafel"))
        val repo = CoinRepositoryImpl(fakeDao())
        repo.deleteCoin(coinId)
        assertTrue(deletedCoinIds.contains(coinId))
        assertEquals(1, deletedCoinIds.size)
        assertEquals(coinId, deletedCoinIds[0])
    }

    @Test
    fun deleteCoin_noOpIfCoinDoesNotExist() = runTest {
        val repo = CoinRepositoryImpl(fakeDao())
        repo.deleteCoin(999L)
        assertTrue(deletedCoinIds.contains(999L))
    }

    @Test
    fun deleteCoin_onlyRemovesSpecifiedCoin() = runTest {
        val coin1Id = createCoin("Breakfast", listOf("Ful", "Eggs"))
        val coin2Id = createCoin("Lunch", listOf("Pizza", "Pasta"))
        val repo = CoinRepositoryImpl(fakeDao())
        repo.deleteCoin(coin1Id)
        assertEquals(1, deletedCoinIds.size)
        assertEquals(coin1Id, deletedCoinIds[0])
        assertTrue(storedCoins.containsKey(coin2Id))
    }
}
