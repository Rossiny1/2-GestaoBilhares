package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.DespesaDao
import com.example.gestaobilhares.data.entities.Despesa
import kotlinx.coroutines.flow.Flow
import android.util.Log

/**
 * Repository especializado para operações relacionadas a despesas.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 */
class DespesaRepository(
    private val despesaDao: DespesaDao
) {
    
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

