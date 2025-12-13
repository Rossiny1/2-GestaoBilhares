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

    // Add more tests as needed
}
