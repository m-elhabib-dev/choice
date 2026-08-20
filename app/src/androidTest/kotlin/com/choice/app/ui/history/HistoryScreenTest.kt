package com.choice.app.ui.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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

class HistoryScreenTest {

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
    fun historyScreen_showsEmptyStateWhenNoDecisions() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Breakfast", listOf("Ful", "Eggs"))
        }

        val viewModel = HistoryViewModel(repository, coinId)

        composeTestRule.setContent {
            HistoryScreen(
                viewModel = viewModel,
                onBack = {},
                onStatistics = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.isEmpty }

        composeTestRule.onNodeWithText("No decisions yet").assertIsDisplayed()
    }

    @Test
    fun historyScreen_showsDecisionsNewestFirst() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Breakfast", listOf("Ful", "Eggs"))
        }

        runBlocking {
            repository.recordDecision(coinId, 1L, "Ful")
            Thread.sleep(10)
            repository.recordDecision(coinId, 2L, "Eggs")
        }

        val viewModel = HistoryViewModel(repository, coinId)

        composeTestRule.setContent {
            HistoryScreen(
                viewModel = viewModel,
                onBack = {},
                onStatistics = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.decisions.size >= 2 }

        val decisions = viewModel.uiState.value.decisions
        assert(decisions.size == 2) { "Expected 2 decisions, got ${decisions.size}" }
        assert(decisions[0].choiceTextSnapshot == "Eggs") {
            "Expected newest first (Eggs), got ${decisions[0].choiceTextSnapshot}"
        }
        assert(decisions[1].choiceTextSnapshot == "Ful") {
            "Expected oldest second (Ful), got ${decisions[1].choiceTextSnapshot}"
        }
    }

    @Test
    fun historyScreen_displaysChoiceTextAndTimestamp() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Breakfast", listOf("Ful", "Eggs"))
        }

        runBlocking {
            repository.recordDecision(coinId, 1L, "Falafel")
        }

        val viewModel = HistoryViewModel(repository, coinId)

        composeTestRule.setContent {
            HistoryScreen(
                viewModel = viewModel,
                onBack = {},
                onStatistics = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.decisions.isNotEmpty() }

        composeTestRule.onNodeWithText("Falafel").assertIsDisplayed()
    }

    @Test
    fun historyScreen_hidesEmptyStateWhenDecisionsExist() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Breakfast", listOf("Ful", "Eggs"))
        }

        runBlocking {
            repository.recordDecision(coinId, 1L, "Eggs")
        }

        val viewModel = HistoryViewModel(repository, coinId)

        composeTestRule.setContent {
            HistoryScreen(
                viewModel = viewModel,
                onBack = {},
                onStatistics = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.decisions.isNotEmpty() }

        composeTestRule.onNodeWithText("No decisions yet").assertDoesNotExist()
    }

    @Test
    fun historyScreen_displaysHistoryTitle() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Breakfast", listOf("Ful", "Eggs"))
        }

        val viewModel = HistoryViewModel(repository, coinId)

        composeTestRule.setContent {
            HistoryScreen(
                viewModel = viewModel,
                onBack = {},
                onStatistics = {},
            )
        }

        composeTestRule.onNodeWithText("History").assertIsDisplayed()
    }
}
