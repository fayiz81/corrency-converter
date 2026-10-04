package com.example.data.repository

import android.util.Log
import com.example.data.api.ApiClient
import com.example.data.api.FrankfurterApi
import com.example.data.local.ConversionHistoryEntity
import com.example.data.local.CurrencyDao
import com.example.data.local.FavoritePairEntity
import com.example.data.local.SavedRateEntity
import com.example.data.model.CurrencyCatalog
import com.example.data.model.CurrencyInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.sin

class CurrencyRepository(
    private val api: FrankfurterApi = ApiClient.frankfurterApi,
    private val dao: CurrencyDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val conversionHistory: Flow<List<ConversionHistoryEntity>> = dao.getConversionHistory()
    val favoritePairs: Flow<List<FavoritePairEntity>> = dao.getFavoritePairs()

    suspend fun getCurrencies(): List<CurrencyInfo> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCurrencies()
            if (response.isNotEmpty()) {
                val list = response.map { (code, name) ->
                    CurrencyCatalog.getCurrency(code, name)
                }.sortedBy { it.code }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.w("CurrencyRepository", "Failed to fetch currencies from API, using catalog", e)
        }
        CurrencyCatalog.supportedCurrencies
    }

    suspend fun getLatestRates(baseCurrency: String): Result<Pair<Map<String, Double>, String>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getLatestRates(base = baseCurrency)
            val rates = response.rates.toMutableMap()
            // Ensure self-rate is 1.0
            rates[baseCurrency] = 1.0

            // Cache to database
            val entities = rates.map { (target, rate) ->
                SavedRateEntity(
                    baseCurrency = baseCurrency,
                    targetCurrency = target,
                    rate = rate,
                    date = response.date
                )
            }
            dao.insertCachedRates(entities)

            return@withContext Result.success(Pair(rates, response.date))
        } catch (e: Exception) {
            Log.w("CurrencyRepository", "Network failed for base $baseCurrency, loading cache", e)

            // Try local Room cache
            val cached = dao.getCachedRates(baseCurrency)
            if (cached.isNotEmpty()) {
                val cachedMap = cached.associate { it.targetCurrency to it.rate }.toMutableMap()
                cachedMap[baseCurrency] = 1.0
                val date = cached.firstOrNull()?.date ?: dateFormat.format(Date())
                return@withContext Result.success(Pair(cachedMap, "$date (offline)"))
            }

            // Fallback to synthetic estimates if complete first launch is offline
            val fallbackMap = getInitialFallbackRates(baseCurrency)
            val today = dateFormat.format(Date())
            Result.success(Pair(fallbackMap, "$today (estimated)"))
        }
    }

    suspend fun getTimeSeries(
        baseCurrency: String,
        targetCurrency: String,
        days: Int
    ): List<Pair<String, Double>> = withContext(Dispatchers.IO) {
        if (baseCurrency == targetCurrency) {
            val today = dateFormat.format(Date())
            return@withContext listOf(Pair(today, 1.0))
        }

        try {
            val calendar = Calendar.getInstance()
            val endDate = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val startDate = dateFormat.format(calendar.time)

            val response = api.getTimeSeries(
                dateRange = "$startDate..$endDate",
                base = baseCurrency,
                symbols = targetCurrency
            )

            val sortedPoints = response.rates.mapNotNull { (date, rates) ->
                val rate = rates[targetCurrency]
                if (rate != null) Pair(date, rate) else null
            }.sortedBy { it.first }

            if (sortedPoints.isNotEmpty()) {
                return@withContext sortedPoints
            }
        } catch (e: Exception) {
            Log.w("CurrencyRepository", "Failed to fetch time series from API", e)
        }

        // Generate smooth fallback trend if offline
        generateFallbackTimeSeries(baseCurrency, targetCurrency, days)
    }

    suspend fun saveConversion(
        fromCurrency: String,
        toCurrency: String,
        fromAmount: Double,
        toAmount: Double,
        rate: Double
    ) = withContext(Dispatchers.IO) {
        dao.insertHistory(
            ConversionHistoryEntity(
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                fromAmount = fromAmount,
                toAmount = toAmount,
                rate = rate,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearHistory()
    }

    suspend fun deleteHistoryItem(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteHistoryById(id)
    }

    suspend fun toggleFavorite(from: String, to: String): Boolean = withContext(Dispatchers.IO) {
        val exists = dao.isFavorite(from, to)
        if (exists) {
            dao.removeFavorite(from, to)
            false
        } else {
            dao.addFavorite(FavoritePairEntity(fromCurrency = from, toCurrency = to))
            true
        }
    }

    private fun generateFallbackTimeSeries(
        base: String,
        target: String,
        days: Int
    ): List<Pair<String, Double>> {
        val calendar = Calendar.getInstance()
        val dates = mutableListOf<String>()
        for (i in days downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            // Skip weekends as foreign exchange markets are closed
            val dayOfWeek = c.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                dates.add(dateFormat.format(c.time))
            }
        }

        val baseRate = getInitialFallbackRates(base)[target] ?: 1.15
        return dates.mapIndexed { index, date ->
            // Subtle natural oscillation around the base rate
            val variance = sin(index.toDouble() * 0.45) * 0.012 * baseRate
            Pair(date, (baseRate + variance))
        }
    }

    private fun getInitialFallbackRates(base: String): Map<String, Double> {
        val usdRates = mapOf(
            "USD" to 1.0,
            "EUR" to 0.92,
            "GBP" to 0.78,
            "JPY" to 154.5,
            "CAD" to 1.36,
            "AUD" to 1.52,
            "CHF" to 0.89,
            "CNY" to 7.23,
            "INR" to 83.5,
            "BRL" to 5.42,
            "MXN" to 18.2,
            "SGD" to 1.35,
            "HKD" to 7.81,
            "KRW" to 1380.0,
            "SEK" to 10.6,
            "NOK" to 10.8,
            "TRY" to 33.2,
            "ZAR" to 18.1
        )

        val baseInUsd = usdRates[base] ?: 1.0
        return usdRates.mapValues { (_, rateInUsd) ->
            rateInUsd / baseInUsd
        }
    }
}
