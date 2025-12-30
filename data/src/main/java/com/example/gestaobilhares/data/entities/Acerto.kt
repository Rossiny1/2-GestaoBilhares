package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Entidade que representa um Acerto no banco de dados.
 * Acertos são registros financeiros periódicos por cliente.
 */
@Entity(
    tableName = "acertos",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["cliente_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Colaborador::class,
            parentColumns = ["id"],
            childColumns = ["colaborador_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        // ✅ FASE 8A: FOREIGN KEY PARA ROTA
        ForeignKey(
            entity = Rota::class,
            parentColumns = ["id"],
            childColumns = ["rota_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        androidx.room.Index(value = ["cliente_id"]),
        androidx.room.Index(value = ["colaborador_id"]),
        // Novo: índices para rota e cicloId
        androidx.room.Index(value = ["rota_id"]),
        androidx.room.Index(value = ["ciclo_id"]),
        androidx.room.Index(value = ["rota_id", "ciclo_id"]) // Índice composto para queries eficientes
    ]
)
data class Acerto(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    val id: Long = 0,
    
    @ColumnInfo(name = "cliente_id")
    @SerializedName("cliente_id")
    val clienteId: Long,
    
    @ColumnInfo(name = "colaborador_id")
    @SerializedName("colaborador_id")
    val colaboradorId: Long? = null,
    
    @ColumnInfo(name = "data_acerto")
    @SerializedName("data_acerto")
    val dataAcerto: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "periodo_inicio")
    @SerializedName("periodo_inicio")
    val periodoInicio: Long,
    
    @ColumnInfo(name = "periodo_fim")
    @SerializedName("periodo_fim")
    val periodoFim: Long,
    
    @ColumnInfo(name = "total_mesas")
    @SerializedName("total_mesas")
    val totalMesas: Double = 0.0,
    
    @ColumnInfo(name = "debito_anterior")
    @SerializedName("debito_anterior")
    val debitoAnterior: Double = 0.0,
    
    @ColumnInfo(name = "valor_total")
    @SerializedName("valor_total")
    val valorTotal: Double = 0.0,
    
    @ColumnInfo(name = "desconto")
    @SerializedName("desconto")
    val desconto: Double = 0.0,
    
    @ColumnInfo(name = "valor_com_desconto")
    @SerializedName("valor_com_desconto")
    val valorComDesconto: Double = 0.0,
    
    @ColumnInfo(name = "valor_recebido")
    @SerializedName("valor_recebido")
    val valorRecebido: Double = 0.0,
    
    @ColumnInfo(name = "debito_atual")
    @SerializedName("debito_atual")
    val debitoAtual: Double = 0.0,
    
    @ColumnInfo(name = "status")
    @SerializedName("status")
    val status: StatusAcerto = StatusAcerto.PENDENTE,
    
    @ColumnInfo(name = "observacoes")
    @SerializedName("observacoes")
    val observacoes: String? = null,
    
    @ColumnInfo(name = "data_criacao")
    @SerializedName("data_criacao")
    val dataCriacao: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "data_finalizacao")
    @SerializedName("data_finalizacao")
    val dataFinalizacao: Long? = null,
    
    @ColumnInfo(name = "metodos_pagamento_json")
    @SerializedName("metodos_pagamento_json")
    val metodosPagamentoJson: String? = null,
    
    // ✅ NOVOS CAMPOS: Para resolver problema de dados perdidos
    @ColumnInfo(name = "representante")
    @SerializedName("representante")
    val representante: String? = null,
    
    @ColumnInfo(name = "tipo_acerto")
    @SerializedName("tipo_acerto")
    val tipoAcerto: String? = null,
    
    @ColumnInfo(name = "pano_trocado")
    @SerializedName("pano_trocado")
    val panoTrocado: Boolean = false,
    
    @ColumnInfo(name = "numero_pano")
    @SerializedName("numero_pano")
    val numeroPano: String? = null,
    
    @ColumnInfo(name = "dados_extras_json")
    @SerializedName("dados_extras_json")
    val dadosExtrasJson: String? = null,
    
    // NOVO: VÍNCULO COM CICLO DE ACERTO (id do ciclo)
    @ColumnInfo(name = "rota_id")
    @SerializedName("rota_id")
    val rotaId: Long? = null,
    
    @ColumnInfo(name = "ciclo_id")
    @SerializedName("ciclo_id")
    val cicloId: Long? = null
)

/**
 * Enum para status do acerto
 */
enum class StatusAcerto {
    PENDENTE,    // Acerto criado mas não finalizado
    FINALIZADO,  // Acerto finalizado
    CANCELADO    // Acerto cancelado
} 
