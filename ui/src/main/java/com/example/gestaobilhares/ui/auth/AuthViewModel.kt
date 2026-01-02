package com.example.gestaobilhares.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.entities.Colaborador
import com.example.gestaobilhares.data.entities.NivelAcesso
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.ui.common.BaseViewModel
import com.example.gestaobilhares.core.utils.NetworkUtils
import com.example.gestaobilhares.core.utils.UserSessionManager
// import com.example.gestaobilhares.core.utils.PasswordHasher // TODO: Classe removida
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.FieldNamingPolicy
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import timber.log.Timber

/**
 * ViewModel responsável pela lógica de autenticação híbrida (Firebase + Local).
 * Implementa padrão MVVM para separar lógica de negócio da UI.
 * Suporta autenticação online (Firebase) e offline (Room Database).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val networkUtils: NetworkUtils,
    private val userSessionManager: UserSessionManager
) : BaseViewModel() {
    
    // Instância do Firebase Auth
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    // Instância do Firestore
    private val firestore = FirebaseFirestore.getInstance()
    
    // Instância do Crashlytics para logs estruturados
    private val crashlytics = FirebaseCrashlytics.getInstance()
    
    // Gson para serialização/deserialização - padrão LOWER_CASE_WITH_UNDERSCORES para Firestore
    private val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    // ✅ MODERNIZADO: StateFlow para estado da autenticação
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // ✅ MODERNIZADO: StateFlow para mensagens de erro
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // isLoading já existe na BaseViewModel
    
    // ✅ MODERNIZADO: StateFlow para modo de conexão
    private val _isOnline = MutableStateFlow<Boolean>(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    init {
        // Inicializar sempre como não autenticado para mostrar tela de login
        _authState.value = AuthState.Unauthenticated
        _isOnline.value = true // Assumir online por padrão
        
        // Observar mudanças na conectividade
        viewModelScope.launch {
            try {
                networkUtils.isNetworkAvailable.collect { isAvailable ->
                    _isOnline.value = isAvailable
                    
                    // ✅ FASE 1: SyncManager antigo removido
                    // A sincronização é gerenciada pelo SyncManagerV2 em outros pontos do app
                    // Quando necessário, pode ser acionada manualmente via UI
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao observar conectividade: %s", e.message)
            }
        }
    }
    
    /**
     * Verifica se há conexão com internet
     */
    private fun isNetworkAvailable(): Boolean {
        return networkUtils.isConnected()
    }
    
    /**
     * Função para realizar login híbrido (online/offline)
     */
    fun login(email: String, senha: String) {
        // ✅ LOGS ESTRUTURADOS PARA CRASHLYTICS: Início do fluxo de login
        crashlytics.setCustomKey("login_email", email)
        crashlytics.setCustomKey("login_senha_length", senha.length)
        crashlytics.setCustomKey("login_timestamp", System.currentTimeMillis())
        crashlytics.log("[LOGIN_FLOW] Iniciando login híbrido para: $email")
        
        Timber.d("AuthViewModel", "=== INICIANDO LOGIN HÍBRIDO ===")
        Timber.d("AuthViewModel", "Email: $email")
        Timber.d("AuthViewModel", "Senha: ${senha.length} caracteres")
        
        // Validação básica
        if (email.isBlank() || senha.isBlank()) {
            crashlytics.setCustomKey("login_error", "email_ou_senha_em_branco")
            crashlytics.log("[LOGIN_FLOW] Erro: Email ou senha em branco")
            Timber.e("Email ou senha em branco")
            _errorMessage.value = "Email e senha são obrigatórios"
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            crashlytics.setCustomKey("login_error", "email_invalido")
            crashlytics.log("[LOGIN_FLOW] Erro: Email inválido: $email")
            Timber.e("Email inválido: %s", email)
            _errorMessage.value = "Email inválido"
            return
        }
        
        if (senha.length < 6) {
            crashlytics.setCustomKey("login_error", "senha_muito_curta")
            crashlytics.log("[LOGIN_FLOW] Erro: Senha muito curta: ${senha.length} caracteres")
            Timber.e("Senha muito curta: %d caracteres", senha.length)
            _errorMessage.value = "Senha deve ter pelo menos 6 caracteres"
            return
        }
        
        viewModelScope.launch {
            try {
                showLoading()
                _errorMessage.value = ""
                
                // Verificar conectividade
                val online = isNetworkAvailable()
                _isOnline.value = online
                crashlytics.setCustomKey("login_online", online)
                crashlytics.log("[LOGIN_FLOW] Status de conexão: ${if (online) "ONLINE" else "OFFLINE"}")
                
                if (online) {
                    // Tentar login online primeiro
                    crashlytics.log("[LOGIN_FLOW] Tentando login online...")
                    Timber.d("AuthViewModel", "Tentando login online...")
                    try {
                        val result = firebaseAuth.signInWithEmailAndPassword(email, senha).await()
                        
                        if (result.user != null) {
                            crashlytics.setCustomKey("login_online_success", true)
                            crashlytics.setCustomKey("login_firebase_uid", result.user!!.uid)
                            crashlytics.log("[LOGIN_FLOW] ✅ Login online bem-sucedido - Firebase UID: ${result.user!!.uid}")
                            Timber.d("AuthViewModel", "✅ LOGIN ONLINE SUCESSO!")

                            // ✅ NOVO: Emitir log específico para criação automática de dados após login
                            Timber.w(
                                "🔍 DB_POPULATION",
                                "🚨 LOGIN ONLINE CONCLUÍDO - DISPARANDO CARREGAMENTO INICIAL DE DADOS"
                            )
    
                            // ✅ NOVO: Criar/atualizar colaborador para usuário online
                            Timber.d("AuthViewModel", "🔍 Chamando criarOuAtualizarColaboradorOnline...")
                            var colaborador = criarOuAtualizarColaboradorOnline(result.user!!, senha)
                            Timber.d("AuthViewModel", "   Resultado: ${if (colaborador != null) "SUCESSO - ${colaborador.nome}" else "NULL - não encontrado"}")
                            
                            // ✅ SUPERADMIN: Se for rossinys@gmail.com e não encontrou, criar automaticamente
                            if (colaborador == null && email == "rossinys@gmail.com") {
                                Timber.d("AuthViewModel", "🔧 Criando SUPERADMIN automaticamente para: $email")
                                colaborador = criarSuperAdminAutomatico(email, result.user!!.uid, senha)
                            }
                            
                            if (colaborador == null) {
                                Timber.w("AuthViewModel", "⚠️ Colaborador não encontrado após criarOuAtualizarColaboradorOnline")
                                Timber.w("AuthViewModel", "   Tentando busca direta na nuvem como fallback...")
                                try {
                                    val fallbackResult = buscarColaboradorNaNuvemPorEmail(email)
                                    if (fallbackResult != null) {
                                        val (colaboradorFallback, fallbackCompanyId) = fallbackResult
                                        Timber.d("AuthViewModel", "✅ Colaborador encontrado no fallback: ${colaboradorFallback.nome}")
                                        // Atualizar firebaseUid e salvar localmente
                                        val colaboradorComUid = colaboradorFallback.copy(
                                            firebaseUid = result.user!!.uid,
                                            dataUltimoAcesso = System.currentTimeMillis()
                                        )
                                        try {
                                            val colaboradorExistente = appRepository.obterColaboradorPorEmail(email)
                                            if (colaboradorExistente != null) {
                                                appRepository.atualizarColaborador(colaboradorComUid.copy(id = colaboradorExistente.id))
                                                colaborador = colaboradorComUid.copy(id = colaboradorExistente.id)
                                            } else {
                                                appRepository.inserirColaborador(colaboradorComUid)
                                                colaborador = colaboradorComUid
                                            }
                                            userSessionManager.startSession(colaborador!!, fallbackCompanyId)
                                            Timber.d("AuthViewModel", "✅ Colaborador salvo e sessão iniciada no fallback")
                                        } catch (e: Exception) {
                                            Timber.e(e, "❌ Erro ao salvar colaborador no fallback: %s", e.message)
                                            // Mesmo com erro, tentar usar o colaborador da nuvem
                                            userSessionManager.startSession(colaboradorComUid, fallbackCompanyId)
                                            colaborador = colaboradorComUid
                                        }
                                    } else {
                                        Timber.e("AuthViewModel", "❌ Colaborador também não encontrado no fallback")
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "❌ Erro no fallback: %s", e.message)
                                }
                            }
                            
                            if (colaborador == null) {
                                Timber.e("AuthViewModel", "❌ ERRO FINAL: Colaborador não encontrado após todas as tentativas")
                                Timber.e("AuthViewModel", "   Email: $email")
                                Timber.e("AuthViewModel", "   Firebase UID: ${result.user!!.uid}")
                                _errorMessage.value = "Usuário não encontrado. Contate o administrador."
                                return@launch
                            }
                            
                            // ✅ CORREÇÃO CRÍTICA: Verificar se o colaborador está aprovado e ativo ANTES de permitir login
                            if (!colaborador.aprovado) {
                                Timber.w("AuthViewModel", "❌ Colaborador não está aprovado - bloqueando login")
                                Timber.w("AuthViewModel", "   Email: $email")
                                Timber.w("AuthViewModel", "   Nome: ${colaborador.nome}")
                                Timber.w("AuthViewModel", "   Aprovado: ${colaborador.aprovado}")
                                firebaseAuth.signOut() // Fazer logout do Firebase
                                _errorMessage.value = "Sua conta está aguardando aprovação do administrador."
                                hideLoading()
                                return@launch
                            }
                            
                            if (!colaborador.ativo) {
                                Timber.w("AuthViewModel", "❌ Colaborador está inativo - bloqueando login")
                                Timber.w("AuthViewModel", "   Email: $email")
                                Timber.w("AuthViewModel", "   Nome: ${colaborador.nome}")
                                Timber.w("AuthViewModel", "   Ativo: ${colaborador.ativo}")
                                firebaseAuth.signOut() // Fazer logout do Firebase
                                _errorMessage.value = "Sua conta está inativa. Contate o administrador."
                                hideLoading()
                                return@launch
                            }
                            
                            // ✅ SUPERADMIN: rossinys@gmail.com nunca precisa alterar senha no primeiro acesso
                            val isSuperAdmin = email == "rossinys@gmail.com"
                            
                            // ✅ NOVO: Verificar se é primeiro acesso (exceto superadmin)
                            // Só é primeiro acesso se a flag for true E ainda não tiver senha definitiva salva
                            if (!isSuperAdmin && colaborador.primeiroAcesso && colaborador.senhaHash == null) {
                                Timber.d("AuthViewModel", "⚠️ PRIMEIRO ACESSO DETECTADO - Redirecionando para alteração de senha")
                                _authState.value = AuthState.FirstAccessRequired(colaborador)
                                return@launch
                            }
                            
                            // ✅ CORREÇÃO CRÍTICA: Garantir que a sessão foi iniciada antes de autenticar
                            // A função criarOuAtualizarColaboradorOnline já inicia a sessão, mas vamos verificar
                            val nomeSessao = userSessionManager.getCurrentUserName()
                            val idSessao = userSessionManager.getCurrentUserId()
                            Timber.d("AuthViewModel", "🔍 Verificação da sessão online:")
                            Timber.d("AuthViewModel", "   Nome na sessão: $nomeSessao")
                            Timber.d("AuthViewModel", "   ID na sessão: $idSessao")
                            
                            // ✅ CORREÇÃO: Se a sessão não foi iniciada, iniciar agora
                            if (idSessao == 0L) {
                                val cloudInfo = if (online) buscarColaboradorNaNuvemPorEmail(email) else null
                                userSessionManager.startSession(colaborador, cloudInfo?.second ?: "empresa_001")
                            }
                            
                            val localUser = LocalUser(
                                uid = colaborador.id.toString(),
                                email = colaborador.email,
                                displayName = colaborador.nome,
                                nivelAcesso = colaborador.nivelAcesso
                            )
                            
                            _authState.value = AuthState.Authenticated(localUser, true)
                            Timber.d("AuthViewModel", "✅ Estado de autenticação definido - sessão ativa")
                            return@launch
                        }
                    } catch (e: Exception) {
                        // ✅ LOGS ESTRUTURADOS PARA CRASHLYTICS: Erro no login online
                        val errorCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                        crashlytics.setCustomKey("login_online_error", errorCode ?: "unknown")
                        crashlytics.setCustomKey("login_online_error_type", e.javaClass.simpleName)
                        crashlytics.log("[LOGIN_FLOW] ⚠️ Login online falhou: $errorCode - ${e.message}")
                        crashlytics.recordException(e)
                        
                        Timber.w("AuthViewModel", "Login online falhou: ${e.message}")
                        Timber.w("AuthViewModel", "Tipo de erro: ${e.javaClass.simpleName}")
                        
                        // ✅ CORREÇÃO: Se o erro for "wrong password" ou "user not found", 
                        // continuar para tentar login offline (pode ser senha temporária)
                        Timber.d("AuthViewModel", "Código de erro Firebase: $errorCode")
                        
                        // Se for erro de credenciais inválidas, pode ser senha temporária
                        // Continuar para tentar login offline
                        if (errorCode == "ERROR_WRONG_PASSWORD" || errorCode == "ERROR_USER_NOT_FOUND" || errorCode == "ERROR_INVALID_EMAIL") {
                            crashlytics.log("[LOGIN_FLOW] Erro de credenciais - tentando login offline com senha temporária...")
                            Timber.d("AuthViewModel", "Erro de credenciais - tentando login offline com senha temporária...")
                        } else {
                            // Para outros erros (rede, etc), também tentar offline
                            crashlytics.log("[LOGIN_FLOW] Erro de conexão ou outro - tentando login offline...")
                            Timber.d("AuthViewModel", "Erro de conexão ou outro - tentando login offline...")
                        }
                    }
                }
                
                // Se online falhou ou está offline, tentar login local
                Timber.d("AuthViewModel", "Tentando login offline...")
                Timber.d("AuthViewModel", "Email para busca: $email")
                
                // ✅ CORREÇÃO: Buscar colaborador por email ou firebaseUid
                var colaborador = appRepository.obterColaboradorPorEmail(email)
                
                // ✅ CORREÇÃO: Não buscar por Firebase UID quando login online falhou
                // O Firebase UID pode ser de outro usuário (ex: superadmin logado anteriormente)
                // Só buscar por Firebase UID se o login online foi bem-sucedido
                // (isso já foi tratado no bloco de login online acima)
                
                // ✅ CORREÇÃO CRÍTICA: Se não encontrou localmente E estiver online, buscar na nuvem
                // Isso é especialmente importante quando o app foi limpo e o usuário existe na nuvem
                if (colaborador == null && online) {
                    crashlytics.log("[LOGIN_FLOW] 🔍 Colaborador não encontrado localmente. Buscando na nuvem...")
                    crashlytics.setCustomKey("login_busca_nuvem", true)
                    Timber.d("AuthViewModel", "🔍 Colaborador não encontrado localmente. Buscando na nuvem...")
                    try {
                        val result = buscarColaboradorNaNuvemPorEmail(email)
                        if (result != null) {
                            crashlytics.setCustomKey("login_colaborador_encontrado_nuvem", true)
                            crashlytics.log("[LOGIN_FLOW] ✅ Colaborador encontrado na nuvem: ${result.first.nome}")
                            colaborador = result.first
                            val detectedCompanyId = result.second
                            Timber.d("AuthViewModel", "✅ Colaborador encontrado na nuvem: ${colaborador.nome}")
                            Timber.d("AuthViewModel", "   Aprovado: ${colaborador.aprovado}")
                            Timber.d("AuthViewModel", "   Ativo: ${colaborador.ativo}")
                            Timber.d("AuthViewModel", "   Primeiro acesso: ${colaborador.primeiroAcesso}")
                            Timber.d("AuthViewModel", "   Senha temporária presente: ${colaborador.senhaTemporaria != null}")
                            
                            // ✅ CORREÇÃO CRÍTICA: Verificar se está aprovado ANTES de salvar
                            crashlytics.setCustomKey("login_colaborador_aprovado", colaborador.aprovado)
                            crashlytics.setCustomKey("login_colaborador_ativo", colaborador.ativo)
                            crashlytics.setCustomKey("login_colaborador_primeiro_acesso", colaborador.primeiroAcesso)
                            
                            if (!colaborador.aprovado) {
                                crashlytics.setCustomKey("login_error", "colaborador_nao_aprovado")
                                crashlytics.log("[LOGIN_FLOW] ❌ Colaborador encontrado na nuvem mas não está aprovado")
                                Timber.w("AuthViewModel", "❌ Colaborador encontrado na nuvem mas não está aprovado")
                                _errorMessage.value = "Sua conta está aguardando aprovação do administrador."
                                hideLoading()
                                return@launch
                            }
                            
                            if (!colaborador.ativo) {
                                crashlytics.setCustomKey("login_error", "colaborador_inativo")
                                crashlytics.log("[LOGIN_FLOW] ❌ Colaborador encontrado na nuvem mas está inativo")
                                Timber.w("AuthViewModel", "❌ Colaborador encontrado na nuvem mas está inativo")
                                _errorMessage.value = "Sua conta está inativa. Contate o administrador."
                                hideLoading()
                                return@launch
                            }
                            
                            // Salvar colaborador localmente para próximos logins offline
                            try {
                                appRepository.inserirColaborador(colaborador)
                                Timber.d("AuthViewModel", "✅ Colaborador salvo localmente")
                            } catch (e: Exception) {
                                Timber.w("AuthViewModel", "⚠️ Erro ao salvar colaborador localmente: ${e.message}")
                                // Continuar mesmo com erro - o colaborador foi encontrado na nuvem
                            }
                            
                            // ✅ CORREÇÃO CRÍTICA: Validar senha e verificar primeiro acesso IMEDIATAMENTE
                            // Usar mesma lógica de validação de senha
                            val senhaLimpa = senha.trim()
                            val senhaHashLimpa = colaborador.senhaHash?.trim()
                            val senhaTemporariaLimpa = colaborador.senhaTemporaria?.trim()
                            
                            Timber.d("AuthViewModel", "🔍 Validação de senha (DADOS DA NUVEM - LOGIN OFFLINE):")
                            Timber.d("AuthViewModel", "   Senha fornecida: '${senhaLimpa}' (${senhaLimpa.length} caracteres)")
                            Timber.d("AuthViewModel", "   Hash armazenado: ${if (senhaHashLimpa != null) "'$senhaHashLimpa' (${senhaHashLimpa.length} caracteres)" else "ausente"}")
                            Timber.d("AuthViewModel", "   Senha temporária: ${if (senhaTemporariaLimpa != null) "'$senhaTemporariaLimpa' (${senhaTemporariaLimpa.length} caracteres)" else "ausente"}")
                            
                            val senhaValida = when {
                                senhaHashLimpa != null && senhaLimpa == senhaHashLimpa -> {
                                    Timber.d("AuthViewModel", "✅ Senha pessoal válida")
                                    true
                                }
                                senhaTemporariaLimpa != null && senhaLimpa == senhaTemporariaLimpa -> {
                                    Timber.d("AuthViewModel", "✅ Senha temporária válida")
                                    true
                                }
                                else -> {
                                    Timber.d("AuthViewModel", "❌ Senha inválida")
                                    false
                                }
                            }
                            
                            crashlytics.setCustomKey("login_senha_valida", senhaValida)
                            
                            if (!senhaValida) {
                                crashlytics.setCustomKey("login_error", "senha_invalida_nuvem")
                                crashlytics.log("[LOGIN_FLOW] ❌ Senha inválida para colaborador da nuvem")
                                Timber.w("AuthViewModel", "❌ Senha inválida para colaborador da nuvem")
                                _errorMessage.value = "Senha incorreta"
                                hideLoading()
                                return@launch
                            }
                            
                            // ✅ CORREÇÃO CRÍTICA: Verificar se é primeiro acesso (exceto superadmin)
                            val isSuperAdmin = email == "rossinys@gmail.com"
                            val isPrimeiroAcesso = !isSuperAdmin && 
                                                  colaborador.primeiroAcesso && 
                                                  colaborador.senhaHash == null &&
                                                  senhaTemporariaLimpa != null && 
                                                  senhaLimpa == senhaTemporariaLimpa
                            
                            Timber.d("AuthViewModel", "🔍 Verificação de primeiro acesso (DADOS DA NUVEM):")
                            Timber.d("AuthViewModel", "   É superadmin: $isSuperAdmin")
                            Timber.d("AuthViewModel", "   Primeiro acesso flag: ${colaborador.primeiroAcesso}")
                            Timber.d("AuthViewModel", "   SenhaHash presente: ${colaborador.senhaHash != null}")
                            Timber.d("AuthViewModel", "   Senha temporária presente: ${senhaTemporariaLimpa != null}")
                            Timber.d("AuthViewModel", "   Senha corresponde à temporária: ${senhaLimpa == senhaTemporariaLimpa}")
                            Timber.d("AuthViewModel", "   É primeiro acesso: $isPrimeiroAcesso")
                            
                            crashlytics.setCustomKey("login_primeiro_acesso", isPrimeiroAcesso)
                            
                            if (isPrimeiroAcesso) {
                                crashlytics.log("[LOGIN_FLOW] ⚠️ PRIMEIRO ACESSO DETECTADO (DADOS DA NUVEM) - Redirecionando para alteração de senha")
                                Timber.d("AuthViewModel", "⚠️ PRIMEIRO ACESSO DETECTADO (DADOS DA NUVEM) - Redirecionando para alteração de senha")
                                // ✅ CORREÇÃO CRÍTICA: Iniciar sessão ANTES de redirecionar
                                userSessionManager.startSession(colaborador, detectedCompanyId)
                                crashlytics.log("[LOGIN_FLOW] ✅ Sessão iniciada para primeiro acesso: ${colaborador.nome}")
                                Timber.d("AuthViewModel", "✅ Sessão iniciada para primeiro acesso: ${colaborador.nome}")
                                
                                _authState.value = AuthState.FirstAccessRequired(colaborador)
                                hideLoading()
                                return@launch
                            }
                            
                            // ✅ Se não é primeiro acesso, continuar com o fluxo normal de login offline
                            // (o código abaixo já trata isso)
                        } else {
                            crashlytics.setCustomKey("login_colaborador_encontrado_nuvem", false)
                            crashlytics.log("[LOGIN_FLOW] ⚠️ Colaborador não encontrado na nuvem")
                            Timber.w("AuthViewModel", "⚠️ Colaborador não encontrado na nuvem")
                        }
                    } catch (e: Exception) {
                        crashlytics.setCustomKey("login_erro_busca_nuvem", true)
                        crashlytics.setCustomKey("login_erro_busca_nuvem_tipo", e.javaClass.simpleName)
                        crashlytics.log("[LOGIN_FLOW] ❌ Erro ao buscar colaborador na nuvem: ${e.message}")
                        crashlytics.recordException(e)
                        Timber.e("AuthViewModel", "❌ Erro ao buscar colaborador na nuvem: ${e.message}", e)
                        // Continuar para tentar outras formas de login
                    }
                } else if (colaborador != null && online) {
                    // ✅ NOVO: Se encontrou localmente E estiver online, verificar se há atualizações na nuvem
                    Timber.d("AuthViewModel", "🔍 Colaborador encontrado localmente. Verificando atualizações na nuvem...")
                    try {
                        val result = buscarColaboradorNaNuvemPorEmail(email)
                        if (result != null) {
                            val colaboradorNuvem = result.first
                            Timber.d("AuthViewModel", "✅ Colaborador encontrado na nuvem. Atualizando dados locais...")
                            // Atualizar colaborador local com dados da nuvem (preservando ID local)
                            val colaboradorAtualizado = colaboradorNuvem.copy(id = colaborador.id)
                            try {
                                appRepository.atualizarColaborador(colaboradorAtualizado)
                                colaborador = colaboradorAtualizado
                                Timber.d("AuthViewModel", "✅ Colaborador atualizado com dados da nuvem")
                            } catch (e: Exception) {
                                Timber.w("AuthViewModel", "⚠️ Erro ao atualizar colaborador local: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w("AuthViewModel", "⚠️ Erro ao buscar atualizações na nuvem: ${e.message}")
                        // Continuar com dados locais
                    }
                }
                
                Timber.d("AuthViewModel", "🔍 Colaborador encontrado: ${colaborador?.nome ?: "NÃO ENCONTRADO"}")
                if (colaborador != null) {
                    Timber.d("AuthViewModel", "   ID: ${colaborador.id}")
                    Timber.d("AuthViewModel", "   Email: ${colaborador.email}")
                    Timber.d("AuthViewModel", "   Nível: ${colaborador.nivelAcesso}")
                    Timber.d("AuthViewModel", "   Aprovado: ${colaborador.aprovado}")
                    Timber.d("AuthViewModel", "   Ativo: ${colaborador.ativo}")
                    Timber.d("AuthViewModel", "   Senha temporária: ${colaborador.senhaTemporaria}")
                    Timber.d("AuthViewModel", "   Firebase UID: ${colaborador.firebaseUid}")
                    
                    // ✅ CORREÇÃO: Verificar se o colaborador está aprovado e ativo
                    if (!colaborador.aprovado) {
                        Timber.w("AuthViewModel", "❌ Colaborador não está aprovado")
                        _errorMessage.value = "Sua conta está aguardando aprovação do administrador."
                        hideLoading()
                        return@launch
                    }
                    
                    if (!colaborador.ativo) {
                        Timber.w("AuthViewModel", "❌ Colaborador está inativo")
                        _errorMessage.value = "Sua conta está inativa. Contate o administrador."
                        hideLoading()
                        return@launch
                    }
                
                    // ✅ OFFLINE-FIRST: Sistema seguro de validação offline
                    // Validação offline: usar hash de senha armazenado (temporária ou pessoal)
                    // Validação online: sempre usar Firebase Auth (já validado acima)
                    
                    // ✅ CORREÇÃO: Comparar senhas removendo espaços e verificando case
                    val senhaLimpa = senha.trim()
                    val senhaHashLimpa = colaborador.senhaHash?.trim()
                    val senhaTemporariaLimpa = colaborador.senhaTemporaria?.trim()
                    
                    Timber.d("AuthViewModel", "🔍 Validação de senha OFFLINE (DETALHADA):")
                    Timber.d("AuthViewModel", "   Senha fornecida: '${senhaLimpa}' (${senhaLimpa.length} caracteres)")
                    Timber.d("AuthViewModel", "   Hash armazenado: ${if (senhaHashLimpa != null) "'$senhaHashLimpa' (${senhaHashLimpa.length} caracteres)" else "ausente"}")
                    Timber.d("AuthViewModel", "   Senha temporária: ${if (senhaTemporariaLimpa != null) "'$senhaTemporariaLimpa' (${senhaTemporariaLimpa.length} caracteres)" else "ausente"}")
                    Timber.d("AuthViewModel", "   Primeiro acesso: ${colaborador.primeiroAcesso}")
                    Timber.d("AuthViewModel", "   Aprovado: ${colaborador.aprovado}")
                    Timber.d("AuthViewModel", "   Firebase UID: ${if (colaborador.firebaseUid != null) "presente" else "ausente"}")
                    
                    val senhaValida = when {
                        // ✅ Verificar senha pessoal (hash) - para logins após primeiro acesso
                        senhaHashLimpa != null && 
                        senhaLimpa == senhaHashLimpa -> {
                            Timber.d("AuthViewModel", "✅ Senha pessoal válida")
                            true
                        }
                        // ✅ Verificar senha temporária - para primeiro acesso
                        senhaTemporariaLimpa != null && 
                        senhaLimpa == senhaTemporariaLimpa -> {
                            Timber.d("AuthViewModel", "✅ Senha temporária válida")
                            true
                        }
                        else -> {
                            Timber.d("AuthViewModel", "❌ Senha inválida")
                            false
                        }
                    }
                    
                    Timber.d("AuthViewModel", "   Resultado final: $senhaValida")
                    
                    // ✅ SEGURANÇA: Superadmin também deve validar senha corretamente
                    // Seguindo melhores práticas de segurança, não permitir login com qualquer senha
                    val isSuperAdmin = email == "rossinys@gmail.com"
                    
                    // ✅ CORREÇÃO DE SEGURANÇA: Superadmin deve ter senha válida como qualquer usuário
                    if (!senhaValida) {
                        Timber.w("AuthViewModel", "❌ Senha inválida para ${if (isSuperAdmin) "SUPERADMIN" else "usuário"}")
                        _errorMessage.value = "Senha incorreta"
                        return@launch
                    }
                    
                    if (senhaValida) {
                        // ✅ CORREÇÃO: Verificar se é primeiro acesso (usando senha temporária) - exceto superadmin
                        // Usar senha limpa para comparação. 
                        // SÓ é primeiro acesso se a flag for true E não houver senha definitiva (senhaHash)
                        val isPrimeiroAcesso = !isSuperAdmin && 
                                              colaborador.primeiroAcesso && 
                                              colaborador.senhaHash == null &&
                                              senhaTemporariaLimpa != null && 
                                              senhaLimpa == senhaTemporariaLimpa
                        
                        Timber.d("AuthViewModel", "🔍 Verificação de primeiro acesso:")
                        Timber.d("AuthViewModel", "   É superadmin: $isSuperAdmin")
                        Timber.d("AuthViewModel", "   Primeiro acesso flag: ${colaborador.primeiroAcesso}")
                        Timber.d("AuthViewModel", "   Senha temporária presente: ${senhaTemporariaLimpa != null}")
                        Timber.d("AuthViewModel", "   Senha corresponde à temporária: ${senhaLimpa == senhaTemporariaLimpa}")
                        Timber.d("AuthViewModel", "   É primeiro acesso: $isPrimeiroAcesso")
                        Timber.d("AuthViewModel", "   Status online: $online")
                        
                        // ✅ CORREÇÃO: Se estiver online e for primeiro acesso, redirecionar para alteração de senha
                        // Se estiver offline, bloquear e pedir conexão
                        if (isPrimeiroAcesso) {
                            if (online) {
                                Timber.d("AuthViewModel", "⚠️ PRIMEIRO ACESSO DETECTADO ONLINE - Redirecionando para alteração de senha")
                                
                                // ✅ CORREÇÃO CRÍTICA: Iniciar sessão ANTES de redirecionar
                                // Isso é necessário para que o ChangePasswordFragment possa acessar le colaborador
                                val cloudInfo = buscarColaboradorNaNuvemPorEmail(colaborador.email)
                                userSessionManager.startSession(colaborador, cloudInfo?.second ?: "empresa_001")
                                Timber.d("AuthViewModel", "✅ Sessão iniciada para primeiro acesso: ${colaborador.nome}")
                                
                                _authState.value = AuthState.FirstAccessRequired(colaborador)
                                return@launch
                            } else {
                                Timber.d("AuthViewModel", "⚠️ PRIMEIRO ACESSO DETECTADO OFFLINE - Requer conexão online")
                                _errorMessage.value = "Primeiro acesso requer conexão com internet. Conecte-se e tente novamente."
                                return@launch
                            }
                        }
                        
                        // ✅ SUPERADMIN: Garantir que sempre é ADMIN, aprovado, sem primeiro acesso
                        // ✅ SEGURANÇA: Atualizar senha apenas se for válida (já validada acima)
                        var colaboradorFinal = if (isSuperAdmin) {
                            colaborador.copy(
                                nivelAcesso = NivelAcesso.ADMIN,
                                aprovado = true,
                                primeiroAcesso = false,
                                senhaHash = senhaLimpa // ✅ Atualizar com senha válida para login offline
                            ).also {
                                appRepository.atualizarColaborador(it)
                                Timber.d("AuthViewModel", "✅ SUPERADMIN: Dados atualizados (senha válida confirmada)")
                            }
                        } else {
                            colaborador
                        }
                        
                        val tipoAutenticacao = if (colaboradorFinal.senhaHash != null) "senha pessoal" else "senha temporária"
                        var isOnlineLogin = false // ✅ CORREÇÃO CRÍTICA: Começar como offline
                        
                        // ✅ CORREÇÃO CRÍTICA: Só marcar como online se conseguir autenticar no Firebase
                        // Isso é necessário para que o Firestore permita acesso (regras de segurança)
                        // Seguindo o mesmo padrão do login Google que funciona
                        if (online) {
                            Timber.d("AuthViewModel", "🔍 Dispositivo online. Garantindo autenticação no Firebase...")
                            val firebaseOutcome = garantirAutenticacaoFirebase(colaboradorFinal, senhaLimpa)
                            colaboradorFinal = firebaseOutcome.colaboradorAtualizado
                            isOnlineLogin = firebaseOutcome.autenticado
                            
                            // ✅ NOVO: Forçar refresh de claims se logado online com espera ativa
                            if (isOnlineLogin) {
                                try {
                                    Timber.d("AuthViewModel", "🔄 Garantindo que o token tenha a claim 'companyId'...")
                                    val claimFound = waitAndVerifyCompanyIdClaim()
                                    if (claimFound) {
                                        Timber.d("AuthViewModel", "✅ Claim 'companyId' confirmada no token")
                                    } else {
                                        Timber.w("AuthViewModel", "⚠️ Claim 'companyId' não encontrada após espera. Sincronização inicial pode falhar.")
                                    }
                                } catch (e: Exception) {
                                    Timber.w("AuthViewModel", "⚠️ Falha ao atualizar token: ${e.message}")
                                }
                            }
                        }
                        
                        Timber.d("AuthViewModel", "✅ LOGIN ${if (isOnlineLogin) "ONLINE" else "OFFLINE"} SUCESSO! (Tipo: $tipoAutenticacao)")

                        Timber.w(
                            "🔍 DB_POPULATION",
                            "🚨 LOGIN ${if (isOnlineLogin) "ONLINE" else "OFFLINE"} CONCLUÍDO - REALIZANDO CONFIGURAÇÃO LOCAL (POTENCIAL POPULAÇÃO)"
                        )
                        
                        Timber.d("AuthViewModel", "🔍 Iniciando sessão para: ${colaboradorFinal.nome}")
                        Timber.d("AuthViewModel", "   ID: ${colaboradorFinal.id}")
                        Timber.d("AuthViewModel", "   Email: ${colaboradorFinal.email}")
                        Timber.d("AuthViewModel", "   Status online: $isOnlineLogin")
                        Timber.d("AuthViewModel", "   Firebase Auth autenticado: ${firebaseAuth.currentUser != null}")
                        
                        // ✅ NOVO: Iniciar sessão do usuário
                        // Iniciar sessão do usuário com companyId via busca na nuvem
                        val cloudInfo = if (online) buscarColaboradorNaNuvemPorEmail(colaboradorFinal.email) else null
                        userSessionManager.startSession(colaboradorFinal, cloudInfo?.second ?: userSessionManager.getCurrentCompanyId())
                        
                        // ✅ NOVO: Verificar se a sessão foi iniciada corretamente
                        val nomeSessao = userSessionManager.getCurrentUserName()
                        val idSessao = userSessionManager.getCurrentUserId()
                        Timber.d("AuthViewModel", "🔍 Verificação da sessão:")
                        Timber.d("AuthViewModel", "   Nome na sessão: $nomeSessao")
                        Timber.d("AuthViewModel", "   ID na sessão: $idSessao")
                        
                        // Criar usuário local simulado
                        val localUser = LocalUser(
                            uid = colaboradorFinal.id.toString(),
                            email = colaboradorFinal.email,
                            displayName = colaboradorFinal.nome,
                            nivelAcesso = colaboradorFinal.nivelAcesso
                        )
                        
                        // ✅ CORREÇÃO CRÍTICA: Marcar como online apenas se conseguir autenticar no Firebase
                        // Isso permite que a sincronização seja disparada no RoutesFragment
                        // ✅ VERIFICAÇÃO FINAL: Confirmar que Firebase Auth está autenticado se marcando como online
                        if (isOnlineLogin) {
                            val firebaseUser = firebaseAuth.currentUser
                            if (firebaseUser == null) {
                                Timber.e("AuthViewModel", "❌ ERRO CRÍTICO: Tentando marcar como online mas Firebase Auth não está autenticado!")
                                Timber.e("AuthViewModel", "❌ Forçando como OFFLINE para evitar erros de sincronização")
                                isOnlineLogin = false
                            } else {
                                Timber.d("AuthViewModel", "✅ Firebase Auth confirmado autenticado - UID: ${firebaseUser.uid}")
                            }
                        }
                        
                        _authState.value = AuthState.Authenticated(localUser, isOnlineLogin)
                        Timber.d("AuthViewModel", "✅ Estado de autenticação definido - online: $isOnlineLogin")
                        Timber.d("AuthViewModel", "   Firebase Auth autenticado: ${firebaseAuth.currentUser != null}")
                        Timber.d("AuthViewModel", "   Firebase UID: ${firebaseAuth.currentUser?.uid ?: "não autenticado"}")
                        return@launch
                    } else {
                        _errorMessage.value = "Senha incorreta"
                    }
                } else {
                    // ✅ NOVO: Se não encontrou localmente e está online, buscar na nuvem
                    Timber.d("AuthViewModel", "🔍 Colaborador não encontrado localmente")
                    Timber.d("AuthViewModel", "   Status online: $online")
                    Timber.d("AuthViewModel", "   Email: $email")
                    
                    if (online) {
                        Timber.d("AuthViewModel", "🔍 Colaborador não encontrado localmente. Buscando na nuvem...")
                        // Se não encontrou aprovado, tentar encontrar mesmo não aprovado para verificação
                        val result = buscarColaboradorNaNuvemPorEmail(email)
                        
                        if (result != null) {
                            val colaboradorNuvem = result.first
                            val detectedCompanyId = result.second

                            Timber.d("AuthViewModel", "✅ Colaborador encontrado na nuvem: ${colaboradorNuvem.nome}")
                            Timber.d("AuthViewModel", "   Aprovado: ${colaboradorNuvem.aprovado}")
                            
                            // Salvar colaborador localmente para próximos logins offline
                            appRepository.inserirColaborador(colaboradorNuvem)
                            
                            // Verificar se está aprovado
                            if (colaboradorNuvem.aprovado) {
                                // ✅ CORREÇÃO: Usar mesma lógica de validação de senha (com trim)
                                val senhaLimpa = senha.trim()
                                val senhaHashLimpa = colaboradorNuvem.senhaHash?.trim()
                                val senhaTemporariaLimpa = colaboradorNuvem.senhaTemporaria?.trim()
                                
                                Timber.d("AuthViewModel", "🔍 Validação de senha (DADOS DA NUVEM):")
                                Timber.d("AuthViewModel", "   Senha fornecida: '${senhaLimpa}' (${senhaLimpa.length} caracteres)")
                                Timber.d("AuthViewModel", "   Hash armazenado: ${if (senhaHashLimpa != null) "'$senhaHashLimpa' (${senhaHashLimpa.length} caracteres)" else "ausente"}")
                                Timber.d("AuthViewModel", "   Senha temporária: ${if (senhaTemporariaLimpa != null) "'$senhaTemporariaLimpa' (${senhaTemporariaLimpa.length} caracteres)" else "ausente"}")
                                
                                val senhaValida = when {
                                    // ✅ Verificar senha pessoal (hash) - para logins após primeiro acesso
                                    senhaHashLimpa != null && senhaLimpa == senhaHashLimpa -> {
                                        Timber.d("AuthViewModel", "✅ Senha pessoal válida")
                                        true
                                    }
                                    // ✅ Verificar senha temporária - para primeiro acesso
                                    senhaTemporariaLimpa != null && senhaLimpa == senhaTemporariaLimpa -> {
                                        Timber.d("AuthViewModel", "✅ Senha temporária válida")
                                        true
                                    }
                                    else -> {
                                        Timber.d("AuthViewModel", "❌ Senha inválida")
                                        false
                                    }
                                }
                                
                                // ✅ SEGURANÇA: Superadmin também deve validar senha corretamente
                                // Seguindo melhores práticas de segurança, não permitir login com qualquer senha
                                val isSuperAdmin = email == "rossinys@gmail.com"
                                
                                // ✅ CORREÇÃO DE SEGURANÇA: Superadmin deve ter senha válida como qualquer usuário
                                if (!senhaValida) {
                                    Timber.w("AuthViewModel", "❌ Senha inválida para ${if (isSuperAdmin) "SUPERADMIN" else "usuário"} (dados da nuvem)")
                                    _errorMessage.value = "Senha incorreta"
                                    return@launch
                                }
                                
                                if (senhaValida) {
                                    Timber.d("AuthViewModel", "✅ LOGIN COM DADOS DA NUVEM SUCESSO!")
                                    
                                    // ✅ CORREÇÃO: Verificar se é primeiro acesso (exceto superadmin)
                                    var colaboradorNuvemAtualizado = colaboradorNuvem
                                    val isPrimeiroAcesso = !isSuperAdmin && 
                                                          colaboradorNuvemAtualizado.primeiroAcesso && 
                                                          senhaTemporariaLimpa != null && 
                                                          senhaLimpa == senhaTemporariaLimpa
                                    
                                    if (isPrimeiroAcesso) {
                                        Timber.d("AuthViewModel", "⚠️ PRIMEIRO ACESSO DETECTADO - Redirecionando para alteração de senha")
                                        userSessionManager.startSession(colaboradorNuvemAtualizado, detectedCompanyId)
                                        _authState.value = AuthState.FirstAccessRequired(colaboradorNuvemAtualizado)
                                        return@launch
                                    }
                                    
                                    // ✅ CORREÇÃO CRÍTICA: Só marcar como online se conseguir autenticar no Firebase
                                    // Seguindo o mesmo padrão do login Google que funciona
                                    var isOnlineLogin = false
                                    
                                    if (isNetworkAvailable()) {
                                        val firebaseOutcome = garantirAutenticacaoFirebase(colaboradorNuvemAtualizado, senhaLimpa)
                                        colaboradorNuvemAtualizado = firebaseOutcome.colaboradorAtualizado
                                        isOnlineLogin = firebaseOutcome.autenticado
                                    }
                                    
                                    // Iniciar sessão
                                    userSessionManager.startSession(colaboradorNuvemAtualizado, detectedCompanyId)
                                    
                                    val localUser = LocalUser(
                                        uid = colaboradorNuvemAtualizado.id.toString(),
                                        email = colaboradorNuvemAtualizado.email,
                                        displayName = colaboradorNuvemAtualizado.nome,
                                        nivelAcesso = colaboradorNuvemAtualizado.nivelAcesso
                                    )
                                    
                                    // ✅ CORREÇÃO: Marcar como online apenas se conseguir autenticar no Firebase
                                    // ✅ VERIFICAÇÃO FINAL: Confirmar que Firebase Auth está autenticado se marcando como online
                                    if (isOnlineLogin) {
                                        val firebaseUser = firebaseAuth.currentUser
                                        if (firebaseUser == null) {
                                            Timber.e("AuthViewModel", "❌ ERRO CRÍTICO: Tentando marcar como online mas Firebase Auth não está autenticado!")
                                            Timber.e("AuthViewModel", "❌ Forçando como OFFLINE para evitar erros de sincronização")
                                            isOnlineLogin = false
                                        } else {
                                            Timber.d("AuthViewModel", "✅ Firebase Auth confirmado autenticado - UID: ${firebaseUser.uid}")
                                        }
                                    }
                                    
                                    _authState.value = AuthState.Authenticated(localUser, isOnlineLogin)
                                    Timber.d("AuthViewModel", "✅ Estado de autenticação definido - online: $isOnlineLogin (dados da nuvem)")
                                    Timber.d("AuthViewModel", "   Firebase Auth autenticado: ${firebaseAuth.currentUser != null}")
                                    Timber.d("AuthViewModel", "   Firebase UID: ${firebaseAuth.currentUser?.uid ?: "não autenticado"}")
                                    return@launch
                                } else {
                                    _errorMessage.value = "Senha incorreta"
                                    return@launch
                                }
                            } else {
                                _errorMessage.value = "Sua conta está aguardando aprovação do administrador"
                                return@launch
                            }
                        }
                    }
                    
                    // ✅ SUPERADMIN: Se não existe colaborador local, criar automaticamente para rossinys@gmail.com
                    if (email == "rossinys@gmail.com") {
                        Timber.d("AuthViewModel", "🔧 Criando SUPERADMIN automaticamente (offline) para: $email")
                        val colaboradorSuperAdmin = criarSuperAdminAutomatico(email, null, senha)
                        
                        if (colaboradorSuperAdmin != null) {
                            val localUser = LocalUser(
                                uid = colaboradorSuperAdmin.id.toString(),
                                email = colaboradorSuperAdmin.email,
                                displayName = colaboradorSuperAdmin.nome,
                                nivelAcesso = colaboradorSuperAdmin.nivelAcesso
                            )
                            _authState.value = AuthState.Authenticated(localUser, false)
                            return@launch
                        }
                    }
                    
                    crashlytics.setCustomKey("login_error", "usuario_nao_encontrado")
                    crashlytics.log("[LOGIN_FLOW] ❌ ERRO FINAL: Usuário não encontrado (online: $online)")
                    _errorMessage.value = if (online) {
                        "Usuário não encontrado. Contate o administrador para criar sua conta."
                    } else {
                        "Usuário não encontrado. Faça login online primeiro para sincronizar sua conta."
                    }
                }
                
                _authState.value = AuthState.Unauthenticated
                
            } catch (e: Exception) {
                crashlytics.setCustomKey("login_error", "excecao_geral")
                crashlytics.setCustomKey("login_error_tipo", e.javaClass.simpleName)
                crashlytics.log("[LOGIN_FLOW] ❌ ERRO NO LOGIN: ${e.message}")
                crashlytics.recordException(e)
                Timber.e(e, "❌ ERRO NO LOGIN: %s", e.message)
                _authState.value = AuthState.Unauthenticated
                _errorMessage.value = getFirebaseErrorMessage(e)
            } finally {
                crashlytics.log("[LOGIN_FLOW] === FIM DO LOGIN HÍBRIDO ===")
                hideLoading()
                Timber.d("AuthViewModel", "=== FIM DO LOGIN HÍBRIDO ===")
            }
        }
    }
    
    /**
     * ✅ NOVO: Gera senha aleatória para acesso offline
     * Gera uma senha de 8 caracteres com letras e números
     */
    private fun gerarSenhaOffline(): String {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = java.util.Random()
        return (1..8)
            .map { caracteres[random.nextInt(caracteres.length)] }
            .joinToString("")
    }
    
    /**
     * Função para logout
     */
    fun logout() {
        firebaseAuth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
    
    /**
     * Função para verificar usuário atual
     */
    fun checkCurrentUser() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Authenticated(currentUser, true)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    /**
     * Função para resetar senha (apenas online)
     */
    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _errorMessage.value = "Email é obrigatório"
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Email inválido"
            return
        }
        
        if (!isNetworkAvailable()) {
            _errorMessage.value = "Recuperação de senha requer conexão com internet"
            return
        }
        
        viewModelScope.launch {
            try {
                showLoading()
                firebaseAuth.sendPasswordResetEmail(email).await()
                _errorMessage.value = "Email de recuperação enviado!"
            } catch (e: Exception) {
                _errorMessage.value = getFirebaseErrorMessage(e)
            } finally {
                hideLoading()
            }
        }
    }
    
    // ✅ REMOVIDO: Método salvarDadosUsuario não estava funcionando
    // O UserSessionManager já salva os dados corretamente
    
    /**
     * Função para limpar mensagens de erro
     */
    fun clearErrorMessage() {
        _errorMessage.value = ""
    }
    
    /**
     * ✅ NOVO: Cria ou atualiza colaborador para usuário online
     * Retorna o colaborador atualizado ou null se não encontrado
     */
    private suspend fun criarOuAtualizarColaboradorOnline(firebaseUser: FirebaseUser, senha: String = ""): Colaborador? {
        try {
            val email = firebaseUser.email ?: return null
            
            // Verificar se já existe colaborador com este email
            val colaboradorExistente = appRepository.obterColaboradorPorEmail(email)
            
            if (colaboradorExistente != null) {
                Timber.d("AuthViewModel", "Colaborador existente encontrado: ${colaboradorExistente.nome}")

                Timber.w(
                    "🔍 DB_POPULATION",
                    "🚨 ATUALIZANDO COLABORADOR LOCAL APÓS LOGIN ONLINE: ${colaboradorExistente.email}"
                )

                // ✅ SUPERADMIN: rossinys@gmail.com sempre é ADMIN e aprovado
                val colaboradorAtualizado = if (email == "rossinys@gmail.com") {
                    // Superadmin - sempre ADMIN, aprovado, sem primeiro acesso
                    // ✅ CORREÇÃO CRÍTICA: Atualizar senhaHash com a senha atual para login offline funcionar
                    val senhaParaHash = if (senha.isNotEmpty()) senha.trim() else colaboradorExistente.senhaHash
                    Timber.d("AuthViewModel", "🔧 SUPERADMIN: Atualizando senhaHash para login offline")
                    Timber.d("AuthViewModel", "   Senha fornecida: ${if (senha.isNotEmpty()) "presente (${senha.length} caracteres)" else "ausente"}")
                    Timber.d("AuthViewModel", "   SenhaHash anterior: ${colaboradorExistente.senhaHash}")
                    Timber.d("AuthViewModel", "   SenhaHash novo: $senhaParaHash")
                    
                    colaboradorExistente.copy(
                        nome = firebaseUser.displayName ?: colaboradorExistente.nome,
                        firebaseUid = firebaseUser.uid,
                        dataUltimoAcesso = System.currentTimeMillis(),
                        nivelAcesso = NivelAcesso.ADMIN,
                        aprovado = true,
                        primeiroAcesso = false, // Superadmin nunca precisa alterar senha
                        dataAprovacao = colaboradorExistente.dataAprovacao ?: System.currentTimeMillis(),
                        aprovadoPor = colaboradorExistente.aprovadoPor ?: "Sistema (Superadmin)",
                        senhaHash = senhaParaHash // ✅ Atualizar senhaHash para login offline
                    )
                } else {
                    // ✅ CORREÇÃO: Para outros usuários, MANTER nível de acesso original
                    colaboradorExistente.copy(
                        nome = firebaseUser.displayName ?: colaboradorExistente.nome,
                        firebaseUid = firebaseUser.uid,
                        dataUltimoAcesso = System.currentTimeMillis()
                        // NÃO alterar nivelAcesso, aprovado, etc. para usuários normais
                    )
                }
                
                // Salvar atualizações no banco local
                appRepository.atualizarColaborador(colaboradorAtualizado)
                
                Timber.d("AuthViewModel", "✅ Colaborador sincronizado:")
                Timber.d("AuthViewModel", "   Nome: ${colaboradorAtualizado.nome}")
                Timber.d("AuthViewModel", "   Email: ${colaboradorAtualizado.email}")
                Timber.d("AuthViewModel", "   Nível: ${colaboradorAtualizado.nivelAcesso}")
                Timber.d("AuthViewModel", "   Aprovado: ${colaboradorAtualizado.aprovado}")
                Timber.d("AuthViewModel", "   É admin especial: ${email == "rossinys@gmail.com"}")

                userSessionManager.startSession(colaboradorAtualizado, userSessionManager.getCurrentCompanyId()) // Assuming companyId is already set or default
                return colaboradorAtualizado
            } else {
                Timber.d("AuthViewModel", "🔍 Colaborador não encontrado localmente. Buscando na nuvem...")
                Timber.d("AuthViewModel", "   Email para busca: $email")
                Timber.d("AuthViewModel", "   Firebase UID: ${firebaseUser.uid}")
                
                // ✅ CORREÇÃO CRÍTICA: Buscar colaborador na nuvem quando não encontrar localmente
                var colaboradorNuvemResult: Pair<Colaborador, String>? = null
                try {
                    colaboradorNuvemResult = buscarColaboradorNaNuvemPorEmail(email)
                    Timber.d("AuthViewModel", "   Resultado da busca na nuvem: ${if (colaboradorNuvemResult != null) "ENCONTRADO" else "NÃO ENCONTRADO"}")
                } catch (e: Exception) {
                    Timber.e(e, "❌ ERRO ao buscar colaborador na nuvem: %s", e.message)
                }
                
                if (colaboradorNuvemResult != null) {
                    val colaboradorNuvem = colaboradorNuvemResult.first
                    val detectedCompanyId = colaboradorNuvemResult.second

                    Timber.d("AuthViewModel", "✅ Colaborador encontrado na nuvem: ${colaboradorNuvem.nome}")
                    Timber.d("AuthViewModel", "   ID: ${colaboradorNuvem.id}")
                    Timber.d("AuthViewModel", "   Email: ${colaboradorNuvem.email}")
                    Timber.d("AuthViewModel", "   Aprovado: ${colaboradorNuvem.aprovado}")
                    
                    // ✅ Atualizar firebaseUid com o UID do Firebase Authentication
                    val colaboradorAtualizado = colaboradorNuvem.copy(
                        firebaseUid = firebaseUser.uid,
                        dataUltimoAcesso = System.currentTimeMillis()
                    )
                    
                    // ✅ SELF-HEALING: Se logou com sucesso e a senha é diferente da temporária, 
                    // o primeiro acesso já foi concluído e a nuvem está com dado antigo.
                    val senhaTemporariaLimpa = colaboradorAtualizado.senhaTemporaria?.trim()
                    val isSecretlyFinished = !email.equals("rossinys@gmail.com", ignoreCase = true) && 
                                            colaboradorAtualizado.primeiroAcesso && 
                                            senha.isNotEmpty() && 
                                            (senhaTemporariaLimpa == null || senha.trim() != senhaTemporariaLimpa)
                    
                    val colaboradorFinal = if (email == "rossinys@gmail.com") {
                        // (rossinys@gmail.com logic remains the same)
                        val senhaParaHash = if (senha.isNotEmpty()) senha.trim() else colaboradorAtualizado.senhaHash
                        colaboradorAtualizado.copy(
                            nivelAcesso = NivelAcesso.ADMIN,
                            aprovado = true,
                            primeiroAcesso = false,
                            dataAprovacao = colaboradorAtualizado.dataAprovacao ?: System.currentTimeMillis(),
                            aprovadoPor = colaboradorAtualizado.aprovadoPor ?: "Sistema (Superadmin)",
                            senhaHash = senhaParaHash
                        )
                    } else if (isSecretlyFinished) {
                        Timber.d("AuthViewModel", "🩹 SELF-HEALING: Detectado que o primeiro acesso já foi feito (senha != temporária). Corrigindo flag...")
                        colaboradorAtualizado.copy(
                            primeiroAcesso = false,
                            senhaHash = senha.trim(),
                            senhaTemporaria = null,
                            dataUltimaAtualizacao = System.currentTimeMillis()
                        ).also { 
                            // Sincronizar correção para a nuvem imediatamente
                            viewModelScope.launch {
                                try {
                                    sincronizarColaboradorParaNuvem(it, detectedCompanyId)
                                    Timber.d("AuthViewModel", "✅ SELF-HEALING: Nuvem corrigida com sucesso")
                                } catch (e: Exception) {
                                    Timber.e("AuthViewModel", "❌ SELF-HEALING: Erro ao sincronizar correção: ${e.message}")
                                }
                            }
                        }
                    } else {
                        colaboradorAtualizado
                    }
                    
                    // ✅ Salvar colaborador localmente
                    try {
                        // Verificar se já existe por ID (pode ter sido criado com ID diferente)
                        val colaboradorExistentePorId = appRepository.obterColaboradorPorId(colaboradorFinal.id)
                        if (colaboradorExistentePorId != null) {
                            Timber.d("AuthViewModel", "Colaborador já existe localmente (por ID), atualizando...")
                            appRepository.atualizarColaborador(colaboradorFinal)
                        } else {
                            // Verificar se existe por email (pode ter ID diferente)
                            val colaboradorExistentePorEmail = appRepository.obterColaboradorPorEmail(email)
                            if (colaboradorExistentePorEmail != null) {
                                Timber.d("AuthViewModel", "Colaborador já existe localmente (por email), atualizando com ID da nuvem...")
                                // Atualizar o existente com os dados da nuvem, mantendo o ID local
                                val colaboradorMesclado = colaboradorFinal.copy(id = colaboradorExistentePorEmail.id)
                                appRepository.atualizarColaborador(colaboradorMesclado)
                                userSessionManager.startSession(colaboradorMesclado, detectedCompanyId)
                                return colaboradorMesclado
                            } else {
                                Timber.d("AuthViewModel", "Colaborador não existe localmente, inserindo...")
                                appRepository.inserirColaborador(colaboradorFinal)
                            }
                        }
                        
                        Timber.d("AuthViewModel", "✅ Colaborador salvo localmente com sucesso")
                        userSessionManager.startSession(colaboradorFinal, detectedCompanyId)
                        return colaboradorFinal
                        
                    } catch (e: Exception) {
                        Timber.e(e, "❌ Erro ao salvar colaborador localmente: %s", e.message)
                        // Mesmo com erro ao salvar, tentar iniciar sessão com dados da nuvem
                        userSessionManager.startSession(colaboradorFinal, detectedCompanyId)
                        return colaboradorFinal
                    }
                }
                
                // ✅ SUPERADMIN: Criar automaticamente para rossinys@gmail.com se não encontrou na nuvem
                if (email == "rossinys@gmail.com") {
                    Timber.d("AuthViewModel", "🔧 Criando SUPERADMIN automaticamente para: $email")
                    val colaborador = criarSuperAdminAutomatico(email, firebaseUser.uid, "")
                    if (colaborador != null) {
                        return colaborador
                    }
                }
                
                Timber.d("AuthViewModel", "❌ Colaborador não encontrado nem localmente nem na nuvem")
                _errorMessage.value = "Usuário não encontrado. Contate o administrador para criar sua conta."
                _authState.value = AuthState.Unauthenticated
                return null
            }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ ERRO ao criar/atualizar colaborador online: %s", e.message)
            Timber.e("AuthViewModel", "   Stack trace: ${e.stackTraceToString()}")
            Timber.e("AuthViewModel", "   Email: ${firebaseUser.email}")
            Timber.e("AuthViewModel", "   Firebase UID: ${firebaseUser.uid}")
            return null
        }
    }
    
    /**
     * ✅ NOVO: Altera senha do usuário (primeiro acesso ou alteração normal)
     * Atualiza senha no Firebase e salva hash no banco local para login offline
     */
    fun alterarSenha(novaSenha: String, confirmarSenha: String) {
        if (novaSenha.isBlank() || confirmarSenha.isBlank()) {
            _errorMessage.value = "Todos os campos são obrigatórios"
            return
        }
        
        if (novaSenha.length < 8) {
            _errorMessage.value = "Senha deve ter pelo menos 8 caracteres"
            return
        }
        
        if (novaSenha != confirmarSenha) {
            _errorMessage.value = "Senhas não coincidem"
            return
        }
        
        viewModelScope.launch {
            try {
                showLoading()
                _errorMessage.value = ""
                
                // ✅ CORREÇÃO: Usar sessão local em vez de Firebase Auth
                // Quando o login online falha, não há usuário no Firebase, mas há sessão local
                val colaboradorId = userSessionManager.getCurrentUserId()
                if (colaboradorId == 0L) {
                    Timber.w("AuthViewModel", "⚠️ Nenhum colaborador na sessão local (ID: $colaboradorId)")
                    _errorMessage.value = "Sessão expirada. Faça login novamente."
                    _authState.value = AuthState.Unauthenticated
                    return@launch
                }
                
                val colaborador = appRepository.obterColaboradorPorId(colaboradorId)
                if (colaborador == null) {
                    Timber.w("AuthViewModel", "⚠️ Colaborador não encontrado na sessão")
                    _errorMessage.value = "Colaborador não encontrado. Faça login novamente."
                    _authState.value = AuthState.Unauthenticated
                    return@launch
                }
                
                // ✅ CORREÇÃO: Tentar autenticar no Firebase se não estiver autenticado
                // Isso é necessário para atualizar a senha no Firebase
                var firebaseUser = firebaseAuth.currentUser
                if (firebaseUser == null && isNetworkAvailable() && colaborador.firebaseUid != null) {
                    Timber.d("AuthViewModel", "🔧 Usuário não autenticado no Firebase. Tentando autenticar...")
                    // Não podemos autenticar sem senha, então vamos criar/atualizar a conta
                    // Se a conta não existir, será criada quando o usuário fizer login novamente
                    Timber.d("AuthViewModel", "⚠️ Não é possível atualizar senha no Firebase sem autenticação")
                    Timber.d("AuthViewModel", "   A senha será atualizada localmente e no Firebase na próxima sincronização")
                }
                
                // ✅ CORREÇÃO: Atualizar senha no Firebase se estiver autenticado
                if (isNetworkAvailable() && firebaseUser != null) {
                    try {
                        firebaseUser.updatePassword(novaSenha).await()
                        Timber.d("AuthViewModel", "✅ Senha atualizada no Firebase")
                    } catch (e: Exception) {
                        Timber.w("AuthViewModel", "⚠️ Erro ao atualizar senha no Firebase: ${e.message}")
                        Timber.d("AuthViewModel", "   Continuando para atualizar senha localmente...")
                        // Não falhar se não conseguir atualizar no Firebase
                        // A senha será atualizada na próxima sincronização
                    }
                } else {
                    Timber.d("AuthViewModel", "⚠️ Não é possível atualizar senha no Firebase (offline ou não autenticado)")
                    Timber.d("AuthViewModel", "   A senha será atualizada localmente e sincronizada depois")
                }
                
                // ✅ OFFLINE-FIRST: Salvar hash da senha no banco local para login offline
                // TODO: Implementar hash de senha (PasswordHasher removido)
                val senhaHash = novaSenha // TEMPORÁRIO: Usar senha sem hash até implementar
                
                // Marcar primeiro acesso como concluído e salvar hash
                appRepository.marcarPrimeiroAcessoConcluido(colaborador.id, senhaHash)
                
                Timber.d("AuthViewModel", "✅ Senha atualizada e primeiro acesso concluído")
                
                // Atualizar colaborador local
                val colaboradorAtualizado = colaborador.copy(
                    primeiroAcesso = false,
                    senhaTemporaria = null,
                    senhaHash = senhaHash
                )
                appRepository.atualizarColaborador(colaboradorAtualizado)
                
                // ✅ CORREÇÃO CRÍTICA: Sincronizar colaborador atualizado com a nuvem
                // Isso garante que a senha alterada esteja disponível para login em app vazio
                if (isNetworkAvailable()) {
                    try {
                        Timber.d("AuthViewModel", "🔄 Sincronizando colaborador atualizado com a nuvem após alteração de senha...")
                        sincronizarColaboradorParaNuvem(colaboradorAtualizado, userSessionManager.getCurrentCompanyId())
                        Timber.d("AuthViewModel", "✅ Colaborador sincronizado com sucesso (senha atualizada na nuvem)")
                    } catch (e: Exception) {
                        Timber.w("AuthViewModel", "⚠️ Erro ao sincronizar colaborador após alteração de senha: ${e.message}")
                        Timber.d("AuthViewModel", "   A senha foi atualizada localmente, mas não foi sincronizada com a nuvem")
                        Timber.d("AuthViewModel", "   O colaborador precisará fazer login novamente para sincronizar")
                        // Não falhar o processo se a sincronização falhar - a senha já foi atualizada localmente
                    }
                } else {
                    Timber.d("AuthViewModel", "⚠️ Dispositivo offline - senha atualizada localmente")
                    Timber.d("AuthViewModel", "   A senha será sincronizada com a nuvem quando o dispositivo estiver online")
                }
                
                // Reiniciar sessão
                userSessionManager.startSession(colaboradorAtualizado, userSessionManager.getCurrentCompanyId())
                
                // Criar usuário local
                val localUser = LocalUser(
                    uid = colaboradorAtualizado.id.toString(),
                    email = colaboradorAtualizado.email,
                    displayName = colaboradorAtualizado.nome,
                    nivelAcesso = colaboradorAtualizado.nivelAcesso
                )
                
                _authState.value = AuthState.Authenticated(localUser, isNetworkAvailable())
                showMessage("Senha alterada com sucesso!")
                
            } catch (e: Exception) {
                Timber.e(e, "Erro ao alterar senha: %s", e.message)
                _errorMessage.value = "Erro ao alterar senha: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }
    
    /**
     * Função para converter erros do Firebase em mensagens amigáveis
     */
    private fun getFirebaseErrorMessage(exception: Exception): String {
        return when (exception.message) {
            "The email address is badly formatted." -> "Email com formato inválido"
            "The password is invalid or the user does not have a password." -> "Senha incorreta"
            "There is no user record corresponding to this identifier." -> "Usuário não encontrado"
            "The email address is already in use by another account." -> "Este email já está em uso"
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> "Erro de conexão. Verifique sua internet"
            else -> "Erro: ${exception.message ?: "Erro desconhecido"}"
        }
    }
    
    private data class FirebaseAuthOutcome(
        val autenticado: Boolean,
        val colaboradorAtualizado: Colaborador
    )
    
    /**
     * Garante que um colaborador validado tenha autenticação ativa no Firebase.
     * Se a conta ainda não existir no Firebase Auth, cria automaticamente utilizando a senha validada.
     */
    private suspend fun garantirAutenticacaoFirebase(
        colaborador: Colaborador,
        senhaValidada: String
    ): FirebaseAuthOutcome {
        Timber.d("AuthViewModel", "🔐 Garantindo autenticação Firebase para ${colaborador.email}")
        var colaboradorAtualizado = colaborador
        
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null && firebaseUser.email.equals(colaborador.email, ignoreCase = true)) {
            val uid = firebaseUser.uid
            if (!uid.isNullOrBlank() && uid != colaboradorAtualizado.firebaseUid) {
                colaboradorAtualizado = atualizarFirebaseUidLocalESync(colaboradorAtualizado, uid)
            }
            return FirebaseAuthOutcome(true, colaboradorAtualizado)
        }
        
        val outcome = tentarAutenticarOuCriarFirebaseUser(colaboradorAtualizado, senhaValidada)
        return outcome ?: FirebaseAuthOutcome(false, colaboradorAtualizado)
    }
    
    private suspend fun tentarAutenticarOuCriarFirebaseUser(
        colaborador: Colaborador,
        senhaValidada: String
    ): FirebaseAuthOutcome? {
        var colaboradorAtualizado = colaborador
        return try {
            val firebaseResult = firebaseAuth.signInWithEmailAndPassword(colaborador.email, senhaValidada).await()
            val user = firebaseResult.user
            if (user != null) {
                colaboradorAtualizado = atualizarFirebaseUidLocalESync(colaboradorAtualizado, user.uid)
                FirebaseAuthOutcome(true, colaboradorAtualizado)
            } else {
                Timber.w("AuthViewModel", "⚠️ SignInFirebase retornou usuário nulo")
                FirebaseAuthOutcome(false, colaboradorAtualizado)
            }
        } catch (e: Exception) {
            val errorCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
            if (errorCode == "ERROR_USER_NOT_FOUND") {
                Timber.w("AuthViewModel", "⚠️ Usuário não existe no Firebase. Criando automaticamente: ${colaborador.email}")
                return try {
                    val createResult = firebaseAuth.createUserWithEmailAndPassword(colaborador.email, senhaValidada).await()
                    val newUser = createResult.user
                    if (newUser != null) {
                        colaboradorAtualizado = atualizarFirebaseUidLocalESync(colaboradorAtualizado, newUser.uid)
                        FirebaseAuthOutcome(true, colaboradorAtualizado)
                    } else {
                        Timber.w("AuthViewModel", "⚠️ Criação do usuário retornou nulo")
                        FirebaseAuthOutcome(false, colaboradorAtualizado)
                    }
                } catch (createError: Exception) {
                    Timber.e("AuthViewModel", "❌ Falha ao criar usuário no Firebase: ${createError.message}")
                    FirebaseAuthOutcome(false, colaboradorAtualizado)
                }
            } else {
                Timber.w(
                    "AuthViewModel",
                    "⚠️ Erro ao autenticar no Firebase (${errorCode ?: e.javaClass.simpleName}): ${e.message}"
                )
                FirebaseAuthOutcome(false, colaboradorAtualizado)
            }
        }
    }
    
    private suspend fun atualizarFirebaseUidLocalESync(
        colaborador: Colaborador,
        novoFirebaseUid: String
    ): Colaborador {
        if (novoFirebaseUid.isBlank() || colaborador.firebaseUid == novoFirebaseUid) {
            return colaborador
        }
        
        val colaboradorAtualizado = colaborador.copy(firebaseUid = novoFirebaseUid)
        appRepository.atualizarColaborador(colaboradorAtualizado)
        
        runCatching {
            sincronizarColaboradorParaNuvem(colaboradorAtualizado, userSessionManager.getCurrentCompanyId()) // Assuming companyId is already set or default
        }.onFailure {
            Timber.w("AuthViewModel", "⚠️ Falha ao sincronizar colaborador com novo Firebase UID: ${it.message}")
        }
        
        return colaboradorAtualizado
    }
    
    /**
     * Sincroniza um colaborador específico para a nuvem (Firestore)
     * Usado para sincronizar colaboradores criados ou atualizados localmente
     */
    private suspend fun sincronizarColaboradorParaNuvem(colaborador: Colaborador, companyId: String) {
        try {
            Timber.d("AuthViewModel", "=== SINCRONIZANDO COLABORADOR PARA NUVEM ===")
            Timber.d("AuthViewModel", "   ID: ${colaborador.id}")
            Timber.d("AuthViewModel", "   Nome: ${colaborador.nome}")
            Timber.d("AuthViewModel", "   Email: ${colaborador.email}")
            Timber.d("AuthViewModel", "   Empresa: $companyId")
            Timber.d("AuthViewModel", "   Aprovado: ${colaborador.aprovado}")
            Timber.d("AuthViewModel", "   Usuário atual: ${firebaseAuth.currentUser?.uid}")
            Timber.d("AuthViewModel", "   Email do token: ${firebaseAuth.currentUser?.email}")
            
            // Estrutura: empresas/empresa_001/entidades/colaboradores/items
            val collectionRef = firestore
                .collection("empresas")
                .document(companyId)
                .collection("entidades")
                .document("colaboradores")
                .collection("items")
            
            Timber.d("AuthViewModel", "   Caminho: empresas/$companyId/entidades/colaboradores/items")
            
            // ✅ CORREÇÃO CRÍTICA: Usar Gson para converter colaborador para Map (snake_case automático)
            // Isso garante consistência com o ColaboradorSyncHandler e as regras do Firestore
            val colaboradorJson = gson.toJson(colaborador)
            @Suppress("UNCHECKED_CAST")
            val colaboradorMap = gson.fromJson(colaboradorJson, Map::class.java) as? MutableMap<String, Any?> 
                ?: mutableMapOf<String, Any?>()
            
            // Adicionar campos adicionais necessários
            colaboradorMap["room_id"] = colaborador.id
            colaboradorMap["id"] = colaborador.id
            colaboradorMap["last_modified"] = FieldValue.serverTimestamp()
            colaboradorMap["sync_timestamp"] = FieldValue.serverTimestamp()
            
            // ✅ CORREÇÃO: Converter campos de data para Timestamp do Firestore
            colaboradorMap["data_cadastro"] = Timestamp(Date(colaborador.dataCadastro))
            colaboradorMap["data_ultima_atualizacao"] = Timestamp(Date(colaborador.dataUltimaAtualizacao))
            colaborador.dataAprovacao?.let { colaboradorMap["data_aprovacao"] = Timestamp(Date(it)) }
            colaborador.dataUltimoAcesso?.let { colaboradorMap["data_ultimo_acesso"] = Timestamp(Date(it)) }
            
            // ✅ CORREÇÃO: Garantir que nivel_acesso seja string (enum)
            colaboradorMap["nivel_acesso"] = colaborador.nivelAcesso.name
            
            Timber.d("AuthViewModel", "   Map criado com ${colaboradorMap.size} campos")
            
            // ✅ CORREÇÃO: Usar ID apropriado para evitar conflitos
            // Prioridade: 1) Firebase UID (se disponível), 2) Email (para colaboradores pendentes sem UID), 3) ID numérico (fallback)
            val documentId: String = colaborador.firebaseUid?.takeIf { it.isNotBlank() }
                ?: if (colaborador.aprovado == false && colaborador.firebaseUid == null) {
                    // Colaborador pendente sem UID: usar email como ID único para evitar conflitos
                    colaborador.email.replace(".", "_").replace("@", "_")
                } else {
                    // Colaborador já aprovado ou com firebaseUid: usar ID numérico
                    colaborador.id.toString()
                }
            
            Timber.d("AuthViewModel", "   Criando documento com ID: $documentId (ID local: ${colaborador.id}, firebaseUid: ${colaborador.firebaseUid}, email: ${colaborador.email}, aprovado: ${colaborador.aprovado})")
            
            try {
                collectionRef
                    .document(documentId)
                    .set(colaboradorMap)
                    .await()
                Timber.d("AuthViewModel", "✅ Colaborador criado no Firestore com sucesso! (ID: $documentId)")
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                // Se o documento já existe, atualizar em vez de criar
                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.ALREADY_EXISTS) {
                    Timber.d("AuthViewModel", "⚠️ Documento já existe, atualizando...")
                    collectionRef
                        .document(documentId)
                        .set(colaboradorMap)
                        .await()
                    Timber.d("AuthViewModel", "✅ Colaborador atualizado no Firestore")
                } else {
                    throw e
                }
            }
            
            Timber.d("AuthViewModel", "✅ Colaborador sincronizado com sucesso para a nuvem")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Erro ao sincronizar colaborador para a nuvem: %s", e.message)
            Timber.e("AuthViewModel", "   Tipo de erro: ${e.javaClass.simpleName}")
            Timber.e("AuthViewModel", "   Stack trace: ${e.stackTraceToString()}")
            
            // Log específico para erros de permissão
            if (e.message?.contains("PERMISSION_DENIED") == true || 
                e.message?.contains("permission-denied") == true) {
                Timber.e("AuthViewModel", "❌ ERRO DE PERMISSÃO: Verifique as regras do Firestore")
                Timber.e("AuthViewModel", "   Usuário autenticado: ${firebaseAuth.currentUser != null}")
                Timber.e("AuthViewModel", "   UID: ${firebaseAuth.currentUser?.uid}")
                Timber.e("AuthViewModel", "   Email: ${firebaseAuth.currentUser?.email}")
            }
            
            throw e
        }
    }
    
    /**
     * ✅ NOVO: Busca colaborador na nuvem (Firestore) por email usando busca global
     * Retorna o colaborador e o ID da empresa se encontrado, null caso contrário
     */
    private suspend fun buscarColaboradorNaNuvemPorEmail(email: String): Pair<Colaborador, String>? {
        return try {
            crashlytics.log("[BUSCA_NUVEM] 🔍 Iniciando busca global na nuvem para: $email")
            crashlytics.setCustomKey("busca_nuvem_email", email)
            crashlytics.setCustomKey("busca_nuvem_firebase_auth", firebaseAuth.currentUser != null)
            crashlytics.setCustomKey("busca_nuvem_firebase_uid", firebaseAuth.currentUser?.uid ?: "null")
            
            Timber.d("AuthViewModel", "🔍 === INICIANDO BUSCA GLOBAL NA NUVEM ===")
            Timber.d("AuthViewModel", "   Email: $email")
            Timber.d("AuthViewModel", "   Firebase Auth autenticado: ${firebaseAuth.currentUser != null}")
            Timber.d("AuthViewModel", "   Firebase UID: ${firebaseAuth.currentUser?.uid ?: "não autenticado"}")
            
            val emailNormalizado = email.trim().lowercase()
            
            // 1. Tentar busca exata via collectionGroup
            crashlytics.log("[BUSCA_NUVEM] Tentando busca 1 (email exato via collectionGroup)...")
            var querySnapshot = try {
                firestore.collectionGroup("items")
                    .whereEqualTo("email", email)
                    .get()
                    .await()
            } catch (e: Exception) {
                crashlytics.setCustomKey("busca_nuvem_erro_collection_group", true)
                crashlytics.setCustomKey("busca_nuvem_erro_tipo", e.javaClass.simpleName)
                crashlytics.log("[BUSCA_NUVEM] ❌ Erro na busca collectionGroup: ${e.message}")
                crashlytics.recordException(e)
                throw e
            }
            
            crashlytics.setCustomKey("busca_nuvem_resultado_1", querySnapshot.size())
            Timber.d("AuthViewModel", "   Busca 1 (email exato): ${querySnapshot.size()} documentos encontrados")
            var doc = querySnapshot.documents.find { it.reference.path.contains("/colaboradores/items/") }
            
            // 2. Se não encontrou, tentar email normalizado
            if (doc == null && email != emailNormalizado) {
                Timber.d("AuthViewModel", "   Tentando busca 2 (email normalizado): $emailNormalizado")
                querySnapshot = firestore.collectionGroup("items")
                    .whereEqualTo("email", emailNormalizado)
                    .get()
                    .await()
                Timber.d("AuthViewModel", "   Busca 2 (email normalizado): ${querySnapshot.size()} documentos encontrados")
                doc = querySnapshot.documents.find { it.reference.path.contains("/colaboradores/items/") }
            }
            
            // 3. Se não encontrou, tentar busca via firebaseUid (mais robusto)
            if (doc == null) {
                val firebaseUid = firebaseAuth.currentUser?.uid
                if (firebaseUid != null) {
                    Timber.d("AuthViewModel", "   Tentando busca 3 (firebaseUid): $firebaseUid")
                    querySnapshot = firestore.collectionGroup("items")
                        .whereEqualTo("firebaseUid", firebaseUid)
                        .get()
                        .await()
                    Timber.d("AuthViewModel", "   Busca 3 (firebaseUid): ${querySnapshot.size()} documentos encontrados")
                    doc = querySnapshot.documents.find { it.reference.path.contains("/colaboradores/items/") }
                }
            }
            
            // 4. Fallback para empresa_001 se collectionGroup falhar ou não encontrar
            if (doc == null) {
                crashlytics.log("[BUSCA_NUVEM] Tentando fallback direto na empresa_001...")
                Timber.d("AuthViewModel", "   Não encontrado via collectionGroup. Tentando fallback direto na empresa_001...")
                val collectionRef = firestore.collection("empresas").document("empresa_001")
                    .collection("entidades").document("colaboradores").collection("items")
                
                try {
                    crashlytics.log("[BUSCA_NUVEM] Fallback: Buscando por email na empresa_001...")
                    querySnapshot = collectionRef.whereEqualTo("email", email).get().await()
                    crashlytics.setCustomKey("busca_nuvem_fallback_resultado", querySnapshot.size())
                    doc = querySnapshot.documents.firstOrNull()
                    
                    if (doc == null) {
                        val firebaseUid = firebaseAuth.currentUser?.uid
                        if (firebaseUid != null) {
                            crashlytics.log("[BUSCA_NUVEM] Fallback: Buscando por firebaseUid na empresa_001...")
                            querySnapshot = collectionRef.whereEqualTo("firebaseUid", firebaseUid).get().await()
                            crashlytics.setCustomKey("busca_nuvem_fallback_resultado_uid", querySnapshot.size())
                            doc = querySnapshot.documents.firstOrNull()
                        }
                    }
                    crashlytics.log("[BUSCA_NUVEM] Fallback empresa_001: ${if (doc != null) "ENCONTRADO" else "NÃO ENCONTRADO"}")
                    Timber.d("AuthViewModel", "   Fallback empresa_001: ${if (doc != null) "ENCONTRADO" else "NÃO ENCONTRADO"}")
                } catch (e: Exception) {
                    crashlytics.setCustomKey("busca_nuvem_erro_fallback", true)
                    crashlytics.setCustomKey("busca_nuvem_erro_fallback_tipo", e.javaClass.simpleName)
                    crashlytics.setCustomKey("busca_nuvem_erro_fallback_mensagem", e.message ?: "unknown")
                    crashlytics.log("[BUSCA_NUVEM] ❌ Erro no fallback empresa_001: ${e.message}")
                    crashlytics.recordException(e)
                    Timber.e("AuthViewModel", "   Erro no fallback empresa_001: ${e.message}", e)
                }
            }
            
            if (doc == null) {
                crashlytics.setCustomKey("busca_nuvem_resultado_final", "nao_encontrado")
                crashlytics.log("[BUSCA_NUVEM] ⚠️ Colaborador não encontrado na nuvem em nenhuma coleção")
                Timber.w("AuthViewModel", "⚠️ Colaborador não encontrado na nuvem em nenhuma coleção.")
                return null
            }
            
            crashlytics.setCustomKey("busca_nuvem_resultado_final", "encontrado")
            crashlytics.log("[BUSCA_NUVEM] ✅ Colaborador encontrado na nuvem!")

            val data = doc.data ?: return null
            val path = doc.reference.path
            val segments = path.split("/")
            val companyId = if (segments.size > 1 && segments[0] == "empresas") segments[1] else "empresa_001"
            
            Timber.d("AuthViewModel", "DIAG: Documento encontrado na nuvem! Path: $path, Empresa: $companyId")

            // Converter Timestamps para Date (GSON não lida nativamente com Firebase Timestamps)
            val dataConvertida = data.toMutableMap()
            fun toDate(v: Any?): Date? = when(v) {
                is com.google.firebase.Timestamp -> v.toDate()
                is Date -> v
                is Long -> Date(v)
                else -> null
            }
            
            // Campos que podem vir do Firestore como Timestamp
            val dateFields = listOf(
                "data_cadastro", "data_ultima_atualizacao", "data_aprovacao", 
                "data_ultimo_acesso", "data_nascimento"
            )
            
            dateFields.forEach { field ->
                if (data.containsKey(field)) {
                    dataConvertida[field] = toDate(data[field])
                }
            }
            
            // Garantir que campos essenciais não sejam nulos para o Room
            if (dataConvertida["data_cadastro"] == null) dataConvertida["data_cadastro"] = Date()
            if (dataConvertida["data_ultima_atualizacao"] == null) dataConvertida["data_ultima_atualizacao"] = Date()

            val colaboradorId = doc.id.toLongOrNull() ?: (data["id"] as? Number)?.toLong() ?: 0L
            
            // Com a nova política de GSON (LOWER_CASE_WITH_UNDERSCORES) e @SerializedName na entidade,
            // o mapeamento deve ser automático e robusto.
            val colaborador = gson.fromJson(gson.toJson(dataConvertida), Colaborador::class.java).copy(id = colaboradorId)
            
            Timber.d("AuthViewModel", "✅ Colaborador processado: ${colaborador.nome} (ID: ${colaborador.id}, Acesso: ${colaborador.nivelAcesso})")
            Pair(colaborador, companyId)
            
        } catch (e: Exception) {
            crashlytics.setCustomKey("busca_nuvem_erro_geral", true)
            crashlytics.setCustomKey("busca_nuvem_erro_tipo", e.javaClass.simpleName)
            crashlytics.setCustomKey("busca_nuvem_erro_mensagem", e.message ?: "unknown")
            crashlytics.log("[BUSCA_NUVEM] ❌ Erro na busca na nuvem: ${e.message}")
            
            // ✅ LOG ESPECÍFICO PARA ERROS DE PERMISSÃO
            if (e is FirebaseFirestoreException) {
                crashlytics.setCustomKey("busca_nuvem_erro_firestore_code", e.code.name)
                crashlytics.log("[BUSCA_NUVEM] ❌ Erro Firestore: ${e.code.name} - ${e.message}")
                
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    crashlytics.setCustomKey("busca_nuvem_permission_denied", true)
                    crashlytics.log("[BUSCA_NUVEM] ❌ PERMISSION_DENIED: Usuário não autenticado ou sem permissão")
                    crashlytics.log("[BUSCA_NUVEM] ❌ PERMISSION_DENIED: Verificar se as regras do Firestore permitem busca sem autenticação")
                    crashlytics.log("[BUSCA_NUVEM] ❌ PERMISSION_DENIED: Path tentado: collectionGroup('items')")
                    crashlytics.setCustomKey("busca_nuvem_firebase_auth_uid", firebaseAuth.currentUser?.uid ?: "null")
                    crashlytics.setCustomKey("busca_nuvem_firebase_auth_email", firebaseAuth.currentUser?.email ?: "null")
                }
            }
            
            crashlytics.recordException(e)
            Timber.e("AuthViewModel", "❌ Erro na busca na nuvem: ${e.message}", e)
            Timber.e("AuthViewModel", "   Stack trace: ${e.stackTraceToString()}")
            null
        }
    }
    
    /**
     * ✅ NOVO: Aguarda e verifica a presença da claim 'companyId' no token do Firebase.
     * Tenta por até 10 segundos (5 tentativas de 2 segundos).
     * Essencial para evitar PERMISSION_DENIED em apps vazios logo após o login.
     */
    private suspend fun waitAndVerifyCompanyIdClaim(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        var attempts = 0
        val maxAttempts = 5
        
        while (attempts < maxAttempts) {
            attempts++
            try {
                Timber.d("AuthViewModel", "DIAG: Verificando claims (Tentativa $attempts/$maxAttempts)...")
                val tokenResult = user.getIdToken(true).await()
                val claims = tokenResult.claims
                val companyId = claims["companyId"] as? String
                
                if (!companyId.isNullOrBlank()) {
                    Timber.d("AuthViewModel", "DIAG: Claim 'companyId' encontrada: $companyId")
                    return true
                }
                
                Timber.d("AuthViewModel", "DIAG: Claim 'companyId' ainda nao disponivel. Aguardando 2s...")
                kotlinx.coroutines.delay(2000)
            } catch (e: Exception) {
                Timber.e("AuthViewModel", "DIAG: Erro ao verificar claims na tentativa $attempts: ${e.message}")
                kotlinx.coroutines.delay(2000)
            }
        }
        
        return false
    }

    
    /**
     * ✅ SUPERADMIN: Cria colaborador superadmin automaticamente para rossinys@gmail.com
     * Sempre cria como ADMIN, aprovado, sem primeiro acesso obrigatório
     */
    private suspend fun criarSuperAdminAutomatico(
        email: String,
        firebaseUid: String?,
        senha: String
    ): Colaborador? {
        try {
            Timber.d("AuthViewModel", "🔧 Criando SUPERADMIN: $email")
            
            // Verificar se já existe
            val existente = appRepository.obterColaboradorPorEmail(email)
            if (existente != null) {
                // Atualizar para garantir que é ADMIN e aprovado
                val atualizado = existente.copy(
                    nivelAcesso = NivelAcesso.ADMIN,
                    aprovado = true,
                    ativo = true,
                    primeiroAcesso = false, // Superadmin nunca precisa alterar senha
                    firebaseUid = firebaseUid ?: existente.firebaseUid,
                    senhaHash = if (senha.isNotEmpty()) senha else existente.senhaHash, // Salvar senha para login offline
                    senhaTemporaria = null, // Limpar senha temporária
                    dataAprovacao = existente.dataAprovacao ?: System.currentTimeMillis(),
                    aprovadoPor = existente.aprovadoPor ?: "Sistema (Superadmin)"
                )
                appRepository.atualizarColaborador(atualizado)
                userSessionManager.startSession(atualizado)
                Timber.d("AuthViewModel", "✅ SUPERADMIN atualizado: ${atualizado.nome}")
                return atualizado
            }
            
            // Criar novo superadmin
            val senhaHash = if (senha.isNotEmpty()) senha else "superadmin123" // TEMPORÁRIO: Senha padrão se não fornecida
            
            val novoColaborador = Colaborador(
                nome = "Super Admin",
                email = email,
                nivelAcesso = NivelAcesso.ADMIN,
                aprovado = true,
                ativo = true,
                primeiroAcesso = false, // Superadmin nunca precisa alterar senha
                senhaHash = senhaHash, // Salvar senha para login offline
                senhaTemporaria = null,
                firebaseUid = firebaseUid,
                dataAprovacao = System.currentTimeMillis(),
                aprovadoPor = "Sistema (Superadmin Automático)"
            )
            
            val colaboradorId = appRepository.inserirColaborador(novoColaborador)
            val colaboradorComId = novoColaborador.copy(id = colaboradorId)
            
            Timber.d("AuthViewModel", "✅ SUPERADMIN criado: ${colaboradorComId.nome}")
            
            // ✅ NOVO: Sincronizar superadmin para a nuvem imediatamente
            // Isso dispara a Cloud Function que define as Custom Claims (admin=true)
            if (isNetworkAvailable()) {
                try {
                    Timber.d("AuthViewModel", "🔄 Sincronizando SUPERADMIN para a nuvem...")
                    sincronizarColaboradorParaNuvem(colaboradorComId, "empresa_001")
                    Timber.d("AuthViewModel", "✅ SUPERADMIN sincronizado")
                } catch (e: Exception) {
                    Timber.w("AuthViewModel", "⚠️ Erro ao sincronizar SUPERADMIN: ${e.message}")
                }
            }
            
            userSessionManager.startSession(colaboradorComId, "empresa_001")
            
            return colaboradorComId
            
        } catch (e: Exception) {
            Timber.e(e, "Erro ao criar superadmin: %s", e.message)
            return null
        }
    }
}

/**
 * Estados da autenticação
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val user: Any, val isOnline: Boolean) : AuthState()
    data class FirstAccessRequired(val colaborador: com.example.gestaobilhares.data.entities.Colaborador) : AuthState()
}

/**
 * Classe para representar usuário local (offline)
 */
data class LocalUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val nivelAcesso: NivelAcesso
) 


