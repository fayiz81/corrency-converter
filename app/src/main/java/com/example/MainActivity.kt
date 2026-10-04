package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.api.ApiClient
import com.example.data.local.CurrencyDatabase
import com.example.data.repository.CurrencyRepository
import com.example.ui.CurrencyScreen
import com.example.ui.CurrencyViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val viewModel: CurrencyViewModel by viewModels {
    val database = CurrencyDatabase.getDatabase(applicationContext)
    val repository = CurrencyRepository(
      api = ApiClient.frankfurterApi,
      dao = database.currencyDao()
    )
    CurrencyViewModel.provideFactory(repository)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        CurrencyScreen(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

