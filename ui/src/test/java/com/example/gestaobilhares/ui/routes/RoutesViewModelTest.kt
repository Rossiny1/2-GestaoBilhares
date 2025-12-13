package com.example.gestaobilhares.ui.routes

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.RotaResumo
import com.example.gestaobilhares.data.entities.StatusRota
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.ColaboradorRota
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
import app.cash.turbine.test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutesViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var appRepository: AppRepository

    @Mock
    private lateinit var userSessionManager: UserSessionManager

    private lateinit var viewModel: RoutesViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock android.util.Log
        try {
            val logMock = org.mockito.Mockito.mockStatic(android.util.Log::class.java)
            logMock.`when`<Int> { android.util.Log.d(any<String>(), any<String>()) }.thenReturn(0)
            logMock.`when`<Int> { android.util.Log.e(any<String>(), any<String>(), any()) }.thenReturn(0)
            logMock.`when`<Int> { android.util.Log.w(any<String>(), any<String>()) }.thenReturn(0)
            logMock.`when`<Int> { android.util.Log.i(any<String>(), any<String>()) }.thenReturn(0)
        } catch (e: Exception) {
            // Ignore if already mocked
        }

        // Default mocks to prevent initialization crashes
        whenever(appRepository.getRotasResumoComAtualizacaoTempoReal()).thenReturn(flowOf(emptyList()))
        
        whenever(userSessionManager.isAdmin()).thenReturn(true) // Default to admin
        whenever(userSessionManager.getCurrentUserId()).thenReturn(1L)
        whenever(userSessionManager.getCurrentUserName()).thenReturn("Admin")
        whenever(userSessionManager.getCurrentUserEmail()).thenReturn("admin@test.com")
        whenever(userSessionManager.getLoginTimestamp()).thenReturn(System.currentTimeMillis())

        viewModel = RoutesViewModel(appRepository, userSessionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should match repository data`() = runTest {
        // Arrange
        val rota = Rota(id = 1, nome = "Rota 1", cidades = "Cidade 1")
        val rotaResumo = RotaResumo(
            rota = rota,
            quantidadeMesas = 10,
            clientesAtivos = 5,
            pendencias = 0,
            valorAcertado = 1000.0,
            cicloAtual = 1,
            status = StatusRota.EM_ANDAMENTO
        )
        whenever(appRepository.getRotasResumoComAtualizacaoTempoReal()).thenReturn(flowOf(listOf(rotaResumo)))
        
        // Re-init ViewModel to trigger init block with new mock
        viewModel = RoutesViewModel(appRepository, userSessionManager)
        advanceUntilIdle()

        // Assert
        viewModel.rotasResumo.test {
            val rotas = awaitItem()
            assertThat(rotas).hasSize(1)
            assertThat(rotas[0].rota.nome).isEqualTo("Rota 1")
        }
    }
    
    @Test
    fun `should filter routes for non-admin user`() = runTest {
       // Arrange
        val rota1 = Rota(id = 1, nome = "Rota 1", cidades = "Cidades 1") // User has access
        val rota2 = Rota(id = 2, nome = "Rota 2", cidades = "Cidades 2") // User NO access
        
        val rotaResumo1 = RotaResumo(rota = rota1, quantidadeMesas = 5, clientesAtivos = 2, pendencias = 0, valorAcertado = 100.0, cicloAtual = 1, status = StatusRota.EM_ANDAMENTO)
        val rotaResumo2 = RotaResumo(rota = rota2, quantidadeMesas = 5, clientesAtivos = 2, pendencias = 0, valorAcertado = 100.0, cicloAtual = 1, status = StatusRota.EM_ANDAMENTO)
        
        whenever(appRepository.getRotasResumoComAtualizacaoTempoReal()).thenReturn(flowOf(listOf(rotaResumo1, rotaResumo2)))
        
        // Mock User as Non-Admin
        whenever(userSessionManager.isAdmin()).thenReturn(false)
        whenever(userSessionManager.getCurrentUserId()).thenReturn(2L)
        
        // Mock Access: User 2 has access to Rota 1 only
        val colaboradorRota = ColaboradorRota(colaboradorId = 2L, rotaId = 1L)
        whenever(appRepository.obterRotasPorColaborador(2L)).thenReturn(flowOf(listOf(colaboradorRota)))

        // Actions
        viewModel = RoutesViewModel(appRepository, userSessionManager)
        advanceUntilIdle()

        // Assert
        viewModel.rotasResumo.test {
            val rotas = awaitItem()
            assertThat(rotas).hasSize(1)
            assertThat(rotas[0].rota.id).isEqualTo(1L)
        }
    }
}
