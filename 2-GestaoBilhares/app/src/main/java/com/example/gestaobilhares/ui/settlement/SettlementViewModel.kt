package com.example.gestaobilhares.ui.settlement

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.MesaRepository

/**
 * ViewModel para SettlementFragment
 * FASE 4A - Implementação básica para desbloqueio
 */
@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val mesaRepository: MesaRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _clientName = MutableStateFlow("")
    val clientName: StateFlow<String> = _clientName.asStateFlow()

    private val _clientAddress = MutableStateFlow("")
    val clientAddress: StateFlow<String> = _clientAddress.asStateFlow()

    private val _mesasCliente = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasCliente: StateFlow<List<Mesa>> = _mesasCliente.asStateFlow()

    private val _resultadoSalvamento = MutableStateFlow<Result<Unit>?>(null)
    val resultadoSalvamento: StateFlow<Result<Unit>?> = _resultadoSalvamento.asStateFlow()

    data class DadosAcerto(
        val mesas: List<Mesa>,
        val representante: String,
        val panoTrocado: Boolean,
        val numeroPano: String?,
        val tipoAcerto: String,
        val observacao: String,
        val justificativa: String?,
        val metodosPagamento: Map<String, Double>
    )

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

    fun loadMesasCliente(clienteId: Long) {
        viewModelScope.launch {
            mesaRepository.obterMesasPorCliente(clienteId).collect { mesas ->
                _mesasCliente.value = mesas
            }
        }
    }

    /**
     * Salva o acerto, agora recebendo os valores discriminados por método de pagamento.
     * @param dadosAcerto Dados principais do acerto
     * @param metodosPagamento Mapa de método para valor recebido
     */
    fun salvarAcerto(dadosAcerto: DadosAcerto, metodosPagamento: Map<String, Double>) {
        viewModelScope.launch {
            try {
                // TODO: Integrar persistência real
                Log.d("SettlementViewModel", "Salvando acerto: $dadosAcerto, pagamentos: $metodosPagamento")
                // Simular persistência com sucesso
                _resultadoSalvamento.value = Result.success(Unit)
            } catch (e: Exception) {
                _resultadoSalvamento.value = Result.failure(e)
            }
        }
    }

    fun resetarResultadoSalvamento() {
        _resultadoSalvamento.value = null
    }
} 
