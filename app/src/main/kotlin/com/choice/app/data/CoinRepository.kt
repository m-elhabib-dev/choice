package com.choice.app.data

import com.choice.app.domain.CoinWithChoices
import kotlinx.coroutines.flow.Flow

interface CoinRepository {
    fun observeCoins(): Flow<List<CoinWithChoices>>

    fun observeQuickAccessCoins(limit: Int = 5): Flow<List<CoinWithChoices>>

    fun observeCoin(coinId: Long): Flow<CoinWithChoices?>

    suspend fun saveCoin(coinId: Long?, name: String, choices: List<String>): Long

    suspend fun deleteCoin(coinId: Long)

    suspend fun recordInteraction(coinId: Long)
}
