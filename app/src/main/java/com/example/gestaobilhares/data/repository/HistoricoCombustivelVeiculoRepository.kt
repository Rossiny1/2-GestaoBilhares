package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.HistoricoCombustivelVeiculoDao
import com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo
import kotlinx.coroutines.flow.Flow
class HistoricoCombustivelVeiculoRepository constructor(
    private val dao: HistoricoCombustivelVeiculoDao
) {
    fun listarPorVeiculo(veiculoId: Long): Flow<List<HistoricoCombustivelVeiculo>> = dao.listarPorVeiculo(veiculoId)
    fun listarPorVeiculoEAno(veiculoId: Long, ano: String): Flow<List<HistoricoCombustivelVeiculo>> = dao.listarPorVeiculoEAno(veiculoId, ano)
    suspend fun inserir(historico: HistoricoCombustivelVeiculo): Long = dao.inserir(historico)
    suspend fun atualizar(historico: HistoricoCombustivelVeiculo) = dao.atualizar(historico)
    suspend fun deletar(historico: HistoricoCombustivelVeiculo) = dao.deletar(historico)
    suspend fun obterTotalGastoPorAno(veiculoId: Long, ano: String): Double? = dao.obterTotalGastoPorAno(veiculoId, ano)
    suspend fun obterTotalKmPorAno(veiculoId: Long, ano: String): Double? = dao.obterTotalKmPorAno(veiculoId, ano)
    suspend fun obterTotalLitrosPorAno(veiculoId: Long, ano: String): Double? = dao.obterTotalLitrosPorAno(veiculoId, ano)
}

