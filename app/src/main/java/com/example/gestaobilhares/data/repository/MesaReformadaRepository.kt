package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.MesaReformadaDao
import com.example.gestaobilhares.data.entities.MesaReformada
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório para operações com mesas reformadas.
 */
@Singleton
class MesaReformadaRepository @Inject constructor(
    private val mesaReformadaDao: MesaReformadaDao
) {

    suspend fun inserir(mesaReformada: MesaReformada): Long {
        return mesaReformadaDao.inserir(mesaReformada)
    }

    fun listarTodas(): Flow<List<MesaReformada>> {
        return mesaReformadaDao.listarTodas()
    }

    suspend fun buscarPorId(id: Long): MesaReformada? {
        return mesaReformadaDao.buscarPorId(id)
    }

    suspend fun buscarPorMesaId(mesaId: Long): MesaReformada? {
        return mesaReformadaDao.buscarPorMesaId(mesaId)
    }

    fun buscarPorNumero(numero: String): Flow<List<MesaReformada>> {
        return mesaReformadaDao.buscarPorNumero(numero)
    }

    fun buscarPorPeriodo(dataInicio: Long, dataFim: Long): Flow<List<MesaReformada>> {
        return mesaReformadaDao.buscarPorPeriodo(dataInicio, dataFim)
    }

    suspend fun contarTotalReformas(): Int {
        return mesaReformadaDao.contarTotalReformas()
    }

    fun buscarPorTipo(tipoMesa: String): Flow<List<MesaReformada>> {
        return mesaReformadaDao.buscarPorTipo(tipoMesa)
    }

    suspend fun atualizar(mesaReformada: MesaReformada) {
        mesaReformadaDao.atualizar(mesaReformada)
    }

    suspend fun deletar(mesaReformada: MesaReformada) {
        mesaReformadaDao.deletar(mesaReformada)
    }

    suspend fun deletarPorId(id: Long) {
        mesaReformadaDao.deletarPorId(id)
    }
}
