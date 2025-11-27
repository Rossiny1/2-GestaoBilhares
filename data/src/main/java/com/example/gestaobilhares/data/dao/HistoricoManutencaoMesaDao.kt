package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoricoManutencaoMesaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(historico: HistoricoManutencaoMesa): Long

    @Query("SELECT * FROM historico_manutencao_mesa WHERE mesa_id = :mesaId ORDER BY data_manutencao DESC")
    fun buscarPorMesaId(mesaId: Long): Flow<List<HistoricoManutencaoMesa>>

    @Query("SELECT * FROM historico_manutencao_mesa WHERE numero_mesa = :numeroMesa ORDER BY data_manutencao DESC")
    fun buscarPorNumeroMesa(numeroMesa: String): Flow<List<HistoricoManutencaoMesa>>

    @Query("SELECT * FROM historico_manutencao_mesa WHERE tipo_manutencao = :tipoManutencao ORDER BY data_manutencao DESC")
    fun buscarPorTipoManutencao(tipoManutencao: TipoManutencao): Flow<List<HistoricoManutencaoMesa>>

    @Query("SELECT * FROM historico_manutencao_mesa WHERE data_manutencao BETWEEN :dataInicio AND :dataFim ORDER BY data_manutencao DESC")
    fun buscarPorPeriodo(dataInicio: Long, dataFim: Long): Flow<List<HistoricoManutencaoMesa>>

    @Query("SELECT * FROM historico_manutencao_mesa ORDER BY data_manutencao DESC")
    fun listarTodos(): Flow<List<HistoricoManutencaoMesa>>

    @Query("SELECT * FROM historico_manutencao_mesa WHERE id = :id")
    suspend fun buscarPorId(id: Long): HistoricoManutencaoMesa?

    @Update
    suspend fun atualizar(historico: HistoricoManutencaoMesa)

    @Delete
    suspend fun deletar(historico: HistoricoManutencaoMesa)

    @Query("DELETE FROM historico_manutencao_mesa WHERE id = :id")
    suspend fun deletarPorId(id: Long)

    @Query("DELETE FROM historico_manutencao_mesa WHERE mesa_id = :mesaId")
    suspend fun deletarPorMesaId(mesaId: Long)

    // Queries para obter informações específicas
    @Query("SELECT * FROM historico_manutencao_mesa WHERE mesa_id = :mesaId AND tipo_manutencao = 'PINTURA' ORDER BY data_manutencao DESC LIMIT 1")
    suspend fun obterUltimaPintura(mesaId: Long): HistoricoManutencaoMesa?

    @Query("SELECT * FROM historico_manutencao_mesa WHERE mesa_id = :mesaId AND tipo_manutencao = 'TROCA_PANO' ORDER BY data_manutencao DESC LIMIT 1")
    suspend fun obterUltimaTrocaPano(mesaId: Long): HistoricoManutencaoMesa?

    @Query("SELECT * FROM historico_manutencao_mesa WHERE mesa_id = :mesaId AND tipo_manutencao = 'TROCA_TABELA' ORDER BY data_manutencao DESC LIMIT 1")
    suspend fun obterUltimaTrocaTabela(mesaId: Long): HistoricoManutencaoMesa?
}
