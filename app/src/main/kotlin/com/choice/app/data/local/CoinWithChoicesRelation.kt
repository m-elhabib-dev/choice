package com.choice.app.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class CoinWithChoicesRelation(
    @Embedded val coin: CoinEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "coin_id",
    )
    val choices: List<ChoiceEntity>,
)
