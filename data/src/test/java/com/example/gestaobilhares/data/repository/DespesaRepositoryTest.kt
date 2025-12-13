package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.DespesaDao
import com.example.gestaobilhares.data.entities.Despesa
import com.example.gestaobilhares.data.repository.domain.DespesaRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import app.cash.turbine.test

class DespesaRepositoryTest {

    private lateinit var despesaDao: DespesaDao
    private lateinit var repository: DespesaRepository

    @Before
    fun setup() {
        despesaDao = mock()
        repository = DespesaRepository(despesaDao)
    }

    @Test
    fun `inserir deve chamar dao inserir e retornar id`() = runTest {
        // Arrange
        val despesa = Despesa(
            id = 0,
            rotaId = 1L,
            descricao = "Gasolina",
            valor = 100.0,
            categoria = "Combustivel",
            origemLancamento = "ROTA"
        )
        val expectedId = 20L
        whenever(despesaDao.inserir(despesa)).thenReturn(expectedId)

        // Act
        val result = repository.inserir(despesa)

        // Assert
        verify(despesaDao).inserir(despesa)
        assertEquals(expectedId, result)
    }

    @Test
    fun `buscarPorCicloId deve chamar dao buscarPorCicloId e retornar fluxo`() = runTest {
        // Arrange
        val cicloId = 10L
        val despesas = listOf(
            Despesa(id = 1, rotaId = 1L, descricao = "Almoço", valor = 50.0, categoria = "Alimentacao", cicloId = cicloId),
            Despesa(id = 2, rotaId = 1L, descricao = "Jantar", valor = 80.0, categoria = "Alimentacao", cicloId = cicloId)
        )
        whenever(despesaDao.buscarPorCicloId(cicloId)).thenReturn(flowOf(despesas))

        // Act & Assert
        repository.buscarPorCicloId(cicloId).test {
            assertEquals(despesas, awaitItem())
            awaitComplete()
        }
        verify(despesaDao).buscarPorCicloId(cicloId)
    }

    @Test
    fun `obterPorRota deve chamar dao buscarPorRota`() = runTest {
        // Arrange
        val rotaId = 5L
        val despesas = listOf(
            Despesa(id = 3, rotaId = rotaId, descricao = "Peças", valor = 200.0, categoria = "Manutencao")
        )
        whenever(despesaDao.buscarPorRota(rotaId)).thenReturn(flowOf(despesas))

        // Act & Assert
        repository.obterPorRota(rotaId).test {
            assertEquals(despesas, awaitItem())
            awaitComplete()
        }
        verify(despesaDao).buscarPorRota(rotaId)
    }

    @Test
    fun `buscarGlobaisPorCiclo deve chamar dao buscarGlobaisPorCiclo`() = runTest {
        // Arrange
        val ano = 2024
        val numero = 5
        val despesasGlobais = listOf(
            Despesa(id = 4, rotaId = 0L, descricao = "Sede", valor = 500.0, categoria = "Aluguel", origemLancamento = "GLOBAL", cicloAno = ano, cicloNumero = numero)
        )
        whenever(despesaDao.buscarGlobaisPorCiclo(ano, numero)).thenReturn(despesasGlobais)

        // Act
        val result = repository.buscarGlobaisPorCiclo(ano, numero)

        // Assert
        assertEquals(despesasGlobais, result)
        verify(despesaDao).buscarGlobaisPorCiclo(ano, numero)
    }

    @Test
    fun `somarGlobaisPorCiclo deve chamar dao somarGlobaisPorCiclo`() = runTest {
        // Arrange
        val ano = 2024
        val numero = 5
        val expectedTotal = 1500.0
        whenever(despesaDao.somarGlobaisPorCiclo(ano, numero)).thenReturn(expectedTotal)

        // Act
        val result = repository.somarGlobaisPorCiclo(ano, numero)

        // Assert
        assertEquals(expectedTotal, result, 0.0)
        verify(despesaDao).somarGlobaisPorCiclo(ano, numero)
    }
}
