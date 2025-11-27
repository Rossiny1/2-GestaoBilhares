package com.example.gestaobilhares.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "metas",
    foreignKeys = [
        ForeignKey(
            entity = Rota::class,
            parentColumns = ["id"],
            childColumns = ["rotaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CicloAcertoEntity::class,
            parentColumns = ["id"],
            childColumns = ["cicloId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("rotaId"), Index("cicloId")]
)
data class Meta(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val tipo: String, // Ex: "FATURAMENTO", "CLIENTES_ACERTADOS"
    val valorObjetivo: Double,
    var valorAtual: Double = 0.0,
    val dataInicio: Date,
    val dataFim: Date,
    val rotaId: Long,
    val cicloId: Long
)
