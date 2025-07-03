package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

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
        )
    ],
    indices = [
        androidx.room.Index(value = ["cliente_id"]),
        androidx.room.Index(value = ["colaborador_id"])
    ]
)
data class Acerto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "cliente_id")
    val clienteId: Long,
    
    @ColumnInfo(name = "colaborador_id")
    val colaboradorId: Long? = null,
    
    @ColumnInfo(name = "data_acerto")
    val dataAcerto: Date = Date(),
    
    @ColumnInfo(name = "periodo_inicio")
    val periodoInicio: Date,
    
    @ColumnInfo(name = "periodo_fim")
    val periodoFim: Date,
    
    @ColumnInfo(name = "total_mesas")
    val totalMesas: Double = 0.0,
    
    @ColumnInfo(name = "debito_anterior")
    val debitoAnterior: Double = 0.0,
    
    @ColumnInfo(name = "valor_total")
    val valorTotal: Double = 0.0,
    
    @ColumnInfo(name = "desconto")
    val desconto: Double = 0.0,
    
    @ColumnInfo(name = "valor_com_desconto")
    val valorComDesconto: Double = 0.0,
    
    @ColumnInfo(name = "valor_recebido")
    val valorRecebido: Double = 0.0,
    
    @ColumnInfo(name = "debito_atual")
    val debitoAtual: Double = 0.0,
    
    @ColumnInfo(name = "status")
    val status: StatusAcerto = StatusAcerto.PENDENTE,
    
    @ColumnInfo(name = "observacoes")
    val observacoes: String? = null,
    
    @ColumnInfo(name = "data_criacao")
    val dataCriacao: Date = Date(),
    
    @ColumnInfo(name = "data_finalizacao")
    val dataFinalizacao: Date? = null,
    
    @ColumnInfo(name = "metodos_pagamento_json")
    val metodosPagamentoJson: String? = null
)

/**
 * Enum para status do acerto
 */
enum class StatusAcerto {
    PENDENTE,    // Acerto criado mas não finalizado
    FINALIZADO,  // Acerto finalizado
    CANCELADO    // Acerto cancelado
} 
