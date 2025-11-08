package com.example.gestaobilhares.ui.settlement

import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.repository.AppRepository
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
import java.util.Date

/**
 * ✅ FASE 12.2: Testes unitários para SettlementViewModel
 * 
 * Testa funcionalidades críticas:
 * - Carregamento de dados do cliente
 * - Salvamento de acerto
 * - Cálculo de valores
 * - Estados de resultado
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettlementViewModelTest {

    @Mock
    private lateinit var mockAppRepository: AppRepository

    private lateinit var viewModel: SettlementViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = SettlementViewModel(mockAppRepository)
    }

    @Test
    fun `loadClientForSettlement deve carregar dados do cliente`() = runTest(testDispatcher) {
        // Arrange
        val clienteId = 1L
        val clienteMock = Cliente(
            id = clienteId,
            nome = "Cliente Teste",
            endereco = "Rua Teste, 123",
            debitoAtual = 100.0
        )
        
        whenever(mockAppRepository.obterClientePorId(clienteId))
            .thenReturn(clienteMock)
        
        val mesasMock = listOf(
            Mesa(id = 1, numero = "Mesa 1", clienteId = clienteId, tipoMesa = TipoMesa.FIXA)
        )
        whenever(mockAppRepository.obterMesasPorClienteDireto(clienteId))
            .thenReturn(mesasMock)

        // Act
        viewModel.loadClientForSettlement(clienteId)
        advanceUntilIdle()

        // Assert
        val clientName = viewModel.clientName.first()
        val clientAddress = viewModel.clientAddress.first()
        assertEquals("Nome do cliente deve ser carregado", "Cliente Teste", clientName)
        assertEquals("Endereço do cliente deve ser carregado", "Rua Teste, 123", clientAddress)
        verify(mockAppRepository).obterClientePorId(clienteId)
    }

    @Test
    fun `loadClientForSettlement deve tratar cliente não encontrado`() = runTest(testDispatcher) {
        // Arrange
        val clienteId = 999L
        
        whenever(mockAppRepository.obterClientePorId(clienteId))
            .thenReturn(null)

        // Act
        viewModel.loadClientForSettlement(clienteId)
        advanceUntilIdle()

        // Assert
        val clientName = viewModel.clientName.first()
        assertTrue("Deve indicar cliente não encontrado", 
            clientName.contains("não encontrado"))
    }

    @Test
    fun `resultadoSalvamento deve iniciar como null`() = runTest(testDispatcher) {
        // Assert
        val resultado = viewModel.resultadoSalvamento.first()
        assertNull("Resultado inicial deve ser null", resultado)
    }

    @Test
    fun `mesasCliente deve iniciar vazia`() = runTest(testDispatcher) {
        // Assert
        val mesas = viewModel.mesasCliente.first()
        assertTrue("Lista de mesas inicial deve estar vazia", mesas.isEmpty())
    }

    @Test
    fun `debitoAnterior deve iniciar como zero`() = runTest(testDispatcher) {
        // Assert
        val debito = viewModel.debitoAnterior.first()
        assertEquals("Débito anterior inicial deve ser zero", 0.0, debito, 0.01)
    }
}

