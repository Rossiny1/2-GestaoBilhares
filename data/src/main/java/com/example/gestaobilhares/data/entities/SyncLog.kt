package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * ✅ FASE 3B: Entidade para log de sincronização
 * Registra todas as operações de sincronização para auditoria e debugging
 * Seguindo melhores práticas Android 2025
 */
@Entity(
    tableName = "sync_logs",
    indices = [
        androidx.room.Index(value = ["entity_type", "entity_id"]),
        androidx.room.Index(value = ["sync_status"]),
        androidx.room.Index(value = ["timestamp"]),
        androidx.room.Index(value = ["operation"])
    ]
)
data class SyncLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "entity_type")
    val entityType: String, // "Cliente", "Acerto", "Mesa", etc.
    
    @ColumnInfo(name = "entity_id")
    val entityId: Long,
    
    @ColumnInfo(name = "operation")
    val operation: String, // "CREATE", "UPDATE", "DELETE"
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String, // "SUCCESS", "FAILED", "PENDING"
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Date = Date(),
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "payload")
    val payload: String? = null // JSON do dado sincronizado
)

