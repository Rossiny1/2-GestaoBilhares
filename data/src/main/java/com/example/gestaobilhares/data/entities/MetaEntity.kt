package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "metas",
    foreignKeys = [
        ForeignKey(
            entity = Rota::class,
            parentColumns = ["id"],
            childColumns = ["rota_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CicloAcertoEntity::class,
            parentColumns = ["id"],
            childColumns = ["ciclo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("rota_id"), Index("ciclo_id")]
)
data class MetaEntity(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    val id: Long = 0,
    
    @ColumnInfo(name = "nome")
    @SerializedName("nome")
    val nome: String,
    
    @ColumnInfo(name = "tipo")
    @SerializedName("tipo")
    val tipo: String, // Ex: "FATURAMENTO", "CLIENTES_ACERTADOS"
    
    @ColumnInfo(name = "valor_objetivo")
    @SerializedName("valor_objetivo")
    val valorObjetivo: Double,
    
    @ColumnInfo(name = "valor_atual")
    @SerializedName("valor_atual")
    val valorAtual: Double = 0.0,
    
    @ColumnInfo(name = "data_inicio")
    @SerializedName("data_inicio")
    val dataInicio: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "data_fim")
    @SerializedName("data_fim")
    val dataFim: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "rota_id")
    @SerializedName("rota_id")
    val rotaId: Long,
    
    @ColumnInfo(name = "ciclo_id")
    @SerializedName("ciclo_id")
    val cicloId: Long
)
