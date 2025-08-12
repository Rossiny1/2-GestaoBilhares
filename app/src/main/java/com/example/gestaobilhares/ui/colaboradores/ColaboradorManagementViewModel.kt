package com.example.gestaobilhares.ui.colaboradores

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/**
 * ViewModel para gerenciamento de colaboradores.
 * Implementa CRUD de colaboradores com controle de acesso administrativo.
 */
class ColaboradorManagementViewModel(
    private val appRepository: AppRepository
) : ViewModel() {

    // ==================== DADOS OBSERVÁVEIS ====================
    
    // Lista de colaboradores filtrada
    private val _colaboradores = MutableLiveData<List<Colaborador>>()
    val colaboradores: LiveData<List<Colaborador>> = _colaboradores
    
    // Estatísticas
    private val _totalColaboradores = MutableLiveData(0)
    val totalColaboradores: LiveData<Int> = _totalColaboradores
    
    private val _colaboradoresAtivos = MutableLiveData(0)
    val colaboradoresAtivos: LiveData<Int> = _colaboradoresAtivos
    
    private val _pendentesAprovacao = MutableLiveData(0)
    val pendentesAprovacao: LiveData<Int> = _pendentesAprovacao
    
    // Estado de loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Mensagens
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // Controle de acesso admin
    private val _hasAdminAccess = MutableLiveData<Boolean>()
    val hasAdminAccess: LiveData<Boolean> = _hasAdminAccess
    
    // Filtro atual
    private val _filtroAtual = MutableLiveData(FiltroColaborador.TODOS)
    val filtroAtual: LiveData<FiltroColaborador> = _filtroAtual
    
    // ==================== INICIALIZAÇÃO ====================
    
    init {
        carregarDados()
        verificarAcessoAdmin()
    }
    
    // ==================== CARREGAMENTO DE DADOS ====================
    
    /**
     * Carrega todos os dados necessários
     */
    fun carregarDados() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Carregar estatísticas
                carregarEstatisticas()
                
                // Carregar colaboradores com filtro atual
                aplicarFiltro(_filtroAtual.value ?: FiltroColaborador.TODOS)
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar dados: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Carrega estatísticas dos colaboradores
     */
    private suspend fun carregarEstatisticas() {
        try {
            // Total de colaboradores
            val todosColaboradores = appRepository.obterTodosColaboradores().first()
            _totalColaboradores.value = todosColaboradores.size
            
            // Colaboradores ativos
            val colaboradoresAtivos = appRepository.obterColaboradoresAtivos().first()
            _colaboradoresAtivos.value = colaboradoresAtivos.size
            
            // Pendentes aprovação
            val pendentes = appRepository.obterColaboradoresPendentesAprovacao().first()
            _pendentesAprovacao.value = pendentes.size
            
        } catch (e: Exception) {
            android.util.Log.e("ColaboradorManagementViewModel", "Erro ao carregar estatísticas: ${e.message}")
        }
    }
    
    // ==================== FILTROS ====================
    
    /**
     * Aplica filtro na lista de colaboradores
     */
    fun aplicarFiltro(filtro: FiltroColaborador) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _filtroAtual.value = filtro
                
                val colaboradoresFiltrados = when (filtro) {
                    FiltroColaborador.TODOS -> {
                        appRepository.obterTodosColaboradores().first()
                    }
                    FiltroColaborador.ATIVOS -> {
                        appRepository.obterColaboradoresAtivos().first()
                    }
                    FiltroColaborador.PENDENTES -> {
                        appRepository.obterColaboradoresPendentesAprovacao().first()
                    }
                    FiltroColaborador.ADMINISTRADORES -> {
                        appRepository.obterColaboradoresPorNivelAcesso(NivelAcesso.ADMIN).first()
                    }
                }
                
                _colaboradores.value = colaboradoresFiltrados
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao aplicar filtro: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // ==================== OPERAÇÕES CRUD ====================
    
    /**
     * Aprova um colaborador pendente
     */
    fun aprovarColaborador(colaboradorId: Long, aprovadoPor: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                appRepository.aprovarColaborador(
                    colaboradorId = colaboradorId,
                    dataAprovacao = java.util.Date(),
                    aprovadoPor = aprovadoPor
                )
                
                _message.value = "Colaborador aprovado com sucesso!"
                carregarDados() // Recarregar dados
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao aprovar colaborador: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Aprova um colaborador com credenciais de acesso
     */
    fun aprovarColaboradorComCredenciais(
        colaboradorId: Long,
        email: String,
        senha: String,
        nivelAcesso: NivelAcesso,
        observacoes: String,
        aprovadoPor: String
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Atualizar colaborador com credenciais e aprovação
                appRepository.aprovarColaboradorComCredenciais(
                    colaboradorId = colaboradorId,
                    email = email,
                    senha = senha,
                    nivelAcesso = nivelAcesso,
                    observacoes = observacoes,
                    dataAprovacao = java.util.Date(),
                    aprovadoPor = aprovadoPor
                )
                
                _message.value = "Colaborador aprovado com credenciais geradas!"
                carregarDados() // Recarregar dados
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao aprovar colaborador: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Ativa/desativa um colaborador
     */
    fun alterarStatusColaborador(colaboradorId: Long, ativo: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                appRepository.alterarStatusColaborador(colaboradorId, ativo)
                
                val status = if (ativo) "ativado" else "desativado"
                _message.value = "Colaborador $status com sucesso!"
                carregarDados() // Recarregar dados
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao alterar status: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Deleta um colaborador
     */
    fun deletarColaborador(colaborador: Colaborador) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                appRepository.deletarColaborador(colaborador)
                
                _message.value = "Colaborador excluído com sucesso!"
                carregarDados() // Recarregar dados
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao excluir colaborador: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // ==================== CONTROLE DE ACESSO ====================
    
    /**
     * Verifica se o usuário atual tem acesso de administrador
     */
    private fun verificarAcessoAdmin() {
        viewModelScope.launch {
            try {
                // TODO: Implementar verificação real com Firebase UID
                // Por enquanto, assume que tem acesso admin para demonstração
                _hasAdminAccess.value = true
            } catch (e: Exception) {
                _hasAdminAccess.value = false
                _errorMessage.value = "Erro ao verificar permissões: ${e.message}"
            }
        }
    }
    
    // ==================== UTILITÁRIOS ====================
    
    /**
     * Limpa mensagens
     */
    fun limparMensagens() {
        _message.value = ""
        _errorMessage.value = ""
    }
    
    /**
     * Atualiza dados
     */
    fun atualizarDados() {
        carregarDados()
    }
}

/**
 * Enum para filtros de colaboradores
 */
enum class FiltroColaborador {
    TODOS,
    ATIVOS,
    PENDENTES,
    ADMINISTRADORES
}
