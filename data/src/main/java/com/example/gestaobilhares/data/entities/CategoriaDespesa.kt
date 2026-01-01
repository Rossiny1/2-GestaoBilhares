package com.example.gestaobilhares.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entidade que representa uma categoria de despesa no banco de dados.
 * Permite criação e edição dinâmica de categorias pelo usuário.
 */
@Entity(
    tableName = "categorias_despesa",
    indices = [
        Index(value = ["nome"], unique = true)
    ]
)
data class CategoriaDespesa(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val nome: String,
    
    val descricao: String = "",
    
    val ativa: Boolean = true,
    
    val dataCriacao: Long = System.currentTimeMillis(),
    
    val dataAtualizacao: Long = System.currentTimeMillis(),
    
    val criadoPor: String = ""
) {
    /**
     * Verifica se a categoria está ativa
     */
    val estaAtiva: Boolean
        get() = ativa
}

/**
 * Data class para criação de nova categoria
 */
data class NovaCategoriaDespesa(
    val nome: String,
    val descricao: String = "",
    val criadoPor: String = ""
)

/**
 * Data class para edição de categoria
 */
data class EdicaoCategoriaDespesa(
    val id: Long,
    val nome: String,
    val descricao: String = "",
    val ativa: Boolean = true
) 