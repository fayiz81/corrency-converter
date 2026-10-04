package com.example.ui

import com.example.data.local.ConversionHistoryEntity
import com.example.data.model.CurrencyInfo

enum class Timeframe(val label: String, val days: Int) {
    WEEK("7D", 7),
    MONTH("30D", 30),
    QUARTER("90D", 90)
}

data class CurrencyUiState(
    val fromCurrency: String = "USD",
    val toCurrency: String = "EUR",
    val fromAmountInput: String = "100",
    val toAmount: Double = 0.0,
    val exchangeRate: Double = 1.0,
    val inverseRate: Double = 1.0,
    val rateDate: String = "",
    val allCurrencies: List<CurrencyInfo> = emptyList(),
    val popularRates: List<Pair<CurrencyInfo, Double>> = emptyList(),
    val trendPoints: List<Pair<String, Double>> = emptyList(),
    val selectedTimeframe: Timeframe = Timeframe.MONTH,
    val isFavorite: Boolean = false,
    val isLoadingRates: Boolean = false,
    val isLoadingChart: Boolean = false,
    val isOffline: Boolean = false,
    val history: List<ConversionHistoryEntity> = emptyList(),
    val userMessage: String? = null
)
