package com.example.gestaobilhares.ui.cycles

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.example.gestaobilhares.data.repository.AppRepository
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
import org.mockito.kotlin.whenever
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class CycleManagementViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var appRepository: AppRepository

    private lateinit var viewModel: CycleManagementViewModel
    
    // Dispatcher for coroutines
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = CycleManagementViewModel(appRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `carregarDadosCiclo deve atualizar dadosCiclo`() = runTest {
        // Arrange
        val cicloId = 1L
        val rotaId = 1L
        val dataAgora = Date()
        
        val ciclo = CicloAcertoEntity(
            id = cicloId,
            rotaId = rotaId,
            numeroCiclo = 1,
            ano = 2025,
            dataInicio = dataAgora,
            dataFim = dataAgora,
            status = StatusCicloAcerto.EM_ANDAMENTO,
            totalClientes = 10,
            observacoes = "Teste",
            criadoPor = "Tester"
        )
        
        val rota = Rota(
            id = rotaId,
            nome = "Rota Teste",
            cidades = "Regiao Teste"
        )

        // Mock Flows
        whenever(appRepository.buscarDespesasPorCicloId(cicloId)).thenReturn(flowOf(emptyList()))
        whenever(appRepository.buscarAcertosPorCicloId(cicloId)).thenReturn(flowOf(emptyList()))
        
        // Mock Suspend functions
        whenever(appRepository.buscarCicloPorId(cicloId)).thenReturn(ciclo)
        whenever(appRepository.buscarRotaPorId(rotaId)).thenReturn(rota)

        // Act
        viewModel.carregarDadosCiclo(cicloId, rotaId)
        advanceUntilIdle()

        // Assert
        viewModel.dadosCiclo.test {
            val dados = awaitItem()
            assertThat(dados).isNotNull()
            assertThat(dados?.titulo).contains("Rota Teste")
        }
    }
    @Test
    fun `estatisticas devem ser calculadas corretamente ao carregar dados`() = runTest {
        // Arrange
        val cicloId = 1L
        val rotaId = 1L
        val dataAgora = Date()
        
        val ciclo = CicloAcertoEntity(
            id = cicloId, rotaId = rotaId, numeroCiclo = 1, ano = 2025,
            dataInicio = dataAgora, dataFim = dataAgora, status = StatusCicloAcerto.EM_ANDAMENTO,
            totalClientes = 10, observacoes = "Teste", criadoPor = "Tester"
        )
        val rota = Rota(id = rotaId, nome = "Rota Teste", cidades = "Regiao Teste")
        
        // Simular 2 acertos de 100 reais cada (Total 200) - PIX e Dinheiro
        // Acerto 1: 100 reais PIX
        val acerto1 = Acerto(id = 1, valorRecebido = 100.0, metodosPagamentoJson = "{\"PIX\": 100.0}", rotaId = rotaId, clienteId = 1L, periodoInicio = Date(), periodoFim = Date())
        // Acerto 2: 100 reais DINHEIRO
        val acerto2 = Acerto(id = 2, valorRecebido = 100.0, metodosPagamentoJson = "{\"DINHEIRO\": 100.0}", rotaId = rotaId, clienteId = 2L, periodoInicio = Date(), periodoFim = Date())
        
        whenever(appRepository.buscarAcertosPorCicloId(cicloId)).thenReturn(flowOf(listOf(acerto1, acerto2)))
        whenever(appRepository.buscarDespesasPorCicloId(cicloId)).thenReturn(flowOf(emptyList()))
        whenever(appRepository.buscarCicloPorId(cicloId)).thenReturn(ciclo)
        whenever(appRepository.buscarRotaPorId(rotaId)).thenReturn(rota)

        // Act
        viewModel.carregarDadosCiclo(cicloId, rotaId)
        advanceUntilIdle()

        // Assert Stat Flow
        viewModel.estatisticas.test {
            val stats = awaitItem()
            // FinancialCalculator pode ter lógica complexa, mas esperamos que some os recebidos
            // Se o FinancialCalculator não for mockado, ele roda a lógica real.
            // Assumimos que ele está funcionando (Testes unitários dele cobrem a lógica matemática).
            // Apenas verificamos se o ViewModel repassou os valores.
            
            // Verifique se o totalRecebido é > 0, indicando que o cálculo ocorreu
            assertThat(stats.totalRecebido).isEqualTo(200.0) 
            assertThat(stats.somaPix).isEqualTo(100.0)
            // Dinheiro might be in 'dinheiro' or 'subtotal' depending on logic
        }
    }
}
