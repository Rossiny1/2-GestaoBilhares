package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.StatusRota
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.data.repository.RotaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Filtros disponíveis para a lista de clientes
 */
enum class FiltroCliente {
    TODOS, ATIVOS, DEVEDORES
}

/**
 * ViewModel para ClientListFragment
 * FASE 3: Implementação com controle de status da rota
 */
class ClientListViewModel(
    private val clienteRepository: ClienteRepository,
    private val rotaRepository: RotaRepository
) : ViewModel() {

    private val _rotaInfo = MutableStateFlow<Rota?>(null)
    val rotaInfo: StateFlow<Rota?> = _rotaInfo.asStateFlow()

    private val _statusRota = MutableStateFlow(StatusRota.EM_ANDAMENTO)
    val statusRota: StateFlow<StatusRota> = _statusRota.asStateFlow()

    private val _cicloAcerto = MutableStateFlow(1)
    val cicloAcerto: StateFlow<Int> = _cicloAcerto.asStateFlow()

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
                        // Calcular ciclo de acerto baseado no ano atual
                        calcularCicloAcerto(it)
                        // Carregar status atual da rota (mock por enquanto)
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
     * Inicia a rota criando um novo ciclo de acerto
     */
    fun iniciarRota() {
        viewModelScope.launch {
            try {
                // TODO: Implementar lógica de banco de dados para ciclos de acerto
                _statusRota.value = StatusRota.EM_ANDAMENTO
                
                // Incrementar ciclo de acerto se necessário
                val novoCiclo = calcularProximoCiclo()
                _cicloAcerto.value = novoCiclo
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao iniciar rota: ${e.message}"
            }
        }
    }

    /**
     * Finaliza a rota atual
     */
    fun finalizarRota() {
        viewModelScope.launch {
            try {
                // TODO: Implementar lógica de finalização no banco de dados
                _statusRota.value = StatusRota.FINALIZADA
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao finalizar rota: ${e.message}"
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
     * Calcula o ciclo de acerto baseado no ano atual e histórico da rota
     */
    private fun calcularCicloAcerto(rota: Rota) {
        val anoAtual = Calendar.getInstance().get(Calendar.YEAR)
        
        // TODO: Implementar lógica de consulta ao banco para contar ciclos do ano
        // Por enquanto, começar sempre com o 1º acerto
        _cicloAcerto.value = 1
    }

    /**
     * Calcula o próximo ciclo de acerto
     */
    private fun calcularProximoCiclo(): Int {
        val cicloAtual = _cicloAcerto.value
        
        // TODO: Implementar lógica do banco de dados
        // Por enquanto, incrementar até máximo de 12 ciclos por ano
        return if (cicloAtual < 12) cicloAtual + 1 else 1
    }

    /**
     * Carrega o status atual da rota
     */
    private fun carregarStatusRota(rota: Rota) {
        // TODO: Implementar lógica de consulta ao banco de dados
        // Por enquanto, definir como "Não Iniciada" por padrão
        _statusRota.value = StatusRota.PAUSADA
    }

    /**
     * Limpa mensagens de erro
     */
    fun limparErro() {
        _errorMessage.value = null
    }
} 
