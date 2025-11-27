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
    @Query("SELECT * FROM sync_metadata WHERE entity_type = :entityType LIMIT 1")
    suspend fun obterPorTipoEntidade(entityType: String): SyncMetadata?
    
    /**
     * Obtém metadata de sincronização para um tipo de entidade (Flow para observação reativa).
     */
    @Query("SELECT * FROM sync_metadata WHERE entity_type = :entityType LIMIT 1")
    fun obterPorTipoEntidadeFlow(entityType: String): Flow<SyncMetadata?>
    
    /**
     * Obtém timestamp da última sincronização para um tipo de entidade.
     * Retorna 0L se nunca foi sincronizado.
     */
    @Query("SELECT last_sync_timestamp FROM sync_metadata WHERE entity_type = :entityType LIMIT 1")
    suspend fun obterUltimoTimestamp(entityType: String): Long
    
    /**
     * Obtém todas as metadata de sincronização.
     */
    @Query("SELECT * FROM sync_metadata ORDER BY entity_type ASC")
    suspend fun obterTodas(): List<SyncMetadata>
    
    /**
     * Obtém todas as metadata de sincronização (Flow para observação reativa).
     */
    @Query("SELECT * FROM sync_metadata ORDER BY entity_type ASC")
    fun obterTodasFlow(): Flow<List<SyncMetadata>>
    
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
        (entity_type, last_sync_timestamp, last_sync_count, last_sync_duration_ms, 
         last_sync_bytes_downloaded, last_sync_bytes_uploaded, last_error, updated_at)
        VALUES 
        (:entityType, :timestamp, :count, :durationMs, :bytesDownloaded, :bytesUploaded, :error, :updatedAt)
    """)
    suspend fun atualizarTimestamp(
        entityType: String,
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
    @Query("DELETE FROM sync_metadata WHERE entity_type = :entityType")
    suspend fun deletarPorTipoEntidade(entityType: String)
    
    /**
     * Deleta todas as metadata de sincronização.
     * Útil para resetar todas as sincronizações.
     */
    @Query("DELETE FROM sync_metadata")
    suspend fun deletarTodas()
    
    /**
     * Conta quantos tipos de entidade têm metadata de sincronização.
     */
    @Query("SELECT COUNT(*) FROM sync_metadata")
    suspend fun contar(): Int
}

