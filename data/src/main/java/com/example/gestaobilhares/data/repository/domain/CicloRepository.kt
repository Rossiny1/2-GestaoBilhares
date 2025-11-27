package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.CicloAcertoDao
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import kotlinx.coroutines.flow.Flow
import android.util.Log

/**
 * Repository especializado para operações relacionadas a ciclos de acerto.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 */
class CicloRepository(
    private val cicloAcertoDao: CicloAcertoDao
) {
    
    fun obterTodos(): Flow<List<CicloAcertoEntity>> = cicloAcertoDao.listarTodos()
    suspend fun buscarUltimoFinalizadoPorRota(rotaId: Long) = cicloAcertoDao.buscarUltimoCicloFinalizadoPorRota(rotaId)
    suspend fun buscarPorRotaEAno(rotaId: Long, ano: Int) = cicloAcertoDao.buscarCiclosPorRotaEAno(rotaId, ano)
    suspend fun buscarPorRota(rotaId: Long) = cicloAcertoDao.buscarCiclosPorRota(rotaId)
    suspend fun buscarProximoNumero(rotaId: Long, ano: Int) = cicloAcertoDao.buscarProximoNumeroCiclo(rotaId, ano)
    suspend fun buscarAtivo(rotaId: Long) = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
    suspend fun buscarPorId(cicloId: Long) = cicloAcertoDao.buscarPorId(cicloId)
    
    suspend fun inserir(ciclo: CicloAcertoEntity): Long {
        logDbInsertStart("CICLO", "RotaID=${ciclo.rotaId}, Numero=${ciclo.numeroCiclo}, Status=${ciclo.status}")
        return try {
            val id = cicloAcertoDao.inserir(ciclo)
            logDbInsertSuccess("CICLO", "ID=$id, RotaID=${ciclo.rotaId}")
            id
        } catch (e: Exception) {
            logDbInsertError("CICLO", "RotaID=${ciclo.rotaId}", e)
            throw e
        }
    }
    
    suspend fun buscarParaMetas(rotaId: Long): List<CicloAcertoEntity> {
        val cicloEmAndamento = cicloAcertoDao.buscarCicloEmAndamento(rotaId)
        val ciclosFuturos = cicloAcertoDao.buscarCiclosFuturosPorRota(rotaId)
        
        val listaCombinada = mutableListOf<CicloAcertoEntity>()
        cicloEmAndamento?.let { listaCombinada.add(it) }
        listaCombinada.addAll(ciclosFuturos)
        
        return listaCombinada
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

