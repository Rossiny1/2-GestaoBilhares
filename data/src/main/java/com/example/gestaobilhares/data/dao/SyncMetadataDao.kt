package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.SyncMetadata
import kotlinx.coroutines.flow.Flow

/**
 * ✅ NOVO (2025): DAO para metadata de sincronização.
 * Gerencia informações sobre sincronizações incrementais por entidade.
 */
@Dao
interface SyncMetadataDao {
    /**
     * Obtém metadata de sincronização para um tipo de entidade específico.
     * Retorna null se nunca foi sincronizado.
     */
    @Query("SELECT * FROM sync_metadata WHERE entity_type = :entityType AND user_id = :userId LIMIT 1")
    suspend fun obterPorTipoEntidade(entityType: String, userId: Long): SyncMetadata?
    
    /**
     * Obtém metadata de sincronização para um tipo de entidade (Flow para observação reativa).
     */
    @Query("SELECT * FROM sync_metadata WHERE entity_type = :entityType AND user_id = :userId LIMIT 1")
    fun obterPorTipoEntidadeFlow(entityType: String, userId: Long): Flow<SyncMetadata?>
    
    /**
     * Obtém timestamp da última sincronização para um tipo de entidade.
     * Retorna 0L se nunca foi sincronizado.
     */
    @Query("SELECT last_sync_timestamp FROM sync_metadata WHERE entity_type = :entityType AND user_id = :userId LIMIT 1")
    suspend fun obterUltimoTimestamp(entityType: String, userId: Long): Long
    
    /**
     * Obtém todas as metadata de sincronização.
     */
    @Query("SELECT * FROM sync_metadata WHERE user_id = :userId ORDER BY entity_type ASC")
    suspend fun obterTodas(userId: Long): List<SyncMetadata>
    
    /**
     * Obtém todas as metadata de sincronização (Flow para observação reativa).
     */
    @Query("SELECT * FROM sync_metadata WHERE user_id = :userId ORDER BY entity_type ASC")
    fun obterTodasFlow(userId: Long): Flow<List<SyncMetadata>>
    
    /**
     * Insere ou atualiza metadata de sincronização.
     * Usa REPLACE para atualizar se já existir.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirOuAtualizar(metadata: SyncMetadata): Long
    
    /**
     * Atualiza timestamp da última sincronização.
     * Cria registro se não existir.
     */
    @Query("""
        INSERT OR REPLACE INTO sync_metadata 
        (entity_type, user_id, last_sync_timestamp, last_sync_count, last_sync_duration_ms, 
         last_sync_bytes_downloaded, last_sync_bytes_uploaded, last_error, updated_at)
        VALUES 
        (:entityType, :userId, :timestamp, :count, :durationMs, :bytesDownloaded, :bytesUploaded, :error, :updatedAt)
    """)
    suspend fun atualizarTimestamp(
        entityType: String,
        userId: Long,
        timestamp: Long,
        count: Int = 0,
        durationMs: Long = 0L,
        bytesDownloaded: Long = 0L,
        bytesUploaded: Long = 0L,
        error: String? = null,
        updatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Deleta metadata de sincronização para um tipo de entidade.
     * Útil para resetar sincronização.
     */
    @Query("DELETE FROM sync_metadata WHERE entity_type = :entityType AND user_id = :userId")
    suspend fun deletarPorTipoEntidade(entityType: String, userId: Long)
    
    /**
     * Deleta todas as metadata de sincronização.
     * Útil para resetar todas as sincronizações.
     */
    @Query("DELETE FROM sync_metadata WHERE user_id = :userId")
    suspend fun deletarTodas(userId: Long)
    
    @Query("DELETE FROM sync_metadata")
    suspend fun limparTudo()
    
    /**
     * Conta quantos tipos de entidade têm metadata de sincronização.
     */
    @Query("SELECT COUNT(*) FROM sync_metadata WHERE user_id = :userId")
    suspend fun contar(userId: Long): Int
}

