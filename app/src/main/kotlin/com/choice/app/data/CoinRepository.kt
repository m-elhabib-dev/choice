package com.choice.app.data

import com.choice.app.domain.CoinWithChoices
import kotlinx.coroutines.flow.Flow

interface CoinRepository {
    fun observeCoins(): Flow<List<CoinWithChoices>>

    fun observeQuickCoins(): Flow<List<CoinWithChoices>>

    fun observeCoin(coinId: Long): Flow<CoinWithChoices?>

    suspend fun saveCoin(coinId: Long?, name: String, choices: List<String>): Long

    suspend fun deleteCoin(coinId: Long)

    suspend fun recordInteraction(coinId: Long)

    suspend fun setFavorite(coinId: Long, isFavorite: Boolean)

    suspend fun recordDecision(coinId: Long, choiceId: Long, choiceTextSnapshot: String)

    suspend fun reorderChoices(coinId: Long, orderedChoiceIds: List<Long>)

    suspend fun setWeightedEnabled(coinId: Long, enabled: Boolean)

    suspend fun setAvoidLastResultEnabled(coinId: Long, enabled: Boolean)

    suspend fun updateChoiceWeights(coinId: Long, weights: List<Pair<Long, Int?>>)

    suspend fun getLastDecision(coinId: Long): com.choice.app.domain.Decision?

    fun observeDecisionHistory(coinId: Long): Flow<List<com.choice.app.domain.Decision>>
}
