package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Entidade que representa o histórico de abastecimento de um veículo.
 * Armazena todos os abastecimentos realizados
 */
@Entity(tableName = "historico_combustivel_veiculo")
data class HistoricoCombustivelVeiculo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "veiculo_id")
    val veiculoId: Long, // ID do veículo
    
    @ColumnInfo(name = "data_abastecimento")
    val dataAbastecimento: Long, // Data do abastecimento
    
    @ColumnInfo(name = "litros")
    val litros: Double, // Quantidade de litros abastecida
    
    @ColumnInfo(name = "valor")
    val valor: Double, // Valor gasto no abastecimento
    
    @ColumnInfo(name = "km_veiculo")
    val kmVeiculo: Long, // KM do veículo no momento do abastecimento
    
    @ColumnInfo(name = "km_rodado")
    val kmRodado: Double, // KM rodado desde o último abastecimento
    
    @ColumnInfo(name = "posto")
    val posto: String? = null, // Nome do posto onde foi abastecido
    
    @ColumnInfo(name = "observacoes")
    val observacoes: String? = null, // Observações adicionais
    
    @ColumnInfo(name = "data_criacao")
    val dataCriacao: Long = System.currentTimeMillis() // Data de criação do registro
) : Serializable
