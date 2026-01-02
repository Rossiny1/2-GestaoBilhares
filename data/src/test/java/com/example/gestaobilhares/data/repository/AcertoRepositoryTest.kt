package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.AcertoDao
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.repository.domain.AcertoRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import app.cash.turbine.test

class AcertoRepositoryTest {

    private lateinit var acertoDao: AcertoDao
    private lateinit var repository: AcertoRepository

    @Before
    fun setup() {
        acertoDao = mock()
        repository = AcertoRepository(acertoDao)
    }

    @Test
    fun `inserir deve chamar dao inserir e retornar id`() = runTest {
        // Arrange
        val now = System.currentTimeMillis()
        val acerto = Acerto(
            id = 0,
            rotaId = 1L,
            clienteId = 1L,
            valorRecebido = 100.0,
            dataAcerto = now,
            metodosPagamentoJson = "{\"DINHEIRO\": 100.0}",
            periodoInicio = now,
            periodoFim = now
        )
        val expectedId = 10L
        whenever(acertoDao.inserir(acerto)).thenReturn(expectedId)

        // Act
        val result = repository.inserir(acerto)

        // Assert
        verify(acertoDao).inserir(acerto)
        assertEquals(expectedId, result)
    }

    @Test
    fun `buscarPorCicloId deve chamar dao buscarPorCicloId e retornar fluxo`() = runTest {
        // Arrange
        val cicloId = 5L
        val now = System.currentTimeMillis()
        val acertos = listOf(
            Acerto(id = 1, rotaId = 1L, clienteId = 1L, valorRecebido = 50.0, dataAcerto = now, metodosPagamentoJson = "PIX", cicloId = cicloId, periodoInicio = now, periodoFim = now),
            Acerto(id = 2, rotaId = 1L, clienteId = 2L, valorRecebido = 150.0, dataAcerto = now, metodosPagamentoJson = "DINHEIRO", cicloId = cicloId, periodoInicio = now, periodoFim = now)
        )
        whenever(acertoDao.buscarPorCicloId(cicloId)).thenReturn(flowOf(acertos))

        // Act & Assert
        repository.buscarPorCicloId(cicloId).test {
            assertEquals(acertos, awaitItem())
            awaitComplete()
        }
        verify(acertoDao).buscarPorCicloId(cicloId)
    }

    @Test
    fun `buscarPorRotaECicloId deve chamar dao buscarPorRotaECicloId e retornar fluxo`() = runTest {
        // Arrange
        val rotaId = 2L
        val cicloId = 10L
        val now = System.currentTimeMillis()
        val acertos = listOf(
            Acerto(id = 3, rotaId = rotaId, clienteId = 5L, valorRecebido = 200.0, dataAcerto = now, metodosPagamentoJson = "CARTAO", periodoInicio = now, periodoFim = now)
        )
        whenever(acertoDao.buscarPorRotaECicloId(rotaId, cicloId)).thenReturn(flowOf(acertos))

        // Act & Assert
        repository.buscarPorRotaECicloId(rotaId, cicloId).test {
            assertEquals(acertos, awaitItem())
            awaitComplete()
        }
        verify(acertoDao).buscarPorRotaECicloId(rotaId, cicloId)
    }

    @Test
    fun `obterPorCliente deve chamar dao buscarPorCliente`() = runTest {
        // Arrange
        val clienteId = 99L
        val now = System.currentTimeMillis()
        val acertos = listOf(Acerto(id = 1, rotaId = 1L, clienteId = clienteId, valorRecebido = 10.0, dataAcerto = now, periodoInicio = now, periodoFim = now))
        whenever(acertoDao.buscarPorCliente(clienteId)).thenReturn(flowOf(acertos))

        // Act & Assert
        repository.obterPorCliente(clienteId).test {
            assertEquals(acertos, awaitItem())
            awaitComplete()
        }
        verify(acertoDao).buscarPorCliente(clienteId)
    }
}
