package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.AcertoDao
import com.example.gestaobilhares.data.entities.Acerto
import kotlinx.coroutines.flow.Flow
import android.util.Log

/**
 * Repository especializado para operações relacionadas a acertos.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 */
class AcertoRepository(
    private val acertoDao: AcertoDao
) {
    
    fun obterPorCliente(clienteId: Long) = acertoDao.buscarPorCliente(clienteId)
    suspend fun obterPorId(id: Long) = acertoDao.buscarPorId(id)
    suspend fun buscarUltimoPorCliente(clienteId: Long) = acertoDao.buscarUltimoAcertoPorCliente(clienteId)
    fun obterTodos() = acertoDao.listarTodos()
    fun buscarPorCicloId(cicloId: Long) = acertoDao.buscarPorCicloId(cicloId)
    fun buscarPorRotaECicloId(rotaId: Long, cicloId: Long) = acertoDao.buscarPorRotaECicloId(rotaId, cicloId)
    fun buscarPorClienteECicloId(clienteId: Long, cicloId: Long) = acertoDao.buscarPorClienteECicloId(clienteId, cicloId)
    
    suspend fun inserir(acerto: Acerto): Long {
        logDbInsertStart("ACERTO", "ClienteID=${acerto.clienteId}, RotaID=${acerto.rotaId}, Valor=${acerto.valorRecebido}")
        return try {
            val id = acertoDao.inserir(acerto)
            logDbInsertSuccess("ACERTO", "ClienteID=${acerto.clienteId}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("ACERTO", "ClienteID=${acerto.clienteId}", e)
            throw e
        }
    }
    
    suspend fun atualizar(acerto: Acerto) = acertoDao.atualizar(acerto)
    suspend fun deletar(acerto: Acerto) = acertoDao.deletar(acerto)
    suspend fun buscarUltimoPorMesa(mesaId: Long) = acertoDao.buscarUltimoAcertoPorMesa(mesaId)
    suspend fun buscarObservacaoUltimoAcerto(clienteId: Long) = acertoDao.buscarObservacaoUltimoAcerto(clienteId)
    suspend fun buscarUltimosPorClientes(clienteIds: List<Long>) = acertoDao.buscarUltimosAcertosPorClientes(clienteIds)
    
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

