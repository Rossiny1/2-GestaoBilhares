package com.example.gestaobilhares.ui.settlement

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.gestaobilhares.data.repository.AppRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

/**
 * Testes unitários para SettlementViewModel
 * 
 * Testa as funcionalidades críticas:
 * - Cálculo de subtotal
 * - Validação de relógios
 * - Preparação de dados para acerto
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettlementViewModelTest {

    // Rule para executar LiveData synchronously
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockRepository: AppRepository

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var viewModel: SettlementViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        viewModel = SettlementViewModel(mockRepository)
    }

    @Test
    fun `calcularSubtotal deve retornar valor correto com relogios validos`() = runTest {
        // Arrange
        val relogioInicial = 1000
        val relogioFinal = 1100
        val valorFicha = 0.50
        val esperado = 50.0 // (1100 - 1000) * 0.50 = 50
        
        // Act
        viewModel.updateSubtotal(relogioInicial, relogioFinal, valorFicha, 0.0)
        
        // Assert
        val result = viewModel.subtotalAtual.first()
        assertThat(result).isWithin(0.01).of(esperado)
    }

    @Test
    fun `calcularSubtotal deve retornar zero quando relogioFinal menor que inicial`() = runTest {
        // Arrange
        val relogioInicial = 1100
        val relogioFinal = 1000
        val valorFicha = 0.50
        
        // Act
        viewModel.updateSubtotal(relogioInicial, relogioFinal, valorFicha, 0.0)
        
        // Assert
        val result = viewModel.subtotalAtual.first()
        assertThat(result).isEqualTo(0.0)
    }

    @Test
    fun `calcularSubtotal deve considerar valorFixo quando presente`() = runTest {
        // Arrange
        val relogioInicial = 1000
        val relogioFinal = 1100
        val valorFicha = 0.50
        val valorFixo = 100.0
        val esperado = 100.0 // valorFixo sobrescreve cálculo por fichas
        
        // Act
        viewModel.updateSubtotal(relogioInicial, relogioFinal, valorFicha, valorFixo)
        
        // Assert
        val result = viewModel.subtotalAtual.first()
        assertThat(result).isWithin(0.01).of(esperado)
    }

    @Test
    fun `validarRelogios deve retornar true quando relógios são válidos`() {
        // Arrange
        val relogioInicial = 1000
        val relogioFinal = 1100
        
        // Act
        val result = viewModel.validarRelogios(relogioInicial, relogioFinal)
        
        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `validarRelogios deve retornar false quando relogioFinal menor que inicial`() {
        // Arrange
        val relogioInicial = 1100
        val relogioFinal = 1000
        
        // Act
        val result = viewModel.validarRelogios(relogioInicial, relogioFinal)
        
        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `validarRelogios deve retornar false quando relogios iguais`() {
        // Arrange
        val relogioInicial = 1000
        val relogioFinal = 1000
        
        // Act
        val result = viewModel.validarRelogios(relogioInicial, relogioFinal)
        
        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `calcularTotalAcerto deve somar todos os subtotais das mesas`() = runTest {
        // Arrange
        val subtotais = listOf(50.0, 75.0, 100.0)
        val esperado = 225.0
        
        // Act
        val result = viewModel.calcularTotalAcerto(subtotais)
        
        // Assert
        assertThat(result).isWithin(0.01).of(esperado)
    }

    @Test
    fun `calcularTotalAcerto deve retornar zero para lista vazia`() = runTest {
        // Arrange
        val subtotais = emptyList<Double>()
        
        // Act
        val result = viewModel.calcularTotalAcerto(subtotais)
        
        // Assert
        assertThat(result).isEqualTo(0.0)
    }
}
