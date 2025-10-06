package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.HistoricoManutencaoMesaDao
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import kotlinx.coroutines.flow.Flow
class HistoricoManutencaoMesaRepository constructor(
    private val historicoManutencaoMesaDao: HistoricoManutencaoMesaDao
) {

    suspend fun inserir(historico: HistoricoManutencaoMesa): Long {
        return historicoManutencaoMesaDao.inserir(historico)
    }

    fun buscarPorMesaId(mesaId: Long): Flow<List<HistoricoManutencaoMesa>> {
        return historicoManutencaoMesaDao.buscarPorMesaId(mesaId)
    }

    fun buscarPorNumeroMesa(numeroMesa: String): Flow<List<HistoricoManutencaoMesa>> {
        return historicoManutencaoMesaDao.buscarPorNumeroMesa(numeroMesa)
    }

    fun buscarPorTipoManutencao(tipoManutencao: TipoManutencao): Flow<List<HistoricoManutencaoMesa>> {
        return historicoManutencaoMesaDao.buscarPorTipoManutencao(tipoManutencao)
    }

    fun buscarPorPeriodo(dataInicio: Long, dataFim: Long): Flow<List<HistoricoManutencaoMesa>> {
        return historicoManutencaoMesaDao.buscarPorPeriodo(dataInicio, dataFim)
    }

    fun listarTodos(): Flow<List<HistoricoManutencaoMesa>> {
        return historicoManutencaoMesaDao.listarTodos()
    }

    suspend fun buscarPorId(id: Long): HistoricoManutencaoMesa? {
        return historicoManutencaoMesaDao.buscarPorId(id)
    }

    suspend fun atualizar(historico: HistoricoManutencaoMesa) {
        historicoManutencaoMesaDao.atualizar(historico)
    }

    suspend fun deletar(historico: HistoricoManutencaoMesa) {
        historicoManutencaoMesaDao.deletar(historico)
    }

    suspend fun deletarPorId(id: Long) {
        historicoManutencaoMesaDao.deletarPorId(id)
    }

    suspend fun deletarPorMesaId(mesaId: Long) {
        historicoManutencaoMesaDao.deletarPorMesaId(mesaId)
    }

    // Métodos específicos para obter últimas manutenções
    suspend fun obterUltimaPintura(mesaId: Long): HistoricoManutencaoMesa? {
        return historicoManutencaoMesaDao.obterUltimaPintura(mesaId)
    }

    suspend fun obterUltimaTrocaPano(mesaId: Long): HistoricoManutencaoMesa? {
        return historicoManutencaoMesaDao.obterUltimaTrocaPano(mesaId)
    }

    suspend fun obterUltimaTrocaTabela(mesaId: Long): HistoricoManutencaoMesa? {
        return historicoManutencaoMesaDao.obterUltimaTrocaTabela(mesaId)
    }
}

