package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * ✅ FASE 3B: Entidade para fila de sincronização
 * Gerencia a fila de operações pendentes de sincronização
 * Seguindo melhores práticas Android 2025
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        androidx.room.Index(value = ["status", "priority"]),
        androidx.room.Index(value = ["entity_type", "entity_id"]),
        androidx.room.Index(value = ["scheduled_for"]),
        androidx.room.Index(value = ["created_at"]),
        androidx.room.Index(value = ["retry_count"])
    ]
)
data class SyncQueue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "entity_type")
    val entityType: String, // "Cliente", "Acerto", "Mesa", etc.
    
    @ColumnInfo(name = "entity_id")
    val entityId: Long,
    
    @ColumnInfo(name = "operation")
    val operation: String, // "CREATE", "UPDATE", "DELETE"

    @ColumnInfo(name = "payload")
    val payload: String, // JSON do dado a ser sincronizado

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "scheduled_for")
    val scheduledFor: Date = Date(), // Próxima tentativa de sincronização

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0, // Número de tentativas de sincronização

    @ColumnInfo(name = "status")
    val status: String = "PENDING", // PENDING, PROCESSING, FAILED, COMPLETED

    @ColumnInfo(name = "priority")
    val priority: Int = 0 // 0 = normal, 1 = alta
)

