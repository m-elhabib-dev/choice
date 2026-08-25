package com.choice.app.ui.coinsettings

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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

class CoinSettingsWeightingTest {

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
    fun weightingToggle_canBeEnabled() {
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

        composeTestRule.onNodeWithTag("weightedToggle").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.weightedEnabled }

        composeTestRule.onNodeWithText(context.getString(R.string.coin_settings_choice_weights_title)).assertIsDisplayed()
    }

    @Test
    fun weightingToggle_showsWeightInputsWhenEnabled() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Test Coin", listOf("A", "B"))
        }

        val viewModel = CoinSettingsViewModel(repository, coinId)

        composeTestRule.setContent {
            CoinSettingsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.coinName.isNotEmpty() }

        composeTestRule.onNodeWithTag("weightedToggle").performClick()

        composeTestRule.waitUntil(5_000) {
            viewModel.uiState.value.weightedEnabled
        }

        composeTestRule.onNodeWithTag("weight_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("weight_1").assertIsDisplayed()
    }

    @Test
    fun invalidWeight_blocksSaveWithError() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Test Coin", listOf("A", "B"))
        }

        val viewModel = CoinSettingsViewModel(repository, coinId)

        composeTestRule.setContent {
            CoinSettingsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.coinName.isNotEmpty() }

        composeTestRule.onNodeWithTag("weightedToggle").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.weightedEnabled }

        composeTestRule.onNodeWithTag("weight_0").performTextInput("0")

        composeTestRule.onNodeWithText(context.getString(R.string.coin_settings_save)).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.coin_settings_error_weight_positive)).assertIsDisplayed()
    }

    @Test
    fun negativeWeight_blocksSaveWithError() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Test Coin", listOf("A", "B"))
        }

        val viewModel = CoinSettingsViewModel(repository, coinId)

        composeTestRule.setContent {
            CoinSettingsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.coinName.isNotEmpty() }

        composeTestRule.onNodeWithTag("weightedToggle").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.weightedEnabled }

        composeTestRule.onNodeWithTag("weight_0").performTextInput("-5")

        composeTestRule.onNodeWithText(context.getString(R.string.coin_settings_save)).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.coin_settings_error_weight_positive)).assertIsDisplayed()
    }

    @Test
    fun validWeight_succeeds() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Test Coin", listOf("A", "B"))
        }

        val viewModel = CoinSettingsViewModel(repository, coinId)

        composeTestRule.setContent {
            CoinSettingsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.coinName.isNotEmpty() }

        composeTestRule.onNodeWithTag("weightedToggle").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.weightedEnabled }

        composeTestRule.onNodeWithTag("weight_0").performTextInput("8")
        composeTestRule.onNodeWithTag("weight_1").performTextInput("1")

        composeTestRule.onNodeWithText(context.getString(R.string.coin_settings_save)).performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.isSaved }
    }

    @Test
    fun blankWeight_defaultsToNull() {
        val coinId = runBlocking {
            repository.saveCoin(null, "Test Coin", listOf("A", "B"))
        }

        val viewModel = CoinSettingsViewModel(repository, coinId)

        composeTestRule.setContent {
            CoinSettingsScreen(
                viewModel = viewModel,
                onBack = {},
            )
        }

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.coinName.isNotEmpty() }

        composeTestRule.onNodeWithTag("weightedToggle").performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.weightedEnabled }

        composeTestRule.onNodeWithTag("weight_0").performTextInput("5")

        composeTestRule.onNodeWithText(context.getString(R.string.coin_settings_save)).performClick()

        composeTestRule.waitUntil(5_000) { viewModel.uiState.value.isSaved }

        val savedWeights = runBlocking {
            repository.observeCoin(coinId).first()?.choices?.map { it.weight }
        }
        assert(savedWeights == listOf(5, null)) { "Expected [5, null] but got $savedWeights" }
    }
}
