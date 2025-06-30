package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.AcertoDao
import com.example.gestaobilhares.data.entities.Acerto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcertoRepository @Inject constructor(
    private val acertoDao: AcertoDao
) {
    suspend fun inserir(acerto: Acerto): Long = acertoDao.inserir(acerto)
    fun buscarPorCliente(clienteId: Long): Flow<List<Acerto>> = acertoDao.buscarPorCliente(clienteId)
    fun listarTodos(): Flow<List<Acerto>> = acertoDao.listarTodos()
    suspend fun buscarPorId(id: Long): Acerto? = acertoDao.buscarPorId(id)
    suspend fun atualizar(acerto: Acerto) = acertoDao.atualizar(acerto)
    suspend fun deletar(acerto: Acerto) = acertoDao.deletar(acerto)
} 