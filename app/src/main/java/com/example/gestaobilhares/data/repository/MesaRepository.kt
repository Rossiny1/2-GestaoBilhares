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

    fun obterMesasDisponiveis(): Flow<List<Mesa>> {
        android.util.Log.d("MesaRepository", "=== OBTENDO MESAS DISPONÍVEIS ===")
        return mesaDao.obterMesasDisponiveis().also { flow ->
            // Log adicional para debug
            android.util.Log.d("MesaRepository", "Query de mesas disponíveis executada")
        }
    }

    suspend fun inserir(mesa: Mesa): Long = mesaDao.inserir(mesa)

    suspend fun atualizar(mesa: Mesa) = mesaDao.atualizar(mesa)

    suspend fun deletar(mesa: Mesa) = mesaDao.deletar(mesa)

    suspend fun vincularMesa(mesaId: Long, clienteId: Long) = mesaDao.vincularMesa(mesaId, clienteId)

    suspend fun desvincularMesa(mesaId: Long) = mesaDao.desvincularMesa(mesaId)

    suspend fun vincularMesaComValorFixo(mesaId: Long, clienteId: Long, valorFixo: Double) = 
        mesaDao.vincularMesaComValorFixo(mesaId, clienteId, valorFixo)

    suspend fun retirarMesa(mesaId: Long) = mesaDao.retirarMesa(mesaId)

    suspend fun atualizarRelogioMesa(
        mesaId: Long, 
        relogioInicial: Int, 
        relogioFinal: Int,
        fichasInicial: Int,
        fichasFinal: Int
    ) = mesaDao.atualizarRelogioMesa(mesaId, relogioInicial, relogioFinal, fichasInicial, fichasFinal)

    suspend fun atualizarRelogioFinal(mesaId: Long, relogioFinal: Int) = 
        mesaDao.atualizarRelogioFinal(mesaId, relogioFinal)

    suspend fun obterMesaPorId(mesaId: Long): Mesa? = mesaDao.obterMesaPorId(mesaId)

    /**
     * ✅ FUNÇÃO FALLBACK: Obtém mesas do cliente diretamente (sem Flow)
     */
    suspend fun obterMesasPorClienteDireto(clienteId: Long): List<Mesa> =
        mesaDao.obterMesasPorClienteDireto(clienteId)
} 