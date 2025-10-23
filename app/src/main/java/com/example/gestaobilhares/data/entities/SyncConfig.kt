package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * ✅ FASE 3B: Entidade para configurações de sincronização
 * Armazena metadados e configurações globais da sincronização
 * Seguindo melhores práticas Android 2025
 */
@Entity(
    tableName = "sync_config",
    indices = [
        androidx.room.Index(value = ["key"], unique = true)
    ]
)
data class SyncConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "key")
    val key: String, // Ex: "last_sync_timestamp_clientes", "sync_interval"

    @ColumnInfo(name = "value")
    val value: String, // Valor da configuração (timestamp, intervalo, etc.)

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date = Date()
)
