package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.HistoricoManutencaoVeiculoDao
import com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo
import com.example.gestaobilhares.core.utils.DateUtils
import kotlinx.coroutines.flow.Flow

class HistoricoManutencaoVeiculoRepository constructor(
    private val dao: HistoricoManutencaoVeiculoDao
) {
    fun listarPorVeiculo(veiculoId: Long): Flow<List<HistoricoManutencaoVeiculo>> = dao.listarPorVeiculo(veiculoId)
    
    // ✅ FASE 2: Converter ano (String) para timestamps de início e fim do ano usando função centralizada
    fun listarPorVeiculoEAno(veiculoId: Long, ano: String): Flow<List<HistoricoManutencaoVeiculo>> {
        val (inicioAno, fimAno) = DateUtils.calcularRangeAno(ano)
        return dao.listarPorVeiculoEAno(veiculoId, inicioAno, fimAno)
    }
    
    suspend fun inserir(historico: HistoricoManutencaoVeiculo): Long = dao.inserir(historico)
    suspend fun atualizar(historico: HistoricoManutencaoVeiculo) = dao.atualizar(historico)
    suspend fun deletar(historico: HistoricoManutencaoVeiculo) = dao.deletar(historico)
    
    // ✅ FASE 2: Converter ano (String) para timestamps de início e fim do ano usando função centralizada
    suspend fun obterTotalGastoPorAno(veiculoId: Long, ano: String): Double? {
        val (inicioAno, fimAno) = DateUtils.calcularRangeAno(ano)
        return dao.obterTotalGastoPorAno(veiculoId, inicioAno, fimAno)
    }
}

