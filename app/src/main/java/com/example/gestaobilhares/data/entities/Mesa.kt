package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Entidade que representa uma Mesa de sinuca no banco de dados.
 * Mesas pertencem a clientes e têm contadores de fichas.
 */
@Parcelize
@Entity(
    tableName = "mesas",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["cliente_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["cliente_id"])
    ]
)
data class Mesa(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "numero")
    val numero: String,
    
    @ColumnInfo(name = "cliente_id")
    val clienteId: Long? = null,
    
    @ColumnInfo(name = "fichas_inicial")
    val fichasInicial: Int = 0,
    
    @ColumnInfo(name = "fichas_final") 
    val fichasFinal: Int = 0,
    
    @ColumnInfo(name = "relogio_inicial")
    val relogioInicial: Int = 0,
    
    @ColumnInfo(name = "relogio_final")
    val relogioFinal: Int = 0,
    
    @ColumnInfo(name = "valor_fixo")
    val valorFixo: Double = 0.0,
    
    @ColumnInfo(name = "tipo_mesa")
    val tipoMesa: TipoMesa = TipoMesa.SINUCA,
    
    @ColumnInfo(name = "tamanho")
    val tamanho: TamanhoMesa = TamanhoMesa.GRANDE,
    
    @ColumnInfo(name = "estado_conservacao")
    val estadoConservacao: EstadoConservacao = EstadoConservacao.BOM,
    
    @ColumnInfo(name = "ativa")
    val ativa: Boolean = true,
    
    @ColumnInfo(name = "observacoes")
    val observacoes: String? = null,
    
    @ColumnInfo(name = "data_instalacao")
    val dataInstalacao: Date = Date(),
    
    @ColumnInfo(name = "data_ultima_leitura")
    val dataUltimaLeitura: Date = Date()
) : Parcelable

/**
 * Enum para tipos de mesa
 */
@Parcelize
enum class TipoMesa : Parcelable {
    SINUCA,
    MAQUINA_MUSICA,
    PEMBOLIM,
    SNOOKER,
    POOL,
    BILHAR,
    OUTROS
}

/**
 * Enum para tamanhos de mesa
 */
@Parcelize
enum class TamanhoMesa : Parcelable {
    PEQUENA,
    GRANDE
}

/**
 * Enum para estado de conservação
 */
@Parcelize
enum class EstadoConservacao : Parcelable {
    OTIMO,
    BOM,
    RUIM
} 
