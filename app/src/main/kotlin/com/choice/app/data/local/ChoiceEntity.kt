package com.choice.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "choices",
    foreignKeys = [
        ForeignKey(
            entity = CoinEntity::class,
            parentColumns = ["id"],
            childColumns = ["coin_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["coin_id"])],
)
data class ChoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "coin_id")
    val coinId: Long,
    val text: String,
    val position: Int,
    @ColumnInfo(name = "weight")
    val weight: Int? = null,
)
