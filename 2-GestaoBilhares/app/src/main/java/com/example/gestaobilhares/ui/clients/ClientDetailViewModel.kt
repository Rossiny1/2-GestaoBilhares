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
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.TipoMesa
import android.util.Log

/**
 * ViewModel para ClientDetailFragment
 * FASE 4A - Implementação crítica com dados mock
 */
@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    private val clienteRepository: ClienteRepository,
    private val mesaRepository: MesaRepository,
    private val acertoRepository: AcertoRepository
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

    init {
        // MOCK: Adiciona acertos fictícios ao histórico para teste visual
        _settlementHistory.value = listOf(
            AcertoResumo(
                id = 1L,
                data = "28/06/2025 10:00",
                valor = 150.0,
                status = "Pago",
                mesasAcertadas = 2
            ),
            AcertoResumo(
                id = 2L,
                data = "21/06/2025 09:30",
                valor = 120.0,
                status = "Pago",
                mesasAcertadas = 1
            )
        )
    }

    fun loadClientDetails(clienteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("ClientDetailViewModel", "=== CARREGANDO DETALHES DO CLIENTE $clienteId ===")
            
            try {
                val cliente = clienteRepository.obterPorId(clienteId)
                cliente?.let {
                    Log.d("ClientDetailViewModel", "Cliente encontrado: ${it.nome}")
                    
                    _clientDetails.value = ClienteResumo(
                        id = it.id,
                        nome = it.nome,
                        endereco = it.endereco ?: "",
                        telefone = it.telefone ?: "",
                        mesasAtivas = 0, // Atualizado abaixo
                        ultimaVisita = "-", // TODO: Buscar última visita real
                        observacoes = it.observacoes ?: ""
                    )
                    
                    // Tentar carregar mesas reais do banco
                    mesaRepository.obterMesasPorCliente(clienteId).collect { mesas ->
                        Log.d("ClientDetailViewModel", "Mesas reais encontradas: ${mesas.size}")
                        
                        // Se não houver mesas reais, usar dados mock para teste
                        val mesasParaUsar = if (mesas.isEmpty()) {
                            Log.d("ClientDetailViewModel", "Nenhuma mesa real encontrada, usando dados MOCK")
                            listOf(
                                Mesa(
                                    id = clienteId * 100 + 1,
                                    numero = "M${clienteId}A",
                                    tipoMesa = TipoMesa.SINUCA,
                                    clienteId = clienteId,
                                    ativa = true,
                                    fichasInicial = 1000,
                                    fichasFinal = 0
                                ),
                                Mesa(
                                    id = clienteId * 100 + 2,
                                    numero = "M${clienteId}B",
                                    tipoMesa = TipoMesa.POOL,
                                    clienteId = clienteId,
                                    ativa = true,
                                    fichasInicial = 850,
                                    fichasFinal = 0
                                )
                            )
                        } else {
                            Log.d("ClientDetailViewModel", "Usando mesas reais do banco")
                            mesas
                        }
                        
                        _mesasCliente.value = mesasParaUsar
                        _clientDetails.value = _clientDetails.value?.copy(mesasAtivas = mesasParaUsar.size)
                        
                        Log.d("ClientDetailViewModel", "Mesas carregadas: ${mesasParaUsar.size}")
                        mesasParaUsar.forEachIndexed { index, mesa ->
                            Log.d("ClientDetailViewModel", "Mesa $index: ${mesa.numero} (ID: ${mesa.id}, Tipo: ${mesa.tipoMesa})")
                        }
                    }
                    
                    // Buscar histórico real
                    loadSettlementHistory(clienteId)
                }
            } catch (e: Exception) {
                Log.e("ClientDetailViewModel", "Erro ao carregar detalhes do cliente", e)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
                Log.d("ClientDetailViewModel", "=== CARREGAMENTO CONCLUÍDO ===")
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

    fun loadSettlementHistory(clienteId: Long) {
        viewModelScope.launch {
            acertoRepository.buscarPorCliente(clienteId).collect { acertos ->
                _settlementHistory.value = acertos.map { acerto ->
                    AcertoResumo(
                        id = acerto.id,
                        data = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", acerto.dataAcerto).toString(),
                        valor = acerto.valorRecebido,
                        status = acerto.status.name,
                        mesasAcertadas = acerto.totalMesas.toInt()
                    )
                }
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

@Parcelize
data class AcertoResumo(
    val id: Long,
    val data: String,
    val valor: Double,
    val status: String,
    val mesasAcertadas: Int
) : Parcelable 
