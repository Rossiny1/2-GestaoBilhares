package com.example.gestaobilhares.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade que representa uma Rota no banco de dados.
 * Uma rota é um agrupamento lógico de clientes em uma região específica.
 */
@Entity(tableName = "rotas")
data class Rota(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val nome: String,
    val descricao: String = "",
    val colaboradorResponsavel: String = "Não definido",
    val cidades: String = "Não definido",
    val ativa: Boolean = true,
    val cor: String = "#6200EA", // Cor padrão roxa do tema
    val dataCriacao: Long = System.currentTimeMillis(),
    val dataAtualizacao: Long = System.currentTimeMillis()
)

/**
 * Data class para representar informações resumidas de uma rota
 * incluindo contadores de clientes e pendências
 */
data class RotaResumo(
    val rota: Rota,
    val clientesAtivos: Int = 0,
    val pendencias: Int = 0,
    val valorAcertado: Double = 0.0,
    val quantidadeMesas: Int = 0,
    val percentualAcertados: Int = 0, // Percentual de clientes que acertaram (0-100)
    val status: StatusRota = StatusRota.EM_ANDAMENTO
)

/**
 * Enum para representar o status de uma rota
 */
enum class StatusRota {
    EM_ANDAMENTO,
    CONCLUIDA,
    FINALIZADA,
    PAUSADA
} 
