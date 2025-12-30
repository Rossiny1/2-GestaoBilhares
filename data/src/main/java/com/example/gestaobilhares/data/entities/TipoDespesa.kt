package com.example.gestaobilhares.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entidade que representa um tipo de despesa no banco de dados.
 * Tipos são subitens das categorias de despesas.
 */
@Entity(
    tableName = "tipos_despesa",
    foreignKeys = [
        ForeignKey(
            entity = CategoriaDespesa::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoriaId"]),
        Index(value = ["nome", "categoriaId"], unique = true)
    ]
)
data class TipoDespesa(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val categoriaId: Long,
    
    val nome: String,
    
    val descricao: String = "",
    
    val ativo: Boolean = true,
    
    val dataCriacao: Long = System.currentTimeMillis(),
    
    val dataAtualizacao: Long = System.currentTimeMillis(),
    
    val criadoPor: String = ""
) {
    /**
     * Verifica se o tipo está ativo
     */
    val estaAtivo: Boolean
        get() = ativo
}

/**
 * Data class para criação de novo tipo
 */
data class NovoTipoDespesa(
    val categoriaId: Long,
    val nome: String,
    val descricao: String = "",
    val criadoPor: String = ""
)

/**
 * Data class para edição de tipo
 */
data class EdicaoTipoDespesa(
    val id: Long,
    val categoriaId: Long,
    val nome: String,
    val descricao: String = "",
    val ativo: Boolean = true
)

/**
 * Data class para exibição de tipo com informações da categoria
 */
data class TipoDespesaComCategoria(
    val id: Long,
    val categoriaId: Long,
    val nome: String,
    val descricao: String,
    val ativo: Boolean,
    val categoriaNome: String
) {
    /**
     * Converte para objeto TipoDespesa
     */
    val tipoDespesa: TipoDespesa
        get() = TipoDespesa(
            id = id,
            categoriaId = categoriaId,
            nome = nome,
            descricao = descricao,
            ativo = ativo
        )
} 