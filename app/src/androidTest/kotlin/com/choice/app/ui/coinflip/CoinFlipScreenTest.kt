package com.choice.app.ui.coinflip

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

class CoinFlipScreenTest {

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
    fun flipCoin_displaysExactlyOneChoiceAfterFlip() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Breakfast", listOf("Ful", "Eggs", "Falafel", "Cheese"))
        }
        val choiceTexts = runBlocking {
            repository.observeCoin(coinId).first()!!.choices.map { it.text }
        }

        val viewModel = CoinFlipViewModel(repository, coinId)

        composeTestRule.setContent {
            CoinFlipScreen(
                viewModel = viewModel,
                onBack = {},
                onEditCoin = {},
                onSettings = {},
                onHistory = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.coinName.isNotEmpty() }

        composeTestRule.onNodeWithText("Flip").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.result != null }

        val displayedChoices = choiceTexts.filter { text ->
            try {
                composeTestRule.onNodeWithText(text).fetchSemanticsNode()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        assert(displayedChoices.size == 1) {
            "Expected exactly one choice displayed, but found ${displayedChoices.size}: $displayedChoices"
        }
    }

    @Test
    fun flipCoin_doesNotShowReFlipControl() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Breakfast", listOf("Ful", "Eggs", "Falafel", "Cheese"))
        }

        val viewModel = CoinFlipViewModel(repository, coinId)

        composeTestRule.setContent {
            CoinFlipScreen(
                viewModel = viewModel,
                onBack = {},
                onEditCoin = {},
                onSettings = {},
                onHistory = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.coinName.isNotEmpty() }

        composeTestRule.onNodeWithText("Flip").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.result != null }

        composeTestRule.onNodeWithText("Flip Again").assertDoesNotExist()
        composeTestRule.onNodeWithText("Flip").assertDoesNotExist()
    }

    @Test
    fun flipCoin_displaysCoinName() {
        val coinId = runBlocking { repository.saveCoin(null, "Breakfast", listOf("Ful", "Eggs")) }

        val viewModel = CoinFlipViewModel(repository, coinId)

        composeTestRule.setContent {
            CoinFlipScreen(
                viewModel = viewModel,
                onBack = {},
                onEditCoin = {},
                onSettings = {},
                onHistory = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.coinName.isNotEmpty() }

        composeTestRule.onNodeWithText("Breakfast").assertIsDisplayed()
    }
}
