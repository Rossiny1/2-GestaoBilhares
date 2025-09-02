package com.example.gestaobilhares.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.gestaobilhares.data.entities.Colaborador
import com.example.gestaobilhares.data.entities.NivelAcesso
import com.example.gestaobilhares.data.database.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Gerenciador de sessão do usuário e controle de acesso
 * Centraliza informações do usuário logado e suas permissões
 */
class UserSessionManager private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: UserSessionManager? = null
        
        fun getInstance(context: Context): UserSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserSessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // Chaves para SharedPreferences
        private const val PREFS_NAME = "user_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_NIVEL_ACESSO = "user_nivel_acesso"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_APPROVED = "user_approved"
    }
    
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Estados observáveis
    private val _currentUser = MutableStateFlow<Colaborador?>(null)
    val currentUser: StateFlow<Colaborador?> = _currentUser.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val _userLevel = MutableStateFlow(NivelAcesso.USER)
    val userLevel: StateFlow<NivelAcesso> = _userLevel.asStateFlow()
    
    init {
        // Restaurar sessão ao inicializar
        restoreSession()
    }
    
    /**
     * Inicia sessão do usuário
     */
    fun startSession(colaborador: Colaborador) {
        _currentUser.value = colaborador
        _isLoggedIn.value = true
        _userLevel.value = colaborador.nivelAcesso
        
        // Salvar no SharedPreferences
        sharedPrefs.edit().apply {
            putLong(KEY_USER_ID, colaborador.id)
            putString(KEY_USER_EMAIL, colaborador.email)
            putString(KEY_USER_NAME, colaborador.nome)
            putString(KEY_USER_NIVEL_ACESSO, colaborador.nivelAcesso.name)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_USER_APPROVED, colaborador.aprovado)
            apply()
        }
        
        android.util.Log.d("UserSessionManager", "✅ SESSÃO INICIADA - DEBUG COMPLETO:")
        android.util.Log.d("UserSessionManager", "   ID: ${colaborador.id}")
        android.util.Log.d("UserSessionManager", "   Nome: ${colaborador.nome}")
        android.util.Log.d("UserSessionManager", "   Email: ${colaborador.email}")
        android.util.Log.d("UserSessionManager", "   Nível: ${colaborador.nivelAcesso}")
        android.util.Log.d("UserSessionManager", "   Aprovado: ${colaborador.aprovado}")
        android.util.Log.d("UserSessionManager", "   isAdmin(): ${isAdmin()}")
        android.util.Log.d("UserSessionManager", "   hasMenuAccess(): ${hasMenuAccess()}")
    }
    
    /**
     * Encerra sessão do usuário
     */
    fun endSession() {
        _currentUser.value = null
        _isLoggedIn.value = false
        _userLevel.value = NivelAcesso.USER
        
        // Limpar SharedPreferences
        sharedPrefs.edit().clear().apply()
        
        android.util.Log.d("UserSessionManager", "🔓 Sessão encerrada")
    }
    
    /**
     * Restaura sessão do SharedPreferences
     */
    private fun restoreSession() {
        val isLoggedIn = sharedPrefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (isLoggedIn) {
            val userId = sharedPrefs.getLong(KEY_USER_ID, 0)
            val userEmail = sharedPrefs.getString(KEY_USER_EMAIL, "") ?: ""
            val userName = sharedPrefs.getString(KEY_USER_NAME, "") ?: ""
            val userLevelString = sharedPrefs.getString(KEY_USER_NIVEL_ACESSO, NivelAcesso.USER.name)
            val userApproved = sharedPrefs.getBoolean(KEY_USER_APPROVED, false)
            
            try {
                val userLevel = NivelAcesso.valueOf(userLevelString ?: NivelAcesso.USER.name)
                
                // Criar objeto Colaborador básico para sessão
                val colaborador = Colaborador(
                    id = userId,
                    email = userEmail,
                    nome = userName,
                    nivelAcesso = userLevel,
                    aprovado = userApproved
                )
                
                _currentUser.value = colaborador
                _isLoggedIn.value = true
                _userLevel.value = userLevel
                
                android.util.Log.d("UserSessionManager", "🔄 SESSÃO RESTAURADA - DEBUG COMPLETO:")
                android.util.Log.d("UserSessionManager", "   ID: $userId")
                android.util.Log.d("UserSessionManager", "   Nome: $userName")
                android.util.Log.d("UserSessionManager", "   Email: $userEmail")
                android.util.Log.d("UserSessionManager", "   Nível: $userLevel")
                android.util.Log.d("UserSessionManager", "   Aprovado: $userApproved")
                android.util.Log.d("UserSessionManager", "   isAdmin(): ${isAdmin()}")
                android.util.Log.d("UserSessionManager", "   hasMenuAccess(): ${hasMenuAccess()}")
            } catch (e: Exception) {
                android.util.Log.e("UserSessionManager", "Erro ao restaurar sessão: ${e.message}")
                endSession()
            }
        }
    }
    
    /**
     * Verifica se o usuário é administrador
     */
    fun isAdmin(): Boolean {
        return _userLevel.value == NivelAcesso.ADMIN
    }
    
    /**
     * Verifica se o usuário é um usuário comum
     */
    fun isUser(): Boolean {
        return _userLevel.value == NivelAcesso.USER
    }
    
    /**
     * Verifica se o usuário está aprovado
     */
    fun isApproved(): Boolean {
        return _currentUser.value?.aprovado == true
    }
    
    /**
     * Obtém o ID do usuário atual
     */
    fun getCurrentUserId(): Long {
        return _currentUser.value?.id ?: 0L
    }
    
    /**
     * Obtém o nome do usuário atual
     */
    fun getCurrentUserName(): String {
        val nome = _currentUser.value?.nome ?: ""
        android.util.Log.d("UserSessionManager", "🔍 getCurrentUserName() chamado:")
        android.util.Log.d("UserSessionManager", "   _currentUser.value: ${_currentUser.value}")
        android.util.Log.d("UserSessionManager", "   Nome retornado: '$nome'")
        return nome
    }
    
    /**
     * Obtém o email do usuário atual
     */
    fun getCurrentUserEmail(): String {
        return _currentUser.value?.email ?: ""
    }
    
    /**
     * Verifica se o usuário tem permissão para acessar o menu principal
     * ✅ CORREÇÃO: USER aprovado também deve ter acesso ao menu (com funcionalidades limitadas)
     */
    fun hasMenuAccess(): Boolean {
        return isApproved() // Tanto ADMIN quanto USER aprovados têm acesso ao menu
    }
    
    /**
     * Verifica se o usuário tem permissão para gerenciar mesas
     */
    fun canManageTables(): Boolean {
        return isAdmin() && isApproved()
    }
    
    /**
     * Verifica se o usuário tem permissão para gerenciar rotas
     */
    fun canManageRoutes(): Boolean {
        return isAdmin() && isApproved()
    }
    
    /**
     * Verifica se o usuário tem permissão para gerenciar colaboradores
     */
    fun canManageCollaborators(): Boolean {
        return isAdmin() && isApproved()
    }
    
    /**
     * Verifica se o usuário pode acessar uma rota específica
     * Para usuários USER, só podem acessar rotas onde são responsáveis
     * Para usuários ADMIN, podem acessar todas as rotas
     */
    fun canAccessRoute(rotaId: Long, userRotaIds: List<Long> = emptyList()): Boolean {
        return when {
            isAdmin() && isApproved() -> true
            isUser() && isApproved() -> userRotaIds.contains(rotaId)
            else -> false
        }
    }
    
    /**
     * Verifica se o usuário pode editar uma rota específica
     * Mesmas regras do acesso, mas com verificação adicional
     */
    fun canEditRoute(rotaId: Long, userRotaIds: List<Long> = emptyList()): Boolean {
        return canAccessRoute(rotaId, userRotaIds)
    }
    
    /**
     * Busca as rotas que o usuário atual pode acessar
     * Para ADMIN: todas as rotas
     * Para USER: apenas rotas onde é responsável
     */
    suspend fun getUserAccessibleRoutes(context: Context): List<Long> {
        return try {
            if (isAdmin()) {
                // Admin tem acesso a todas as rotas
                emptyList() // Lista vazia indica "todas as rotas"
            } else {
                // USER: buscar apenas rotas onde é responsável
                val database = AppDatabase.getDatabase(context)
                val colaboradorRotas = database.colaboradorDao().obterRotasPorColaborador(getCurrentUserId()).first()
                colaboradorRotas.map { it.rotaId }
            }
        } catch (e: Exception) {
            android.util.Log.e("UserSessionManager", "Erro ao buscar rotas do usuário: ${e.message}")
            emptyList()
        }
    }
}
