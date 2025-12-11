package com.example.gestaobilhares.ui.routes

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.repository.AppRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.util.Date

/**
 * Testes unitários para RoutesViewModel
 * 
 * Testa as funcionalidades de:
 * - Carregamento de rotas
 * - Filtros
 * - Cálculos de totais
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoutesViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockRepository: AppRepository

    private lateinit var viewModel: RoutesViewModel
    
    private val rotasTeste = listOf(
        Rota(
            id = 1,
            nome = "Rota Norte",
            codigo = "RN001",
            descricao = "Rota do Norte",
            ativo = true,
            dataCriacao = Date(),
            dataUltimaAtualizacao = Date()
        ),
        Rota(
            id = 2,
            nome = "Rota Sul",
            codigo = "RS001",
            descricao = "Rota do Sul",
            ativo = true,
            dataCriacao = Date(),
            dataUltimaAtualizacao = Date()
        ),
        Rota(
            id = 3,
            nome = "Rota Inativa",
            codigo = "RI001",
            descricao = "Rota Desativada",
            ativo = false,
            dataCriacao = Date(),
            dataUltimaAtualizacao = Date()
        )
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        val testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        viewModel = RoutesViewModel(mockRepository)
    }

    @Test
    fun `carregarRotas deve filtrar apenas rotas ativas por padrao`() = runTest {
        // Arrange
        whenever(mockRepository.obterTodasRotas()).thenReturn(flowOf(rotasTeste))
        
        // Act
        viewModel.carregarRotas()
        
        // Assert
        viewModel.rotas.test {
            val rotas = awaitItem()
            assertThat(rotas).hasSize(2)
            assertThat(rotas.all { it.ativo }).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filtrarPorNome deve retornar apenas rotas que contenham o texto`() = runTest {
        // Arrange
        whenever(mockRepository.obterTodasRotas()).thenReturn(flowOf(rotasTeste))
        viewModel.carregarRotas()
        
        // Act
        viewModel.filtrarPorNome("Norte")
        
        // Assert
        viewModel.rotasFiltradas.test {
            val rotas = awaitItem()
            assertThat(rotas).hasSize(1)
            assertThat(rotas.first().nome).contains("Norte")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filtrarPorNome com texto vazio deve retornar todas as rotas`() = runTest {
        // Arrange
        whenever(mockRepository.obterTodasRotas()).thenReturn(flowOf(rotasTeste))
        viewModel.carregarRotas()
        
        // Act
        viewModel.filtrarPorNome("")
        
        // Assert
        viewModel.rotasFiltradas.test {
            val rotas = awaitItem()
            assertThat(rotas).hasSize(2) // apenas ativas
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filtrarPorNome deve ser case insensitive`() = runTest {
        // Arrange
        whenever(mockRepository.obterTodasRotas()).thenReturn(flowOf(rotasTeste))
        viewModel.carregarRotas()
        
        // Act
        viewModel.filtrarPorNome("NORTE")
        
        // Assert
        viewModel.rotasFiltradas.test {
            val rotas = awaitItem()
            assertThat(rotas).hasSize(1)
            assertThat(rotas.first().nome).contains("Norte")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
