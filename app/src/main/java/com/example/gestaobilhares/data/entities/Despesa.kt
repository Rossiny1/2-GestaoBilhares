package com.example.gestaobilhares.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDateTime
import java.util.Date

/**
 * Entidade que representa uma despesa no banco de dados.
 * Despesas são vinculadas a uma rota específica e só podem ser criadas
 * entre os momentos de "Iniciar Rota" e "Finalizar Rota".
 * 
 * @property id Identificador único da despesa
 * @property rotaId ID da rota à qual a despesa está vinculada
 * @property descricao Descrição da despesa
 * @property valor Valor da despesa em reais
 * @property categoria Categoria da despesa (combustível, alimentação, etc.)
 * @property dataHora Data e hora da criação da despesa
 * @property observacoes Observações adicionais sobre a despesa
 * @property criadoPor ID do colaborador que criou a despesa
 */
@Entity(
    tableName = "despesas",
    foreignKeys = [
        ForeignKey(
            entity = Rota::class,
            parentColumns = ["id"],
            childColumns = ["rotaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["rotaId"]),
        // Novo: índice para cicloId
        Index(value = ["cicloId"]),
        Index(value = ["rotaId", "cicloId"]), // Índice composto para queries eficientes
        // Índices novos para globais
        Index(value = ["origemLancamento"]),
        Index(value = ["cicloAno"]),
        Index(value = ["cicloNumero"]),
        Index(value = ["cicloAno", "cicloNumero"]) 
    ]
)
data class Despesa(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val rotaId: Long,
    
    val descricao: String,
    
    val valor: Double,
    
    val categoria: String,
    
    // ✅ NOVO: VÍNCULO COM TIPO DE DESPESA
    val tipoDespesa: String = "",
    
    val dataHora: LocalDateTime = LocalDateTime.now(),
    
    val observacoes: String = "",
    
    val criadoPor: String = "", // ID do colaborador
    
    // NOVO: VÍNCULO COM CICLO DE ACERTO (id do ciclo)
    val cicloId: Long? = null,
    
    // ✅ NOVO: Origem do lançamento (ROTA ou GLOBAL)
    val origemLancamento: String = "ROTA",
    
    // ✅ NOVO: Identificação de ciclo global por ano/número
    val cicloAno: Int? = null,
    val cicloNumero: Int? = null,
    
    // ✅ NOVO: CAMPOS PARA FOTO DO COMPROVANTE
    val fotoComprovante: String? = null,
    val dataFotoComprovante: Date? = null
    ,
    // ✅ NOVO: Campos para despesas de viagem
    val veiculoId: Long? = null,
    val kmRodado: Long? = null,
    val litrosAbastecidos: Double? = null
)

/**
 * Enum para categorias de despesas (legado - será substituído por entidades dinâmicas)
 */
enum class CategoriaDespesaEnum(val displayName: String) {
    COMBUSTIVEL("Combustível"),
    ALIMENTACAO("Alimentação"),
    TRANSPORTE("Transporte"),
    MANUTENCAO("Manutenção"),
    MATERIAIS("Materiais"),
    OUTROS("Outros")
}

/**
 * Data class para resumo de despesas por rota
 * Usada para queries que fazem JOIN entre despesas e rotas
 */
data class DespesaResumo(
    val id: Long,
    val rotaId: Long,
    val descricao: String,
    val valor: Double,
    val categoria: String,
    val dataHora: LocalDateTime,
    val observacoes: String,
    val criadoPor: String,
    val nomeRota: String
) {
    /**
     * Converte para objeto Despesa
     */
    val despesa: Despesa
        get() = Despesa(
            id = id,
            rotaId = rotaId,
            descricao = descricao,
            valor = valor,
            categoria = categoria,
            dataHora = dataHora,
            observacoes = observacoes,
            criadoPor = criadoPor
        )
}

/**
 * Data class para resultado de soma por categoria
 */
data class CategoriaDespesaTotal(
    val categoria: String,
    val total: Double
)

/**
 * Data class para estatísticas de despesas
 */
data class EstatisticasDespesas(
    val totalDespesas: Double = 0.0,
    val quantidadeDespesas: Int = 0,
    val despesasPorCategoria: Map<String, Double> = emptyMap(),
    val periodoInicio: LocalDateTime? = null,
    val periodoFim: LocalDateTime? = null
) 
