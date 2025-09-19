package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.MesaVendidaDao
import com.example.gestaobilhares.data.entities.MesaVendida
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório para operações com mesas vendidas
 * ✅ NOVO: SISTEMA DE VENDA DE MESAS
 */
@Singleton
class MesaVendidaRepository @Inject constructor(
    private val mesaVendidaDao: MesaVendidaDao
) {

    /**
     * Insere uma nova mesa vendida
     */
    suspend fun inserir(mesaVendida: MesaVendida): Long {
        return mesaVendidaDao.inserir(mesaVendida)
    }

    /**
     * Lista todas as mesas vendidas
     */
    fun listarTodas(): Flow<List<MesaVendida>> {
        return mesaVendidaDao.listarTodas()
    }

    /**
     * Busca uma mesa vendida por ID
     */
    suspend fun buscarPorId(id: Long): MesaVendida? {
        return mesaVendidaDao.buscarPorId(id)
    }

    /**
     * Busca uma mesa vendida pela mesa original
     */
    suspend fun buscarPorMesaOriginal(mesaIdOriginal: Long): MesaVendida? {
        return mesaVendidaDao.buscarPorMesaOriginal(mesaIdOriginal)
    }

    /**
     * Busca mesas vendidas por número
     */
    fun buscarPorNumero(numero: String): Flow<List<MesaVendida>> {
        return mesaVendidaDao.buscarPorNumero(numero)
    }

    /**
     * Busca mesas vendidas por comprador
     */
    fun buscarPorComprador(nome: String): Flow<List<MesaVendida>> {
        return mesaVendidaDao.buscarPorComprador(nome)
    }

    /**
     * Busca mesas vendidas por período
     */
    fun buscarPorPeriodo(dataInicio: Long, dataFim: Long): Flow<List<MesaVendida>> {
        return mesaVendidaDao.buscarPorPeriodo(dataInicio, dataFim)
    }

    /**
     * Calcula o total de vendas
     */
    suspend fun calcularTotalVendas(): Double {
        return mesaVendidaDao.calcularTotalVendas() ?: 0.0
    }

    /**
     * Conta o total de vendas
     */
    suspend fun contarTotalVendas(): Int {
        return mesaVendidaDao.contarTotalVendas()
    }

    /**
     * Busca mesas vendidas por tipo
     */
    fun buscarPorTipo(tipoMesa: String): Flow<List<MesaVendida>> {
        return mesaVendidaDao.buscarPorTipo(tipoMesa)
    }

    /**
     * Atualiza uma mesa vendida
     */
    suspend fun atualizar(mesaVendida: MesaVendida) {
        mesaVendidaDao.atualizar(mesaVendida)
    }

    /**
     * Deleta uma mesa vendida
     */
    suspend fun deletar(mesaVendida: MesaVendida) {
        mesaVendidaDao.deletar(mesaVendida)
    }

    /**
     * Deleta uma mesa vendida por ID
     */
    suspend fun deletarPorId(id: Long) {
        mesaVendidaDao.deletarPorId(id)
    }
}
