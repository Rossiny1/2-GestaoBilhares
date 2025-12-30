package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoricoCombustivelVeiculoDao {
    // ✅ NOVO: Listar todos os históricos (para uso no ViewModel como no código antigo)
    @Query("SELECT * FROM historico_combustivel_veiculo ORDER BY data_abastecimento DESC")
    fun listarTodos(): Flow<List<HistoricoCombustivelVeiculo>>

    @Query("SELECT * FROM historico_combustivel_veiculo WHERE id = :id")
    suspend fun buscarPorId(id: Long): HistoricoCombustivelVeiculo?
    
    @Query("SELECT * FROM historico_combustivel_veiculo WHERE veiculo_id = :veiculoId ORDER BY data_abastecimento DESC")
    fun listarPorVeiculo(veiculoId: Long): Flow<List<HistoricoCombustivelVeiculo>>
    
    // ✅ CORREÇÃO: Query corrigida para trabalhar com Date do Java
    @Query("SELECT * FROM historico_combustivel_veiculo WHERE veiculo_id = :veiculoId AND strftime('%Y', data_abastecimento / 1000, 'unixepoch') = :ano ORDER BY data_abastecimento DESC")
    fun listarPorVeiculoEAno(veiculoId: Long, ano: String): Flow<List<HistoricoCombustivelVeiculo>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(historico: HistoricoCombustivelVeiculo): Long
    
    @Update
    suspend fun atualizar(historico: HistoricoCombustivelVeiculo)
    
    @Delete
    suspend fun deletar(historico: HistoricoCombustivelVeiculo)
    
    // ✅ CORREÇÃO: Query corrigida para trabalhar com Date do Java
    @Query("SELECT SUM(valor) FROM historico_combustivel_veiculo WHERE veiculo_id = :veiculoId AND strftime('%Y', data_abastecimento / 1000, 'unixepoch') = :ano")
    suspend fun obterTotalGastoPorAno(veiculoId: Long, ano: String): Double?
    
    // ✅ CORREÇÃO: Query corrigida para trabalhar com Date do Java
    @Query("SELECT SUM(km_rodado) FROM historico_combustivel_veiculo WHERE veiculo_id = :veiculoId AND strftime('%Y', data_abastecimento / 1000, 'unixepoch') = :ano")
    suspend fun obterTotalKmPorAno(veiculoId: Long, ano: String): Double?
    
    // ✅ CORREÇÃO: Query corrigida para trabalhar com Date do Java
    @Query("SELECT SUM(litros) FROM historico_combustivel_veiculo WHERE veiculo_id = :veiculoId AND strftime('%Y', data_abastecimento / 1000, 'unixepoch') = :ano")
    suspend fun obterTotalLitrosPorAno(veiculoId: Long, ano: String): Double?
}
