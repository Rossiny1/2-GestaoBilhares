package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.Date

/**
 * Entidade que representa o histórico de manutenção de um veículo.
 * Armazena todas as manutenções realizadas (troca de óleo, pneus, revisão, etc.)
 */
@Entity(tableName = "historico_manutencao_veiculo")
data class HistoricoManutencaoVeiculo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "veiculo_id")
    val veiculoId: Long, // ID do veículo
    
    @ColumnInfo(name = "tipo_manutencao")
    val tipoManutencao: String, // Tipo da manutenção (Troca de óleo, Pneus, Revisão, etc.)
    
    @ColumnInfo(name = "descricao")
    val descricao: String, // Descrição detalhada da manutenção
    
    @ColumnInfo(name = "data_manutencao")
    val dataManutencao: Date, // Data em que a manutenção foi realizada
    
    @ColumnInfo(name = "valor")
    val valor: Double, // Valor gasto na manutenção
    
    @ColumnInfo(name = "km_veiculo")
    val kmVeiculo: Long, // KM do veículo no momento da manutenção
    
    @ColumnInfo(name = "responsavel")
    val responsavel: String? = null, // Quem realizou a manutenção
    
    @ColumnInfo(name = "observacoes")
    val observacoes: String? = null, // Observações adicionais
    
    @ColumnInfo(name = "data_criacao")
    val dataCriacao: Date = Date() // Data de criação do registro
) : Serializable
