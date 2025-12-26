package com.example.gestaobilhares.sync.handlers

import kotlinx.coroutines.flow.first

/**
 * Interface para handlers de sincronização de entidades específicas.
 * Segue o padrão Strategy para permitir diferentes implementações por entidade.
 * 
 * Cada handler é responsável por:
 * - Pull (sincronização do servidor para local)
 * - Push (sincronização do local para servidor)
 * - Validação e tratamento de conflitos
 */
interface SyncHandler {
    /**
     * Nome da entidade (ex: "mesas", "contratos")
     */
    val entityType: String
    
    /**
     * Sincroniza dados do servidor (Firestore) para o local (Room).
     * 
     * @param timestampOverride Timestamp opcional para forçar sincronização a partir de uma data específica
     * @return Result<Int> com o número de registros sincronizados
     */
    suspend fun pull(timestampOverride: Long? = null): Result<Int>
    
    /**
     * Sincroniza dados do local (Room) para o servidor (Firestore).
     * 
     * @return Result<Int> com o número de registros sincronizados
     */
    suspend fun push(): Result<Int>
}

/**
 * Resultado do processamento de um documento individual durante o pull.
 */
enum class ProcessResult {
    Synced,
    Skipped,
    Error
}

