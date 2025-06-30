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
import com.example.gestaobilhares.data.repositories.ClienteRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.entities.Acerto
import kotlinx.coroutines.flow.Flow

/**
 * ViewModel para SettlementFragment
 * FASE 4A - Implementação básica para desbloqueio
 */
@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val mesaRepository: MesaRepository,
    private val clienteRepository: ClienteRepository,
    private val acertoRepository: AcertoRepository
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

    private val _historicoAcertos = MutableStateFlow<List<Acerto>>(emptyList())
    val historicoAcertos: StateFlow<List<Acerto>> = _historicoAcertos.asStateFlow()

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
                val cliente = clienteRepository.obterPorId(clienteId)
                if (cliente != null) {
                    _clientName.value = cliente.nome
                    _clientAddress.value = cliente.endereco ?: "---"
                    Log.d("SettlementViewModel", "Nome do cliente carregado: ${cliente.nome}, endereço: ${cliente.endereco}")
                } else {
                    _clientName.value = "Cliente não encontrado"
                    _clientAddress.value = "---"
                    Log.d("SettlementViewModel", "Cliente não encontrado para ID: $clienteId")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _clientName.value = "Erro ao carregar cliente"
                _clientAddress.value = "---"
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

    fun carregarHistoricoAcertos(clienteId: Long) {
        viewModelScope.launch {
            acertoRepository.buscarPorCliente(clienteId).collect { acertos ->
                _historicoAcertos.value = acertos
            }
        }
    }

    /**
     * Salva o acerto, agora recebendo os valores discriminados por método de pagamento.
     * @param clienteId ID do cliente
     * @param dadosAcerto Dados principais do acerto
     * @param metodosPagamento Mapa de método para valor recebido
     */
    fun salvarAcerto(clienteId: Long, dadosAcerto: DadosAcerto, metodosPagamento: Map<String, Double>) {
        viewModelScope.launch {
            try {
                Log.d("SettlementViewModel", "Salvando acerto com clienteId=$clienteId, mesas=${dadosAcerto.mesas.map { it.numero }}")
                val acerto = Acerto(
                    clienteId = clienteId,
                    colaboradorId = null, // TODO: preencher com usuário logado
                    periodoInicio = java.util.Date(), // TODO: ajustar datas reais
                    periodoFim = java.util.Date(),
                    totalMesas = dadosAcerto.mesas.size.toDouble(),
                    debitoAnterior = 0.0, // TODO: buscar débito real
                    valorTotal = 0.0, // TODO: calcular valor total
                    desconto = 0.0, // TODO: pegar desconto
                    valorComDesconto = 0.0, // TODO: calcular
                    valorRecebido = metodosPagamento.values.sum(),
                    debitoAtual = 0.0, // TODO: calcular
                    status = com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO,
                    observacoes = dadosAcerto.observacao
                )
                acertoRepository.inserir(acerto)
                _resultadoSalvamento.value = Result.success(Unit)
            } catch (e: Exception) {
                Log.e("SettlementViewModel", "Erro ao salvar acerto: ${e.localizedMessage}", e)
                _resultadoSalvamento.value = Result.failure(e)
            }
        }
    }

    fun resetarResultadoSalvamento() {
        _resultadoSalvamento.value = null
    }
} 
