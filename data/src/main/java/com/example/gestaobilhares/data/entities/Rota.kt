package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Entidade que representa uma Rota no banco de dados.
 * Uma rota é um agrupamento lógico de clientes em uma região específica.
 */
@Entity(tableName = "rotas")
data class Rota(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    val id: Long = 0,
    
    @ColumnInfo(name = "nome")
    @SerializedName("nome")
    val nome: String,
    
    @ColumnInfo(name = "descricao")
    @SerializedName("descricao")
    val descricao: String = "",
    
    @ColumnInfo(name = "colaborador_responsavel")
    @SerializedName("colaborador_responsavel")
    val colaboradorResponsavel: String = "Não definido",
    
    @ColumnInfo(name = "cidades")
    @SerializedName("cidades")
    val cidades: String = "Não definido",
    
    @ColumnInfo(name = "ativa")
    @SerializedName("ativa")
    val ativa: Boolean = true,
    
    @ColumnInfo(name = "cor")
    @SerializedName("cor")
    val cor: String = "#6200EA", // Cor padrão roxa do tema
    
    @ColumnInfo(name = "data_criacao")
    @SerializedName("data_criacao")
    val dataCriacao: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "data_atualizacao")
    @SerializedName("data_atualizacao")
    val dataAtualizacao: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "status_atual")
    @SerializedName("status_atual")
    val statusAtual: StatusRota = StatusRota.PAUSADA,
    
    @ColumnInfo(name = "ciclo_acerto_atual")
    @SerializedName("ciclo_acerto_atual")
    val cicloAcertoAtual: Int = 1,
    
    @ColumnInfo(name = "ano_ciclo")
    @SerializedName("ano_ciclo")
    val anoCiclo: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    
    @ColumnInfo(name = "data_inicio_ciclo")
    @SerializedName("data_inicio_ciclo")
    val dataInicioCiclo: Long? = null,
    
    @ColumnInfo(name = "data_fim_ciclo")
    @SerializedName("data_fim_ciclo")
    val dataFimCiclo: Long? = null
) : Serializable

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
    val status: StatusRota = StatusRota.EM_ANDAMENTO,
    val cicloAtual: Int = 1,
    val dataInicioCiclo: Long? = null, // ✅ NOVO: Data de início do ciclo
    val dataFimCiclo: Long? = null     // ✅ NOVO: Data de fim do ciclo
) {
    /**
     * Formata a informação do ciclo atual com datas de início e fim
     * ✅ CORREÇÃO: Exibe "Acerto" em vez de "Ciclo" para maior clareza
     * ✅ NOVO: Mostra data de início e fim quando disponível
     */
    fun getCicloFormatado(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        
        val dataInicioFormatada = if (dataInicioCiclo != null) {
            dateFormat.format(java.util.Date(dataInicioCiclo))
        } else {
            "Data não definida"
        }
        
        val dataFimFormatada = if (dataFimCiclo != null) {
            dateFormat.format(java.util.Date(dataFimCiclo))
        } else {
            "Em andamento"
        }
        
        return "${cicloAtual}º Acerto - $dataInicioFormatada a $dataFimFormatada"
    }
}

/**
 * Enum para representar o status de uma rota
 */
enum class StatusRota {
    EM_ANDAMENTO,    // Rota iniciada, permite acertos
    PAUSADA,         // Rota não iniciada ou pausada
    FINALIZADA,      // Ciclo de acerto finalizado
    CONCLUIDA        // Rota completamente concluída (não usado atualmente)
}

/**
 * Data class para representar um ciclo de acerto
 */
data class CicloAcerto(
    val numero: Int,
    val ano: Int,
    val rotaId: Long,
    val rotaNome: String,
    val status: StatusRota,
    val dataInicio: Long? = null,
    val dataFim: Long? = null,
    val totalClientes: Int = 0,
    val clientesAcertados: Int = 0,
    val valorTotal: Double = 0.0
) {
    val titulo: String
        get() = "${numero}º Acerto"
    
    val tituloCompleto: String
        get() = "$titulo $rotaNome"
    
    val percentualConclusao: Int
        get() = if (totalClientes > 0) (clientesAcertados * 100) / totalClientes else 0
} 

