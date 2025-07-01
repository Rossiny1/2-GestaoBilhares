package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.AcertoMesaDao
import com.example.gestaobilhares.data.entities.AcertoMesa
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcertoMesaRepository @Inject constructor(
    private val acertoMesaDao: AcertoMesaDao
) {
    suspend fun inserir(acertoMesa: AcertoMesa): Long = acertoMesaDao.inserir(acertoMesa)
    
    suspend fun inserirLista(acertoMesas: List<AcertoMesa>) = acertoMesaDao.inserirLista(acertoMesas)
    
    fun buscarPorAcerto(acertoId: Long): Flow<List<AcertoMesa>> = acertoMesaDao.buscarPorAcerto(acertoId)
    
    fun buscarPorMesa(mesaId: Long): Flow<List<AcertoMesa>> = acertoMesaDao.buscarPorMesa(mesaId)
    
    suspend fun buscarUltimoAcertoMesa(mesaId: Long): AcertoMesa? = acertoMesaDao.buscarUltimoAcertoMesa(mesaId)
    
    suspend fun buscarPorAcertoId(acertoId: Long): List<AcertoMesa> = acertoMesaDao.buscarPorAcertoId(acertoId)
    
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