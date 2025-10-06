package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.StatusRota
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.example.gestaobilhares.data.entities.Despesa
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.data.repository.RotaRepository
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
// // import dagger.hilt.android.lifecycle.HiltViewModel // REMOVIDO: Hilt nao e mais usado // ✅ REMOVIDO: Hilt não é mais usado
// import javax.inject.Inject // REMOVIDO: Hilt nao e mais usado

/**
 * Filtros disponíveis para a lista de clientes
 */
enum class FiltroCliente {
    TODOS, ACERTADOS, NAO_ACERTADOS, PENDENCIAS
}

/**
 * ViewModel para ClientListFragment
 * ✅ FASE 8C: Integração com sistema de ciclo de acerto real
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientListViewModel constructor(
    private val clienteRepository: ClienteRepository,
    private val rotaRepository: RotaRepository,
    private val cicloAcertoRepository: CicloAcertoRepository,
    private val acertoRepository: AcertoRepository,
    private val appRepository: AppRepository
) : BaseViewModel() {

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
        // Configurar fluxo reativo baseado no rotaId
        viewModelScope.launch {
            _rotaIdFlow.flatMapLatest { rotaId ->
                if (rotaId == null) {
                    return@flatMapLatest kotlinx.coroutines.flow.flowOf(null)
                }
                
                // Observar ciclo ativo da rota
                cicloAcertoRepository.observarCicloAtivo(rotaId)
            }.collect { ciclo ->
                android.util.Log.d("DEBUG_DIAG", "[CARD] cicloAtivo retornado: id=${ciclo?.id}, status=${ciclo?.status}, dataInicio=${ciclo?.dataInicio}, dataFim=${ciclo?.dataFim}")
                _cicloAtivo.value = ciclo
            }
        }

        // Configurar card de progresso reativo
        viewModelScope.launch {
            _rotaIdFlow.flatMapLatest { rotaId ->
                if (rotaId == null) {
                    return@flatMapLatest kotlinx.coroutines.flow.flowOf(null)
                }
                
                val cicloAtivoFlow = cicloAcertoRepository.observarCicloAtivo(rotaId)
                val todosClientesFlow = clienteRepository.obterClientesPorRota(rotaId)

                combine(cicloAtivoFlow, todosClientesFlow) { ciclo, todosClientes ->
                    val debitoTotal = todosClientes.sumOf { it.debitoAtual }
                    if (ciclo == null) {
                        return@combine CicloProgressoCard(0.0, 0.0, 0.0, 0, 0, todosClientes.size, 0, debitoTotal)
                    }

                    val acertos = acertoRepository.buscarPorRotaECicloId(rotaId, ciclo.id).first()
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
            }.collect {
                _cicloProgressoCard.value = it
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
                rotaRepository.obterRotaPorId(rotaId).collect { rota ->
                    _rotaInfo.value = rota
                    rota?.let {
                        // ✅ CORREÇÃO: Carregar ciclo primeiro, depois status
                        carregarCicloAcertoReal(it)
                        // ✅ CORREÇÃO: Carregar status após ciclo estar carregado
                        carregarStatusRota()
                    }
                }
            } catch (e: Exception) {
                logError("ROTA_LOAD", "Erro ao carregar rota: ${e.message}", e)
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
     * ✅ FASE 9B: Carrega clientes da rota com filtros combinados
     */
    fun carregarClientes(rotaId: Long) {
        AppLogger.log("ClientListVM", "carregarClientes chamado para rotaId: $rotaId")
        
        // Atualizar o rotaId no fluxo se necessário
        if (_rotaIdFlow.value != rotaId) {
            _rotaIdFlow.value = rotaId
        }
        
        viewModelScope.launch {
            try {
                showLoading()
                clienteRepository.obterClientesPorRota(rotaId).collect { clientes ->
                    _clientesTodos.value = clientes
                    aplicarFiltrosCombinados() // Aplicar filtros após carregar
                    
                    // ✅ NOVO: Calcular dados do card de progresso
                    calcularDadosProgressoCiclo(clientes)
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
                
                // Buscar próximo número de ciclo
                val proximoCiclo = cicloAcertoRepository.buscarProximoNumeroCiclo(rota.id, anoAtual)
                
                // ✅ NOVO: Salvar pendências do ciclo anterior antes de reinicializar
                val pendenciasCicloAnterior = _pendencias.value
                
                // Criar novo ciclo de acerto
                val novoCiclo = CicloAcertoEntity(
                    rotaId = rota.id,
                    numeroCiclo = proximoCiclo,
                    ano = anoAtual,
                    dataInicio = com.example.gestaobilhares.utils.DateUtils.obterDataAtual(),
                    dataFim = Date(), // Será atualizado quando finalizar
                    status = StatusCicloAcerto.EM_ANDAMENTO,
                    criadoPor = "Sistema" // TODO: Implementar UserSessionManager para pegar usuário atual
                )
                
                val cicloId = cicloAcertoRepository.inserirOuAtualizarCiclo(novoCiclo)
                
                // Atualizar estado
                _cicloAcerto.value = proximoCiclo
                _cicloAcertoEntity.value = novoCiclo.copy(id = cicloId)
                _cicloAtivo.value = novoCiclo.copy(id = cicloId) // ✅ CORREÇÃO: Atualizar ciclo ativo
                _statusCiclo.value = StatusCicloAcerto.EM_ANDAMENTO
                _statusRota.value = StatusRota.EM_ANDAMENTO
                
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
                
                logState("CICLO_FINALIZAR", "Iniciando finalização da rota ${rota.nome} - Ciclo ${cicloAtual.numeroCiclo}")
                
                // Centralizar a lógica de finalização no repositório
                cicloAcertoRepository.finalizarCiclo(cicloAtual.id, Date())
                
                // Recarregar o ciclo para obter os dados atualizados (status e débito total)
                val cicloFinalizado = cicloAcertoRepository.buscarCicloPorId(cicloAtual.id)
                
                // Atualizar estado da UI
                _cicloAcertoEntity.value = cicloFinalizado
                _cicloAtivo.value = cicloFinalizado
                _statusCiclo.value = StatusCicloAcerto.FINALIZADO
                _statusRota.value = StatusRota.FINALIZADA
                
                logState("CICLO_FINALIZAR", "Ciclo ${cicloAtual.numeroCiclo} finalizado com sucesso via repositório")
                logState("CICLO_FINALIZAR", "Status da rota atualizado para: ${_statusRota.value}")
                
                // ✅ CORREÇÃO: Não recarregar rota inteira, apenas atualizar status
                // carregarRota(rota.id) // REMOVIDO para evitar loop
                
                // ✅ NOTIFICAR MUDANÇA DE STATUS para atualização em tempo real
                notificarMudancaStatusRota(rota.id)
                
            } catch (e: Exception) {
                logError("CICLO_FINALIZAR", "Erro ao finalizar rota: ${e.message}", e)
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
            android.util.Log.d("ClientListViewModel", "🔄 Carregando ciclo para rota ${rota.nome} (ID: ${rota.id})")
            
            // ✅ CORREÇÃO: Usar a mesma lógica do AppRepository.obterCicloAtualRota()
            val emAndamento = cicloAcertoRepository.buscarCicloAtivo(rota.id)
            
            if (emAndamento != null) {
                // Ciclo em andamento - mostrar o número atual
                _cicloAcerto.value = emAndamento.numeroCiclo
                _cicloAcertoEntity.value = emAndamento
                _cicloAtivo.value = emAndamento
                _statusCiclo.value = emAndamento.status
                _progressoCiclo.value = emAndamento.percentualConclusao
                
                android.util.Log.d("ClientListViewModel", "✅ Ciclo em andamento carregado: ${emAndamento.numeroCiclo}º Acerto (ID: ${emAndamento.id}, Status: ${emAndamento.status})")
            } else {
                // ✅ CORREÇÃO: Nenhum ciclo em andamento - espelhar o AppRepository exibindo o ÚLTIMO ciclo finalizado
                val ultimoCiclo = cicloAcertoRepository.buscarUltimoCicloPorRota(rota.id)
                if (ultimoCiclo != null) {
                    _cicloAcerto.value = ultimoCiclo.numeroCiclo
                    _cicloAcertoEntity.value = ultimoCiclo
                    _cicloAtivo.value = ultimoCiclo
                    _statusCiclo.value = ultimoCiclo.status
                    _progressoCiclo.value = ultimoCiclo.percentualConclusao
                    android.util.Log.d("ClientListViewModel", "🔄 Nenhum ciclo em andamento, exibindo último finalizado: ${ultimoCiclo.numeroCiclo}º Acerto (Status: ${ultimoCiclo.status})")
                } else {
                    _cicloAcerto.value = 1
                    _cicloAcertoEntity.value = null
                    _cicloAtivo.value = null
                    _statusCiclo.value = StatusCicloAcerto.FINALIZADO
                    _progressoCiclo.value = 0
                    android.util.Log.d("ClientListViewModel", "🆕 Primeira vez nesta rota, exibindo 1º Acerto")
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ClientListViewModel", "Erro ao carregar ciclo: ${e.message}", e)
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
                logError("BUSCA", "Erro na busca: ${e.message}", e)
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
                logError("PESQUISA_AVANCADA", "Erro na pesquisa avançada: ${e.message}", e)
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
            cicloAcertoRepository.buscarUltimoCicloPorRota(rotaId)
        } catch (e: Exception) {
            android.util.Log.e("ClientListViewModel", "Erro ao buscar último ciclo finalizado: ${e.message}")
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
                android.util.Log.d("ClientListViewModel", "🔄 Forçando atualização do ciclo atual para rota ${rota.nome}")
                carregarCicloAcertoReal(rota)
            } catch (e: Exception) {
                android.util.Log.e("ClientListViewModel", "Erro ao atualizar ciclo atual: ${e.message}")
            }
        }
    }

    /**
     * ✅ FASE 9B: Aplica filtro à lista de clientes com filtros combinados
     */
    private suspend fun aplicarFiltrosCombinados() {
        val query = com.example.gestaobilhares.utils.StringUtils.removerEspacosExtras(_buscaAtual.value)
        val filtro = _filtroAtual.value
        val todos = _clientesTodos.value
        val isAdvancedSearch = _isAdvancedSearch.value
        val searchType = _searchType.value
        val searchCriteria = com.example.gestaobilhares.utils.StringUtils.removerEspacosExtras(_searchCriteria.value)
        
        // ✅ CORREÇÃO: Filtro PENDENCIAS agora é inclusivo - mostra todos os clientes com pendências
        val filtradosPorStatus = when (filtro) {
            FiltroCliente.TODOS -> todos
            FiltroCliente.ACERTADOS -> filtrarClientesAcertados(todos)
            FiltroCliente.NAO_ACERTADOS -> filtrarClientesNaoAcertados(todos)
            FiltroCliente.PENDENCIAS -> filtrarClientesPendenciasInclusivo(todos)
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
        
        // Atualizar lista filtrada
        _clientes.value = resultadoFinal
    }

    /**
     * ✅ NOVO: Filtra clientes acertados no ciclo atual
     */
    private suspend fun filtrarClientesAcertados(clientes: List<Cliente>): List<Cliente> {
        val clientesAcertados = mutableListOf<Cliente>()
        val cicloId = _cicloAcertoEntity.value?.id ?: -1L
        
        for (cliente in clientes) {
            if (clienteFoiAcertadoNoCiclo(cliente.id, cicloId)) {
                clientesAcertados.add(cliente)
            }
        }
        
        return clientesAcertados
    }

    /**
     * ✅ NOVO: Filtra clientes não acertados no ciclo atual
     */
    private suspend fun filtrarClientesNaoAcertados(clientes: List<Cliente>): List<Cliente> {
        val clientesNaoAcertados = mutableListOf<Cliente>()
        val cicloId = _cicloAcertoEntity.value?.id ?: -1L
        
        for (cliente in clientes) {
            if (!clienteFoiAcertadoNoCiclo(cliente.id, cicloId)) {
                clientesNaoAcertados.add(cliente)
            }
        }
        
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
        
        android.util.Log.d("ClientListViewModel", "🔍 Iniciando filtro PEND inclusivo para ${clientes.size} clientes")
        
        for (cliente in clientes) {
            // ✅ DEBUG: Log para verificar o débito de cada cliente
            android.util.Log.d("ClientListViewModel", "Verificando cliente ${cliente.nome}: débitoAtual = ${com.example.gestaobilhares.utils.StringUtils.formatarMoeda(cliente.debitoAtual)}")
            
            // ✅ CRITÉRIO INCLUSIVO: Se o cliente tem pendências, incluir independente do status de acerto
            if (clienteTemPendencias(cliente.id)) {
                clientesPendencias.add(cliente)
                android.util.Log.d("ClientListViewModel", "✅ Cliente ${cliente.nome} adicionado ao filtro PEND")
            } else {
                android.util.Log.d("ClientListViewModel", "❌ Cliente ${cliente.nome} NÃO adicionado ao filtro PEND")
            }
        }
        
        android.util.Log.d("ClientListViewModel", "✅ Filtro PEND inclusivo: ${clientesPendencias.size} clientes com pendências encontrados")
        
        return clientesPendencias
    }

    /**
     * ✅ NOVO: Verifica se o cliente foi acertado no ciclo especificado
     */
    private suspend fun clienteFoiAcertadoNoCiclo(clienteId: Long, cicloId: Long): Boolean {
        return try {
            if (cicloId == -1L) return false
            
            val acertos = appRepository.buscarAcertosPorCicloId(cicloId).first()
            acertos.any { it.clienteId == clienteId }
        } catch (e: Exception) {
            android.util.Log.e("ClientListViewModel", "Erro ao verificar acerto do cliente: ${e.message}")
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
            android.util.Log.d("ClientListViewModel", "Verificando pendências - Cliente ${cliente.nome}: débitoAtual = ${com.example.gestaobilhares.utils.StringUtils.formatarMoeda(cliente.debitoAtual)}")
            
            // ✅ CRITÉRIO 1: Débito > R$300
            val temDebitoAlto = cliente.debitoAtual > 300.0
            
            // ✅ CRITÉRIO 2: Não acertado há mais de 4 meses
            val ultimoAcerto = appRepository.buscarUltimoAcertoPorCliente(clienteId)
            val semAcertoRecente = when {
                ultimoAcerto == null -> true // Se nunca foi acertado, considerar como pendência
                else -> {
                    val dataAtual = java.util.Date()
                    val dataUltimoAcerto = ultimoAcerto.dataAcerto
                    val diffEmMeses = ((dataAtual.time - dataUltimoAcerto.time) / (1000L * 60 * 60 * 24 * 30)).toInt()
                    diffEmMeses > 4
                }
            }
            
            // ✅ RETORNAR TRUE se atender QUALQUER UM dos critérios
            val temPendencia = temDebitoAlto || semAcertoRecente
            
            android.util.Log.d("ClientListViewModel", "Cliente ${cliente.nome}: temDebitoAlto=$temDebitoAlto, semAcertoRecente=$semAcertoRecente, temPendencia=$temPendencia")
            
            if (temPendencia) {
                android.util.Log.d("ClientListViewModel", "✅ Cliente ${cliente.nome} tem pendência: Débito=${com.example.gestaobilhares.utils.StringUtils.formatarMoeda(cliente.debitoAtual)}, SemAcertoRecente=$semAcertoRecente")
            }
            
            temPendencia
        } catch (e: Exception) {
            android.util.Log.e("ClientListViewModel", "Erro ao verificar pendências do cliente: ${e.message}")
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
            android.util.Log.e("ClientListViewModel", "Erro ao verificar mesas vinculadas: ${e.message}")
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
                clientes.filter { it.nome.contains(criteria, ignoreCase = true) }
            }
            SearchType.NUMERO_MESA -> {
                val clientesComMesas = mutableListOf<Cliente>()
                for (cliente in clientes) {
                    val mesas = appRepository.obterMesasPorCliente(cliente.id).first()
                    if (mesas.any { it.numero.contains(criteria, ignoreCase = true) }) {
                        clientesComMesas.add(cliente)
                    }
                }
                clientesComMesas
            }
            SearchType.CIDADE -> {
                clientes.filter { cliente ->
                    cliente.cidade?.contains(criteria, ignoreCase = true) == true
                }
            }
            SearchType.CPF -> {
                clientes.filter { cliente ->
                    val cpfFormatado = com.example.gestaobilhares.utils.StringUtils.formatarCPF(cliente.cpfCnpj)
                    cpfFormatado.contains(criteria, ignoreCase = true) || 
                    cliente.cpfCnpj?.contains(criteria, ignoreCase = true) == true
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
                android.util.Log.e("ClientListViewModel", "Erro ao limpar busca: ${e.message}")
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
     * Limpa mensagens de erro
     */
    fun limparErro() {
        clearError()
    }

    /**
     * ✅ NOVO: Calcula dados do card de progresso do ciclo em tempo real
     */
    private suspend fun calcularDadosProgressoCiclo(clientes: List<Cliente>) {
        try {
            val totalClientes = clientes.size
            _totalClientes.value = totalClientes
            
            // Calcular clientes acertados no ciclo atual
            val cicloId = _cicloAcertoEntity.value?.id ?: -1L
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
            
            android.util.Log.d("ClientListViewModel", "✅ Dados do progresso calculados: $percentual% de $totalClientes clientes, $pendencias pendências")
            
        } catch (e: Exception) {
            android.util.Log.e("ClientListViewModel", "Erro ao calcular dados do progresso: ${e.message}")
            // Valores padrão em caso de erro
            _percentualAcertados.value = 0
            _totalClientes.value = 0
            _clientesAcertados.value = 0
            _pendencias.value = 0
        }
    }

    /**
     * ✅ CORRIGIDO: Calcula quantos clientes foram acertados no ciclo atual usando dados reais
     */
    private suspend fun calcularClientesAcertadosNoCiclo(clientes: List<Cliente>, cicloId: Long): Int {
        return try {
            val rotaId = _rotaIdFlow.value ?: return 0
            // Buscar acertos reais do banco de dados para esta rota e ciclo
            val acertos = acertoRepository.buscarPorRotaECicloId(rotaId, cicloId).first()
            
            // Contar clientes únicos que foram acertados
            val clientesAcertados = acertos.map { it.clienteId }.distinct()
            
            android.util.Log.d("ClientListViewModel", "✅ Clientes acertados no ciclo $cicloId: ${clientesAcertados.size} de ${clientes.size}")
            
            clientesAcertados.size
        } catch (e: Exception) {
            android.util.Log.e("ClientListViewModel", "Erro ao calcular clientes acertados: ${e.message}")
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
            }.time
            
            var pendencias = 0
            
            for (cliente in clientes) {
                // Buscar último acerto do cliente
                val ultimoAcerto = acertoRepository.buscarUltimoAcertoPorCliente(cliente.id)
                
                val temPendencia = when {
                    // Cliente com débito > R$300
                    cliente.debitoAtual > 300.0 -> true
                    
                    // Cliente sem acerto há mais de 4 meses
                    ultimoAcerto == null || ultimoAcerto.dataAcerto.before(quatroMesesAtras) -> true
                    
                    else -> false
                }
                
                if (temPendencia) {
                        pendencias++
                }
            }
            
            android.util.Log.d("ClientListViewModel", "✅ Pendências calculadas: $pendencias de ${clientes.size} clientes (débito>R$300 ou sem acerto>4meses)")
            
            pendencias
        } catch (e: Exception) {
            android.util.Log.e("ClientListViewModel", "Erro ao calcular pendências: ${e.message}")
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
     */
    private fun notificarMudancaStatusRota(rotaId: Long) {
        // TODO: Implementar notificação via EventBus ou similar
        android.util.Log.d("ClientListViewModel", "🔄 Notificando mudança de status da rota: $rotaId")
    }

    /**
     * ✅ NOVO: Carrega dados reais da rota em tempo real (clientes ativos e mesas)
     */
    fun carregarDadosRotaEmTempoReal(rotaId: Long) {
        android.util.Log.d("ClientListViewModel", "=== INICIANDO CARREGAMENTO DE DADOS ROTA $rotaId ===")

        viewModelScope.launch {
            try {
                // Primeiro carregar clientes ativos
                val clientes = clienteRepository.obterClientesPorRota(rotaId).first()
                val clientesAtivos = clientes.count { it.ativo }

                android.util.Log.d("ClientListViewModel", "=== DADOS CLIENTES CARREGADOS ===")
                android.util.Log.d("ClientListViewModel", "Total clientes: ${clientes.size}, Ativos: $clientesAtivos")

                // Depois carregar mesas da rota
                val mesasDaRota = appRepository.buscarMesasPorRota(rotaId).first()
                val totalMesas = mesasDaRota.size

                android.util.Log.d("ClientListViewModel", "=== DADOS MESAS CARREGADOS ===")
                android.util.Log.d("ClientListViewModel", "Mesas encontradas: $totalMesas")
                mesasDaRota.forEach { mesa ->
                    android.util.Log.d("ClientListViewModel", "Mesa: ${mesa.numero} (ID: ${mesa.id}, ClienteId: ${mesa.clienteId})")
                }

                // Atualizar dados apenas uma vez com ambos os valores
                val dadosAtualizados = DadosRotaReais(
                    totalClientes = clientesAtivos,
                    totalMesas = totalMesas
                )

                android.util.Log.d("ClientListViewModel", "=== ATUALIZANDO DADOS FINAIS ===")
                android.util.Log.d("ClientListViewModel", "Dados finais: ${dadosAtualizados.totalClientes} clientes, ${dadosAtualizados.totalMesas} mesas")

                _dadosRotaReais.value = dadosAtualizados

            } catch (e: Exception) {
                android.util.Log.e("ClientListViewModel", "Erro ao carregar dados da rota: ${e.message}", e)
                // Valores padrão em caso de erro
                _dadosRotaReais.value = DadosRotaReais(0, 0)
            }
        }
    }

    // Funções obsoletas removidas - card de progresso agora é totalmente reativo
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

