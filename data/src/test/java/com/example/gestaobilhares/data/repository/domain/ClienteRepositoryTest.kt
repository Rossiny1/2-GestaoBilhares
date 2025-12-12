package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.entities.Cliente
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.*
import java.util.Date

class ClienteRepositoryTest {

    private val clienteDao: ClienteDao = mock()
    private val repository = ClienteRepository(clienteDao)

    @Test
    fun `inserir deve retornar ID quando insercao for bem sucedida via INSERT`() = runTest {
        // Arrange
        val cliente = criarClienteSimples(id = 0)
        whenever(clienteDao.inserir(cliente)).thenReturn(100L)

        // Act
        val id = repository.inserir(cliente)

        // Assert
        assertEquals(100L, id)
        verify(clienteDao).inserir(cliente)
        verify(clienteDao, never()).atualizar(any())
    }

    @Test
    fun `inserir deve chamar atualizar e retornar ID do objeto quando houver conflito no INSERT`() = runTest {
        // Arrange
        val clienteId = 50L
        val cliente = criarClienteSimples(id = clienteId)
        
        // Simula conflito (OnConflictStrategy.IGNORE retorna -1)
        whenever(clienteDao.inserir(cliente)).thenReturn(-1L)
        
        // Act
        val id = repository.inserir(cliente)

        // Assert
        assertEquals(clienteId, id) // Deve retornar o ID original do cliente
        verify(clienteDao).inserir(cliente) // Tentou inserir
        verify(clienteDao).atualizar(cliente) // Chamou atualizar (UPSERT)
    }

    private fun criarClienteSimples(id: Long): Cliente {
        return Cliente(
            id = id,
            nome = "Cliente Teste",
            telefone = "123456789",
            rotaId = 1L
            // dataCadastro e dataUltimaAtualizacao têm defaults
        )
    }
}
