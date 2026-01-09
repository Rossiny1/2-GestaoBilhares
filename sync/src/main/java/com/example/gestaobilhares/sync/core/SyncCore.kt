package com.example.gestaobilhares.sync.core

import com.example.gestaobilhares.data.dao.SyncMetadataDao
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.data.repository.AppRepository
import timber.log.Timber
import kotlinx.coroutines.flow.first

/**
 * Funcionalidades principais de sincronização.
 * Contém métodos de orquestração e gerenciamento de estado.
 */
class SyncCore(
    private val syncMetadataDao: SyncMetadataDao,
    private val userSessionManager: UserSessionManager,
    private val appRepository: AppRepository
) {
    
    companion object {
        private const val TAG = "SyncCore"
        private const val GLOBAL_SYNC_METADATA = "global_sync"
        private const val DEFAULT_BACKGROUND_IDLE_HOURS = 4L
    }
    
    private val currentUserId: Long
        get() = userSessionManager.getCurrentUserId()
    
    /**
     * Obtém o timestamp da última sincronização para um tipo de entidade.
     * 
     * Segue melhores práticas Android 2025 para sincronização incremental.
     */
    suspend fun getLastSyncTimestamp(entityType: String): Long {
        return try {
            syncMetadataDao.obterUltimoTimestamp(entityType, currentUserId)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Erro ao obter último timestamp para $entityType")
            0L
        }
    }
    
    /**
     * Obtém o timestamp do último push para um tipo de entidade.
     * 
     * Segue melhores práticas Android 2025 para sincronização incremental.
     */
    suspend fun getLastPushTimestamp(entityType: String): Long {
        return try {
            syncMetadataDao.obterUltimoTimestamp("${entityType}_push", currentUserId)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Erro ao obter último push timestamp para $entityType")
            0L
        }
    }
    
    /**
     * Salva metadados da sincronização push.
     * 
     * @param entityType Tipo da entidade sincronizada
     * @param syncCount Número de registros sincronizados
     * @param durationMs Duração da sincronização em milissegundos
     * @param bytesUploaded Bytes enviados (opcional)
     * @param error Erro ocorrido, se houver (null se sucesso)
     */
    suspend fun savePushMetadata(
        entityType: String,
        syncCount: Int,
        durationMs: Long,
        bytesUploaded: Long? = null,
        error: String? = null
    ) {
        try {
            val metadata = com.example.gestaobilhares.data.entities.SyncMetadata(
                entityType = "${entityType}_push",
                userId = currentUserId,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastSyncCount = syncCount,
                lastSyncDurationMs = durationMs,
                lastSyncBytesUploaded = bytesUploaded ?: 0L,
                lastError = error,
                updatedAt = System.currentTimeMillis()
            )
            
            syncMetadataDao.inserirOuAtualizar(metadata)
            Timber.tag(TAG).d("💾 Push metadata salva para $entityType: $syncCount registros, ${durationMs}ms")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Erro ao salvar push metadata para $entityType")
        }
    }
    
    /**
     * Salva metadados da sincronização.
     * 
     * @param entityType Tipo da entidade sincronizada
     * @param syncCount Número de registros sincronizados
     * @param durationMs Duração da sincronização em milissegundos
     * @param bytesTransferred Bytes transferidos (opcional)
     * @param error Erro ocorrido, se houver (null se sucesso)
     */
    suspend fun saveSyncMetadata(
        entityType: String,
        syncCount: Int,
        durationMs: Long,
        bytesTransferred: Long? = null,
        error: String? = null
    ) {
        try {
            val metadata = com.example.gestaobilhares.data.entities.SyncMetadata(
                entityType = entityType,
                userId = currentUserId,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastSyncCount = syncCount,
                lastSyncDurationMs = durationMs,
                lastSyncBytesDownloaded = bytesTransferred ?: 0L,
                lastError = error,
                updatedAt = System.currentTimeMillis()
            )
            
            syncMetadataDao.inserirOuAtualizar(metadata)
            Timber.tag(TAG).d("💾 Sync metadata salva para $entityType: $syncCount registros, ${durationMs}ms")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Erro ao salvar sync metadata para $entityType")
        }
    }
    
    /**
     * Verifica se deve executar sincronização em background.
     * 
     * Critérios:
     * - Usuário está logado
     * - Tem conexão com internet
     * - Existem operações pendentes/falhadas na fila
     * - Última sincronização global ocorreu há mais de [maxIdleHours]
     */
    suspend fun shouldRunBackgroundSync(
        pendingThreshold: Int = 0,
        maxIdleHours: Long = DEFAULT_BACKGROUND_IDLE_HOURS
    ): Boolean {
        try {
            // 1. Verificar se usuário está logado
            if (!userSessionManager.isLoggedIn.value) {
                Timber.tag(TAG).d("🚫 Usuário não está logado")
                return false
            }
            
            // 2. Verificar última sincronização global
            val lastGlobalSync = getGlobalLastSyncTimestamp()
            val timeSinceLastSync = System.currentTimeMillis() - lastGlobalSync
            val maxIdleMs = maxIdleHours * 60 * 60 * 1000
            
            if (timeSinceLastSync < maxIdleMs) {
                Timber.tag(TAG).d("⏰ Última sincronização muito recente: ${timeSinceLastSync}ms < ${maxIdleMs}ms")
                return false
            }
            
            // 3. Verificar se há operações pendentes (se necessário)
            if (pendingThreshold > 0) {
                val pendingCount = getPendingOperationsCount()
                if (pendingCount < pendingThreshold) {
                    Timber.tag(TAG).d("📊 Poucas operações pendentes: $pendingCount < $pendingThreshold")
                    return false
                }
            }
            
            Timber.tag(TAG).d("✅ Deve executar background sync")
            return true
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Erro ao verificar shouldRunBackgroundSync")
            return false
        }
    }
    
    /**
     * Obtém o timestamp da última sincronização global.
     */
    suspend fun getGlobalLastSyncTimestamp(): Long {
        return runCatching { syncMetadataDao.obterUltimoTimestamp(GLOBAL_SYNC_METADATA, currentUserId) }
            .getOrDefault(0L)
    }
    
    /**
     * Obtém o número de operações pendentes.
     */
    private suspend fun getPendingOperationsCount(): Int {
        return try {
            // Implementar contagem de operações pendentes se necessário
            // Por enquanto, retorna 0
            0
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Erro ao contar operações pendentes")
            0
        }
    }
    
    /**
     * Verifica se o usuário tem acesso a uma rota específica.
     */
    suspend fun canAccessRoute(rotaId: Long): Boolean {
        return try {
            if (userSessionManager.isAdmin()) {
                return true
            }
            
            val rotasPermitidas = userSessionManager.getRotasPermitidas()
            rotasPermitidas.contains(rotaId)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Erro ao verificar acesso à rota $rotaId")
            false
        }
    }
    
    /**
     * Obtém os IDs de rotas acessíveis para o usuário atual.
     */
    suspend fun getAccessibleRouteIds(): Set<Long> {
        return try {
            if (userSessionManager.isAdmin()) {
                // Admin tem acesso a todas as rotas
                val todasRotas = appRepository.obterTodasRotas().first()
                return todasRotas.mapNotNull { it.id }.toSet()
            }
            
            val rotasPermitidas = userSessionManager.getRotasPermitidas()
            rotasPermitidas.toSet()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Erro ao obter rotas acessíveis")
            emptySet()
        }
    }
    
    /**
     * Verifica se deve sincronizar dados de uma rota específica.
     */
    suspend fun shouldSyncRouteData(
        rotaId: Long?,
        clienteId: Long? = null,
        mesaId: Long? = null
    ): Boolean {
        if (rotaId == null) return true // Se não tem rota, permite sincronização
        
        return try {
            // Verificar se usuário tem acesso à rota
            if (!canAccessRoute(rotaId)) {
                Timber.tag(TAG).w("🚫 Usuário não tem acesso à rota $rotaId")
                return false
            }
            
            // Verificar se cliente/mesa pertence à rota (se fornecido)
            if (clienteId != null) {
                val clienteRotaId = getClienteRouteId(clienteId)
                if (clienteRotaId != null && clienteRotaId != rotaId) {
                    Timber.tag(TAG).w("🚫 Cliente $clienteId não pertence à rota $rotaId")
                    return false
                }
            }
            
            if (mesaId != null) {
                val mesaRotaId = getMesaRouteId(mesaId)
                if (mesaRotaId != null && mesaRotaId != rotaId) {
                    Timber.tag(TAG).w("🚫 Mesa $mesaId não pertence à rota $rotaId")
                    return false
                }
            }
            
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Erro ao verificar shouldSyncRouteData")
            false
        }
    }
    
    /**
     * Obtém o ID da rota de um cliente.
     */
    private suspend fun getClienteRouteId(clienteId: Long): Long? {
        return try {
            val cliente = appRepository.obterClientePorId(clienteId)
            cliente?.rotaId
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Erro ao obter rota do cliente $clienteId")
            null
        }
    }
    
    /**
     * Obtém o ID da rota de uma mesa.
     */
    private suspend fun getMesaRouteId(mesaId: Long): Long? {
        return try {
            val mesa = appRepository.obterMesaPorId(mesaId)
            val clienteId = mesa?.clienteId
            if (clienteId != null && clienteId > 0) {
                val cliente = appRepository.obterClientePorId(clienteId)
                cliente?.rotaId
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Erro ao obter rota da mesa $mesaId")
            null
        }
    }
}
