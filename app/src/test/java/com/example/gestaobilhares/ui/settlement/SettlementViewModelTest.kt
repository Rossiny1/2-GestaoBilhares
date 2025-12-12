package com.example.gestaobilhares.ui.settlement

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.testutils.TestDataFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Testes unitários para SettlementViewModel.
 * 
 * Testa os principais fluxos:
 * - Carregamento de dados do cliente para acerto
 * - Carregamento de mesas do cliente
 * - Carregamento do histórico de acertos
 * - Busca de débito anterior
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettlementViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var appRepository: AppRepository

    private lateinit var viewModel: SettlementViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = SettlementViewModel(appRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadClientForSettlement deve carregar cliente e suas mesas`() = runTest {
        // Given
        val clienteId = 1L
        val cliente = TestDataFactory.createCliente(id = clienteId)
        val mesas = listOf(
            TestDataFactory.createMesa(id = 1L, clienteId = clienteId),
            TestDataFactory.createMesa(id = 2L, clienteId = clienteId)
        )
        
        whenever(appRepository.obterClientePorId(clienteId)).thenReturn(cliente)
        whenever(appRepository.buscarMesasPorCliente(clienteId)).thenReturn(flowOf(mesas))
        whenever(appRepository.buscarAcertosPorCliente(clienteId)).thenReturn(flowOf(emptyList()))
        whenever(appRepository.obterDebitoAtual(clienteId)).thenReturn(0.0)

        // When
        viewModel.loadClientForSettlement(clienteId)
        advanceUntilIdle()

        // Then
        verify(appRepository).obterClientePorId(clienteId)
        verify(appRepository).buscarMesasPorCliente(clienteId)
    }

    @Test
    fun `carregarHistoricoAcertos deve carregar acertos do cliente`() = runTest {
        // Given
        val clienteId = 1L
        val acertos = listOf(
            TestDataFactory.createAcerto(id = 1L, clienteId = clienteId),
            TestDataFactory.createAcerto(id = 2L, clienteId = clienteId)
        )
        
        whenever(appRepository.buscarAcertosPorCliente(clienteId))
            .thenReturn(flowOf(acertos))

        // When
        viewModel.carregarHistoricoAcertos(clienteId)
        advanceUntilIdle()

        // Then
        verify(appRepository).buscarAcertosPorCliente(clienteId)
    }

    @Test
    fun `buscarDebitoAnterior deve retornar debito quando nao esta em edicao`() = runTest {
        // Given
        val clienteId = 1L
        val debitoEsperado = 150.0
        
        whenever(appRepository.obterDebitoAtual(clienteId)).thenReturn(debitoEsperado)

        // When
        viewModel.buscarDebitoAnterior(clienteId, acertoIdParaEdicao = null)
        advanceUntilIdle()

        // Then
        verify(appRepository).obterDebitoAtual(clienteId)
    }

    @Test
    fun `resetarResultadoSalvamento deve limpar o resultado`() {
        // When
        viewModel.resetarResultadoSalvamento()

        // Then
        assertThat(viewModel.resultadoSalvamento.value).isNull()
    }

    @Test
    fun `setLoading deve atualizar estado de loading`() {
        // When
        viewModel.setLoading(true)

        // Then
        assertThat(viewModel.isLoading.value).isTrue()

        // When
        viewModel.setLoading(false)

        // Then
        assertThat(viewModel.isLoading.value).isFalse()
    }

    @Test
    fun `buscarAcertoPorId deve retornar acerto quando existe`() = runTest {
        // Given
        val acertoId = 1L
        val acerto = TestDataFactory.createAcerto(id = acertoId)
        
        whenever(appRepository.buscarAcertoPorId(acertoId)).thenReturn(acerto)

        // When
        viewModel.buscarAcertoPorId(acertoId)
        advanceUntilIdle()

        // Then
        verify(appRepository).buscarAcertoPorId(acertoId)
    }
}
