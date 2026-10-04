package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.CurrencyUiState
import com.example.ui.components.ConversionCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        ConversionCard(
          uiState = CurrencyUiState(
            fromCurrency = "USD",
            toCurrency = "EUR",
            fromAmountInput = "100",
            toAmount = 92.14,
            exchangeRate = 0.9214,
            inverseRate = 1.0853,
            rateDate = "2026-09-21"
          ),
          onAmountChange = {},
          onFromCurrencyClick = {},
          onToCurrencyClick = {},
          onSwapClick = {},
          onQuickAmountClick = {},
          onRefreshClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

