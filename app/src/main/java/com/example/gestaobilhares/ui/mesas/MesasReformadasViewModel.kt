package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.repository.MesaReformadaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para a tela de mesas reformadas.
 * Gerencia o estado e operações relacionadas às mesas reformadas.
 */
class MesasReformadasViewModel constructor(
    private val mesaReformadaRepository: MesaReformadaRepository
) : BaseViewModel() {

    private val _mesasReformadas = MutableStateFlow<List<MesaReformada>>(emptyList())
    val mesasReformadas: StateFlow<List<MesaReformada>> = _mesasReformadas.asStateFlow()

    // isLoading já existe na BaseViewModel

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun carregarMesasReformadas() {
        viewModelScope.launch {
            try {
                showLoading()
                // ✅ CORREÇÃO: Observar o Flow continuamente para atualizar quando dados são importados
                mesaReformadaRepository.listarTodas().collect { mesas ->
                    _mesasReformadas.value = mesas
                    hideLoading() // Ocultar loading após primeira coleta
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar mesas reformadas: ${e.message}"
                hideLoading()
            }
        }
    }

    // clearError já existe na BaseViewModel
}

