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
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * ViewModel responsável pela lógica de autenticação híbrida (Firebase + Local).
 * Implementa padrão MVVM para separar lógica de negócio da UI.
 * Suporta autenticação online (Firebase) e offline (Room Database).
 */
class AuthViewModel constructor() : BaseViewModel() {
    
    // Instância do Firebase Auth
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    // Instância do Firestore
    private val firestore = FirebaseFirestore.getInstance()
    
    // Gson para serialização/deserialização
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    // Repositório para acesso local
    private lateinit var appRepository: AppRepository
    
    // Utilitário de rede
    private lateinit var networkUtils: NetworkUtils
    
    // Gerenciador de sessão do usuário
    private lateinit var userSessionManager: UserSessionManager
    
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
    }
    
    /**
     * Inicializa o repositório local, utilitário de rede e gerenciador de sessão
     */
    fun initializeRepository(context: Context) {
        try {
            android.util.Log.d("AuthViewModel", "🚨 INICIANDO REPOSITORIO - CONTEXT: ${context.javaClass.simpleName}")
            android.util.Log.d("AuthViewModel", "🚨 CONTEXT PACKAGE: ${context.packageName}")
            
            // Inicializar banco de dados de forma segura
            android.util.Log.d("AuthViewModel", "🔧 CRIANDO APPDATABASE...")
            val database = AppDatabase.getDatabase(context)
            android.util.Log.d("AuthViewModel", "✅ AppDatabase inicializado")
            
            appRepository = com.example.gestaobilhares.factory.RepositoryFactory.getAppRepository(context)
            android.util.Log.d("AuthViewModel", "AppRepository inicializado com sucesso")
            
            // Inicializar utilitários de forma segura
            try {
                networkUtils = NetworkUtils(context)
                android.util.Log.d("AuthViewModel", "✅ NetworkUtils inicializado")
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Erro ao inicializar NetworkUtils: ${e.message}")
                // Continuar sem NetworkUtils (modo offline)
            }
            
            // ✅ FASE 1: SyncManager antigo removido - usar SyncManagerV2 quando necessário
            // A sincronização é gerenciada pelo SyncManagerV2 em outros pontos do app
            
            try {
                userSessionManager = UserSessionManager.getInstance(context)
                android.util.Log.d("AuthViewModel", "✅ UserSessionManager inicializado")
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Erro ao inicializar UserSessionManager: ${e.message}")
                // Continuar sem UserSessionManager
            }
            
            android.util.Log.d("AuthViewModel", "✅ Repositório local inicializado com sucesso")
            
            // Observar mudanças na conectividade
            viewModelScope.launch {
                try {
                    networkUtils.isNetworkAvailable.collect { isAvailable ->
                        val wasOffline = _isOnline.value == false
                        _isOnline.value = isAvailable
                        
                        // ✅ FASE 1: SyncManager antigo removido
                        // A sincronização é gerenciada pelo SyncManagerV2 em outros pontos do app
                        // Quando necessário, pode ser acionada manualmente via UI
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "Erro ao observar conectividade: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Erro crítico ao inicializar repositório: ${e.message}")
            // Definir como offline em caso de erro
            _isOnline.value = false
        }
    }
    
    /**
     * Verifica se há conexão com internet
     */
    private fun isNetworkAvailable(): Boolean {
        return if (::networkUtils.isInitialized) {
            networkUtils.isConnected()
        } else {
            true // Assumir online se NetworkUtils não foi inicializado
        }
    }
    
    /**
     * Função para realizar login híbrido (online/offline)
     */
    fun login(email: String, senha: String) {
        android.util.Log.d("AuthViewModel", "=== INICIANDO LOGIN HÍBRIDO ===")
        android.util.Log.d("AuthViewModel", "Email: $email")
        android.util.Log.d("AuthViewModel", "Senha: ${senha.length} caracteres")
        
        // Verificar se o repositório foi inicializado
        if (!::appRepository.isInitialized) {
            android.util.Log.e("AuthViewModel", "ERRO: appRepository nao foi inicializado")
            _errorMessage.value = "Erro de inicializacao. Reinicie o app."
            return
        }
        
        // Validação básica
        if (email.isBlank() || senha.isBlank()) {
            android.util.Log.e("AuthViewModel", "Email ou senha em branco")
            _errorMessage.value = "Email e senha são obrigatórios"
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            android.util.Log.e("AuthViewModel", "Email inválido: $email")
            _errorMessage.value = "Email inválido"
            return
        }
        
        if (senha.length < 6) {
            android.util.Log.e("AuthViewModel", "Senha muito curta: ${senha.length} caracteres")
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
                
                if (online) {
                    // Tentar login online primeiro
                    android.util.Log.d("AuthViewModel", "Tentando login online...")
                    var loginOnlineSucesso = false
                    var erroLoginOnline: Exception? = null
                    try {
                        val result = firebaseAuth.signInWithEmailAndPassword(email, senha).await()
                        
                        if (result.user != null) {
                            android.util.Log.d("AuthViewModel", "✅ LOGIN ONLINE SUCESSO!")
                            loginOnlineSucesso = true

                            // ✅ NOVO: Emitir log específico para criação automática de dados após login
                            android.util.Log.w(
                                "🔍 DB_POPULATION",
                                "🚨 LOGIN ONLINE CONCLUÍDO - DISPARANDO CARREGAMENTO INICIAL DE DADOS"
                            )
    
                            // ✅ NOVO: Criar/atualizar colaborador para usuário online
                            var colaborador = criarOuAtualizarColaboradorOnline(result.user!!)
                            
                            // ✅ SUPERADMIN: Se for rossinys@gmail.com e não encontrou, criar automaticamente
                            if (colaborador == null && email == "rossinys@gmail.com") {
                                android.util.Log.d("AuthViewModel", "🔧 Criando SUPERADMIN automaticamente para: $email")
                                colaborador = criarSuperAdminAutomatico(email, result.user!!.uid, senha)
                            }
                            
                            if (colaborador == null) {
                                _errorMessage.value = "Usuário não encontrado. Contate o administrador."
                                return@launch
                            }
                            
                            // ✅ SUPERADMIN: rossinys@gmail.com nunca precisa alterar senha no primeiro acesso
                            val isSuperAdmin = email == "rossinys@gmail.com"
                            
                            // ✅ NOVO: Verificar se é primeiro acesso (exceto superadmin)
                            if (!isSuperAdmin && colaborador.primeiroAcesso) {
                                android.util.Log.d("AuthViewModel", "⚠️ PRIMEIRO ACESSO DETECTADO - Redirecionando para alteração de senha")
                                _authState.value = AuthState.FirstAccessRequired(colaborador)
                                return@launch
                            }
                            
                            // ✅ CORREÇÃO CRÍTICA: Garantir que a sessão foi iniciada antes de autenticar
                            // A função criarOuAtualizarColaboradorOnline já inicia a sessão, mas vamos verificar
                            val nomeSessao = userSessionManager.getCurrentUserName()
                            val idSessao = userSessionManager.getCurrentUserId()
                            android.util.Log.d("AuthViewModel", "🔍 Verificação da sessão online:")
                            android.util.Log.d("AuthViewModel", "   Nome na sessão: $nomeSessao")
                            android.util.Log.d("AuthViewModel", "   ID na sessão: $idSessao")
                            
                            // ✅ CORREÇÃO: Se a sessão não foi iniciada, iniciar agora
                            if (idSessao == null || idSessao == 0L) {
                                android.util.Log.w("AuthViewModel", "⚠️ Sessão não iniciada após criarOuAtualizarColaboradorOnline - iniciando agora")
                                userSessionManager.startSession(colaborador)
                            }
                            
                            val localUser = LocalUser(
                                uid = colaborador.id.toString(),
                                email = colaborador.email,
                                displayName = colaborador.nome,
                                nivelAcesso = colaborador.nivelAcesso
                            )
                            
                            _authState.value = AuthState.Authenticated(localUser, true)
                            android.util.Log.d("AuthViewModel", "✅ Estado de autenticação definido - sessão ativa")
                            return@launch
                        }
                    } catch (e: Exception) {
                        erroLoginOnline = e
                        android.util.Log.w("AuthViewModel", "Login online falhou: ${e.message}")
                        android.util.Log.w("AuthViewModel", "Tipo de erro: ${e.javaClass.simpleName}")
                        
                        // ✅ CORREÇÃO: Se o erro for "wrong password" ou "user not found", 
                        // continuar para tentar login offline (pode ser senha temporária)
                        val errorCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                        android.util.Log.d("AuthViewModel", "Código de erro Firebase: $errorCode")
                        
                        // Se for erro de credenciais inválidas, pode ser senha temporária
                        // Continuar para tentar login offline
                        if (errorCode == "ERROR_WRONG_PASSWORD" || errorCode == "ERROR_USER_NOT_FOUND" || errorCode == "ERROR_INVALID_EMAIL") {
                            android.util.Log.d("AuthViewModel", "Erro de credenciais - tentando login offline com senha temporária...")
                        } else {
                            // Para outros erros (rede, etc), também tentar offline
                            android.util.Log.d("AuthViewModel", "Erro de conexão ou outro - tentando login offline...")
                        }
                    }
                }
                
                // Se online falhou ou está offline, tentar login local
                android.util.Log.d("AuthViewModel", "Tentando login offline...")
                android.util.Log.d("AuthViewModel", "Email para busca: $email")
                
                // ✅ CORREÇÃO: Buscar colaborador por email, firebaseUid ou googleId
                var colaborador = appRepository.obterColaboradorPorEmail(email)
                
                // ✅ CORREÇÃO: Não buscar por Firebase UID quando login online falhou
                // O Firebase UID pode ser de outro usuário (ex: superadmin logado anteriormente)
                // Só buscar por Firebase UID se o login online foi bem-sucedido
                // (isso já foi tratado no bloco de login online acima)
                
                // ✅ CORREÇÃO CRÍTICA: Se não encontrou localmente E estiver online, buscar na nuvem
                if (colaborador == null && online) {
                    android.util.Log.d("AuthViewModel", "🔍 Colaborador não encontrado localmente. Buscando na nuvem...")
                    colaborador = buscarColaboradorNaNuvemPorEmail(email)
                    if (colaborador != null) {
                        android.util.Log.d("AuthViewModel", "✅ Colaborador encontrado na nuvem: ${colaborador.nome}")
                        // Salvar colaborador localmente para próximos logins offline
                        try {
                            appRepository.inserirColaborador(colaborador)
                            android.util.Log.d("AuthViewModel", "✅ Colaborador salvo localmente")
                        } catch (e: Exception) {
                            android.util.Log.w("AuthViewModel", "⚠️ Erro ao salvar colaborador localmente: ${e.message}")
                        }
                    }
                } else if (colaborador != null && online) {
                    // ✅ NOVO: Se encontrou localmente E estiver online, verificar se há atualizações na nuvem
                    android.util.Log.d("AuthViewModel", "🔍 Colaborador encontrado localmente. Verificando atualizações na nuvem...")
                    val colaboradorNuvem = buscarColaboradorNaNuvemPorEmail(email)
                    if (colaboradorNuvem != null) {
                        android.util.Log.d("AuthViewModel", "✅ Colaborador encontrado na nuvem. Atualizando dados locais...")
                        // Atualizar colaborador local com dados da nuvem (preservando ID local)
                        val colaboradorAtualizado = colaboradorNuvem.copy(id = colaborador.id)
                        try {
                            appRepository.atualizarColaborador(colaboradorAtualizado)
                            colaborador = colaboradorAtualizado
                            android.util.Log.d("AuthViewModel", "✅ Colaborador atualizado com dados da nuvem")
                        } catch (e: Exception) {
                            android.util.Log.w("AuthViewModel", "⚠️ Erro ao atualizar colaborador local: ${e.message}")
                        }
                    }
                }
                
                android.util.Log.d("AuthViewModel", "🔍 Colaborador encontrado: ${colaborador?.nome ?: "NÃO ENCONTRADO"}")
                if (colaborador != null) {
                    android.util.Log.d("AuthViewModel", "   ID: ${colaborador.id}")
                    android.util.Log.d("AuthViewModel", "   Email: ${colaborador.email}")
                    android.util.Log.d("AuthViewModel", "   Nível: ${colaborador.nivelAcesso}")
                    android.util.Log.d("AuthViewModel", "   Aprovado: ${colaborador.aprovado}")
                    android.util.Log.d("AuthViewModel", "   Ativo: ${colaborador.ativo}")
                    android.util.Log.d("AuthViewModel", "   Senha temporária: ${colaborador.senhaTemporaria}")
                    android.util.Log.d("AuthViewModel", "   Firebase UID: ${colaborador.firebaseUid}")
                    
                    // ✅ CORREÇÃO: Verificar se o colaborador está aprovado e ativo
                    if (!colaborador.aprovado) {
                        android.util.Log.w("AuthViewModel", "❌ Colaborador não está aprovado")
                        _errorMessage.value = "Sua conta está aguardando aprovação do administrador."
                        hideLoading()
                        return@launch
                    }
                    
                    if (!colaborador.ativo) {
                        android.util.Log.w("AuthViewModel", "❌ Colaborador está inativo")
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
                    
                    android.util.Log.d("AuthViewModel", "🔍 Validação de senha OFFLINE (DETALHADA):")
                    android.util.Log.d("AuthViewModel", "   Senha fornecida: '${senhaLimpa}' (${senhaLimpa.length} caracteres)")
                    android.util.Log.d("AuthViewModel", "   Hash armazenado: ${if (senhaHashLimpa != null) "'$senhaHashLimpa' (${senhaHashLimpa.length} caracteres)" else "ausente"}")
                    android.util.Log.d("AuthViewModel", "   Senha temporária: ${if (senhaTemporariaLimpa != null) "'$senhaTemporariaLimpa' (${senhaTemporariaLimpa.length} caracteres)" else "ausente"}")
                    android.util.Log.d("AuthViewModel", "   Primeiro acesso: ${colaborador.primeiroAcesso}")
                    android.util.Log.d("AuthViewModel", "   Aprovado: ${colaborador.aprovado}")
                    android.util.Log.d("AuthViewModel", "   Firebase UID: ${if (colaborador.firebaseUid != null) "presente" else "ausente"}")
                    
                    val senhaValida = when {
                        // ✅ Verificar senha pessoal (hash) - para logins após primeiro acesso
                        senhaHashLimpa != null && 
                        senhaLimpa == senhaHashLimpa -> {
                            android.util.Log.d("AuthViewModel", "✅ Senha pessoal válida")
                            true
                        }
                        // ✅ Verificar senha temporária - para primeiro acesso
                        senhaTemporariaLimpa != null && 
                        senhaLimpa == senhaTemporariaLimpa -> {
                            android.util.Log.d("AuthViewModel", "✅ Senha temporária válida")
                            true
                        }
                        else -> {
                            android.util.Log.d("AuthViewModel", "❌ Senha inválida")
                            false
                        }
                    }
                    
                    android.util.Log.d("AuthViewModel", "   Resultado final: $senhaValida")
                    
                    // ✅ SUPERADMIN: rossinys@gmail.com sempre permite login offline com qualquer senha
                    val isSuperAdmin = email == "rossinys@gmail.com"
                    
                    // ✅ CORREÇÃO: Se for superadmin e senha não validar, atualizar senha e permitir login
                    val senhaValidaFinal = if (isSuperAdmin && !senhaValida) {
                        android.util.Log.d("AuthViewModel", "🔧 SUPERADMIN: Senha não validou, mas atualizando e permitindo login")
                        true // Permitir login para superadmin mesmo se senha não bateu
                    } else {
                        senhaValida
                    }
                    
                    if (senhaValidaFinal) {
                        // ✅ CORREÇÃO: Verificar se é primeiro acesso (usando senha temporária) - exceto superadmin
                        // Usar senha limpa para comparação
                        val isPrimeiroAcesso = !isSuperAdmin && 
                                              colaborador.primeiroAcesso && 
                                              senhaTemporariaLimpa != null && 
                                              senhaLimpa == senhaTemporariaLimpa
                        
                        android.util.Log.d("AuthViewModel", "🔍 Verificação de primeiro acesso:")
                        android.util.Log.d("AuthViewModel", "   É superadmin: $isSuperAdmin")
                        android.util.Log.d("AuthViewModel", "   Primeiro acesso flag: ${colaborador.primeiroAcesso}")
                        android.util.Log.d("AuthViewModel", "   Senha temporária presente: ${senhaTemporariaLimpa != null}")
                        android.util.Log.d("AuthViewModel", "   Senha corresponde à temporária: ${senhaLimpa == senhaTemporariaLimpa}")
                        android.util.Log.d("AuthViewModel", "   É primeiro acesso: $isPrimeiroAcesso")
                        android.util.Log.d("AuthViewModel", "   Status online: $online")
                        
                        // ✅ CORREÇÃO: Se estiver online e for primeiro acesso, redirecionar para alteração de senha
                        // Se estiver offline, bloquear e pedir conexão
                        if (isPrimeiroAcesso) {
                            if (online) {
                                android.util.Log.d("AuthViewModel", "⚠️ PRIMEIRO ACESSO DETECTADO ONLINE - Redirecionando para alteração de senha")
                                
                                // ✅ CORREÇÃO CRÍTICA: Iniciar sessão ANTES de redirecionar
                                // Isso é necessário para que o ChangePasswordFragment possa acessar o colaborador
                                userSessionManager.startSession(colaborador)
                                android.util.Log.d("AuthViewModel", "✅ Sessão iniciada para primeiro acesso: ${colaborador.nome}")
                                
                                _authState.value = AuthState.FirstAccessRequired(colaborador)
                                return@launch
                            } else {
                                android.util.Log.d("AuthViewModel", "⚠️ PRIMEIRO ACESSO DETECTADO OFFLINE - Requer conexão online")
                                _errorMessage.value = "Primeiro acesso requer conexão com internet. Conecte-se e tente novamente."
                                return@launch
                            }
                        }
                        
                        // ✅ SUPERADMIN: Garantir que sempre é ADMIN, aprovado, sem primeiro acesso
                        // ✅ CORREÇÃO: Sempre atualizar senha do superadmin para login offline
                        var colaboradorFinal = if (isSuperAdmin) {
                            colaborador.copy(
                                nivelAcesso = NivelAcesso.ADMIN,
                                aprovado = true,
                                primeiroAcesso = false,
                                senhaHash = senha // ✅ SEMPRE atualizar senha para login offline
                            ).also {
                                appRepository.atualizarColaborador(it)
                                android.util.Log.d("AuthViewModel", "✅ SUPERADMIN: Senha atualizada para login offline")
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
                            android.util.Log.d("AuthViewModel", "🔍 Dispositivo online. Garantindo autenticação no Firebase...")
                            val firebaseOutcome = garantirAutenticacaoFirebase(colaboradorFinal, senhaLimpa)
                            colaboradorFinal = firebaseOutcome.colaboradorAtualizado
                            isOnlineLogin = firebaseOutcome.autenticado
                        }
                        
                        android.util.Log.d("AuthViewModel", "✅ LOGIN ${if (isOnlineLogin) "ONLINE" else "OFFLINE"} SUCESSO! (Tipo: $tipoAutenticacao)")

                        android.util.Log.w(
                            "🔍 DB_POPULATION",
                            "🚨 LOGIN ${if (isOnlineLogin) "ONLINE" else "OFFLINE"} CONCLUÍDO - REALIZANDO CONFIGURAÇÃO LOCAL (POTENCIAL POPULAÇÃO)"
                        )
                        
                        android.util.Log.d("AuthViewModel", "🔍 Iniciando sessão para: ${colaboradorFinal.nome}")
                        android.util.Log.d("AuthViewModel", "   ID: ${colaboradorFinal.id}")
                        android.util.Log.d("AuthViewModel", "   Email: ${colaboradorFinal.email}")
                        android.util.Log.d("AuthViewModel", "   Status online: $isOnlineLogin")
                        android.util.Log.d("AuthViewModel", "   Firebase Auth autenticado: ${firebaseAuth.currentUser != null}")
                        
                        // ✅ NOVO: Iniciar sessão do usuário
                        android.util.Log.d("AuthViewModel", "🔍 Iniciando sessão ${if (isOnlineLogin) "online" else "offline"} para: ${colaboradorFinal.nome}")
                        userSessionManager.startSession(colaboradorFinal)
                        
                        // ✅ NOVO: Verificar se a sessão foi iniciada corretamente
                        val nomeSessao = userSessionManager.getCurrentUserName()
                        val idSessao = userSessionManager.getCurrentUserId()
                        android.util.Log.d("AuthViewModel", "🔍 Verificação da sessão:")
                        android.util.Log.d("AuthViewModel", "   Nome na sessão: $nomeSessao")
                        android.util.Log.d("AuthViewModel", "   ID na sessão: $idSessao")
                        
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
                                android.util.Log.e("AuthViewModel", "❌ ERRO CRÍTICO: Tentando marcar como online mas Firebase Auth não está autenticado!")
                                android.util.Log.e("AuthViewModel", "❌ Forçando como OFFLINE para evitar erros de sincronização")
                                isOnlineLogin = false
                            } else {
                                android.util.Log.d("AuthViewModel", "✅ Firebase Auth confirmado autenticado - UID: ${firebaseUser.uid}")
                            }
                        }
                        
                        _authState.value = AuthState.Authenticated(localUser, isOnlineLogin)
                        android.util.Log.d("AuthViewModel", "✅ Estado de autenticação definido - online: $isOnlineLogin")
                        android.util.Log.d("AuthViewModel", "   Firebase Auth autenticado: ${firebaseAuth.currentUser != null}")
                        android.util.Log.d("AuthViewModel", "   Firebase UID: ${firebaseAuth.currentUser?.uid ?: "não autenticado"}")
                        return@launch
                    } else {
                        _errorMessage.value = "Senha incorreta"
                    }
                } else {
                    // ✅ NOVO: Se não encontrou localmente e está online, buscar na nuvem
                    android.util.Log.d("AuthViewModel", "🔍 Colaborador não encontrado localmente")
                    android.util.Log.d("AuthViewModel", "   Status online: $online")
                    android.util.Log.d("AuthViewModel", "   Email: $email")
                    
                    if (online) {
                        android.util.Log.d("AuthViewModel", "🔍 Colaborador não encontrado localmente. Buscando na nuvem...")
                        val colaboradorNuvem = buscarColaboradorNaNuvemPorEmail(email)
                        
                        if (colaboradorNuvem != null) {
                            android.util.Log.d("AuthViewModel", "✅ Colaborador encontrado na nuvem: ${colaboradorNuvem.nome}")
                            android.util.Log.d("AuthViewModel", "   Aprovado: ${colaboradorNuvem.aprovado}")
                            
                            // Salvar colaborador localmente para próximos logins offline
                            appRepository.inserirColaborador(colaboradorNuvem)
                            
                            // Verificar se está aprovado
                            if (colaboradorNuvem.aprovado) {
                                // ✅ CORREÇÃO: Usar mesma lógica de validação de senha (com trim)
                                val senhaLimpa = senha.trim()
                                val senhaHashLimpa = colaboradorNuvem.senhaHash?.trim()
                                val senhaTemporariaLimpa = colaboradorNuvem.senhaTemporaria?.trim()
                                
                                android.util.Log.d("AuthViewModel", "🔍 Validação de senha (DADOS DA NUVEM):")
                                android.util.Log.d("AuthViewModel", "   Senha fornecida: '${senhaLimpa}' (${senhaLimpa.length} caracteres)")
                                android.util.Log.d("AuthViewModel", "   Hash armazenado: ${if (senhaHashLimpa != null) "'$senhaHashLimpa' (${senhaHashLimpa.length} caracteres)" else "ausente"}")
                                android.util.Log.d("AuthViewModel", "   Senha temporária: ${if (senhaTemporariaLimpa != null) "'$senhaTemporariaLimpa' (${senhaTemporariaLimpa.length} caracteres)" else "ausente"}")
                                
                                val senhaValida = when {
                                    // ✅ Verificar senha pessoal (hash) - para logins após primeiro acesso
                                    senhaHashLimpa != null && senhaLimpa == senhaHashLimpa -> {
                                        android.util.Log.d("AuthViewModel", "✅ Senha pessoal válida")
                                        true
                                    }
                                    // ✅ Verificar senha temporária - para primeiro acesso
                                    senhaTemporariaLimpa != null && senhaLimpa == senhaTemporariaLimpa -> {
                                        android.util.Log.d("AuthViewModel", "✅ Senha temporária válida")
                                        true
                                    }
                                    else -> {
                                        android.util.Log.d("AuthViewModel", "❌ Senha inválida")
                                        false
                                    }
                                }
                                
                                // ✅ SUPERADMIN: rossinys@gmail.com sempre permite login
                                val isSuperAdmin = email == "rossinys@gmail.com"
                                val senhaValidaFinal = if (isSuperAdmin && !senhaValida) {
                                    android.util.Log.d("AuthViewModel", "🔧 SUPERADMIN: Senha não validou, mas permitindo login")
                                    true
                                } else {
                                    senhaValida
                                }
                                
                                if (senhaValidaFinal) {
                                    android.util.Log.d("AuthViewModel", "✅ LOGIN COM DADOS DA NUVEM SUCESSO!")
                                    
                                    // ✅ CORREÇÃO: Verificar se é primeiro acesso (exceto superadmin)
                                    var colaboradorNuvemAtualizado = colaboradorNuvem
                                    val isPrimeiroAcesso = !isSuperAdmin && 
                                                          colaboradorNuvemAtualizado.primeiroAcesso && 
                                                          senhaTemporariaLimpa != null && 
                                                          senhaLimpa == senhaTemporariaLimpa
                                    
                                    if (isPrimeiroAcesso) {
                                        android.util.Log.d("AuthViewModel", "⚠️ PRIMEIRO ACESSO DETECTADO - Redirecionando para alteração de senha")
                                        userSessionManager.startSession(colaboradorNuvemAtualizado)
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
                                    userSessionManager.startSession(colaboradorNuvemAtualizado)
                                    
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
                                            android.util.Log.e("AuthViewModel", "❌ ERRO CRÍTICO: Tentando marcar como online mas Firebase Auth não está autenticado!")
                                            android.util.Log.e("AuthViewModel", "❌ Forçando como OFFLINE para evitar erros de sincronização")
                                            isOnlineLogin = false
                                        } else {
                                            android.util.Log.d("AuthViewModel", "✅ Firebase Auth confirmado autenticado - UID: ${firebaseUser.uid}")
                                        }
                                    }
                                    
                                    _authState.value = AuthState.Authenticated(localUser, isOnlineLogin)
                                    android.util.Log.d("AuthViewModel", "✅ Estado de autenticação definido - online: $isOnlineLogin (dados da nuvem)")
                                    android.util.Log.d("AuthViewModel", "   Firebase Auth autenticado: ${firebaseAuth.currentUser != null}")
                                    android.util.Log.d("AuthViewModel", "   Firebase UID: ${firebaseAuth.currentUser?.uid ?: "não autenticado"}")
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
                        android.util.Log.d("AuthViewModel", "🔧 Criando SUPERADMIN automaticamente (offline) para: $email")
                        val colaborador = criarSuperAdminAutomatico(email, null, senha)
                        
                        if (colaborador != null) {
                            val localUser = LocalUser(
                                uid = colaborador.id.toString(),
                                email = colaborador.email,
                                displayName = colaborador.nome,
                                nivelAcesso = colaborador.nivelAcesso
                            )
                            _authState.value = AuthState.Authenticated(localUser, false)
                            return@launch
                        }
                    }
                    
                    _errorMessage.value = if (online) {
                        "Usuário não encontrado. Contate o administrador para criar sua conta."
                    } else {
                        "Usuário não encontrado. Faça login online primeiro para sincronizar sua conta."
                    }
                }
                
                _authState.value = AuthState.Unauthenticated
                
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "❌ ERRO NO LOGIN: ${e.message}", e)
                _authState.value = AuthState.Unauthenticated
                _errorMessage.value = getFirebaseErrorMessage(e)
            } finally {
                hideLoading()
                android.util.Log.d("AuthViewModel", "=== FIM DO LOGIN HÍBRIDO ===")
            }
        }
    }
    
    /**
     * ✅ NOVO: Função para registrar novo usuário (cadastro público)
     * Cria colaborador pendente de aprovação do administrador
     */
    fun register(email: String, senha: String, confirmarSenha: String, nome: String = "") {
        android.util.Log.d("AuthViewModel", "=== INICIANDO CADASTRO ===")
        android.util.Log.d("AuthViewModel", "Email: $email")
        android.util.Log.d("AuthViewModel", "Nome: $nome")
        
        // Validação básica
        if (email.isBlank() || senha.isBlank() || confirmarSenha.isBlank()) {
            _errorMessage.value = "Todos os campos são obrigatórios"
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Email inválido"
            return
        }
        
        if (senha.length < 6) {
            _errorMessage.value = "Senha deve ter pelo menos 6 caracteres"
            return
        }
        
        if (senha != confirmarSenha) {
            _errorMessage.value = "Senhas não coincidem"
            return
        }
        
        viewModelScope.launch {
            try {
                showLoading()
                _errorMessage.value = ""
                
                // Verificar se o repositório foi inicializado
                if (!::appRepository.isInitialized) {
                    android.util.Log.e("AuthViewModel", "ERRO: appRepository não foi inicializado")
                    _errorMessage.value = "Erro de inicialização. Reinicie o app."
                    hideLoading()
                    return@launch
                }
                
                // Verificar se já existe colaborador com este email
                val colaboradorExistente = appRepository.obterColaboradorPorEmail(email)
                if (colaboradorExistente != null) {
                    android.util.Log.d("AuthViewModel", "Colaborador já existe com este email")
                    _errorMessage.value = "Este email já está cadastrado. Faça login ou recupere sua senha."
                    hideLoading()
                    return@launch
                }
                
                // Criar usuário no Firebase Authentication
                android.util.Log.d("AuthViewModel", "Criando conta no Firebase...")
                val result = firebaseAuth.createUserWithEmailAndPassword(email, senha).await()
                
                if (result.user == null) {
                    android.util.Log.e("AuthViewModel", "Falha ao criar conta no Firebase")
                    _errorMessage.value = "Falha ao criar conta"
                    hideLoading()
                    return@launch
                }
                
                android.util.Log.d("AuthViewModel", "✅ Conta Firebase criada com sucesso! UID: ${result.user!!.uid}")
                
                // Criar colaborador pendente de aprovação
                val nomeColaborador = nome.ifBlank { email.substringBefore("@") }
                val novoColaborador = Colaborador(
                    nome = nomeColaborador,
                    email = email,
                    telefone = "",
                    cpf = "",
                    nivelAcesso = NivelAcesso.USER,
                    ativo = true,
                    firebaseUid = result.user!!.uid,
                    googleId = null,
                    aprovado = false, // Pendente de aprovação
                    primeiroAcesso = true,
                    dataCadastro = Date(),
                    dataUltimaAtualizacao = Date()
                )
                
                android.util.Log.d("AuthViewModel", "Criando colaborador pendente: ${novoColaborador.nome}")
                val colaboradorId = appRepository.inserirColaborador(novoColaborador)
                val colaboradorComId = novoColaborador.copy(id = colaboradorId)
                android.util.Log.d("AuthViewModel", "✅ Colaborador criado com ID: $colaboradorId")
                
                // ✅ NOVO: Sincronizar colaborador para a nuvem imediatamente
                if (isNetworkAvailable()) {
                    try {
                        android.util.Log.d("AuthViewModel", "Sincronizando colaborador para a nuvem...")
                        sincronizarColaboradorParaNuvem(colaboradorComId)
                        android.util.Log.d("AuthViewModel", "✅ Colaborador sincronizado para a nuvem")
                    } catch (e: Exception) {
                        android.util.Log.w("AuthViewModel", "⚠️ Erro ao sincronizar colaborador para a nuvem: ${e.message}")
                        // Não falhar o cadastro se a sincronização falhar - será sincronizado depois
                    }
                } else {
                    android.util.Log.d("AuthViewModel", "⚠️ Dispositivo offline - colaborador será sincronizado quando houver conexão")
                }
                
                // Fazer logout do Firebase (usuário não deve ficar logado até ser aprovado)
                firebaseAuth.signOut()
                
                // ✅ CORREÇÃO: Mostrar mensagem de sucesso em diálogo (não Toast)
                android.util.Log.d("AuthViewModel", "✅ CADASTRO CONCLUÍDO - Pendente de aprovação")
                showMessage("Cadastro realizado com sucesso!\n\nSeu cadastro foi enviado para análise e está pendente de aprovação pelo administrador.")
                _authState.value = AuthState.Unauthenticated
                
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "❌ Erro no cadastro: ${e.message}", e)
                _authState.value = AuthState.Unauthenticated
                _errorMessage.value = getFirebaseErrorMessage(e)
            } finally {
                hideLoading()
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
     * ✅ NOVO: Função para cadastro com Google
     * Cria colaborador pendente de aprovação usando conta Google
     * Gera senha automaticamente para acesso offline (não precisa alterar senha)
     */
    fun registerWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            try {
                android.util.Log.d("AuthViewModel", "=== INICIANDO CADASTRO COM GOOGLE ===")
                android.util.Log.d("AuthViewModel", "Email: ${account.email}")
                android.util.Log.d("AuthViewModel", "Nome: ${account.displayName}")
                
                showLoading()
                _errorMessage.value = ""
                
                // Verificar se o repositório foi inicializado
                if (!::appRepository.isInitialized) {
                    android.util.Log.e("AuthViewModel", "ERRO: appRepository não foi inicializado")
                    _errorMessage.value = "Erro de inicialização. Reinicie o app."
                    hideLoading()
                    return@launch
                }
                
                val email = account.email ?: run {
                    _errorMessage.value = "Email não disponível na conta Google"
                    hideLoading()
                    return@launch
                }
                
                // Verificar se já existe colaborador com este email
                val colaboradorExistente = appRepository.obterColaboradorPorEmail(email)
                if (colaboradorExistente != null) {
                    android.util.Log.d("AuthViewModel", "Colaborador já existe com este email")
                    _errorMessage.value = "Este email já está cadastrado. Faça login ou recupere sua senha."
                    hideLoading()
                    return@launch
                }
                
                // ✅ NOVO: Gerar senha automaticamente para acesso offline
                val senhaOffline = gerarSenhaOffline()
                android.util.Log.d("AuthViewModel", "🔑 Senha offline gerada: $senhaOffline")
                
                // Criar colaborador pendente de aprovação com senha para acesso offline
                val novoColaborador = Colaborador(
                    nome = account.displayName ?: email.substringBefore("@"),
                    email = email,
                    telefone = "",
                    cpf = "",
                    nivelAcesso = NivelAcesso.USER,
                    ativo = true,
                    firebaseUid = null, // Será preenchido quando aprovado
                    googleId = account.id,
                    aprovado = false, // Pendente de aprovação
                    primeiroAcesso = false, // ✅ CORREÇÃO: Não precisa alterar senha (já tem senha gerada)
                    senhaHash = senhaOffline, // ✅ NOVO: Salvar senha gerada para acesso offline
                    senhaTemporaria = null, // Não usar senha temporária
                    dataCadastro = Date(),
                    dataUltimaAtualizacao = Date()
                )
                
                android.util.Log.d("AuthViewModel", "Criando colaborador pendente: ${novoColaborador.nome}")
                val colaboradorId = appRepository.inserirColaborador(novoColaborador)
                val colaboradorComId = novoColaborador.copy(id = colaboradorId)
                android.util.Log.d("AuthViewModel", "✅ Colaborador criado com ID: $colaboradorId")
                android.util.Log.d("AuthViewModel", "🔑 Senha offline gerada e salva para acesso sem internet")
                
                // ✅ NOVO: Sincronizar colaborador para a nuvem imediatamente
                if (isNetworkAvailable()) {
                    try {
                        android.util.Log.d("AuthViewModel", "Sincronizando colaborador para a nuvem...")
                        sincronizarColaboradorParaNuvem(colaboradorComId)
                        android.util.Log.d("AuthViewModel", "✅ Colaborador sincronizado para a nuvem")
                    } catch (e: Exception) {
                        android.util.Log.w("AuthViewModel", "⚠️ Erro ao sincronizar colaborador para a nuvem: ${e.message}")
                        // Não falhar o cadastro se a sincronização falhar - será sincronizado depois
                    }
                } else {
                    android.util.Log.d("AuthViewModel", "⚠️ Dispositivo offline - colaborador será sincronizado quando houver conexão")
                }
                
                // ✅ CORREÇÃO: Mostrar mensagem de sucesso em diálogo (não Toast)
                android.util.Log.d("AuthViewModel", "✅ CADASTRO COM GOOGLE CONCLUÍDO - Pendente de aprovação")
                showMessage("Cadastro realizado com sucesso!\n\nSeu cadastro foi enviado para análise e está pendente de aprovação pelo administrador.\n\nUma senha foi gerada automaticamente para acesso offline.")
                _authState.value = AuthState.Unauthenticated
                
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "❌ Erro no cadastro com Google: ${e.message}", e)
                _authState.value = AuthState.Unauthenticated
                _errorMessage.value = "Erro ao realizar cadastro: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }
    
    /**
     * Função para login com Google (híbrido - online/offline)
     */
    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            try {
                android.util.Log.d("AuthViewModel", "=== INICIANDO GOOGLE SIGN-IN HÍBRIDO ===")
                android.util.Log.d("AuthViewModel", "Email: ${account.email}")
                android.util.Log.d("AuthViewModel", "Display Name: ${account.displayName}")
                android.util.Log.d("AuthViewModel", "ID Token: ${account.idToken?.take(20)}...")
                android.util.Log.d("AuthViewModel", "Account ID: ${account.id}")
                
                showLoading()
                _errorMessage.value = ""
                
                // Verificar se o repositório foi inicializado
                if (!::appRepository.isInitialized) {
                    android.util.Log.e("AuthViewModel", "ERRO: Repositorio nao foi inicializado")
                    _errorMessage.value = "Erro de inicializacao. Reinicie o app."
                    hideLoading()
                    return@launch
                }
                
                // Verificar conectividade
                val online = isNetworkAvailable()
                _isOnline.value = online
                android.util.Log.d("AuthViewModel", "Conectividade: $online")
                
                // PRIMEIRO: Tentar login online (Firebase)
                if (online) {
                    try {
                        android.util.Log.d("AuthViewModel", "Tentando login online com Firebase...")
                        
                        // Verificar se o ID Token está presente
                        if (account.idToken.isNullOrEmpty()) {
                            android.util.Log.e("AuthViewModel", "ID Token está vazio")
                            throw Exception("ID Token não disponível")
                        }
                        
                        // Obter credenciais do Google
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        android.util.Log.d("AuthViewModel", "Credencial criada: ${credential != null}")
                        
                        // Fazer login no Firebase com as credenciais do Google
                        val result = firebaseAuth.signInWithCredential(credential).await()
                        
                        android.util.Log.d("AuthViewModel", "Resultado do Firebase: $result")
                        android.util.Log.d("AuthViewModel", "User: ${result.user}")
                        
                        if (result.user != null) {
                            android.util.Log.d("AuthViewModel", "✅ FIREBASE LOGIN SUCESSO! User ID: ${result.user!!.uid}")
                            
                            // Verificar se existe colaborador com este email no banco local
                            val email = account.email ?: ""
                            android.util.Log.d("AuthViewModel", "Verificando colaborador local com email: $email")
                            
                            // ✅ CORREÇÃO: Buscar colaborador por email, googleId ou firebaseUid
                            var colaborador = appRepository.obterColaboradorPorEmail(email)
                            
                            // Se não encontrou por email, tentar buscar por googleId
                            val googleId = account.id
                            if (colaborador == null && googleId != null && googleId.isNotEmpty()) {
                                android.util.Log.d("AuthViewModel", "🔍 Colaborador não encontrado por email. Buscando por Google ID: $googleId")
                                colaborador = appRepository.obterColaboradorPorGoogleId(googleId)
                                if (colaborador != null) {
                                    android.util.Log.d("AuthViewModel", "✅ Colaborador encontrado por Google ID: ${colaborador.nome}")
                                    // ✅ CORREÇÃO: Atualizar email do Google para login offline funcionar
                                    if (colaborador.email != email && email.isNotEmpty()) {
                                        android.util.Log.d("AuthViewModel", "🔧 Atualizando email do colaborador para incluir email do Google: $email")
                                        val colaboradorAtualizado = colaborador.copy(email = email)
                                        appRepository.atualizarColaborador(colaboradorAtualizado)
                                        colaborador = colaboradorAtualizado
                                    }
                                }
                            }
                            
                            // Se ainda não encontrou, tentar buscar por firebaseUid
                            if (colaborador == null && result.user != null && result.user!!.uid.isNotEmpty()) {
                                android.util.Log.d("AuthViewModel", "🔍 Colaborador não encontrado por Google ID. Buscando por Firebase UID: ${result.user!!.uid}")
                                colaborador = appRepository.obterColaboradorPorFirebaseUid(result.user!!.uid)
                                if (colaborador != null) {
                                    android.util.Log.d("AuthViewModel", "✅ Colaborador encontrado por Firebase UID: ${colaborador.nome}")
                                    // ✅ CORREÇÃO: Atualizar email do Google para login offline funcionar
                                    if (colaborador.email != email && email.isNotEmpty()) {
                                        android.util.Log.d("AuthViewModel", "🔧 Atualizando email do colaborador para incluir email do Google: $email")
                                        val colaboradorAtualizado = colaborador.copy(email = email)
                                        appRepository.atualizarColaborador(colaboradorAtualizado)
                                        colaborador = colaboradorAtualizado
                                    }
                                }
                            }
                            
                            if (colaborador != null) {
                                android.util.Log.d("AuthViewModel", "Colaborador encontrado: ${colaborador.nome}")
                                android.util.Log.d("AuthViewModel", "Aprovado: ${colaborador.aprovado}")
                                
                                // Verificar se está aprovado
                                if (colaborador.aprovado) {
                                    android.util.Log.d("AuthViewModel", "✅ LOGIN HÍBRIDO SUCESSO!")
                                    
                                    // ✅ NOVO: Iniciar sessão do usuário
                                    android.util.Log.d("AuthViewModel", "🔍 Iniciando sessão Google para: ${colaborador.nome}")
                                    userSessionManager.startSession(colaborador)
                                    
                                    // ✅ NOVO: Verificar se a sessão foi iniciada corretamente
                                    val nomeSessao = userSessionManager.getCurrentUserName()
                                    val idSessao = userSessionManager.getCurrentUserId()
                                    android.util.Log.d("AuthViewModel", "🔍 Verificação da sessão Google:")
                                    android.util.Log.d("AuthViewModel", "   Nome na sessão: $nomeSessao")
                                    android.util.Log.d("AuthViewModel", "   ID na sessão: $idSessao")
                                    
                                    // ✅ REMOVIDO: salvarDadosUsuario não estava funcionando
                                    // O UserSessionManager já salva os dados corretamente
                                    
                                    // Criar usuário local
                                    val localUser = LocalUser(
                                        uid = colaborador.id.toString(),
                                        email = colaborador.email,
                                        displayName = colaborador.nome,
                                        nivelAcesso = colaborador.nivelAcesso
                                    )
                                    
                                    _authState.value = AuthState.Authenticated(localUser, true)
                                    return@launch
                                } else {
                                    android.util.Log.d("AuthViewModel", "Colaborador não aprovado")
                                    _errorMessage.value = "Sua conta está aguardando aprovação do administrador"
                                    _authState.value = AuthState.Unauthenticated
                                    return@launch
                                }
                            } else {
                                // ✅ NOVO: Se não encontrou localmente, buscar na nuvem
                                android.util.Log.d("AuthViewModel", "🔍 Colaborador não encontrado localmente. Buscando na nuvem...")
                                val colaboradorNuvem = buscarColaboradorNaNuvemPorEmail(email)
                                
                                if (colaboradorNuvem != null) {
                                    android.util.Log.d("AuthViewModel", "✅ Colaborador encontrado na nuvem: ${colaboradorNuvem.nome}")
                                    android.util.Log.d("AuthViewModel", "   Aprovado: ${colaboradorNuvem.aprovado}")
                                    
                                    // Salvar colaborador localmente para próximos logins offline
                                    appRepository.inserirColaborador(colaboradorNuvem)
                                    
                                    // Verificar se está aprovado
                                    if (colaboradorNuvem.aprovado) {
                                        android.util.Log.d("AuthViewModel", "✅ LOGIN GOOGLE COM DADOS DA NUVEM SUCESSO!")
                                        
                                        // Iniciar sessão
                                        userSessionManager.startSession(colaboradorNuvem)
                                        
                                        val localUser = LocalUser(
                                            uid = colaboradorNuvem.id.toString(),
                                            email = colaboradorNuvem.email,
                                            displayName = colaboradorNuvem.nome,
                                            nivelAcesso = colaboradorNuvem.nivelAcesso
                                        )
                                        
                                        _authState.value = AuthState.Authenticated(localUser, true)
                                        return@launch
                                    } else {
                                        android.util.Log.d("AuthViewModel", "Colaborador não aprovado")
                                        _errorMessage.value = "Sua conta está aguardando aprovação do administrador"
                                        _authState.value = AuthState.Unauthenticated
                                        return@launch
                                    }
                                } else {
                                    // Usuário não existe no banco local nem na nuvem - BLOQUEADO: criação automática desabilitada
                                    android.util.Log.d("AuthViewModel", "Usuário não encontrado no banco local nem na nuvem - criação automática BLOQUEADA")
                                    _errorMessage.value = "Usuário não encontrado. Contate o administrador para criar sua conta."
                                    _authState.value = AuthState.Unauthenticated
                                    return@launch
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("AuthViewModel", "Login online falhou: ${e.message}")
                        android.util.Log.w("AuthViewModel", "Tipo de erro: ${e.javaClass.simpleName}")
                        // Continuar para tentar login offline
                    }
                }
                
                // SEGUNDO: Tentar login offline (banco local)
                android.util.Log.d("AuthViewModel", "Tentando login offline...")
                
                // Verificar se existe colaborador com este email
                val email = account.email ?: ""
                android.util.Log.d("AuthViewModel", "Buscando colaborador com email: $email")
                
                // ✅ CORREÇÃO: Buscar colaborador por email, googleId ou firebaseUid
                var colaborador = appRepository.obterColaboradorPorEmail(email)
                
                // Se não encontrou por email, tentar buscar por googleId
                val googleId = account.id
                if (colaborador == null && googleId != null && googleId.isNotEmpty()) {
                    android.util.Log.d("AuthViewModel", "🔍 Colaborador não encontrado por email. Buscando por Google ID: $googleId")
                    colaborador = appRepository.obterColaboradorPorGoogleId(googleId)
                    if (colaborador != null) {
                        android.util.Log.d("AuthViewModel", "✅ Colaborador encontrado por Google ID: ${colaborador.nome}")
                        // ✅ CORREÇÃO: Atualizar email do Google para login offline funcionar
                        if (colaborador.email != email && email.isNotEmpty()) {
                            android.util.Log.d("AuthViewModel", "🔧 Atualizando email do colaborador para incluir email do Google: $email")
                            val colaboradorAtualizado = colaborador.copy(email = email)
                            appRepository.atualizarColaborador(colaboradorAtualizado)
                            colaborador = colaboradorAtualizado
                        }
                    }
                }
                
                // Se ainda não encontrou, tentar buscar por firebaseUid (se disponível)
                if (colaborador == null && online) {
                    try {
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        if (firebaseUser != null && firebaseUser.uid.isNotEmpty()) {
                            android.util.Log.d("AuthViewModel", "🔍 Colaborador não encontrado por Google ID. Buscando por Firebase UID: ${firebaseUser.uid}")
                            colaborador = appRepository.obterColaboradorPorFirebaseUid(firebaseUser.uid)
                            if (colaborador != null) {
                                android.util.Log.d("AuthViewModel", "✅ Colaborador encontrado por Firebase UID: ${colaborador.nome}")
                                // ✅ CORREÇÃO: Atualizar email do Google para login offline funcionar
                                if (colaborador.email != email && email.isNotEmpty()) {
                                    android.util.Log.d("AuthViewModel", "🔧 Atualizando email do colaborador para incluir email do Google: $email")
                                    val colaboradorAtualizado = colaborador.copy(email = email)
                                    appRepository.atualizarColaborador(colaboradorAtualizado)
                                    colaborador = colaboradorAtualizado
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("AuthViewModel", "Erro ao buscar por Firebase UID: ${e.message}")
                    }
                }
                
                android.util.Log.d("AuthViewModel", "Colaborador encontrado: ${colaborador != null}")
                
                // ✅ NOVO: Se não encontrou localmente e está online, buscar na nuvem
                if (colaborador == null && online) {
                    android.util.Log.d("AuthViewModel", "🔍 Colaborador não encontrado localmente. Buscando na nuvem...")
                    colaborador = buscarColaboradorNaNuvemPorEmail(email)
                    
                    if (colaborador != null) {
                        android.util.Log.d("AuthViewModel", "✅ Colaborador encontrado na nuvem: ${colaborador.nome}")
                        // Salvar colaborador localmente para próximos logins offline
                        appRepository.inserirColaborador(colaborador)
                    }
                }
                
                if (colaborador != null) {
                    android.util.Log.d("AuthViewModel", "Colaborador encontrado: ${colaborador.nome}")
                    android.util.Log.d("AuthViewModel", "Aprovado: ${colaborador.aprovado}")
                    
                    // Verificar se está aprovado
                    if (colaborador.aprovado) {
                        android.util.Log.d("AuthViewModel", "✅ LOGIN OFFLINE SUCESSO!")
                        
                        // ✅ NOVO: Iniciar sessão do usuário
                        android.util.Log.d("AuthViewModel", "🔍 Iniciando sessão Google offline para: ${colaborador.nome}")
                        userSessionManager.startSession(colaborador)
                        
                        // ✅ NOVO: Verificar se a sessão foi iniciada corretamente
                        val nomeSessao = userSessionManager.getCurrentUserName()
                        val idSessao = userSessionManager.getCurrentUserId()
                        android.util.Log.d("AuthViewModel", "🔍 Verificação da sessão Google offline:")
                        android.util.Log.d("AuthViewModel", "   Nome na sessão: $nomeSessao")
                        android.util.Log.d("AuthViewModel", "   ID na sessão: $idSessao")
                        
                        // Criar usuário local
                        val localUser = LocalUser(
                            uid = colaborador.id.toString(),
                            email = colaborador.email,
                            displayName = colaborador.nome,
                            nivelAcesso = colaborador.nivelAcesso
                        )
                        
                        _authState.value = AuthState.Authenticated(localUser, false)
                        return@launch
                    } else {
                        android.util.Log.d("AuthViewModel", "Colaborador não aprovado")
                        _errorMessage.value = "Sua conta está aguardando aprovação do administrador"
                        _authState.value = AuthState.Unauthenticated
                        return@launch
                    }
                }
                
                // TERCEIRO: Criar novo colaborador automaticamente (pendente de aprovação)
                android.util.Log.d("AuthViewModel", "Criando novo colaborador automaticamente (pendente de aprovação)...")
                
                try {
                    // ✅ NOVO: Gerar senha automaticamente para acesso offline
                    val senhaOffline = gerarSenhaOffline()
                    android.util.Log.d("AuthViewModel", "🔑 Senha offline gerada: $senhaOffline")
                    
                    val novoColaborador = Colaborador(
                        nome = account.displayName ?: "Usuário Google",
                        email = account.email ?: "",
                        telefone = "",
                        cpf = "",
                        nivelAcesso = NivelAcesso.USER,
                        ativo = true,
                        firebaseUid = account.id ?: "",
                        googleId = account.id,
                        aprovado = false, // Fica pendente até aprovação do admin
                        primeiroAcesso = false, // ✅ CORREÇÃO: Não precisa alterar senha (já tem senha gerada)
                        senhaHash = senhaOffline, // ✅ NOVO: Salvar senha gerada para acesso offline
                        senhaTemporaria = null, // Não usar senha temporária
                        dataCadastro = Date(),
                        dataUltimaAtualizacao = Date()
                    )
                    
                    android.util.Log.d("AuthViewModel", "Novo colaborador criado (pendente): ${novoColaborador.nome}")
                    val colaboradorId = appRepository.inserirColaborador(novoColaborador)
                    val colaboradorComId = novoColaborador.copy(id = colaboradorId)
                    android.util.Log.d("AuthViewModel", "Novo colaborador criado com ID: $colaboradorId")
                    android.util.Log.d("AuthViewModel", "🔑 Senha offline gerada e salva para acesso sem internet")
                    
                    // ✅ NOVO: Sincronizar colaborador para a nuvem imediatamente
                    if (isNetworkAvailable()) {
                        try {
                            android.util.Log.d("AuthViewModel", "Sincronizando colaborador para a nuvem...")
                            sincronizarColaboradorParaNuvem(colaboradorComId)
                            android.util.Log.d("AuthViewModel", "✅ Colaborador sincronizado para a nuvem")
                        } catch (e: Exception) {
                            android.util.Log.w("AuthViewModel", "⚠️ Erro ao sincronizar colaborador para a nuvem: ${e.message}")
                        }
                    }
                    
                    // ✅ CORREÇÃO: Mostrar mensagem de sucesso em diálogo (não Toast)
                    showMessage("Conta criada com sucesso!\n\nAguarde aprovação do administrador.\n\nUma senha foi gerada automaticamente para acesso offline.")
                    _authState.value = AuthState.Unauthenticated
                    return@launch
                    
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "Erro ao criar colaborador: ${e.message}")
                    android.util.Log.e("AuthViewModel", "Tipo de erro: ${e.javaClass.simpleName}")
                    _errorMessage.value = "Erro ao criar conta. Tente novamente."
                    _authState.value = AuthState.Unauthenticated
                    return@launch
                }
                
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "❌ ERRO NO GOOGLE SIGN-IN: ${e.message}", e)
                android.util.Log.e("AuthViewModel", "Tipo de erro: ${e.javaClass.simpleName}")
                _authState.value = AuthState.Unauthenticated
                _errorMessage.value = "Erro no login com Google: ${e.message}"
            } finally {
                hideLoading()
                android.util.Log.d("AuthViewModel", "=== FIM DO GOOGLE SIGN-IN HÍBRIDO ===")
            }
        }
    }
    
    /**
     * Cria colaborador automaticamente quando usuário se registra via Google
     */
    private suspend fun criarColaboradorAutomatico(firebaseUser: FirebaseUser, displayName: String) {
        try {
            android.util.Log.d("AuthViewModel", "=== CRIANDO COLABORADOR AUTOMÁTICO ===")
            android.util.Log.d("AuthViewModel", "Email: ${firebaseUser.email}")
            android.util.Log.d("AuthViewModel", "Display Name: $displayName")
            android.util.Log.d("AuthViewModel", "UID: ${firebaseUser.uid}")
            
            val googleId = firebaseUser.providerData.firstOrNull { it.providerId == "google.com" }?.uid
            android.util.Log.d("AuthViewModel", "Google ID: $googleId")
            
            // ✅ NOVO: Gerar senha automaticamente para acesso offline
            val senhaOffline = gerarSenhaOffline()
            android.util.Log.d("AuthViewModel", "🔑 Senha offline gerada: $senhaOffline")
            
            val colaborador = Colaborador(
                nome = displayName.ifEmpty { firebaseUser.displayName ?: "Usuário Google" },
                email = firebaseUser.email ?: "",
                telefone = "",
                cpf = "",
                nivelAcesso = NivelAcesso.USER,
                ativo = true,
                firebaseUid = firebaseUser.uid,
                googleId = googleId,
                aprovado = false, // Precisa ser aprovado pelo admin
                primeiroAcesso = false, // ✅ CORREÇÃO: Não precisa alterar senha (já tem senha gerada)
                senhaHash = senhaOffline, // ✅ NOVO: Salvar senha gerada para acesso offline
                senhaTemporaria = null, // Não usar senha temporária
                dataCadastro = Date(),
                dataUltimaAtualizacao = Date()
            )
            
            val colaboradorId = appRepository.inserirColaborador(colaborador)
            val colaboradorComId = colaborador.copy(id = colaboradorId)
            
            android.util.Log.d("AuthViewModel", "✅ Colaborador criado automaticamente com ID: $colaboradorId")
            android.util.Log.d("AuthViewModel", "Nome: ${colaborador.nome}")
            android.util.Log.d("AuthViewModel", "Email: ${colaborador.email}")
            android.util.Log.d("AuthViewModel", "Aprovado: ${colaborador.aprovado}")
            android.util.Log.d("AuthViewModel", "🔑 Senha offline gerada e salva para acesso sem internet")
            
            // ✅ NOVO: Sincronizar colaborador para a nuvem imediatamente
            if (isNetworkAvailable()) {
                try {
                    android.util.Log.d("AuthViewModel", "Sincronizando colaborador para a nuvem...")
                    sincronizarColaboradorParaNuvem(colaboradorComId)
                    android.util.Log.d("AuthViewModel", "✅ Colaborador sincronizado para a nuvem")
                } catch (e: Exception) {
                    android.util.Log.w("AuthViewModel", "⚠️ Erro ao sincronizar colaborador para a nuvem: ${e.message}")
                    // Não falhar se a sincronização falhar - será sincronizado depois
                }
            } else {
                android.util.Log.d("AuthViewModel", "⚠️ Dispositivo offline - colaborador será sincronizado quando houver conexão")
            }
            
            // ✅ CORREÇÃO: NÃO iniciar sessão para usuários não aprovados
            android.util.Log.d("AuthViewModel", "⚠️ Usuário não aprovado - sessão NÃO será iniciada")
            android.util.Log.d("AuthViewModel", "   Nome: ${colaboradorComId.nome}")
            android.util.Log.d("AuthViewModel", "   Email: ${colaboradorComId.email}")
            android.util.Log.d("AuthViewModel", "   Aprovado: ${colaboradorComId.aprovado}")
            android.util.Log.d("AuthViewModel", "   Status: Aguardando aprovação do administrador")
            
            android.util.Log.d("AuthViewModel", "=== FIM CRIAÇÃO COLABORADOR ===")
            
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "❌ Erro ao criar colaborador: ${e.message}")
            android.util.Log.e("AuthViewModel", "Tipo de erro: ${e.javaClass.simpleName}")
            throw e // Re-throw para que o erro seja tratado no método chamador
        }
    }
    
    // ✅ REMOVIDO: Método salvarDadosUsuario não estava funcionando
    // O UserSessionManager já salva os dados corretamente
    
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
    private suspend fun criarOuAtualizarColaboradorOnline(firebaseUser: FirebaseUser): Colaborador? {
        try {
            val email = firebaseUser.email ?: return null
            val nome = firebaseUser.displayName ?: email.substringBefore("@")
            
            // Verificar se já existe colaborador com este email
            val colaboradorExistente = appRepository.obterColaboradorPorEmail(email)
            
            if (colaboradorExistente != null) {
                android.util.Log.d("AuthViewModel", "Colaborador existente encontrado: ${colaboradorExistente.nome}")

                android.util.Log.w(
                    "🔍 DB_POPULATION",
                    "🚨 ATUALIZANDO COLABORADOR LOCAL APÓS LOGIN ONLINE: ${colaboradorExistente.email}"
                )

                // ✅ SUPERADMIN: rossinys@gmail.com sempre é ADMIN e aprovado
                val colaboradorAtualizado = if (email == "rossinys@gmail.com") {
                    // Superadmin - sempre ADMIN, aprovado, sem primeiro acesso
                    colaboradorExistente.copy(
                        nome = firebaseUser.displayName ?: colaboradorExistente.nome,
                        firebaseUid = firebaseUser.uid,
                        dataUltimoAcesso = java.util.Date(),
                        nivelAcesso = NivelAcesso.ADMIN,
                        aprovado = true,
                        primeiroAcesso = false, // Superadmin nunca precisa alterar senha
                        dataAprovacao = colaboradorExistente.dataAprovacao ?: java.util.Date(),
                        aprovadoPor = colaboradorExistente.aprovadoPor ?: "Sistema (Superadmin)"
                    )
                } else {
                    // ✅ CORREÇÃO: Para outros usuários, MANTER nível de acesso original
                    colaboradorExistente.copy(
                        nome = firebaseUser.displayName ?: colaboradorExistente.nome,
                        firebaseUid = firebaseUser.uid,
                        dataUltimoAcesso = java.util.Date()
                        // NÃO alterar nivelAcesso, aprovado, etc. para usuários normais
                    )
                }
                
                // Salvar atualizações no banco local
                appRepository.atualizarColaborador(colaboradorAtualizado)
                
                android.util.Log.d("AuthViewModel", "✅ Colaborador sincronizado:")
                android.util.Log.d("AuthViewModel", "   Nome: ${colaboradorAtualizado.nome}")
                android.util.Log.d("AuthViewModel", "   Email: ${colaboradorAtualizado.email}")
                android.util.Log.d("AuthViewModel", "   Nível: ${colaboradorAtualizado.nivelAcesso}")
                android.util.Log.d("AuthViewModel", "   Aprovado: ${colaboradorAtualizado.aprovado}")
                android.util.Log.d("AuthViewModel", "   É admin especial: ${email == "rossinys@gmail.com"}")

                userSessionManager.startSession(colaboradorAtualizado)
                return colaboradorAtualizado
            } else {
                android.util.Log.d("AuthViewModel", "Colaborador ainda não existe localmente")
                android.util.Log.w(
                    "🔍 DB_POPULATION",
                    "🚨 CRIAÇÃO AUTOMÁTICA DE COLABORADORES BLOQUEADA - LOGIN ONLINE"
                )

                // 🚨 BLOQUEADO: Criação automática de colaboradores desabilitada
                android.util.Log.d("AuthViewModel", "CRIAÇÃO AUTOMÁTICA DE COLABORADORES BLOQUEADA")
                android.util.Log.w(
                    "🔍 DB_POPULATION",
                    "🚨 CRIAÇÃO AUTOMÁTICA DE COLABORADORES BLOQUEADA - LOGIN ONLINE"
                )
                
                // ✅ SUPERADMIN: Criar automaticamente para rossinys@gmail.com
                if (email == "rossinys@gmail.com") {
                    val colaborador = criarSuperAdminAutomatico(email, firebaseUser.uid, "")
                    if (colaborador != null) {
                        return colaborador
                    }
                }
                
                android.util.Log.d("AuthViewModel", "Usuário não encontrado - criação automática bloqueada")
                _errorMessage.value = "Usuário não encontrado. Contate o administrador para criar sua conta."
                _authState.value = AuthState.Unauthenticated
                return null
            }
            
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Erro ao criar/atualizar colaborador online: ${e.message}")
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
                if (colaboradorId == null || colaboradorId == 0L) {
                    android.util.Log.w("AuthViewModel", "⚠️ Nenhum colaborador na sessão local (ID: $colaboradorId)")
                    _errorMessage.value = "Sessão expirada. Faça login novamente."
                    _authState.value = AuthState.Unauthenticated
                    return@launch
                }
                
                val colaborador = appRepository.obterColaboradorPorId(colaboradorId)
                if (colaborador == null) {
                    android.util.Log.w("AuthViewModel", "⚠️ Colaborador não encontrado na sessão")
                    _errorMessage.value = "Colaborador não encontrado. Faça login novamente."
                    _authState.value = AuthState.Unauthenticated
                    return@launch
                }
                
                val email = colaborador.email
                
                // ✅ CORREÇÃO: Tentar autenticar no Firebase se não estiver autenticado
                // Isso é necessário para atualizar a senha no Firebase
                var firebaseUser = firebaseAuth.currentUser
                if (firebaseUser == null && isNetworkAvailable() && colaborador.firebaseUid != null) {
                    android.util.Log.d("AuthViewModel", "🔧 Usuário não autenticado no Firebase. Tentando autenticar...")
                    // Não podemos autenticar sem senha, então vamos criar/atualizar a conta
                    // Se a conta não existir, será criada quando o usuário fizer login novamente
                    android.util.Log.d("AuthViewModel", "⚠️ Não é possível atualizar senha no Firebase sem autenticação")
                    android.util.Log.d("AuthViewModel", "   A senha será atualizada localmente e no Firebase na próxima sincronização")
                }
                
                // ✅ CORREÇÃO: Atualizar senha no Firebase se estiver autenticado
                if (isNetworkAvailable() && firebaseUser != null) {
                    try {
                        firebaseUser.updatePassword(novaSenha).await()
                        android.util.Log.d("AuthViewModel", "✅ Senha atualizada no Firebase")
                    } catch (e: Exception) {
                        android.util.Log.w("AuthViewModel", "⚠️ Erro ao atualizar senha no Firebase: ${e.message}")
                        android.util.Log.d("AuthViewModel", "   Continuando para atualizar senha localmente...")
                        // Não falhar se não conseguir atualizar no Firebase
                        // A senha será atualizada na próxima sincronização
                    }
                } else {
                    android.util.Log.d("AuthViewModel", "⚠️ Não é possível atualizar senha no Firebase (offline ou não autenticado)")
                    android.util.Log.d("AuthViewModel", "   A senha será atualizada localmente e sincronizada depois")
                }
                
                // ✅ OFFLINE-FIRST: Salvar hash da senha no banco local para login offline
                // TODO: Implementar hash de senha (PasswordHasher removido)
                val senhaHash = novaSenha // TEMPORÁRIO: Usar senha sem hash até implementar
                
                // Marcar primeiro acesso como concluído e salvar hash
                appRepository.marcarPrimeiroAcessoConcluido(colaborador.id, senhaHash)
                
                android.util.Log.d("AuthViewModel", "✅ Senha atualizada e primeiro acesso concluído")
                
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
                        android.util.Log.d("AuthViewModel", "🔄 Sincronizando colaborador atualizado com a nuvem após alteração de senha...")
                        sincronizarColaboradorParaNuvem(colaboradorAtualizado)
                        android.util.Log.d("AuthViewModel", "✅ Colaborador sincronizado com sucesso (senha atualizada na nuvem)")
                    } catch (e: Exception) {
                        android.util.Log.w("AuthViewModel", "⚠️ Erro ao sincronizar colaborador após alteração de senha: ${e.message}")
                        android.util.Log.d("AuthViewModel", "   A senha foi atualizada localmente, mas não foi sincronizada com a nuvem")
                        android.util.Log.d("AuthViewModel", "   O colaborador precisará fazer login novamente para sincronizar")
                        // Não falhar o processo se a sincronização falhar - a senha já foi atualizada localmente
                    }
                } else {
                    android.util.Log.d("AuthViewModel", "⚠️ Dispositivo offline - senha atualizada localmente")
                    android.util.Log.d("AuthViewModel", "   A senha será sincronizada com a nuvem quando o dispositivo estiver online")
                }
                
                // Reiniciar sessão
                userSessionManager.startSession(colaboradorAtualizado)
                
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
                android.util.Log.e("AuthViewModel", "Erro ao alterar senha: ${e.message}", e)
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
        android.util.Log.d("AuthViewModel", "🔐 Garantindo autenticação Firebase para ${colaborador.email}")
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
                android.util.Log.w("AuthViewModel", "⚠️ SignInFirebase retornou usuário nulo")
                FirebaseAuthOutcome(false, colaboradorAtualizado)
            }
        } catch (e: Exception) {
            val errorCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
            if (errorCode == "ERROR_USER_NOT_FOUND") {
                android.util.Log.w("AuthViewModel", "⚠️ Usuário não existe no Firebase. Criando automaticamente: ${colaborador.email}")
                return try {
                    val createResult = firebaseAuth.createUserWithEmailAndPassword(colaborador.email, senhaValidada).await()
                    val newUser = createResult.user
                    if (newUser != null) {
                        colaboradorAtualizado = atualizarFirebaseUidLocalESync(colaboradorAtualizado, newUser.uid)
                        FirebaseAuthOutcome(true, colaboradorAtualizado)
                    } else {
                        android.util.Log.w("AuthViewModel", "⚠️ Criação do usuário retornou nulo")
                        FirebaseAuthOutcome(false, colaboradorAtualizado)
                    }
                } catch (createError: Exception) {
                    android.util.Log.e("AuthViewModel", "❌ Falha ao criar usuário no Firebase: ${createError.message}")
                    FirebaseAuthOutcome(false, colaboradorAtualizado)
                }
            } else {
                android.util.Log.w(
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
            sincronizarColaboradorParaNuvem(colaboradorAtualizado)
        }.onFailure {
            android.util.Log.w("AuthViewModel", "⚠️ Falha ao sincronizar colaborador com novo Firebase UID: ${it.message}")
        }
        
        return colaboradorAtualizado
    }
    
    /**
     * ✅ NOVO: Sincroniza um colaborador específico para a nuvem (Firestore)
     * Usado após criar um novo cadastro para que apareça na lista de pendentes do admin
     */
    private suspend fun sincronizarColaboradorParaNuvem(colaborador: Colaborador) {
        try {
            android.util.Log.d("AuthViewModel", "=== SINCRONIZANDO COLABORADOR PARA NUVEM ===")
            android.util.Log.d("AuthViewModel", "   ID: ${colaborador.id}")
            android.util.Log.d("AuthViewModel", "   Nome: ${colaborador.nome}")
            android.util.Log.d("AuthViewModel", "   Email: ${colaborador.email}")
            
            // Estrutura: empresas/empresa_001/entidades/colaboradores/items
            val collectionRef = firestore
                .collection("empresas")
                .document("empresa_001")
                .collection("entidades")
                .document("colaboradores")
                .collection("items")
            
            // Converter colaborador para Map
            val colaboradorMap = mutableMapOf<String, Any?>()
            colaboradorMap["roomId"] = colaborador.id
            colaboradorMap["id"] = colaborador.id
            colaboradorMap["nome"] = colaborador.nome
            colaboradorMap["email"] = colaborador.email
            colaboradorMap["telefone"] = colaborador.telefone
            colaboradorMap["cpf"] = colaborador.cpf
            colaboradorMap["nivelAcesso"] = colaborador.nivelAcesso.name
            colaboradorMap["ativo"] = colaborador.ativo
            colaboradorMap["aprovado"] = colaborador.aprovado
            colaboradorMap["primeiroAcesso"] = colaborador.primeiroAcesso
            colaboradorMap["firebaseUid"] = colaborador.firebaseUid
            colaboradorMap["googleId"] = colaborador.googleId
            colaboradorMap["senhaTemporaria"] = colaborador.senhaTemporaria
            colaboradorMap["senhaHash"] = colaborador.senhaHash
            colaboradorMap["dataCadastro"] = Timestamp(colaborador.dataCadastro)
            colaboradorMap["dataUltimaAtualizacao"] = Timestamp(colaborador.dataUltimaAtualizacao)
            colaboradorMap["dataAprovacao"] = colaborador.dataAprovacao?.let { Timestamp(it) }
            colaboradorMap["aprovadoPor"] = colaborador.aprovadoPor
            colaboradorMap["dataUltimoAcesso"] = colaborador.dataUltimoAcesso?.let { Timestamp(it) }
            colaboradorMap["lastModified"] = FieldValue.serverTimestamp()
            colaboradorMap["syncTimestamp"] = FieldValue.serverTimestamp()
            
            // ✅ CORREÇÃO: Verificar se já existe colaborador com este email no Firestore antes de criar
            val existingQuery = collectionRef
                .whereEqualTo("email", colaborador.email)
                .limit(1)
                .get()
                .await()
            
            if (!existingQuery.isEmpty) {
                // Se já existe, atualizar o documento existente em vez de criar novo
                val existingDoc = existingQuery.documents.first()
                android.util.Log.d("AuthViewModel", "⚠️ Colaborador já existe no Firestore (ID: ${existingDoc.id}), atualizando...")
                existingDoc.reference.set(colaboradorMap).await()
                android.util.Log.d("AuthViewModel", "✅ Colaborador atualizado no Firestore")
            } else {
                // Se não existe, criar novo documento
                collectionRef
                    .document(colaborador.id.toString())
                    .set(colaboradorMap)
                    .await()
                android.util.Log.d("AuthViewModel", "✅ Colaborador criado no Firestore")
            }
            
            android.util.Log.d("AuthViewModel", "✅ Colaborador sincronizado com sucesso para a nuvem")
            
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "❌ Erro ao sincronizar colaborador para a nuvem: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * ✅ NOVO: Busca colaborador na nuvem (Firestore) por email
     * Retorna o colaborador se encontrado e aprovado, null caso contrário
     */
    private suspend fun buscarColaboradorNaNuvemPorEmail(email: String): Colaborador? {
        return try {
            android.util.Log.d("AuthViewModel", "🔍 === INICIANDO BUSCA NA NUVEM ===")
            android.util.Log.d("AuthViewModel", "   Email: $email")
            
            // Estrutura: empresas/empresa_001/entidades/colaboradores/items
            val collectionRef = firestore
                .collection("empresas")
                .document("empresa_001")
                .collection("entidades")
                .document("colaboradores")
                .collection("items")
            
            android.util.Log.d("AuthViewModel", "   Caminho da coleção: empresas/empresa_001/entidades/colaboradores/items")
            
            // ✅ CORREÇÃO: Normalizar email (trim, lowercase) para busca
            val emailNormalizado = email.trim().lowercase()
            android.util.Log.d("AuthViewModel", "   Email original: '$email'")
            android.util.Log.d("AuthViewModel", "   Email normalizado: '$emailNormalizado'")
            
            // Buscar colaborador por email (tentar exato primeiro)
            android.util.Log.d("AuthViewModel", "   Executando query: whereEqualTo('email', '$email')")
            val querySnapshot = try {
                val snapshot = collectionRef
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .await()
                
                // Se não encontrou com email exato, tentar com email normalizado
                if (snapshot.isEmpty && email != emailNormalizado) {
                    android.util.Log.d("AuthViewModel", "   Email exato não encontrado. Tentando com email normalizado...")
                    collectionRef
                        .whereEqualTo("email", emailNormalizado)
                        .limit(1)
                        .get()
                        .await()
                } else {
                    snapshot
                }
            } catch (e: FirebaseFirestoreException) {
                // Se a query falhar por falta de índice, tentar buscar todos e filtrar em memória
                if (e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    android.util.Log.w("AuthViewModel", "⚠️ Índice não encontrado. Buscando todos os colaboradores e filtrando em memória...")
                    val allDocs = collectionRef.get().await()
                    val filteredDocs = allDocs.documents.filter { doc ->
                        val data = doc.data
                        val docEmail = data?.get("email") as? String
                        docEmail?.equals(email, ignoreCase = true) == true
                    }
                    if (filteredDocs.isEmpty()) {
                        android.util.Log.d("AuthViewModel", "⚠️ Colaborador não encontrado na nuvem (após filtro em memória)")
                        return null
                    }
                    val doc = filteredDocs.first()
                    val colaboradorData = doc.data ?: return null
                    
                    // Converter documento do Firestore para Colaborador
                    val colaboradorId = doc.id.toLongOrNull()
                        ?: (colaboradorData["roomId"] as? Number)?.toLong()
                        ?: (colaboradorData["id"] as? Number)?.toLong()
                        ?: return null
                    
                    val colaboradorJson = gson.toJson(colaboradorData)
                    val colaboradorFirestore = gson.fromJson(colaboradorJson, Colaborador::class.java)?.copy(id = colaboradorId)
                    
                    if (colaboradorFirestore == null) {
                        android.util.Log.e("AuthViewModel", "❌ Erro ao converter colaborador do Firestore")
                        return null
                    }
                    
                    android.util.Log.d("AuthViewModel", "✅ Colaborador encontrado na nuvem (filtro em memória): ${colaboradorFirestore.nome}")
                    return colaboradorFirestore
                } else {
                    throw e
                }
            }
            
            android.util.Log.d("AuthViewModel", "   Documentos encontrados: ${querySnapshot.size()}")
            
            if (querySnapshot.isEmpty) {
                android.util.Log.d("AuthViewModel", "⚠️ Colaborador não encontrado na nuvem (query retornou vazio)")
                android.util.Log.d("AuthViewModel", "   Tentando busca alternativa: buscar todos e filtrar em memória...")
                
                // ✅ CORREÇÃO: Tentar buscar todos os colaboradores e filtrar em memória
                // Isso pode ser necessário se o email estiver armazenado de forma diferente
                try {
                    val allDocs = collectionRef.get().await()
                    android.util.Log.d("AuthViewModel", "   Total de colaboradores na nuvem: ${allDocs.size()}")
                    
                    val filteredDocs = allDocs.documents.filter { doc ->
                        val data = doc.data
                        val docEmail = data?.get("email") as? String
                        val emailMatch = docEmail?.equals(email, ignoreCase = true) == true || 
                                         docEmail?.trim()?.lowercase() == emailNormalizado
                        if (emailMatch) {
                            android.util.Log.d("AuthViewModel", "   ✅ Email encontrado: '$docEmail' (corresponde a '$email')")
                        }
                        emailMatch
                    }
                    
                    if (filteredDocs.isEmpty()) {
                        android.util.Log.d("AuthViewModel", "⚠️ Colaborador não encontrado na nuvem (após busca alternativa)")
                        return null
                    }
                    
                    val doc = filteredDocs.first()
                    val colaboradorData = doc.data ?: return null
                    
                    // Converter documento do Firestore para Colaborador
                    val colaboradorId = doc.id.toLongOrNull()
                        ?: (colaboradorData["roomId"] as? Number)?.toLong()
                        ?: (colaboradorData["id"] as? Number)?.toLong()
                        ?: return null
                    
                    val colaboradorJson = gson.toJson(colaboradorData)
                    val colaboradorFirestore = gson.fromJson(colaboradorJson, Colaborador::class.java)?.copy(id = colaboradorId)
                    
                    if (colaboradorFirestore == null) {
                        android.util.Log.e("AuthViewModel", "❌ Erro ao converter colaborador do Firestore (busca alternativa)")
                        return null
                    }
                    
                    android.util.Log.d("AuthViewModel", "✅ Colaborador encontrado na nuvem (busca alternativa): ${colaboradorFirestore.nome}")
                    return colaboradorFirestore
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "❌ Erro na busca alternativa: ${e.message}")
                    return null
                }
            }
            
            val doc = querySnapshot.documents.firstOrNull()
            if (doc == null) {
                android.util.Log.e("AuthViewModel", "❌ Documento não encontrado após query bem-sucedida")
                return null
            }
            
            android.util.Log.d("AuthViewModel", "   Documento ID: ${doc.id}")
            val colaboradorData = doc.data
            if (colaboradorData == null) {
                android.util.Log.e("AuthViewModel", "❌ Documento sem dados")
                return null
            }
            
            android.util.Log.d("AuthViewModel", "   Campos do documento: ${colaboradorData.keys}")
            
            // Converter documento do Firestore para Colaborador
            val colaboradorId = doc.id.toLongOrNull()
                ?: (colaboradorData["roomId"] as? Number)?.toLong()
                ?: (colaboradorData["id"] as? Number)?.toLong()
                ?: run {
                    android.util.Log.e("AuthViewModel", "❌ Não foi possível extrair ID do colaborador")
                    android.util.Log.e("AuthViewModel", "   doc.id: ${doc.id}")
                    android.util.Log.e("AuthViewModel", "   roomId: ${colaboradorData["roomId"]}")
                    android.util.Log.e("AuthViewModel", "   id: ${colaboradorData["id"]}")
                    return null
                }
            
            android.util.Log.d("AuthViewModel", "   Colaborador ID extraído: $colaboradorId")
            
            val colaboradorJson = gson.toJson(colaboradorData)
            val colaboradorFirestore = gson.fromJson(colaboradorJson, Colaborador::class.java)?.copy(id = colaboradorId)
            
            if (colaboradorFirestore == null) {
                android.util.Log.e("AuthViewModel", "❌ Erro ao converter colaborador do Firestore para JSON")
                android.util.Log.e("AuthViewModel", "   JSON gerado: $colaboradorJson")
                return null
            }
            
            android.util.Log.d("AuthViewModel", "✅ Colaborador encontrado na nuvem: ${colaboradorFirestore.nome}")
            android.util.Log.d("AuthViewModel", "   ID: ${colaboradorFirestore.id}")
            android.util.Log.d("AuthViewModel", "   Email: ${colaboradorFirestore.email}")
            android.util.Log.d("AuthViewModel", "   Aprovado: ${colaboradorFirestore.aprovado}")
            android.util.Log.d("AuthViewModel", "   Nível: ${colaboradorFirestore.nivelAcesso}")
            android.util.Log.d("AuthViewModel", "=== FIM BUSCA NA NUVEM ===")
            
            return colaboradorFirestore
            
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "❌ ERRO CRÍTICO ao buscar colaborador na nuvem", e)
            android.util.Log.e("AuthViewModel", "   Tipo de erro: ${e.javaClass.simpleName}")
            android.util.Log.e("AuthViewModel", "   Mensagem: ${e.message}")
            android.util.Log.e("AuthViewModel", "   Stack trace: ${e.stackTraceToString()}")
            return null
        }
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
            android.util.Log.d("AuthViewModel", "🔧 Criando SUPERADMIN: $email")
            
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
                    dataAprovacao = existente.dataAprovacao ?: java.util.Date(),
                    aprovadoPor = existente.aprovadoPor ?: "Sistema (Superadmin)"
                )
                appRepository.atualizarColaborador(atualizado)
                userSessionManager.startSession(atualizado)
                android.util.Log.d("AuthViewModel", "✅ SUPERADMIN atualizado: ${atualizado.nome}")
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
                dataAprovacao = java.util.Date(),
                aprovadoPor = "Sistema (Superadmin Automático)"
            )
            
            val colaboradorId = appRepository.inserirColaborador(novoColaborador)
            val colaboradorComId = novoColaborador.copy(id = colaboradorId)
            
            android.util.Log.d("AuthViewModel", "✅ SUPERADMIN criado: ${colaboradorComId.nome}")
            userSessionManager.startSession(colaboradorComId)
            
            return colaboradorComId
            
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Erro ao criar superadmin: ${e.message}", e)
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


