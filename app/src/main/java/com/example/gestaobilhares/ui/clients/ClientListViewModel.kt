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
    
    private val _faturamento = MutableStateFlow(0.0)
    val faturamento: StateFlow<Double> = _faturamento.asStateFlow()
    
    private val _pendencias = MutableStateFlow(0)
    val pendencias: StateFlow<Int> = _pendencias.asStateFlow()
    
    // ✅ NOVO: Dados de despesas do ciclo atual
    private val _despesas = MutableStateFlow(0.0)
    val despesas: StateFlow<Double> = _despesas.asStateFlow()

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
                _faturamento.value = 0.0
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
                
                // Atualizar ciclo para finalizado
                val cicloFinalizado = cicloAtual.copy(
                    dataFim = Date(),
                    status = StatusCicloAcerto.FINALIZADO,
                    dataAtualizacao = Date()
                )
                
                cicloAcertoRepository.inserirOuAtualizarCiclo(cicloFinalizado)
                
                // Atualizar estado
                _cicloAcertoEntity.value = cicloFinalizado
                _statusCiclo.value = StatusCicloAcerto.FINALIZADO
                _statusRota.value = StatusRota.FINALIZADA
                
                android.util.Log.d("ClientListViewModel", "✅ Ciclo ${cicloAtual.numeroCiclo} finalizado com sucesso")
                
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
     * ✅ FASE 9B: Aplicar filtros combinados (busca + status)
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
            val cicloAtual = _cicloAcerto.value
            val clientesAcertados = calcularClientesAcertadosNoCiclo(clientes, cicloAtual)
            _clientesAcertados.value = clientesAcertados
            
            // Calcular percentual
            val percentual = if (totalClientes > 0) {
                (clientesAcertados * 100) / totalClientes
            } else {
                0
            }
            _percentualAcertados.value = percentual
            _progressoCiclo.value = percentual
            
            // Calcular faturamento do ciclo atual
            val faturamento = calcularFaturamentoCicloAtual(cicloAtual)
            _faturamento.value = faturamento
            
            // Calcular despesas do ciclo atual
            val despesas = calcularDespesasCicloAtual(cicloAtual)
            _despesas.value = despesas
            
            // Calcular pendências
            val pendencias = calcularPendencias(clientes)
            _pendencias.value = pendencias
            
            android.util.Log.d("ClientListViewModel", "✅ Dados do progresso calculados: $percentual% de $totalClientes clientes, R$ $faturamento, R$ $despesas despesas, $pendencias pendências")
            
        } catch (e: Exception) {
            android.util.Log.e("ClientListViewModel", "Erro ao calcular dados do progresso: ${e.message}")
            // Valores padrão em caso de erro
            _percentualAcertados.value = 0
            _totalClientes.value = 0
            _clientesAcertados.value = 0
            _faturamento.value = 0.0
            _pendencias.value = 0
        }
    }

    /**
     * ✅ CORRIGIDO: Calcula quantos clientes foram acertados no ciclo atual usando dados reais
     */
    private suspend fun calcularClientesAcertadosNoCiclo(clientes: List<Cliente>, cicloAtual: Int): Int {
        return try {
            // Buscar acertos reais do banco de dados para esta rota e ciclo
            val acertos = acertoRepository.buscarPorRotaECiclo(rotaId, cicloAtual).first()
            
            // Contar clientes únicos que foram acertados
            val clientesAcertados = acertos.map { it.clienteId }.distinct()
            
            android.util.Log.d("ClientListViewModel", "✅ Clientes acertados no ciclo $cicloAtual: ${clientesAcertados.size} de ${clientes.size}")
            
            clientesAcertados.size
        } catch (e: Exception) {
            android.util.Log.e("ClientListViewModel", "Erro ao calcular clientes acertados: ${e.message}")
            0
        }
    }

    /**
     * ✅ CORRIGIDO: Calcula faturamento real do ciclo atual
     */
    private suspend fun calcularFaturamentoCicloAtual(cicloAtual: Int): Double {
        return try {
            // Buscar acertos reais do banco de dados para esta rota e ciclo
            val acertos = acertoRepository.buscarPorRotaECiclo(rotaId, cicloAtual).first()
            
            // Somar todos os valores recebidos dos acertos
            val faturamentoTotal = acertos.sumOf { it.valorRecebido }
            
            android.util.Log.d("ClientListViewModel", "✅ Faturamento real do ciclo $cicloAtual: R$ $faturamentoTotal")
            
            faturamentoTotal
        } catch (e: Exception) {
            android.util.Log.e("ClientListViewModel", "Erro ao calcular faturamento: ${e.message}")
            0.0
        }
    }

    /**
     * ✅ NOVO: Calcula despesas reais do ciclo atual
     */
    private suspend fun calcularDespesasCicloAtual(cicloAtual: Int): Double {
        return try {
            // Buscar despesas reais do banco de dados para esta rota e ciclo
            val despesas = appRepository.buscarDespesasPorRotaECiclo(rotaId, cicloAtual).first()
            
            // Somar todos os valores das despesas
            val totalDespesas = despesas.sumOf { despesa: Despesa -> despesa.valor }
            
            android.util.Log.d("ClientListViewModel", "✅ Despesas reais do ciclo $cicloAtual: R$ $totalDespesas")
            
            totalDespesas
        } catch (e: Exception) {
            android.util.Log.e("ClientListViewModel", "Erro ao calcular despesas: ${e.message}")
            0.0
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
                    cliente.debitoAnterior > 300.0 -> true
                    
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
} 
