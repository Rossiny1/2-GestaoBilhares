package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.SyncQueue
import kotlinx.coroutines.flow.Flow

/**
 * ✅ FASE 3B: DAO para operações de fila de sincronização
 * Gerencia a fila de operações pendentes de sincronização
 * Seguindo melhores práticas Android 2025
 */
@Dao
interface SyncQueueDao {

    /**
     * Inserir nova operação na fila
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirSyncQueue(syncQueue: SyncQueue): Long

    /**
     * Inserir múltiplas operações na fila
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirSyncQueues(syncQueues: List<SyncQueue>): List<Long>

    /**
     * Atualizar operação na fila
     */
    @Update
    suspend fun atualizarSyncQueue(syncQueue: SyncQueue)

    /**
     * Deletar operação da fila
     */
    @Delete
    suspend fun deletarSyncQueue(syncQueue: SyncQueue)

    /**
     * Buscar operação por ID
     */
    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun buscarSyncQueuePorId(id: Long): SyncQueue?

    /**
     * Buscar operações pendentes (prontas para processar)
     */
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' AND scheduled_for <= :timestampAtual ORDER BY priority DESC, created_at ASC")
    fun buscarOperacoesPendentes(timestampAtual: Long): Flow<List<SyncQueue>>

    /**
     * Buscar operações por status
     */
    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY created_at DESC")
    fun buscarOperacoesPorStatus(status: String): Flow<List<SyncQueue>>

    /**
     * Buscar operações por tipo de entidade
     */
    @Query("SELECT * FROM sync_queue WHERE entity_type = :entityType ORDER BY created_at DESC")
    fun buscarOperacoesPorTipoEntidade(entityType: String): Flow<List<SyncQueue>>

    /**
     * Buscar operações por ID da entidade
     */
    @Query("SELECT * FROM sync_queue WHERE entity_id = :entityId ORDER BY created_at DESC")
    fun buscarOperacoesPorEntidadeId(entityId: Long): Flow<List<SyncQueue>>

    /**
     * Buscar operações por prioridade
     */
    @Query("SELECT * FROM sync_queue WHERE priority = :priority ORDER BY created_at ASC")
    fun buscarOperacoesPorPrioridade(priority: Int): Flow<List<SyncQueue>>

    /**
     * Buscar operações com falha (para retry)
     */
    @Query("SELECT * FROM sync_queue WHERE status = 'FAILED' AND retry_count < :maxRetries ORDER BY created_at ASC")
    fun buscarOperacoesComFalha(maxRetries: Int = 3): Flow<List<SyncQueue>>

    /**
     * Buscar operações agendadas para processamento
     */
    @Query("SELECT * FROM sync_queue WHERE scheduled_for <= :timestampAtual AND status = 'PENDING' ORDER BY priority DESC, created_at ASC")
    fun buscarOperacoesAgendadas(timestampAtual: Long): Flow<List<SyncQueue>>

    /**
     * Marcar operação como processando
     */
    @Query("UPDATE sync_queue SET status = 'PROCESSING' WHERE id = :id")
    suspend fun marcarComoProcessando(id: Long)

    /**
     * Marcar operação como concluída
     */
    @Query("UPDATE sync_queue SET status = 'COMPLETED' WHERE id = :id")
    suspend fun marcarComoConcluida(id: Long)

    /**
     * Marcar operação como falhou e incrementar retry
     */
    @Query("UPDATE sync_queue SET status = 'FAILED', retry_count = retry_count + 1, scheduled_for = :proximaTentativa WHERE id = :id")
    suspend fun marcarComoFalhou(id: Long, proximaTentativa: Long)

    /**
     * Contar operações por status
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = :status")
    suspend fun contarOperacoesPorStatus(status: String): Int

    /**
     * Contar operações pendentes
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    suspend fun contarOperacoesPendentes(): Int

    /**
     * Limpar operações concluídas antigas
     */
    @Query("DELETE FROM sync_queue WHERE status = 'COMPLETED' AND created_at < :timestampLimite")
    suspend fun limparOperacoesConcluidas(timestampLimite: Long): Int

    /**
     * Limpar operações com muitas tentativas
     */
    @Query("DELETE FROM sync_queue WHERE retry_count >= :maxRetries AND status = 'FAILED'")
    suspend fun limparOperacoesComMuitasTentativas(maxRetries: Int = 5): Int

    /**
     * Buscar próxima operação para processar
     */
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' AND scheduled_for <= :timestampAtual ORDER BY priority DESC, created_at ASC LIMIT 1")
    suspend fun buscarProximaOperacao(timestampAtual: Long): SyncQueue?

    /**
     * Buscar todas as operações (para debugging)
     */
    @Query("SELECT * FROM sync_queue ORDER BY created_at DESC LIMIT :limite")
    fun buscarTodasOperacoes(limite: Int = 100): Flow<List<SyncQueue>>
}
