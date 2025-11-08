package com.example.gestaobilhares.ui.clients

import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.StatusRota
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.utils.UserSessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * ✅ FASE 12.2: Testes unitários para ClientListViewModel
 * 
 * Testa funcionalidades críticas:
 * - Carregamento de clientes por rota
 * - Filtros de clientes
 * - Carregamento de rota
 * - Estados reativos
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientListViewModelTest {

    @Mock
    private lateinit var mockAppRepository: AppRepository

    @Mock
    private lateinit var mockUserSessionManager: UserSessionManager

    private lateinit var viewModel: ClientListViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = ClientListViewModel(mockAppRepository, mockUserSessionManager)
    }

    @Test
    fun `carregarClientes deve carregar clientes da rota`() = runTest(testDispatcher) {
        // Arrange
        val rotaId = 1L
        val clientesMock = listOf(
            Cliente(id = 1, nome = "Cliente 1", rotaId = rotaId, debitoAtual = 100.0),
            Cliente(id = 2, nome = "Cliente 2", rotaId = rotaId, debitoAtual = 200.0)
        )
        
        whenever(mockAppRepository.obterClientesPorRotaComDebitoAtual(rotaId))
            .thenReturn(flowOf(clientesMock))

        // Act
        viewModel.carregarClientes(rotaId)
        advanceUntilIdle()

        // Assert
        val clientes = viewModel.clientes.first()
        assertEquals("Deve carregar 2 clientes", 2, clientes.size)
        assertEquals("Primeiro cliente deve ser Cliente 1", "Cliente 1", clientes[0].nome)
        verify(mockAppRepository).obterClientesPorRotaComDebitoAtual(rotaId)
    }

    @Test
    fun `carregarRota deve carregar informações da rota`() = runTest(testDispatcher) {
        // Arrange
        val rotaId = 1L
        val rotaMock = Rota(
            id = rotaId,
            nome = "Rota Teste",
            statusAtual = StatusRota.EM_ANDAMENTO,
            cicloAcertoAtual = 1
        )
        
        whenever(mockAppRepository.obterRotaPorId(rotaId))
            .thenReturn(rotaMock)

        // Act
        viewModel.carregarRota(rotaId)
        advanceUntilIdle()

        // Assert
        val rotaInfo = viewModel.rotaInfo.first()
        assertNotNull("Rota deve ser carregada", rotaInfo)
        assertEquals("Nome da rota deve ser correto", "Rota Teste", rotaInfo?.nome)
        verify(mockAppRepository).obterRotaPorId(rotaId)
    }

    @Test
    fun `aplicarFiltro deve filtrar clientes corretamente`() = runTest(testDispatcher) {
        // Arrange
        val rotaId = 1L
        val clientesMock = listOf(
            Cliente(id = 1, nome = "Cliente 1", rotaId = rotaId, debitoAtual = 0.0),
            Cliente(id = 2, nome = "Cliente 2", rotaId = rotaId, debitoAtual = 500.0)
        )
        
        whenever(mockAppRepository.obterClientesPorRotaComDebitoAtual(rotaId))
            .thenReturn(flowOf(clientesMock))

        // Act
        viewModel.carregarClientes(rotaId)
        advanceUntilIdle()
        viewModel.aplicarFiltro(FiltroCliente.ACERTADOS)
        advanceUntilIdle()

        // Assert
        val clientes = viewModel.clientes.first()
        assertEquals("Deve filtrar apenas clientes acertados", 1, clientes.size)
        assertEquals("Cliente acertado deve ser Cliente 1", "Cliente 1", clientes[0].nome)
    }

    @Test
    fun `aplicarFiltro PENDENCIAS deve filtrar clientes com débito`() = runTest(testDispatcher) {
        // Arrange
        val rotaId = 1L
        val clientesMock = listOf(
            Cliente(id = 1, nome = "Cliente 1", rotaId = rotaId, debitoAtual = 0.0),
            Cliente(id = 2, nome = "Cliente 2", rotaId = rotaId, debitoAtual = 500.0)
        )
        
        whenever(mockAppRepository.obterClientesPorRotaComDebitoAtual(rotaId))
            .thenReturn(flowOf(clientesMock))

        // Act
        viewModel.carregarClientes(rotaId)
        advanceUntilIdle()
        viewModel.aplicarFiltro(FiltroCliente.PENDENCIAS)
        advanceUntilIdle()

        // Assert
        val clientes = viewModel.clientes.first()
        assertEquals("Deve filtrar apenas clientes com pendências", 1, clientes.size)
        assertEquals("Cliente com pendência deve ser Cliente 2", "Cliente 2", clientes[0].nome)
    }

    @Test
    fun `filtroAtual deve iniciar como TODOS`() = runTest(testDispatcher) {
        // Assert
        val filtro = viewModel.filtroAtual.first()
        assertEquals("Filtro inicial deve ser TODOS", FiltroCliente.TODOS, filtro)
    }
}

