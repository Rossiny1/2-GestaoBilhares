package com.example.gestaobilhares.ui.inventory.equipments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EquipmentsViewModel @Inject constructor() : ViewModel() {
    
    private val _equipments = MutableStateFlow<List<Equipment>>(emptyList())
    val equipments: StateFlow<List<Equipment>> = _equipments.asStateFlow()

    init {
        loadEquipments()
    }

    private fun loadEquipments() {
        viewModelScope.launch {
            // TODO: Implementar carregamento de equipamentos do banco de dados
            _equipments.value = emptyList()
        }
    }
}

data class Equipment(
    val id: Long,
    val name: String,
    val type: String,
    val status: String,
    val location: String
)
