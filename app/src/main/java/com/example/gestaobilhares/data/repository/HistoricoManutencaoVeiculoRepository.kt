package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.HistoricoManutencaoVeiculoDao
import com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo
import kotlinx.coroutines.flow.Flow
class HistoricoManutencaoVeiculoRepository constructor(
    private val dao: HistoricoManutencaoVeiculoDao
) {
    fun listarPorVeiculo(veiculoId: Long): Flow<List<HistoricoManutencaoVeiculo>> = dao.listarPorVeiculo(veiculoId)
    fun listarPorVeiculoEAno(veiculoId: Long, ano: String): Flow<List<HistoricoManutencaoVeiculo>> = dao.listarPorVeiculoEAno(veiculoId, ano)
    suspend fun inserir(historico: HistoricoManutencaoVeiculo): Long = dao.inserir(historico)
    suspend fun atualizar(historico: HistoricoManutencaoVeiculo) = dao.atualizar(historico)
    suspend fun deletar(historico: HistoricoManutencaoVeiculo) = dao.deletar(historico)
    suspend fun obterTotalGastoPorAno(veiculoId: Long, ano: String): Double? = dao.obterTotalGastoPorAno(veiculoId, ano)
}

