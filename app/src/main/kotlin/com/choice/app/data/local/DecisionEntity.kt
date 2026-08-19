package com.choice.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "decisions",
    foreignKeys = [
        ForeignKey(
            entity = CoinEntity::class,
            parentColumns = ["id"],
            childColumns = ["coinId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["choiceId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["coinId"]),
        Index(value = ["choiceId"]),
    ],
)
data class DecisionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val coinId: Long,
    @ColumnInfo(name = "choiceId")
    val choiceId: Long?,
    @ColumnInfo(name = "choiceTextSnapshot")
    val choiceTextSnapshot: String,
    @ColumnInfo(name = "decidedAt")
    val decidedAt: Long = System.currentTimeMillis(),
)
