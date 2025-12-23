package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.DespesaDao
import com.example.gestaobilhares.data.entities.Despesa
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Repository especializado para operações relacionadas a despesas.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 */
class DespesaRepository(
    private val despesaDao: DespesaDao
) {
    
    // ✅ NOVO: Método para obter todas as despesas diretamente (para sincronização)
    // Retorna Despesa (sem JOIN) - necessário para push/pull
    fun obterTodasDespesas() = despesaDao.buscarTodas()
    
    // Método que retorna DespesaResumo (com JOIN) - para exibição
    fun obterTodas() = despesaDao.buscarTodasComRota()
    fun obterPorRota(rotaId: Long) = despesaDao.buscarPorRota(rotaId)
    suspend fun obterPorId(id: Long) = despesaDao.buscarPorId(id)
    fun buscarPorCicloId(cicloId: Long) = despesaDao.buscarPorCicloId(cicloId)
    fun buscarPorRotaECicloId(rotaId: Long, cicloId: Long) = despesaDao.buscarPorRotaECicloId(rotaId, cicloId)
    
    suspend fun inserir(despesa: Despesa): Long {
        logDbInsertStart("DESPESA", "Descricao=${despesa.descricao}, RotaID=${despesa.rotaId}")
        return try {
            val id = despesaDao.inserir(despesa)
            logDbInsertSuccess("DESPESA", "Descricao=${despesa.descricao}, ID=$id")
            id
        } catch (e: Exception) {
            logDbInsertError("DESPESA", "Descricao=${despesa.descricao}", e)
            throw e
        }
    }
    
    suspend fun atualizar(despesa: Despesa) = despesaDao.atualizar(despesa)
    suspend fun deletar(despesa: Despesa) = despesaDao.deletar(despesa)
    suspend fun calcularTotalPorRota(rotaId: Long) = despesaDao.calcularTotalPorRota(rotaId)
    suspend fun calcularTotalGeral() = despesaDao.calcularTotalGeral()
    suspend fun contarPorRota(rotaId: Long) = despesaDao.contarPorRota(rotaId)
    suspend fun deletarPorRota(rotaId: Long) = despesaDao.deletarPorRota(rotaId)
    suspend fun buscarGlobaisPorCiclo(ano: Int, numero: Int) = despesaDao.buscarGlobaisPorCiclo(ano, numero)
    suspend fun somarGlobaisPorCiclo(ano: Int, numero: Int) = despesaDao.somarGlobaisPorCiclo(ano, numero)
    
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

