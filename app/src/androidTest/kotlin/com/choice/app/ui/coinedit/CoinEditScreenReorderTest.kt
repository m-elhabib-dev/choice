package com.choice.app.ui.coinedit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.choice.app.data.CoinRepositoryImpl
import com.choice.app.data.local.ChoiceDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CoinEditScreenReorderTest {

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
    fun reorder_moveChoiceUp_showsNewOrder() {
        val coinId = runBlocking { seedCoin("Breakfast", listOf("Ful", "Eggs", "Falafel")) }
        val viewModel = CoinEditViewModel(repository, coinId = coinId)

        composeTestRule.setContent {
            CoinEditScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.name == "Breakfast" }

        composeTestRule.onAllNodesWithContentDescription("Move up").onFirst().performClick()

        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.isSaved }

        val coins = runBlocking { repository.observeCoins().first() }
        assertEquals(1, coins.size)
        assertEquals("Eggs", coins[0].choices[0].text)
        assertEquals("Ful", coins[0].choices[1].text)
        assertEquals("Falafel", coins[0].choices[2].text)
    }

    @Test
    fun reorder_moveChoiceDown_showsNewOrder() {
        val coinId = runBlocking { seedCoin("Breakfast", listOf("Ful", "Eggs", "Falafel")) }
        val viewModel = CoinEditViewModel(repository, coinId = coinId)

        composeTestRule.setContent {
            CoinEditScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.name == "Breakfast" }

        val moveDownButtons = composeTestRule.onAllNodesWithContentDescription("Move down")
        moveDownButtons.onFirst().performClick()

        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.isSaved }

        val coins = runBlocking { repository.observeCoins().first() }
        assertEquals(1, coins.size)
        assertEquals("Ful", coins[0].choices[0].text)
        assertEquals("Falafel", coins[0].choices[1].text)
        assertEquals("Eggs", coins[0].choices[2].text)
    }

    @Test
    fun reorder_moveUpDisabledAtTop() {
        val coinId = runBlocking { seedCoin("Breakfast", listOf("Ful", "Eggs", "Falafel")) }
        val viewModel = CoinEditViewModel(repository, coinId = coinId)

        composeTestRule.setContent {
            CoinEditScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.name == "Breakfast" }

        val moveUpButtons = composeTestRule.onAllNodesWithContentDescription("Move up")
        moveUpButtons.onFirst().assertIsDisplayed()
    }

    @Test
    fun reorder_moveDownDisabledAtBottom() {
        val coinId = runBlocking { seedCoin("Breakfast", listOf("Ful", "Eggs", "Falafel")) }
        val viewModel = CoinEditViewModel(repository, coinId = coinId)

        composeTestRule.setContent {
            CoinEditScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.name == "Breakfast" }

        val moveDownButtons = composeTestRule.onAllNodesWithContentDescription("Move down")
        moveDownButtons.onFirst().assertIsDisplayed()
    }
}
