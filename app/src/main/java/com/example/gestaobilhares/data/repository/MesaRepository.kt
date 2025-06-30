package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.MesaDao
import com.example.gestaobilhares.data.entities.Mesa
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository para operações relacionadas a mesas
 * Implementa o padrão Repository para abstrair a camada de dados
 * e fornecer uma interface limpa para os ViewModels.
 */
@Singleton
class MesaRepository @Inject constructor(
    private val mesaDao: MesaDao
) {
    fun obterMesasPorCliente(clienteId: Long): Flow<List<Mesa>> =
        mesaDao.obterMesasPorCliente(clienteId)

    fun obterMesasDisponiveis(): Flow<List<Mesa>> =
        mesaDao.obterMesasDisponiveis()

    suspend fun inserir(mesa: Mesa): Long = mesaDao.inserir(mesa)

    suspend fun atualizar(mesa: Mesa) = mesaDao.atualizar(mesa)

    suspend fun deletar(mesa: Mesa) = mesaDao.deletar(mesa)

    suspend fun vincularMesa(mesaId: Long, clienteId: Long) = mesaDao.vincularMesa(mesaId, clienteId)

    suspend fun desvincularMesa(mesaId: Long) = mesaDao.desvincularMesa(mesaId)

    suspend fun vincularMesaComValorFixo(mesaId: Long, clienteId: Long, valorFixo: Double) = 
        mesaDao.vincularMesaComValorFixo(mesaId, clienteId, valorFixo)
} 