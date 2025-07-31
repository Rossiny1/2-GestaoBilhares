package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.DespesaDao
import com.example.gestaobilhares.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * Repository para gerenciar despesas.
 * Centraliza o acesso aos dados de despesas, seja do banco local ou dados mock.
 * 
 * @property despesaDao DAO para operações no banco de dados
 */
class DespesaRepository(
    private val despesaDao: DespesaDao
) {
    
    // ✅ CORRIGIDO: Usar dados reais do banco de dados
    private val usarDadosMock = false
    
    /**
     * Busca todas as despesas com informações das rotas.
     * @return Flow com lista de DespesaResumo
     */
    fun buscarTodasComRota(): Flow<List<DespesaResumo>> {
        return if (usarDadosMock) {
            flowOf(obterDespesasMock())
        } else {
            despesaDao.buscarTodasComRota()
        }
    }

    /**
     * Busca despesas de uma rota específica.
     * @param rotaId ID da rota
     * @return Flow com lista de despesas da rota
     */
    fun buscarPorRota(rotaId: Long): Flow<List<Despesa>> {
        return if (usarDadosMock) {
            flowOf(obterDespesasMockPorRota(rotaId))
        } else {
            despesaDao.buscarPorRota(rotaId)
        }
    }

    /**
     * Busca despesas por período.
     * @param dataInicio Data de início
     * @param dataFim Data de fim
     * @return Flow com lista de DespesaResumo no período
     */
    fun buscarPorPeriodo(dataInicio: LocalDateTime, dataFim: LocalDateTime): Flow<List<DespesaResumo>> {
        return if (usarDadosMock) {
            flowOf(obterDespesasMock().filter { 
                it.dataHora.isAfter(dataInicio) && it.dataHora.isBefore(dataFim)
            })
        } else {
            despesaDao.buscarPorPeriodo(dataInicio, dataFim)
        }
    }

    /**
     * Busca despesas por categoria.
     * @param categoria Categoria das despesas
     * @return Flow com lista de DespesaResumo da categoria
     */
    fun buscarPorCategoria(categoria: String): Flow<List<DespesaResumo>> {
        return if (usarDadosMock) {
            flowOf(obterDespesasMock().filter { it.categoria == categoria })
        } else {
            despesaDao.buscarPorCategoria(categoria)
        }
    }

    /**
     * Insere uma nova despesa.
     * @param despesa Despesa a ser inserida
     * @return ID da despesa inserida
     */
    suspend fun inserir(despesa: Despesa): Long {
        return if (usarDadosMock) {
            // Em modo mock, retorna um ID simulado
            System.currentTimeMillis()
        } else {
            despesaDao.inserir(despesa)
        }
    }

    /**
     * Atualiza uma despesa existente.
     * @param despesa Despesa com dados atualizados
     */
    suspend fun atualizar(despesa: Despesa) {
        if (!usarDadosMock) {
            despesaDao.atualizar(despesa)
        }
    }

    /**
     * ✅ NOVO: Busca uma despesa por ID
     * @param id ID da despesa
     * @return Despesa encontrada ou null
     */
    suspend fun buscarPorId(id: Long): Despesa? {
        return if (usarDadosMock) {
            // ✅ CORREÇÃO: Converter DespesaResumo para Despesa
            obterDespesasMock().find { it.id == id }?.let { despesaResumo ->
                Despesa(
                    id = despesaResumo.id,
                    rotaId = despesaResumo.rotaId,
                    descricao = despesaResumo.descricao,
                    valor = despesaResumo.valor,
                    categoria = despesaResumo.categoria,
                    dataHora = despesaResumo.dataHora,
                    observacoes = despesaResumo.observacoes,
                    criadoPor = despesaResumo.criadoPor
                )
            }
        } else {
            despesaDao.buscarPorId(id)
        }
    }

    /**
     * Deleta uma despesa.
     * @param despesa Despesa a ser deletada
     */
    suspend fun deletar(despesa: Despesa) {
        if (!usarDadosMock) {
            despesaDao.deletar(despesa)
        }
    }

    /**
     * Calcula total de despesas de uma rota.
     * @param rotaId ID da rota
     * @return Total das despesas da rota
     */
    suspend fun calcularTotalPorRota(rotaId: Long): Double {
        return if (usarDadosMock) {
            obterDespesasMockPorRota(rotaId).sumOf { it.valor }
        } else {
            despesaDao.calcularTotalPorRota(rotaId)
        }
    }

    /**
     * Busca despesas por cicloId.
     * @param cicloId ID do ciclo
     * @return Flow com lista de despesas do ciclo
     */
    fun buscarPorCicloId(cicloId: Long): Flow<List<Despesa>> {
        return if (usarDadosMock) {
            flowOf(emptyList()) // Mock vazio para ciclos
        } else {
            despesaDao.buscarPorCicloId(cicloId)
        }
    }
    
    /**
     * Busca despesas por rota e cicloId.
     */
    fun buscarPorRotaECicloId(rotaId: Long, cicloId: Long) = despesaDao.buscarPorRotaECicloId(rotaId, cicloId)

    /**
     * Busca despesas sem cicloId (para debug).
     */
    fun buscarSemCicloId() = despesaDao.buscarSemCicloId()

    /**
     * Dados mock para desenvolvimento e testes.
     * Remove quando integrar com dados reais.
     */
    private fun obterDespesasMock(): List<DespesaResumo> {
        val agora = LocalDateTime.now()
        
        return listOf(
            DespesaResumo(
                id = 1,
                rotaId = 1,
                descricao = "Combustível para veículo",
                valor = 85.50,
                categoria = CategoriaDespesaEnum.COMBUSTIVEL.displayName,
                dataHora = agora.minusDays(1),
                observacoes = "Posto BR - Km 150",
                criadoPor = "João Silva",
                nomeRota = "Rota Centro"
            ),
            DespesaResumo(
                id = 2,
                rotaId = 1,
                descricao = "Almoço da equipe",
                valor = 45.00,
                categoria = CategoriaDespesaEnum.ALIMENTACAO.displayName,
                dataHora = agora.minusDays(2),
                observacoes = "Restaurante do João",
                criadoPor = "João Silva",
                nomeRota = "Rota Centro"
            ),
            DespesaResumo(
                id = 3,
                rotaId = 2,
                descricao = "Manutenção preventiva",
                valor = 120.00,
                categoria = CategoriaDespesaEnum.MANUTENCAO.displayName,
                dataHora = agora.minusDays(3),
                observacoes = "Troca de óleo e filtros",
                criadoPor = "Maria Santos",
                nomeRota = "Rota Periferia"
            ),
            DespesaResumo(
                id = 4,
                rotaId = 2,
                descricao = "Material de limpeza",
                valor = 32.75,
                categoria = CategoriaDespesaEnum.MATERIAIS.displayName,
                dataHora = agora.minusDays(4),
                observacoes = "Produtos para limpeza das mesas",
                criadoPor = "Maria Santos",
                nomeRota = "Rota Periferia"
            ),
            DespesaResumo(
                id = 5,
                rotaId = 3,
                descricao = "Passagem de ônibus",
                valor = 8.50,
                categoria = CategoriaDespesaEnum.TRANSPORTE.displayName,
                dataHora = agora.minusDays(5),
                observacoes = "Transporte para cliente",
                criadoPor = "Carlos Oliveira",
                nomeRota = "Rota Industrial"
            )
        )
    }

    /**
     * Filtra despesas mock por rota.
     */
    private fun obterDespesasMockPorRota(rotaId: Long): List<Despesa> {
        return obterDespesasMock()
            .filter { it.rotaId == rotaId }
            .map { it.despesa }
    }
} 
