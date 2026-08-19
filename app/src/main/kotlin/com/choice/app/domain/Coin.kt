package com.choice.app.domain

data class Coin(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val interactionCount: Int,
    val lastInteractionAt: Long?,
    val isFavorite: Boolean = false,
    val weightedEnabled: Boolean = false,
    val avoidLastResultEnabled: Boolean = false,
)

data class Choice(
    val id: Long,
    val coinId: Long,
    val text: String,
    val position: Int,
    val weight: Int? = null,
)

data class CoinWithChoices(
    val coin: Coin,
    val choices: List<Choice>,
)

data class CoinSummary(
    val id: Long,
    val name: String,
    val choiceCount: Int,
    val isFavorite: Boolean = false,
)
