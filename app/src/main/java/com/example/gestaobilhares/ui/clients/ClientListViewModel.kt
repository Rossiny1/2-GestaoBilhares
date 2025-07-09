package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.StatusRota
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.data.repository.RotaRepository
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
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
    private val cicloAcertoRepository: CicloAcertoRepository
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

    private val _clientesTodos = MutableStateFlow<List<Cliente>>(emptyList())
    private val _filtroAtual = MutableStateFlow(FiltroCliente.ATIVOS)
    
    val clientes: StateFlow<List<Cliente>> = combine(
        _clientesTodos,
        _filtroAtual
    ) { clientes, filtro ->
        when (filtro) {
            FiltroCliente.ATIVOS -> clientes.filter { it.ativo }
            FiltroCliente.DEVEDORES -> clientes.filter { it.debitoAnterior > 100.0 }
            FiltroCliente.TODOS -> clientes
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
     * Carrega clientes da rota
     */
    fun carregarClientes(rotaId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                clienteRepository.obterClientesPorRota(rotaId).collect { clientes ->
                    _clientesTodos.value = clientes
                }
            } catch (e: Exception) {
                android.util.Log.e("ClientListViewModel", "Erro ao carregar clientes: ${e.message}")
                _errorMessage.value = "Erro ao carregar clientes: ${e.message}"
                // Definir lista vazia para evitar crash
                _clientesTodos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Aplica filtro à lista de clientes
     */
    fun aplicarFiltro(filtro: FiltroCliente) {
        _filtroAtual.value = filtro
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
                
                val cicloId = cicloAcertoRepository.inserir(novoCiclo)
                
                // Atualizar estado
                _cicloAcerto.value = proximoCiclo
                _cicloAcertoEntity.value = novoCiclo.copy(id = cicloId)
                _statusCiclo.value = StatusCicloAcerto.EM_ANDAMENTO
                _statusRota.value = StatusRota.EM_ANDAMENTO
                
                android.util.Log.d("ClientListViewModel", "✅ Ciclo $proximoCiclo iniciado com sucesso")
                
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
                
                cicloAcertoRepository.atualizar(cicloFinalizado)
                
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
            var cicloAtual = cicloAcertoRepository.buscarCicloEmAndamento(rota.id)
            
            // Se não há ciclo em andamento, buscar o último ciclo
            if (cicloAtual == null) {
                cicloAtual = cicloAcertoRepository.buscarUltimoCicloPorRota(rota.id)
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

    /**
     * Limpa mensagens de erro
     */
    fun limparErro() {
        _errorMessage.value = null
    }
} 
