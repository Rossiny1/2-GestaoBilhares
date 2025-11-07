package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidade que representa um equipamento no inventário.
 * Campos: Nome, Descrição, Quantidade e Localização.
 */
@Entity(
    tableName = "equipments",
    indices = [
        // ✅ FASE PRIORIDADE ALTA: Índices para otimização de queries
        androidx.room.Index(value = ["name"]),
        androidx.room.Index(value = ["location"])
    ]
)
data class Equipment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "quantity")
    val quantity: Int,

    @ColumnInfo(name = "location")
    val location: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date()
)

