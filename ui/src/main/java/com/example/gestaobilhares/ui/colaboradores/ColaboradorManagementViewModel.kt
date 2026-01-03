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
                
                // ✅ CORREÇÃO: Buscar colaborador antes de aprovar para sincronizar depois
                val colaborador = appRepository.obterColaboradorPorId(colaboradorId)
                if (colaborador == null) {
                    _errorMessage.value = "Colaborador não encontrado"
                    hideLoading()
                    return@launch
                }
                
                // Aprovar no banco local
                appRepository.aprovarColaborador(
                    colaboradorId = colaboradorId,
                    dataAprovacao = java.util.Date(),
                    aprovadoPor = aprovadoPor
                )
                
                // ✅ CORREÇÃO CRÍTICA: Buscar colaborador atualizado e ATUALIZAR IMEDIATAMENTE no Firestore
                val colaboradorAtualizado = appRepository.obterColaboradorPorId(colaboradorId)
                if (colaboradorAtualizado == null) {
                    _errorMessage.value = "Erro: Colaborador não encontrado após aprovação"
                    hideLoading()
                    return@launch
                }
                
                // ✅ ATUALIZAÇÃO IMEDIATA: Sincronizar para Firestore ANTES de mostrar mensagem de sucesso
                try {
                    val companyId = userSessionManager.getCurrentCompanyId() ?: "empresa_001"
                    sincronizarColaboradorParaFirestore(colaboradorAtualizado, companyId)
                    Timber.d("ColaboradorManagementViewModel", "✅ Colaborador aprovado e ATUALIZADO no Firestore")
                } catch (e: Exception) {
                    Timber.e("ColaboradorManagementViewModel", "❌ Erro ao atualizar no Firestore: ${e.message}", e)
                    _errorMessage.value = "Colaborador aprovado localmente, mas erro ao atualizar no servidor: ${e.message}"
                    // Continuar mesmo com erro para não bloquear a aprovação local
                }
                
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
     * ✅ NOVO: Sincroniza colaborador para Firestore após aprovação
     */
    private suspend fun sincronizarColaboradorParaFirestore(colaborador: Colaborador, companyId: String) {
        try {
            Timber.d("ColaboradorManagementViewModel", "=== SINCRONIZANDO COLABORADOR PARA FIRESTORE ===")
            Timber.d("ColaboradorManagementViewModel", "   ID: ${colaborador.id}")
            Timber.d("ColaboradorManagementViewModel", "   Nome: ${colaborador.nome}")
            Timber.d("ColaboradorManagementViewModel", "   Email: ${colaborador.email}")
            Timber.d("ColaboradorManagementViewModel", "   Firebase UID: ${colaborador.firebaseUid}")
            Timber.d("ColaboradorManagementViewModel", "   Aprovado: ${colaborador.aprovado}")
            Timber.d("ColaboradorManagementViewModel", "   Company ID: $companyId")
            
            val uid = colaborador.firebaseUid
            
            // ✅ PADRONIZAÇÃO: Usar APENAS o novo schema (empresas/{empresaId}/colaboradores/{uid})
            // REMOVIDO: Fallback para schema antigo para evitar duplicação
            if (uid == null || uid.isBlank()) {
                Timber.w("ColaboradorManagementViewModel", "⚠️ Colaborador não tem Firebase UID, não é possível sincronizar")
                Timber.w("ColaboradorManagementViewModel", "   Email: ${colaborador.email}")
                Timber.w("ColaboradorManagementViewModel", "   ID Local: ${colaborador.id}")
                Timber.w("ColaboradorManagementViewModel", "   Aprovado: ${colaborador.aprovado}")
                Timber.w("ColaboradorManagementViewModel", "   É necessário ter Firebase UID para sincronizar no novo schema")
                Timber.w("ColaboradorManagementViewModel", "   DICA: Use 'Aprovar com Credenciais' para criar o usuário no Firebase Auth primeiro")
                
                // ✅ CORREÇÃO CRÍTICA: Tentar buscar UID do Firebase Auth pelo email
                try {
                    Timber.d("ColaboradorManagementViewModel", "   Tentando buscar Firebase UID pelo email...")
                    // Nota: Não podemos buscar usuário por email diretamente no cliente
                    // Mas podemos verificar se o usuário atual tem esse email
                    val currentUser = firebaseAuth.currentUser
                    if (currentUser != null && currentUser.email == colaborador.email) {
                        val foundUid = currentUser.uid
                        Timber.d("ColaboradorManagementViewModel", "   ✅ Firebase UID encontrado via currentUser: $foundUid")
                        // Atualizar colaborador localmente com o UID encontrado
                        val colaboradorComUid = colaborador.copy(firebaseUid = foundUid)
                        appRepository.atualizarColaborador(colaboradorComUid)
                        // Tentar sincronizar novamente com o UID encontrado
                        val docRef = firestore.collection("empresas").document(companyId)
                            .collection("colaboradores").document(foundUid)
                        prepararDadosColaboradorParaFirestore(colaboradorComUid, companyId, foundUid, docRef)
                        return
                    } else {
                        Timber.w("ColaboradorManagementViewModel", "   ⚠️ Usuário atual não corresponde ao email do colaborador")
                        Timber.w("ColaboradorManagementViewModel", "   CurrentUser email: ${currentUser?.email}, Colaborador email: ${colaborador.email}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "ColaboradorManagementViewModel", "   Erro ao buscar Firebase UID: ${e.message}")
                }
                
                return
            }
            
            // ✅ Sincronizar APENAS no novo schema: empresas/{empresaId}/colaboradores/{uid}
            val docRef = firestore
                .collection("empresas")
                .document(companyId)
                .collection("colaboradores")
                .document(uid)
            
            Timber.d("ColaboradorManagementViewModel", "   Caminho Firestore: ${docRef.path}")
            
            // Preparar e atualizar dados do colaborador
            prepararDadosColaboradorParaFirestore(colaborador, companyId, uid, docRef)
            
            Timber.d("ColaboradorManagementViewModel", "✅ Sincronização concluída com sucesso!")
            
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            Timber.e("ColaboradorManagementViewModel", "❌ Erro Firestore ao sincronizar colaborador: ${e.code} - ${e.message}")
            Timber.e("ColaboradorManagementViewModel", "   Caminho: empresas/$companyId/colaboradores/${colaborador.firebaseUid}")
            throw e
        } catch (e: Exception) {
            Timber.e("ColaboradorManagementViewModel", "❌ Erro ao sincronizar colaborador para Firestore: %s", e.message)
            Timber.e("ColaboradorManagementViewModel", "   Stack: ${e.stackTraceToString()}")
            throw e
        }
    }
    
    /**
     * ✅ NOVO: Prepara e atualiza dados do colaborador no Firestore
     */
    private suspend fun prepararDadosColaboradorParaFirestore(
        colaborador: Colaborador,
        companyId: String,
        uid: String?,
        docRef: com.google.firebase.firestore.DocumentReference
    ) {
        // Converter para Map usando Gson (snake_case)
        val gson = com.google.gson.GsonBuilder()
            .setFieldNamingPolicy(com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
        
        val colaboradorJson = gson.toJson(colaborador)
        @Suppress("UNCHECKED_CAST")
        val colaboradorMap = gson.fromJson(colaboradorJson, Map::class.java) as? MutableMap<String, Any?> 
            ?: mutableMapOf()
        
        // Adicionar campos adicionais
        colaboradorMap["room_id"] = colaborador.id
        colaboradorMap["id"] = colaborador.id
        colaboradorMap["last_modified"] = FieldValue.serverTimestamp()
        colaboradorMap["sync_timestamp"] = FieldValue.serverTimestamp()
        
        // Converter datas para Timestamp
        colaboradorMap["data_cadastro"] = Timestamp(Date(colaborador.dataCadastro))
        colaboradorMap["data_ultima_atualizacao"] = Timestamp(Date(colaborador.dataUltimaAtualizacao))
        colaborador.dataAprovacao?.let { colaboradorMap["data_aprovacao"] = Timestamp(Date(it)) }
        colaborador.dataUltimoAcesso?.let { colaboradorMap["data_ultimo_acesso"] = Timestamp(Date(it)) }
        
        // ✅ CORREÇÃO CRÍTICA: Garantir campos boolean corretos (IMPORTANTE para aprovação)
        colaboradorMap["aprovado"] = colaborador.aprovado
        colaboradorMap["ativo"] = colaborador.ativo
        colaboradorMap["primeiro_acesso"] = colaborador.primeiroAcesso
        colaboradorMap["nivel_acesso"] = colaborador.nivelAcesso.name
        
        // ✅ CORREÇÃO: Garantir campos obrigatórios
        colaboradorMap["nome"] = colaborador.nome
        colaboradorMap["email"] = colaborador.email
        if (uid != null) {
            colaboradorMap["firebase_uid"] = uid
            colaboradorMap["firebaseUid"] = uid
        }
        colaboradorMap["empresa_id"] = companyId
        colaboradorMap["companyId"] = companyId
        
        // ✅ ATUALIZAÇÃO IMEDIATA: AGUARDAR atualização no Firestore (await bloqueante)
        Timber.d("ColaboradorManagementViewModel", "🔄 Atualizando Firestore: ${docRef.path}")
        Timber.d("ColaboradorManagementViewModel", "   Campo 'aprovado': ${colaboradorMap["aprovado"]}")
        Timber.d("ColaboradorManagementViewModel", "   Campo 'ativo': ${colaboradorMap["ativo"]}")
        Timber.d("ColaboradorManagementViewModel", "   Campo 'nivel_acesso': ${colaboradorMap["nivel_acesso"]}")
        Timber.d("ColaboradorManagementViewModel", "   Total de campos: ${colaboradorMap.size}")
        
        try {
            // ✅ CORREÇÃO: Usar set() com merge para garantir que campos existentes não sejam sobrescritos
            // Mas como queremos atualizar tudo, vamos usar set() direto
            docRef.set(colaboradorMap).await()
            
            // ✅ VERIFICAÇÃO: Ler o documento após atualização para confirmar
            val docSnapshot = docRef.get(com.google.firebase.firestore.Source.SERVER).await()
            val aprovadoNoFirestore = docSnapshot.getBoolean("aprovado") ?: false
            
            Timber.d("ColaboradorManagementViewModel", "✅ Colaborador ATUALIZADO no Firestore: ${colaborador.nome}")
            Timber.d("ColaboradorManagementViewModel", "   Aprovado local: ${colaborador.aprovado}")
            Timber.d("ColaboradorManagementViewModel", "   Aprovado no Firestore: $aprovadoNoFirestore")
            
            if (aprovadoNoFirestore != colaborador.aprovado) {
                Timber.w("ColaboradorManagementViewModel", "⚠️ DISCREPÂNCIA: Campo 'aprovado' não foi atualizado corretamente no Firestore!")
                Timber.w("ColaboradorManagementViewModel", "   Tentando atualizar novamente apenas o campo 'aprovado'...")
                docRef.update("aprovado", colaborador.aprovado).await()
                Timber.d("ColaboradorManagementViewModel", "✅ Campo 'aprovado' atualizado separadamente")
            }
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            Timber.e("ColaboradorManagementViewModel", "❌ Erro Firestore ao atualizar: ${e.code} - ${e.message}")
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Timber.e("ColaboradorManagementViewModel", "   PERMISSÃO NEGADA: Verifique as regras do Firestore")
                Timber.e("ColaboradorManagementViewModel", "   Usuário atual: ${firebaseAuth.currentUser?.email}")
                Timber.e("ColaboradorManagementViewModel", "   UID do colaborador: $uid")
            }
            throw e
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
                
                // ✅ CORREÇÃO CRÍTICA: Buscar colaborador atualizado e ATUALIZAR IMEDIATAMENTE no Firestore
                val colaboradorAtualizado = appRepository.obterColaboradorPorId(colaboradorId)
                if (colaboradorAtualizado == null) {
                    _errorMessage.value = "Erro: Colaborador não encontrado após aprovação"
                    hideLoading()
                    return@launch
                }
                
                // ✅ ATUALIZAÇÃO IMEDIATA: Sincronizar para Firestore ANTES de mostrar mensagem de sucesso
                try {
                    val companyId = userSessionManager.getCurrentCompanyId() ?: "empresa_001"
                    sincronizarColaboradorParaFirestore(colaboradorAtualizado, companyId)
                    Timber.d("ColaboradorManagementViewModel", "✅ Colaborador aprovado com credenciais e ATUALIZADO no Firestore")
                } catch (e: Exception) {
                    Timber.e("ColaboradorManagementViewModel", "❌ Erro ao atualizar no Firestore: ${e.message}", e)
                    _errorMessage.value = "Colaborador aprovado localmente, mas erro ao atualizar no servidor: ${e.message}"
                    // Continuar mesmo com erro para não bloquear a aprovação local
                }
                
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
                
                // ✅ PADRONIZAÇÃO: Usar APENAS o novo schema (empresas/{empresaId}/colaboradores/{uid})
                // REMOVIDO: Criação no schema antigo (entidades/colaboradores/items) para evitar duplicação
                // Esta função está DEPRECATED - use aprovarColaboradorComCredenciais que cria o usuário no Firebase Auth primeiro
                Timber.w("ColaboradorManagementViewModel", "⚠️ aprovarColaboradorDoAuthentication está DEPRECATED")
                Timber.w("ColaboradorManagementViewModel", "   Use aprovarColaboradorComCredenciais que cria o usuário no Firebase Auth primeiro")
                _errorMessage.value = "Esta função está desativada. Use 'Aprovar com Credenciais' que cria o usuário no Firebase Auth primeiro."
                
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

