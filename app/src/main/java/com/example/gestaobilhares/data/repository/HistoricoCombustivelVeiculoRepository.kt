package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.HistoricoCombustivelVeiculoDao
import com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo
import com.example.gestaobilhares.utils.DateUtils
import kotlinx.coroutines.flow.Flow

class HistoricoCombustivelVeiculoRepository constructor(
    private val dao: HistoricoCombustivelVeiculoDao
) {
    fun listarPorVeiculo(veiculoId: Long): Flow<List<HistoricoCombustivelVeiculo>> = dao.listarPorVeiculo(veiculoId)
    
    // ✅ FASE 2: Converter ano (String) para timestamps de início e fim do ano usando função centralizada
    fun listarPorVeiculoEAno(veiculoId: Long, ano: String): Flow<List<HistoricoCombustivelVeiculo>> {
        val (inicioAno, fimAno) = DateUtils.calcularRangeAno(ano)
        return dao.listarPorVeiculoEAno(veiculoId, inicioAno, fimAno)
    }
    
    suspend fun inserir(historico: HistoricoCombustivelVeiculo): Long = dao.inserir(historico)
    suspend fun atualizar(historico: HistoricoCombustivelVeiculo) = dao.atualizar(historico)
    suspend fun deletar(historico: HistoricoCombustivelVeiculo) = dao.deletar(historico)
    
    // ✅ FASE 2: Converter ano (String) para timestamps de início e fim do ano usando função centralizada
    suspend fun obterTotalGastoPorAno(veiculoId: Long, ano: String): Double? {
        val (inicioAno, fimAno) = DateUtils.calcularRangeAno(ano)
        return dao.obterTotalGastoPorAno(veiculoId, inicioAno, fimAno)
    }
    
    // ✅ FASE 2: Converter ano (String) para timestamps de início e fim do ano usando função centralizada
    suspend fun obterTotalKmPorAno(veiculoId: Long, ano: String): Double? {
        val (inicioAno, fimAno) = DateUtils.calcularRangeAno(ano)
        return dao.obterTotalKmPorAno(veiculoId, inicioAno, fimAno)
    }
    
    // ✅ FASE 2: Converter ano (String) para timestamps de início e fim do ano usando função centralizada
    suspend fun obterTotalLitrosPorAno(veiculoId: Long, ano: String): Double? {
        val (inicioAno, fimAno) = DateUtils.calcularRangeAno(ano)
        return dao.obterTotalLitrosPorAno(veiculoId, inicioAno, fimAno)
    }
}

