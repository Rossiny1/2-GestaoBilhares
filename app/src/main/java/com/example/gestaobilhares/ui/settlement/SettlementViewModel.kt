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
import kotlinx.coroutines.flow.first
import com.example.gestaobilhares.data.repository.AcertoMesaRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ViewModel para SettlementFragment
 * FASE 4A - Implementação básica para desbloqueio
 */
@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val mesaRepository: MesaRepository,
    private val clienteRepository: ClienteRepository,
    private val acertoRepository: AcertoRepository,
    private val acertoMesaRepository: AcertoMesaRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _clientName = MutableStateFlow("")
    val clientName: StateFlow<String> = _clientName.asStateFlow()

    private val _clientAddress = MutableStateFlow("")
    val clientAddress: StateFlow<String> = _clientAddress.asStateFlow()

    private val _mesasCliente = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasCliente: StateFlow<List<Mesa>> = _mesasCliente.asStateFlow()

    private val _resultadoSalvamento = MutableStateFlow<Result<Long>?>(null)
    val resultadoSalvamento: StateFlow<Result<Long>?> = _resultadoSalvamento.asStateFlow()

    private val _historicoAcertos = MutableStateFlow<List<Acerto>>(emptyList())
    val historicoAcertos: StateFlow<List<Acerto>> = _historicoAcertos.asStateFlow()

    private val _debitoAnterior = MutableStateFlow(0.0)
    val debitoAnterior: StateFlow<Double> = _debitoAnterior.asStateFlow()

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

    /**
     * Prepara as mesas para acerto, definindo relógios iniciais baseados no último acerto
     */
    suspend fun prepararMesasParaAcerto(mesasCliente: List<Mesa>): List<Mesa> {
        return mesasCliente.map { mesa ->
            try {
                // Buscar o último acerto desta mesa
                val ultimoAcertoMesa = acertoMesaRepository.buscarUltimoAcertoMesa(mesa.id)
                
                if (ultimoAcertoMesa != null) {
                    // Usar o relógio final do último acerto como inicial do próximo
                    mesa.copy(fichasInicial = ultimoAcertoMesa.relogioFinal)
                } else {
                    // Primeiro acerto - usar relógio inicial cadastrado ou 0
                    mesa.copy(fichasInicial = mesa.fichasInicial ?: 0)
                }
            } catch (e: Exception) {
                Log.e("SettlementViewModel", "Erro ao preparar mesa ${mesa.numero}: ${e.message}")
                mesa.copy(fichasInicial = mesa.fichasInicial ?: 0)
            }
        }
    }

    fun carregarDadosCliente(clienteId: Long, callback: (com.example.gestaobilhares.data.entities.Cliente?) -> Unit) {
        viewModelScope.launch {
            try {
                val cliente = clienteRepository.obterPorId(clienteId)
                callback(cliente)
            } catch (e: Exception) {
                Log.e("SettlementViewModel", "Erro ao carregar dados do cliente: ${e.localizedMessage}", e)
                callback(null)
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
     * ✅ FUNÇÃO FALLBACK: Carrega mesas diretamente sem usar Flow
     */
    suspend fun carregarMesasClienteDireto(clienteId: Long): List<Mesa> {
        return try {
            Log.d("SettlementViewModel", "Carregando mesas diretamente para cliente $clienteId")
            mesaRepository.obterMesasPorClienteDireto(clienteId)
        } catch (e: Exception) {
            Log.e("SettlementViewModel", "Erro ao carregar mesas direto: ${e.message}")
            emptyList()
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
     * Busca o débito atual do último acerto do cliente para usar como débito anterior
     */
    fun buscarDebitoAnterior(clienteId: Long) {
        viewModelScope.launch {
            try {
                val ultimoAcerto = acertoRepository.buscarUltimoAcertoPorCliente(clienteId)
                if (ultimoAcerto != null) {
                    _debitoAnterior.value = ultimoAcerto.debitoAtual
                    Log.d("SettlementViewModel", "Débito anterior carregado: R$ ${ultimoAcerto.debitoAtual}")
                } else {
                    _debitoAnterior.value = 0.0
                    Log.d("SettlementViewModel", "Nenhum acerto anterior encontrado, débito anterior: R$ 0,00")
                }
            } catch (e: Exception) {
                Log.e("SettlementViewModel", "Erro ao buscar débito anterior: ${e.message}")
                _debitoAnterior.value = 0.0
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
                
                // Calcular valores do acerto
                val valorRecebido = metodosPagamento.values.sum()
                val debitoAnterior = _debitoAnterior.value
                val valorTotal = dadosAcerto.mesas.sumOf { mesa ->
                    if (mesa.valorFixo > 0) {
                        mesa.valorFixo // Mesa de valor fixo
                    } else {
                        // Mesa de fichas jogadas - calcular baseado no cliente
                        val cliente = clienteRepository.obterPorId(clienteId)
                        val fichasJogadas = (mesa.fichasFinal - mesa.fichasInicial).coerceAtLeast(0)
                        fichasJogadas * (cliente?.comissaoFicha ?: 0.0)
                    }
                }
                val desconto = 0.0 // TODO: pegar desconto do formulário
                val valorComDesconto = valorTotal - desconto
                val debitoAtual = debitoAnterior + valorComDesconto - valorRecebido
                
                val metodosPagamentoJson = Gson().toJson(metodosPagamento)
                val acerto = Acerto(
                    clienteId = clienteId,
                    colaboradorId = null, // TODO: preencher com usuário logado
                    periodoInicio = java.util.Date(), // TODO: ajustar datas reais
                    periodoFim = java.util.Date(),
                    totalMesas = dadosAcerto.mesas.size.toDouble(),
                    debitoAnterior = debitoAnterior,
                    valorTotal = valorTotal,
                    desconto = desconto,
                    valorComDesconto = valorComDesconto,
                    valorRecebido = valorRecebido,
                    debitoAtual = debitoAtual,
                    status = com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO,
                    observacoes = dadosAcerto.observacao,
                    metodosPagamentoJson = metodosPagamentoJson
                )
                
                val acertoId = acertoRepository.inserir(acerto)
                Log.d("SettlementViewModel", "Acerto salvo com ID: $acertoId")
                
                // Salvar dados detalhados de cada mesa do acerto
                val cliente = clienteRepository.obterPorId(clienteId)
                val acertoMesas = dadosAcerto.mesas.map { mesa ->
                    val fichasJogadas = if (mesa.valorFixo > 0) {
                        0 // Mesa de valor fixo não tem fichas jogadas
                    } else {
                        (mesa.fichasFinal - mesa.fichasInicial).coerceAtLeast(0)
                    }
                    
                    val subtotal = if (mesa.valorFixo > 0) {
                        mesa.valorFixo
                    } else {
                        fichasJogadas * (cliente?.comissaoFicha ?: 0.0)
                    }
                    
                    com.example.gestaobilhares.data.entities.AcertoMesa(
                        acertoId = acertoId,
                        mesaId = mesa.id,
                        relogioInicial = mesa.fichasInicial ?: 0,
                        relogioFinal = mesa.fichasFinal ?: 0,
                        fichasJogadas = fichasJogadas,
                        valorFixo = mesa.valorFixo ?: 0.0,
                        valorFicha = cliente?.valorFicha ?: 0.0,
                        comissaoFicha = cliente?.comissaoFicha ?: 0.0,
                        subtotal = subtotal,
                        comDefeito = false, // TODO: pegar do formulário
                        observacoes = null
                    )
                }
                
                acertoMesaRepository.inserirLista(acertoMesas)
                Log.d("SettlementViewModel", "Dados de ${acertoMesas.size} mesas salvos para o acerto $acertoId")
                
                _resultadoSalvamento.value = Result.success(acertoId)
            } catch (e: Exception) {
                Log.e("SettlementViewModel", "Erro ao salvar acerto: ${e.localizedMessage}", e)
                _resultadoSalvamento.value = Result.failure(e)
            }
        }
    }

    fun resetarResultadoSalvamento() {
        _resultadoSalvamento.value = null
    }
    
    fun limparResultadoSalvamento() {
        _resultadoSalvamento.value = null
    }

    suspend fun buscarAcertoPorId(acertoId: Long): Acerto? {
        return acertoRepository.buscarPorId(acertoId)
    }

    suspend fun buscarMesasDoAcerto(acertoId: Long): List<com.example.gestaobilhares.data.entities.AcertoMesa> {
        return acertoMesaRepository.buscarPorAcertoId(acertoId)
    }

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }
} 