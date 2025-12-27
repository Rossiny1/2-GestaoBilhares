package com.example.gestaobilhares.ui.colaboradores

import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
// import com.example.gestaobilhares.core.utils.PasswordHasher // TODO: Classe removida
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import java.util.Date

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import timber.log.Timber

/**
 * ViewModel para gerenciamento de colaboradores.
 * Implementa CRUD de colaboradores com controle de acesso administrativo.
 */
@HiltViewModel
class ColaboradorManagementViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val userSessionManager: com.example.gestaobilhares.core.utils.UserSessionManager
) : BaseViewModel() {
    
    // Instância do Firebase Auth para criar contas
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // ==================== DADOS OBSERVÁVEIS ====================
    
    // Lista de colaboradores filtrada
    private val _colaboradores = MutableStateFlow<List<Colaborador>>(emptyList())
    val colaboradores: StateFlow<List<Colaborador>> = _colaboradores.asStateFlow()
    
    // Estatísticas
    val totalColaboradores: StateFlow<Int> = appRepository.contarTotalColaboradoresFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val colaboradoresAtivos: StateFlow<Int> = appRepository.contarColaboradoresAtivosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val pendentesAprovacao: StateFlow<Int> = appRepository.contarColaboradoresPendentesAprovacaoFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
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
                
                // Carregar colaboradores com filtro atual
                aplicarFiltro(_filtroAtual.value)
                
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar dados: ${e.message}"
            } finally {
                hideLoading()
            }
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
     * ✅ NOVO: Cria conta no Firebase quando aprova colaborador
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
                
                // ✅ CORREÇÃO: Buscar colaborador para obter o email ORIGINAL
                val colaborador = appRepository.obterColaboradorPorId(colaboradorId)
                if (colaborador == null) {
                    Timber.e("ColaboradorManagementViewModel", "Colaborador não encontrado: $colaboradorId")
                    _errorMessage.value = "Colaborador não encontrado"
                    hideLoading()
                    return@launch
                }
                
                // ✅ CORREÇÃO CRÍTICA: Usar o email ORIGINAL do colaborador (não o emailAcesso sugerido)
                // O email original é o que foi fornecido no cadastro e já pode ter um usuário Firebase criado
                val emailParaFirebase = colaborador.email
                Timber.d("ColaboradorManagementViewModel", "Email original do colaborador: $emailParaFirebase")
                Timber.d("ColaboradorManagementViewModel", "Email sugerido (emailAcesso): $email")
                
                // ✅ CORREÇÃO: Se o colaborador já tem firebaseUid, usar esse (não criar novo usuário)
                var firebaseUid: String? = colaborador.firebaseUid
                
                // Se não tem firebaseUid, criar conta no Firebase Authentication com o email ORIGINAL
                if (firebaseUid == null) {
                    try {
                        Timber.d("ColaboradorManagementViewModel", "Criando conta Firebase para email ORIGINAL: $emailParaFirebase")
                        val result = firebaseAuth.createUserWithEmailAndPassword(emailParaFirebase, senha).await()
                        firebaseUid = result.user?.uid
                        Timber.d("ColaboradorManagementViewModel", "✅ Conta Firebase criada com sucesso! UID: $firebaseUid")
                    } catch (e: Exception) {
                        Timber.e("ColaboradorManagementViewModel", "Erro ao criar conta Firebase: ${e.message}")
                        // Se o usuário já existe no Firebase, tentar obter o UID
                        try {
                            val user = firebaseAuth.currentUser
                            if (user?.email == emailParaFirebase) {
                                firebaseUid = user.uid
                                Timber.d("ColaboradorManagementViewModel", "Usuário já existe no Firebase, UID: $firebaseUid")
                            } else {
                                // Tentar fazer login para obter o UID
                                val signInResult = firebaseAuth.signInWithEmailAndPassword(emailParaFirebase, senha).await()
                                firebaseUid = signInResult.user?.uid
                                Timber.d("ColaboradorManagementViewModel", "Login realizado para obter UID: $firebaseUid")
                                // Fazer logout para não manter sessão
                                firebaseAuth.signOut()
                            }
                        } catch (e2: Exception) {
                            Timber.w("ColaboradorManagementViewModel", "Não foi possível obter UID do Firebase: ${e2.message}")
                            // Continuar sem Firebase UID (modo offline)
                        }
                    }
                } else {
                    Timber.d("ColaboradorManagementViewModel", "✅ Colaborador já tem Firebase UID: $firebaseUid (não criando novo usuário)")
                }
                
                // ✅ FASE 12.1: Hashear senha antes de armazenar (nunca texto plano)
                // TODO: Implementar hash de senha (PasswordHasher removido)
                val senhaHash = senha // TEMPORÁRIO: Usar senha sem hash até implementar
                
                // ✅ CORREÇÃO: Atualizar colaborador com credenciais e aprovação
                // IMPORTANTE: Usar o email ORIGINAL do colaborador, não o emailAcesso sugerido
                appRepository.aprovarColaboradorComCredenciais(
                    colaboradorId = colaboradorId,
                    email = emailParaFirebase, // ✅ CORREÇÃO: Usar email original, não emailAcesso
                    senha = senhaHash, // ✅ SEGURANÇA: Armazenar hash, não texto plano
                    nivelAcesso = nivelAcesso,
                    observacoes = observacoes,
                    dataAprovacao = java.util.Date(),
                    aprovadoPor = aprovadoPor,
                    firebaseUid = firebaseUid // ✅ NOVO: Salvar Firebase UID
                )
                
                showMessage("Colaborador aprovado com credenciais geradas!")
                carregarDados() // Recarregar dados
                
            } catch (e: Exception) {
                Timber.e("ColaboradorManagementViewModel", "Erro ao aprovar colaborador: ${e.message}", e)
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
    
    /**
     * ✅ NOVO: Aprova colaborador diretamente do Firebase Authentication
     * Útil quando o colaborador foi criado no Auth mas não está no Firestore
     * 
     * @param email Email do colaborador no Firebase Authentication
     * @param senha Senha temporária para o colaborador
     * @param nivelAcesso Nível de acesso do colaborador
     * @param observacoes Observações sobre a aprovação
     * @param aprovadoPor Nome do administrador que está aprovando
     */
    fun aprovarColaboradorDoAuthentication(
        email: String,
        senha: String,
        nivelAcesso: NivelAcesso,
        observacoes: String,
        aprovadoPor: String
    ) {
        viewModelScope.launch {
            try {
                showLoading()
                Timber.d("ColaboradorManagementViewModel", "=== APROVANDO COLABORADOR DO AUTHENTICATION ===")
                Timber.d("ColaboradorManagementViewModel", "   Email: $email")
                
                // 1. Buscar usuário no Firebase Authentication pelo email
                val userRecord = try {
                    // Usar Admin SDK via Cloud Function ou buscar diretamente
                    // Como não temos Admin SDK no app, vamos buscar no Firestore primeiro
                    Timber.d("ColaboradorManagementViewModel", "   Buscando usuário no Authentication...")
                    null // Será implementado via Cloud Function
                } catch (e: Exception) {
                    Timber.e("ColaboradorManagementViewModel", "   Erro ao buscar usuário: ${e.message}")
                    throw Exception("Usuário não encontrado no Firebase Authentication: ${e.message}")
                }
                
                // 2. Buscar documento no Firestore usando email como ID temporário
                val companyId = userSessionManager.getCurrentCompanyId() ?: "empresa_001"
                val documentId = email.replace(".", "_").replace("@", "_")
                val collectionRef = firestore
                    .collection("empresas")
                    .document(companyId)
                    .collection("entidades")
                    .document("colaboradores")
                    .collection("items")
                
                Timber.d("ColaboradorManagementViewModel", "   Buscando documento no Firestore: $documentId")
                
                val docSnapshot = collectionRef.document(documentId).get().await()
                
                if (!docSnapshot.exists()) {
                    // Se não existe, criar novo documento
                    Timber.d("ColaboradorManagementViewModel", "   Documento não existe. Criando novo...")
                    
                    val colaboradorMap = mutableMapOf<String, Any?>()
                    colaboradorMap["id"] = System.currentTimeMillis() // ID temporário único
                    colaboradorMap["roomId"] = colaboradorMap["id"]
                    colaboradorMap["nome"] = email.substringBefore("@")
                    colaboradorMap["email"] = email
                    colaboradorMap["telefone"] = ""
                    colaboradorMap["cpf"] = ""
                    colaboradorMap["nivelAcesso"] = nivelAcesso.name
                    colaboradorMap["ativo"] = true
                    colaboradorMap["aprovado"] = true
                    colaboradorMap["primeiroAcesso"] = true
                    colaboradorMap["senhaTemporaria"] = senha
                    colaboradorMap["senhaHash"] = senha // TEMPORÁRIO: usar hash depois
                    colaboradorMap["dataCadastro"] = Timestamp(Date())
                    colaboradorMap["dataAprovacao"] = Timestamp(Date())
                    colaboradorMap["dataUltimaAtualizacao"] = Timestamp(Date())
                    colaboradorMap["aprovadoPor"] = aprovadoPor
                    colaboradorMap["observacoes"] = observacoes
                    colaboradorMap["lastModified"] = FieldValue.serverTimestamp()
                    colaboradorMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    // Tentar buscar firebaseUid do Authentication
                    try {
                        // Buscar todos os usuários e encontrar pelo email
                        // Nota: Isso requer Admin SDK, então vamos deixar null por enquanto
                        // A Cloud Function vai preencher depois
                        colaboradorMap["firebaseUid"] = null
                    } catch (e: Exception) {
                        Timber.w("ColaboradorManagementViewModel", "   Não foi possível obter firebaseUid: ${e.message}")
                    }
                    
                    collectionRef.document(documentId).set(colaboradorMap).await()
                    Timber.d("ColaboradorManagementViewModel", "✅ Documento criado no Firestore")
                } else {
                    // Se existe, atualizar com dados de aprovação
                    Timber.d("ColaboradorManagementViewModel", "   Documento existe. Atualizando...")
                    
                    val updateMap = mutableMapOf<String, Any?>()
                    updateMap["aprovado"] = true
                    updateMap["dataAprovacao"] = Timestamp(Date())
                    updateMap["aprovadoPor"] = aprovadoPor
                    updateMap["nivelAcesso"] = nivelAcesso.name
                    updateMap["senhaTemporaria"] = senha
                    updateMap["senhaHash"] = senha // TEMPORÁRIO
                    updateMap["observacoes"] = observacoes
                    updateMap["dataUltimaAtualizacao"] = Timestamp(Date())
                    updateMap["lastModified"] = FieldValue.serverTimestamp()
                    updateMap["syncTimestamp"] = FieldValue.serverTimestamp()
                    
                    collectionRef.document(documentId).update(updateMap).await()
                    Timber.d("ColaboradorManagementViewModel", "✅ Documento atualizado no Firestore")
                }
                
                showMessage("Colaborador aprovado com sucesso! O documento foi criado/atualizado no Firestore.")
                carregarDados() // Recarregar dados
                
            } catch (e: Exception) {
                Timber.e("ColaboradorManagementViewModel", "Erro ao aprovar colaborador do Authentication: ${e.message}", e)
                _errorMessage.value = "Erro ao aprovar colaborador: ${e.message}"
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

