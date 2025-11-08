package com.example.gestaobilhares.ui.colaboradores

import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.utils.PasswordHasher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/**
 * ViewModel para gerenciamento de colaboradores.
 * Implementa CRUD de colaboradores com controle de acesso administrativo.
 */
class ColaboradorManagementViewModel(
    private val appRepository: AppRepository,
    private val userSessionManager: com.example.gestaobilhares.utils.UserSessionManager? = null
) : BaseViewModel() {

    // ==================== DADOS OBSERVÁVEIS ====================
    
    // Lista de colaboradores filtrada
    private val _colaboradores = MutableStateFlow<List<Colaborador>>(emptyList())
    val colaboradores: StateFlow<List<Colaborador>> = _colaboradores.asStateFlow()
    
    // Estatísticas
    private val _totalColaboradores = MutableStateFlow(0)
    val totalColaboradores: StateFlow<Int> = _totalColaboradores.asStateFlow()
    
    private val _colaboradoresAtivos = MutableStateFlow(0)
    val colaboradoresAtivos: StateFlow<Int> = _colaboradoresAtivos.asStateFlow()
    
    private val _pendentesAprovacao = MutableStateFlow(0)
    val pendentesAprovacao: StateFlow<Int> = _pendentesAprovacao.asStateFlow()
    
    // isLoading já existe na BaseViewModel
    
    // message já existe na BaseViewModel
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Controle de acesso admin
    private val _hasAdminAccess = MutableStateFlow<Boolean>(false)
    val hasAdminAccess: StateFlow<Boolean> = _hasAdminAccess.asStateFlow()
    
    // Filtro atual
    private val _filtroAtual = MutableStateFlow(FiltroColaborador.TODOS)
    val filtroAtual: StateFlow<FiltroColaborador> = _filtroAtual.asStateFlow()
    
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
                showLoading()
                
                // Carregar estatísticas
                carregarEstatisticas()
                
                // Carregar colaboradores com filtro atual
                aplicarFiltro(_filtroAtual.value)
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar dados: ${e.message}"
            } finally {
                hideLoading()
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
                showLoading()
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
                hideLoading()
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
                showLoading()
                
                appRepository.aprovarColaborador(
                    colaboradorId = colaboradorId,
                    dataAprovacao = java.util.Date(),
                    aprovadoPor = aprovadoPor
                )
                
                showMessage("Colaborador aprovado com sucesso!")
                carregarDados() // Recarregar dados
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao aprovar colaborador: ${e.message}"
            } finally {
                hideLoading()
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
                showLoading()
                
                // ✅ FASE 12.1: Hashear senha antes de armazenar (nunca texto plano)
                val senhaHash = PasswordHasher.hashPassword(senha)
                
                // Atualizar colaborador com credenciais e aprovação
                appRepository.aprovarColaboradorComCredenciais(
                    colaboradorId = colaboradorId,
                    email = email,
                    senha = senhaHash, // ✅ SEGURANÇA: Armazenar hash, não texto plano
                    nivelAcesso = nivelAcesso,
                    observacoes = observacoes,
                    dataAprovacao = java.util.Date(),
                    aprovadoPor = aprovadoPor
                )
                
                showMessage("Colaborador aprovado com credenciais geradas!")
                carregarDados() // Recarregar dados
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao aprovar colaborador: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }
    
    /**
     * Ativa/desativa um colaborador
     */
    fun alterarStatusColaborador(colaboradorId: Long, ativo: Boolean) {
        viewModelScope.launch {
            try {
                showLoading()
                
                appRepository.alterarStatusColaborador(colaboradorId, ativo)
                
                val status = if (ativo) "ativado" else "desativado"
                showMessage("Colaborador $status com sucesso!")
                carregarDados() // Recarregar dados
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao alterar status: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }
    
    /**
     * Deleta um colaborador
     */
    fun deletarColaborador(colaborador: Colaborador) {
        viewModelScope.launch {
            try {
                showLoading()
                
                appRepository.deletarColaborador(colaborador)
                
                showMessage("Colaborador excluído com sucesso!")
                carregarDados() // Recarregar dados
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao excluir colaborador: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }
    
    // ==================== CONTROLE DE ACESSO ====================
    
    /**
     * Verifica se o usuário atual tem acesso de administrador
     * ✅ FASE 12.7: Usar UserSessionManager para verificação real
     */
    private fun verificarAcessoAdmin() {
        viewModelScope.launch {
            try {
                _hasAdminAccess.value = userSessionManager?.isAdmin() ?: false
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
        showMessage("")
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
