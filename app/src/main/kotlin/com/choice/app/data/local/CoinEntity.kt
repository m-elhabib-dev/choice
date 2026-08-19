package com.choice.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coins")
data class CoinEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "interaction_count", defaultValue = "0")
    val interactionCount: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_interaction_at")
    val lastInteractionAt: Long? = null,
    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "weighted_enabled", defaultValue = "0")
    val weightedEnabled: Boolean = false,
    @ColumnInfo(name = "avoid_last_result_enabled", defaultValue = "0")
    val avoidLastResultEnabled: Boolean = false,
)
