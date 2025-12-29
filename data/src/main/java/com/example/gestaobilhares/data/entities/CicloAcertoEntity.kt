package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Entidade que representa um ciclo de acerto finalizado no banco de dados.
 * Permite relatórios históricos e análise de desempenho por ciclo.
 * 
 * ✅ FASE 8A: NOVA ENTIDADE PARA HISTÓRICO DE CICLOS
 */
@Entity(
    tableName = "ciclos_acerto",
    foreignKeys = [
        ForeignKey(
            entity = Rota::class,
            parentColumns = ["id"],
            childColumns = ["rota_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["rota_id"]),
        androidx.room.Index(value = ["ano"]),
        androidx.room.Index(value = ["numero_ciclo"]),
        androidx.room.Index(value = ["rota_id", "ano", "numero_ciclo"]), // Índice composto único
        androidx.room.Index(value = ["data_inicio"]),
        androidx.room.Index(value = ["data_fim"])
    ]
)
data class CicloAcertoEntity(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    val id: Long = 0,
    
    @ColumnInfo(name = "rota_id")
    @SerializedName("rota_id")
    val rotaId: Long,
    
    @ColumnInfo(name = "numero_ciclo")
    @SerializedName("numero_ciclo")
    val numeroCiclo: Int,
    
    @ColumnInfo(name = "ano")
    @SerializedName("ano")
    val ano: Int,
    
    @ColumnInfo(name = "data_inicio")
    @SerializedName("data_inicio")
    val dataInicio: Date,
    
    @ColumnInfo(name = "data_fim")
    @SerializedName("data_fim")
    val dataFim: Date,
    
    @ColumnInfo(name = "status")
    @SerializedName("status")
    val status: StatusCicloAcerto = StatusCicloAcerto.FINALIZADO,
    
    @ColumnInfo(name = "total_clientes")
    @SerializedName("total_clientes")
    val totalClientes: Int = 0,
    
    @ColumnInfo(name = "clientes_acertados")
    @SerializedName("clientes_acertados")
    val clientesAcertados: Int = 0,
    
    @ColumnInfo(name = "valor_total_acertado")
    @SerializedName("valor_total_acertado")
    val valorTotalAcertado: Double = 0.0,
    
    @ColumnInfo(name = "valor_total_despesas")
    @SerializedName("valor_total_despesas")
    val valorTotalDespesas: Double = 0.0,
    
    @ColumnInfo(name = "lucro_liquido")
    @SerializedName("lucro_liquido")
    val lucroLiquido: Double = 0.0,
    
    @ColumnInfo(name = "debito_total")
    @SerializedName("debito_total")
    val debitoTotal: Double = 0.0,
    
    @ColumnInfo(name = "observacoes")
    @SerializedName("observacoes")
    val observacoes: String? = null,
    
    @ColumnInfo(name = "criado_por")
    @SerializedName("criado_por")
    val criadoPor: String? = null,
    
    @ColumnInfo(name = "data_criacao")
    @SerializedName("data_criacao")
    val dataCriacao: Date = Date(),
    
    @ColumnInfo(name = "data_atualizacao")
    @SerializedName("data_atualizacao")
    val dataAtualizacao: Date = Date()
) {
    /**
     * Calcula o percentual de conclusão do ciclo
     */
    val percentualConclusao: Int
        get() = if (totalClientes > 0) (clientesAcertados * 100) / totalClientes else 0
    
    /**
     * Calcula o título do ciclo (ex: "1º Acerto 2025")
     */
    val titulo: String
        get() = "${numeroCiclo}º Acerto $ano"
    
    /**
     * Verifica se o ciclo está em andamento
     */
    val estaEmAndamento: Boolean
        get() = status == StatusCicloAcerto.EM_ANDAMENTO
    
    /**
     * Verifica se o ciclo está finalizado
     */
    val estaFinalizado: Boolean
        get() = status == StatusCicloAcerto.FINALIZADO
}

/**
 * Enum para status do ciclo de acerto
 */
enum class StatusCicloAcerto {
    EM_ANDAMENTO,    // Ciclo iniciado mas não finalizado
    FINALIZADO,      // Ciclo finalizado com sucesso
    CANCELADO,       // Ciclo cancelado
    PLANEJADO        // Ciclo planejado para o futuro
} 