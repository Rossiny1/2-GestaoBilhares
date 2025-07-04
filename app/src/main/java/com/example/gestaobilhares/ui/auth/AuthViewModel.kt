package com.example.gestaobilhares.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel responsável pela lógica de autenticação com Firebase.
 * Implementa padrão MVVM para separar lógica de negócio da UI.
 */
class AuthViewModel : ViewModel() {
    
    // Instância do Firebase Auth
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    // LiveData para estado da autenticação
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState
    
    // LiveData para mensagens de erro
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // LiveData para estado de loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    init {
        // Inicializar sempre como não autenticado para mostrar tela de login
        _authState.value = AuthState.Unauthenticated
    }
    
    /**
     * Função para realizar login com email e senha
     */
    fun login(email: String, senha: String) {
        android.util.Log.d("AuthViewModel", "=== INICIANDO LOGIN ===")
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
                android.util.Log.d("AuthViewModel", "Iniciando autenticação com Firebase...")
                _isLoading.value = true
                _errorMessage.value = ""
                
                // Verificar se Firebase Auth está disponível
                android.util.Log.d("AuthViewModel", "Firebase Auth instance: $firebaseAuth")
                android.util.Log.d("AuthViewModel", "Firebase Auth current user: ${firebaseAuth.currentUser}")
                
                // Realizar login com Firebase
                android.util.Log.d("AuthViewModel", "Chamando signInWithEmailAndPassword...")
                val result = firebaseAuth.signInWithEmailAndPassword(email, senha).await()
                
                android.util.Log.d("AuthViewModel", "Resultado do login: $result")
                android.util.Log.d("AuthViewModel", "User: ${result.user}")
                
                if (result.user != null) {
                    android.util.Log.d("AuthViewModel", "✅ LOGIN SUCESSO! User ID: ${result.user!!.uid}")
                    android.util.Log.d("AuthViewModel", "User email: ${result.user!!.email}")
                    _authState.value = AuthState.Authenticated(result.user!!)
                } else {
                    android.util.Log.e("AuthViewModel", "❌ Falha na autenticação - user é null")
                    _authState.value = AuthState.Unauthenticated
                    _errorMessage.value = "Falha na autenticação"
                }
                
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "❌ ERRO NO LOGIN: ${e.message}", e)
                android.util.Log.e("AuthViewModel", "Tipo de erro: ${e.javaClass.simpleName}")
                _authState.value = AuthState.Unauthenticated
                _errorMessage.value = getFirebaseErrorMessage(e)
            } finally {
                _isLoading.value = false
                android.util.Log.d("AuthViewModel", "=== FIM DO LOGIN ===")
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
                    _authState.value = AuthState.Authenticated(result.user!!)
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
     * Função para logout
     */
    fun logout() {
        firebaseAuth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
    
    /**
     * Função para verificar usuário atual.
     * Pode ser chamada manualmente quando necessário (ex: splash screen, auto-login).
     */
    fun checkCurrentUser() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Authenticated(currentUser)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    /**
     * Função para resetar senha
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
    data class Authenticated(val user: FirebaseUser) : AuthState()
} 
