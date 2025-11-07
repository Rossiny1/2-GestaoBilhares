package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface HistoricoManutencaoVeiculoDao {
    @Query("SELECT * FROM historico_manutencao_veiculo ORDER BY data_manutencao DESC")
    fun listarTodos(): Flow<List<HistoricoManutencaoVeiculo>>
    
    @Query("SELECT * FROM historico_manutencao_veiculo WHERE veiculo_id = :veiculoId ORDER BY data_manutencao DESC")
    fun listarPorVeiculo(veiculoId: Long): Flow<List<HistoricoManutencaoVeiculo>>
    
    // ✅ FASE 2: Query otimizada usando range query (pode usar índices) em vez de strftime()
    @Query("SELECT * FROM historico_manutencao_veiculo WHERE veiculo_id = :veiculoId AND data_manutencao >= :inicioAno AND data_manutencao < :fimAno ORDER BY data_manutencao DESC")
    fun listarPorVeiculoEAno(veiculoId: Long, inicioAno: Long, fimAno: Long): Flow<List<HistoricoManutencaoVeiculo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(historico: HistoricoManutencaoVeiculo): Long

    @Update
    suspend fun atualizar(historico: HistoricoManutencaoVeiculo)

    @Delete
    suspend fun deletar(historico: HistoricoManutencaoVeiculo)

    // ✅ FASE 2: Query otimizada usando range query (pode usar índices) em vez de strftime()
    @Query("SELECT SUM(valor) FROM historico_manutencao_veiculo WHERE veiculo_id = :veiculoId AND data_manutencao >= :inicioAno AND data_manutencao < :fimAno")
    suspend fun obterTotalGastoPorAno(veiculoId: Long, inicioAno: Long, fimAno: Long): Double?
}
