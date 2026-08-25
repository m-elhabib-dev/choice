package com.choice.app.ui.main

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.choice.app.R
import com.choice.app.data.CoinRepositoryImpl
import com.choice.app.data.local.ChoiceDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainScreenDeleteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: ChoiceDatabase
    private lateinit var repository: CoinRepositoryImpl
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ChoiceDatabase::class.java).build()
        repository = CoinRepositoryImpl(database.coinDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun deleteCoin_cancelDoesNotRemoveCoin() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Breakfast", listOf("Ful", "Eggs"))
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

        // Long press on the coin to trigger delete (simulate via testTag or direct ViewModel call)
        // Since MainScreen doesn't have a direct delete button yet, we test via ViewModel directly
        viewModel.requestDelete(coinId)

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.deleteConfirmation != null }

        composeTestRule.onNodeWithText(context.getString(R.string.delete_coin_cancel)).performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.deleteConfirmation == null }

        val coins = runBlocking { repository.observeCoins().first() }
        assertEquals(1, coins.size)
        assertEquals("Breakfast", coins[0].coin.name)
    }

    @Test
    fun deleteCoin_confirmRemovesCoin() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Breakfast", listOf("Ful", "Eggs"))
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

        viewModel.requestDelete(coinId)

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.deleteConfirmation != null }

        composeTestRule.onNodeWithText(context.getString(R.string.delete_coin_confirm)).performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.deleteConfirmation == null }

        val coins = runBlocking { repository.observeCoins().first() }
        assertTrue(coins.isEmpty())
    }

    @Test
    fun deleteCoin_confirmRemovesFromMainScreen() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Breakfast", listOf("Ful", "Eggs"))
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

        composeTestRule.onNodeWithText("Breakfast").assertIsDisplayed()

        viewModel.requestDelete(coinId)
        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.deleteConfirmation != null }

        composeTestRule.onNodeWithText(context.getString(R.string.delete_coin_confirm)).performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.allCoins.isEmpty() }

        composeTestRule.onNodeWithText(context.getString(R.string.main_empty_title)).assertIsDisplayed()
    }
}
