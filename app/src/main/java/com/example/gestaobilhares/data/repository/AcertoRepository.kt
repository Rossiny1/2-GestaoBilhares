package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.AcertoDao
import com.example.gestaobilhares.data.entities.Acerto
import kotlinx.coroutines.flow.Flow

class AcertoRepository(
    private val acertoDao: AcertoDao
) {
    suspend fun inserir(acerto: Acerto): Long = acertoDao.inserir(acerto)
    fun buscarPorCliente(clienteId: Long): Flow<List<Acerto>> = acertoDao.buscarPorCliente(clienteId)
    fun listarTodos(): Flow<List<Acerto>> = acertoDao.listarTodos()
    suspend fun buscarPorId(id: Long): Acerto? {
        return acertoDao.buscarPorId(id)
    }
    suspend fun atualizar(acerto: Acerto) {
        acertoDao.atualizar(acerto)
    }
    suspend fun deletar(acerto: Acerto) = acertoDao.deletar(acerto)
    
    /**
     * Busca o último acerto de uma mesa específica
     * @param mesaId ID da mesa
     * @return Último acerto da mesa, ou null se não houver
     */
    suspend fun buscarUltimoAcertoMesa(mesaId: Long): Acerto? {
        return try {
            acertoDao.buscarUltimoAcertoPorMesa(mesaId)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun buscarUltimoAcertoPorCliente(clienteId: Long): Acerto? {
        return acertoDao.buscarUltimoAcertoPorCliente(clienteId)
    }

    /**
     * ✅ NOVO: Busca a observação do último acerto de um cliente
     * @param clienteId ID do cliente
     * @return Observação do último acerto, ou null se não houver
     */
    suspend fun buscarObservacaoUltimoAcerto(clienteId: Long): String? {
        return try {
            acertoDao.buscarObservacaoUltimoAcerto(clienteId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * ✅ NOVO: Busca acertos por rota e ciclo específico
     * @param rotaId ID da rota
     * @param cicloAcerto Número do ciclo de acerto
     * @return Flow com lista de acertos da rota e ciclo
     */
    fun buscarPorRotaECiclo(rotaId: Long, cicloAcerto: Int): Flow<List<Acerto>> {
        return acertoDao.buscarPorRotaECiclo(rotaId, cicloAcerto)
    }
} 