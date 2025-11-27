package com.example.gestaobilhares.core.utils

import android.content.Context
import android.content.SharedPreferences
import timber.log.Timber
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
        private const val KEY_LOGIN_TIMESTAMP = "login_timestamp" // ✅ NOVO: Timestamp do login para detectar novo login
    }
    
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appContext: Context = context
    private val ioScope = CoroutineScope(Dispatchers.IO)

    // DataStore
    private val Context.dataStore by preferencesDataStore(name = PREFS_NAME)
    private object Keys {
        val USER_ID = longPreferencesKey(KEY_USER_ID)
        val USER_EMAIL = stringPreferencesKey(KEY_USER_EMAIL)
        val USER_NAME = stringPreferencesKey(KEY_USER_NAME)
        val USER_LEVEL = stringPreferencesKey(KEY_USER_NIVEL_ACESSO)
        val IS_LOGGED = booleanPreferencesKey(KEY_IS_LOGGED_IN)
        val USER_APPROVED = booleanPreferencesKey(KEY_USER_APPROVED)
    }
    
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
        
        // ✅ NOVO: Timestamp do login para detectar novo login
        val loginTimestamp = System.currentTimeMillis()
        
        // Escrita dupla: SharedPreferences (legado) + DataStore
        sharedPrefs.edit().apply {
            putLong(KEY_USER_ID, colaborador.id)
            putString(KEY_USER_EMAIL, colaborador.email)
            putString(KEY_USER_NAME, colaborador.nome)
            putString(KEY_USER_NIVEL_ACESSO, colaborador.nivelAcesso.name)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_USER_APPROVED, colaborador.aprovado)
            putLong(KEY_LOGIN_TIMESTAMP, loginTimestamp) // ✅ NOVO: Salvar timestamp do login
            commit() // ✅ CORREÇÃO: Usar commit() para garantir salvamento imediato
        }
        ioScope.launch {
            try {
                appContext.dataStore.edit { prefs ->
                    prefs[Keys.USER_ID] = colaborador.id
                    prefs[Keys.USER_EMAIL] = colaborador.email
                    prefs[Keys.USER_NAME] = colaborador.nome
                    prefs[Keys.USER_LEVEL] = colaborador.nivelAcesso.name
                    prefs[Keys.IS_LOGGED] = true
                    prefs[Keys.USER_APPROVED] = colaborador.aprovado
                }
            } catch (e: Exception) {
                android.util.Log.e("UserSessionManager", "Erro ao salvar DataStore: ${e.message}")
            }
        }
        
        Timber.d("Sessao iniciada: id=%s nome=%s nivel=%s aprovado=%s admin=%s menu=%s",
            colaborador.id, colaborador.nome, colaborador.nivelAcesso, colaborador.aprovado, isAdmin(), hasMenuAccess())
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
        // Limpar DataStore
        ioScope.launch {
            try {
                appContext.dataStore.edit { it.clear() }
            } catch (e: Exception) {
                android.util.Log.e("UserSessionManager", "Erro ao limpar DataStore: ${e.message}")
            }
        }
        
        Timber.d("Sessao encerrada")
    }
    
    /**
     * Restaura sessão do SharedPreferences
     */
    private fun restoreSession() {
        // Preferir DataStore; fallback para SharedPreferences
        var isLoggedIn = false
        try {
            // leitura síncrona não é suportada; fazemos melhor esforço com SharedPreferences para boot rápido
            // e agendamos atualização do StateFlow a partir do DataStore em background
            isLoggedIn = sharedPrefs.getBoolean(KEY_IS_LOGGED_IN, false)
            ioScope.launch {
                try {
                    val logged = appContext.dataStore.data
                        .catch { }
                        .map { prefs -> prefs[Keys.IS_LOGGED] == true }
                        .firstOrNull() == true
                    if (logged) restoreFromDataStore()
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
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
                
                Timber.d("Sessao restaurada: id=%s nome=%s nivel=%s aprovado=%s admin=%s menu=%s",
                    userId, userName, userLevel, userApproved, isAdmin(), hasMenuAccess())
            } catch (e: Exception) {
                Timber.e(e, "Erro ao restaurar sessao: %s", e.message)
                endSession()
            }
        }
    }

    private suspend fun restoreFromDataStore() {
        try {
            val prefs = appContext.dataStore.data.first()
            val userId = prefs[Keys.USER_ID] ?: 0L
            val userEmail = prefs[Keys.USER_EMAIL] ?: ""
            val userName = prefs[Keys.USER_NAME] ?: ""
            val userLevelString = prefs[Keys.USER_LEVEL] ?: NivelAcesso.USER.name
            val userApproved = prefs[Keys.USER_APPROVED] ?: false

            val userLevel = NivelAcesso.valueOf(userLevelString)
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
        } catch (e: Exception) {
            Timber.e(e, "Erro ao restaurar DataStore: %s", e.message)
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
        Timber.d("getCurrentUserName() nome='%s' user=%s", nome, _currentUser.value)
        return nome
    }
    
    /**
     * Obtém o email do usuário atual
     */
    fun getCurrentUserEmail(): String {
        return _currentUser.value?.email ?: ""
    }
    
    /**
     * Obtém o nível de acesso atual do usuário logado.
     */
    fun getCurrentUserLevel(): NivelAcesso {
        return _userLevel.value
    }
    
    /**
     * ✅ NOVO: Obtém o timestamp do último login
     * Usado para detectar se é um novo login (quando o timestamp muda)
     */
    fun getLoginTimestamp(): Long {
        return sharedPrefs.getLong(KEY_LOGIN_TIMESTAMP, 0L)
    }
    
    /**
     * Verifica se o usuário tem permissão para acessar o menu principal
     * ✅ CORREÇÃO: Apenas ADMIN pode acessar o menu principal
     */
    fun hasMenuAccess(): Boolean {
        return isAdmin() && isApproved() // Apenas ADMIN aprovado tem acesso ao menu
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
     * Verifica se o usuário tem permissão para gerenciar contratos
     */
    fun canManageContracts(): Boolean {
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
            Timber.e(e, "Erro ao buscar rotas do usuario: %s", e.message)
            emptyList()
        }
    }

    suspend fun hasAnyRouteAssignments(context: Context): Boolean {
        return try {
            val database = AppDatabase.getDatabase(context)
            database.colaboradorDao().contarTotalRotasColaborador(getCurrentUserId()) > 0
        } catch (e: Exception) {
            Timber.e(e, "Erro ao verificar rotas locais: %s", e.message)
            false
        }
    }
}
