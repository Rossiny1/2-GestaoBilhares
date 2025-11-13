package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface HistoricoManutencaoVeiculoDao {
    @Query("SELECT * FROM historico_manutencao_veiculo WHERE veiculo_id = :veiculoId ORDER BY data_manutencao DESC")
    fun listarPorVeiculo(veiculoId: Long): Flow<List<HistoricoManutencaoVeiculo>>
    
    // ✅ CORREÇÃO: Query corrigida para trabalhar com Date do Java
    @Query("SELECT * FROM historico_manutencao_veiculo WHERE veiculo_id = :veiculoId AND strftime('%Y', data_manutencao) = :ano ORDER BY data_manutencao DESC")
    fun listarPorVeiculoEAno(veiculoId: Long, ano: String): Flow<List<HistoricoManutencaoVeiculo>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(historico: HistoricoManutencaoVeiculo): Long
    
    @Update
    suspend fun atualizar(historico: HistoricoManutencaoVeiculo)
    
    @Delete
    suspend fun deletar(historico: HistoricoManutencaoVeiculo)
    
    // ✅ CORREÇÃO: Query corrigida para trabalhar com Date do Java
    @Query("SELECT SUM(valor) FROM historico_manutencao_veiculo WHERE veiculo_id = :veiculoId AND strftime('%Y', data_manutencao) = :ano")
    suspend fun obterTotalGastoPorAno(veiculoId: Long, ano: String): Double?
}
