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
            AppDatabase.getDatabase(context)
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
                            android.util.Log.d("AuthViewModel", "🔍 Chamando criarOuAtualizarColaboradorOnline...")
                            var colaborador = criarOuAtualizarColaboradorOnline(result.user!!)
                            android.util.Log.d("AuthViewModel", "   Resultado: ${if (colaborador != null) "SUCESSO - ${colaborador.nome}" else "NULL - não encontrado"}")
                            
                            // ✅ SUPERADMIN: Se for rossinys@gmail.com e não encontrou, criar automaticamente
                            if (colaborador == null && email == "rossinys@gmail.com") {
                                android.util.Log.d("AuthViewModel", "🔧 Criando SUPERADMIN automaticamente para: $email")
                                colaborador = criarSuperAdminAutomatico(email, result.user!!.uid, senha)
                            }
                            
                            // ✅ CORREÇÃO: Se ainda não encontrou, tentar buscar diretamente na nuvem como fallback
                            if (colaborador == null) {
                                android.util.Log.w("AuthViewModel", "⚠️ Colaborador não encontrado após criarOuAtualizarColaboradorOnline")
                                android.util.Log.w("AuthViewModel", "   Tentando busca direta na nuvem como fallback...")
                                try {
                                    val colaboradorFallback = buscarColaboradorNaNuvemPorEmail(email)
                                    if (colaboradorFallback != null) {
                                        android.util.Log.d("AuthViewModel", "✅ Colaborador encontrado no fallback: ${colaboradorFallback.nome}")
                                        // Atualizar firebaseUid e salvar localmente
                                        val colaboradorComUid = colaboradorFallback.copy(
                                            firebaseUid = result.user!!.uid,
                                            dataUltimoAcesso = java.util.Date()
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
                                            userSessionManager.startSession(colaborador)
                                            android.util.Log.d("AuthViewModel", "✅ Colaborador salvo e sessão iniciada no fallback")
                                        } catch (e: Exception) {
                                            android.util.Log.e("AuthViewModel", "❌ Erro ao salvar colaborador no fallback: ${e.message}", e)
                                            // Mesmo com erro, tentar usar o colaborador da nuvem
                                            userSessionManager.startSession(colaboradorComUid)
                                            colaborador = colaboradorComUid
                                        }
                                    } else {
                                        android.util.Log.e("AuthViewModel", "❌ Colaborador também não encontrado no fallback")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("AuthViewModel", "❌ Erro no fallback: ${e.message}", e)
                                }
                            }
                            
                            if (colaborador == null) {
                                android.util.Log.e("AuthViewModel", "❌ ERRO FINAL: Colaborador não encontrado após todas as tentativas")
                                android.util.Log.e("AuthViewModel", "   Email: $email")
                                android.util.Log.e("AuthViewModel", "   Firebase UID: ${result.user!!.uid}")
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
                            if (idSessao == 0L) {
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
                
                // ✅ CORREÇÃO: Buscar colaborador por email ou firebaseUid
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
                    
                    // ✅ SEGURANÇA: Superadmin também deve validar senha corretamente
                    // Seguindo melhores práticas de segurança, não permitir login com qualquer senha
                    val isSuperAdmin = email == "rossinys@gmail.com"
                    
                    // ✅ CORREÇÃO DE SEGURANÇA: Superadmin deve ter senha válida como qualquer usuário
                    if (!senhaValida) {
                        android.util.Log.w("AuthViewModel", "❌ Senha inválida para ${if (isSuperAdmin) "SUPERADMIN" else "usuário"}")
                        _errorMessage.value = "Senha incorreta"
                        return@launch
                    }
                    
                    if (senhaValida) {
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
                        // ✅ SEGURANÇA: Atualizar senha apenas se for válida (já validada acima)
                        var colaboradorFinal = if (isSuperAdmin) {
                            colaborador.copy(
                                nivelAcesso = NivelAcesso.ADMIN,
                                aprovado = true,
                                primeiroAcesso = false,
                                senhaHash = senhaLimpa // ✅ Atualizar com senha válida para login offline
                            ).also {
                                appRepository.atualizarColaborador(it)
                                android.util.Log.d("AuthViewModel", "✅ SUPERADMIN: Dados atualizados (senha válida confirmada)")
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
                                
                                // ✅ SEGURANÇA: Superadmin também deve validar senha corretamente
                                // Seguindo melhores práticas de segurança, não permitir login com qualquer senha
                                val isSuperAdmin = email == "rossinys@gmail.com"
                                
                                // ✅ CORREÇÃO DE SEGURANÇA: Superadmin deve ter senha válida como qualquer usuário
                                if (!senhaValida) {
                                    android.util.Log.w("AuthViewModel", "❌ Senha inválida para ${if (isSuperAdmin) "SUPERADMIN" else "usuário"} (dados da nuvem)")
                                    _errorMessage.value = "Senha incorreta"
                                    return@launch
                                }
                                
                                if (senhaValida) {
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
    private suspend fun criarOuAtualizarColaboradorOnline(firebaseUser: FirebaseUser): Colaborador? {
        try {
            val email = firebaseUser.email ?: return null
            
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
                android.util.Log.d("AuthViewModel", "🔍 Colaborador não encontrado localmente. Buscando na nuvem...")
                android.util.Log.d("AuthViewModel", "   Email para busca: $email")
                android.util.Log.d("AuthViewModel", "   Firebase UID: ${firebaseUser.uid}")
                
                // ✅ CORREÇÃO CRÍTICA: Buscar colaborador na nuvem quando não encontrar localmente
                var colaboradorNuvem: Colaborador? = null
                try {
                    colaboradorNuvem = buscarColaboradorNaNuvemPorEmail(email)
                    android.util.Log.d("AuthViewModel", "   Resultado da busca na nuvem: ${if (colaboradorNuvem != null) "ENCONTRADO" else "NÃO ENCONTRADO"}")
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "❌ ERRO ao buscar colaborador na nuvem: ${e.message}", e)
                    android.util.Log.e("AuthViewModel", "   Stack trace: ${e.stackTraceToString()}")
                }
                
                if (colaboradorNuvem != null) {
                    android.util.Log.d("AuthViewModel", "✅ Colaborador encontrado na nuvem: ${colaboradorNuvem.nome}")
                    android.util.Log.d("AuthViewModel", "   ID: ${colaboradorNuvem.id}")
                    android.util.Log.d("AuthViewModel", "   Email: ${colaboradorNuvem.email}")
                    android.util.Log.d("AuthViewModel", "   Aprovado: ${colaboradorNuvem.aprovado}")
                    
                    // ✅ Atualizar firebaseUid com o UID do Firebase Authentication
                    val colaboradorAtualizado = colaboradorNuvem.copy(
                        firebaseUid = firebaseUser.uid,
                        dataUltimoAcesso = java.util.Date()
                    )
                    
                    // ✅ SUPERADMIN: rossinys@gmail.com sempre é ADMIN e aprovado
                    val colaboradorFinal = if (email == "rossinys@gmail.com") {
                        colaboradorAtualizado.copy(
                            nivelAcesso = NivelAcesso.ADMIN,
                            aprovado = true,
                            primeiroAcesso = false,
                            dataAprovacao = colaboradorAtualizado.dataAprovacao ?: java.util.Date(),
                            aprovadoPor = colaboradorAtualizado.aprovadoPor ?: "Sistema (Superadmin)"
                        )
                    } else {
                        colaboradorAtualizado
                    }
                    
                    // ✅ Salvar colaborador localmente
                    try {
                        // Verificar se já existe por ID (pode ter sido criado com ID diferente)
                        val colaboradorExistentePorId = appRepository.obterColaboradorPorId(colaboradorFinal.id)
                        if (colaboradorExistentePorId != null) {
                            android.util.Log.d("AuthViewModel", "Colaborador já existe localmente (por ID), atualizando...")
                            appRepository.atualizarColaborador(colaboradorFinal)
                        } else {
                            // Verificar se existe por email (pode ter ID diferente)
                            val colaboradorExistentePorEmail = appRepository.obterColaboradorPorEmail(email)
                            if (colaboradorExistentePorEmail != null) {
                                android.util.Log.d("AuthViewModel", "Colaborador já existe localmente (por email), atualizando com ID da nuvem...")
                                // Atualizar o existente com os dados da nuvem, mantendo o ID local
                                val colaboradorMesclado = colaboradorFinal.copy(id = colaboradorExistentePorEmail.id)
                                appRepository.atualizarColaborador(colaboradorMesclado)
                                userSessionManager.startSession(colaboradorMesclado)
                                return colaboradorMesclado
                            } else {
                                android.util.Log.d("AuthViewModel", "Colaborador não existe localmente, inserindo...")
                                appRepository.inserirColaborador(colaboradorFinal)
                            }
                        }
                        
                        android.util.Log.d("AuthViewModel", "✅ Colaborador salvo localmente com sucesso")
                        userSessionManager.startSession(colaboradorFinal)
                        return colaboradorFinal
                        
                    } catch (e: Exception) {
                        android.util.Log.e("AuthViewModel", "❌ Erro ao salvar colaborador localmente: ${e.message}", e)
                        // Mesmo com erro ao salvar, tentar iniciar sessão com dados da nuvem
                        userSessionManager.startSession(colaboradorFinal)
                        return colaboradorFinal
                    }
                }
                
                // ✅ SUPERADMIN: Criar automaticamente para rossinys@gmail.com se não encontrou na nuvem
                if (email == "rossinys@gmail.com") {
                    android.util.Log.d("AuthViewModel", "🔧 Criando SUPERADMIN automaticamente para: $email")
                    val colaborador = criarSuperAdminAutomatico(email, firebaseUser.uid, "")
                    if (colaborador != null) {
                        return colaborador
                    }
                }
                
                android.util.Log.d("AuthViewModel", "❌ Colaborador não encontrado nem localmente nem na nuvem")
                _errorMessage.value = "Usuário não encontrado. Contate o administrador para criar sua conta."
                _authState.value = AuthState.Unauthenticated
                return null
            }
            
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "❌ ERRO ao criar/atualizar colaborador online: ${e.message}", e)
            android.util.Log.e("AuthViewModel", "   Stack trace: ${e.stackTraceToString()}")
            android.util.Log.e("AuthViewModel", "   Email: ${firebaseUser.email}")
            android.util.Log.e("AuthViewModel", "   Firebase UID: ${firebaseUser.uid}")
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
                    
                    // ✅ CORREÇÃO: Converter Timestamps do Firestore para Date antes de fazer parse com Gson
                    val colaboradorDataConvertido = colaboradorData.toMutableMap()
                    
                    fun converterTimestampParaDate(value: Any?): Date? {
                        return when (value) {
                            is com.google.firebase.Timestamp -> value.toDate()
                            is Date -> value
                            is Long -> Date(value)
                            is String -> {
                                value.toLongOrNull()?.let { Date(it) }
                                    ?: try {
                                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).parse(value)
                                    } catch (e: Exception) {
                                        null
                                    }
                            }
                            null -> null
                            else -> null
                        }
                    }
                    
                    colaboradorDataConvertido["dataCadastro"] = converterTimestampParaDate(colaboradorData["dataCadastro"])
                        ?: converterTimestampParaDate(colaboradorData["data_cadastro"])
                        ?: Date()
                    
                    colaboradorDataConvertido["dataUltimaAtualizacao"] = converterTimestampParaDate(colaboradorData["dataUltimaAtualizacao"])
                        ?: converterTimestampParaDate(colaboradorData["data_ultima_atualizacao"])
                        ?: converterTimestampParaDate(colaboradorData["lastModified"])
                        ?: Date()
                    
                    colaboradorDataConvertido["dataAprovacao"] = converterTimestampParaDate(colaboradorData["dataAprovacao"])
                        ?: converterTimestampParaDate(colaboradorData["data_aprovacao"])
                    
                    colaboradorDataConvertido["dataUltimoAcesso"] = converterTimestampParaDate(colaboradorData["dataUltimoAcesso"])
                        ?: converterTimestampParaDate(colaboradorData["data_ultimo_acesso"])
                    
                    colaboradorDataConvertido["dataNascimento"] = converterTimestampParaDate(colaboradorData["dataNascimento"])
                        ?: converterTimestampParaDate(colaboradorData["data_nascimento"])
                    
                    val colaboradorJson = gson.toJson(colaboradorDataConvertido)
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
                    
                    // ✅ CORREÇÃO: Converter Timestamps do Firestore para Date antes de fazer parse com Gson
                    val colaboradorDataConvertido = colaboradorData.toMutableMap()
                    
                    fun converterTimestampParaDate(value: Any?): Date? {
                        return when (value) {
                            is com.google.firebase.Timestamp -> value.toDate()
                            is Date -> value
                            is Long -> Date(value)
                            is String -> {
                                value.toLongOrNull()?.let { Date(it) }
                                    ?: try {
                                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).parse(value)
                                    } catch (e: Exception) {
                                        null
                                    }
                            }
                            null -> null
                            else -> null
                        }
                    }
                    
                    colaboradorDataConvertido["dataCadastro"] = converterTimestampParaDate(colaboradorData["dataCadastro"])
                        ?: converterTimestampParaDate(colaboradorData["data_cadastro"])
                        ?: Date()
                    
                    colaboradorDataConvertido["dataUltimaAtualizacao"] = converterTimestampParaDate(colaboradorData["dataUltimaAtualizacao"])
                        ?: converterTimestampParaDate(colaboradorData["data_ultima_atualizacao"])
                        ?: converterTimestampParaDate(colaboradorData["lastModified"])
                        ?: Date()
                    
                    colaboradorDataConvertido["dataAprovacao"] = converterTimestampParaDate(colaboradorData["dataAprovacao"])
                        ?: converterTimestampParaDate(colaboradorData["data_aprovacao"])
                    
                    colaboradorDataConvertido["dataUltimoAcesso"] = converterTimestampParaDate(colaboradorData["dataUltimoAcesso"])
                        ?: converterTimestampParaDate(colaboradorData["data_ultimo_acesso"])
                    
                    colaboradorDataConvertido["dataNascimento"] = converterTimestampParaDate(colaboradorData["dataNascimento"])
                        ?: converterTimestampParaDate(colaboradorData["data_nascimento"])
                    
                    val colaboradorJson = gson.toJson(colaboradorDataConvertido)
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
            
            // ✅ CORREÇÃO CRÍTICA: Converter Timestamps do Firestore para Date antes de fazer parse com Gson
            val colaboradorDataConvertido = colaboradorData.toMutableMap()
            
            // Converter campos Timestamp para Date
            fun converterTimestampParaDate(value: Any?): Date? {
                return when (value) {
                    is com.google.firebase.Timestamp -> value.toDate()
                    is Date -> value
                    is Long -> Date(value)
                    is String -> {
                        // Tentar parsear como timestamp Long
                        value.toLongOrNull()?.let { Date(it) }
                            ?: try {
                                // Tentar parsear como ISO string
                                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).parse(value)
                            } catch (e: Exception) {
                                null
                            }
                    }
                    null -> null
                    else -> null
                }
            }
            
            // Converter todos os campos de data
            colaboradorDataConvertido["dataCadastro"] = converterTimestampParaDate(colaboradorData["dataCadastro"])
                ?: converterTimestampParaDate(colaboradorData["data_cadastro"])
                ?: Date()
            
            colaboradorDataConvertido["dataUltimaAtualizacao"] = converterTimestampParaDate(colaboradorData["dataUltimaAtualizacao"])
                ?: converterTimestampParaDate(colaboradorData["data_ultima_atualizacao"])
                ?: converterTimestampParaDate(colaboradorData["lastModified"])
                ?: Date()
            
            colaboradorDataConvertido["dataAprovacao"] = converterTimestampParaDate(colaboradorData["dataAprovacao"])
                ?: converterTimestampParaDate(colaboradorData["data_aprovacao"])
            
            colaboradorDataConvertido["dataUltimoAcesso"] = converterTimestampParaDate(colaboradorData["dataUltimoAcesso"])
                ?: converterTimestampParaDate(colaboradorData["data_ultimo_acesso"])
            
            // Converter dataNascimento se existir
            colaboradorDataConvertido["dataNascimento"] = converterTimestampParaDate(colaboradorData["dataNascimento"])
                ?: converterTimestampParaDate(colaboradorData["data_nascimento"])
            
            val colaboradorJson = gson.toJson(colaboradorDataConvertido)
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
            android.util.Log.d("AuthViewModel", "   Ativo: ${colaboradorFirestore.ativo}")
            android.util.Log.d("AuthViewModel", "=== FIM BUSCA NA NUVEM ===")
            
            // ✅ IMPORTANTE: Retornar o colaborador mesmo se não estiver aprovado
            // A aprovação será verificada depois, mas precisamos do colaborador para salvar localmente
            return colaboradorFirestore
            
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "❌ ERRO GERAL na busca na nuvem: ${e.message}", e)
            android.util.Log.e("AuthViewModel", "   Stack trace: ${e.stackTraceToString()}")
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


