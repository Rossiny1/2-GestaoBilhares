package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.DespesaDao
import com.example.gestaobilhares.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository para gerenciar despesas.
 * Centraliza o acesso aos dados de despesas, seja do banco local ou dados mock.
 * 
 * @property despesaDao DAO para operações no banco de dados
 */
@Singleton
class DespesaRepository @Inject constructor(
    private val despesaDao: DespesaDao
) {
    
    
    /**
     * Busca todas as despesas com informações das rotas.
     * @return Flow com lista de DespesaResumo
     */
    fun buscarTodasComRota(): Flow<List<DespesaResumo>> {
        return despesaDao.buscarTodasComRota()
    }

    /**
     * Busca despesas de uma rota específica.
     * @param rotaId ID da rota
     * @return Flow com lista de despesas da rota
     */
    fun buscarPorRota(rotaId: Long): Flow<List<Despesa>> {
        return despesaDao.buscarPorRota(rotaId)
    }

    /**
     * Busca despesas por período.
     * @param dataInicio Data de início
     * @param dataFim Data de fim
     * @return Flow com lista de DespesaResumo no período
     */
    fun buscarPorPeriodo(dataInicio: LocalDateTime, dataFim: LocalDateTime): Flow<List<DespesaResumo>> {
        return despesaDao.buscarPorPeriodo(dataInicio, dataFim)
    }

    /**
     * Busca despesas por categoria.
     * @param categoria Categoria das despesas
     * @return Flow com lista de DespesaResumo da categoria
     */
    fun buscarPorCategoria(categoria: String): Flow<List<DespesaResumo>> {
        return despesaDao.buscarPorCategoria(categoria)
    }

    /**
     * Insere uma nova despesa.
     * @param despesa Despesa a ser inserida
     * @return ID da despesa inserida
     */
    suspend fun inserir(despesa: Despesa): Long {
        return despesaDao.inserir(despesa)
    }

    /**
     * Atualiza uma despesa existente.
     * @param despesa Despesa com dados atualizados
     */
    suspend fun atualizar(despesa: Despesa) {
        despesaDao.atualizar(despesa)
    }

    /**
     * ✅ NOVO: Busca uma despesa por ID
     * @param id ID da despesa
     * @return Despesa encontrada ou null
     */
    suspend fun buscarPorId(id: Long): Despesa? {
        return despesaDao.buscarPorId(id)
    }

    /**
     * Deleta uma despesa.
     * @param despesa Despesa a ser deletada
     */
    suspend fun deletar(despesa: Despesa) {
        despesaDao.deletar(despesa)
    }

    /**
     * Calcula total de despesas de uma rota.
     * @param rotaId ID da rota
     * @return Total das despesas da rota
     */
    suspend fun calcularTotalPorRota(rotaId: Long): Double {
        return despesaDao.calcularTotalPorRota(rotaId)
    }

    /**
     * Busca despesas por cicloId.
     * @param cicloId ID do ciclo
     * @return Flow com lista de despesas do ciclo
     */
    fun buscarPorCicloId(cicloId: Long): Flow<List<Despesa>> {
        return despesaDao.buscarPorCicloId(cicloId)
    }
    
    /**
     * Busca despesas por rota e cicloId.
     */
    fun buscarPorRotaECicloId(rotaId: Long, cicloId: Long) = despesaDao.buscarPorRotaECicloId(rotaId, cicloId)

    /**
     * Busca despesas sem cicloId (para debug).
     */
    fun buscarSemCicloId() = despesaDao.buscarSemCicloId()

    // ✅ NOVO: Despesas globais por ciclo (ano, número)
    suspend fun buscarGlobaisPorCiclo(ano: Int, numero: Int): List<Despesa> {
        return despesaDao.buscarGlobaisPorCiclo(ano, numero)
    }

    // ✅ NOVO: Soma de despesas globais por ciclo (ano, número)
    suspend fun somarGlobaisPorCiclo(ano: Int, numero: Int): Double {
        return despesaDao.somarGlobaisPorCiclo(ano, numero)
    }

    // ❌ REMOVIDO: Dados mock excluídos para evitar criação automática de dados
} 
