package com.choice.app.ui.share

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.choice.app.R
import com.choice.app.data.CoinRepository
import com.choice.app.domain.CoinWithChoices
import com.choice.app.domain.SharedCoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class ImportCoinScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private val fakeRepository = object : CoinRepository {
        var importedPayload: SharedCoin? = null

        override fun observeCoins(): Flow<List<CoinWithChoices>> = flowOf(emptyList())
        override fun observeQuickCoins(): Flow<List<CoinWithChoices>> = flowOf(emptyList())
        override fun observeCoin(coinId: Long): Flow<CoinWithChoices?> = flowOf(null)
        override suspend fun saveCoin(coinId: Long?, name: String, choices: List<String>): Long = 1L
        override suspend fun deleteCoin(coinId: Long) {}
        override suspend fun recordInteraction(coinId: Long) {}
        override suspend fun setFavorite(coinId: Long, isFavorite: Boolean) {}
        override suspend fun recordDecision(coinId: Long, choiceId: Long, choiceTextSnapshot: String) {}
        override suspend fun reorderChoices(coinId: Long, orderedChoiceIds: List<Long>) {}
        override suspend fun setWeightedEnabled(coinId: Long, enabled: Boolean) {}
        override suspend fun setAvoidLastResultEnabled(coinId: Long, enabled: Boolean) {}
        override suspend fun updateChoiceWeights(coinId: Long, weights: List<Pair<Long, Int?>>) {}
        override suspend fun getLastDecision(coinId: Long) = null
        override fun observeDecisionHistory(coinId: Long) = flowOf(emptyList<com.choice.app.domain.Decision>())
        override suspend fun importSharedCoin(payload: SharedCoin): Long {
            importedPayload = payload
            return 1L
        }
    }

    @Test
    fun importCoinScreen_showsTitleAndInput() {
        val viewModel = ImportCoinViewModel(fakeRepository)
        composeTestRule.setContent {
            ImportCoinScreen(
                viewModel = viewModel,
                onBack = {},
                onImportSuccess = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.share_import_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.share_import_instructions)).assertIsDisplayed()
    }

    @Test
    fun importCoinScreen_showsImportButton() {
        val viewModel = ImportCoinViewModel(fakeRepository)
        composeTestRule.setContent {
            ImportCoinScreen(
                viewModel = viewModel,
                onBack = {},
                onImportSuccess = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.share_import_action)).assertIsDisplayed()
    }

    @Test
    fun importCoinScreen_importButtonDisabledWhenEmpty() {
        val viewModel = ImportCoinViewModel(fakeRepository)
        composeTestRule.setContent {
            ImportCoinScreen(
                viewModel = viewModel,
                onBack = {},
                onImportSuccess = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.share_import_action)).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.share_error_empty_payload)).assertIsDisplayed()
    }
}
