package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.HistoricoManutencaoVeiculoDao
import com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class HistoricoManutencaoVeiculoRepository constructor(
    private val dao: HistoricoManutencaoVeiculoDao
) {
    fun listarPorVeiculo(veiculoId: Long): Flow<List<HistoricoManutencaoVeiculo>> = dao.listarPorVeiculo(veiculoId)
    
    // ✅ FASE 2: Converter ano (String) para timestamps de início e fim do ano
    fun listarPorVeiculoEAno(veiculoId: Long, ano: String): Flow<List<HistoricoManutencaoVeiculo>> {
        val (inicioAno, fimAno) = calcularRangeAno(ano)
        return dao.listarPorVeiculoEAno(veiculoId, inicioAno, fimAno)
    }
    
    suspend fun inserir(historico: HistoricoManutencaoVeiculo): Long = dao.inserir(historico)
    suspend fun atualizar(historico: HistoricoManutencaoVeiculo) = dao.atualizar(historico)
    suspend fun deletar(historico: HistoricoManutencaoVeiculo) = dao.deletar(historico)
    
    // ✅ FASE 2: Converter ano (String) para timestamps de início e fim do ano
    suspend fun obterTotalGastoPorAno(veiculoId: Long, ano: String): Double? {
        val (inicioAno, fimAno) = calcularRangeAno(ano)
        return dao.obterTotalGastoPorAno(veiculoId, inicioAno, fimAno)
    }
    
    /**
     * ✅ FASE 2: Calcula timestamps de início e fim do ano para range queries otimizadas
     */
    private fun calcularRangeAno(ano: String): Pair<Long, Long> {
        val anoInt = ano.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, anoInt)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val inicioAno = calendar.timeInMillis
        
        calendar.add(Calendar.YEAR, 1)
        val fimAno = calendar.timeInMillis
        
        return Pair(inicioAno, fimAno)
    }
}

