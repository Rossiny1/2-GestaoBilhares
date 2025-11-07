package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface HistoricoCombustivelVeiculoDao {
    @Query("SELECT * FROM historico_combustivel_veiculo ORDER BY data_abastecimento DESC")
    fun listarTodos(): Flow<List<HistoricoCombustivelVeiculo>>
    
    @Query("SELECT * FROM historico_combustivel_veiculo WHERE veiculo_id = :veiculoId ORDER BY data_abastecimento DESC")
    fun listarPorVeiculo(veiculoId: Long): Flow<List<HistoricoCombustivelVeiculo>>
    
    // ✅ FASE 2: Query otimizada usando range query (pode usar índices) em vez de strftime()
    @Query("SELECT * FROM historico_combustivel_veiculo WHERE veiculo_id = :veiculoId AND data_abastecimento >= :inicioAno AND data_abastecimento < :fimAno ORDER BY data_abastecimento DESC")
    fun listarPorVeiculoEAno(veiculoId: Long, inicioAno: Long, fimAno: Long): Flow<List<HistoricoCombustivelVeiculo>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(historico: HistoricoCombustivelVeiculo): Long
    
    @Update
    suspend fun atualizar(historico: HistoricoCombustivelVeiculo)
    
    @Delete
    suspend fun deletar(historico: HistoricoCombustivelVeiculo)
    
    // ✅ FASE 2: Query otimizada usando range query (pode usar índices) em vez de strftime()
    @Query("SELECT SUM(valor) FROM historico_combustivel_veiculo WHERE veiculo_id = :veiculoId AND data_abastecimento >= :inicioAno AND data_abastecimento < :fimAno")
    suspend fun obterTotalGastoPorAno(veiculoId: Long, inicioAno: Long, fimAno: Long): Double?

    // ✅ FASE 2: Query otimizada usando range query (pode usar índices) em vez de strftime()
    @Query("SELECT SUM(km_rodado) FROM historico_combustivel_veiculo WHERE veiculo_id = :veiculoId AND data_abastecimento >= :inicioAno AND data_abastecimento < :fimAno")
    suspend fun obterTotalKmPorAno(veiculoId: Long, inicioAno: Long, fimAno: Long): Double?

    // ✅ FASE 2: Query otimizada usando range query (pode usar índices) em vez de strftime()
    @Query("SELECT SUM(litros) FROM historico_combustivel_veiculo WHERE veiculo_id = :veiculoId AND data_abastecimento >= :inicioAno AND data_abastecimento < :fimAno")
    suspend fun obterTotalLitrosPorAno(veiculoId: Long, inicioAno: Long, fimAno: Long): Double?
}
