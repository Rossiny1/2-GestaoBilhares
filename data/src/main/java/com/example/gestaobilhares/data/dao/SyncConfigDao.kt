package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.SyncConfig
import kotlinx.coroutines.flow.Flow

/**
 * ✅ FASE 3B: DAO para operações de configurações de sincronização
 * Gerencia configurações globais e metadados da sincronização
 * Seguindo melhores práticas Android 2025
 */
@Dao
interface SyncConfigDao {

    /**
     * Inserir nova configuração
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirSyncConfig(syncConfig: SyncConfig): Long

    /**
     * Inserir múltiplas configurações
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirSyncConfigs(syncConfigs: List<SyncConfig>): List<Long>

    /**
     * Atualizar configuração
     */
    @Update
    suspend fun atualizarSyncConfig(syncConfig: SyncConfig)

    /**
     * Deletar configuração
     */
    @Delete
    suspend fun deletarSyncConfig(syncConfig: SyncConfig)

    /**
     * Buscar configuração por chave
     */
    @Query("SELECT * FROM sync_config WHERE key = :key")
    suspend fun buscarSyncConfigPorChave(key: String): SyncConfig?

    /**
     * Buscar configuração por chave (Flow para observação)
     */
    @Query("SELECT * FROM sync_config WHERE key = :key")
    fun buscarSyncConfigPorChaveFlow(key: String): Flow<SyncConfig?>

    /**
     * Buscar todas as configurações
     */
    @Query("SELECT * FROM sync_config ORDER BY key ASC")
    fun buscarTodasSyncConfigs(): Flow<List<SyncConfig>>

    /**
     * Buscar configurações por padrão de chave
     */
    @Query("SELECT * FROM sync_config WHERE key LIKE :pattern ORDER BY key ASC")
    fun buscarSyncConfigsPorPadrao(pattern: String): Flow<List<SyncConfig>>

    /**
     * Atualizar valor de configuração por chave
     */
    @Query("UPDATE sync_config SET value = :value, last_updated = :timestamp WHERE key = :key")
    suspend fun atualizarValorConfig(key: String, value: String, timestamp: Long)

    /**
     * Deletar configuração por chave
     */
    @Query("DELETE FROM sync_config WHERE key = :key")
    suspend fun deletarSyncConfigPorChave(key: String)

    /**
     * Verificar se configuração existe
     */
    @Query("SELECT COUNT(*) FROM sync_config WHERE key = :key")
    suspend fun configuracaoExiste(key: String): Int

    /**
     * Buscar timestamp da última sincronização por tipo
     */
    @Query("SELECT value FROM sync_config WHERE key = :chaveTimestamp")
    suspend fun buscarUltimoTimestampSync(chaveTimestamp: String): String?

    /**
     * Atualizar timestamp da última sincronização
     */
    @Query("INSERT OR REPLACE INTO sync_config (key, value, last_updated) VALUES (:chaveTimestamp, :timestamp, :timestampAtual)")
    suspend fun atualizarUltimoTimestampSync(chaveTimestamp: String, timestamp: String, timestampAtual: Long)

    /**
     * Buscar configurações de sincronização específicas
     */
    @Query("SELECT * FROM sync_config WHERE key IN ('sync_interval', 'max_retries', 'batch_size', 'sync_enabled') ORDER BY key ASC")
    fun buscarConfiguracoesSync(): Flow<List<SyncConfig>>

    /**
     * Inicializar configurações padrão
     */
    @Query("""
        INSERT OR IGNORE INTO sync_config (key, value, last_updated) VALUES 
        ('sync_interval', '300000', :timestamp),
        ('max_retries', '3', :timestamp),
        ('batch_size', '50', :timestamp),
        ('sync_enabled', 'true', :timestamp),
        ('last_sync_timestamp_clientes', '0', :timestamp),
        ('last_sync_timestamp_acertos', '0', :timestamp),
        ('last_sync_timestamp_mesas', '0', :timestamp),
        ('last_sync_timestamp_rotas', '0', :timestamp)
    """)
    suspend fun inicializarConfiguracoesPadrao(timestamp: Long)

    /**
     * Limpar configurações antigas
     */
    @Query("DELETE FROM sync_config WHERE last_updated < :timestampLimite")
    suspend fun limparConfiguracoesAntigas(timestampLimite: Long): Int

    /**
     * Contar total de configurações
     */
    @Query("SELECT COUNT(*) FROM sync_config")
    suspend fun contarTotalConfiguracoes(): Int
}
