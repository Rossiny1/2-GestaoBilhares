package com.example.gestaobilhares.ui.routes

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.RotaResumo
import com.example.gestaobilhares.data.entities.StatusRota
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.TipoMeta
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
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import app.cash.turbine.test

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(org.robolectric.RobolectricTestRunner::class)
class RoutesViewModelTest {

    @Mock
    private lateinit var appRepository: AppRepository

    @Mock
    private lateinit var userSessionManager: UserSessionManager

    @Mock
    private lateinit var context: android.content.Context

    @Mock
    private lateinit var sharedPreferences: android.content.SharedPreferences

    @Mock
    private lateinit var sharedPreferencesEditor: android.content.SharedPreferences.Editor

    @Mock
    private lateinit var syncRepository: com.example.gestaobilhares.sync.SyncRepository

    @Mock
    private lateinit var networkUtils: com.example.gestaobilhares.sync.utils.NetworkUtils

    private lateinit var viewModel: RoutesViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock default behavior for userSessionManager
        whenever(userSessionManager.isAdmin()).thenReturn(true)
        whenever(userSessionManager.getCurrentUserId()).thenReturn(1L)
        whenever(userSessionManager.getCurrentUserName()).thenReturn("Admin")
        whenever(userSessionManager.getCurrentUserEmail()).thenReturn("admin@test.com")
        whenever(userSessionManager.getLoginTimestamp()).thenReturn(1000L)

        // Mock SharedPreferences
        whenever(context.getSharedPreferences(any(), any())).thenReturn(sharedPreferences)
        whenever(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putLong(any(), any())).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putBoolean(any(), any())).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.remove(any())).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferences.getLong(any(), any())).thenReturn(0L)

        // Mock NetworkUtils
        whenever(networkUtils.isConnected()).thenReturn(true)

        // Default mocks to prevent initialization crashes
        whenever(appRepository.getRotasResumoComAtualizacaoTempoReal()).thenReturn(flowOf(emptyList()))
        
        viewModel = RoutesViewModel(appRepository, userSessionManager, syncRepository, networkUtils)
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
        viewModel = RoutesViewModel(appRepository, userSessionManager, syncRepository, networkUtils)
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
        viewModel = RoutesViewModel(appRepository, userSessionManager, syncRepository, networkUtils)
        advanceUntilIdle()

        // Assert
        assertThat(viewModel.rotasResumo.value).hasSize(1)
        assertThat(viewModel.rotasResumo.value[0].rota.id).isEqualTo(1L)
    }

    @Test
    fun `checkSyncPendencies deve mostrar dialogo quando houver dados na nuvem`() = runTest {
        // Arrange
        val currentTimestamp = 2000L
        whenever(userSessionManager.getLoginTimestamp()).thenReturn(currentTimestamp)
        whenever(sharedPreferences.getLong(any(), org.mockito.kotlin.eq(0L))).thenReturn(0L) // Diálogo não mostrado
        
        // Mock appRepository.obterTodasRotas()
        whenever(appRepository.obterTodasRotas()).thenReturn(flowOf(emptyList()))
        whenever(appRepository.contarOperacoesSyncPendentes()).thenReturn(0)
        
        // Mock syncRepository.hasDataInCloud()
        whenever(syncRepository.hasDataInCloud()).thenReturn(true)
        whenever(syncRepository.getGlobalLastSyncTimestamp()).thenReturn(0L)

        // Act
        viewModel.checkSyncPendencies(context)
        advanceUntilIdle()

        // Assert
        val state = viewModel.syncDialogState.value
        assertThat(state).isNotNull()
        assertThat(state?.isCloudData).isTrue()
    }

    @Test
    fun `checkSyncPendencies deve mostrar dialogo quando houver pendencias locais`() = runTest {
        // Arrange
        val currentTimestamp = 2000L
        whenever(userSessionManager.getLoginTimestamp()).thenReturn(currentTimestamp)
        whenever(sharedPreferences.getLong(any(), org.mockito.kotlin.eq(0L))).thenReturn(0L)
        
        whenever(appRepository.obterTodasRotas()).thenReturn(flowOf(listOf(Rota(1, "R1"))))
        whenever(appRepository.contarOperacoesSyncPendentes()).thenReturn(5)
        whenever(syncRepository.getGlobalLastSyncTimestamp()).thenReturn(1000L)

        // Act
        viewModel.checkSyncPendencies(context)
        advanceUntilIdle()

        // Assert
        val state = viewModel.syncDialogState.value
        assertThat(state).isNotNull()
        assertThat(state?.hasLocalPending).isTrue()
        assertThat(state?.pendingCount).isEqualTo(5)
    }

    @Test
    fun `navigateToClients deve definir estado de navegacao`() {
        val rota = Rota(1, "R1")
        val rotaResumo = RotaResumo(
            rota = rota,
            clientesAtivos = 1,
            pendencias = 1,
            valorAcertado = 0.0,
            quantidadeMesas = 10,
            percentualAcertados = 100,
            status = StatusRota.EM_ANDAMENTO,
            cicloAtual = 1
        )
        
        viewModel.navigateToClients(rotaResumo)
        
        assertThat(viewModel.navigateToClients.value).isEqualTo(rota.id)
    }

    @Test
    fun `navigationToClientsCompleted deve limpar estado de navegacao`() {
        viewModel.navigationToClientsCompleted()
        
        assertThat(viewModel.navigateToClients.value).isNull()
    }

    @Test
    fun `formatarTextoMeta deve retornar texto formatado`() {
        val meta = MetaColaborador(
            id = 1L,
            colaboradorId = 1L,
            tipoMeta = TipoMeta.FATURAMENTO,
            valorMeta = 1000.0,
            cicloId = 1L,
            valorAtual = 500.0
        )
        
        val texto = viewModel.formatarTextoMeta(meta)
        
        assertThat(texto).contains("Faturamento:")
        assertThat(texto).contains("500/1000")
        assertThat(texto).contains("50.0%")
    }
}
