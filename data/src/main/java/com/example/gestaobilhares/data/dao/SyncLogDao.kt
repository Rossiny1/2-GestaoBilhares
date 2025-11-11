package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.SyncLog
import kotlinx.coroutines.flow.Flow

/**
 * ✅ FASE 3B: DAO para operações de log de sincronização
 * Gerencia logs de todas as operações de sincronização para auditoria e debugging
 * Seguindo melhores práticas Android 2025
 */
@Dao
interface SyncLogDao {

    /**
     * Inserir novo log de sincronização
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirSyncLog(syncLog: SyncLog): Long

    /**
     * Inserir múltiplos logs de sincronização
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirSyncLogs(syncLogs: List<SyncLog>): List<Long>

    /**
     * Atualizar log de sincronização
     */
    @Update
    suspend fun atualizarSyncLog(syncLog: SyncLog)

    /**
     * Deletar log de sincronização
     */
    @Delete
    suspend fun deletarSyncLog(syncLog: SyncLog)

    /**
     * Buscar log por ID
     */
    @Query("SELECT * FROM sync_logs WHERE id = :id")
    suspend fun buscarSyncLogPorId(id: Long): SyncLog?

    /**
     * Buscar logs por tipo de entidade
     */
    @Query("SELECT * FROM sync_logs WHERE entity_type = :entityType ORDER BY timestamp DESC")
    fun buscarSyncLogsPorTipoEntidade(entityType: String): Flow<List<SyncLog>>

    /**
     * Buscar logs por ID da entidade
     */
    @Query("SELECT * FROM sync_logs WHERE entity_id = :entityId ORDER BY timestamp DESC")
    fun buscarSyncLogsPorEntidadeId(entityId: Long): Flow<List<SyncLog>>

    /**
     * Buscar logs por status de sincronização
     */
    @Query("SELECT * FROM sync_logs WHERE sync_status = :syncStatus ORDER BY timestamp DESC")
    fun buscarSyncLogsPorStatus(syncStatus: String): Flow<List<SyncLog>>

    /**
     * Buscar logs por operação
     */
    @Query("SELECT * FROM sync_logs WHERE operation = :operation ORDER BY timestamp DESC")
    fun buscarSyncLogsPorOperacao(operation: String): Flow<List<SyncLog>>

    /**
     * Buscar logs por período
     */
    @Query("SELECT * FROM sync_logs WHERE timestamp BETWEEN :dataInicio AND :dataFim ORDER BY timestamp DESC")
    fun buscarSyncLogsPorPeriodo(dataInicio: Long, dataFim: Long): Flow<List<SyncLog>>

    /**
     * Buscar logs com erro
     */
    @Query("SELECT * FROM sync_logs WHERE sync_status = 'FAILED' AND error_message IS NOT NULL ORDER BY timestamp DESC")
    fun buscarSyncLogsComErro(): Flow<List<SyncLog>>

    /**
     * Buscar logs recentes (últimas 24 horas)
     */
    @Query("SELECT * FROM sync_logs WHERE timestamp >= :timestampLimite ORDER BY timestamp DESC")
    fun buscarSyncLogsRecentes(timestampLimite: Long): Flow<List<SyncLog>>

    /**
     * Contar logs por status
     */
    @Query("SELECT COUNT(*) FROM sync_logs WHERE sync_status = :syncStatus")
    suspend fun contarSyncLogsPorStatus(syncStatus: String): Int

    /**
     * Deletar logs antigos (manter apenas últimos N dias)
     */
    @Query("DELETE FROM sync_logs WHERE timestamp < :timestampLimite")
    suspend fun deletarSyncLogsAntigos(timestampLimite: Long): Int

    /**
     * Buscar todos os logs (para debugging)
     */
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT :limite")
    fun buscarTodosSyncLogs(limite: Int = 100): Flow<List<SyncLog>>
}
