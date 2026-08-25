package com.choice.app.ui.coinflip

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
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CoinFlipScreenAcceptOverrideTest {

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
    fun resultScreen_showsOneEmphasizedChoice() {
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

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_flip)).performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.result != null }

        val resultText = viewModel.uiState.value.result!!
        composeTestRule.onNodeWithText(resultText).assertIsDisplayed()
    }

    @Test
    fun resultScreen_showsPrimaryAcceptButton() {
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

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_flip)).performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.result != null }

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_accept)).assertIsDisplayed()
    }

    @Test
    fun resultScreen_showsSecondaryFlipAgainButton() {
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

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_flip)).performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.result != null }

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_flip_again)).assertIsDisplayed()
    }

    @Test
    fun acceptButton_recordsDecisionAndShowsAccepted() {
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

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_flip)).performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.result != null }

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_accept)).performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.accepted }

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_decision_accepted)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_accept)).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_flip_again)).assertDoesNotExist()
    }

    @Test
    fun flipAgain_discardsResultAndFlipAgain() {
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

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_flip)).performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.result != null }

        val firstResult = viewModel.uiState.value.result!!

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_flip_again)).performClick()

        composeTestRule.waitUntil(5_000) {
            viewModel.uiState.value.result != null && viewModel.uiState.value.result != firstResult
                    || viewModel.uiState.value.result == null
        }

        // The result is either different or the ViewModel is in the process of flipping again
        // Either way, the Accept/Flip again buttons should be visible (not accepted)
        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.result != null }

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_accept)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_flip_again)).assertIsDisplayed()
    }

    @Test
    fun flipAgain_doesNotRecordDecision() {
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

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_flip)).performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.result != null }

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_flip_again)).performClick()

        composeTestRule.waitUntil(5_000) { !viewModel.uiState.value.accepted }

        // Verify no decision was recorded (accepted is false, meaning accept was not called)
        assert(!viewModel.uiState.value.accepted) {
            "Expected no decision to be recorded on flip again"
        }
    }

    @Test
    fun flipButton_notVisibleAfterFlip() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Breakfast", listOf("Ful", "Eggs"))
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

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_flip)).performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.result != null }

        composeTestRule.onNodeWithText(context.getString(R.string.coin_flip_action_flip)).assertDoesNotExist()
    }
}
