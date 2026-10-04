package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ConversionCard
import com.example.ui.components.ConversionHistorySheet
import com.example.ui.components.CurrencyPickerSheet
import com.example.ui.components.RatesBoard
import com.example.ui.components.TrendChart

private enum class PickerTarget {
    FROM, TO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyScreen(
    viewModel: CurrencyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }
    var showHistorySheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissUserMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("currency_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Project 02 Tag
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = "Project 02",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // No key needed badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = "No key needed",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                actions = {
                    // History Icon with Badge
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (uiState.history.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(uiState.history.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Conversion History",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Hero Section matching prompt screenshot
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Currency Converter",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Convert an amount between currencies using live exchange rates.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Frankfurter ↗ Link
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://frankfurter.dev/")
                                )
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                            .testTag("frankfurter_link"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frankfurter",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open frankfurter.dev",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Main Interactive Conversion Card
            item {
                ConversionCard(
                    uiState = uiState,
                    onAmountChange = { viewModel.onFromAmountChanged(it) },
                    onFromCurrencyClick = { pickerTarget = PickerTarget.FROM },
                    onToCurrencyClick = { pickerTarget = PickerTarget.TO },
                    onSwapClick = { viewModel.onSwapCurrencies() },
                    onQuickAmountClick = { viewModel.onQuickAmountSelected(it) },
                    onRefreshClick = { viewModel.fetchRatesAndChart() }
                )
            }

            // Interactive Trend Chart
            item {
                TrendChart(
                    uiState = uiState,
                    onTimeframeSelected = { viewModel.onTimeframeSelected(it) }
                )
            }

            // Global Popular Rates Board
            if (uiState.popularRates.isNotEmpty()) {
                item {
                    RatesBoard(
                        baseCurrency = uiState.fromCurrency,
                        popularRates = uiState.popularRates,
                        onCurrencyClick = { viewModel.onToCurrencySelected(it) }
                    )
                }
            }

            // Attribution Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Data provided by European Central Bank reference rates via Frankfurter API",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Currency Picker Bottom Sheet
        pickerTarget?.let { target ->
            CurrencyPickerSheet(
                title = if (target == PickerTarget.FROM) "Select Base Currency" else "Select Target Currency",
                currencies = uiState.allCurrencies,
                selectedCurrencyCode = if (target == PickerTarget.FROM) uiState.fromCurrency else uiState.toCurrency,
                onCurrencySelected = { code ->
                    if (target == PickerTarget.FROM) {
                        viewModel.onFromCurrencySelected(code)
                    } else {
                        viewModel.onToCurrencySelected(code)
                    }
                },
                onDismissRequest = { pickerTarget = null }
            )
        }

        // Conversion History Bottom Sheet
        if (showHistorySheet) {
            ConversionHistorySheet(
                history = uiState.history,
                onItemClick = { viewModel.onRestoreHistoryItem(it) },
                onDeleteItem = { viewModel.onDeleteHistoryItem(it) },
                onClearAll = { viewModel.onClearHistory() },
                onDismissRequest = { showHistorySheet = false }
            )
        }
    }
}
