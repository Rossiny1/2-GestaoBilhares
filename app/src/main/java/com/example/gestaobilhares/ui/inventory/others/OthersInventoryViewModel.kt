package com.example.gestaobilhares.ui.inventory.others

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OthersInventoryViewModel @Inject constructor() : ViewModel() {
    
    private val _othersItems = MutableStateFlow<List<OtherItem>>(emptyList())
    val othersItems: StateFlow<List<OtherItem>> = _othersItems.asStateFlow()

    init {
        loadOthersItems()
    }

    private fun loadOthersItems() {
        viewModelScope.launch {
            // TODO: Implementar carregamento de outros itens do banco de dados
            _othersItems.value = getSampleOthersItems()
        }
    }

    private fun getSampleOthersItems(): List<OtherItem> {
        return listOf(
            OtherItem(
                id = 1L,
                name = "Giz para Mesa",
                description = "Giz para marcar jogadas na mesa",
                quantity = 5,
                location = "Gaveta Principal"
            ),
            OtherItem(
                id = 2L,
                name = "Pano de Limpeza",
                description = "Pano para limpeza das mesas",
                quantity = 3,
                location = "Armário de Limpeza"
            )
        )
    }
}

data class OtherItem(
    val id: Long,
    val name: String,
    val description: String,
    val quantity: Int,
    val location: String
)
