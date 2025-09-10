package com.example.gestaobilhares.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

@Entity(
    tableName = "contratos_locacao",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["clienteId"]), Index(value = ["numeroContrato"], unique = true)]
)
data class ContratoLocacao(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val numeroContrato: String, // Número único do contrato
    val clienteId: Long, // ID do cliente locatário
    
    // Dados do locador (fixos)
    val locadorNome: String = "BILHAR GLOBO R & A LTDA",
    val locadorCnpj: String = "34.994.884/0001-69",
    val locadorEndereco: String = "Rua João Pinheiro, nº 765, Bairro Centro, Montes Claros, MG",
    val locadorCep: String = "39.400-093",
    
    // Dados do locatário (do cliente)
    val locatarioNome: String,
    val locatarioCpf: String,
    val locatarioEndereco: String,
    val locatarioTelefone: String,
    val locatarioEmail: String,
    
    // Dados do contrato
    val valorMensal: Double, // Valor fixo mensal
    val diaVencimento: Int, // Dia do mês para vencimento
    val tipoPagamento: String, // "FIXO" ou "PERCENTUAL"
    val percentualReceita: Double? = null, // Se for percentual
    
    // Status e datas
    val dataContrato: Date,
    val dataInicio: Date,
    val status: String = "ATIVO", // ATIVO, RESCINDIDO, SUSPENSO
    val assinaturaLocador: String? = null, // Base64 da assinatura
    val assinaturaLocatario: String? = null, // Base64 da assinatura
    
    // Controle
    val dataCriacao: Date = Date(),
    val dataAtualizacao: Date = Date()
)

@Entity(
    tableName = "contrato_mesas",
    foreignKeys = [
        ForeignKey(
            entity = ContratoLocacao::class,
            parentColumns = ["id"],
            childColumns = ["contratoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Mesa::class,
            parentColumns = ["id"],
            childColumns = ["mesaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["contratoId"]), Index(value = ["mesaId"])]
)
data class ContratoMesa(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val contratoId: Long,
    val mesaId: Long,
    
    // Dados específicos da mesa no contrato
    val tipoEquipamento: String, // Sinuca, Jukebox, Pembolim
    val numeroSerie: String, // Número da mesa
    val valorFicha: Double? = null, // Se for por fichas
    val valorFixo: Double? = null // Se for valor fixo
)
