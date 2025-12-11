package com.example.gestaobilhares.ui.expenses

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.gestaobilhares.data.entities.Despesa
import com.example.gestaobilhares.data.repository.AppRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalExpensesViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var appRepository: AppRepository

    private lateinit var viewModel: GlobalExpensesViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        
        // Setup default mocks needed for init block
        runTest {
            whenever(appRepository.buscarDespesasGlobaisPorCiclo(any(), any())).thenReturn(emptyList())
        }

        viewModel = GlobalExpensesViewModel(appRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadAllGlobalExpenses deve carregar despesas e atualizar estado`() = runTest {
        // Arrange
        val despesa = Despesa(
            id = 1L,
            rotaId = 0L,
            descricao = "Teste Despesa",
            valor = 100.0,
            categoria = "Categoria Teste",
            cicloId = 1L
        )
        // Mock especifico para este teste - valores literais para evitar ambiguidade
        whenever(appRepository.buscarDespesasGlobaisPorCiclo(2025, 1)).thenReturn(listOf(despesa))
        
        // Configurar o ano e ciclo para coincidir com o mock
        viewModel.setSelectedYear(2025)
        // O loadAllGlobalExpenses chama o repo com o ano selecionado e itera ciclos 1..12

        // Act
        viewModel.loadAllGlobalExpenses()

        // Assert
        viewModel.globalExpenses.test {
            val items = awaitItem()
            assertThat(items).isNotEmpty()
            assertThat(items.first().valor).isEqualTo(100.0)
            assertThat(items.first().descricao).isEqualTo("Teste Despesa")
        }

        viewModel.totalExpenses.test {
            val total = awaitItem()
            assertThat(total).isEqualTo(100.0)
        }
    }

    @Test
    fun `deleteGlobalExpense deve chamar repository e recarregar dados`() = runTest {
        // Arrange
        val despesa = Despesa(
            id = 1L, 
            rotaId = 0L,
            descricao = "Para Deletar", 
            valor = 50.0, 
            categoria = "Cat",
            dataHora = LocalDateTime.now()
        )
        whenever(appRepository.buscarDespesasGlobaisPorCiclo(any(), any())).thenReturn(emptyList())
        
        // Act
        viewModel.deleteGlobalExpense(despesa)

        // Assert
        // Verifica se o delete foi chamado
        verify(appRepository).deletarDespesa(despesa)
    }

    @Test
    fun `loadAvailableCycles deve popular lista com 12 ciclos`() = runTest {
        // O init já chama loadAvailableCycles
        
        viewModel.availableCycles.test {
            val cycles = awaitItem()
            assertThat(cycles).hasSize(12)
            assertThat(cycles.first().numeroCiclo).isEqualTo(1)
            assertThat(cycles.last().numeroCiclo).isEqualTo(12)
        }
    }
}
