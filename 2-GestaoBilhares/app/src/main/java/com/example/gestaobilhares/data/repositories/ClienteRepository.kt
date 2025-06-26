package com.example.gestaobilhares.data.repositories

import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.entities.Cliente
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository para operações relacionadas a clientes
 * 
 * Implementa o padrão Repository para abstrair a camada de dados
 * e fornecer uma interface limpa para os ViewModels.
 */
@Singleton
class ClienteRepository @Inject constructor(
    private val clienteDao: ClienteDao
) {

    /**
     * Obtém todos os clientes por rota
     */
    fun obterClientesPorRota(rotaId: Long): Flow<List<Cliente>> {
        return clienteDao.obterClientesPorRota(rotaId)
    }

    /**
     * Insere um novo cliente
     */
    suspend fun inserir(cliente: Cliente): Long {
        return clienteDao.inserir(cliente)
    }

    /**
     * Atualiza um cliente
     */
    suspend fun atualizar(cliente: Cliente) {
        clienteDao.atualizar(cliente)
    }

    /**
     * Deleta um cliente
     */
    suspend fun deletar(cliente: Cliente) {
        clienteDao.deletar(cliente)
    }

    /**
     * Obtém cliente por ID
     */
    suspend fun obterPorId(id: Long): Cliente? {
        return clienteDao.obterPorId(id)
    }

    /**
     * Obtém todos os clientes
     */
    fun obterTodos(): Flow<List<Cliente>> {
        return clienteDao.obterTodos()
    }
} 
