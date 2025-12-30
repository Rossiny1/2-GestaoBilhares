package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Entidade que representa o relacionamento entre Pano e Mesa.
 * Armazena o histórico de panos utilizados em cada mesa.
 */
@Entity(
    tableName = "pano_mesas",
    foreignKeys = [
        ForeignKey(
            entity = Mesa::class,
            parentColumns = ["id"],
            childColumns = ["mesa_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PanoEstoque::class,
            parentColumns = ["id"],
            childColumns = ["pano_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["mesa_id"]),
        androidx.room.Index(value = ["pano_id"]),
        androidx.room.Index(value = ["mesa_id", "ativo"], unique = true)
    ]
)
data class PanoMesa(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "mesa_id")
    val mesaId: Long,
    
    @ColumnInfo(name = "pano_id")
    val panoId: Long,
    
    @ColumnInfo(name = "data_troca")
    val dataTroca: Long,
    
    @ColumnInfo(name = "ativo")
    val ativo: Boolean = true, // Se é o pano atual da mesa
    
    @ColumnInfo(name = "observacoes")
    val observacoes: String? = null,
    
    @ColumnInfo(name = "data_criacao")
    val dataCriacao: Long = System.currentTimeMillis()
)
