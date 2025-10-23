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
                    
                    // Simular sincronização (aqui seria a lógica real com Firestore)
                    val success = simulateSyncOperation(operation)
                    
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
                            "Simulação de falha",
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
     * Simular operação de sincronização (placeholder para Firestore)
     */
    private suspend fun simulateSyncOperation(operation: SyncQueue): Boolean {
        // Simular delay de rede
        delay(1000)
        
        // Simular 90% de sucesso
        return (0..100).random() < 90
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
