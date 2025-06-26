package com.example.gestaobilhares.ui.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para SettlementFragment
 * FASE 4A - Implementação básica para desbloqueio
 */
@HiltViewModel
class SettlementViewModel @Inject constructor(
    // TODO: Injetar repositórios quando implementarmos
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _clientName = MutableStateFlow("")
    val clientName: StateFlow<String> = _clientName.asStateFlow()

    private val _clientAddress = MutableStateFlow("")
    val clientAddress: StateFlow<String> = _clientAddress.asStateFlow()

    fun loadClientForSettlement(clienteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // MOCK DATA - Simular carregamento de dados do cliente
                kotlinx.coroutines.delay(500) // Simular latência
                
                val mockData = when (clienteId) {
                    1L -> Pair("Bar do João", "Rua das Flores, 123 - Centro")
                    2L -> Pair("Boteco da Maria", "Av. Principal, 456 - Vila Nova") 
                    3L -> Pair("Clube da Maria", "Rua do Comércio, 789")
                    else -> Pair("Cliente $clienteId", "Endereço do Cliente $clienteId")
                }
                
                _clientName.value = mockData.first
                _clientAddress.value = mockData.second
                
            } catch (e: Exception) {
                // TODO: Implementar tratamento de erro
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveSettlement(
        @Suppress("UNUSED_PARAMETER") clienteId: Long,
        @Suppress("UNUSED_PARAMETER") fichasInicial: Int,
        @Suppress("UNUSED_PARAMETER") fichasFinal: Int,
        @Suppress("UNUSED_PARAMETER") valorFicha: Double
    ) {
        // Parâmetros serão utilizados na implementação futura
        @Suppress("UNUSED_PARAMETER")
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // TODO: Implementar salvamento real
                // Por enquanto, apenas simular sucesso
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 
