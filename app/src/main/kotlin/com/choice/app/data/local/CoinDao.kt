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
    @Query("SELECT * FROM coins WHERE is_favorite = 1 ORDER BY last_interaction_at DESC")
    fun observeQuickCoins(): Flow<List<CoinWithChoicesRelation>>

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

    @Query("UPDATE coins SET is_favorite = :isFavorite WHERE id = :coinId")
    suspend fun setFavorite(coinId: Long, isFavorite: Boolean)

    @Query("DELETE FROM coins WHERE id = :coinId")
    suspend fun deleteCoinById(coinId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: DecisionEntity): Long

    @Transaction
    @Query("SELECT * FROM decisions WHERE coinId = :coinId ORDER BY decidedAt DESC")
    fun observeDecisionsByCoinId(coinId: Long): Flow<List<DecisionEntity>>

    @Query("SELECT * FROM decisions WHERE coinId = :coinId ORDER BY decidedAt DESC LIMIT 1")
    suspend fun getLastDecisionByCoinId(coinId: Long): DecisionEntity?

    @Query("UPDATE choices SET position = :position WHERE id = :choiceId")
    suspend fun updateChoicePosition(choiceId: Long, position: Int)

    @Query("UPDATE coins SET weighted_enabled = :enabled WHERE id = :coinId")
    suspend fun setWeightedEnabled(coinId: Long, enabled: Boolean)

    @Query("UPDATE coins SET avoid_last_result_enabled = :enabled WHERE id = :coinId")
    suspend fun setAvoidLastResultEnabled(coinId: Long, enabled: Boolean)

    @Query("UPDATE choices SET weight = :weight WHERE id = :choiceId")
    suspend fun updateChoiceWeight(choiceId: Long, weight: Int?)
}
