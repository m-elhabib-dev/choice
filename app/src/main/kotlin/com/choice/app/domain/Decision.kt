package com.choice.app.domain

data class Decision(
    val id: Long,
    val coinId: Long,
    val choiceId: Long?,
    val choiceTextSnapshot: String,
    val decidedAt: Long,
)
