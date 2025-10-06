package com.example.gestaobilhares.ui.inventory.others

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
class OthersInventoryViewModel constructor() : ViewModel() {
    
    private val _othersItems = MutableStateFlow<List<OtherItem>>(emptyList())
    val othersItems: StateFlow<List<OtherItem>> = _othersItems.asStateFlow()

    init {
        loadOthersItems()
    }

    private fun loadOthersItems() {
        viewModelScope.launch {
            // TODO: Implementar carregamento de outros itens do banco de dados
            _othersItems.value = emptyList()
        }
    }
}

data class OtherItem(
    val id: Long,
    val name: String,
    val description: String,
    val quantity: Int,
    val location: String
)

