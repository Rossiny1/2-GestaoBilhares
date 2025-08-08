package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidade que representa o relacionamento entre Acerto e Mesa.
 * Armazena o histórico detalhado de cada mesa em cada acerto.
 */
@Entity(
    tableName = "acerto_mesas",
    foreignKeys = [
        ForeignKey(
            entity = Acerto::class,
            parentColumns = ["id"],
            childColumns = ["acerto_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Mesa::class,
            parentColumns = ["id"],
            childColumns = ["mesa_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["acerto_id"]),
        androidx.room.Index(value = ["mesa_id"]),
        androidx.room.Index(value = ["acerto_id", "mesa_id"], unique = true)
    ]
)
data class AcertoMesa(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "acerto_id")
    val acertoId: Long,
    
    @ColumnInfo(name = "mesa_id")
    val mesaId: Long,
    
    @ColumnInfo(name = "relogio_inicial")
    val relogioInicial: Int,
    
    @ColumnInfo(name = "relogio_final")
    val relogioFinal: Int,
    
    @ColumnInfo(name = "fichas_jogadas")
    val fichasJogadas: Int,
    
    @ColumnInfo(name = "valor_fixo")
    val valorFixo: Double = 0.0,
    
    @ColumnInfo(name = "valor_ficha")
    val valorFicha: Double = 0.0,
    
    @ColumnInfo(name = "comissao_ficha")
    val comissaoFicha: Double = 0.0,
    
    @ColumnInfo(name = "subtotal")
    val subtotal: Double,
    
    @ColumnInfo(name = "com_defeito")
    val comDefeito: Boolean = false,
    
    @ColumnInfo(name = "relogio_reiniciou")
    val relogioReiniciou: Boolean = false,
    
    @ColumnInfo(name = "observacoes")
    val observacoes: String? = null,
    
    @ColumnInfo(name = "foto_relogio_final")
    val fotoRelogioFinal: String? = null,
    
    @ColumnInfo(name = "data_foto")
    val dataFoto: Date? = null,
    
    @ColumnInfo(name = "data_criacao")
    val dataCriacao: Date = Date()
) 