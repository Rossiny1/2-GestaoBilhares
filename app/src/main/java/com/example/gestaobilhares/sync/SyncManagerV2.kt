package com.example.gestaobilhares.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
// Firestore/Auth/Coroutines
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
// JSON
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ✅ FASE 3C: Gerenciador de Sincronização V2
 * Utiliza as novas entidades SyncLog, SyncQueue e SyncConfig
 * Seguindo melhores práticas Android 2025
 */
class SyncManagerV2(
    private val context: Context,
    private val appRepository: AppRepository,
    private val database: AppDatabase
) {
    
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isSyncing = AtomicBoolean(false)
    
    // DAOs das novas entidades
    private val syncLogDao = database.syncLogDao()
    private val syncQueueDao = database.syncQueueDao()
    private val syncConfigDao = database.syncConfigDao()
    
    // LiveData para status
    private val _syncStatus = MutableLiveData<SyncStatus>()
    val syncStatus: LiveData<SyncStatus> = _syncStatus
    
    private val _pendingOperationsCount = MutableLiveData<Int>()
    val pendingOperationsCount: LiveData<Int> = _pendingOperationsCount
    
    private val _lastSyncTime = MutableLiveData<Long>()
    val lastSyncTime: LiveData<Long> = _lastSyncTime

    // Firebase Firestore e utilitários
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val gson: Gson by lazy { Gson() }

    init {
        syncScope.launch {
            initializeSyncConfig()
        }
        startPeriodicSync()
        observePendingOperations()
    }

    /**
     * Inicializar configurações padrão de sincronização
     */
    private suspend fun initializeSyncConfig() {
        try {
            syncConfigDao.inicializarConfiguracoesPadrao(System.currentTimeMillis())
            // Garantir que exista um empresa_id padrão
            val empresaConfig = syncConfigDao.buscarSyncConfigPorChave("empresa_id")
            if (empresaConfig == null) {
                val now = System.currentTimeMillis()
                // empresa_001 é o padrão visto no console do Firestore
                syncConfigDao.atualizarValorConfig("empresa_id", "empresa_001", now)
            }
            android.util.Log.d("SyncManagerV2", "Configurações de sincronização inicializadas")
        } catch (e: Exception) {
            android.util.Log.w("SyncManagerV2", "Erro ao inicializar configurações: ${e.message}")
        }
    }

    /**
     * Observar contagem de operações pendentes
     */
    private fun observePendingOperations() {
        syncScope.launch {
            syncQueueDao.contarOperacoesPendentes().let { count ->
                _pendingOperationsCount.postValue(count)
            }
        }
    }

    /**
     * Adicionar operação à fila de sincronização
     */
    suspend fun addToSyncQueue(
        entityType: String,
        entityId: Long,
        operation: String,
        payload: String,
        priority: Int = 0
    ) {
        try {
            val syncQueue = SyncQueue(
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payload = payload,
                createdAt = Date(),
                scheduledFor = Date(), // Processar imediatamente
                retryCount = 0,
                status = "PENDING",
                priority = priority
            )
            
            syncQueueDao.inserirSyncQueue(syncQueue)
            
            // Log da operação
            logSyncOperation(entityType, entityId, operation, "PENDING", null, payload)
            
            // Atualizar contagem
            observePendingOperations()
            
            android.util.Log.d("SyncManagerV2", "Operação adicionada à fila: $entityType:$entityId")
            
            // Tentar sincronizar se online
            if (isOnline()) {
                processSyncQueue()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "Erro ao adicionar à fila: ${e.message}")
        }
    }

    /**
     * Processar fila de sincronização
     */
    suspend fun processSyncQueue() {
        if (isSyncing.get() || !isOnline()) return
        if (!isAuthenticated()) {
            android.util.Log.w("SyncManagerV2", "Ignorando sync: usuário não autenticado no Firebase")
            return
        }
        
        isSyncing.set(true)
        _syncStatus.postValue(SyncStatus.SYNCING)
        
        try {
            val currentTime = System.currentTimeMillis()
            val operations = syncQueueDao.buscarOperacoesAgendadas(currentTime).first()
            
            android.util.Log.d("SyncManagerV2", "Processando ${operations.size} operações")
            
            for (operation in operations) {
                try {
                    // Marcar como processando
                    syncQueueDao.marcarComoProcessando(operation.id)
                    
                    // Aplicar operação real no Firestore
                    val success = applyOperationToFirestore(operation)
                    
                    if (success) {
                        // Marcar como concluída
                        syncQueueDao.marcarComoConcluida(operation.id)
                        logSyncOperation(
                            operation.entityType,
                            operation.entityId,
                            operation.operation,
                            "SUCCESS",
                            null,
                            operation.payload
                        )
                    } else {
                        // Marcar como falhou e agendar retry
                        val nextRetry = currentTime + (30000 * (operation.retryCount + 1)) // 30s, 60s, 90s
                        syncQueueDao.marcarComoFalhou(operation.id, nextRetry)
                        logSyncOperation(
                            operation.entityType,
                            operation.entityId,
                            operation.operation,
                            "FAILED",
                            "Falha ao aplicar operação no Firestore",
                            operation.payload
                        )
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.e("SyncManagerV2", "Erro ao processar operação ${operation.id}: ${e.message}")
                    syncQueueDao.marcarComoFalhou(operation.id, currentTime + 60000)
                }
            }
            
            // Atualizar timestamp da última sincronização
            updateLastSyncTimestamp()
            
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "Erro ao processar fila: ${e.message}")
            _syncStatus.postValue(SyncStatus.ERROR)
        } finally {
            isSyncing.set(false)
            _syncStatus.postValue(SyncStatus.SYNCED)
            observePendingOperations()
        }
    }

    /**
     * Aplicar operação no Firestore usando payload JSON armazenado na fila.
     * Mantém o app offline-first: Room continua fonte da verdade, Firestore é o espelho.
     */
    private suspend fun applyOperationToFirestore(operation: SyncQueue): Boolean {
        return try {
            val empresaId = getEmpresaId()
            val collection = getCollectionName(operation.entityType)
            val docId = operation.entityId.toString()

            // Converter o payload JSON em Map<String, Any?> para enviar ao Firestore
            val mapType = object : TypeToken<Map<String, Any?>>() {}.type
            val payloadMap: Map<String, Any?> = try {
                gson.fromJson(operation.payload, mapType) ?: emptyMap()
            } catch (e: Exception) {
                android.util.Log.w("SyncManagerV2", "Payload inválido para ${operation.entityType}:${operation.entityId} -> ${e.message}")
                emptyMap()
            }

            val docRef = firestore
                .collection("empresas")
                .document(empresaId)
                .collection(collection)
                .document(docId)

            when (operation.operation.uppercase(Locale.getDefault())) {
                "CREATE", "UPDATE" -> {
                    // Merge para não sobrescrever campos inexistentes
                    docRef.set(payloadMap, SetOptions.merge()).await()
                }
                "DELETE" -> {
                    docRef.delete().await()
                }
                else -> {
                    android.util.Log.w("SyncManagerV2", "Operação desconhecida: ${operation.operation}")
                    return false
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("SyncManagerV2", "Falha no Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Log de operação de sincronização
     */
    private suspend fun logSyncOperation(
        entityType: String,
        entityId: Long,
        operation: String,
        status: String,
        errorMessage: String?,
        payload: String?
    ) {
        try {
            val syncLog = SyncLog(
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                syncStatus = status,
                timestamp = Date(),
                errorMessage = errorMessage,
                payload = payload
            )
            
            syncLogDao.inserirSyncLog(syncLog)
        } catch (e: Exception) {
            android.util.Log.w("SyncManagerV2", "Erro ao logar operação: ${e.message}")
        }
    }

    /**
     * Verificar se está online
     */
    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /** Verifica se há usuário autenticado no Firebase (regras exigem auth) */
    private fun isAuthenticated(): Boolean {
        return try {
            FirebaseAuth.getInstance().currentUser != null
        } catch (_: Exception) { false }
    }

    /** Obtém o ID da empresa para particionar os dados no Firestore */
    private suspend fun getEmpresaId(): String {
        return try {
            val cfg = syncConfigDao.buscarSyncConfigPorChave("empresa_id")
            cfg?.value ?: "empresa_001"
        } catch (_: Exception) { "empresa_001" }
    }

    /** Mapeia tipos de entidades para coleções do Firestore */
    private fun getCollectionName(entityType: String): String = when (entityType.lowercase(Locale.getDefault())) {
        "cliente" -> "clientes"
        "acerto" -> "acertos"
        "mesa" -> "mesas"
        "rota" -> "rotas"
        "colaborador" -> "colaboradores"
        else -> entityType.lowercase(Locale.getDefault()) + "s"
    }

    /**
     * Iniciar sincronização periódica
     */
    private fun startPeriodicSync() {
        syncScope.launch {
            while (isActive) {
                delay(300000) // 5 minutos
                
                if (isOnline() && !isSyncing.get()) {
                    processSyncQueue()
                }
            }
        }
    }

    /**
     * Atualizar timestamp da última sincronização
     */
    private suspend fun updateLastSyncTimestamp() {
        try {
            val currentTime = System.currentTimeMillis()
            syncConfigDao.atualizarUltimoTimestampSync("last_sync_timestamp_global", currentTime.toString(), currentTime)
            _lastSyncTime.postValue(currentTime)
        } catch (e: Exception) {
            android.util.Log.w("SyncManagerV2", "Erro ao atualizar timestamp: ${e.message}")
        }
    }

    /**
     * Forçar sincronização manual
     */
    fun forceSync() {
        syncScope.launch {
            processSyncQueue()
        }
    }

    /**
     * Limpar logs antigos
     */
    suspend fun cleanupOldLogs() {
        try {
            val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 dias
            val deletedLogs = syncLogDao.deletarSyncLogsAntigos(cutoffTime)
            val deletedQueue = syncQueueDao.limparOperacoesConcluidas(cutoffTime)
            
            android.util.Log.d("SyncManagerV2", "Limpeza: $deletedLogs logs, $deletedQueue operações removidas")
        } catch (e: Exception) {
            android.util.Log.w("SyncManagerV2", "Erro na limpeza: ${e.message}")
        }
    }

    /**
     * Obter estatísticas de sincronização
     */
    suspend fun getSyncStats(): SyncStats {
        return try {
            val pendingCount = syncQueueDao.contarOperacoesPendentes()
            val failedCount = syncQueueDao.contarOperacoesPorStatus("FAILED")
            val completedCount = syncQueueDao.contarOperacoesPorStatus("COMPLETED")
            
            SyncStats(
                pendingOperations = pendingCount,
                failedOperations = failedCount,
                completedOperations = completedCount,
                isOnline = isOnline(),
                isSyncing = isSyncing.get()
            )
        } catch (e: Exception) {
            android.util.Log.w("SyncManagerV2", "Erro ao obter estatísticas: ${e.message}")
            SyncStats(0, 0, 0, false, false)
        }
    }

    /**
     * Destruir recursos
     */
    fun destroy() {
        syncScope.cancel()
    }
}

/**
 * Status de sincronização
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    ERROR
}

/**
 * Estatísticas de sincronização
 */
data class SyncStats(
    val pendingOperations: Int,
    val failedOperations: Int,
    val completedOperations: Int,
    val isOnline: Boolean,
    val isSyncing: Boolean
)
