package com.choice.app.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.choice.app.data.CoinRepositoryImpl
import com.choice.app.data.local.ChoiceDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainScreenQuickAccessTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: ChoiceDatabase
    private lateinit var repository: CoinRepositoryImpl

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChoiceDatabase::class.java,
        ).build()
        repository = CoinRepositoryImpl(database.coinDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun quickAccess_showsTopRankedCoins() {
        runBlocking {
            val coinAId = repository.saveCoin(null, "Coin A", listOf("X", "Y"))
            val coinBId = repository.saveCoin(null, "Coin B", listOf("Z", "W"))
            val coinCId = repository.saveCoin(null, "Coin C", listOf("P", "Q"))
            repository.recordInteraction(coinAId)
            repository.recordInteraction(coinAId)
            repository.recordInteraction(coinAId)
            repository.recordInteraction(coinBId)
            repository.recordInteraction(coinBId)
            repository.recordInteraction(coinCId)
        }

        val viewModel = MainViewModel(repository)

        composeTestRule.setContent {
            MainScreen(
                viewModel = viewModel,
                onCoinClick = {},
                onCreateCoin = {},
                onEditCoin = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.quickAccessCoins.isNotEmpty() }

        composeTestRule.onNodeWithText("Quick Access").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Coin A").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Coin B").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Coin C").onFirst().assertIsDisplayed()
    }

    @Test
    fun quickAccess_topThreeVisibleWithoutScrolling() {
        runBlocking {
            val coin1Id = repository.saveCoin(null, "Coin 1", listOf("A", "B"))
            val coin2Id = repository.saveCoin(null, "Coin 2", listOf("C", "D"))
            val coin3Id = repository.saveCoin(null, "Coin 3", listOf("E", "F"))
            listOf(coin1Id, coin2Id, coin3Id).forEach { id ->
                repeat(3) { repository.recordInteraction(id) }
            }
        }

        val viewModel = MainViewModel(repository)

        composeTestRule.setContent {
            MainScreen(
                viewModel = viewModel,
                onCoinClick = {},
                onCreateCoin = {},
                onEditCoin = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.quickAccessCoins.size >= 3 }

        composeTestRule.onAllNodesWithText("Coin 1").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Coin 2").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Coin 3").onFirst().assertIsDisplayed()
    }
}
