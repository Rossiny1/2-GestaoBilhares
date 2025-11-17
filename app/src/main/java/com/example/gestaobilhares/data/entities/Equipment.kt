package com.example.gestaobilhares.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade para representar equipamentos no banco de dados.
 * Campos: Nome, Descrição, Quantidade e Localização.
 */
@Entity(tableName = "equipments")
data class Equipment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val quantity: Int,
    val location: String? = null
)

