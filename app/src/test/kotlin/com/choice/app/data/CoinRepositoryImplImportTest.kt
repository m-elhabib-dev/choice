package com.choice.app.data

import com.choice.app.data.local.CoinDao
import com.choice.app.data.local.CoinEntity
import com.choice.app.data.local.CoinWithChoicesRelation
import com.choice.app.data.local.ChoiceEntity
import com.choice.app.data.local.DecisionEntity
import com.choice.app.domain.SharedCoin
import com.choice.app.domain.SharedChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinRepositoryImplImportTest {

    private var insertedCoins = mutableListOf<CoinEntity>()
    private var insertedChoices = mutableListOf<ChoiceEntity>()

    private fun fakeDao(): CoinDao = object : CoinDao {
        override fun observeCoins(): Flow<List<CoinWithChoicesRelation>> = flowOf(emptyList())
        override fun observeQuickCoins(): Flow<List<CoinWithChoicesRelation>> = flowOf(emptyList())
        override fun observeCoin(coinId: Long): Flow<CoinWithChoicesRelation?> = flowOf(null)
        override suspend fun insertCoin(coin: CoinEntity): Long {
            insertedCoins.add(coin)
            return insertedCoins.size.toLong()
        }
        override suspend fun insertChoices(choices: List<ChoiceEntity>) {
            insertedChoices.addAll(choices)
        }
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

    @Test
    fun importSharedCoin_insertsNewCoin() = runTest {
        insertedCoins = mutableListOf()
        insertedChoices = mutableListOf()
        val repo = CoinRepositoryImpl(fakeDao())

        val payload = SharedCoin(
            schemaVersion = 1,
            name = "Breakfast",
            choices = listOf(
                SharedChoice(text = "Ful", weight = null),
                SharedChoice(text = "Eggs", weight = 2),
            ),
            weightedEnabled = true,
            avoidLastResultEnabled = false,
        )

        val coinId = repo.importSharedCoin(payload)

        assertEquals(1, insertedCoins.size)
        assertEquals("Breakfast", insertedCoins[0].name)
        assertEquals(true, insertedCoins[0].weightedEnabled)
        assertEquals(false, insertedCoins[0].avoidLastResultEnabled)
        assertEquals(2, insertedChoices.size)
        assertEquals("Ful", insertedChoices[0].text)
        assertEquals(null, insertedChoices[0].weight)
        assertEquals("Eggs", insertedChoices[1].text)
        assertEquals(2, insertedChoices[1].weight)
    }

    @Test
    fun importSharedCoin_alwaysCreatesNewCoin() = runTest {
        insertedCoins = mutableListOf()
        insertedChoices = mutableListOf()
        val repo = CoinRepositoryImpl(fakeDao())

        val payload = SharedCoin(
            schemaVersion = 1,
            name = "Breakfast",
            choices = listOf(
                SharedChoice(text = "Ful", weight = null),
                SharedChoice(text = "Eggs", weight = null),
            ),
            weightedEnabled = false,
            avoidLastResultEnabled = false,
        )

        repo.importSharedCoin(payload)
        repo.importSharedCoin(payload)

        assertEquals(2, insertedCoins.size)
        assertEquals(4, insertedChoices.size)
    }

    @Test
    fun importSharedCoin_setsIsFavoriteFalse() = runTest {
        insertedCoins = mutableListOf()
        insertedChoices = mutableListOf()
        val repo = CoinRepositoryImpl(fakeDao())

        val payload = SharedCoin(
            schemaVersion = 1,
            name = "Lunch",
            choices = listOf(
                SharedChoice(text = "Rice", weight = null),
                SharedChoice(text = "Pasta", weight = null),
            ),
            weightedEnabled = false,
            avoidLastResultEnabled = false,
        )

        repo.importSharedCoin(payload)

        assertEquals(false, insertedCoins[0].isFavorite)
    }

    @Test
    fun importSharedCoin_preservesWeightedAndAvoidLastSettings() = runTest {
        insertedCoins = mutableListOf()
        insertedChoices = mutableListOf()
        val repo = CoinRepositoryImpl(fakeDao())

        val payload = SharedCoin(
            schemaVersion = 1,
            name = "Workout",
            choices = listOf(
                SharedChoice(text = "Chest", weight = 3),
                SharedChoice(text = "Back", weight = 1),
            ),
            weightedEnabled = true,
            avoidLastResultEnabled = true,
        )

        repo.importSharedCoin(payload)

        assertEquals(true, insertedCoins[0].weightedEnabled)
        assertEquals(true, insertedCoins[0].avoidLastResultEnabled)
    }
}
