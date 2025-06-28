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
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.MesaRepository
import com.example.gestaobilhares.data.repositories.ClienteRepository
import kotlinx.coroutines.flow.collect
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

/**
 * ViewModel para ClientDetailFragment
 * FASE 4A - Implementação crítica com dados mock
 */
@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    private val clienteRepository: ClienteRepository,
    private val mesaRepository: MesaRepository
) : ViewModel() {

    private val _clientDetails = MutableStateFlow<ClienteResumo?>(null)
    val clientDetails: StateFlow<ClienteResumo?> = _clientDetails.asStateFlow()

    private val _settlementHistory = MutableStateFlow<List<AcertoResumo>>(emptyList())
    val settlementHistory: StateFlow<List<AcertoResumo>> = _settlementHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _mesasCliente = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasCliente: StateFlow<List<Mesa>> = _mesasCliente.asStateFlow()

    private val _mesasDisponiveis = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasDisponiveis: StateFlow<List<Mesa>> = _mesasDisponiveis.asStateFlow()

    fun loadClientDetails(clienteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cliente = clienteRepository.obterPorId(clienteId)
                cliente?.let {
                    _clientDetails.value = ClienteResumo(
                        id = it.id,
                        nome = it.nome,
                        endereco = it.endereco ?: "",
                        telefone = it.telefone ?: "",
                        mesasAtivas = 0, // Atualizado abaixo
                        ultimaVisita = "-", // TODO: Buscar última visita real
                        observacoes = it.observacoes ?: ""
                    )
                    mesaRepository.obterMesasPorCliente(clienteId).collect { mesas ->
                        _mesasCliente.value = mesas
                        _clientDetails.value = _clientDetails.value?.copy(mesasAtivas = mesas.size)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun adicionarMesaAoCliente(mesaId: Long, clienteId: Long) {
        viewModelScope.launch {
            mesaRepository.vincularMesa(mesaId, clienteId)
            loadClientDetails(clienteId)
        }
    }

    fun retirarMesaDoCliente(mesaId: Long, clienteId: Long, relogioFinal: Int, valorRecebido: Double) {
        viewModelScope.launch {
            mesaRepository.desvincularMesa(mesaId)
            // TODO: Salvar relogioFinal e valorRecebido no histórico de retirada, se necessário
            loadClientDetails(clienteId)
        }
    }

    fun loadMesasDisponiveis() {
        viewModelScope.launch {
            mesaRepository.obterMesasDisponiveis().collect { mesas ->
                _mesasDisponiveis.value = mesas
            }
        }
    }

    fun isAdminUser(): Boolean {
        // TODO: Implementar checagem real de permissão do usuário logado
        // Exemplo mock: return true para admin, false para user comum
        return true // Trocar para lógica real
    }

    fun salvarObservacaoCliente(clienteId: Long, observacao: String) {
        viewModelScope.launch {
            try {
                clienteRepository.atualizarObservacao(clienteId, observacao)
                loadClientDetails(clienteId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Adiciona um novo acerto ao histórico do cliente e reordena do mais recente para o mais antigo.
     */
    fun adicionarAcertoNoHistorico(novoAcerto: AcertoResumo) {
        val listaAtual = _settlementHistory.value.toMutableList()
        listaAtual.add(0, novoAcerto) // Adiciona no topo (mais recente)
        _settlementHistory.value = listaAtual
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

@Parcelize
data class AcertoResumo(
    val id: Long,
    val data: String,
    val valor: Double,
    val status: String,
    val mesasAcertadas: Int
) : Parcelable 
