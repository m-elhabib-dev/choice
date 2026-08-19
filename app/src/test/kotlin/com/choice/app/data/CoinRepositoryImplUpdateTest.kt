package com.choice.app.data

import com.choice.app.data.local.CoinDao
import com.choice.app.data.local.CoinEntity
import com.choice.app.data.local.CoinWithChoicesRelation
import com.choice.app.data.local.ChoiceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CoinRepositoryImplUpdateTest {

    private lateinit var storedCoins: MutableMap<Long, CoinEntity>
    private lateinit var storedChoices: MutableMap<Long, MutableList<ChoiceEntity>>
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
        override suspend fun deleteCoinById(coinId: Long) {}
    }

    private suspend fun createCoin(name: String, choices: List<String>): Long {
        val repo = CoinRepositoryImpl(fakeDao())
        return repo.saveCoin(null, name, choices)
    }

    @Before
    fun setup() {
        storedCoins = mutableMapOf()
        storedChoices = mutableMapOf()
        nextId = 1
    }

    @Test
    fun update_renameCoin_persistsCorrectly() = runTest {
        val coinId = createCoin("Breakfast", listOf("Ful", "Eggs"))
        val repo = CoinRepositoryImpl(fakeDao())
        repo.saveCoin(coinId, "Lunch", listOf("Ful", "Eggs", "Falafel"))
        val coin = storedCoins[coinId]!!
        assertEquals("Lunch", coin.name)
        assertEquals(3, storedChoices[coinId]?.size)
    }

    @Test
    fun update_addChoice_persistsCorrectly() = runTest {
        val coinId = createCoin("Breakfast", listOf("Ful", "Eggs"))
        val repo = CoinRepositoryImpl(fakeDao())
        repo.saveCoin(coinId, "Breakfast", listOf("Ful", "Eggs", "Falafel"))
        val choices = storedChoices[coinId]!!
        assertEquals(3, choices.size)
        assertEquals("Falafel", choices[2].text)
    }

    @Test
    fun update_editChoiceText_persistsCorrectly() = runTest {
        val coinId = createCoin("Breakfast", listOf("Ful", "Eggs"))
        val repo = CoinRepositoryImpl(fakeDao())
        repo.saveCoin(coinId, "Breakfast", listOf("Ful", "Omelette"))
        val choices = storedChoices[coinId]!!
        assertEquals("Omelette", choices[1].text)
    }

    @Test
    fun update_removeChoice_persistsCorrectly() = runTest {
        val coinId = createCoin("Breakfast", listOf("Ful", "Eggs", "Falafel"))
        val repo = CoinRepositoryImpl(fakeDao())
        repo.saveCoin(coinId, "Breakfast", listOf("Ful", "Falafel"))
        val choices = storedChoices[coinId]!!
        assertEquals(2, choices.size)
        assertEquals("Ful", choices[0].text)
        assertEquals("Falafel", choices[1].text)
    }

    @Test
    fun update_fullCycle_replaceAllChoices() = runTest {
        val coinId = createCoin("Breakfast", listOf("Ful", "Eggs"))
        val repo = CoinRepositoryImpl(fakeDao())
        repo.saveCoin(coinId, "Dinner", listOf("Pizza", "Pasta", "Salad", "Soup"))
        val coin = storedCoins[coinId]!!
        assertEquals("Dinner", coin.name)
        val choices = storedChoices[coinId]!!
        assertEquals(4, choices.size)
        assertEquals("Pizza", choices[0].text)
        assertEquals("Soup", choices[3].text)
    }
}
