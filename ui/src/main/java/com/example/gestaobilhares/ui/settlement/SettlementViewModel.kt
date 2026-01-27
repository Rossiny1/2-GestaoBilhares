package com.example.gestaobilhares.ui.settlement

import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.PanoEstoque
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.ui.common.BaseViewModel
import com.example.gestaobilhares.ui.mesas.usecases.OrigemTrocaPano
import com.example.gestaobilhares.ui.mesas.usecases.RegistrarTrocaPanoUseCase
import com.example.gestaobilhares.ui.mesas.usecases.TrocaPanoParams
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel para SettlementFragment
 * FASE 4A - Implementação básica para desbloqueio
 */
@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val registrarTrocaPanoUseCase: RegistrarTrocaPanoUseCase,
    private val userSessionManager: UserSessionManager
) : BaseViewModel() {

    /**
     * ✅ NOVA CLASSE: Resultado específico para salvamento de acerto
     */
    sealed class ResultadoSalvamento {
        data class Sucesso(val acertoId: Long) : ResultadoSalvamento()
        data class Erro(val mensagem: String) : ResultadoSalvamento()
        data class AcertoJaExiste(val acertoExistente: Acerto) : ResultadoSalvamento()
    }

    // Estados de loading e error já estão no BaseViewModel

    private val _clientName = MutableStateFlow("")
    val clientName: StateFlow<String> = _clientName.asStateFlow()

    private val _clientAddress = MutableStateFlow("")
    val clientAddress: StateFlow<String> = _clientAddress.asStateFlow()

    private val _mesasCliente = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasCliente: StateFlow<List<Mesa>> = _mesasCliente.asStateFlow()

    private val _resultadoSalvamento = MutableStateFlow<ResultadoSalvamento?>(null)
    val resultadoSalvamento: StateFlow<ResultadoSalvamento?> = _resultadoSalvamento.asStateFlow()

    private val _historicoAcertos = MutableStateFlow<List<Acerto>>(emptyList())
    val historicoAcertos: StateFlow<List<Acerto>> = _historicoAcertos.asStateFlow()

    private val _debitoAnterior = MutableStateFlow(0.0)
    val debitoAnterior: StateFlow<Double> = _debitoAnterior.asStateFlow()

    data class DadosAcerto(
        val mesas: List<MesaAcerto>,
        val representante: String,
        val panoTrocado: Boolean,
        val numeroPano: String?,
        val tipoAcerto: String,
        val observacao: String,
        val justificativa: String?,
        val metodosPagamento: Map<String, Double>
    )
    
    /**
     * ✅ NOVO: Classe específica para mesas no acerto, incluindo campo comDefeito
     */
    data class MesaAcerto(
        val id: Long,
        val numero: String,
        val relogioInicial: Int,
        val relogioFinal: Int,
        val valorFixo: Double = 0.0,
        val tipoMesa: com.example.gestaobilhares.data.entities.TipoMesa,
        val comDefeito: Boolean = false,
        val relogioReiniciou: Boolean = false,
        val mediaFichasJogadas: Double = 0.0,
        // ✅ NOVO: Campos para fotos
        val fotoRelogioFinal: String? = null,
        val dataFoto: java.util.Date? = null,
        // ✅ NOVO: Campo para identificar troca de pano (fonte da verdade)
        val panoNovoId: Long? = null
    )

    private val _clienteId = MutableStateFlow<Long?>(null)
    val clienteId: StateFlow<Long?> = _clienteId.asStateFlow()

    fun loadClientForSettlement(clienteId: Long) {
        _clienteId.value = clienteId
        viewModelScope.launch {
                showLoading()
            try {
                val cliente = appRepository.obterClientePorId(clienteId)
                if (cliente != null) {
                    _clientName.value = cliente.nome
                    _clientAddress.value = cliente.endereco ?: "---"
                    logOperation("SETTLEMENT", "Nome do cliente carregado: ${cliente.nome}, endereço: ${cliente.endereco}")
                } else {
                    _clientName.value = "Cliente não encontrado"
                    _clientAddress.value = "---"
                    logOperation("SETTLEMENT", "Cliente não encontrado para ID: $clienteId")
                }
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Erro ao carregar cliente ID: $clienteId")
                _clientName.value = "Erro ao carregar cliente"
                _clientAddress.value = "---"
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * ✅ FUNÇÃO CRÍTICA: Prepara as mesas para acerto, definindo relógios iniciais
     * @param mesasCliente Lista de mesas do cliente
     * @param acertoIdParaEdicao ID do acerto sendo editado (null se for novo acerto)
     */
    suspend fun prepararMesasParaAcerto(mesasCliente: List<Mesa>, acertoIdParaEdicao: Long? = null): List<Mesa> {
        Timber.d("SettlementViewModel", "=== PREPARANDO MESAS PARA ACERTO ===")
        Timber.d("SettlementViewModel", "Mesas recebidas: ${mesasCliente.size}, Modo edição: ${acertoIdParaEdicao != null}")
        Timber.d("SettlementViewModel", "Acerto ID para edição: $acertoIdParaEdicao")
        
        return mesasCliente.map { mesa ->
            try {
                logOperation("SETTLEMENT", "Processando mesa ${mesa.numero} (ID: ${mesa.id})")
                
                if (acertoIdParaEdicao != null) {
                    // ✅ MODO EDIÇÃO: Carregar dados do acerto sendo editado
                    logOperation("SETTLEMENT", "Mesa ${mesa.numero}: Buscando dados do acerto ID: $acertoIdParaEdicao")
                    val acertoMesas = appRepository.buscarAcertoMesasPorAcerto(acertoIdParaEdicao)
                    val acertoMesa = acertoMesas.find { it.mesaId == mesa.id }
                    if (acertoMesa != null) {
                        // Usar o relógio inicial e final do acerto sendo editado
                        val relogioInicial = acertoMesa.relogioInicial
                        val relogioFinal = acertoMesa.relogioFinal
                        logOperation("SETTLEMENT", "Mesa ${mesa.numero}: MODO EDIÇÃO - relógio inicial: $relogioInicial, relógio final: $relogioFinal")
                        logOperation("SETTLEMENT", "Mesa ${mesa.numero}: AcertoMesa encontrado - ID: ${acertoMesa.id}, AcertoID: ${acertoMesa.acertoId}")
                        mesa.copy(
                            relogioInicial = relogioInicial,
                            relogioFinal = relogioFinal
                        )
                    } else {
                        // Fallback: usar dados da mesa
                        val relogioInicial = mesa.relogioInicial
                        logOperation("SETTLEMENT", "Mesa ${mesa.numero}: MODO EDIÇÃO - acerto não encontrado, usando dados da mesa: $relogioInicial")
                        mesa.copy(relogioInicial = relogioInicial)
                    }
                } else {
                    // ✅ MODO NOVO ACERTO: Usar lógica original
                    val ultimoAcertoMesa = appRepository.buscarUltimoAcertoMesaItem(mesa.id)
                    
                    if (ultimoAcertoMesa != null) {
                        // Usar o relógio final do último acerto como inicial do próximo
                        val relogioInicial = ultimoAcertoMesa.relogioFinal
                        logOperation("SETTLEMENT", "Mesa ${mesa.numero}: MODO NOVO ACERTO - relógio final: ${ultimoAcertoMesa.relogioFinal} -> novo relógio inicial: $relogioInicial")
                        mesa.copy(relogioInicial = relogioInicial)
                    } else {
                        // Primeiro acerto - usar relógio inicial cadastrado ou 0
                        val relogioInicial = mesa.relogioInicial
                        logOperation("SETTLEMENT", "Mesa ${mesa.numero}: MODO NOVO ACERTO - primeiro acerto, usando relógio inicial cadastrado: $relogioInicial")
                        mesa.copy(relogioInicial = relogioInicial)
                    }
                }
            } catch (e: Exception) {
                logError("SETTLEMENT", "Erro ao preparar mesa ${mesa.numero}: ${e.message}")
                val relogioInicial = mesa.relogioInicial
                mesa.copy(relogioInicial = relogioInicial)
            }
        }.also { mesasPreparadas ->
            Timber.d("SettlementViewModel", "=== MESAS PREPARADAS ===")
            mesasPreparadas.forEach { mesa ->
                logOperation("SETTLEMENT", "Mesa ${mesa.numero}: relógio inicial=${mesa.relogioInicial}, relógio final=${mesa.relogioFinal}")
            }
        }
    }

    fun carregarDadosCliente(clienteId: Long, callback: (com.example.gestaobilhares.data.entities.Cliente?) -> Unit) {
        viewModelScope.launch {
            try {
                val cliente = appRepository.obterClientePorId(clienteId)
                callback(cliente)
            } catch (e: Exception) {
                logError("SETTLEMENT", "Erro ao carregar dados do cliente: ${e.localizedMessage}", e)
                callback(null)
            }
        }
    }

    fun loadMesasCliente(clienteId: Long) {
        viewModelScope.launch {
            appRepository.obterMesasPorCliente(clienteId).collect { mesas: List<Mesa> ->
                _mesasCliente.value = mesas
            }
        }
    }

    /**
     * ✅ FUNÇÃO FALLBACK: Carrega mesas diretamente sem usar Flow
     */
    suspend fun carregarMesasClienteDireto(clienteId: Long): List<Mesa> {
        return try {
            Timber.d("SettlementViewModel", "Carregando mesas diretamente para cliente $clienteId")
            appRepository.obterMesasPorClienteDireto(clienteId)
        } catch (e: Exception) {
            Timber.e("SettlementViewModel", "Erro ao carregar mesas direto: ${e.message}")
            emptyList()
        }
    }

    fun carregarHistoricoAcertos(clienteId: Long) {
        viewModelScope.launch {
            appRepository.obterAcertosPorCliente(clienteId).collect { acertos: List<Acerto> ->
                _historicoAcertos.value = acertos
            }
        }
    }

    /**
     * ✅ FUNÇÃO CRÍTICA: Busca o débito para usar como débito anterior
     * @param clienteId ID do cliente
     * @param acertoIdParaEdicao ID do acerto sendo editado (null se for novo acerto)
     */
    fun buscarDebitoAnterior(clienteId: Long, acertoIdParaEdicao: Long? = null) {
        viewModelScope.launch {
            try {
                logOperation("SETTLEMENT", "🔍 INICIANDO buscarDebitoAnterior - clienteId: $clienteId, acertoIdParaEdicao: $acertoIdParaEdicao")
                
                if (acertoIdParaEdicao != null) {
                    // ✅ MODO EDIÇÃO: Buscar o débito que existia ANTES deste acerto ser criado
                    logOperation("SETTLEMENT", "🔍 MODO EDIÇÃO: Buscando débito anterior ao acerto ID: $acertoIdParaEdicao")
                    
                    // Buscar todos os acertos do cliente ordenados por data
                    val acertosCliente = appRepository.obterAcertosPorCliente(clienteId).first()
                    val acertosOrdenados = acertosCliente.sortedByDescending { acerto -> acerto.dataAcerto }
                    
                    logOperation("SETTLEMENT", "🔍 MODO EDIÇÃO: Encontrados ${acertosOrdenados.size} acertos do cliente")
                    
                    // Encontrar o acerto sendo editado
                    val acertoParaEdicao = acertosOrdenados.find { acerto -> acerto.id == acertoIdParaEdicao }
                    
                    if (acertoParaEdicao != null) {
                        // ✅ CORREÇÃO CRÍTICA: Para o primeiro acerto, usar o debitoAnterior salvo no próprio acerto
                        if (acertosOrdenados.size == 1) {
                            logOperation("SETTLEMENT", "ℹ️ MODO EDIÇÃO: Este é o PRIMEIRO acerto do cliente - usando debitoAnterior salvo: ${acertoParaEdicao.debitoAnterior}")
                            _debitoAnterior.value = acertoParaEdicao.debitoAnterior
                            logOperation("SETTLEMENT", "✅ MODO EDIÇÃO: Débito anterior do primeiro acerto: R$ ${acertoParaEdicao.debitoAnterior}")
                        } else {
                            // Para acertos subsequentes, encontrar o acerto ANTERIOR ao que está sendo editado
                            val acertoAnterior = acertosOrdenados.find { acerto -> 
                                acerto.dataAcerto < acertoParaEdicao.dataAcerto 
                            }
                            
                            if (acertoAnterior != null) {
                                logOperation("SETTLEMENT", "✅ MODO EDIÇÃO: Acerto anterior encontrado - ID: ${acertoAnterior.id}, Débito Atual: ${acertoAnterior.debitoAtual}")
                                _debitoAnterior.value = acertoAnterior.debitoAtual
                                logOperation("SETTLEMENT", "✅ MODO EDIÇÃO: Débito anterior calculado: R$ ${acertoAnterior.debitoAtual}")
                            } else {
                                logOperation("SETTLEMENT", "ℹ️ MODO EDIÇÃO: Nenhum acerto anterior encontrado, usando debitoAnterior salvo: ${acertoParaEdicao.debitoAnterior}")
                                _debitoAnterior.value = acertoParaEdicao.debitoAnterior
                            }
                        }
                    } else {
                        logError("SETTLEMENT", "❌ MODO EDIÇÃO: Acerto para edição não encontrado, débito anterior: R$ 0,00")
                        _debitoAnterior.value = 0.0
                    }
                } else {
                    // ✅ MODO NOVO ACERTO: Usar débito do último acerto como anterior
                    logOperation("SETTLEMENT", "🔍 MODO NOVO ACERTO: Buscando último acerto do cliente: $clienteId")
                    val ultimoAcerto = appRepository.buscarUltimoAcertoPorCliente(clienteId)
                    
                    if (ultimoAcerto != null) {
                        logOperation("SETTLEMENT", "✅ MODO NOVO ACERTO: Último acerto encontrado - ID: ${ultimoAcerto.id}, Débito Atual: ${ultimoAcerto.debitoAtual}")
                        _debitoAnterior.value = ultimoAcerto.debitoAtual
                        logOperation("SETTLEMENT", "✅ MODO NOVO ACERTO: Débito anterior carregado: R$ ${ultimoAcerto.debitoAtual}")
                    } else {
                        logOperation("SETTLEMENT", "ℹ️ MODO NOVO ACERTO: Nenhum acerto anterior encontrado, débito anterior: R$ 0,00")
                        _debitoAnterior.value = 0.0
                    }
                }
                
                logOperation("SETTLEMENT", "🔍 FINALIZANDO buscarDebitoAnterior - Valor final: R$ ${_debitoAnterior.value}")
                
            } catch (e: Exception) {
                logError("SETTLEMENT", "❌ Erro ao buscar débito anterior: ${e.message}")
                _debitoAnterior.value = 0.0
            }
        }
    }

    /**
     * Salva o acerto, agora recebendo os valores discriminados por método de pagamento.
     * @param clienteId ID do cliente
     * @param dadosAcerto Dados principais do acerto
     * @param metodosPagamento Mapa de método para valor recebido
     * @param desconto Valor do desconto aplicado
     * @param acertoIdParaEdicao ID do acerto sendo editado (null se for novo acerto)
     */
    fun salvarAcerto(clienteId: Long, dadosAcerto: DadosAcerto, metodosPagamento: Map<String, Double>, desconto: Double = 0.0, acertoIdParaEdicao: Long? = null) {
        viewModelScope.launch {
                showLoading()
            try {
                logOperation("SETTLEMENT", "Salvando acerto com clienteId=$clienteId, mesas=${dadosAcerto.mesas.map { it.numero }}, modoEdicao=${acertoIdParaEdicao != null}")
                
                // Buscar cliente uma única vez
                val cliente = appRepository.obterClientePorId(clienteId) ?: throw IllegalStateException("Cliente não encontrado para o ID: $clienteId")
                // ✅ CORREÇÃO: rotaId é Long (não nullable), elvis operator desnecessário
                val rotaId = cliente.rotaId
                
                // ✅ CORREÇÃO CRÍTICA: Buscar APENAS ciclo EM_ANDAMENTO (não último finalizado)
                val cicloAtivo = appRepository.buscarCicloAtivo(rotaId)
                if (cicloAtivo == null) {
                    logError("SETTLEMENT", "❌ ERRO: Nenhum ciclo EM_ANDAMENTO encontrado para a rota $rotaId")
                    _resultadoSalvamento.value = ResultadoSalvamento.Erro("Não há ciclo em andamento para esta rota. Finalize o ciclo anterior e inicie um novo.")
                    hideLoading()
                    return@launch
                }
                
                // ✅ VALIDAÇÃO CRÍTICA: Garantir que o ciclo está realmente EM_ANDAMENTO
                if (cicloAtivo.status != com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO) {
                    logError("SETTLEMENT", "❌ ERRO: Ciclo encontrado não está EM_ANDAMENTO! ID: ${cicloAtivo.id}, Status: ${cicloAtivo.status}, Número: ${cicloAtivo.numeroCiclo}")
                    _resultadoSalvamento.value = ResultadoSalvamento.Erro("O ciclo atual está ${cicloAtivo.status.name.lowercase()}. Apenas ciclos em andamento permitem adição de acertos.")
                    hideLoading()
                    return@launch
                }
                
                val cicloIdEfetivo = cicloAtivo.id
                logOperation("SETTLEMENT", "✅ Acerto será vinculado ao ciclo EM_ANDAMENTO: ID=$cicloIdEfetivo, Número=${cicloAtivo.numeroCiclo}, Status=${cicloAtivo.status}")

                // ✅ CORREÇÃO: Validação apenas para novos acertos (não para edição)
                if (acertoIdParaEdicao == null) {
                    // ✅ DEBUG DETALHADO: Verificar todos os acertos do cliente no ciclo
                    val acertosDoClienteNoCiclo = appRepository.buscarAcertosPorClienteECicloId(clienteId, cicloIdEfetivo).first()
                    logOperation("SETTLEMENT", "🔍 DEBUG VALIDAÇÃO: Cliente $clienteId no ciclo $cicloIdEfetivo")
                    logOperation("SETTLEMENT", "🔍 Total de acertos encontrados: ${acertosDoClienteNoCiclo.size}")
                    
                    acertosDoClienteNoCiclo.forEachIndexed { index, acerto ->
                        logOperation("SETTLEMENT", "🔍 Acerto $index: ID=${acerto.id}, Status=${acerto.status}, Data=${acerto.dataAcerto}")
                    }
                    
                    // Verificar se já existe acerto FINALIZADO para este cliente no ciclo ATUAL
                    // ✅ CORREÇÃO CRÍTICA: Verificar apenas acertos FINALIZADOS (não PENDENTES ou CANCELADOS)
                    val acertoFinalizado = acertosDoClienteNoCiclo.firstOrNull { acerto -> 
                        acerto.status == com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO 
                    }
                    
                    if (acertoFinalizado != null) {
                        logError("SETTLEMENT", "ACERTO JÁ EXISTE: Cliente $clienteId já possui acerto FINALIZADO (ID: ${acertoFinalizado.id}) no ciclo $cicloIdEfetivo")
                        _resultadoSalvamento.value = ResultadoSalvamento.AcertoJaExiste(acertoFinalizado)
                        hideLoading()
                        return@launch
                    }
                    
                    logOperation("SETTLEMENT", "✅ Validação passou: Cliente $clienteId pode criar novo acerto no ciclo $cicloIdEfetivo (nenhum acerto FINALIZADO encontrado)")
                } else {
                    logOperation("SETTLEMENT", "✅ Modo edição ativo (acertoId: $acertoIdParaEdicao). Pulando validação de acerto único.")
                }

                // ✅ FASE 1: Usar FinancialCalculator centralizado
                val valorRecebido = com.example.gestaobilhares.core.utils.FinancialCalculator.calcularValorRecebido(metodosPagamento)
                val debitoAnterior = _debitoAnterior.value
                
                // Converter mesas para formato do FinancialCalculator
                val mesasCalculo = dadosAcerto.mesas.map { mesa ->
                    com.example.gestaobilhares.core.utils.FinancialCalculator.MesaAcertoCalculo(
                        relogioInicial = mesa.relogioInicial,
                        relogioFinal = mesa.relogioFinal,
                        valorFixo = mesa.valorFixo,
                        comDefeito = mesa.comDefeito,
                        relogioReiniciou = mesa.relogioReiniciou,
                        mediaFichasJogadas = mesa.mediaFichasJogadas
                    )
                }
                
                val valorTotal = com.example.gestaobilhares.core.utils.FinancialCalculator.calcularValorTotalMesas(
                    mesas = mesasCalculo,
                    comissaoFicha = cliente.comissaoFicha
                )
                
                val valorComDesconto = com.example.gestaobilhares.core.utils.FinancialCalculator.calcularValorComDesconto(
                    valorTotal = valorTotal,
                    desconto = desconto
                )
                
                val debitoAtual = com.example.gestaobilhares.core.utils.FinancialCalculator.calcularDebitoAtual(
                    debitoAnterior = debitoAnterior,
                    valorTotal = valorTotal,
                    desconto = desconto,
                    valorRecebido = valorRecebido
                )
                
                // ✅ CORREÇÃO: Logs detalhados para debug do cálculo do débito
                logOperation("SETTLEMENT", "=== CÁLCULO DO DÉBITO ATUAL ===")
                logOperation("SETTLEMENT", "Débito anterior: R$ $debitoAnterior")
                logOperation("SETTLEMENT", "Valor total das mesas: R$ $valorTotal")
                logOperation("SETTLEMENT", "Desconto aplicado: R$ $desconto")
                logOperation("SETTLEMENT", "Valor com desconto: R$ $valorComDesconto")
                logOperation("SETTLEMENT", "Valor recebido: R$ $valorRecebido")
                logOperation("SETTLEMENT", "Débito atual calculado: R$ $debitoAtual")
                logOperation("SETTLEMENT", "Fórmula: $debitoAnterior + $valorComDesconto - $valorRecebido = $debitoAtual")
                
                val metodosPagamentoJson = Gson().toJson(metodosPagamento)
                // ✅ CORREÇÃO: Logs detalhados para debug das observações
                logOperation("SETTLEMENT", "=== SALVANDO ACERTO NO BANCO - DEBUG OBSERVAÇÕES ===")
                logOperation("SETTLEMENT", "Observação recebida dos dados: '${dadosAcerto.observacao}'")
                // ✅ CORREÇÃO: observacao é String (não nullable), verificação == null sempre false - removida
                logOperation("SETTLEMENT", "Observação é vazia? ${dadosAcerto.observacao.isEmpty()}")
                logOperation("SETTLEMENT", "Observação é blank? ${dadosAcerto.observacao.isBlank()}")
                
                // ✅ CORREÇÃO: Observação será apenas manual, sem preenchimento automático
                val observacaoParaSalvar = dadosAcerto.observacao.trim()
                
                logOperation("SETTLEMENT", "Observação que será salva no banco: '$observacaoParaSalvar'")

                // ✅ CORREÇÃO: Criar dados extras JSON para campos adicionais
                val dadosExtras = mapOf(
                    "justificativa" to dadosAcerto.justificativa,
                    "versaoApp" to "1.0.0"
                )
                val dadosExtrasJson = Gson().toJson(dadosExtras)
                
                logOperation("SETTLEMENT", "=== SALVANDO TODOS OS DADOS ===")
                logOperation("SETTLEMENT", "Representante: '${dadosAcerto.representante}'")
                logOperation("SETTLEMENT", "Tipo de acerto: '${dadosAcerto.tipoAcerto}'")
                logOperation("SETTLEMENT", "Pano trocado: ${dadosAcerto.panoTrocado}")
                logOperation("SETTLEMENT", "Número do pano: '${dadosAcerto.numeroPano}'")
                logOperation("SETTLEMENT", "Métodos de pagamento: $metodosPagamento")

                // ✅ CORREÇÃO CRÍTICA: Vínculos com rota e ciclo
                Timber.d("SettlementViewModel", "=== VINCULANDO ACERTO À ROTA E CICLO ===")
                Timber.d("SettlementViewModel", "Cliente ID: $clienteId")
                Timber.d("SettlementViewModel", "Rota ID do cliente: $rotaId")
                Timber.d("SettlementViewModel", "Ciclo atual: $cicloIdEfetivo")
                
                // ✅ CORREÇÃO: Lógica diferente para edição vs. novo acerto
                val acertoId: Long
                if (acertoIdParaEdicao != null) {
                    // MODO EDIÇÃO: Atualizar acerto existente
                    logOperation("SETTLEMENT", "🔄 MODO EDIÇÃO: Atualizando acerto existente ID: $acertoIdParaEdicao")
                    
                    // Buscar acerto existente
                    val acertoExistente = appRepository.obterAcertoPorId(acertoIdParaEdicao)
                    if (acertoExistente == null) {
                        logError("SETTLEMENT", "❌ Acerto para edição não encontrado: ID $acertoIdParaEdicao")
                        _resultadoSalvamento.value = ResultadoSalvamento.Erro("Acerto para edição não encontrado")
                        hideLoading()
                        return@launch
                    }
                    
                    // Atualizar dados do acerto existente
                    val acertoAtualizado = acertoExistente.copy(
                        totalMesas = dadosAcerto.mesas.size.toDouble(),
                        debitoAnterior = debitoAnterior,
                        valorTotal = valorTotal,
                        desconto = desconto,
                        valorComDesconto = valorComDesconto,
                        valorRecebido = valorRecebido,
                        debitoAtual = debitoAtual,
                        observacoes = observacaoParaSalvar,
                        dataFinalizacao = com.example.gestaobilhares.core.utils.DateUtils.obterDataAtual().time,
                        metodosPagamentoJson = metodosPagamentoJson,
                        representante = dadosAcerto.representante,
                        tipoAcerto = dadosAcerto.tipoAcerto,
                        panoTrocado = dadosAcerto.panoTrocado,
                        numeroPano = dadosAcerto.numeroPano,
                        dadosExtrasJson = dadosExtrasJson
                    )
                    
                    appRepository.atualizarAcerto(acertoAtualizado)
                    acertoId = acertoIdParaEdicao
                    logOperation("SETTLEMENT", "✅ Acerto atualizado com sucesso! ID: $acertoId")
                    
                } else {
                    // MODO NOVO ACERTO: Criar novo acerto
                    logOperation("SETTLEMENT", "🆕 MODO NOVO ACERTO: Criando novo acerto")
                    
                    val acerto = Acerto(
                        clienteId = clienteId,
                        colaboradorId = null,
                        periodoInicio = com.example.gestaobilhares.core.utils.DateUtils.obterDataAtual().time,
                        periodoFim = com.example.gestaobilhares.core.utils.DateUtils.obterDataAtual().time,
                        totalMesas = dadosAcerto.mesas.size.toDouble(),
                        debitoAnterior = debitoAnterior,
                        valorTotal = valorTotal,
                        desconto = desconto,
                        valorComDesconto = valorComDesconto,
                        valorRecebido = valorRecebido,
                        debitoAtual = debitoAtual,
                        status = com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO,
                        observacoes = observacaoParaSalvar,
                        dataFinalizacao = com.example.gestaobilhares.core.utils.DateUtils.obterDataAtual().time,
                        metodosPagamentoJson = metodosPagamentoJson,
                        representante = dadosAcerto.representante,
                        tipoAcerto = dadosAcerto.tipoAcerto,
                        panoTrocado = dadosAcerto.panoTrocado,
                        numeroPano = dadosAcerto.numeroPano,
                        dadosExtrasJson = dadosExtrasJson,
                        rotaId = rotaId,
                        cicloId = cicloIdEfetivo
                    )
                    
                    acertoId = appRepository.inserirAcerto(acerto)
                    logOperation("SETTLEMENT", "✅ Novo acerto salvo com ID: $acertoId")
                }
                
                // NOVO: Atualizar valores do ciclo após salvar acerto
                // Buscar todos os acertos e despesas ANTERIORES do ciclo para calcular os totais
                val acertosAnteriores = appRepository.buscarAcertosPorRotaECicloId(rotaId, cicloIdEfetivo).first().filter { it.id != acertoId }
                val despesasDoCiclo = appRepository.buscarDespesasPorCicloId(cicloIdEfetivo).first()

                // ✅ CORREÇÃO: Verificar se realmente foi salvo
                val acertoSalvo = appRepository.obterAcertoPorId(acertoId)
                logOperation("SETTLEMENT", "🔍 VERIFICAÇÃO: Observação no banco após salvamento: '${acertoSalvo?.observacoes}'")

                // Somar os valores anteriores com o valor do acerto ATUAL
                val valorTotalAcertado = acertosAnteriores.sumOf { it.valorRecebido } + (acertoSalvo?.valorRecebido ?: 0.0)
                val valorTotalDespesas = despesasDoCiclo.sumOf { it.valor }
                val clientesAcertados = (acertosAnteriores.map { it.clienteId } + (acertoSalvo?.clienteId ?: 0L)).distinct().size
                
                logOperation("SETTLEMENT", "=== ATUALIZANDO VALORES DO CICLO $cicloIdEfetivo ===")
                logOperation("SETTLEMENT", "Total Acertado: $valorTotalAcertado (Anteriores: ${acertosAnteriores.sumOf { it.valorRecebido }} + Atual: ${acertoSalvo?.valorRecebido})")
                logOperation("SETTLEMENT", "Total Despesas: $valorTotalDespesas")
                logOperation("SETTLEMENT", "Clientes Acertados: $clientesAcertados")

                // ✅ IMPLEMENTADO: Atualizar valores do ciclo usando método existente
                appRepository.atualizarValoresCiclo(cicloIdEfetivo)
                
                // ✅ CORREÇÃO CRÍTICA: Salvar dados detalhados de cada mesa do acerto com logs
                logOperation("SETTLEMENT", "=== SALVANDO MESAS DO ACERTO ===")
                logOperation("SETTLEMENT", "Total de mesas recebidas: ${dadosAcerto.mesas.size}")
                logOperation("SETTLEMENT", "Cliente encontrado: ${cliente.nome}")
                logOperation("SETTLEMENT", "Valor ficha do cliente: R$ ${cliente.valorFicha}")
                logOperation("SETTLEMENT", "Comissão ficha do cliente: R$ ${cliente.comissaoFicha}")
                
                // Garantir que não há duplicidade de mesaId
                val mesaIds = dadosAcerto.mesas.map { it.id }
                val duplicados = mesaIds.groupBy { it }.filter { it.value.size > 1 }.keys
                if (duplicados.isNotEmpty()) {
                    logError("SETTLEMENT", "DUPLICIDADE DETECTADA nos IDs das mesas: $duplicados")
                }
                val mesasUnicas = dadosAcerto.mesas.distinctBy { it.id }
                if (mesasUnicas.size != dadosAcerto.mesas.size) {
                    logError("SETTLEMENT", "Removendo mesas duplicadas antes de salvar. Total antes: ${dadosAcerto.mesas.size}, depois: ${mesasUnicas.size}")
                }
                val acertoMesas = mesasUnicas.mapIndexed { index, mesa ->
                    val fichasJogadas = if (mesa.valorFixo > 0) {
                        0 // Mesa de valor fixo não tem fichas jogadas
                    } else {
                        com.example.gestaobilhares.core.utils.FinancialCalculator.calcularFichasJogadasMesa(
                            com.example.gestaobilhares.core.utils.FinancialCalculator.MesaAcertoCalculo(
                                relogioInicial = mesa.relogioInicial,
                                relogioFinal = mesa.relogioFinal,
                                valorFixo = mesa.valorFixo,
                                comDefeito = mesa.comDefeito,
                                relogioReiniciou = mesa.relogioReiniciou,
                                mediaFichasJogadas = mesa.mediaFichasJogadas
                            )
                        )
                    }
                    
                    val subtotal = if (mesa.valorFixo > 0) {
                        mesa.valorFixo
                    } else {
                        fichasJogadas * (cliente.comissaoFicha)
                    }
                    
                    logOperation("SETTLEMENT", "=== MESA ${index + 1} ===")
                    logOperation("SETTLEMENT", "ID da mesa: ${mesa.id}")
                    logOperation("SETTLEMENT", "Número da mesa: ${mesa.numero}")
                    logOperation("SETTLEMENT", "Relógio inicial: ${mesa.relogioInicial}")
                    logOperation("SETTLEMENT", "Relógio final: ${mesa.relogioFinal}")
                    logOperation("SETTLEMENT", "Fichas jogadas: $fichasJogadas")
                    logOperation("SETTLEMENT", "Valor fixo: R$ ${mesa.valorFixo}")
                    logOperation("SETTLEMENT", "Subtotal calculado: R$ $subtotal")
                    logOperation("SETTLEMENT", "Com defeito: ${mesa.comDefeito}")
                    logOperation("SETTLEMENT", "Relógio reiniciou: ${mesa.relogioReiniciou}")
                    
                    com.example.gestaobilhares.data.entities.AcertoMesa(
                        acertoId = acertoId,
                        mesaId = mesa.id,
                        relogioInicial = mesa.relogioInicial,
                        relogioFinal = mesa.relogioFinal,
                        fichasJogadas = fichasJogadas,
                        valorFixo = mesa.valorFixo,
                        valorFicha = cliente.valorFicha,
                        comissaoFicha = cliente.comissaoFicha,
                        subtotal = subtotal,
                        comDefeito = mesa.comDefeito,
                        relogioReiniciou = mesa.relogioReiniciou,
                        observacoes = null,
                        // ✅ CORREÇÃO CRÍTICA: Incluir campos de foto
                        fotoRelogioFinal = mesa.fotoRelogioFinal,
                        dataFoto = mesa.dataFoto?.time
                    )
                }
                
                logOperation("SETTLEMENT", "=== INSERINDO MESAS NO BANCO ===")
                logOperation("SETTLEMENT", "Total de AcertoMesa a inserir: ${acertoMesas.size}")
                acertoMesas.forEachIndexed { index, acertoMesa ->
                    logOperation("SETTLEMENT", "AcertoMesa ${index + 1}: Mesa ${acertoMesa.mesaId} - Subtotal: R$ ${acertoMesa.subtotal}")
                    logOperation("SETTLEMENT", "   📷 Foto: '${acertoMesa.fotoRelogioFinal}'")
                }
                
                // ✅ CRÍTICO: Inserir mesas
                acertoMesas.forEach { mesa ->
                    val mesaId = appRepository.inserirAcertoMesa(mesa)
                    logOperation("SETTLEMENT", "✅ Mesa ${mesa.mesaId} salva com ID: $mesaId")
                }
                logOperation("SETTLEMENT", "✅ Dados de ${acertoMesas.size} mesas salvos para o acerto $acertoId")
                
                // ✅ CRÍTICO: Atualizar o débito atual na tabela de clientes
                appRepository.atualizarDebitoAtualCliente(clienteId, debitoAtual)
                logOperation("SETTLEMENT", "Débito atual atualizado na tabela clientes: R$ $debitoAtual")
                
                // ════════════════════════════════════════════════════════════════
                // SOLUÇÃO DEFINITIVA: Usar dados estruturados (panoNovoId), não string UI
                // ════════════════════════════════════════════════════════════════

                // 1. FONTE DA VERDADE: Se mesa tem panoNovoId, houve troca
                val mesasComPanoNovo = dadosAcerto.mesas.filter { 
                    it.panoNovoId != null && it.panoNovoId > 0 
                }

                Log.d("DEBUG_FIX", "═══════════════════════════════════════")
                Log.d("DEBUG_FIX", "🔍 DIAGNÓSTICO TROCA DE PANO:")
                Log.d("DEBUG_FIX", "   Flag panoTrocado (UI): ${dadosAcerto.panoTrocado}")
                Log.d("DEBUG_FIX", "   String numeroPano: '${dadosAcerto.numeroPano}'")
                Log.d("DEBUG_FIX", "   Mesas com panoNovoId: ${mesasComPanoNovo.size}")
                mesasComPanoNovo.forEachIndexed { idx, mesa ->
                    Log.d("DEBUG_FIX", "   [$idx] Mesa ${mesa.numero} → panoNovoId=${mesa.panoNovoId}")
                }
                Log.d("DEBUG_FIX", "═══════════════════════════════════════")

                // 2. DECISÃO: Se houver mesas com pano novo, registrar no histórico
                if (mesasComPanoNovo.isNotEmpty()) {
                    Log.d("DEBUG_FIX", "✅ Detectada troca de pano baseada em dados estruturados")

                    try {
                        // 3. EXECUÇÃO SEQUENCIAL (impede cancelamento por lifecycle)
                        // Chamada DIRETA, SEM viewModelScope.launch
                        registrarTrocaPanoNoHistorico(
                            mesas = mesasComPanoNovo.map { mesa ->
                                com.example.gestaobilhares.ui.settlement.MesaDTO(
                                    id = mesa.id,
                                    numero = mesa.numero,
                                    relogioInicial = mesa.relogioInicial,
                                    relogioFinal = mesa.relogioFinal,
                                    tipoMesa = mesa.tipoMesa,
                                    tamanho = com.example.gestaobilhares.data.entities.TamanhoMesa.MEDIA,
                                    estadoConservacao = com.example.gestaobilhares.data.entities.EstadoConservacao.BOM,
                                    valorFixo = mesa.valorFixo,
                                    valorFicha = 0.0,
                                    comissaoFicha = 0.0,
                                    ativa = true
                                )
                            },
                            numeroPano = dadosAcerto.numeroPano ?: ""
                        )

                        Log.d("DEBUG_FIX", "✅ registrarTrocaPanoNoHistorico CONCLUÍDO")

                    } catch (e: Exception) {
                        Log.e("DEBUG_FIX", "❌ ERRO ao registrar troca de pano: ${e.message}", e)
                        // NÃO relançar exceção (não bloquear salvamento financeiro)
                    }
                } else {
                    Log.d("DEBUG_FIX", "ℹ️ Nenhuma mesa com panoNovoId detectada (sem troca)")
                }
                
                // ✅ CORREÇÃO: Emitir resultado APENAS após garantir persistência do pano
                // O diálogo de resumo deve aparecer instantaneamente
                _resultadoSalvamento.value = ResultadoSalvamento.Sucesso(acertoId)
                logOperation("SETTLEMENT", "✅ Resultado de salvamento emitido - diálogo será exibido imediatamente")
                
                // ✅ NOVO: Processar uploads e sync em background (sem bloquear UI)
                // Isso permite que o diálogo apareça imediatamente enquanto o sync acontece em background
                viewModelScope.launch sync@{
                    try {
                        // ✅ CRÍTICO: Aguardar tempo suficiente para garantir que uploads de fotos sejam concluídos
                        // O upload para Firebase Storage pode levar alguns segundos dependendo do tamanho da foto
                        // e da velocidade da conexão
                        logOperation("SETTLEMENT", "⏳ [BACKGROUND] Aguardando uploads de fotos completarem...")
                        kotlinx.coroutines.delay(5000) // Aumentado para 5 segundos para garantir upload completo
                        logOperation("SETTLEMENT", "✅ [BACKGROUND] Delay concluído, criando payload de sincronização...")
                        
                        // ✅ CORREÇÃO CRÍTICA: Adicionar acerto à fila de sync APÓS inserir as mesas
                        // Aguardar mais um pouco para garantir que o cache está populado
                        kotlinx.coroutines.delay(1000)
                        // ✅ IMPLEMENTADO: Adicionar acerto à fila de sync usando método existente
                        val acertoSync = acertoSalvo ?: return@sync
                        appRepository.adicionarAcertoComMesasParaSync(acertoSync, acertoMesas)
                        logOperation("SETTLEMENT", "✅ [BACKGROUND] Acerto $acertoId adicionado à fila de sync com ${acertoMesas.size} mesas")
                        
                    } catch (e: Exception) {
                        Log.e("SETTLEMENT", "Erro no sync: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                logError("SETTLEMENT", "Erro ao salvar acerto: ${e.localizedMessage}", e)
                _resultadoSalvamento.value = ResultadoSalvamento.Erro(e.localizedMessage ?: "Erro desconhecido")
            } finally {
                hideLoading()
            }
        }
    }

    fun resetarResultadoSalvamento() {
        _resultadoSalvamento.value = null
    }

    /**
     * Registra a troca de pano no histórico de manutenção das mesas.
     * Use case agora cuida de TUDO (MesaReformada + Histórico + Atualização da Mesa).
     */
    private suspend fun registrarTrocaPanoNoHistorico(
        mesas: List<com.example.gestaobilhares.ui.settlement.MesaDTO>,
        numeroPano: String
    ) {
        Log.d("DEBUG_CARDS", "")
        Log.d("DEBUG_CARDS", "╔════════════════════════════════════════╗")
        Log.d("DEBUG_CARDS", "║   ACERTO - Registrando Troca de Pano  ║")
        Log.d("DEBUG_CARDS", "╚════════════════════════════════════════╝")
        Log.d("DEBUG_CARDS", "📋 Total mesas: ${mesas.size}")
        Log.d("DEBUG_CARDS", "📋 Pano: $numeroPano")

        try {
            val panoId = appRepository.buscarPorNumero(numeroPano)?.id
            Log.d("DEBUG_CARDS", "🔍 Pano ID encontrado: $panoId")

            val dataAtual = com.example.gestaobilhares.core.utils.DateUtils.obterDataAtual().time
            Log.d("DEBUG_CARDS", "📅 Data atual: $dataAtual")

            mesas.forEachIndexed { index, mesa ->
                Log.d("DEBUG_CARDS", "")
                Log.d("DEBUG_CARDS", "─────────────────────────────────────────")
                Log.d("DEBUG_CARDS", "🔹 Mesa ${index + 1}/${mesas.size}")
                Log.d("DEBUG_CARDS", "   ID: ${mesa.id}")
                Log.d("DEBUG_CARDS", "   Número: ${mesa.numero}")

                val descricaoPano = "Troca de pano realizada durante acerto - Pano: $numeroPano"

                Log.d("DEBUG_CARDS", "🚀 Chamando registrarTrocaPanoUseCase...")
                Log.d("DEBUG_CARDS", "🔍 ANTES DO USE CASE - Thread: ${Thread.currentThread().name}")

                // Garantir execução completa sem ser cancelado pelo lifecycle
                withContext(Dispatchers.IO) {
                    val nomeUsuarioLogado = userSessionManager.getCurrentUserName()
                    Log.d("DEBUG_CARDS", "👤 Usuário logado: $nomeUsuarioLogado")
                    
                    registrarTrocaPanoUseCase(
                        TrocaPanoParams(
                            mesaId = mesa.id,
                            numeroMesa = mesa.numero,
                            panoNovoId = panoId,
                            dataManutencao = dataAtual,
                            origem = OrigemTrocaPano.ACERTO,
                            descricao = descricaoPano,
                            observacao = null,
                            nomeUsuario = nomeUsuarioLogado // ✅ CORREÇÃO: Passar usuário real
                        )
                    )
                }

                Log.d("DEBUG_CARDS", "🔍 DEPOIS DO USE CASE - Thread: ${Thread.currentThread().name}")
                Log.d("DEBUG_CARDS", "✅ Use case executado para mesa ${mesa.numero}")
                logOperation("SETTLEMENT", "Troca de pano registrada para mesa ${mesa.numero}")
            }

            Log.d("DEBUG_CARDS", "")
            Log.d("DEBUG_CARDS", "╔════════════════════════════════════════╗")
            Log.d("DEBUG_CARDS", "║   ACERTO - Concluído com Sucesso       ║")
            Log.d("DEBUG_CARDS", "╚════════════════════════════════════════╝")

        } catch (e: Exception) {
            Log.e("DEBUG_CARDS", "")
            Log.e("DEBUG_CARDS", "╔════════════════════════════════════════╗")
            Log.e("DEBUG_CARDS", "║   ACERTO - ERRO FATAL                  ║")
            Log.e("DEBUG_CARDS", "╚════════════════════════════════════════╝")
            Log.e("DEBUG_CARDS", "Exception: ${e.javaClass.simpleName}")
            Log.e("DEBUG_CARDS", "Message: ${e.message}")
            Log.e("DEBUG_CARDS", "StackTrace:", e)
            Timber.e("SettlementViewModel", "Erro ao registrar troca de pano: ${e.message}", e)
        }
    }
    
    fun limparResultadoSalvamento() {
        _resultadoSalvamento.value = null
    }

    suspend fun buscarAcertoPorId(acertoId: Long): Acerto? {
        return appRepository.obterAcertoPorId(acertoId)
    }

    suspend fun buscarMesasDoAcerto(acertoId: Long): List<com.example.gestaobilhares.data.entities.AcertoMesa> {
        return appRepository.buscarAcertoMesasPorAcerto(acertoId)
    }

    fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            showLoading()
        } else {
            hideLoading()
        }
    }

    /**
     * ✅ NOVO: Busca uma mesa específica por ID para obter o número real
     */
    suspend fun buscarMesaPorId(mesaId: Long): Mesa? {
        return try {
            appRepository.obterMesaPorId(mesaId)
        } catch (e: Exception) {
            Timber.e("SettlementViewModel", "Erro ao buscar mesa por ID: ${e.message}", e)
            null
        }
    }
    
    /**
     * ✅ NOVO: Busca um cliente específico por ID para obter dados como comissão da ficha
     */
    suspend fun obterClientePorId(clienteId: Long): com.example.gestaobilhares.data.entities.Cliente? {
        return try {
            appRepository.obterClientePorId(clienteId)
        } catch (e: Exception) {
            Timber.e("SettlementViewModel", "Erro ao buscar cliente por ID: ${e.message}", e)
            null
        }
    }
    
    /**
     * ✅ NOVO: Busca o contrato ativo do cliente para exibir no recibo
     */
    suspend fun buscarContratoAtivoPorCliente(clienteId: Long): com.example.gestaobilhares.data.entities.ContratoLocacao? {
        return try {
            // Usar o AppRepository através do ClienteRepository
            appRepository.buscarContratoAtivoPorCliente(clienteId)
        } catch (e: Exception) {
            Timber.e("SettlementViewModel", "Erro ao buscar contrato ativo do cliente: ${e.message}", e)
            null
        }
    }
    
    /**
     * ✅ NOVO: Busca mesas do acerto por ID para preenchimento na edição
     */
    suspend fun buscarAcertoMesasPorAcertoId(acertoId: Long): List<com.example.gestaobilhares.data.entities.AcertoMesa> {
        return try {
            appRepository.buscarAcertoMesasPorAcerto(acertoId)
        } catch (e: Exception) {
            Timber.e("SettlementViewModel", "Erro ao buscar mesas do acerto: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * ✅ NOVO: Define o débito anterior para edição de acerto
     */
    fun definirDebitoAnteriorParaEdicao(debitoAnterior: Double) {
        _debitoAnterior.value = debitoAnterior
        logOperation("SETTLEMENT", "Débito anterior definido para edição: R$ $debitoAnterior")
    }
    
    /**
     * ✅ NOVO: Calcula a média de fichas jogadas dos últimos acertos de uma mesa
     * @param mesaId ID da mesa
     * @param limite Máximo de acertos a considerar (padrão 5)
     * @return Média de fichas jogadas, ou 0 se não houver acertos anteriores
     */
    suspend fun calcularMediaFichasJogadas(mesaId: Long, limite: Int = 5): Double {
        val currentClienteId = _clienteId.value
        if (currentClienteId == null) {
            Timber.e("SettlementViewModel", "Erro ao calcular média: clienteId não definido")
            return 0.0
        }

        return try {
            appRepository.calcularMediaFichasJogadas(mesaId, currentClienteId, limite)
        } catch (e: Exception) {
            Timber.e("SettlementViewModel", "Erro ao calcular média de fichas: ${e.message}", e)
            0.0
        }
    }
    
    /**
     * ✅ NOVO: Marca um pano como usado no estoque
     */
    suspend fun marcarPanoComoUsado(numeroPano: String, motivo: String = "Usado no acerto") {
        try {
            Timber.d("SettlementViewModel", "Marcando pano $numeroPano como usado: $motivo")
            appRepository.marcarPanoComoUsadoPorNumero(numeroPano)
            Timber.d("SettlementViewModel", "Pano $numeroPano marcado como usado com sucesso")
        } catch (e: Exception) {
            Timber.e("SettlementViewModel", "Erro ao marcar pano como usado: ${e.message}", e)
        }
    }
    
    /**
     * ✅ NOVO: Troca o pano na mesa e marca como usado no estoque
     */
    suspend fun trocarPanoNaMesa(numeroPano: String, motivo: String = "Usado no acerto") {
        try {
            // 1. Buscar o pano no estoque
            val pano = appRepository.buscarPorNumero(numeroPano)
            if (pano == null) {
                logError("SETTLEMENT", "Pano $numeroPano não encontrado no estoque")
                return
            }
            
            // 2. Marcar pano como usado no estoque
            appRepository.marcarPanoComoUsado(pano.id)
            
            val mesaAtual = _mesasCliente.value.firstOrNull()
            if (mesaAtual == null) {
                logError("SETTLEMENT", "Nenhuma mesa disponível para vincular pano")
                return
            }

            // ✅ IMPLEMENTADO: Vincular pano à mesa usando número disponível
            appRepository.vincularPanoAMesa(pano.id, mesaAtual.numero)
            
        } catch (e: Exception) {
            Timber.e("SettlementViewModel", "Erro ao trocar pano na mesa: ${e.message}", e)
        }
    }
    
    /**
     * ✅ NOVO: Troca o pano em uma mesa específica
     */
    suspend fun trocarPanoNaMesa(mesaId: Long, numeroPano: String, motivo: String = "Usado no acerto") {
        try {
            Timber.d("SettlementViewModel", "Iniciando troca de pano $numeroPano na mesa $mesaId")
            
            // 1. Buscar o pano no estoque
            val pano = appRepository.buscarPorNumero(numeroPano)
            if (pano == null) {
                logError("SETTLEMENT", "Pano $numeroPano não encontrado no estoque")
                return
            }
            
            Timber.d("SettlementViewModel", "Pano encontrado: ${pano.numero} (ID: ${pano.id})")
            
            // 2. Marcar pano como usado no estoque
            appRepository.marcarPanoComoUsado(pano.id)
            Timber.d("SettlementViewModel", "Pano ${pano.id} marcado como usado no estoque")
            
            // 3. Atualizar mesa com novo pano
            atualizarPanoDaMesa(mesaId, pano.id)
            
            Timber.d("SettlementViewModel", "Pano $numeroPano trocado na mesa $mesaId com sucesso: $motivo")
            
        } catch (e: Exception) {
            Timber.e("SettlementViewModel", "Erro ao trocar pano na mesa: ${e.message}", e)
        }
    }
    
    /**
     * ✅ NOVO: Atualiza o pano atual de uma mesa
     */
    private suspend fun atualizarPanoDaMesa(mesaId: Long, panoId: Long) {
        try {
            Timber.d("SettlementViewModel", "Atualizando pano da mesa $mesaId com pano $panoId")
            
            // Buscar a mesa atual
            val mesa = appRepository.obterMesaPorId(mesaId)
            if (mesa != null) {
                logOperation("SETTLEMENT", "Mesa encontrada: ${mesa.numero}")
                
                // ✅ CORREÇÃO: Usar data atual de forma segura
                val dataAtual = try {
                    com.example.gestaobilhares.core.utils.DateUtils.obterDataAtual()
                } catch (e: Exception) {
                    Timber.w("SettlementViewModel", "Erro ao obter data atual, usando data padrão: ${e.message}")
                    java.util.Date() // Fallback para data atual do sistema
                }
                
                // Atualizar mesa com novo pano e data
                val mesaAtualizada = mesa.copy(
                    panoAtualId = panoId,
                    dataUltimaTrocaPano = dataAtual.time
                )
                appRepository.atualizarMesa(mesaAtualizada)
                logOperation("SETTLEMENT", "Mesa $mesaId atualizada com pano $panoId com sucesso")
                
                // ════════════════════════════════════════════════════════════════
                // LOG DIAGNÓSTICO: Rastrear panoId da mesa ANTES de virar DTO
                // ════════════════════════════════════════════════════════════════
                Log.w("DEBUG_POPUP", "╔═══════════════════════════════════════════════════╗")
                Log.w("DEBUG_POPUP", "║  RASTREAMENTO PANO - APÓS ATUALIZAR MESA          ║")
                Log.w("DEBUG_POPUP", "╚═══════════════════════════════════════════════════╝")
                Log.w("DEBUG_POPUP", "🔍 Mesa ID: ${mesa.id}")
                Log.w("DEBUG_POPUP", "🔍 Mesa Número: ${mesa.numero}")
                Log.w("DEBUG_POPUP", "🔍 mesa.panoAtualId: ${mesaAtualizada.panoAtualId}")
                Log.w("DEBUG_POPUP", "🔍 panoId recém atribuído: $panoId")
                Log.w("DEBUG_POPUP", "🔍 Tipo do objeto mesa: ${mesa.javaClass.simpleName}")
                Log.w("DEBUG_POPUP", "🔍 Tipo do objeto mesaAtualizada: ${mesaAtualizada.javaClass.simpleName}")
                Log.w("DEBUG_POPUP", "╚═══════════════════════════════════════════════════╝")
            } else {
                logError("SETTLEMENT", "Mesa $mesaId não encontrada")
            }
        } catch (e: Exception) {
            Timber.e("SettlementViewModel", "Erro ao atualizar pano da mesa: ${e.message}", e)
            throw e // Re-throw para que o Fragment possa tratar
        }
    }
    
    /**
     * ✅ NOVO: Carrega o pano atual de uma mesa
     */
    suspend fun carregarPanoAtualDaMesa(mesaId: Long): PanoEstoque? {
        return try {
            // 1. Buscar a mesa
            val mesa = appRepository.obterMesaPorId(mesaId)
            val panoAtualId = mesa?.panoAtualId
            if (panoAtualId == null) {
                logOperation("SETTLEMENT", "Mesa $mesaId não possui pano atual")
                return null
            }
            
            // 2. Buscar o pano atual
            val pano = appRepository.obterPanoPorId(panoAtualId)
            if (pano == null) {
                logError("SETTLEMENT", "Pano $panoAtualId não encontrado no estoque")
                return null
            }
            
            Timber.d("SettlementViewModel", "Pano atual da mesa $mesaId: ${pano.numero}")
            pano
            
        } catch (e: Exception) {
            Timber.e("SettlementViewModel", "Erro ao carregar pano atual da mesa: ${e.message}", e)
            null
        }
    }
} 

