package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {

    // Cached Rates
    @Query("SELECT * FROM cached_rates WHERE baseCurrency = :base")
    suspend fun getCachedRates(base: String): List<SavedRateEntity>

    @Query("SELECT * FROM cached_rates WHERE baseCurrency = :base AND targetCurrency = :target LIMIT 1")
    suspend fun getCachedRate(base: String, target: String): SavedRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedRates(rates: List<SavedRateEntity>)

    // Conversion History
    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC LIMIT 30")
    fun getConversionHistory(): Flow<List<ConversionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: ConversionHistoryEntity)

    @Query("DELETE FROM conversion_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM conversion_history")
    suspend fun clearHistory()

    // Favorite Pairs
    @Query("SELECT * FROM favorite_pairs ORDER BY addedAt DESC")
    fun getFavoritePairs(): Flow<List<FavoritePairEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(pair: FavoritePairEntity)

    @Query("DELETE FROM favorite_pairs WHERE fromCurrency = :from AND toCurrency = :to")
    suspend fun removeFavorite(from: String, to: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_pairs WHERE fromCurrency = :from AND toCurrency = :to)")
    suspend fun isFavorite(from: String, to: String): Boolean
}
