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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainScreenQuickCoinsTest {

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
    fun quickCoins_favoritedCoinAppearsInQuickCoins() {
        runBlocking {
            val coinId = repository.saveCoin(null, "Favorite Coin", listOf("X", "Y"))
            repository.setFavorite(coinId, true)
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

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.quickCoins.isNotEmpty() }

        composeTestRule.onNodeWithText("Quick Coins").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Favorite Coin").onFirst().assertIsDisplayed()
    }

    @Test
    fun quickCoins_unfavoritedCoinRemovedFromQuickCoins() {
        runBlocking {
            val coinId = repository.saveCoin(null, "Temp Coin", listOf("X", "Y"))
            repository.setFavorite(coinId, true)
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

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.quickCoins.isNotEmpty() }

        composeTestRule.onAllNodesWithText("Temp Coin").onFirst().assertIsDisplayed()

        runBlocking {
            val coins = repository.observeQuickCoins()
            val list = coins.first()
            if (list.isNotEmpty()) {
                repository.setFavorite(list[0].coin.id, false)
            }
        }

        composeTestRule.waitUntil(5_000) {
            viewModel.uiState.value.quickCoins.none { it.name == "Temp Coin" }
        }
    }

    @Test
    fun quickCoins_hiddenWhenNoFavorites() {
        runBlocking {
            repository.saveCoin(null, "Normal Coin", listOf("X", "Y"))
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

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.allCoins.isNotEmpty() }

        composeTestRule.onNodeWithText("Quick Coins").assertDoesNotExist()
    }

    @Test
    fun quickCoins_unfavoritedCoinRemainsInFullList() {
        runBlocking {
            repository.saveCoin(null, "Stays Here", listOf("X", "Y"))
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

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.allCoins.isNotEmpty() }

        composeTestRule.onAllNodesWithText("Stays Here").onFirst().assertIsDisplayed()
    }
}
