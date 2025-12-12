package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.AcertoDao
import com.example.gestaobilhares.data.dao.CicloAcertoDao
import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.dao.RotaDao
import com.example.gestaobilhares.data.entities.Rota
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.*
import java.util.Date

class RotaRepositoryTest {

    private val rotaDao: RotaDao = mock()
    private val clienteDao: ClienteDao = mock()
    private val acertoDao: AcertoDao = mock()
    private val cicloAcertoDao: CicloAcertoDao = mock()
    
    private val repository = RotaRepository(rotaDao, clienteDao, acertoDao, cicloAcertoDao)

    @Test
    fun `inserir deve retornar ID quando insercao for bem sucedida (Novo)`() = runTest {
        // Arrange
        val rota = criarRotaSimples(id = 0)
        whenever(rotaDao.insertRota(rota)).thenReturn(100L)

        // Act
        val id = repository.inserir(rota)

        // Assert
        assertEquals(100L, id)
        verify(rotaDao).insertRota(rota)
        verify(rotaDao, never()).updateRota(any())
    }

    @Test
    fun `inserir deve chamar update e retornar ID existente quando houver conflito (Pre-existente)`() = runTest {
        // Arrange
        val rotaId = 50L
        val rota = criarRotaSimples(id = rotaId)
        // Simula conflito (IGNORE retorna -1)
        whenever(rotaDao.insertRota(rota)).thenReturn(-1L)
        
        // Act
        val id = repository.inserir(rota)

        // Assert
        assertEquals(rotaId, id)
        verify(rotaDao).insertRota(rota)
        verify(rotaDao).updateRota(rota) // Deve chamar update para garantir upsert sem delete
    }

    private fun criarRotaSimples(id: Long): Rota {
        return Rota(
            id = id,
            nome = "Rota Teste",
            dataCriacao = System.currentTimeMillis(),
            dataAtualizacao = System.currentTimeMillis()
        )
    }
}
