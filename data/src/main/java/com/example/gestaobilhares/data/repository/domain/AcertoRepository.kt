package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.AcertoDao
import com.example.gestaobilhares.data.entities.Acerto
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Repository especializado para operações relacionadas a acertos.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 */
class AcertoRepository(
    private val acertoDao: AcertoDao
) {
    
    fun obterPorCliente(clienteId: Long) = acertoDao.buscarPorCliente(clienteId)
    fun obterRecentesPorCliente(clienteId: Long, limit: Int) = acertoDao.buscarRecentesPorCliente(clienteId, limit)
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
    
    suspend fun atualizar(acerto: Acerto): Int = acertoDao.atualizar(acerto)
    suspend fun deletar(acerto: Acerto) = acertoDao.deletar(acerto)
    suspend fun buscarUltimoPorMesa(mesaId: Long) = acertoDao.buscarUltimoAcertoPorMesa(mesaId)
    suspend fun buscarObservacaoUltimoAcerto(clienteId: Long) = acertoDao.buscarObservacaoUltimoAcerto(clienteId)
    suspend fun buscarUltimosPorClientes(clienteIds: List<Long>) = acertoDao.buscarUltimosAcertosPorClientes(clienteIds)
    suspend fun removerAcertosExcedentes(clienteId: Long, limit: Int) = acertoDao.removerAcertosExcedentes(clienteId, limit)
    suspend fun buscarPorPeriodo(clienteId: Long, inicio: Long, fim: Long) = acertoDao.buscarPorPeriodo(clienteId, inicio, fim)
    
    private fun logDbInsertStart(entity: String, details: String) {
        val stackTrace = Thread.currentThread().stackTrace
        Timber.tag("🔍 DB_POPULATION").w("════════════════════════════════════════")
        Timber.tag("🔍 DB_POPULATION").w("🚨 INSERINDO $entity: $details")
        Timber.tag("🔍 DB_POPULATION").w("📍 Chamado por:")
        stackTrace.take(10).forEachIndexed { index, element ->
            Timber.tag("🔍 DB_POPULATION").w("   [$index] $element")
        }
        Timber.tag("🔍 DB_POPULATION").w("════════════════════════════════════════")
    }
    
    private fun logDbInsertSuccess(entity: String, details: String) {
        Timber.tag("🔍 DB_POPULATION").w("✅ $entity inserido com sucesso: $details")
    }
    
    private fun logDbInsertError(entity: String, details: String, e: Exception) {
        Timber.tag("🔍 DB_POPULATION").e(e, "❌ Erro ao inserir $entity: $details")
    }
}

