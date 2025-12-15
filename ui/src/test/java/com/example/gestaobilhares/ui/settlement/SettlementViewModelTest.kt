package com.example.gestaobilhares.ui.settlement

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.StatusAcerto
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
import java.util.Date

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
    fun `loadClientForSettlement deve carregar cliente`() = runTest {
        // Given
        val clienteId = 1L
        val cliente = Cliente(
            id = clienteId, 
            rotaId = 1L, 
            nome = "Bar do Zé", 
            endereco = "Rua Teste", 
            telefone = "123", 
            dataCadastro = Date(), 
            ativo = true,
            debitoAtual = 0.0,
            comissaoFicha = 0.5,
            valorFicha = 2.0
        )
        
        whenever(appRepository.obterClientePorId(clienteId)).thenReturn(cliente)

        // When
        viewModel.loadClientForSettlement(clienteId)
        advanceUntilIdle()

        // Then
        verify(appRepository).obterClientePorId(clienteId)
        assertThat(viewModel.clientName.value).isEqualTo("Bar do Zé")
    }

    @Test
    fun `buscarDebitoAnterior no modo NOVO ACERTO deve usar debito do ultimo acerto`() = runTest {
        // Arrange
        val clienteId = 1L
        val ultimoAcerto = Acerto(
            id = 10, clienteId = clienteId, debitoAtual = 50.0,
            rotaId = 1L, periodoInicio = Date(), periodoFim = Date()
        )
        whenever(appRepository.buscarUltimoAcertoPorCliente(clienteId)).thenReturn(ultimoAcerto)

        // Act
        viewModel.buscarDebitoAnterior(clienteId, null) // null = novo acerto
        advanceUntilIdle()

        // Assert
        assertThat(viewModel.debitoAnterior.value).isEqualTo(50.0)
        verify(appRepository).buscarUltimoAcertoPorCliente(clienteId)
    }

    @Test
    fun `buscarDebitoAnterior no modo EDIÇÃO deve usar debitoAnterior do proprio acerto se for o primeiro`() = runTest {
        // Arrange
        val clienteId = 1L
        val acertoParaEdicaoId = 20L
        val dataAgora = Date()
        
        val acertoEdicao = Acerto(
            id = acertoParaEdicaoId, 
            clienteId = clienteId, 
            dataAcerto = dataAgora,
            debitoAnterior = 30.0,
            rotaId = 1L, periodoInicio = Date(), periodoFim = Date()
        )
        
        // Simular que só existe este acerto
        whenever(appRepository.obterAcertosPorCliente(clienteId)).thenReturn(flowOf(listOf(acertoEdicao)))

        // Act
        viewModel.buscarDebitoAnterior(clienteId, acertoParaEdicaoId) // Modo edição
        advanceUntilIdle()

        // Assert
        assertThat(viewModel.debitoAnterior.value).isEqualTo(30.0)
    }

    @Test
    fun `buscarDebitoAnterior no modo EDIÇÃO deve buscar acerto imediatamente anterior se houver mais`() = runTest {
        // Arrange
        val clienteId = 1L
        val acertoParaEdicaoId = 20L
        // Datas: Acerto 1 (Antigo) -> Acerto 2 (Edição)
        val dataAntiga = Date(1672531200000) // 01/01/2023
        val dataEdicao = Date(1704067200000) // 01/01/2024
        
        val acertoAntigo = Acerto(
            id = 10, clienteId = clienteId, dataAcerto = dataAntiga, 
            debitoAtual = 15.0, // Este é o valor esperado
            rotaId = 1L, periodoInicio = Date(), periodoFim = Date()
        )
        val acertoEdicao = Acerto(
            id = acertoParaEdicaoId, clienteId = clienteId, dataAcerto = dataEdicao,
            debitoAnterior = 999.0, // Valor salvo que deve ser ignorado em favor do recalculado/buscado
            rotaId = 1L, periodoInicio = Date(), periodoFim = Date()
        )

        whenever(appRepository.obterAcertosPorCliente(clienteId)).thenReturn(flowOf(listOf(acertoEdicao, acertoAntigo)))

        // Act
        viewModel.buscarDebitoAnterior(clienteId, acertoParaEdicaoId)
        advanceUntilIdle()

        // Assert
        assertThat(viewModel.debitoAnterior.value).isEqualTo(15.0)
    }
}
