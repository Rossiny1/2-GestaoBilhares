package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade que representa um Pano no estoque.
 * Armazena informações sobre os panos disponíveis para uso nas mesas.
 */
@Entity(tableName = "panos_estoque")
data class PanoEstoque(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "numero")
    val numero: String, // Número do pano (ex: "P001", "P002")
    
    @ColumnInfo(name = "cor")
    val cor: String, // Cor do pano (ex: "Azul", "Verde", "Vermelho")
    
    @ColumnInfo(name = "tamanho")
    val tamanho: String, // Tamanho do pano (ex: "Pequeno", "Médio", "Grande")
    
    @ColumnInfo(name = "material")
    val material: String, // Material do pano (ex: "Veludo", "Feltro")
    
    @ColumnInfo(name = "disponivel")
    val disponivel: Boolean = true, // Se o pano está disponível para uso
    
    @ColumnInfo(name = "observacoes")
    val observacoes: String? // Observações sobre o pano
)
