package com.choice.app.domain

data class Coin(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val interactionCount: Int,
    val lastInteractionAt: Long?,
)

data class Choice(
    val id: Long,
    val coinId: Long,
    val text: String,
    val position: Int,
)

data class CoinWithChoices(
    val coin: Coin,
    val choices: List<Choice>,
)

data class CoinSummary(
    val id: Long,
    val name: String,
    val choiceCount: Int,
)
