package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.Date
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

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
    @SerializedName("id")
    val id: Long = 0,
    
    @ColumnInfo(name = "numero")
    @SerializedName("numero")
    val numero: String,
    
    @ColumnInfo(name = "cliente_id")
    @SerializedName("cliente_id")
    val clienteId: Long? = null,
    
    @ColumnInfo(name = "fichas_inicial")
    @SerializedName("fichas_inicial")
    val fichasInicial: Int = 0,
    
    @ColumnInfo(name = "fichas_final") 
    @SerializedName("fichas_final")
    val fichasFinal: Int = 0,
    
    @ColumnInfo(name = "relogio_inicial")
    @SerializedName("relogio_inicial")
    val relogioInicial: Int = 0,
    
    @ColumnInfo(name = "relogio_final")
    @SerializedName("relogio_final")
    val relogioFinal: Int = 0,
    
    @ColumnInfo(name = "valor_fixo")
    @SerializedName("valor_fixo")
    val valorFixo: Double = 0.0,
    
    @ColumnInfo(name = "tipo_mesa")
    @SerializedName("tipo_mesa")
    val tipoMesa: TipoMesa = TipoMesa.SINUCA,
    
    @ColumnInfo(name = "tamanho")
    @SerializedName("tamanho")
    val tamanho: TamanhoMesa = TamanhoMesa.GRANDE,
    
    @ColumnInfo(name = "estado_conservacao")
    @SerializedName("estado_conservacao")
    val estadoConservacao: EstadoConservacao = EstadoConservacao.BOM,
    
    @ColumnInfo(name = "ativa")
    @SerializedName("ativa")
    val ativa: Boolean = true,
    
    @ColumnInfo(name = "observacoes")
    @SerializedName("observacoes")
    val observacoes: String? = null,
    
    @ColumnInfo(name = "data_instalacao")
    @SerializedName("data_instalacao")
    val dataInstalacao: Date = Date(),
    
    @ColumnInfo(name = "data_ultima_leitura")
    @SerializedName("data_ultima_leitura")
    val dataUltimaLeitura: Date = Date(),
    
    // ✅ NOVO: Campos para pano atual da mesa
    @ColumnInfo(name = "pano_atual_id")
    @SerializedName("pano_atual_id")
    val panoAtualId: Long? = null,
    
    @ColumnInfo(name = "data_ultima_troca_pano")
    @SerializedName("data_ultima_troca_pano")
    val dataUltimaTrocaPano: Date? = null
) : Parcelable, Serializable 

/**
 * Enum para tipos de mesa
 */
enum class TipoMesa {
    SINUCA,
    JUKEBOX,
    PEMBOLIM,
    OUTROS
}

/**
 * Enum para tamanhos de mesa
 */
enum class TamanhoMesa {
    PEQUENA,
    MEDIA,
    GRANDE
}

/**
 * Enum para estado de conservação
 */
enum class EstadoConservacao {
    OTIMO,
    BOM,
    RUIM
}
