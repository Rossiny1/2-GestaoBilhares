package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade que representa uma operação de sincronização enfileirada.
 * Usada para implementar sincronização offline-first.
 */
@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "operation_type")
    val operationType: String, // CREATE, UPDATE, DELETE
    
    @ColumnInfo(name = "entity_type")
    val entityType: String, // Cliente, Acerto, Mesa, etc.
    
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    
    @ColumnInfo(name = "entity_data")
    val entityData: String, // JSON serializado
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    
    @ColumnInfo(name = "max_retries")
    val maxRetries: Int = 3,
    
    @ColumnInfo(name = "status")
    val status: String = SyncOperationStatus.PENDING.name // PENDING, PROCESSING, COMPLETED, FAILED
)

/**
 * Status da operação de sincronização
 */
enum class SyncOperationStatus {
    PENDING,      // Aguardando processamento
    PROCESSING,   // Sendo processada
    COMPLETED,    // Concluída com sucesso
    FAILED        // Falhou após todas as tentativas
}

