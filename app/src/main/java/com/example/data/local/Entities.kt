package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_rates", primaryKeys = ["baseCurrency", "targetCurrency"])
data class SavedRateEntity(
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val date: String,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversion_history")
data class ConversionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromCurrency: String,
    val toCurrency: String,
    val fromAmount: Double,
    val toAmount: Double,
    val rate: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_pairs", primaryKeys = ["fromCurrency", "toCurrency"])
data class FavoritePairEntity(
    val fromCurrency: String,
    val toCurrency: String,
    val addedAt: Long = System.currentTimeMillis()
)
