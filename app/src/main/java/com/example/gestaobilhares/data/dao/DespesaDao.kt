package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.Despesa
import com.example.gestaobilhares.data.entities.DespesaResumo
import com.example.gestaobilhares.data.entities.CategoriaDespesaTotal
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DAO (Data Access Object) para operações de banco de dados com Despesas.
 * Fornece métodos para CRUD e consultas específicas relacionadas a despesas.
 */
@Dao
interface DespesaDao {

    /**
     * Insere uma nova despesa no banco de dados.
     * @param despesa A despesa a ser inserida
     * @return O ID da despesa inserida
     */
    @Insert
    suspend fun inserir(despesa: Despesa): Long

    /**
     * Atualiza uma despesa existente.
     * @param despesa A despesa com os dados atualizados
     */
    @Update
    suspend fun atualizar(despesa: Despesa)

    /**
     * Deleta uma despesa do banco de dados.
     * @param despesa A despesa a ser deletada
     */
    @Delete
    suspend fun deletar(despesa: Despesa)

    /**
     * Busca uma despesa pelo ID.
     * @param id O ID da despesa
     * @return A despesa encontrada ou null
     */
    @Query("SELECT * FROM despesas WHERE id = :id")
    suspend fun buscarPorId(id: Long): Despesa?

    /**
     * Busca todas as despesas de uma rota específica.
     * @param rotaId ID da rota
     * @return Flow com lista de despesas da rota
     */
    @Query("SELECT * FROM despesas WHERE rotaId = :rotaId ORDER BY dataHora DESC")
    fun buscarPorRota(rotaId: Long): Flow<List<Despesa>>

    /**
     * Busca todas as despesas com informações da rota (join).
     * @return Flow com lista de DespesaResumo
     */
    @Query("""
        SELECT d.*, r.nome as nomeRota 
        FROM despesas d 
        INNER JOIN rotas r ON d.rotaId = r.id 
        ORDER BY d.dataHora DESC
    """)
    fun buscarTodasComRota(): Flow<List<DespesaResumo>>

    /**
     * Busca despesas por período de datas.
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return Flow com lista de despesas no período
     */
    @Query("""
        SELECT d.*, r.nome as nomeRota 
        FROM despesas d 
        INNER JOIN rotas r ON d.rotaId = r.id 
        WHERE d.dataHora BETWEEN :dataInicio AND :dataFim 
        ORDER BY d.dataHora DESC
    """)
    fun buscarPorPeriodo(dataInicio: LocalDateTime, dataFim: LocalDateTime): Flow<List<DespesaResumo>>

    /**
     * Busca despesas por categoria.
     * @param categoria A categoria das despesas
     * @return Flow com lista de despesas da categoria
     */
    @Query("""
        SELECT d.*, r.nome as nomeRota 
        FROM despesas d 
        INNER JOIN rotas r ON d.rotaId = r.id 
        WHERE d.categoria = :categoria 
        ORDER BY d.dataHora DESC
    """)
    fun buscarPorCategoria(categoria: String): Flow<List<DespesaResumo>>

    /**
     * Calcula o total de despesas de uma rota.
     * @param rotaId ID da rota
     * @return O valor total das despesas da rota
     */
    @Query("SELECT COALESCE(SUM(valor), 0.0) FROM despesas WHERE rotaId = :rotaId")
    suspend fun calcularTotalPorRota(rotaId: Long): Double

    /**
     * Calcula o total geral de despesas.
     * @return O valor total de todas as despesas
     */
    @Query("SELECT COALESCE(SUM(valor), 0.0) FROM despesas")
    suspend fun calcularTotalGeral(): Double

    /**
     * Conta a quantidade de despesas de uma rota.
     * @param rotaId ID da rota
     * @return A quantidade de despesas da rota
     */
    @Query("SELECT COUNT(*) FROM despesas WHERE rotaId = :rotaId")
    suspend fun contarPorRota(rotaId: Long): Int

    /**
     * Busca despesas por rota e período (para relatórios).
     * @param rotaId ID da rota
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return Lista de despesas da rota no período
     */
    @Query("""
        SELECT * FROM despesas 
        WHERE rotaId = :rotaId AND dataHora BETWEEN :dataInicio AND :dataFim 
        ORDER BY dataHora DESC
    """)
    suspend fun buscarPorRotaEPeriodo(
        rotaId: Long, 
        dataInicio: LocalDateTime, 
        dataFim: LocalDateTime
    ): List<Despesa>

    /**
     * Deleta todas as despesas de uma rota (usado quando rota é deletada).
     * @param rotaId ID da rota
     */
    @Query("DELETE FROM despesas WHERE rotaId = :rotaId")
    suspend fun deletarPorRota(rotaId: Long)

    /**
     * Busca todas as despesas de um cicloId específico (todas as rotas).
     */
    @Query("SELECT * FROM despesas WHERE cicloId = :cicloId ORDER BY dataHora DESC")
    fun buscarPorCicloId(cicloId: Long): Flow<List<Despesa>>

    /**
     * Busca todas as despesas de uma rota e cicloId específico.
     */
    @Query("SELECT * FROM despesas WHERE rotaId = :rotaId AND cicloId = :cicloId ORDER BY dataHora DESC")
    fun buscarPorRotaECicloId(rotaId: Long, cicloId: Long): Flow<List<Despesa>>
} 
