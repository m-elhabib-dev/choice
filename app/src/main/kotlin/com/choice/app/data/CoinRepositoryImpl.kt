package com.choice.app.data

import com.choice.app.data.local.CoinDao
import com.choice.app.data.local.CoinEntity
import com.choice.app.data.local.CoinWithChoicesRelation
import com.choice.app.data.local.ChoiceEntity
import com.choice.app.domain.Coin
import com.choice.app.domain.CoinSummary
import com.choice.app.domain.CoinWithChoices
import com.choice.app.domain.Choice
import com.choice.app.domain.QuickAccessScore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CoinRepositoryImpl(
    private val coinDao: CoinDao,
) : CoinRepository {

    override fun observeCoins(): Flow<List<CoinWithChoices>> =
        coinDao.observeCoins().map { relations -> relations.map { it.toDomain() } }

    override fun observeQuickAccessCoins(limit: Int): Flow<List<CoinWithChoices>> =
        coinDao.observeCoinsWithInteractions().map { relations ->
            relations
                .map { it.toDomain() }
                .map { coinWithChoices ->
                    val coin = coinWithChoices.coin
                    coinWithChoices to QuickAccessScore.compute(
                        coin.interactionCount,
                        coin.lastInteractionAt,
                    )
                }
                .filter { (_, score) -> score > 0.0 }
                .sortedByDescending { (_, score) -> score }
                .take(limit)
                .map { (coinWithChoices, _) -> coinWithChoices }
        }

    override fun observeCoin(coinId: Long): Flow<CoinWithChoices?> =
        coinDao.observeCoin(coinId).map { it?.toDomain() }

    override suspend fun saveCoin(coinId: Long?, name: String, choices: List<String>): Long {
        require(name.isNotBlank()) { "Coin name must not be blank" }
        require(name.length <= 40) { "Coin name must not exceed 40 characters" }
        require(choices.size >= 2) { "A coin must have at least 2 choices" }
        choices.forEach { text ->
            require(text.isNotBlank()) { "Choice text must not be blank" }
            require(text.length <= 60) { "Choice text must not exceed 60 characters" }
        }

        val persistedCoinId = coinDao.insertCoin(
            CoinEntity(
                id = coinId ?: 0,
                name = name.trim(),
            ),
        )

        if (coinId != null) {
            coinDao.deleteChoicesByCoinId(coinId)
        }

        coinDao.insertChoices(
            choices.mapIndexed { index, text ->
                ChoiceEntity(
                    coinId = persistedCoinId,
                    text = text.trim(),
                    position = index,
                )
            },
        )

        return persistedCoinId
    }

    override suspend fun deleteCoin(coinId: Long) {
        coinDao.deleteCoinById(coinId)
    }

    override suspend fun recordInteraction(coinId: Long) {
        coinDao.recordInteraction(coinId)
    }
}

private fun CoinWithChoicesRelation.toDomain(): CoinWithChoices =
    CoinWithChoices(
        coin = Coin(
            id = coin.id,
            name = coin.name,
            createdAt = coin.createdAt,
            interactionCount = coin.interactionCount,
            lastInteractionAt = coin.lastInteractionAt,
        ),
        choices = choices
            .sortedBy { it.position }
            .map { choice ->
                Choice(
                    id = choice.id,
                    coinId = choice.coinId,
                    text = choice.text,
                    position = choice.position,
                )
            },
    )
