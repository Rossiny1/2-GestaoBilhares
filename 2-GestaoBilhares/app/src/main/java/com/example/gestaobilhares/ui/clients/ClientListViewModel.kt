package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Cliente
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel temporário para lista de clientes - usando dados mock
 */
@HiltViewModel
class ClientListViewModel @Inject constructor() : ViewModel() {

    private val _clientes = MutableStateFlow<List<Cliente>>(emptyList())
    val clientes: StateFlow<List<Cliente>> = _clientes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Carrega clientes por rota - versão temporária com dados mock
     */
    fun carregarClientes(rotaId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Simulando delay de rede
                kotlinx.coroutines.delay(1000)
                
                // Dados mock temporários
                val clientesMock = listOf(
                    Cliente(
                        id = 1,
                        rotaId = rotaId,
                        nome = "Bar do João",
                        nomeFantasia = "João's Bar",
                        endereco = "Rua das Flores, 123",
                        telefone = "(11) 99999-9999",
                        valorFicha = 2.50,
                        debitoAnterior = 125.00
                    ),
                    Cliente(
                        id = 2,
                        rotaId = rotaId,
                        nome = "Sinuca do Pedro",
                        nomeFantasia = "Pedro's Sinuca",
                        endereco = "Av. Central, 456",
                        telefone = "(11) 88888-8888",
                        valorFicha = 3.00,
                        debitoAnterior = 89.50
                    ),
                    Cliente(
                        id = 3,
                        rotaId = rotaId,
                        nome = "Clube da Maria",
                        nomeFantasia = "Clube Recreativo",
                        endereco = "Rua do Comércio, 789",
                        telefone = "(11) 77777-7777",
                        valorFicha = 2.75,
                        debitoAnterior = 200.00
                    )
                )
                
                _clientes.value = clientesMock
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar clientes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Limpa mensagens de erro
     */
    fun limparErro() {
        _errorMessage.value = null
    }
} 
