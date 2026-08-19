package com.choice.app.ui.coinedit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

class CoinEditScreenEditTest {

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

    private suspend fun seedCoin(name: String, choices: List<String>): Long {
        return repository.saveCoin(null, name, choices)
    }

    @Test
    fun editCoin_endToEnd_renameAndChangeChoices() {
        val coinId = runBlocking { seedCoin("Breakfast", listOf("Ful", "Eggs", "Falafel")) }
        val viewModel = CoinEditViewModel(repository, coinId = coinId)

        composeTestRule.setContent {
            CoinEditScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.name == "Breakfast" }

        composeTestRule.onNodeWithTag("name").performClick()
        composeTestRule.onNodeWithTag("name").performTextInput("Lunch")

        composeTestRule.onAllNodesWithTag("choice").onFirst().performClick()
        composeTestRule.onAllNodesWithTag("choice").onFirst().performTextInput("Pizza")

        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.isSaved }

        val coins = runBlocking { repository.observeCoins().first() }
        assertEquals(1, coins.size)
        assertEquals("Lunch", coins[0].coin.name)
    }

    @Test
    fun editCoin_removeChoice_persistsChanges() {
        val coinId = runBlocking { seedCoin("Breakfast", listOf("Ful", "Eggs", "Falafel")) }
        val viewModel = CoinEditViewModel(repository, coinId = coinId)

        composeTestRule.setContent {
            CoinEditScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.name == "Breakfast" }

        composeTestRule.onAllNodesWithContentDescription("Remove choice").onFirst().performClick()

        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.isSaved }

        val coins = runBlocking { repository.observeCoins().first() }
        assertEquals(1, coins.size)
        assertEquals(2, coins[0].choices.size)
    }

    @Test
    fun editCoin_removingChoiceFrom2ChoiceCoin_showsError() {
        val coinId = runBlocking { seedCoin("Breakfast", listOf("Ful", "Eggs")) }
        val viewModel = CoinEditViewModel(repository, coinId = coinId)

        composeTestRule.setContent {
            CoinEditScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.name == "Breakfast" }

        composeTestRule.onAllNodesWithContentDescription("Remove choice").onFirst().performClick()

        composeTestRule.onNodeWithText("At least 2 choices are required").assertIsDisplayed()

        val coins = runBlocking { repository.observeCoins().first() }
        assertEquals(2, coins[0].choices.size)
    }
}
