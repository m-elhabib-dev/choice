package com.choice.app.data

import com.choice.app.data.local.CoinDao
import com.choice.app.data.local.CoinEntity
import com.choice.app.data.local.CoinWithChoicesRelation
import com.choice.app.data.local.ChoiceEntity
import com.choice.app.data.local.DecisionEntity
import com.choice.app.domain.Coin
import com.choice.app.domain.CoinSummary
import com.choice.app.domain.CoinWithChoices
import com.choice.app.domain.Choice
import com.choice.app.domain.Decision
import com.choice.app.domain.SharedCoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CoinRepositoryImpl(
    private val coinDao: CoinDao,
) : CoinRepository {

    override fun observeCoins(): Flow<List<CoinWithChoices>> =
        coinDao.observeCoins().map { relations -> relations.map { it.toDomain() } }

    override fun observeQuickCoins(): Flow<List<CoinWithChoices>> =
        coinDao.observeQuickCoins().map { relations -> relations.map { it.toDomain() } }

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

    override suspend fun setFavorite(coinId: Long, isFavorite: Boolean) {
        coinDao.setFavorite(coinId, isFavorite)
    }

    override suspend fun recordDecision(coinId: Long, choiceId: Long, choiceTextSnapshot: String) {
        coinDao.insertDecision(
            DecisionEntity(
                coinId = coinId,
                choiceId = choiceId,
                choiceTextSnapshot = choiceTextSnapshot,
            )
        )
    }

    override suspend fun reorderChoices(coinId: Long, orderedChoiceIds: List<Long>) {
        orderedChoiceIds.forEachIndexed { index, choiceId ->
            coinDao.updateChoicePosition(choiceId, index)
        }
    }

    override suspend fun setWeightedEnabled(coinId: Long, enabled: Boolean) {
        coinDao.setWeightedEnabled(coinId, enabled)
    }

    override suspend fun setAvoidLastResultEnabled(coinId: Long, enabled: Boolean) {
        coinDao.setAvoidLastResultEnabled(coinId, enabled)
    }

    override suspend fun updateChoiceWeights(coinId: Long, weights: List<Pair<Long, Int?>>) {
        weights.forEach { (choiceId, weight) ->
            if (weight != null) {
                require(weight >= 1) { "Weight must be a positive whole number" }
            }
            coinDao.updateChoiceWeight(choiceId, weight)
        }
    }

    override suspend fun getLastDecision(coinId: Long): Decision? {
        val entity = coinDao.getLastDecisionByCoinId(coinId) ?: return null
        return Decision(
            id = entity.id,
            coinId = entity.coinId,
            choiceId = entity.choiceId,
            choiceTextSnapshot = entity.choiceTextSnapshot,
            decidedAt = entity.decidedAt,
        )
    }

    override fun observeDecisionHistory(coinId: Long): Flow<List<Decision>> =
        coinDao.observeDecisionsByCoinId(coinId).map { entities ->
            entities.map { entity ->
                Decision(
                    id = entity.id,
                    coinId = entity.coinId,
                    choiceId = entity.choiceId,
                    choiceTextSnapshot = entity.choiceTextSnapshot,
                    decidedAt = entity.decidedAt,
                )
            }
        }

    override suspend fun importSharedCoin(payload: SharedCoin): Long {
        val persistedCoinId = coinDao.insertCoin(
            CoinEntity(
                id = 0,
                name = payload.name.trim(),
                weightedEnabled = payload.weightedEnabled,
                avoidLastResultEnabled = payload.avoidLastResultEnabled,
            ),
        )

        coinDao.insertChoices(
            payload.choices.mapIndexed { index, sharedChoice ->
                ChoiceEntity(
                    coinId = persistedCoinId,
                    text = sharedChoice.text.trim(),
                    position = index,
                    weight = sharedChoice.weight,
                )
            },
        )

        return persistedCoinId
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
            isFavorite = coin.isFavorite,
            weightedEnabled = coin.weightedEnabled,
            avoidLastResultEnabled = coin.avoidLastResultEnabled,
        ),
        choices = choices
            .sortedBy { it.position }
            .map { choice ->
                Choice(
                    id = choice.id,
                    coinId = choice.coinId,
                    text = choice.text,
                    position = choice.position,
                    weight = choice.weight,
                )
            },
    )
