package com.choice.app.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.choice.app.data.CoinRepositoryImpl
import com.choice.app.data.local.ChoiceDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainScreenSearchTest {

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
    fun search_typingNarrowsList() {
        runBlocking {
            repository.saveCoin(null, "Breakfast", listOf("Ful", "Eggs"))
            repository.saveCoin(null, "Lunch", listOf("Rice", "Pasta"))
            repository.saveCoin(null, "Dinner", listOf("Chicken", "Beef"))
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

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.allCoins.size >= 3 }

        composeTestRule.onNodeWithText("Search coins...").performClick()
        composeTestRule.onNodeWithText("Search coins...").performTextInput("Break")

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.filteredAllCoins.size == 1 }

        composeTestRule.onNodeWithText("Breakfast").assertIsDisplayed()
    }

    @Test
    fun search_clearingRestoresFullList() {
        runBlocking {
            repository.saveCoin(null, "Breakfast", listOf("Ful", "Eggs"))
            repository.saveCoin(null, "Lunch", listOf("Rice", "Pasta"))
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

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.allCoins.size >= 2 }

        composeTestRule.onNodeWithText("Search coins...").performClick()
        composeTestRule.onNodeWithText("Search coins...").performTextInput("Break")

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.filteredAllCoins.size == 1 }

        composeTestRule.onNodeWithText("Search coins...").performClick()
        composeTestRule.onNodeWithText("Search coins...").performTextInput("")

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.filteredAllCoins.size == 2 }

        composeTestRule.onNodeWithText("Breakfast").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lunch").assertIsDisplayed()
    }

    @Test
    fun search_noMatch_showsNoResultsState() {
        runBlocking {
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

        composeTestRule.onNodeWithText("Search coins...").performClick()
        composeTestRule.onNodeWithText("Search coins...").performTextInput("XYZ123")

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.isNoResults }

        composeTestRule.onNodeWithText("No matching coins").assertIsDisplayed()
    }
}
