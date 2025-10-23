package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.MesaDao
import com.example.gestaobilhares.data.entities.Mesa
import kotlinx.coroutines.flow.Flow
/**
 * Repository para operações relacionadas a mesas
 * Implementa o padrão Repository para abstrair a camada de dados
 * e fornecer uma interface limpa para os ViewModels.
 */
class MesaRepository constructor(
    private val mesaDao: MesaDao
) {
    fun obterMesasPorCliente(clienteId: Long): Flow<List<Mesa>> =
        mesaDao.obterMesasPorCliente(clienteId)

    fun obterMesasDisponiveis(): Flow<List<Mesa>> = mesaDao.obterMesasDisponiveis()

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

    /**
     * ✅ NOVO: Busca uma mesa específica por ID
     */
    suspend fun obterMesaPorId(mesaId: Long): Mesa? {
        android.util.Log.d("MesaRepository", "ObterMesaPorId: MesaId=$mesaId")
        val mesa = mesaDao.obterMesaPorId(mesaId)
        if (mesa != null) {
            android.util.Log.d("MesaRepository", "Mesa encontrada: ID=${mesa.id}, Número=${mesa.numero}, Tipo=${mesa.tipoMesa}, ClienteId=${mesa.clienteId}")
        } else {
            android.util.Log.w("MesaRepository", "Mesa não encontrada para ID: $mesaId")
        }
        return mesa
    }
    
    /**
     * ✅ NOVO: Obtém todas as mesas vinculadas a um cliente (versão síncrona)
     */
    suspend fun obterMesasPorClienteDireto(clienteId: Long): List<Mesa> {
        android.util.Log.d("MesaRepository", "ObterMesasPorClienteDireto: ClienteId=$clienteId")
        val mesas = mesaDao.obterMesasPorClienteDireto(clienteId)
        android.util.Log.d("MesaRepository", "Mesas encontradas: ${mesas.size}")
        mesas.forEach { mesa ->
            android.util.Log.d("MesaRepository", "Mesa: ID=${mesa.id}, Número=${mesa.numero}, Tipo=${mesa.tipoMesa}, ClienteId=${mesa.clienteId}")
        }
        return mesas
    }

    suspend fun atualizarRelogioFinal(mesaId: Long, relogioFinal: Int) = 
        mesaDao.atualizarRelogioFinal(mesaId, relogioFinal)
    
    /**
     * ✅ MÉTODO LEGADO: Mantido para compatibilidade com código existente
     */
    suspend fun buscarPorId(mesaId: Long): Mesa? = mesaDao.obterMesaPorId(mesaId)

    /**
     * ✅ NOVA FUNÇÃO: Obtém todas as mesas (disponíveis e em uso)
     */
    fun obterTodasMesas(): Flow<List<Mesa>> = mesaDao.obterTodasMesas()
    
    /**
     * ✅ NOVA FUNÇÃO: Busca contratos por cliente
     */
    fun buscarContratosPorCliente(_clienteId: Long): Flow<List<com.example.gestaobilhares.data.entities.ContratoLocacao>> {
        // Este método será implementado no AppRepository
        // Por enquanto, retorna um Flow vazio
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }
} 
