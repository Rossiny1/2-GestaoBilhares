package com.example.gestaobilhares.ui.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.entities.Colaborador
import com.example.gestaobilhares.data.entities.NivelAcesso
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.utils.NetworkUtils
import com.example.gestaobilhares.utils.SyncManager
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
class AuthViewModel : ViewModel() {
    
    // Instância do Firebase Auth
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    // Repositório para acesso local
    private lateinit var appRepository: AppRepository
    
    // Utilitário de rede
    private lateinit var networkUtils: NetworkUtils
    
    // Gerenciador de sincronização
    private lateinit var syncManager: SyncManager
    
    // LiveData para estado da autenticação
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState
    
    // LiveData para mensagens de erro
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // LiveData para estado de loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // LiveData para modo de conexão
    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> = _isOnline
    
    init {
        // Inicializar sempre como não autenticado para mostrar tela de login
        _authState.value = AuthState.Unauthenticated
        _isOnline.value = true // Assumir online por padrão
    }
    
    /**
     * Inicializa o repositório local e utilitário de rede
     */
    fun initializeRepository(context: Context) {
        val database = AppDatabase.getDatabase(context)
        appRepository = AppRepository(
            database.clienteDao(),
            database.acertoDao(),
            database.mesaDao(),
            database.rotaDao(),
            database.despesaDao(),
            database.colaboradorDao(),
            database.cicloAcertoDao()
        )
        
        networkUtils = NetworkUtils(context)
        syncManager = SyncManager(context, appRepository)
        
        // Observar mudanças na conectividade
        viewModelScope.launch {
            networkUtils.isNetworkAvailable.collect { isAvailable ->
                val wasOffline = _isOnline.value == false
                _isOnline.value = isAvailable
                
                // Se voltou a ter conexão, sincronizar dados
                if (wasOffline && isAvailable) {
                    syncManager.onConnectionRestored()
                }
            }
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
                _isLoading.value = true
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
                            _authState.value = AuthState.Authenticated(result.user!!, true)
                            return@launch
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("AuthViewModel", "Login online falhou: ${e.message}")
                    }
                }
                
                // Se online falhou ou está offline, tentar login local
                android.util.Log.d("AuthViewModel", "Tentando login offline...")
                val colaborador = appRepository.obterColaboradorPorEmail(email)
                
                if (colaborador != null) {
                    // Verificar se a senha está correta (implementar hash depois)
                    if (colaborador.senhaTemporaria == senha || senha == "123456") { // Senha padrão temporária
                        android.util.Log.d("AuthViewModel", "✅ LOGIN OFFLINE SUCESSO!")
                        
                        // Criar usuário local simulado
                        val localUser = LocalUser(
                            uid = colaborador.id.toString(),
                            email = colaborador.email,
                            displayName = colaborador.nome,
                            nivelAcesso = colaborador.nivelAcesso
                        )
                        
                        _authState.value = AuthState.Authenticated(localUser, false)
                        return@launch
                    } else {
                        _errorMessage.value = "Senha incorreta"
                    }
                } else {
                    _errorMessage.value = "Usuário não encontrado"
                }
                
                _authState.value = AuthState.Unauthenticated
                
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "❌ ERRO NO LOGIN: ${e.message}", e)
                _authState.value = AuthState.Unauthenticated
                _errorMessage.value = getFirebaseErrorMessage(e)
            } finally {
                _isLoading.value = false
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
                _isLoading.value = true
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
                _isLoading.value = false
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
                
                _isLoading.value = true
                _errorMessage.value = ""
                
                // Verificar se o repositório foi inicializado
                if (!::appRepository.isInitialized) {
                    android.util.Log.e("AuthViewModel", "Repositório não inicializado")
                    _errorMessage.value = "Erro interno: Repositório não inicializado"
                    _authState.value = AuthState.Unauthenticated
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
                                // Usuário não existe no banco local - criar automaticamente
                                android.util.Log.d("AuthViewModel", "Usuário não encontrado no banco local - criando automaticamente")
                                
                                try {
                                    // Criar perfil de colaborador automaticamente
                                    criarColaboradorAutomatico(result.user!!, account.displayName ?: "")
                                    _errorMessage.value = "Conta criada com sucesso! Aguarde aprovação do administrador."
                                    _authState.value = AuthState.Unauthenticated
                                    return@launch
                                } catch (e: Exception) {
                                    android.util.Log.e("AuthViewModel", "Erro ao criar colaborador: ${e.message}")
                                    _errorMessage.value = "Erro ao criar conta. Tente novamente."
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
                
                val colaborador = appRepository.obterColaboradorPorEmail(email)
                android.util.Log.d("AuthViewModel", "Colaborador encontrado: ${colaborador != null}")
                
                if (colaborador != null) {
                    android.util.Log.d("AuthViewModel", "Colaborador encontrado: ${colaborador.nome}")
                    android.util.Log.d("AuthViewModel", "Aprovado: ${colaborador.aprovado}")
                    
                    // Verificar se está aprovado
                    if (colaborador.aprovado) {
                        android.util.Log.d("AuthViewModel", "✅ LOGIN OFFLINE SUCESSO!")
                        
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
                
                // TERCEIRO: Criar novo colaborador automaticamente (offline)
                android.util.Log.d("AuthViewModel", "Criando novo colaborador automaticamente...")
                
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
                        aprovado = false, // Precisa ser aprovado pelo admin
                        dataCadastro = Date(),
                        dataUltimaAtualizacao = Date()
                    )
                    
                    android.util.Log.d("AuthViewModel", "Novo colaborador criado: ${novoColaborador.nome}")
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
                _isLoading.value = false
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
            android.util.Log.d("AuthViewModel", "✅ Colaborador criado automaticamente com ID: $colaboradorId")
            android.util.Log.d("AuthViewModel", "Nome: ${colaborador.nome}")
            android.util.Log.d("AuthViewModel", "Email: ${colaborador.email}")
            android.util.Log.d("AuthViewModel", "Aprovado: ${colaborador.aprovado}")
            android.util.Log.d("AuthViewModel", "=== FIM CRIAÇÃO COLABORADOR ===")
            
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "❌ Erro ao criar colaborador: ${e.message}")
            android.util.Log.e("AuthViewModel", "Tipo de erro: ${e.javaClass.simpleName}")
            throw e // Re-throw para que o erro seja tratado no método chamador
        }
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
                _isLoading.value = true
                firebaseAuth.sendPasswordResetEmail(email).await()
                _errorMessage.value = "Email de recuperação enviado!"
            } catch (e: Exception) {
                _errorMessage.value = getFirebaseErrorMessage(e)
            } finally {
                _isLoading.value = false
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
