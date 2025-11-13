package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.SyncOperationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de fila de sincronização.
 * Gerencia operações enfileiradas para sincronização offline-first.
 */
@Dao
interface SyncOperationDao {
    /**
     * Obtém todas as operações pendentes ordenadas por timestamp (mais antigas primeiro)
     */
    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY timestamp ASC")
    fun obterOperacoesPendentes(): Flow<List<SyncOperationEntity>>
    
    /**
     * Obtém operações pendentes limitadas (para processamento em lotes)
     */
    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY timestamp ASC LIMIT :limit")
    suspend fun obterOperacoesPendentesLimitadas(limit: Int): List<SyncOperationEntity>
    
    /**
     * Insere uma nova operação na fila
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(operation: SyncOperationEntity): Long
    
    /**
     * Atualiza uma operação existente
     */
    @Update
    suspend fun atualizar(operation: SyncOperationEntity)
    
    /**
     * Deleta uma operação da fila
     */
    @Delete
    suspend fun deletar(operation: SyncOperationEntity)
    
    /**
     * Limpa operações completadas antigas (antes do timestamp especificado)
     */
    @Query("DELETE FROM sync_operations WHERE status = 'COMPLETED' AND timestamp < :beforeTimestamp")
    suspend fun limparOperacoesCompletadas(beforeTimestamp: Long)
    
    /**
     * Conta operações pendentes
     */
    @Query("SELECT COUNT(*) FROM sync_operations WHERE status = 'PENDING'")
    suspend fun contarOperacoesPendentes(): Int
    
    /**
     * Conta operações falhadas
     */
    @Query("SELECT COUNT(*) FROM sync_operations WHERE status = 'FAILED'")
    suspend fun contarOperacoesFalhadas(): Int
}

