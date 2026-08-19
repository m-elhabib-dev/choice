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

class CoinEditScreenCreateTest {

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
    fun createCoin_endToEnd_coinAppearsOnMainScreen() {
        var savedNavigatedBack = false
        val viewModel = CoinEditViewModel(repository, coinId = null)

        composeTestRule.setContent {
            CoinEditScreen(
                viewModel = viewModel,
                onBack = { savedNavigatedBack = true },
            )
        }

        composeTestRule.onNodeWithTag("name").performTextInput("Breakfast")
        composeTestRule.onAllNodesWithTag("choice").onFirst().performTextInput("Ful")
        composeTestRule.onAllNodesWithTag("choice")[1].performTextInput("Eggs")

        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.isSaved }

        val coins = runBlocking { repository.observeCoins().first() }
        assertEquals(1, coins.size)
        assertEquals("Breakfast", coins[0].coin.name)
        assertEquals(2, coins[0].choices.size)
        assertTrue(savedNavigatedBack)
    }

    @Test
    fun createCoin_blockedWhenFewerThan2Choices() {
        val viewModel = CoinEditViewModel(repository, coinId = null)

        composeTestRule.setContent {
            CoinEditScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.onNodeWithTag("name").performTextInput("Breakfast")
        composeTestRule.onAllNodesWithTag("choice").onFirst().performTextInput("Ful")

        // Remove the second choice, leaving only 1
        composeTestRule.onAllNodesWithContentDescription("Remove choice")[1].performClick()

        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.formError != null }

        composeTestRule.onNodeWithText("At least 2 choices are required").assertIsDisplayed()

        val coins = runBlocking { repository.observeCoins().first() }
        assertEquals(0, coins.size)
    }

    @Test
    fun createCoin_blockedWhenNameIsBlank() {
        val viewModel = CoinEditViewModel(repository, coinId = null)

        composeTestRule.setContent {
            CoinEditScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.onAllNodesWithTag("choice").onFirst().performTextInput("Ful")
        composeTestRule.onAllNodesWithTag("choice")[1].performTextInput("Eggs")

        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.nameError != null }

        composeTestRule.onNodeWithText("Name cannot be blank").assertIsDisplayed()

        val coins = runBlocking { repository.observeCoins().first() }
        assertEquals(0, coins.size)
    }

    @Test
    fun createCoin_blockedWhenChoiceIsBlank() {
        val viewModel = CoinEditViewModel(repository, coinId = null)

        composeTestRule.setContent {
            CoinEditScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.onNodeWithTag("name").performTextInput("Breakfast")
        composeTestRule.onAllNodesWithTag("choice").onFirst().performTextInput("Ful")
        // Leave second choice blank

        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.choices.any { it.error != null } }

        composeTestRule.onNodeWithText("Choice cannot be blank").assertIsDisplayed()

        val coins = runBlocking { repository.observeCoins().first() }
        assertEquals(0, coins.size)
    }
}
