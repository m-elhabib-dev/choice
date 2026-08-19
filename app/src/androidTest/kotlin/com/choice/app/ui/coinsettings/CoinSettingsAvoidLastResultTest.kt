package com.choice.app.ui.coinsettings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

class CoinSettingsAvoidLastResultTest {

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
    fun avoidLastToggle_canBeEnabled() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Test Coin", listOf("A", "B", "C"))
        }

        val viewModel = CoinSettingsViewModel(repository, coinId)

        composeTestRule.setContent {
            CoinSettingsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.coinName.isNotEmpty() }

        composeTestRule.onNodeWithTag("avoidLastToggle").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.avoidLastResultEnabled }
    }

    @Test
    fun avoidLastResult_persistsAfterSave() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Test Coin", listOf("A", "B", "C"))
        }

        val viewModel = CoinSettingsViewModel(repository, coinId)

        composeTestRule.setContent {
            CoinSettingsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.coinName.isNotEmpty() }

        composeTestRule.onNodeWithTag("avoidLastToggle").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.avoidLastResultEnabled }

        composeTestRule.onNodeWithText("Save Settings").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.isSaved }

        val coin = runBlocking {
            repository.observeCoin(coinId).first()?.coin
        }
        assert(coin?.avoidLastResultEnabled == true) {
            "Expected avoidLastResultEnabled=true but got ${coin?.avoidLastResultEnabled}"
        }
    }
}
