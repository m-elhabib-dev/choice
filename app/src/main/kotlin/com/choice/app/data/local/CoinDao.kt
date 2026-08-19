package com.choice.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinDao {
    @Transaction
    @Query("SELECT * FROM coins ORDER BY name")
    fun observeCoins(): Flow<List<CoinWithChoicesRelation>>

    @Transaction
    @Query("SELECT * FROM coins WHERE interaction_count > 0")
    fun observeCoinsWithInteractions(): Flow<List<CoinWithChoicesRelation>>

    @Transaction
    @Query("SELECT * FROM coins WHERE id = :coinId")
    fun observeCoin(coinId: Long): Flow<CoinWithChoicesRelation?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoin(coin: CoinEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChoices(choices: List<ChoiceEntity>)

    @Query("DELETE FROM choices WHERE coin_id = :coinId")
    suspend fun deleteChoicesByCoinId(coinId: Long)

    @Query("UPDATE coins SET interaction_count = interaction_count + 1, last_interaction_at = :now WHERE id = :coinId")
    suspend fun recordInteraction(coinId: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM coins WHERE id = :coinId")
    suspend fun deleteCoinById(coinId: Long)
}
