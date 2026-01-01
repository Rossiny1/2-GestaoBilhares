package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Entidade que representa o histórico de manutenção de uma mesa.
 * Armazena todas as manutenções realizadas (pintura, troca de pano, troca de tabela, etc.)
 */
@Entity(tableName = "historico_manutencao_mesa")
data class HistoricoManutencaoMesa(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "mesa_id")
    val mesaId: Long, // ID da mesa
    
    @ColumnInfo(name = "numero_mesa")
    val numeroMesa: String, // Número da mesa para facilitar consultas
    
    @ColumnInfo(name = "tipo_manutencao")
    val tipoManutencao: TipoManutencao, // Tipo da manutenção realizada
    
    @ColumnInfo(name = "descricao")
    val descricao: String?, // Descrição detalhada da manutenção
    
    @ColumnInfo(name = "data_manutencao")
    val dataManutencao: Long, // Data em que a manutenção foi realizada
    
    @ColumnInfo(name = "responsavel")
    val responsavel: String?, // Quem realizou a manutenção
    
    @ColumnInfo(name = "observacoes")
    val observacoes: String?, // Observações adicionais
    
    @ColumnInfo(name = "custo")
    val custo: Double? = null, // Custo da manutenção (opcional)
    
    @ColumnInfo(name = "foto_antes")
    val fotoAntes: String? = null, // Caminho da foto antes da manutenção
    
    @ColumnInfo(name = "foto_depois")
    val fotoDepois: String? = null, // Caminho da foto depois da manutenção
    
    @ColumnInfo(name = "data_criacao")
    val dataCriacao: Long = System.currentTimeMillis() // Data de criação do registro
) : Serializable

/**
 * Enum que define os tipos de manutenção possíveis
 */
enum class TipoManutencao {
    PINTURA,           // Pintura da mesa
    TROCA_PANO,        // Troca de pano
    TROCA_TABELA,      // Troca de tabela
    REPARO_ESTRUTURAL, // Reparo estrutural
    LIMPEZA,           // Limpeza geral
    OUTROS             // Outros tipos de manutenção
}
