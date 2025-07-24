package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/**
 * Filtros disponíveis para a lista de clientes
 */
enum class FiltroCliente {
    TODOS, ATIVOS, DEVEDORES
}

/**
 * ViewModel para ClientListFragment
 * ✅ FASE 8C: Integração com sistema de ciclo de acerto real
 */
class ClientListViewModel(
    private val clienteRepository: ClienteRepository,
    private val rotaRepository: RotaRepository,
    private val cicloAcertoRepository: CicloAcertoRepository,
    private val acertoRepository: AcertoRepository,
    private val appRepository: AppRepository
) : ViewModel() {

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
    private val _filtroAtual = MutableStateFlow(FiltroCliente.ATIVOS)
    
    // ✅ FASE 9B: Lista de clientes filtrados
    private val _clientes = MutableStateFlow<List<Cliente>>(emptyList())
    val clientes: StateFlow<List<Cliente>> = _clientes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var rotaId: Long = 0

    // Novo: ciclo ativo reativo
    private val _cicloAtivo = MutableStateFlow<CicloAcertoEntity?>(null)
    val cicloAtivo: StateFlow<CicloAcertoEntity?> = _cicloAtivo.asStateFlow()

    // StateFlow para o card de progresso do ciclo
    private val _cicloProgressoCard = MutableStateFlow<CicloProgressoCard?>(null)
    val cicloProgressoCard: StateFlow<CicloProgressoCard?> = _cicloProgressoCard.asStateFlow()

    init {
        viewModelScope.launch {
            // Observa o ciclo ativo da rota
            cicloAcertoRepository.observarCicloAtivo(rotaId).collect {
                android.util.Log.d("DEBUG_DIAG", "[CARD] cicloAtivo retornado: id=${it?.id}, status=${it?.status}, dataInicio=${it?.dataInicio}, dataFim=${it?.dataFim}")
                _cicloAtivo.value = it
            }
        }
        viewModelScope.launch {
            combine(
                cicloAtivo,
                clientes,
                clienteRepository.obterClientesPorRota(rotaId) // Adiciona o Flow de clientes para o débito total
            ) { ciclo, clientesVisiveis, todosClientes ->
                Triple(ciclo, clientesVisiveis, todosClientes)
            }.flatMapLatest { (ciclo, clientesVisiveis, todosClientes) ->
                if (ciclo == null) {
                    val debitoTotal = todosClientes.sumOf { it.debitoAtual }
                    return@flatMapLatest kotlinx.coroutines.flow.flowOf(
                        CicloProgressoCard(0.0, 0.0, 0.0, 0, 0, todosClientes.size, 0, debitoTotal)
                    )
                }
                
                val debitoTotal = todosClientes.sumOf { it.debitoAtual }

                combine(
                    acertoRepository.buscarPorRotaECicloId(rotaId, ciclo.id),
                    appRepository.buscarDespesasPorCicloId(ciclo.id)
                ) { acertos, despesas ->
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
        this.rotaId = rotaId
        viewModelScope.launch {
            try {
                _isLoading.value = true
                rotaRepository.obterRotaPorId(rotaId).collect { rota ->
                    _rotaInfo.value = rota
                    rota?.let {
                        // ✅ FASE 8C: Carregar ciclo real do banco de dados
                        carregarCicloAcertoReal(it)
                        // Carregar status atual da rota
                        carregarStatusRota(it)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ClientListViewModel", "Erro ao carregar rota: ${e.message}")
                _errorMessage.value = "Erro ao carregar informações da rota: ${e.message}"
                // Definir valores padrão para evitar crash
                _rotaInfo.value = Rota(id = rotaId, nome = "Rota $rotaId", ativa = true)
                _statusRota.value = StatusRota.PAUSADA
                _cicloAcerto.value = 1
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ FASE 9B: Carrega clientes da rota com filtros combinados
     */
    fun carregarClientes(rotaId: Long) {
        AppLogger.log("ClientListVM", "carregarClientes chamado para rotaId: $rotaId")
        viewModelScope.launch {
            try {
                _isLoading.value = true
                clienteRepository.obterClientesPorRota(rotaId).collect { clientes ->
                    _clientesTodos.value = clientes
                    aplicarFiltrosCombinados() // Aplicar filtros após carregar
                    
                    // ✅ NOVO: Calcular dados do card de progresso
                    calcularDadosProgressoCiclo(clientes)
                }
            } catch (e: Exception) {
                android.util.Log.e("ClientListViewModel", "Erro ao carregar clientes: ${e.message}")
                _errorMessage.value = "Erro ao carregar clientes: ${e.message}"
                // Definir lista vazia para evitar crash
                _clientesTodos.value = emptyList()
                _clientes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ FASE 9B: Aplica filtro à lista de clientes com filtros combinados
     */
    fun aplicarFiltro(filtro: FiltroCliente) {
        _filtroAtual.value = filtro
        aplicarFiltrosCombinados()
    }

    /**
     * ✅ FASE 8C: Inicia a rota criando um novo ciclo de acerto persistente
     */
    fun iniciarRota() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val rota = _rotaInfo.value ?: return@launch
                val anoAtual = Calendar.getInstance().get(Calendar.YEAR)
                
                // Buscar próximo número de ciclo
                val proximoCiclo = cicloAcertoRepository.buscarProximoNumeroCiclo(rota.id, anoAtual)
                
                // ✅ NOVO: Salvar pendências do ciclo anterior antes de reinicializar
                val pendenciasCicloAnterior = _pendencias.value
                
                // Criar novo ciclo de acerto
                val novoCiclo = CicloAcertoEntity(
                    rotaId = rota.id,
                    numeroCiclo = proximoCiclo,
                    ano = anoAtual,
                    dataInicio = Date(),
                    dataFim = Date(), // Será atualizado quando finalizar
                    status = StatusCicloAcerto.EM_ANDAMENTO,
                    criadoPor = "Sistema" // TODO: Pegar usuário atual
                )
                
                val cicloId = cicloAcertoRepository.inserirOuAtualizarCiclo(novoCiclo)
                
                // Atualizar estado
                _cicloAcerto.value = proximoCiclo
                _cicloAcertoEntity.value = novoCiclo.copy(id = cicloId)
                _statusCiclo.value = StatusCicloAcerto.EM_ANDAMENTO
                _statusRota.value = StatusRota.EM_ANDAMENTO
                
                // ✅ NOVO: Reinicializar campos do card de progresso para o novo ciclo
                _percentualAcertados.value = 0
                _clientesAcertados.value = 0
                _progressoCiclo.value = 0
                
                // ✅ NOVO: Manter pendências do ciclo anterior
                _pendencias.value = pendenciasCicloAnterior
                
                android.util.Log.d("ClientListViewModel", "✅ Ciclo $proximoCiclo iniciado com sucesso - campos reinicializados, pendências mantidas: $pendenciasCicloAnterior")
                
            } catch (e: Exception) {
                android.util.Log.e("ClientListViewModel", "Erro ao iniciar rota: ${e.message}", e)
                _errorMessage.value = "Erro ao iniciar rota: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ FASE 8C: Finaliza a rota atual persistindo o ciclo
     */
    fun finalizarRota() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val cicloAtual = _cicloAcertoEntity.value ?: return@launch
                
                // Centralizar a lógica de finalização no repositório
                cicloAcertoRepository.finalizarCiclo(cicloAtual.id, Date())
                
                // Recarregar o ciclo para obter os dados atualizados (status e débito total)
                val cicloFinalizado = cicloAcertoRepository.buscarCicloPorId(cicloAtual.id)
                
                // Atualizar estado da UI
                _cicloAcertoEntity.value = cicloFinalizado
                _statusCiclo.value = StatusCicloAcerto.FINALIZADO
                _statusRota.value = StatusRota.FINALIZADA
                
                android.util.Log.d("ClientListViewModel", "✅ Ciclo ${cicloAtual.numeroCiclo} finalizado com sucesso via repositório")
                
            } catch (e: Exception) {
                android.util.Log.e("ClientListViewModel", "Erro ao finalizar rota: ${e.message}", e)
                _errorMessage.value = "Erro ao finalizar rota: ${e.message}"
            } finally {
                _isLoading.value = false
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
     */
    private suspend fun carregarCicloAcertoReal(rota: Rota) {
        try {
            // Buscar ciclo em andamento primeiro
            var cicloAtual = cicloAcertoRepository.buscarCicloAtivo(rota.id)
            
            // Se não há ciclo em andamento, buscar o último ciclo
            if (cicloAtual == null) {
                cicloAtual = cicloAcertoRepository.buscarEstatisticasRota(rota.id)
            }
            
            if (cicloAtual != null) {
                _cicloAcerto.value = cicloAtual.numeroCiclo
                _cicloAcertoEntity.value = cicloAtual
                _statusCiclo.value = cicloAtual.status
                _progressoCiclo.value = cicloAtual.percentualConclusao
                
                android.util.Log.d("ClientListViewModel", "✅ Ciclo carregado: ${cicloAtual.titulo}")
            } else {
                // Primeiro ciclo da rota
                _cicloAcerto.value = 1
                _cicloAcertoEntity.value = null
                _statusCiclo.value = StatusCicloAcerto.FINALIZADO
                _progressoCiclo.value = 0
                
                android.util.Log.d("ClientListViewModel", "🆕 Primeiro ciclo da rota")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ClientListViewModel", "Erro ao carregar ciclo: ${e.message}", e)
            // Valores padrão em caso de erro
            _cicloAcerto.value = 1
            _cicloAcertoEntity.value = null
            _statusCiclo.value = StatusCicloAcerto.FINALIZADO
            _progressoCiclo.value = 0
        }
    }

    /**
     * ✅ FASE 8C: Carrega o status atual da rota baseado no ciclo
     */
    private fun carregarStatusRota(rota: Rota) {
        // O status da rota será determinado pelo status do ciclo
        when (_statusCiclo.value) {
            StatusCicloAcerto.EM_ANDAMENTO -> _statusRota.value = StatusRota.EM_ANDAMENTO
            StatusCicloAcerto.FINALIZADO -> _statusRota.value = StatusRota.FINALIZADA
            StatusCicloAcerto.CANCELADO -> _statusRota.value = StatusRota.PAUSADA
        }
    }

    // ✅ FASE 9B: Estado de busca atual
    private val _buscaAtual = MutableStateFlow("")
    val buscaAtual: StateFlow<String> = _buscaAtual.asStateFlow()

    /**
     * ✅ FASE 9B: Busca clientes por nome em tempo real com filtros combinados
     */
    fun buscarClientes(query: String) {
        viewModelScope.launch {
            try {
                _buscaAtual.value = query
                aplicarFiltrosCombinados()
            } catch (e: Exception) {
                android.util.Log.e("ClientListViewModel", "Erro na busca: ${e.message}")
                _errorMessage.value = "Erro na busca: ${e.message}"
            }
        }
    }

    /**
     * ✅ FASE 9B: Aplica filtro à lista de clientes com filtros combinados
     */
    private fun aplicarFiltrosCombinados() {
        val query = _buscaAtual.value.trim()
        val filtro = _filtroAtual.value ?: FiltroCliente.TODOS
        val todos = _clientesTodos.value
        
        // Primeiro filtrar por status
        val filtradosPorStatus = when (filtro) {
            FiltroCliente.TODOS -> todos
            FiltroCliente.ATIVOS -> todos.filter { it.ativo }
            FiltroCliente.DEVEDORES -> todos.filter { it.debitoAnterior > 100.0 }
        }
        
        // Depois filtrar por busca
        val resultadoFinal = if (query.isBlank()) {
            filtradosPorStatus
        } else {
            filtradosPorStatus.filter { it.nome.contains(query, ignoreCase = true) }
        }
        
        // Atualizar lista filtrada
        _clientes.value = resultadoFinal
    }

    /**
     * ✅ FASE 9B: Limpa a busca e restaura lista original
     */
    fun limparBusca() {
        viewModelScope.launch {
            try {
                _buscaAtual.value = ""
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
        return _filtroAtual.value ?: FiltroCliente.TODOS
    }

    /**
     * Limpa mensagens de erro
     */
    fun limparErro() {
        _errorMessage.value = null
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
            val agora = java.util.Calendar.getInstance().time
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

    // ✅ FASE 8C: Atualizar card de progresso de forma centralizada
    private fun atualizarCardProgresso(rotaId: Long) {
        AppLogger.log("ClientListVM", "atualizarCardProgresso chamado após acerto salvo para rotaId: $rotaId")
        viewModelScope.launch {
            try {
                AppLogger.log("ClientListVM", "Iniciando atualização do card de progresso para rotaId: $rotaId")
                val cicloAtual = cicloAcertoRepository.getNumeroCicloAtual(rotaId)
                _cicloAcerto.value = cicloAtual
                AppLogger.log("ClientListVM", "Ciclo atual obtido: $cicloAtual")

                // Buscar clientes da rota e contar
                val clientes = appRepository.obterClientesPorRota(rotaId).first()
                val totalClientes = clientes.size
                AppLogger.log("ClientListVM", "Total de clientes na rota: $totalClientes")

                if (totalClientes > 0) {
                    val clientesAcertados = acertoRepository.getNumeroClientesAcertados(rotaId, cicloAtual.toLong())
                    AppLogger.log("ClientListVM", "Clientes acertados no ciclo: $clientesAcertados")

                    val percentual = if (totalClientes > 0) (clientesAcertados * 100) / totalClientes else 0
                    AppLogger.log("ClientListVM", "Percentual calculado: $percentual%")

                    val receita = acertoRepository.getReceitaTotal(rotaId, cicloAtual.toLong())
                    AppLogger.log("ClientListVM", "Receita total do ciclo: R$$receita")

                    val despesasList = appRepository.obterDespesasPorRota(rotaId).first()
                    val despesas = despesasList.sumOf { it.valor }
                    AppLogger.log("ClientListVM", "Despesas totais da rota: R$$despesas")

                    val pendencias = calcularPendencias(clientes)
                    AppLogger.log("ClientListVM", "Pendências do ciclo: $pendencias")

                    val saldo = receita - despesas

                    val card = CicloProgressoCard(
                        receita = receita,
                        despesas = despesas,
                        saldo = saldo,
                        percentual = percentual,
                        clientesAcertados = clientesAcertados,
                        totalClientes = totalClientes,
                        pendencias = pendencias,
                        debitoTotal = calcularDebitoTotal()
                    )
                    _cicloProgressoCard.value = card
                    AppLogger.log("ClientListVM", "Card de progresso atualizado com sucesso: $card")
                } else {
                    AppLogger.log("ClientListVM", "Nenhum cliente na rota. Resetando card de progresso.")
                    // Resetar card se não houver clientes
                    _cicloProgressoCard.value = CicloProgressoCard(0.0,0.0,0.0,0,0,0,0,0.0)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atualizar o progresso do ciclo: ${e.message}"
                AppLogger.log("ClientListVM", "ERRO ao atualizar card de progresso: ${e.message}")
            }
        }
    }

    // NOVO: Atualiza o card de progresso do ciclo de forma síncrona e direta
    suspend fun atualizarCardProgressoSincrono(rotaId: Long) {
        AppLogger.log("ClientListVM", "[CARD] Iniciando cálculo síncrono do card para rotaId: $rotaId")
        try {
            // Buscar ciclo ativo ou último ciclo
            val ciclo = cicloAcertoRepository.buscarCicloAtivo(rotaId) ?: cicloAcertoRepository.buscarEstatisticasRota(rotaId)
            if (ciclo == null) {
                AppLogger.log("ClientListVM", "[CARD] Nenhum ciclo encontrado para rotaId: $rotaId. Card zerado.")
                _cicloProgressoCard.value = CicloProgressoCard(0.0,0.0,0.0,0,0,0,0,0.0)
                return
            }
            AppLogger.log("ClientListVM", "[CARD] Ciclo usado: id=${ciclo.id}, status=${ciclo.status}, dataInicio=${ciclo.dataInicio}")
            // Buscar clientes da rota
            val clientes = appRepository.obterClientesPorRota(rotaId).first()
            AppLogger.log("ClientListVM", "[CARD] Clientes encontrados: ${clientes.size}")
            // Buscar acertos do ciclo
            val acertos = acertoRepository.buscarPorRotaECicloId(rotaId, ciclo.id).first()
            AppLogger.log("ClientListVM", "[CARD] Acertos encontrados: ${acertos.size}, cicloId dos acertos: ${acertos.map { it.cicloId }}")
            // Buscar despesas do ciclo
            val despesas = appRepository.buscarDespesasPorCicloId(ciclo.id).first()
            AppLogger.log("ClientListVM", "[CARD] Despesas encontradas: ${despesas.size}")
            // Calcular valores
            val clientesAcertados = acertos.map { it.clienteId }.distinct().size
            val totalClientes = clientes.size
            val faturamentoReal = acertos.sumOf { it.valorRecebido }
            val despesasReais = despesas.sumOf { it.valor }
            val pendencias = calcularPendencias(clientes)
            val percentualAcertados = if (totalClientes > 0) (clientesAcertados * 100) / totalClientes else 0
            val saldo = faturamentoReal - despesasReais
            AppLogger.log("ClientListVM", "[CARD] Resultado: receita=$faturamentoReal, despesas=$despesasReais, saldo=$saldo, percentual=$percentualAcertados, clientesAcertados=$clientesAcertados, totalClientes=$totalClientes, pendencias=$pendencias")
            _cicloProgressoCard.value = CicloProgressoCard(
                receita = faturamentoReal,
                despesas = despesasReais,
                saldo = saldo,
                percentual = percentualAcertados,
                clientesAcertados = clientesAcertados,
                totalClientes = totalClientes,
                pendencias = pendencias,
                debitoTotal = calcularDebitoTotal()
            )
        } catch (e: Exception) {
            AppLogger.log("ClientListVM", "[CARD] ERRO ao calcular card: ${e.message}")
            _cicloProgressoCard.value = CicloProgressoCard(0.0,0.0,0.0,0,0,0,0,0.0)
        }
    }

    // NOVO: Calcula o débito total de todos os clientes da rota
    private suspend fun calcularDebitoTotal(): Double {
        return try {
            val debitoTotal = clienteRepository.obterDebitoTotalPorRota(rotaId)
            android.util.Log.d("ClientListViewModel", "✅ Débito total calculado (banco): R$$debitoTotal")
            debitoTotal
        } catch (e: Exception) {
            android.util.Log.e("ClientListViewModel", "Erro ao calcular débito total: ${e.message}")
            0.0
        }
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
