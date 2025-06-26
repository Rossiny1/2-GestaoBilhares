package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidade que representa um Cliente no banco de dados.
 * Clientes são estabelecimentos que alugam mesas de sinuca.
 */
@Entity(
    tableName = "clientes",
    foreignKeys = [
        ForeignKey(
            entity = Rota::class,
            parentColumns = ["id"],
            childColumns = ["rota_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["rota_id"])
    ]
)
data class Cliente(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "nome")
    val nome: String,
    
    @ColumnInfo(name = "nome_fantasia")
    val nomeFantasia: String? = null,
    
    @ColumnInfo(name = "cnpj")
    val cnpj: String? = null,
    
    @ColumnInfo(name = "telefone")
    val telefone: String? = null,
    
    @ColumnInfo(name = "email")
    val email: String? = null,
    
    @ColumnInfo(name = "endereco")
    val endereco: String? = null,
    
    @ColumnInfo(name = "cidade")
    val cidade: String? = null,
    
    @ColumnInfo(name = "estado")
    val estado: String? = null,
    
    @ColumnInfo(name = "cep")
    val cep: String? = null,
    
    @ColumnInfo(name = "rota_id")
    val rotaId: Long,
    
    @ColumnInfo(name = "valor_ficha")
    val valorFicha: Double = 0.0,
    
    @ColumnInfo(name = "debito_anterior")
    val debitoAnterior: Double = 0.0,
    
    @ColumnInfo(name = "ativo")
    val ativo: Boolean = true,
    
    @ColumnInfo(name = "observacoes")
    val observacoes: String? = null,
    
    @ColumnInfo(name = "data_cadastro")
    val dataCadastro: Date = Date(),
    
    @ColumnInfo(name = "data_ultima_atualizacao")
    val dataUltimaAtualizacao: Date = Date()
)

/**
 * Data class para representar um resumo de cliente na lista
 * Contém informações calculadas para exibição na UI
 */
data class ClienteResumo(
    val id: Long,
    val nome: String,
    val nomeFantasia: String?,
    val rotaId: Long,
    val valorFicha: Double,
    val debitoAtual: Double,
    val ativo: Boolean,
    val numeroMesasAtivas: Int,
    val ultimoAcerto: java.util.Date?,
    val diasSemAcerto: Int,
    val statusDebito: StatusDebito,
    val statusAcerto: StatusAcertoCliente
)

/**
 * Enum para indicar o status do débito do cliente
 */
enum class StatusDebito {
    EM_DIA,        // Débito <= R$ 50
    DEBITO_BAIXO,  // Débito entre R$ 50 e R$ 200
    DEBITO_ALTO,   // Débito > R$ 200
    CLIENTE_DEVEDOR // Débito muito alto > R$ 500
}

/**
 * Enum para indicar o status de acerto do cliente
 */
enum class StatusAcertoCliente {
    EM_DIA,           // Acerto há menos de 15 dias
    ATENCAO,          // Acerto entre 15-30 dias
    SEM_ACERTO_RECENTE, // Acerto há mais de 30 dias
    CLIENTE_INATIVO    // Sem acerto há mais de 60 dias
}

/**
 * Data class para resumos da lista de clientes
 */
data class ClienteListResumo(
    val totalClientes: Int,
    val clientesAtivos: Int,
    val clientesDebitoAlto: Int,
    val clientesSemAcertoRecente: Int,
    val valorTotalDebitos: Double,
    val percentualPagantes: Int
) 
