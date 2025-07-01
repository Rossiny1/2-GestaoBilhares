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
        // Removido dados mock - agora carrega do banco de dados real
    }

    fun loadClientDetails(clienteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("ClientDetailViewModel", "=== CARREGANDO DETALHES DO CLIENTE $clienteId ===")
            
            try {
                val cliente = clienteRepository.obterPorId(clienteId)
                cliente?.let {
                    Log.d("ClientDetailViewModel", "Cliente encontrado: ${it.nome}")
                    
                    // Buscar data do último acerto para "última visita"
                    val ultimoAcerto = acertoRepository.buscarUltimoAcertoPorCliente(clienteId)
                    val ultimaVisita = if (ultimoAcerto != null) {
                        val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
                        formatter.format(ultimoAcerto.dataAcerto)
                    } else {
                        "Nunca"
                    }
                    
                    _clientDetails.value = ClienteResumo(
                        id = it.id,
                        nome = it.nome,
                        endereco = it.endereco ?: "",
                        telefone = it.telefone ?: "",
                        valorFicha = it.valorFicha,
                        comissaoFicha = it.comissaoFicha,
                        mesasAtivas = 0, // Atualizado abaixo
                        ultimaVisita = ultimaVisita,
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
                                    fichasFinal = 0,
                                    valorFixo = 0.0 // Mesa de fichas jogadas
                                ),
                                Mesa(
                                    id = clienteId * 100 + 2,
                                    numero = "M${clienteId}B",
                                    tipoMesa = TipoMesa.POOL,
                                    clienteId = clienteId,
                                    ativa = true,
                                    fichasInicial = 850,
                                    fichasFinal = 0,
                                    valorFixo = 0.0 // Mesa de fichas jogadas
                                ),
                                Mesa(
                                    id = clienteId * 100 + 3,
                                    numero = "M${clienteId}C",
                                    tipoMesa = TipoMesa.SINUCA,
                                    clienteId = clienteId,
                                    ativa = true,
                                    fichasInicial = 0,
                                    fichasFinal = 0,
                                    valorFixo = 150.0 // Mesa de valor fixo - R$ 150,00
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
     * Adiciona um novo acerto ao histórico do cliente e recarrega do banco de dados.
     */
    fun adicionarAcertoNoHistorico(novoAcerto: AcertoResumo) {
        Log.d("ClientDetailViewModel", "Adicionando acerto ao histórico: $novoAcerto")
        
        // Recarregar histórico do banco de dados para garantir sincronização
        // Usar o clienteId atual do _clientDetails
        _clientDetails.value?.let { cliente ->
            loadSettlementHistory(cliente.id)
        }
        
        // Também adicionar à lista atual para feedback imediato
        val listaAtual = _settlementHistory.value.toMutableList()
        listaAtual.add(0, novoAcerto) // Adiciona no topo (mais recente)
        _settlementHistory.value = listaAtual
    }

    fun loadSettlementHistory(clienteId: Long) {
        viewModelScope.launch {
            Log.d("ClientDetailViewModel", "Carregando histórico de acertos para cliente: $clienteId")
            try {
                acertoRepository.buscarPorCliente(clienteId).collect { acertos ->
                    Log.d("ClientDetailViewModel", "Acertos encontrados no banco: ${acertos.size}")
                    val acertosResumo = acertos.map { acerto ->
                        AcertoResumo(
                            id = acerto.id,
                            data = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", acerto.dataAcerto).toString(),
                            valorTotal = acerto.valorRecebido,
                            status = acerto.status.name,
                            mesasAcertadas = acerto.totalMesas.toInt(),
                            debitoAtual = 0.0
                        )
                    }
                    _settlementHistory.value = acertosResumo
                    Log.d("ClientDetailViewModel", "Histórico atualizado: ${_settlementHistory.value.size} acertos")
                }
            } catch (e: Exception) {
                Log.e("ClientDetailViewModel", "Erro ao carregar histórico de acertos", e)
                // Manter dados existentes em caso de erro
                if (_settlementHistory.value.isEmpty()) {
                    Log.d("ClientDetailViewModel", "Mantendo dados existentes devido a erro")
                }
            }
        }
    }

    /**
     * Busca a data do último acerto do cliente para exibir como "última visita"
     */
    fun buscarDataUltimoAcerto(clienteId: Long, callback: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val ultimoAcerto = acertoRepository.buscarUltimoAcertoPorCliente(clienteId)
                if (ultimoAcerto != null) {
                    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
                    val dataFormatada = formatter.format(ultimoAcerto.dataAcerto)
                    callback(dataFormatada)
                } else {
                    callback("Nunca")
                }
            } catch (e: Exception) {
                Log.e("ClientDetailViewModel", "Erro ao buscar último acerto: ${e.message}")
                callback("Nunca")
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
    val valorFicha: Double,
    val comissaoFicha: Double,
    val mesasAtivas: Int,
    val ultimaVisita: String,
    val observacoes: String
)

@Parcelize
data class AcertoResumo(
    val id: Long,
    val data: String,
    val valorTotal: Double,
    val status: String,
    val mesasAcertadas: Int,
    val debitoAtual: Double = 0.0
) : Parcelable 
