package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Data classes definidas no final do arquivo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para ClientDetailFragment
 * FASE 4A - Implementação crítica com dados mock
 */
@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    // TODO: Injetar repositórios quando implementarmos
    // private val clienteRepository: ClienteRepository,
    // private val acertoRepository: AcertoRepository
) : ViewModel() {

    private val _clientDetails = MutableStateFlow<ClienteResumo?>(null)
    val clientDetails: StateFlow<ClienteResumo?> = _clientDetails.asStateFlow()

    private val _settlementHistory = MutableStateFlow<List<AcertoResumo>>(emptyList())
    val settlementHistory: StateFlow<List<AcertoResumo>> = _settlementHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadClientDetails(clienteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // MOCK DATA - Dados de exemplo para funcionamento imediato
                val mockClient = when (clienteId) {
                    1L -> ClienteResumo(
                        id = 1L,
                        nome = "Bar do João",
                        endereco = "Rua das Flores, 123 - Centro",
                        telefone = "(11) 99999-9999",
                        mesasAtivas = 2,
                        ultimaVisita = "15/12/2024",
                        observacoes = "Cliente pontual no pagamento. Prefere acertos às quintas-feiras."
                    )
                    2L -> ClienteResumo(
                        id = 2L,
                        nome = "Boteco da Maria",
                        endereco = "Av. Principal, 456 - Vila Nova",
                        telefone = "(11) 88888-8888",
                        mesasAtivas = 1,
                        ultimaVisita = "14/12/2024",
                        observacoes = "Estabelecimento familiar. Movimento maior nos finais de semana."
                    )
                    else -> ClienteResumo(
                        id = clienteId,
                        nome = "Cliente $clienteId",
                        endereco = "Endereço do Cliente $clienteId",
                        telefone = "(11) 77777-7777",
                        mesasAtivas = 1,
                        ultimaVisita = "13/12/2024",
                        observacoes = "Observações do cliente $clienteId"
                    )
                }
                
                // MOCK - Histórico de acertos
                val mockSettlements = listOf(
                    AcertoResumo(
                        id = 1L,
                        data = "10/12/2024",
                        valor = 250.00,
                        status = "Pago",
                        mesasAcertadas = 2
                    ),
                    AcertoResumo(
                        id = 2L,
                        data = "03/12/2024",
                        valor = 180.50,
                        status = "Pendente",
                        mesasAcertadas = 1
                    ),
                    AcertoResumo(
                        id = 3L,
                        data = "26/11/2024",
                        valor = 320.00,
                        status = "Pago",
                        mesasAcertadas = 2
                    )
                )
                
                _clientDetails.value = mockClient
                _settlementHistory.value = mockSettlements
                
            } catch (e: Exception) {
                // TODO: Implementar tratamento de erro
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// Data classes auxiliares - FASE 4A
data class ClienteResumo(
    val id: Long,
    val nome: String,
    val endereco: String,
    val telefone: String,
    val mesasAtivas: Int,
    val ultimaVisita: String,
    val observacoes: String
)

data class AcertoResumo(
    val id: Long,
    val data: String,
    val valor: Double,
    val status: String,
    val mesasAcertadas: Int
) 
