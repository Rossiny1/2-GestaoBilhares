package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoricoManutencaoVeiculoDao {
    // ✅ NOVO: Listar todos os históricos (para uso no ViewModel como no código antigo)
    @Query("SELECT * FROM historico_manutencao_veiculo ORDER BY data_manutencao DESC")
    fun listarTodos(): Flow<List<HistoricoManutencaoVeiculo>>

    @Query("SELECT * FROM historico_manutencao_veiculo WHERE id = :id")
    suspend fun buscarPorId(id: Long): HistoricoManutencaoVeiculo?
    
    @Query("SELECT * FROM historico_manutencao_veiculo WHERE veiculo_id = :veiculoId ORDER BY data_manutencao DESC")
    fun listarPorVeiculo(veiculoId: Long): Flow<List<HistoricoManutencaoVeiculo>>
    
    // ✅ CORREÇÃO: Query corrigida para trabalhar com Date do Java
    @Query("SELECT * FROM historico_manutencao_veiculo WHERE veiculo_id = :veiculoId AND strftime('%Y', data_manutencao / 1000, 'unixepoch') = :ano ORDER BY data_manutencao DESC")
    fun listarPorVeiculoEAno(veiculoId: Long, ano: String): Flow<List<HistoricoManutencaoVeiculo>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(historico: HistoricoManutencaoVeiculo): Long
    
    @Update
    suspend fun atualizar(historico: HistoricoManutencaoVeiculo)
    
    @Delete
    suspend fun deletar(historico: HistoricoManutencaoVeiculo)
    
    // ✅ CORREÇÃO: Query corrigida para trabalhar com Date do Java
    @Query("SELECT SUM(valor) FROM historico_manutencao_veiculo WHERE veiculo_id = :veiculoId AND strftime('%Y', data_manutencao / 1000, 'unixepoch') = :ano")
    suspend fun obterTotalGastoPorAno(veiculoId: Long, ano: String): Double?
}
