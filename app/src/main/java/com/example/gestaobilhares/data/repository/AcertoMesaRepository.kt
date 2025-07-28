package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.AcertoMesaDao
import com.example.gestaobilhares.data.entities.AcertoMesa
import kotlinx.coroutines.flow.Flow

class AcertoMesaRepository(
    private val acertoMesaDao: AcertoMesaDao
) {
    suspend fun inserir(acertoMesa: AcertoMesa): Long = acertoMesaDao.inserir(acertoMesa)
    
    suspend fun inserirLista(acertoMesas: List<AcertoMesa>) = acertoMesaDao.inserirLista(acertoMesas)
    
    fun buscarPorAcerto(acertoId: Long): Flow<List<AcertoMesa>> = acertoMesaDao.buscarPorAcerto(acertoId)
    
    fun buscarPorMesa(mesaId: Long): Flow<List<AcertoMesa>> = acertoMesaDao.buscarPorMesa(mesaId)
    
    suspend fun buscarUltimoAcertoMesa(mesaId: Long): AcertoMesa? = acertoMesaDao.buscarUltimoAcertoMesa(mesaId)
    
    suspend fun buscarPorAcertoId(acertoId: Long): List<AcertoMesa> = acertoMesaDao.buscarPorAcertoId(acertoId)
    
    /**
     * ✅ NOVO: Busca um acerto mesa específico por acerto e mesa
     * @param acertoId ID do acerto
     * @param mesaId ID da mesa
     * @return AcertoMesa específico ou null se não encontrado
     */
    suspend fun buscarAcertoMesaPorAcertoEMesa(acertoId: Long, mesaId: Long): AcertoMesa? = 
        acertoMesaDao.buscarAcertoMesaPorAcertoEMesa(acertoId, mesaId)
    
    /**
     * ✅ NOVO: Busca os últimos acertos de uma mesa para calcular média
     * @param mesaId ID da mesa
     * @param limite Máximo de acertos a buscar (padrão 5)
     * @return Lista dos últimos acertos da mesa
     */
    suspend fun buscarUltimosAcertosMesa(mesaId: Long, limite: Int = 5): List<AcertoMesa> {
        return try {
            acertoMesaDao.buscarUltimosAcertosMesa(mesaId, limite)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * ✅ NOVO: Calcula a média de fichas jogadas dos últimos acertos de uma mesa
     * @param mesaId ID da mesa
     * @param limite Máximo de acertos a considerar (padrão 5)
     * @return Média de fichas jogadas, ou 0 se não houver acertos anteriores
     */
    suspend fun calcularMediaFichasJogadas(mesaId: Long, limite: Int = 5): Double {
        return try {
            val ultimosAcertos = buscarUltimosAcertosMesa(mesaId, limite)
            if (ultimosAcertos.isEmpty()) {
                0.0
            } else {
                val totalFichas = ultimosAcertos.sumOf { it.fichasJogadas }
                totalFichas.toDouble() / ultimosAcertos.size
            }
        } catch (e: Exception) {
            0.0
        }
    }
    
    suspend fun atualizar(acertoMesa: AcertoMesa) = acertoMesaDao.atualizar(acertoMesa)
    
    suspend fun deletar(acertoMesa: AcertoMesa) = acertoMesaDao.deletar(acertoMesa)
    
    suspend fun deletarPorAcerto(acertoId: Long) = acertoMesaDao.deletarPorAcerto(acertoId)
    
    /**
     * Busca o relógio final do último acerto de uma mesa específica
     * @param mesaId ID da mesa
     * @return Relógio final do último acerto, ou null se não houver acertos anteriores
     */
    suspend fun buscarRelogioFinalUltimoAcerto(mesaId: Long): Int? {
        return try {
            val ultimoAcertoMesa = buscarUltimoAcertoMesa(mesaId)
            ultimoAcertoMesa?.relogioFinal
        } catch (e: Exception) {
            null
        }
    }
} 