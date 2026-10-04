package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversionHistoryEntity
import com.example.data.model.CurrencyCatalog
import com.example.data.repository.CurrencyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CurrencyViewModel(
    private val repository: CurrencyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrencyUiState())
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    private var currentRatesMap: Map<String, Double> = emptyMap()
    private var chartJob: Job? = null
    private var historySaveJob: Job? = null

    init {
        loadCurrencies()
        observeHistoryAndFavorites()
        fetchRatesAndChart()
    }

    private fun loadCurrencies() {
        viewModelScope.launch {
            val currencies = repository.getCurrencies()
            _uiState.update { it.copy(allCurrencies = currencies) }
        }
    }

    private fun observeHistoryAndFavorites() {
        viewModelScope.launch {
            repository.conversionHistory.collect { historyList ->
                _uiState.update { it.copy(history = historyList) }
            }
        }
    }

    fun fetchRatesAndChart() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRates = true) }
            val state = _uiState.value

            val result = repository.getLatestRates(state.fromCurrency)
            result.onSuccess { (rates, date) ->
                currentRatesMap = rates
                val rate = rates[state.toCurrency] ?: 1.0
                val inverse = if (rate != 0.0) 1.0 / rate else 0.0
                val amount = state.fromAmountInput.toDoubleOrNull() ?: 0.0
                val converted = amount * rate
                val isOffline = date.contains("offline") || date.contains("estimated")

                // Update popular rates (EUR, GBP, JPY, CAD, AUD, CHF, INR, CNY)
                val popularCodes = listOf("EUR", "USD", "GBP", "JPY", "CAD", "AUD", "CHF", "INR", "CNY")
                    .filter { it != state.fromCurrency }
                    .take(6)

                val popular = popularCodes.mapNotNull { code ->
                    rates[code]?.let { r ->
                        Pair(CurrencyCatalog.getCurrency(code), r)
                    }
                }

                _uiState.update {
                    it.copy(
                        exchangeRate = rate,
                        inverseRate = inverse,
                        toAmount = converted,
                        rateDate = date,
                        popularRates = popular,
                        isLoadingRates = false,
                        isOffline = isOffline
                    )
                }

                loadChartData()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingRates = false,
                        userMessage = "Could not fetch rates: ${err.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun onFromAmountChanged(newInput: String) {
        // Allow empty or valid decimal numbers (digits and at most one decimal point)
        val filtered = newInput.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } > 1) return
        if (filtered.length > 12) return

        val parsedAmount = filtered.toDoubleOrNull() ?: 0.0
        val converted = parsedAmount * _uiState.value.exchangeRate

        _uiState.update {
            it.copy(
                fromAmountInput = filtered,
                toAmount = converted
            )
        }

        scheduleHistorySave()
    }

    fun onQuickAmountSelected(amount: Double) {
        val formatted = if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            amount.toString()
        }
        onFromAmountChanged(formatted)
    }

    fun onFromCurrencySelected(currencyCode: String) {
        if (currencyCode == _uiState.value.fromCurrency) return

        // If user chose same as target, swap them
        if (currencyCode == _uiState.value.toCurrency) {
            onSwapCurrencies()
            return
        }

        _uiState.update { it.copy(fromCurrency = currencyCode) }
        fetchRatesAndChart()
        scheduleHistorySave()
    }

    fun onToCurrencySelected(currencyCode: String) {
        if (currencyCode == _uiState.value.toCurrency) return

        // If user chose same as base, swap them
        if (currencyCode == _uiState.value.fromCurrency) {
            onSwapCurrencies()
            return
        }

        val rate = currentRatesMap[currencyCode] ?: 1.0
        val inverse = if (rate != 0.0) 1.0 / rate else 0.0
        val amount = _uiState.value.fromAmountInput.toDoubleOrNull() ?: 0.0
        val converted = amount * rate

        _uiState.update {
            it.copy(
                toCurrency = currencyCode,
                exchangeRate = rate,
                inverseRate = inverse,
                toAmount = converted
            )
        }

        loadChartData()
        scheduleHistorySave()
    }

    fun onSwapCurrencies() {
        val oldFrom = _uiState.value.fromCurrency
        val oldTo = _uiState.value.toCurrency

        _uiState.update {
            it.copy(
                fromCurrency = oldTo,
                toCurrency = oldFrom
            )
        }

        fetchRatesAndChart()
        scheduleHistorySave()
    }

    fun onTimeframeSelected(timeframe: Timeframe) {
        if (_uiState.value.selectedTimeframe == timeframe) return
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
        loadChartData()
    }

    fun onToggleFavorite() {
        viewModelScope.launch {
            val state = _uiState.value
            val isFav = repository.toggleFavorite(state.fromCurrency, state.toCurrency)
            _uiState.update { it.copy(isFavorite = isFav) }
        }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun onDeleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun onRestoreHistoryItem(item: ConversionHistoryEntity) {
        _uiState.update {
            it.copy(
                fromCurrency = item.fromCurrency,
                toCurrency = item.toCurrency,
                fromAmountInput = if (item.fromAmount % 1.0 == 0.0) item.fromAmount.toLong().toString() else item.fromAmount.toString(),
                toAmount = item.toAmount,
                exchangeRate = item.rate
            )
        }
        fetchRatesAndChart()
    }

    fun dismissUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun loadChartData() {
        chartJob?.cancel()
        chartJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChart = true) }
            val state = _uiState.value
            val points = repository.getTimeSeries(
                baseCurrency = state.fromCurrency,
                targetCurrency = state.toCurrency,
                days = state.selectedTimeframe.days
            )
            _uiState.update {
                it.copy(
                    trendPoints = points,
                    isLoadingChart = false
                )
            }
        }
    }

    private fun scheduleHistorySave() {
        historySaveJob?.cancel()
        historySaveJob = viewModelScope.launch {
            delay(1500) // Debounce before saving to history
            val state = _uiState.value
            val amount = state.fromAmountInput.toDoubleOrNull() ?: 0.0
            if (amount > 0.0 && state.toAmount > 0.0 && state.fromCurrency != state.toCurrency) {
                repository.saveConversion(
                    fromCurrency = state.fromCurrency,
                    toCurrency = state.toCurrency,
                    fromAmount = amount,
                    toAmount = state.toAmount,
                    rate = state.exchangeRate
                )
            }
        }
    }

    companion object {
        fun provideFactory(repository: CurrencyRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CurrencyViewModel(repository) as T
                }
            }
    }
}
