package com.example.gestaobilhares.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
// import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ViewModel responsável pela lógica de autenticação com Firebase.
 * Implementa padrão MVVM para separar lógica de negócio da UI.
 */
// @HiltViewModel
class AuthViewModel constructor() : ViewModel() {
    
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
        // Validação básica
        if (email.isBlank() || senha.isBlank()) {
            _errorMessage.value = "Email e senha são obrigatórios"
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
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = ""
                
                // Realizar login com Firebase
                val result = firebaseAuth.signInWithEmailAndPassword(email, senha).await()
                
                if (result.user != null) {
                    _authState.value = AuthState.Authenticated(result.user!!)
                } else {
                    _authState.value = AuthState.Unauthenticated
                    _errorMessage.value = "Falha na autenticação"
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
