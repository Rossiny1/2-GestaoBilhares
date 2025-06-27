package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.MesaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MesasDepositoViewModel @Inject constructor(
    private val mesaRepository: MesaRepository
) : ViewModel() {
    private val _mesasDisponiveis = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasDisponiveis: StateFlow<List<Mesa>> = _mesasDisponiveis.asStateFlow()

    fun loadMesasDisponiveis() {
        viewModelScope.launch {
            mesaRepository.obterMesasDisponiveis().collect { mesas ->
                _mesasDisponiveis.value = mesas
            }
        }
    }

    fun vincularMesaAoCliente(mesaId: Long, clienteId: Long, tipoFixo: Boolean, valorFixo: Double?) {
        viewModelScope.launch {
            // Aqui pode-se salvar o tipo de acerto/valor fixo em campos extras da mesa futuramente
            mesaRepository.vincularMesa(mesaId, clienteId)
            loadMesasDisponiveis()
        }
    }
} 