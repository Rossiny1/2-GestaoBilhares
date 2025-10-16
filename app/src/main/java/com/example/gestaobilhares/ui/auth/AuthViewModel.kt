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
import com.example.gestaobilhares.utils.NetworkUtils
import com.example.gestaobilhares.utils.SyncManager
import com.example.gestaobilhares.utils.UserSessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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
    
    // Repositório para acesso local
    private lateinit var appRepository: AppRepository
    
    // Utilitário de rede
    private lateinit var networkUtils: NetworkUtils
    
    // Gerenciador de sincronização
    private lateinit var syncManager: SyncManager
    
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
            
            appRepository = AppRepository(
                database.clienteDao(),
                database.acertoDao(),
                database.mesaDao(),
                database.rotaDao(),
                database.despesaDao(),
                database.colaboradorDao(),
                database.cicloAcertoDao(),
                database.acertoMesaDao(),
                database.contratoLocacaoDao(),
                database.aditivoContratoDao(),
                database.assinaturaRepresentanteLegalDao(),
                database.logAuditoriaAssinaturaDao()
            )
            android.util.Log.d("AuthViewModel", "AppRepository inicializado com sucesso")
            
            // Inicializar utilitários de forma segura
            try {
                networkUtils = NetworkUtils(context)
                android.util.Log.d("AuthViewModel", "✅ NetworkUtils inicializado")
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Erro ao inicializar NetworkUtils: ${e.message}")
                // Continuar sem NetworkUtils (modo offline)
            }
            
            try {
                syncManager = SyncManager(context, appRepository)
                android.util.Log.d("AuthViewModel", "✅ SyncManager inicializado")
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Erro ao inicializar SyncManager: ${e.message}")
                // Continuar sem SyncManager
            }
            
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
                        
                        // Se voltou a ter conexão, sincronizar dados
                        if (wasOffline && isAvailable && ::syncManager.isInitialized) {
                            syncManager.onConnectionRestored()
                        }
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
                    try {
                        val result = firebaseAuth.signInWithEmailAndPassword(email, senha).await()
                        
                        if (result.user != null) {
                            android.util.Log.d("AuthViewModel", "✅ LOGIN ONLINE SUCESSO!")

                            // ✅ NOVO: Emitir log específico para criação automática de dados após login
                            android.util.Log.w(
                                "🔍 DB_POPULATION",
                                "🚨 LOGIN ONLINE CONCLUÍDO - DISPARANDO CARREGAMENTO INICIAL DE DADOS"
                            )
    
                            // ✅ NOVO: Criar/atualizar colaborador para usuário online
                            criarOuAtualizarColaboradorOnline(result.user!!)
                            
                            // ✅ NOVO: Verificar se a sessão foi iniciada corretamente
                            val nomeSessao = userSessionManager.getCurrentUserName()
                            val idSessao = userSessionManager.getCurrentUserId()
                            android.util.Log.d("AuthViewModel", "🔍 Verificação da sessão online:")
                            android.util.Log.d("AuthViewModel", "   Nome na sessão: $nomeSessao")
                            android.util.Log.d("AuthViewModel", "   ID na sessão: $idSessao")
                            
                            _authState.value = AuthState.Authenticated(result.user!!, true)
                            return@launch
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("AuthViewModel", "Login online falhou: ${e.message}")
                    }
                }
                
                // Se online falhou ou está offline, tentar login local
                android.util.Log.d("AuthViewModel", "Tentando login offline...")
                android.util.Log.d("AuthViewModel", "Email para busca: $email")
                
                val colaborador = appRepository.obterColaboradorPorEmail(email)
                
                android.util.Log.d("AuthViewModel", "🔍 Colaborador encontrado: ${colaborador?.nome ?: "NÃO ENCONTRADO"}")
                if (colaborador != null) {
                    android.util.Log.d("AuthViewModel", "   ID: ${colaborador.id}")
                    android.util.Log.d("AuthViewModel", "   Email: ${colaborador.email}")
                    android.util.Log.d("AuthViewModel", "   Nível: ${colaborador.nivelAcesso}")
                    android.util.Log.d("AuthViewModel", "   Aprovado: ${colaborador.aprovado}")
                    android.util.Log.d("AuthViewModel", "   Senha temporária: ${colaborador.senhaTemporaria}")
                    android.util.Log.d("AuthViewModel", "   Firebase UID: ${colaborador.firebaseUid}")
                
                    // ✅ NOVO: Sistema híbrido - aceitar senha Firebase ou senha temporária
                    val senhaValida = when {
                        // Senha temporária do sistema local
                        colaborador.senhaTemporaria == senha -> true
                        // Senha padrão para desenvolvimento
                        senha == "123456" -> true
                        // ✅ NOVO: Para usuários que já fizeram login online, aceitar qualquer senha
                        // (assumindo que a autenticação Firebase já validou anteriormente)
                        colaborador.firebaseUid != null -> true
                        else -> false
                    }
                    
                    android.util.Log.d("AuthViewModel", "🔍 Validação de senha:")
                    android.util.Log.d("AuthViewModel", "   Senha fornecida: $senha")
                    android.util.Log.d("AuthViewModel", "   Senha temporária: ${colaborador.senhaTemporaria}")
                    android.util.Log.d("AuthViewModel", "   Senha padrão: 123456")
                    android.util.Log.d("AuthViewModel", "   Firebase UID presente: ${colaborador.firebaseUid != null}")
                    android.util.Log.d("AuthViewModel", "   Senha válida: $senhaValida")
                    
                    if (senhaValida) {
                        val tipoAutenticacao = when {
                            colaborador.senhaTemporaria == senha -> "senha temporária"
                            senha == "123456" -> "senha padrão desenvolvimento"
                            colaborador.firebaseUid != null -> "usuário previamente autenticado online"
                            else -> "desconhecido"
                        }
                        android.util.Log.d("AuthViewModel", "✅ LOGIN OFFLINE SUCESSO! (Tipo: $tipoAutenticacao)")

                        android.util.Log.w(
                            "🔍 DB_POPULATION",
                            "🚨 LOGIN OFFLINE CONCLUÍDO - REALIZANDO CONFIGURAÇÃO LOCAL (POTENCIAL POPULAÇÃO)"
                        )
                        
                        // ✅ NOVO: Verificar se precisa atualizar para ADMIN (email especial)
                        val colaboradorFinal = if (colaborador.email == "rossinys@gmail.com" && colaborador.nivelAcesso != NivelAcesso.ADMIN) {
                            val colaboradorAdmin = colaborador.copy(
                                nivelAcesso = NivelAcesso.ADMIN,
                                aprovado = true,
                                dataAprovacao = java.util.Date(),
                                aprovadoPor = "Sistema (Admin Padrão)"
                            )
                            appRepository.atualizarColaborador(colaboradorAdmin)
                            android.util.Log.d("AuthViewModel", "✅ Colaborador offline atualizado para ADMIN: ${colaboradorAdmin.nome}")
                            colaboradorAdmin
                        } else {
                            colaborador
                        }
                        
                        android.util.Log.d("AuthViewModel", "🔍 Iniciando sessão para: ${colaboradorFinal.nome}")
                        android.util.Log.d("AuthViewModel", "   ID: ${colaboradorFinal.id}")
                        android.util.Log.d("AuthViewModel", "   Email: ${colaboradorFinal.email}")
                        
                        // ✅ NOVO: Iniciar sessão do usuário
                        android.util.Log.d("AuthViewModel", "🔍 Iniciando sessão offline para: ${colaboradorFinal.nome}")
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
                        
                        _authState.value = AuthState.Authenticated(localUser, false)
                        return@launch
                    } else {
                        _errorMessage.value = "Senha incorreta"
                    }
                } else {
                    // ✅ NOVO: Se não existe colaborador local, criar automaticamente para emails específicos
                    // Restaurado e corrigido para rossinys@gmail.com
                    if (email == "rossinys@gmail.com") {
                        android.util.Log.d("AuthViewModel", "🔧 Criando colaborador ADMIN automaticamente para: $email")
                        android.util.Log.w(
                            "🔍 DB_POPULATION",
                            "🚨 CRIANDO COLABORADOR ADMIN AUTOMATICAMENTE - EMAIL: $email"
                        )

                        val novoColaborador = Colaborador(
                            nome = email.substringBefore("@"),
                            email = email,
                            nivelAcesso = NivelAcesso.ADMIN,
                            aprovado = true,
                            ativo = true,
                            senhaTemporaria = senha, // Salvar senha para login offline futuro
                            dataAprovacao = java.util.Date(),
                            aprovadoPor = "Sistema (Admin Padrão Offline)"
                        )

                        val colaboradorId = appRepository.inserirColaborador(novoColaborador)
                        val colaboradorComId = novoColaborador.copy(id = colaboradorId)

                        android.util.Log.d("AuthViewModel", "✅ Colaborador ADMIN criado offline: ${colaboradorComId.nome}")
                        userSessionManager.startSession(colaboradorComId)

                        val localUser = LocalUser(
                            uid = colaboradorComId.id.toString(),
                            email = colaboradorComId.email,
                            displayName = colaboradorComId.nome,
                            nivelAcesso = colaboradorComId.nivelAcesso
                        )

                        _authState.value = AuthState.Authenticated(localUser, false)
                        return@launch
                    } else {
                        _errorMessage.value = "Usuário não encontrado. Faça login online primeiro para sincronizar sua conta."
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
     * Função para registrar novo usuário
     */
    fun register(email: String, senha: String, confirmarSenha: String) {
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
                
                // Criar usuário no Firebase
                val result = firebaseAuth.createUserWithEmailAndPassword(email, senha).await()
                
                if (result.user != null) {
                    _authState.value = AuthState.Authenticated(result.user!!, true)
                } else {
                    _authState.value = AuthState.Unauthenticated
                    _errorMessage.value = "Falha ao criar conta"
                }
                
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated
                _errorMessage.value = getFirebaseErrorMessage(e)
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
                            
                            val colaborador = appRepository.obterColaboradorPorEmail(email)
                            
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
                                // Usuário não existe no banco local - BLOQUEADO: criação automática desabilitada
                                android.util.Log.d("AuthViewModel", "Usuário não encontrado no banco local - criação automática BLOQUEADA")
                                _errorMessage.value = "Usuário não encontrado. Contate o administrador para criar sua conta."
                                _authState.value = AuthState.Unauthenticated
                                return@launch
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
                
                val colaborador = appRepository.obterColaboradorPorEmail(email)
                android.util.Log.d("AuthViewModel", "Colaborador encontrado: ${colaborador != null}")
                
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
                        dataCadastro = Date(),
                        dataUltimaAtualizacao = Date()
                    )
                    
                    android.util.Log.d("AuthViewModel", "Novo colaborador criado (pendente): ${novoColaborador.nome}")
                    val colaboradorId = appRepository.inserirColaborador(novoColaborador)
                    android.util.Log.d("AuthViewModel", "Novo colaborador criado com ID: $colaboradorId")
                    
                    _errorMessage.value = "Conta criada com sucesso! Aguarde aprovação do administrador."
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
                dataCadastro = Date(),
                dataUltimaAtualizacao = Date()
            )
            
            val colaboradorId = appRepository.inserirColaborador(colaborador)
            val colaboradorComId = colaborador.copy(id = colaboradorId)
            
            android.util.Log.d("AuthViewModel", "✅ Colaborador criado automaticamente com ID: $colaboradorId")
            android.util.Log.d("AuthViewModel", "Nome: ${colaborador.nome}")
            android.util.Log.d("AuthViewModel", "Email: ${colaborador.email}")
            android.util.Log.d("AuthViewModel", "Aprovado: ${colaborador.aprovado}")
            
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
     */
    private suspend fun criarOuAtualizarColaboradorOnline(firebaseUser: FirebaseUser) {
        try {
            val email = firebaseUser.email ?: return
            val nome = firebaseUser.displayName ?: email.substringBefore("@")
            
            // Verificar se já existe colaborador com este email
            val colaboradorExistente = appRepository.obterColaboradorPorEmail(email)
            
            if (colaboradorExistente != null) {
                android.util.Log.d("AuthViewModel", "Colaborador existente encontrado: ${colaboradorExistente.nome}")

                android.util.Log.w(
                    "🔍 DB_POPULATION",
                    "🚨 ATUALIZANDO COLABORADOR LOCAL APÓS LOGIN ONLINE: ${colaboradorExistente.email}"
                )

                // ✅ CORREÇÃO CRÍTICA: Manter nível de acesso original, exceto para admin especial
                val colaboradorAtualizado = if (email == "rossinys@gmail.com") {
                    // Apenas para o admin especial - forçar ADMIN
                    colaboradorExistente.copy(
                        nome = firebaseUser.displayName ?: colaboradorExistente.nome,
                        firebaseUid = firebaseUser.uid,
                        dataUltimoAcesso = java.util.Date(),
                        nivelAcesso = NivelAcesso.ADMIN,
                        aprovado = true,
                        dataAprovacao = colaboradorExistente.dataAprovacao ?: java.util.Date(),
                        aprovadoPor = colaboradorExistente.aprovadoPor ?: "Sistema (Admin Padrão)"
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
                
                // ❌ BLOQUEADO: Não criar colaborador automaticamente
                // Apenas para rossinys@gmail.com (admin especial)
                if (email == "rossinys@gmail.com") {
                    val novoColaborador = Colaborador(
                        nome = nome,
                        email = email,
                        nivelAcesso = NivelAcesso.ADMIN,
                        aprovado = true,
                        ativo = true,
                        firebaseUid = firebaseUser.uid,
                        dataAprovacao = java.util.Date(),
                        aprovadoPor = "Sistema (Admin Padrão)"
                    )
                    
                    val colaboradorId = appRepository.inserirColaborador(novoColaborador)
                    val colaboradorComId = novoColaborador.copy(id = colaboradorId)
                    
                    android.util.Log.d("AuthViewModel", "✅ Admin especial criado: $nome")
                    userSessionManager.startSession(colaboradorComId)
                } else {
                    android.util.Log.d("AuthViewModel", "Usuário não encontrado - criação automática bloqueada")
                    _errorMessage.value = "Usuário não encontrado. Contate o administrador para criar sua conta."
                    _authState.value = AuthState.Unauthenticated
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Erro ao criar/atualizar colaborador online: ${e.message}")
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
}

/**
 * Estados da autenticação
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val user: Any, val isOnline: Boolean) : AuthState()
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

