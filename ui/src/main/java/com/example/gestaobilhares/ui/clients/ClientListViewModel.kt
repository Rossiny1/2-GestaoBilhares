package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.ui.common.BaseViewModel
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.StatusRota
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.example.gestaobilhares.data.entities.Despesa
import com.example.gestaobilhares.data.repository.AppRepository
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.FlowPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
/**
 * Filtros disponíveis para a lista de clientes
 */
enum class FiltroCliente {
    TODOS, ACERTADOS, NAO_ACERTADOS, PENDENCIAS
}

/**
 * Filtros gerais para dialog (Ativos/Inativos baseados em mesa e débito)
 */
enum class FiltroGeralCliente {
    ATIVOS,     // Clientes com mesa OU com débito
    INATIVOS,   // Clientes sem mesa E sem débito
    TODOS       // Todos os clientes
}

/**
 * Tipos de pesquisa avançada disponíveis
 */
enum class SearchType(val label: String, val hint: String) {
    NOME_CLIENTE("Nome do Cliente", "Digite o nome do cliente"),
    NUMERO_MESA("Número da Mesa", "Digite o número da mesa"),
    TELEFONE("Telefone", "Digite o telefone"),
    ENDERECO("Endereço / Cidade", "Digite o endereço ou cidade"),
    CPF_CNPJ("CPF/CNPJ", "Digite o CPF ou CNPJ")
}



/**
 * ViewModel para ClientListFragment
 * ✅ FASE 8C: Integração com sistema de ciclo de acerto real
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ClientListViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val userSessionManager: com.example.gestaobilhares.core.utils.UserSessionManager
) : BaseViewModel() {
    
    // ✅ FASE 4B: Pagination Manager para lazy loading
    // ✅ FASE 12.10: Paginação mais agressiva (reduzido de 20 para 15 para melhor performance)

    private val _rotaInfo = MutableStateFlow<Rota?>(null)
    val rotaInfo: StateFlow<Rota?> = _rotaInfo.asStateFlow()

    private val _statusRota = MutableStateFlow(StatusRota.EM_ANDAMENTO)
    val statusRota: StateFlow<StatusRota> = _statusRota.asStateFlow()

    // ✅ FASE 8C: CICLO DE ACERTO REAL
    private val _cicloAcerto = MutableStateFlow(1)
    val cicloAcerto: StateFlow<Int> = _cicloAcerto.asStateFlow()
    
    private val _cicloAcertoEntity = MutableStateFlow<CicloAcertoEntity?>(null)
    val cicloAcertoEntity: StateFlow<CicloAcertoEntity?> = _cicloAcertoEntity.asStateFlow()
    
    private val _statusCiclo = MutableStateFlow(StatusCicloAcerto.FINALIZADO)
    val statusCiclo: StateFlow<StatusCicloAcerto> = _statusCiclo.asStateFlow()
    
    private val _progressoCiclo = MutableStateFlow(0)
    val progressoCiclo: StateFlow<Int> = _progressoCiclo.asStateFlow()

    // ✅ NOVOS: Dados para o card de progresso do ciclo
    private val _percentualAcertados = MutableStateFlow(0)
    val percentualAcertados: StateFlow<Int> = _percentualAcertados.asStateFlow()
    
    private val _totalClientes = MutableStateFlow(0)
    val totalClientes: StateFlow<Int> = _totalClientes.asStateFlow()
    
    private val _clientesAcertados = MutableStateFlow(0)
    val clientesAcertados: StateFlow<Int> = _clientesAcertados.asStateFlow()
    
    private val _pendencias = MutableStateFlow(0)
    val pendencias: StateFlow<Int> = _pendencias.asStateFlow()

    private val _clientesTodos = MutableStateFlow<List<Cliente>>(emptyList())
    private val _filtroAtual = MutableStateFlow(FiltroCliente.NAO_ACERTADOS)
    val filtroAtual: StateFlow<FiltroCliente> = _filtroAtual.asStateFlow()
    
    // ✅ NOVO: Filtro geral para dialog (Ativos/Inativos/Todos)
    private val _filtroGeral = MutableStateFlow(FiltroGeralCliente.TODOS)
    val filtroGeral: StateFlow<FiltroGeralCliente> = _filtroGeral.asStateFlow()
    
    // ✅ FASE 9B: Lista de clientes filtrados
    private val _clientes = MutableStateFlow<List<Cliente>>(emptyList())
    val clientes: StateFlow<List<Cliente>> = _clientes.asStateFlow()

    // Estados de loading e error já estão no BaseViewModel

    private val _rotaIdFlow = MutableStateFlow<Long?>(null)

    // Novo: ciclo ativo reativo
    private val _cicloAtivo = MutableStateFlow<CicloAcertoEntity?>(null)
    val cicloAtivo: StateFlow<CicloAcertoEntity?> = _cicloAtivo.asStateFlow()

    // StateFlow para o card de progresso do ciclo
    private val _cicloProgressoCard = MutableStateFlow<CicloProgressoCard?>(null)
    val cicloProgressoCard: StateFlow<CicloProgressoCard?> = _cicloProgressoCard.asStateFlow()

    // ✅ NOVO: Dados reais da rota (clientes ativos e mesas)
    private val _dadosRotaReais = MutableStateFlow(DadosRotaReais(0, 0))
    val dadosRotaReais: StateFlow<DadosRotaReais> = _dadosRotaReais.asStateFlow()

    init {
        // ✅ NOVO: Observar mudanças nas rotas para sincronização automática
        viewModelScope.launch {
            appRepository.getRotasResumoComAtualizacaoTempoReal().collect { rotasResumo ->
                val rotaAtual = _rotaInfo.value
                if (rotaAtual != null) {
                    // Encontrar a rota atualizada na lista
                    val rotaAtualizada = rotasResumo.find { it.rota.id == rotaAtual.id }?.rota
                    if (rotaAtualizada != null && rotaAtualizada != rotaAtual) {
                        Timber.d("ClientListViewModel", "🔄 Rota sincronizada automaticamente: ${rotaAtualizada.nome}")
                        Timber.d("ClientListViewModel", "   Status: ${rotaAtualizada.statusAtual}")
                        Timber.d("ClientListViewModel", "   Ciclo: ${rotaAtualizada.cicloAcertoAtual}")
                        
                        _rotaInfo.value = rotaAtualizada
                        carregarCicloAcertoReal(rotaAtualizada)
                        carregarStatusRota()
                    }
                }
            }
        }
        
        // Configurar fluxo reativo baseado no rotaId
        viewModelScope.launch {
            _rotaIdFlow.flatMapLatest { rotaId ->
                if (rotaId == null) {
                    return@flatMapLatest flowOf(null)
                }
                
                // Observar ciclo ativo da rota
                flowOf(appRepository.buscarCicloAtualPorRota(rotaId))
            }.collect { ciclo ->
                Timber.d("DEBUG_DIAG", "[CARD] cicloAtivo retornado: id=${ciclo?.id}, status=${ciclo?.status}, dataInicio=${ciclo?.dataInicio}, dataFim=${ciclo?.dataFim}")
                _cicloAtivo.value = ciclo
            }
        }

        // Configurar card de progresso reativo
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            _rotaIdFlow.flatMapLatest { rotaId ->
                if (rotaId == null) {
                    return@flatMapLatest flowOf(
                        CicloProgressoCard(
                            receita = 0.0,
                            despesas = 0.0,
                            saldo = 0.0,
                            percentual = 0,
                            clientesAcertados = 0,
                            totalClientes = 0,
                            pendencias = 0,
                            debitoTotal = 0.0
                        )
                    )
                }
                
                val cicloAtivoFlow = flowOf(appRepository.buscarCicloAtualPorRota(rotaId))
                val todosClientesFlow = appRepository.obterClientesPorRota(rotaId)

                combine(cicloAtivoFlow, todosClientesFlow) { ciclo: CicloAcertoEntity?, todosClientes: List<Cliente> ->
                    val debitoTotal = todosClientes.sumOf { it.debitoAtual }
                    if (ciclo == null) {
                        return@combine CicloProgressoCard(0.0, 0.0, 0.0, 0, 0, todosClientes.size, 0, debitoTotal)
                    }

                    val acertos = appRepository.buscarAcertosPorRotaECiclo(rotaId, ciclo.id)
                    val despesas = appRepository.buscarDespesasPorCicloId(ciclo.id).first()
                    
                    val clientesAcertados = acertos.map { it.clienteId }.distinct().size
                    val totalClientes = todosClientes.size
                    val faturamentoReal = acertos.sumOf { it.valorRecebido }
                    val despesasReais = despesas.sumOf { it.valor }
                    val pendencias = calcularPendenciasReaisSync(todosClientes)
                    val percentualAcertados = if (totalClientes > 0) (clientesAcertados * 100) / totalClientes else 0
                    val saldo = faturamentoReal - despesasReais
                    
                    CicloProgressoCard(
                        receita = faturamentoReal,
                        despesas = despesasReais,
                        saldo = saldo,
                        percentual = percentualAcertados,
                        clientesAcertados = clientesAcertados,
                        totalClientes = totalClientes,
                        pendencias = pendencias,
                        debitoTotal = debitoTotal
                    )
                }
            }
            // Evita "flicker" de abas após importação: aguarda dados estabilizarem
            .debounce(250)
            .distinctUntilChanged()
            .collect { card: CicloProgressoCard ->
                _cicloProgressoCard.value = card
            }
        }
    }

    /**
     * Carrega informações da rota
     */
    fun carregarRota(rotaId: Long) {
        // Definir o rotaId no fluxo reativo
        _rotaIdFlow.value = rotaId
        
        viewModelScope.launch {
            try {
                showLoading()
                val rota: Rota? = appRepository.obterRotaPorId(rotaId)
                _rotaInfo.value = rota
                rota?.let { rotaInfo ->
                    // ✅ CORREÇÃO: Carregar ciclo primeiro, depois status
                    carregarCicloAcertoReal(rotaInfo)
                    // ✅ CORREÇÃO: Carregar status após ciclo estar carregado
                    carregarStatusRota()
                }
            } catch (e: Exception) {
                Timber.e("ClientListViewModel", "Erro ao carregar rota: ${e.message}", e)
                showError("Erro ao carregar informações da rota: ${e.message}", e)
                // Definir valores padrão para evitar crash
                _rotaInfo.value = Rota(id = rotaId, nome = "Rota $rotaId", ativa = true)
                _statusRota.value = StatusRota.PAUSADA
                _cicloAcerto.value = 1
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * ✅ FASE 2D: Carrega clientes com query otimizada (performance melhorada)
     */
    fun carregarClientesOtimizado(rotaId: Long) {
        Timber.d("ClientListVM", "carregarClientesOtimizado chamado para rotaId: $rotaId")
        
        // Atualizar o rotaId no fluxo se necessário
        if (_rotaIdFlow.value != rotaId) {
            _rotaIdFlow.value = rotaId
        }
        
        viewModelScope.launch {
            try {
                showLoading()
                
                // ✅ FASE 2D: Usar query otimizada com débito atual calculado
                Timber.d("ClientListViewModel", "📊 Buscando clientes com débito atual para rotaId: $rotaId")
                val clientes = appRepository.obterClientesPorRotaComDebitoAtual(rotaId).first()
                Timber.d("ClientListViewModel", "✅ Clientes recebidos: ${clientes.size} clientes")
                
                // ✅ DEBUG: Log detalhado do débito de cada cliente
                clientes.forEach { cliente ->
                    Timber.d("ClientListViewModel", "   Cliente: ${cliente.nome} | débitoAtual: R$ ${cliente.debitoAtual}")
                }
                
                _clientesTodos.value = clientes
                Timber.d("ClientListViewModel", "📋 Aplicando filtros combinados...")
                aplicarFiltrosCombinados() // Aplicar filtros após carregar
                
                // ✅ NOVO: Calcular dados do card de progresso
                calcularDadosProgressoCiclo(clientes)
                
                // ✅ CORREÇÃO: Continuar observando mudanças com debounce para evitar atualizações excessivas
                appRepository.obterClientesPorRotaComDebitoAtual(rotaId)
                    .distinctUntilChanged() // ✅ Evita atualizações desnecessárias
                    .collect { clientesAtualizados ->
                        val clientesAnteriores = _clientesTodos.value
                        // ✅ CORREÇÃO: Comparar também por débito para detectar mudanças mesmo com mesma lista
                        val mudouDebito = clientesAtualizados.any { novo ->
                            val antigo = clientesAnteriores.find { it.id == novo.id }
                            antigo?.debitoAtual != novo.debitoAtual
                        }
                        
                        if (clientesAtualizados != clientesAnteriores || mudouDebito) {
                            Timber.d("ClientListViewModel", "🔄 Clientes atualizados detectados: ${clientesAtualizados.size} clientes (mudou débito: $mudouDebito)")
                            
                            // ✅ DEBUG: Log detalhado do débito de cada cliente atualizado
                            clientesAtualizados.forEach { cliente ->
                                val debitoAnterior = clientesAnteriores.find { it.id == cliente.id }?.debitoAtual ?: 0.0
                                if (cliente.debitoAtual != debitoAnterior) {
                                    Timber.d("ClientListViewModel", "   ⚠️ Débito mudou: ${cliente.nome} | R$ $debitoAnterior -> R$ ${cliente.debitoAtual}")
                                } else {
                                    Timber.d("ClientListViewModel", "   Cliente atualizado: ${cliente.nome} | débitoAtual: R$ ${cliente.debitoAtual}")
                                }
                            }
                            
                            _clientesTodos.value = clientesAtualizados
                            Timber.d("ClientListViewModel", "📋 Reaplicando filtros após atualização de clientes...")
                            aplicarFiltrosCombinados()
                            calcularDadosProgressoCiclo(clientesAtualizados)
                        }
                    }
                
            } catch (e: Exception) {
                logError("CLIENTES_LOAD_OTIMIZADO", "Erro ao carregar clientes otimizado: ${e.message}", e)
                showError("Erro ao carregar clientes: ${e.message}", e)
                // Definir lista vazia para evitar crash
                _clientesTodos.value = emptyList()
                _clientes.value = emptyList()
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * ✅ FASE 9B: Carrega clientes da rota com filtros combinados (método original)
     * ✅ CORREÇÃO: Usar query com débito atual para garantir dados atualizados imediatamente
     */
    fun carregarClientes(rotaId: Long) {
        Timber.d("ClientListVM", "carregarClientes chamado para rotaId: $rotaId")
        
        // Atualizar o rotaId no fluxo se necessário
        if (_rotaIdFlow.value != rotaId) {
            _rotaIdFlow.value = rotaId
        }
        
        viewModelScope.launch {
            try {
                showLoading()
                
                // ✅ CORREÇÃO: Usar query com débito atual para garantir dados atualizados imediatamente
                // Isso garante que clientes pagos apareçam corretamente na aba "Pago" sem delay
                val clientes = appRepository.obterClientesPorRotaComDebitoAtual(rotaId).first()
                _clientesTodos.value = clientes
                aplicarFiltrosCombinados() // Aplicar filtros após carregar
                
                // ✅ NOVO: Calcular dados do card de progresso
                calcularDadosProgressoCiclo(clientes)
                
                Timber.d("ClientListViewModel", "✅ Clientes carregados imediatamente: ${clientes.size} clientes")
                
                // ✅ CORREÇÃO: Continuar observando mudanças com query otimizada e debounce
                appRepository.obterClientesPorRotaComDebitoAtual(rotaId)
                    .distinctUntilChanged() // ✅ Evita atualizações desnecessárias
                    .collect { clientesAtualizados ->
                        val clientesAnteriores = _clientesTodos.value
                        // ✅ CORREÇÃO: Comparar também por débito para detectar mudanças mesmo com mesma lista
                        val mudouDebito = clientesAtualizados.any { novo ->
                            val antigo = clientesAnteriores.find { it.id == novo.id }
                            antigo?.debitoAtual != novo.debitoAtual
                        }
                        
                        if (clientesAtualizados != clientesAnteriores || mudouDebito) {
                            Timber.d("ClientListViewModel", "🔄 Clientes atualizados: ${clientesAtualizados.size} clientes (mudou débito: $mudouDebito)")
                            
                            // ✅ DEBUG: Log quando débito muda
                            if (mudouDebito) {
                                clientesAtualizados.forEach { cliente ->
                                    val debitoAnterior = clientesAnteriores.find { it.id == cliente.id }?.debitoAtual ?: 0.0
                                    if (cliente.debitoAtual != debitoAnterior) {
                                        Timber.d("ClientListViewModel", "   ⚠️ Débito mudou: ${cliente.nome} | R$ $debitoAnterior -> R$ ${cliente.debitoAtual}")
                                    }
                                }
                            }
                            
                            _clientesTodos.value = clientesAtualizados
                            aplicarFiltrosCombinados()
                            calcularDadosProgressoCiclo(clientesAtualizados)
                        }
                    }
            } catch (e: Exception) {
                logError("CLIENTES_LOAD", "Erro ao carregar clientes: ${e.message}", e)
                showError("Erro ao carregar clientes: ${e.message}", e)
                // Definir lista vazia para evitar crash
                _clientesTodos.value = emptyList()
                _clientes.value = emptyList()
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * ✅ FASE 2D: Teste de performance - compara query original vs otimizada
     */
    fun testarPerformanceQueries(rotaId: Long) {
        Timber.d("ClientListVM", "testarPerformanceQueries iniciado para rotaId: $rotaId")
        
        viewModelScope.launch {
            try {
                // Teste 1: Query original
                val inicioOriginal = System.currentTimeMillis()
                val clientesOriginal = appRepository.obterClientesPorRota(rotaId).first()
                val tempoOriginal = System.currentTimeMillis() - inicioOriginal
                
                // Teste 2: Query otimizada
                val inicioOtimizada = System.currentTimeMillis()
                val clientesOtimizada = appRepository.obterClientesPorRotaComDebitoAtual(rotaId).first()
                val tempoOtimizada = System.currentTimeMillis() - inicioOtimizada
                
                // Log dos resultados
                Timber.d("ClientListVM", "=== TESTE DE PERFORMANCE ===")
                Timber.d("ClientListVM", "Query Original: ${tempoOriginal}ms - ${clientesOriginal.size} clientes")
                Timber.d("ClientListVM", "Query Otimizada: ${tempoOtimizada}ms - ${clientesOtimizada.size} clientes")
                Timber.d("ClientListVM", "Melhoria: ${((tempoOriginal - tempoOtimizada).toDouble() / tempoOriginal * 100).toInt()}%")
                Timber.d("ClientListVM", "==========================")
                
            } catch (e: Exception) {
                Timber.d("ClientListVM", "Erro no teste de performance: ${e.message}")
            }
        }
    }

    /**
     * ✅ NOVO: Força o recarregamento dos clientes (útil para navegação de volta)
     */
    /**
     * ✅ FASE 2D: Força recarregamento com query otimizada
     */
    fun forcarRecarregamentoClientesOtimizado(rotaId: Long) {
        Timber.d("ClientListVM", "forcarRecarregamentoClientesOtimizado chamado para rotaId: $rotaId")
        
        viewModelScope.launch {
            try {
                // ✅ FASE 2D: Forçar carregamento imediato com query otimizada
                val clientes = appRepository.obterClientesPorRotaComDebitoAtual(rotaId).first()
                _clientesTodos.value = clientes
                aplicarFiltrosCombinados()
                calcularDadosProgressoCiclo(clientes)
                
                Timber.d("ClientListViewModel", "✅ Dados otimizados recarregados: ${clientes.size} clientes")
            } catch (e: Exception) {
                Timber.d("ClientListVM", "Erro ao forçar recarregamento otimizado: ${e.message}")
            }
        }
    }

    fun forcarRecarregamentoClientes(rotaId: Long) {
        Timber.d("ClientListVM", "forcarRecarregamentoClientes chamado para rotaId: $rotaId")
        
        viewModelScope.launch {
            try {
                // Forçar carregamento imediato
                val clientes = appRepository.obterClientesPorRota(rotaId).first()
                _clientesTodos.value = clientes
                aplicarFiltrosCombinados()
                calcularDadosProgressoCiclo(clientes)
                
                Timber.d("ClientListViewModel", "✅ Dados forçados recarregados: ${clientes.size} clientes")
            } catch (e: Exception) {
                Timber.e("ClientListViewModel", "Erro ao forçar recarregamento: ${e.message}", e)
                showError("Erro ao recarregar clientes: ${e.message}", e)
            }
        }
    }

    /**
     * ✅ FASE 9B: Aplica filtro à lista de clientes com filtros combinados
     */
    fun aplicarFiltro(filtro: FiltroCliente) {
        viewModelScope.launch {
            try {
                _filtroAtual.value = filtro
                aplicarFiltrosCombinados()
            } catch (e: Exception) {
                logError("FILTRO_APPLY", "Erro ao aplicar filtro: ${e.message}", e)
                showError("Erro ao aplicar filtro: ${e.message}", e)
            }
        }
    }

    /**
     * ✅ FASE 8C: Inicia a rota criando um novo ciclo de acerto persistente
     */
    fun iniciarRota() {
        viewModelScope.launch {
            try {
                showLoading()
                
                val rota = _rotaInfo.value ?: return@launch
                val anoAtual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                
                // ✅ NOVO: Verificar se há ciclo anterior em andamento e finalizá-lo antes de criar novo
                val cicloAnterior = appRepository.buscarCicloAtivo(rota.id)
                if (cicloAnterior != null && cicloAnterior.status == StatusCicloAcerto.EM_ANDAMENTO) {
                    Timber.d("ClientListViewModel", "🔄 Finalizando ciclo anterior ${cicloAnterior.numeroCiclo}/${cicloAnterior.ano} (id=${cicloAnterior.id}) antes de iniciar novo ciclo")
                    try {
                        // Finalizar o ciclo anterior (isso também finalizará as metas automaticamente)
                        appRepository.finalizarCicloAtualComDados(rota.id)
                        Timber.d("ClientListViewModel", "✅ Ciclo anterior finalizado com sucesso")
                    } catch (e: Exception) {
                        Timber.e("ClientListViewModel", "❌ Erro ao finalizar ciclo anterior: ${e.message}", e)
                        // Continuar mesmo se houver erro na finalização
                    }
                }
                
                // Buscar próximo número de ciclo
                val proximoCiclo = appRepository.buscarProximoNumeroCiclo(rota.id, anoAtual)
                
                // ✅ NOVO: Salvar pendências do ciclo anterior antes de reinicializar
                val pendenciasCicloAnterior = _pendencias.value
                
                // Criar novo ciclo de acerto
                // ✅ FASE 12.7: Usar UserSessionManager para obter usuário atual
                val criadoPor = userSessionManager.getCurrentUserName()
                val novoCiclo = CicloAcertoEntity(
                    rotaId = rota.id,
                    numeroCiclo = proximoCiclo,
                    ano = anoAtual,
                    dataInicio = System.currentTimeMillis(),
                    dataFim = System.currentTimeMillis(), // Será atualizado quando finalizar
                    status = StatusCicloAcerto.EM_ANDAMENTO,
                    criadoPor = criadoPor
                )
                
                val cicloId = appRepository.inserirCicloAcerto(novoCiclo)
                Timber.d("ClientListViewModel", "✅ Novo ciclo ${proximoCiclo}/${anoAtual} criado com ID=$cicloId")
                
                // Atualizar estado
                _cicloAcerto.value = proximoCiclo
                _cicloAcertoEntity.value = novoCiclo.copy(id = cicloId)
                _cicloAtivo.value = novoCiclo.copy(id = cicloId) // ✅ CORREÇÃO: Atualizar ciclo ativo
                _statusCiclo.value = StatusCicloAcerto.EM_ANDAMENTO
                _statusRota.value = StatusRota.EM_ANDAMENTO

                // ✅ CONSISTÊNCIA LOCAL IMEDIATA: refletir início do ciclo na entidade Rota
                try {
                    appRepository.iniciarCicloRota(
                        rotaId = rota.id,
                        ciclo = proximoCiclo,
                        dataInicio = novoCiclo.dataInicio
                    )
                    
                    // ✅ CORREÇÃO: Forçar atualização da rota para disparar Flow e atualizar UI imediatamente
                    // Isso garante que o card da rota na tela de Rotas seja atualizado sem delay
                    val rotaAtualizada = appRepository.buscarRotaPorId(rota.id)
                    if (rotaAtualizada != null) {
                        // Fazer uma atualização trivial para disparar o Flow
                        appRepository.atualizarRota(rotaAtualizada.copy(dataAtualizacao = System.currentTimeMillis()))
                        Timber.d("ClientListViewModel", "✅ Rota atualizada para disparar Flow - UI será atualizada imediatamente")
                    }
                } catch (e: Exception) {
                    Timber.w(
                        "ClientListViewModel",
                        "Falha ao atualizar Rota no início do ciclo: ${e.message}"
                    )
                }
                
                // ✅ NOVO: Reinicializar campos do card de progresso para o novo ciclo
                _percentualAcertados.value = 0
                _clientesAcertados.value = 0
                _progressoCiclo.value = 0
                
                // ✅ NOVO: Manter pendências do ciclo anterior
                _pendencias.value = pendenciasCicloAnterior
                
                logState("CICLO_INICIAR", "Ciclo $proximoCiclo iniciado com sucesso - campos reinicializados, pendências mantidas: $pendenciasCicloAnterior")
                logState("CICLO_INICIAR", "Atualizando _cicloAtivo com novo ciclo: ID=$cicloId, Número=$proximoCiclo, Status=EM_ANDAMENTO")
                
                // ✅ NOTIFICAR MUDANÇA DE STATUS para atualização em tempo real
                notificarMudancaStatusRota(rota.id)
                
                // ✅ NOVO: Notificar que um novo ciclo foi iniciado para atualizar tela de metas
                // Isso fará com que o card de metas seja zerado e fique disponível para criar novas metas
                Timber.d("ClientListViewModel", "📢 Notificando início de novo ciclo para atualização de metas")
                // A atualização da rota já dispara os Flows, então as metas serão recarregadas automaticamente
                
                // ✅ CORREÇÃO: Recarregar clientes IMEDIATAMENTE após iniciar novo acerto
                // Isso garante que os clientes apareçam corretamente nas abas (em aberto/pago)
                Timber.d("ClientListViewModel", "🔄 Recarregando clientes imediatamente após iniciar novo acerto")
                carregarClientes(rota.id)
                
            } catch (e: Exception) {
                logError("CICLO_INICIAR", "Erro ao iniciar rota: ${e.message}", e)
                showError("Erro ao iniciar rota: ${e.message}", e)
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * ✅ FASE 8C: Finaliza a rota atual persistindo o ciclo
     */
    fun finalizarRota() {
        viewModelScope.launch {
            try {
                showLoading()

                val cicloAtual = _cicloAcertoEntity.value ?: return@launch
                val rota = _rotaInfo.value ?: return@launch
                
                Timber.d("ClientListViewModel", "Iniciando finalização da rota ${rota.nome} - Ciclo ${cicloAtual.numeroCiclo}")
                
                // ✅ Centralizar a lógica de finalização completa (consolidação + status FINALIZADO)
                appRepository.finalizarCicloAtualComDados(rota.id)

                // Recarregar o ciclo para obter os dados atualizados (status e débito total "congelado")
                val cicloFinalizado = appRepository.buscarCicloAtualPorRota(rota.id)
                
                // Atualizar estado da UI
                _cicloAcertoEntity.value = cicloFinalizado
                _cicloAtivo.value = cicloFinalizado
                _statusCiclo.value = StatusCicloAcerto.FINALIZADO
                _statusRota.value = StatusRota.FINALIZADA
                
                Timber.d("ClientListViewModel", "Ciclo finalizado com sucesso via repositório")
                Timber.d("ClientListViewModel", "Status da rota atualizado para: ${_statusRota.value}")
                
                // ✅ CORREÇÃO: Pequeno delay para garantir que o banco foi atualizado
                Timber.d("ClientListViewModel", "⏳ Aguardando 300ms para garantir atualização do banco...")
                kotlinx.coroutines.delay(300)
                Timber.d("ClientListViewModel", "✅ Delay concluído, recarregando clientes...")
                
                // ✅ CORREÇÃO: Recarregar clientes após finalizar ciclo para atualizar débitos
                // Isso garante que os débitos sejam exibidos corretamente após a finalização
                Timber.d("ClientListViewModel", "🔄 Chamando carregarClientesOtimizado para rotaId: ${rota.id}")
                carregarClientesOtimizado(rota.id)
                
                // ✅ NOTIFICAR MUDANÇA DE STATUS para atualização em tempo real
                notificarMudancaStatusRota(rota.id)
                
            } catch (e: Exception) {
                Timber.e("ClientListViewModel", "Erro ao finalizar rota: ${e.message}", e)
                showError("Erro ao finalizar rota: ${e.message}", e)
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Verifica se pode acessar detalhes do cliente (rota deve estar em andamento)
     */
    fun podeAcessarCliente(): Boolean {
        return _statusRota.value == StatusRota.EM_ANDAMENTO
    }

    /**
     * Verifica se pode fazer acerto (rota deve estar em andamento)
     */
    fun podeRealizarAcerto(): Boolean {
        return _statusRota.value == StatusRota.EM_ANDAMENTO
    }

    /**
     * ✅ FASE 8C: Carrega o ciclo de acerto real do banco de dados
     * ✅ CORREÇÃO: Usar a mesma lógica do AppRepository para sincronização
     */
    private suspend fun carregarCicloAcertoReal(rota: Rota) {
        try {
            Timber.d("ClientListViewModel", "🔄 Carregando ciclo para rota ${rota.nome} (ID: ${rota.id})")
            
            // ✅ CORREÇÃO: Usar a mesma lógica do AppRepository.obterCicloAtualRota()
            val emAndamento = appRepository.buscarCicloAtualPorRota(rota.id)
            
            if (emAndamento != null) {
                // Ciclo em andamento - mostrar o número atual
                _cicloAcerto.value = emAndamento.numeroCiclo
                _cicloAcertoEntity.value = emAndamento
                _cicloAtivo.value = emAndamento
                _statusCiclo.value = emAndamento.status
                _progressoCiclo.value = emAndamento.percentualConclusao
                
                Timber.d("ClientListViewModel", "✅ Ciclo em andamento carregado: ${emAndamento.numeroCiclo}º Acerto (ID: ${emAndamento.id}, Status: ${emAndamento.status})")
            } else {
                // ✅ CORREÇÃO: Nenhum ciclo em andamento - espelhar o AppRepository exibindo o ÚLTIMO ciclo finalizado
                val ultimoCiclo = appRepository.buscarUltimoCicloFinalizadoPorRota(rota.id)
                if (ultimoCiclo != null) {
                    _cicloAcerto.value = ultimoCiclo.numeroCiclo
                    _cicloAcertoEntity.value = ultimoCiclo
                    _cicloAtivo.value = ultimoCiclo
                    _statusCiclo.value = ultimoCiclo.status
                    _progressoCiclo.value = ultimoCiclo.percentualConclusao
                    Timber.d("ClientListViewModel", "🔄 Nenhum ciclo em andamento, exibindo último finalizado: ${ultimoCiclo.numeroCiclo}º Acerto (Status: ${ultimoCiclo.status})")
                } else {
                    _cicloAcerto.value = 1
                    _cicloAcertoEntity.value = null
                    _cicloAtivo.value = null
                    _statusCiclo.value = StatusCicloAcerto.FINALIZADO
                    _progressoCiclo.value = 0
                    Timber.d("ClientListViewModel", "🆕 Primeira vez nesta rota, exibindo 1º Acerto")
                }
            }
            
        } catch (e: Exception) {
            Timber.e("ClientListViewModel", "Erro ao carregar ciclo: ${e.message}", e)
            // Valores padrão em caso de erro
            _cicloAcerto.value = 1
            _cicloAcertoEntity.value = null
            _cicloAtivo.value = null
            _statusCiclo.value = StatusCicloAcerto.FINALIZADO
            _progressoCiclo.value = 0
        }
    }

    /**
     * ✅ FASE 8C: Carrega o status atual da rota baseado no ciclo
     */
    private fun carregarStatusRota() {
        // O status da rota será determinado pelo status do ciclo
        when (_statusCiclo.value) {
            StatusCicloAcerto.EM_ANDAMENTO -> _statusRota.value = StatusRota.EM_ANDAMENTO
            StatusCicloAcerto.FINALIZADO -> _statusRota.value = StatusRota.FINALIZADA
            StatusCicloAcerto.CANCELADO -> _statusRota.value = StatusRota.PAUSADA
            StatusCicloAcerto.PLANEJADO -> _statusRota.value = StatusRota.PAUSADA
            else -> _statusRota.value = StatusRota.PAUSADA // ✅ Adicionando else para tornar o when exhaustive
        }
    }

    // ✅ FASE 9B: Estado de busca atual
    private val _buscaAtual = MutableStateFlow("")
    
    // ✅ NOVO: Variáveis para pesquisa avançada
    private val _searchType = MutableStateFlow<SearchType?>(null)
    private val _searchCriteria = MutableStateFlow("")
    private val _isAdvancedSearch = MutableStateFlow(false)
    val buscaAtual: StateFlow<String> = _buscaAtual.asStateFlow()

    /**
     * ✅ FASE 9B: Busca clientes por nome em tempo real com filtros combinados
     */
    fun buscarClientes(query: String) {
        viewModelScope.launch {
            try {
                _buscaAtual.value = query
                _isAdvancedSearch.value = false
                _searchType.value = null
                _searchCriteria.value = ""
                aplicarFiltrosCombinados()
            } catch (e: Exception) {
                Timber.e("ClientListViewModel", "Erro na busca: ${e.message}", e)
                showError("Erro na busca: ${e.message}", e)
            }
        }
    }

    /**
     * ✅ NOVO: Pesquisa avançada de clientes por tipo e critério
     */
    fun pesquisarAvancada(searchType: SearchType, criteria: String) {
        viewModelScope.launch {
            try {
                _searchType.value = searchType
                _searchCriteria.value = criteria
                _isAdvancedSearch.value = true
                _buscaAtual.value = ""
                aplicarFiltrosCombinados()
            } catch (e: Exception) {
                Timber.e("ClientListViewModel", "Erro na pesquisa avançada: ${e.message}", e)
                showError("Erro na pesquisa avançada: ${e.message}", e)
            }
        }
    }

    /**
     * ✅ NOVO: Busca o último ciclo finalizado da rota
     */
    suspend fun buscarUltimoCicloFinalizado(): com.example.gestaobilhares.data.entities.CicloAcertoEntity? {
        return try {
            val rotaId = _rotaIdFlow.value ?: return null
            appRepository.buscarUltimoCicloFinalizadoPorRota(rotaId)
        } catch (e: Exception) {
            Timber.e("ClientListViewModel", "Erro ao buscar último ciclo finalizado: ${e.message}")
            null
        }
    }

    /**
     * ✅ NOVO: Força a atualização do ciclo atual (para sincronização com AppRepository)
     */
    fun atualizarCicloAtual() {
        viewModelScope.launch {
            try {
                val rota = _rotaInfo.value ?: return@launch
                Timber.d("ClientListViewModel", "🔄 Forçando atualização do ciclo atual para rota ${rota.nome}")
                carregarCicloAcertoReal(rota)
            } catch (e: Exception) {
                Timber.e("ClientListViewModel", "Erro ao atualizar ciclo atual: ${e.message}")
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * ✅ FASE 9B: Aplica filtro à lista de clientes com filtros combinados
     */
    private suspend fun aplicarFiltrosCombinados() {
        val query = com.example.gestaobilhares.core.utils.StringUtils.removerEspacosExtras(_buscaAtual.value)
        val filtro = _filtroAtual.value
        val todos = _clientesTodos.value
        val isAdvancedSearch = _isAdvancedSearch.value
        val searchType = _searchType.value
        val searchCriteria = com.example.gestaobilhares.core.utils.StringUtils.removerEspacosExtras(_searchCriteria.value)
        
        Timber.d("ClientListViewModel", "🔍 APLICANDO FILTROS COMBINADOS")
        Timber.d("ClientListViewModel", "   Total de clientes antes do filtro: ${todos.size}")
        Timber.d("ClientListViewModel", "   Filtro atual: $filtro")
        
        // ✅ DEBUG: Log do débito de cada cliente antes do filtro
        todos.forEach { cliente ->
            Timber.d("ClientListViewModel", "   [ANTES FILTRO] Cliente: ${cliente.nome} | débitoAtual: R$ ${cliente.debitoAtual}")
        }
        
        // ✅ CORREÇÃO: Filtro PENDENCIAS agora é inclusivo - mostra todos os clientes com pendências
        val filtradosPorStatus = when (filtro) {
            FiltroCliente.TODOS -> todos
            FiltroCliente.ACERTADOS -> filtrarClientesAcertados(todos)
            FiltroCliente.NAO_ACERTADOS -> filtrarClientesNaoAcertados(todos)
            FiltroCliente.PENDENCIAS -> filtrarClientesPendenciasInclusivo(todos)
        }
        
        Timber.d("ClientListViewModel", "   Clientes após filtro de status: ${filtradosPorStatus.size}")
        
        // ✅ DEBUG: Log do débito de cada cliente após filtro de status
        filtradosPorStatus.forEach { cliente ->
            Timber.d("ClientListViewModel", "   [APÓS FILTRO STATUS] Cliente: ${cliente.nome} | débitoAtual: R$ ${cliente.debitoAtual}")
        }
        
        // Depois filtrar por busca (normal ou avançada)
        val resultadoFinal = when {
            isAdvancedSearch && searchCriteria.isNotBlank() -> {
                filtrarPorPesquisaAvancada(filtradosPorStatus, searchType!!, searchCriteria)
            }
            query.isNotBlank() -> {
                filtradosPorStatus.filter { it.nome.contains(query, ignoreCase = true) }
            }
            else -> filtradosPorStatus
        }
        
        Timber.d("ClientListViewModel", "   Clientes após filtro de busca: ${resultadoFinal.size}")
        
        // ✅ DEBUG: Log do débito de cada cliente no resultado final
        resultadoFinal.forEach { cliente ->
            Timber.d("ClientListViewModel", "   [RESULTADO FINAL] Cliente: ${cliente.nome} | débitoAtual: R$ ${cliente.debitoAtual}")
        }
        
        // Atualizar lista filtrada
        _clientes.value = resultadoFinal
        Timber.d("ClientListViewModel", "✅ Filtros aplicados - ${resultadoFinal.size} clientes exibidos")
    }

    /**
     * ✅ NOVO: Filtra clientes acertados no ciclo atual
     * ✅ CORREÇÃO: Busca o ciclo ativo diretamente do repositório para garantir que está atualizado
     */
    private suspend fun filtrarClientesAcertados(clientes: List<Cliente>): List<Cliente> {
        val clientesAcertados = mutableListOf<Cliente>()
        
        // ✅ CORREÇÃO: Buscar ciclo ativo diretamente do repositório usando rotaId
        val rotaId = _rotaInfo.value?.id
        if (rotaId == null) {
            Timber.w("ClientListViewModel", "⚠️ rotaId é null, não é possível filtrar clientes acertados")
            return emptyList()
        }
        
        val cicloAtivo = appRepository.buscarCicloAtivo(rotaId)
        val cicloId = cicloAtivo?.id ?: -1L
        
        Timber.d("ClientListViewModel", "🔍 Filtrando clientes acertados - Ciclo ativo: ID=$cicloId, Número=${cicloAtivo?.numeroCiclo}")
        
        for (cliente in clientes) {
            if (clienteFoiAcertadoNoCiclo(cliente.id, cicloId)) {
                clientesAcertados.add(cliente)
            }
        }
        
        Timber.d("ClientListViewModel", "✅ Clientes acertados encontrados: ${clientesAcertados.size} de ${clientes.size}")
        
        return clientesAcertados
    }

    /**
     * ✅ NOVO: Filtra clientes não acertados no ciclo atual
     * ✅ CORREÇÃO: Busca o ciclo ativo diretamente do repositório para garantir que está atualizado
     */
    private suspend fun filtrarClientesNaoAcertados(clientes: List<Cliente>): List<Cliente> {
        val clientesNaoAcertados = mutableListOf<Cliente>()
        
        // ✅ CORREÇÃO: Buscar ciclo ativo diretamente do repositório usando rotaId
        val rotaId = _rotaInfo.value?.id
        if (rotaId == null) {
            Timber.w("ClientListViewModel", "⚠️ rotaId é null, não é possível filtrar clientes não acertados")
            return emptyList()
        }
        
        val cicloAtivo = appRepository.buscarCicloAtivo(rotaId)
        val cicloId = cicloAtivo?.id ?: -1L
        
        Timber.d("ClientListViewModel", "🔍 Filtrando clientes não acertados - Ciclo ativo: ID=$cicloId, Número=${cicloAtivo?.numeroCiclo}")
        
        for (cliente in clientes) {
            if (!clienteFoiAcertadoNoCiclo(cliente.id, cicloId)) {
                clientesNaoAcertados.add(cliente)
            }
        }
        
        Timber.d("ClientListViewModel", "✅ Clientes não acertados encontrados: ${clientesNaoAcertados.size} de ${clientes.size}")
        
        return clientesNaoAcertados
    }

    /**
     * ✅ NOVO: Filtra clientes com pendências (débito > 300 e não acertado há mais de 4 meses)
     */
    private suspend fun filtrarClientesPendencias(clientes: List<Cliente>): List<Cliente> {
        val clientesPendencias = mutableListOf<Cliente>()
        
        for (cliente in clientes) {
            if (clienteTemPendencias(cliente.id)) {
                clientesPendencias.add(cliente)
            }
        }
        
        return clientesPendencias
    }

    /**
     * ✅ CORREÇÃO: Filtra clientes com pendências de forma INCLUSIVA
     * Mostra TODOS os clientes que têm pendências, independentemente de estarem acertados ou não
     */
    private suspend fun filtrarClientesPendenciasInclusivo(clientes: List<Cliente>): List<Cliente> {
        val clientesPendencias = mutableListOf<Cliente>()
        
        Timber.d("ClientListViewModel", "🔍 Iniciando filtro PEND inclusivo para ${clientes.size} clientes")
        
        for (cliente in clientes) {
            // ✅ DEBUG: Log para verificar o débito de cada cliente
            Timber.d("ClientListViewModel", "Verificando cliente ${cliente.nome}: débitoAtual = ${com.example.gestaobilhares.core.utils.StringUtils.formatarMoeda(cliente.debitoAtual)}")
            
            // ✅ CRITÉRIO INCLUSIVO: Se o cliente tem pendências, incluir independente do status de acerto
            if (clienteTemPendencias(cliente.id)) {
                clientesPendencias.add(cliente)
                Timber.d("ClientListViewModel", "✅ Cliente ${cliente.nome} adicionado ao filtro PEND")
            } else {
                Timber.d("ClientListViewModel", "❌ Cliente ${cliente.nome} NÃO adicionado ao filtro PEND")
            }
        }
        
        Timber.d("ClientListViewModel", "✅ Filtro PEND inclusivo: ${clientesPendencias.size} clientes com pendências encontrados")
        
        return clientesPendencias
    }

    /**
     * ✅ NOVO: Verifica se o cliente foi acertado no ciclo especificado
     * ✅ CORREÇÃO: Verificar apenas acertos FINALIZADOS
     */
    private suspend fun clienteFoiAcertadoNoCiclo(clienteId: Long, cicloId: Long): Boolean {
        return try {
            if (cicloId == -1L) {
                Timber.d("ClientListViewModel", "   ⚠️ cicloId inválido (-1), cliente não foi acertado")
                return false
            }
            
            val acertos = appRepository.buscarAcertosPorCicloId(cicloId).first()
            Timber.d("ClientListViewModel", "   🔍 Verificando acertos do cliente $clienteId no ciclo $cicloId")
            Timber.d("ClientListViewModel", "   Total de acertos no ciclo: ${acertos.size}")
            
            // ✅ CORREÇÃO CRÍTICA: Verificar apenas acertos FINALIZADOS
            val foiAcertado = acertos.any { acerto: com.example.gestaobilhares.data.entities.Acerto -> 
                acerto.clienteId == clienteId && acerto.status == com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO 
            }
            
            Timber.d("ClientListViewModel", "   ✅ Cliente $clienteId foi acertado no ciclo $cicloId? $foiAcertado")
            
            // ✅ DEBUG: Log detalhado dos acertos encontrados
            val acertosDoCliente = acertos.filter { it.clienteId == clienteId }
            acertosDoCliente.forEach { acerto ->
                Timber.d("ClientListViewModel", "      Acerto encontrado: ID=${acerto.id}, Status=${acerto.status}, ClienteId=${acerto.clienteId}")
            }
            
            foiAcertado
        } catch (e: Exception) {
            Timber.e("ClientListViewModel", "Erro ao verificar acerto do cliente: ${e.message}", e)
            false
        }
    }

    /**
     * ✅ CORREÇÃO: Verifica se o cliente tem pendências (débito > 300 OU não acertado há mais de 4 meses)
     */
    private suspend fun clienteTemPendencias(clienteId: Long): Boolean {
        return try {
            // Buscar o cliente com débito atual
            val cliente = appRepository.obterClientePorId(clienteId) ?: return false
            
            // ✅ DEBUG: Log para verificar o débito do cliente
            Timber.d("ClientListViewModel", "Verificando pendências - Cliente ${cliente.nome}: débitoAtual = ${com.example.gestaobilhares.core.utils.StringUtils.formatarMoeda(cliente.debitoAtual)}")
            
            // ✅ CRITÉRIO 1: Débito > R$300
            val temDebitoAlto = cliente.debitoAtual > 300.0
            
            // ✅ CRITÉRIO 2: Não acertado há mais de 4 meses
            val ultimoAcerto = appRepository.buscarUltimoAcertoPorCliente(clienteId)
            val semAcertoRecente = when {
                ultimoAcerto == null -> true // Se nunca foi acertado, considerar como pendência
                else -> {
                    val dataAtual = System.currentTimeMillis()
                    val dataUltimoAcerto = ultimoAcerto.dataAcerto
                    val diffEmMeses = ((dataAtual - dataUltimoAcerto) / (1000L * 60 * 60 * 24 * 30)).toInt()
                    diffEmMeses > 4
                }
            }
            
            // ✅ RETORNAR TRUE se atender QUALQUER UM dos critérios
            val temPendencia = temDebitoAlto || semAcertoRecente
            
            Timber.d("ClientListViewModel", "Cliente ${cliente.nome}: temDebitoAlto=$temDebitoAlto, semAcertoRecente=$semAcertoRecente, temPendencia=$temPendencia")
            
            if (temPendencia) {
                Timber.d("ClientListViewModel", "✅ Cliente ${cliente.nome} tem pendência: Débito=${com.example.gestaobilhares.core.utils.StringUtils.formatarMoeda(cliente.debitoAtual)}, SemAcertoRecente=$semAcertoRecente")
            }
            
            temPendencia
        } catch (e: Exception) {
            Timber.e("ClientListViewModel", "Erro ao verificar pendências do cliente: ${e.message}")
            false
        }
    }

    /**
     * ✅ NOVO: Verifica se o cliente tem mesas vinculadas
     */
    private suspend fun temMesasVinculadas(clienteId: Long): Boolean {
        return try {
            val mesas = appRepository.obterMesasPorCliente(clienteId).first()
            mesas.isNotEmpty()
        } catch (e: Exception) {
            Timber.e("ClientListViewModel", "Erro ao verificar mesas vinculadas: ${e.message}")
            false
        }
    }

    /**
     * ✅ NOVO: Filtra clientes por pesquisa avançada
     */
    private suspend fun filtrarPorPesquisaAvancada(
        clientes: List<Cliente>, 
        searchType: SearchType, 
        criteria: String
    ): List<Cliente> {
        return when (searchType) {
            SearchType.NOME_CLIENTE -> {
                clientes.filter { cliente: com.example.gestaobilhares.data.entities.Cliente -> cliente.nome.contains(criteria, ignoreCase = true) }
            }
            SearchType.NUMERO_MESA -> {
                val clientesComMesas = mutableListOf<Cliente>()
                for (cliente in clientes) {
                    val mesas = appRepository.obterMesasPorCliente(cliente.id).first()
                    if (mesas.any { mesa: com.example.gestaobilhares.data.entities.Mesa -> mesa.numero.contains(criteria, ignoreCase = true) }) {
                        clientesComMesas.add(cliente)
                    }
                }
                clientesComMesas
            }
            SearchType.TELEFONE -> {
                clientes.filter { cliente ->
                    cliente.telefone?.contains(criteria, ignoreCase = true) == true
                }
            }
            SearchType.ENDERECO -> {
                clientes.filter { cliente ->
                    val enderecoMatch = cliente.endereco?.contains(criteria, ignoreCase = true) == true
                    val cidadeMatch = cliente.cidade?.contains(criteria, ignoreCase = true) == true
                    enderecoMatch || cidadeMatch
                }
            }
            SearchType.CPF_CNPJ -> {
                val criterioNumerico = criteria.filter { it.isDigit() }
                clientes.filter { cliente ->
                    val documento = cliente.cpfCnpj?.filter { it.isDigit() } ?: ""
                    documento.contains(criterioNumerico)
                }
            }
        }
    }

    /**
     * ✅ FASE 9B: Limpa a busca e restaura lista original
     */
    fun limparBusca() {
        viewModelScope.launch {
            try {
                _buscaAtual.value = ""
                _isAdvancedSearch.value = false
                _searchType.value = null
                _searchCriteria.value = ""
                aplicarFiltrosCombinados()
            } catch (e: Exception) {
                Timber.e("ClientListViewModel", "Erro ao limpar busca: ${e.message}")
            }
        }
    }


    /**
     * ✅ FASE 9A: Obtém o filtro atual para uso na UI
     */
    fun getFiltroAtual(): FiltroCliente {
        return _filtroAtual.value
    }

    /**
     * ✅ NOVO: Aplica filtro geral (Ativos/Inativos/Todos) para dialog
     * Usa conceitos diferentes dos filtros de abas:
     * - ATIVOS: Clientes com mesa OU com débito
     * - INATIVOS: Clientes sem mesa E sem débito
     * - TODOS: Todos os clientes
     */
    fun aplicarFiltroGeral(filtro: FiltroGeralCliente) {
        viewModelScope.launch {
            try {
                _filtroGeral.value = filtro
                showLoading()
                
                val rotaId = _rotaInfo.value?.id
                if (rotaId == null) {
                    Timber.e("ClientListViewModel", "rotaId é null para aplicar filtro geral")
                    hideLoading()
                    return@launch
                }
                
                Timber.d("ClientListViewModel", "🔍 Aplicando filtro geral: $filtro para rotaId: $rotaId")
                
                val clientesFlow = when (filtro) {
                    FiltroGeralCliente.ATIVOS -> {
                        Timber.d("ClientListViewModel", "   → Buscando clientes ATIVOS (com mesa OU débito)")
                        appRepository.buscarClientesAtivos(rotaId)
                    }
                    FiltroGeralCliente.INATIVOS -> {
                        Timber.d("ClientListViewModel", "   → Buscando clientes INATIVOS (sem mesa E sem débito)")
                        appRepository.buscarClientesInativos(rotaId)
                    }
                    FiltroGeralCliente.TODOS -> {
                        Timber.d("ClientListViewModel", "   → Buscando TODOS os clientes")
                        appRepository.obterClientesPorRotaComDebitoAtual(rotaId)
                    }
                }
                
                // Coletar e atualizar a lista
                clientesFlow.collect { clientes ->
                    _clientes.value = clientes
                    _clientesTodos.value = clientes // Atualizar também a lista base
                    hideLoading()
                    
                    Timber.d("ClientListViewModel", "✅ Filtro geral aplicado: ${clientes.size} clientes encontrados")
                    clientes.forEach { cliente ->
                        Timber.d("ClientListViewModel", "   - ${cliente.nome} (débito: R$ ${cliente.debitoAtual})")
                    }
                }
                
            } catch (e: Exception) {
                Timber.e("ClientListViewModel", "Erro ao aplicar filtro geral: ${e.message}", e)
                showError("Erro ao aplicar filtro: ${e.message}")
                hideLoading()
            }
        }
    }

    /**
     * Limpa mensagens de erro
     */
    fun limparErro() {
        clearError()
        Timber.d("ClientListViewModel", "Erro limpo")
    }

    /**
     * ✅ NOVO: Calcula dados do card de progresso do ciclo em tempo real
     */
    private suspend fun calcularDadosProgressoCiclo(clientes: List<Cliente>) {
        try {
            val totalClientes = clientes.size
            _totalClientes.value = totalClientes
            
            // ✅ CORREÇÃO: Buscar ciclo ativo diretamente do repositório
            val rotaId = _rotaInfo.value?.id
            val cicloAtivo = if (rotaId != null) appRepository.buscarCicloAtivo(rotaId) else null
            val cicloId = cicloAtivo?.id ?: -1L
            
            Timber.d("ClientListViewModel", "📊 Calculando progresso - Ciclo ativo: ID=$cicloId, Número=${cicloAtivo?.numeroCiclo}")
            
            // Calcular clientes acertados no ciclo atual
            val clientesAcertados = calcularClientesAcertadosNoCiclo(clientes, cicloId)
            _clientesAcertados.value = clientesAcertados
            
            // Calcular percentual
            val percentual = if (totalClientes > 0) {
                (clientesAcertados * 100) / totalClientes
            } else {
                0
            }
            _percentualAcertados.value = percentual
            _progressoCiclo.value = percentual
            
            // Calcular pendências
            val pendencias = calcularPendencias(clientes)
            _pendencias.value = pendencias
            
            Timber.d("ClientListViewModel", "✅ Dados do progresso calculados: $percentual% de $totalClientes clientes, $pendencias pendências")
            
        } catch (e: Exception) {
            Timber.e("ClientListViewModel", "Erro ao calcular dados do progresso: ${e.message}")
            // Valores padrão em caso de erro
            _percentualAcertados.value = 0
            _totalClientes.value = 0
            _clientesAcertados.value = 0
            _pendencias.value = 0
        }
    }

    /**
     * ✅ CORRIGIDO: Calcula quantos clientes foram acertados no ciclo atual usando dados reais
     * ✅ CORREÇÃO: Verificar apenas acertos FINALIZADOS
     */
    private suspend fun calcularClientesAcertadosNoCiclo(clientes: List<Cliente>, cicloId: Long): Int {
        return try {
            val rotaId = _rotaIdFlow.value ?: return 0
            // Buscar acertos reais do banco de dados para esta rota e ciclo
            val acertos = appRepository.buscarAcertosPorRotaECiclo(rotaId, cicloId)
            
            // ✅ CORREÇÃO CRÍTICA: Contar apenas clientes únicos com acertos FINALIZADOS
            val clientesAcertados = acertos
                .filter { acerto: com.example.gestaobilhares.data.entities.Acerto -> acerto.status == com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO }
                .map { acerto: com.example.gestaobilhares.data.entities.Acerto -> acerto.clienteId }
                .distinct()
            
            Timber.d("ClientListViewModel", "✅ Clientes acertados no ciclo $cicloId: ${clientesAcertados.size} de ${clientes.size}")
            
            clientesAcertados.size
        } catch (e: Exception) {
            Timber.e("ClientListViewModel", "Erro ao calcular clientes acertados: ${e.message}")
            0
        }
    }

    /**
     * ✅ CORRIGIDO: Calcula pendências reais (débitos > R$300 + sem acerto há >4 meses)
     */
    private suspend fun calcularPendencias(clientes: List<Cliente>): Int {
        return try {
            val quatroMesesAtras = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.MONTH, -4)
            }.timeInMillis
            
            var pendencias = 0
            
            for (cliente in clientes) {
                // Buscar último acerto do cliente
                val ultimoAcerto = appRepository.buscarUltimoAcertoPorCliente(cliente.id)
                
                val temPendencia = when {
                    // Cliente com débito > R$300
                    cliente.debitoAtual > 300.0 -> true
                    
                    // Cliente sem acerto há mais de 4 meses
                    ultimoAcerto == null || ultimoAcerto.dataAcerto < quatroMesesAtras -> true
                    
                    else -> false
                }
                
                if (temPendencia) {
                        pendencias++
                }
            }
            
            Timber.d("ClientListViewModel", "✅ Pendências calculadas: $pendencias de ${clientes.size} clientes (débito>R$300 ou sem acerto>4meses)")
            
            pendencias
        } catch (e: Exception) {
            Timber.e("ClientListViewModel", "Erro ao calcular pendências: ${e.message}")
            0
        }
    }

    // Função síncrona para calcular pendências (simplificada para uso no combine)
    private fun calcularPendenciasReaisSync(clientes: List<Cliente>): Int {
        return clientes.count { cliente ->
            val debitoAtual = cliente.debitoAtual
            // Verificar se o cliente tem débito > R$300
            // Esta é a verificação principal para pendências
            debitoAtual > 300.0
        }
    }

    /**
     * ✅ NOVO: Notifica mudança de status da rota
     * ✅ FASE 12.7: Log é suficiente para rastreamento; notificações reativas via StateFlow
     */
    private fun notificarMudancaStatusRota(rotaId: Long) {
        Timber.d("ClientListViewModel", "Notificando mudanca de status da rota: $rotaId")
        // Notificacoes reativas sao gerenciadas via StateFlow observado pelos Fragments
    }

    /**
     * ✅ NOVO: Carrega dados reais da rota em tempo real (clientes ativos e mesas)
     */
    fun carregarDadosRotaEmTempoReal(rotaId: Long) {
        Timber.d("ClientListViewModel", "=== INICIANDO CARREGAMENTO DE DADOS ROTA $rotaId ===")

        viewModelScope.launch {
            try {
                // Primeiro carregar clientes ativos
                val clientes = appRepository.obterClientesPorRota(rotaId).first()
                val clientesAtivos = clientes.count { it.ativo }

                Timber.d("ClientListViewModel", "=== DADOS CLIENTES CARREGADOS ===")
                Timber.d("ClientListViewModel", "Total clientes: ${clientes.size}, Ativos: $clientesAtivos")

                // Depois carregar mesas da rota
                val mesasDaRota = appRepository.buscarMesasPorRota(rotaId).first()
                val totalMesas = mesasDaRota.size

                Timber.d("ClientListViewModel", "=== DADOS MESAS CARREGADOS ===")
                Timber.d("ClientListViewModel", "Mesas encontradas: $totalMesas")
                mesasDaRota.forEach { mesa ->
                    Timber.d("ClientListViewModel", "Mesa: ${mesa.numero} (ID: ${mesa.id}, ClienteId: ${mesa.clienteId})")
                }

                // Atualizar dados apenas uma vez com ambos os valores
                val dadosAtualizados = DadosRotaReais(
                    totalClientes = clientesAtivos,
                    totalMesas = totalMesas
                )

                Timber.d("ClientListViewModel", "=== ATUALIZANDO DADOS FINAIS ===")
                Timber.d("ClientListViewModel", "Dados finais: ${dadosAtualizados.totalClientes} clientes, ${dadosAtualizados.totalMesas} mesas")

                _dadosRotaReais.value = dadosAtualizados

            } catch (e: Exception) {
                Timber.e("ClientListViewModel", "Erro ao carregar dados da rota: ${e.message}", e)
                // Valores padrão em caso de erro
                _dadosRotaReais.value = DadosRotaReais(0, 0)
            }
        }
    }

    // Funções obsoletas removidas - card de progresso agora é totalmente reativo
    
    // ==================== FASE 4B: LAZY LOADING ====================
    
    /**
     * ✅ FASE 4B: Carregar clientes com lazy loading
     * TODO: PaginationManager não existe - comentar temporariamente
     */
    fun carregarClientesComLazyLoading(rotaId: Long) {
        viewModelScope.launch {
            try {
                showLoading()
                
                // TODO: PaginationManager não existe - usar carregamento direto temporariamente
                val clientes = appRepository.buscarClientesPorRotaComCache(rotaId).first()
                _clientesTodos.value = clientes
                
                Timber.d("ClientListViewModel", "✅ Carregamento direto: ${clientes.size} clientes carregados")
                
                /*
                // Configurar callback de carregamento
                // ✅ FASE 12.5: Callback precisa ser suspend para remover runBlocking
                // Nota: Se o PaginationManager não suportar suspend, pode ser necessário ajustar
                paginationManager.setLoadDataCallback { offset: Int, limit: Int ->
                    kotlinx.coroutines.runBlocking {
                        appRepository.buscarClientesPorRotaComCache(rotaId).first()
                            .drop(offset)
                            .take(limit)
                    }
                }
                
                // Carregar página inicial
                val initialData = paginationManager.loadInitialPage()
                _clientesTodos.value = initialData
                
                Timber.d("ClientListViewModel", "✅ Lazy loading: ${initialData.size} clientes carregados")
                */
                
            } catch (e: Exception) {
                Timber.e("ClientListViewModel", "❌ Erro no lazy loading: ${e.message}")
                showError("Erro ao carregar clientes: ${e.message}", e)
            } finally {
                hideLoading()
            }
        }
    }
    
    /**
     * ✅ FASE 4B: Carregar próxima página
     * TODO: PaginationManager não existe - comentar temporariamente
     */
    fun carregarProximaPagina() {
        // TODO: PaginationManager não existe - não fazer nada temporariamente
                Timber.d("ClientListViewModel", "⚠️ PaginationManager não implementado - carregarProximaPagina ignorado")
        /*
        viewModelScope.launch {
            try {
                if (paginationManager.hasMoreData.value && !paginationManager.isLoading.value) {
                    val nextPageData = paginationManager.loadNextPage()
                    val currentData = _clientesTodos.value.toMutableList()
                    currentData.addAll(nextPageData)
                    _clientesTodos.value = currentData
                    
                    Timber.d("ClientListViewModel", "✅ Próxima página carregada: ${nextPageData.size} clientes")
                }
            } catch (e: Exception) {
                Timber.e("ClientListViewModel", "❌ Erro ao carregar próxima página: ${e.message}")
            }
        }
        */
    }
    
    /**
     * ✅ FASE 4B: Obter estatísticas de paginação
     * TODO: PaginationManager não existe - comentar temporariamente
     */
    fun obterEstatisticasPaginacao(): String {
        // TODO: PaginationManager não existe - retornar string vazia temporariamente
        return "PaginationManager não implementado"
        // return paginationManager.getStats()
    }
    
    /**
     * ✅ FASE 4B: Verificar se deve pré-carregar
     * TODO: PaginationManager não existe - comentar temporariamente
     */
    fun devePrecarregarProximaPagina(@Suppress("UNUSED_PARAMETER") posicaoAtual: Int): Boolean {
        // TODO: PaginationManager não existe - retornar false temporariamente
        return false
        // return paginationManager.shouldPreloadNextPage(posicaoAtual)
    }
    
    /**
     * ✅ FASE 4B: Limpar cache de paginação
     * TODO: PaginationManager não existe - comentar temporariamente
     */
    fun limparCachePaginacao() {
        // TODO: PaginationManager não existe - não fazer nada temporariamente
        Timber.d("ClientListViewModel", "⚠️ PaginationManager não implementado - limparCachePaginacao ignorado")
        // paginationManager.clearCache()
        // Timber.d("ClientListViewModel", "🧹 Cache de paginação limpo")
    }
} 

// Data class para o card de progresso do ciclo
data class CicloProgressoCard(
    val receita: Double,
    val despesas: Double,
    val saldo: Double,
    val percentual: Int,
    val clientesAcertados: Int,
    val totalClientes: Int,
    val pendencias: Int,
    val debitoTotal: Double // NOVO
) 

// ✅ NOVO: Data class para dados reais da rota
data class DadosRotaReais(
    val totalClientes: Int,
    val totalMesas: Int
)

