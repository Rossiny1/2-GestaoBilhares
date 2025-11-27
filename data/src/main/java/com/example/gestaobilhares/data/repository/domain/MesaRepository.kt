package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.MesaDao
import com.example.gestaobilhares.data.dao.MesaReformadaDao
import com.example.gestaobilhares.data.dao.PanoMesaDao
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.entities.PanoMesa
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import android.util.Log

/**
 * Repository especializado para operações relacionadas a mesas.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 * 
 * Responsabilidades:
 * - Operações básicas de mesa
 * - Mesas reformadas
 * - Vinculações pano-mesa
 */
class MesaRepository(
    private val mesaDao: MesaDao?,
    private val mesaReformadaDao: MesaReformadaDao?,
    private val panoMesaDao: PanoMesaDao?
) {
    
    // ==================== MESA BÁSICA ====================
    
    suspend fun obterPorId(id: Long) = mesaDao?.obterMesaPorId(id)
    fun obterPorCliente(clienteId: Long) = mesaDao?.obterMesasPorCliente(clienteId) ?: flowOf(emptyList())
    fun obterDisponiveis() = mesaDao?.obterMesasDisponiveis() ?: flowOf(emptyList())
    fun obterTodas() = mesaDao?.obterTodasMesas() ?: flowOf(emptyList())
    fun buscarPorRota(rotaId: Long) = mesaDao?.buscarMesasPorRota(rotaId) ?: flowOf(emptyList())
    
    suspend fun inserir(mesa: Mesa): Long {
        logDbInsertStart("MESA", "Numero=${mesa.numero}, ClienteID=${mesa.clienteId}")
        return try {
            val id = mesaDao?.inserir(mesa) ?: 0L
            logDbInsertSuccess("MESA", "Numero=${mesa.numero}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("MESA", "Numero=${mesa.numero}", e)
            throw e
        }
    }
    
    suspend fun atualizar(mesa: Mesa) = mesaDao?.atualizar(mesa)
    suspend fun deletar(mesa: Mesa) = mesaDao?.deletar(mesa)
    suspend fun vincularACliente(mesaId: Long, clienteId: Long) = mesaDao?.vincularMesa(mesaId, clienteId)
    suspend fun vincularComValorFixo(mesaId: Long, clienteId: Long, valorFixo: Double) = 
        mesaDao?.vincularMesaComValorFixo(mesaId, clienteId, valorFixo)
    suspend fun desvincularDeCliente(mesaId: Long) = mesaDao?.desvincularMesa(mesaId)
    suspend fun retirar(mesaId: Long) = mesaDao?.retirarMesa(mesaId)
    suspend fun atualizarRelogio(mesaId: Long, relogioInicial: Int, relogioFinal: Int, fichasInicial: Int, fichasFinal: Int) = 
        mesaDao?.atualizarRelogioMesa(mesaId, relogioInicial, relogioFinal, fichasInicial, fichasFinal)
    suspend fun atualizarRelogioFinal(mesaId: Long, relogioFinal: Int) = mesaDao?.atualizarRelogioFinal(mesaId, relogioFinal)
    suspend fun obterPorClienteDireto(clienteId: Long): List<Mesa> = mesaDao?.obterMesasPorClienteDireto(clienteId) ?: emptyList()
    suspend fun contarAtivasPorClientes(clienteIds: List<Long>) = mesaDao?.contarMesasAtivasPorClientes(clienteIds) ?: 0
    
    // ==================== MESA REFORMADA ====================
    
    /**
     * Obtém todas as mesas reformadas
     */
    fun obterTodasMesasReformadas(): Flow<List<MesaReformada>> {
        return mesaReformadaDao?.listarTodas() ?: flowOf(emptyList())
    }
    
    /**
     * Insere uma mesa reformada
     */
    suspend fun inserirMesaReformada(mesaReformada: MesaReformada): Long {
        return mesaReformadaDao?.inserir(mesaReformada) ?: 0L
    }
    
    /**
     * Obtém todos os PanoMesa
     * Para sincronização: busca todas as mesas e depois todos os panos de cada uma
     */
    suspend fun obterTodosPanoMesa(): List<PanoMesa> {
        return if (panoMesaDao != null && mesaDao != null) {
            val mesas = mesaDao.obterTodasMesas().first()
            mesas.flatMap { mesa ->
                panoMesaDao.buscarPorMesa(mesa.id).first()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Insere um PanoMesa
     */
    suspend fun inserirPanoMesa(panoMesa: PanoMesa): Long {
        return panoMesaDao?.inserir(panoMesa) ?: 0L
    }
    
    private fun logDbInsertStart(entity: String, details: String) {
        val stackTrace = Thread.currentThread().stackTrace
        Log.w("🔍 DB_POPULATION", "════════════════════════════════════════")
        Log.w("🔍 DB_POPULATION", "🚨 INSERINDO $entity: $details")
        Log.w("🔍 DB_POPULATION", "📍 Chamado por:")
        stackTrace.take(10).forEachIndexed { index, element ->
            Log.w("🔍 DB_POPULATION", "   [$index] $element")
        }
        Log.w("🔍 DB_POPULATION", "════════════════════════════════════════")
    }
    
    private fun logDbInsertSuccess(entity: String, details: String) {
        Log.w("🔍 DB_POPULATION", "✅ $entity inserido com sucesso: $details")
    }
    
    private fun logDbInsertError(entity: String, details: String, e: Exception) {
        Log.e("🔍 DB_POPULATION", "❌ Erro ao inserir $entity: $details", e)
    }
}

