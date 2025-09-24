package com.example.gestaobilhares.ui.inventory.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockViewModel @Inject constructor() : ViewModel() {
    
    private val _stockItems = MutableStateFlow<List<StockItem>>(emptyList())
    val stockItems: StateFlow<List<StockItem>> = _stockItems.asStateFlow()

    init {
        loadStockItems()
    }

    private fun loadStockItems() {
        viewModelScope.launch {
            // TODO: Implementar carregamento de itens do estoque do banco de dados
            _stockItems.value = getSampleStockItems()
        }
    }

    private fun getSampleStockItems(): List<StockItem> {
        return listOf(
            StockItem(
                id = 1L,
                name = "Taco de Sinuca",
                category = "Acessórios",
                quantity = 10,
                unitPrice = 25.0,
                supplier = "Fornecedor A"
            ),
            StockItem(
                id = 2L,
                name = "Bolas de Sinuca",
                category = "Acessórios",
                quantity = 50,
                unitPrice = 5.0,
                supplier = "Fornecedor B"
            )
        )
    }
}

data class StockItem(
    val id: Long,
    val name: String,
    val category: String,
    val quantity: Int,
    val unitPrice: Double,
    val supplier: String
)
