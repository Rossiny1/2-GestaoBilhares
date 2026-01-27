package com.example.gestaobilhares.ui.metas

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.example.gestaobilhares.data.repository.AppRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MetaCadastroViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var appRepository: AppRepository

    private lateinit var viewModel: MetaCadastroViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = MetaCadastroViewModel(appRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `criarCicloParaRota deve usar ano atual e resetar quando proximo numero for 1`() = runTest {
        // Arrange
        val rotaId = 1L
        val anoAtual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        
        // Mock: próximo número é 1 (indicando reset anual)
        whenever(appRepository.buscarProximoNumeroCiclo(rotaId, anoAtual)).thenReturn(1)
        whenever(appRepository.inserirCicloAcerto(any())).thenReturn(123L)
        whenever(appRepository.buscarCiclosPorRota(rotaId)).thenReturn(emptyList())

        // Act
        viewModel.criarCicloParaRota(rotaId)

        // Assert
        verify(appRepository).buscarProximoNumeroCiclo(rotaId, anoAtual)
        val cicloCaptor = argumentCaptor<CicloAcertoEntity>()
        verify(appRepository).inserirCicloAcerto(cicloCaptor.capture())
        val cicloSalvo = cicloCaptor.firstValue
        assertThat(cicloSalvo.rotaId).isEqualTo(rotaId)
        assertThat(cicloSalvo.numeroCiclo).isEqualTo(1)
        assertThat(cicloSalvo.ano).isEqualTo(anoAtual)
        assertThat(cicloSalvo.status).isEqualTo(StatusCicloAcerto.EM_ANDAMENTO)
        
        // Verificar se ciclo foi marcado como criado
        assertThat(viewModel.cicloCriado.value).isTrue()
    }

    @Test
    fun `criarCicloParaRota deve continuar sequencia quando proximo numero nao for 1`() = runTest {
        // Arrange
        val rotaId = 1L
        val anoAtual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val proximoNumero = 5
        
        // Mock: próximo número é 5 (continuando sequência)
        whenever(appRepository.buscarProximoNumeroCiclo(rotaId, anoAtual)).thenReturn(proximoNumero)
        whenever(appRepository.inserirCicloAcerto(any())).thenReturn(123L)
        whenever(appRepository.buscarCiclosPorRota(rotaId)).thenReturn(emptyList())

        // Act
        viewModel.criarCicloParaRota(rotaId)

        // Assert
        val cicloCaptor = argumentCaptor<CicloAcertoEntity>()
        verify(appRepository).inserirCicloAcerto(cicloCaptor.capture())
        val cicloSalvo = cicloCaptor.firstValue
        assertThat(cicloSalvo.rotaId).isEqualTo(rotaId)
        assertThat(cicloSalvo.numeroCiclo).isEqualTo(proximoNumero)
        assertThat(cicloSalvo.ano).isEqualTo(anoAtual)
        assertThat(cicloSalvo.status).isEqualTo(StatusCicloAcerto.EM_ANDAMENTO)
    }

    @Test
    fun `criarCicloFuturoParaRota deve usar ano atual e status PLANEJADO`() = runTest {
        // Arrange
        val rotaId = 1L
        val anoAtual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        
        // Mock: próximo número é 1 (reset anual)
        whenever(appRepository.buscarProximoNumeroCiclo(rotaId, anoAtual)).thenReturn(1)
        whenever(appRepository.inserirCicloAcerto(any())).thenReturn(123L)
        whenever(appRepository.buscarCiclosPorRota(rotaId)).thenReturn(emptyList())

        // Act
        viewModel.criarCicloFuturoParaRota(rotaId)

        // Assert
        val cicloCaptor = argumentCaptor<CicloAcertoEntity>()
        verify(appRepository).inserirCicloAcerto(cicloCaptor.capture())
        val cicloSalvo = cicloCaptor.firstValue
        assertThat(cicloSalvo.rotaId).isEqualTo(rotaId)
        assertThat(cicloSalvo.numeroCiclo).isEqualTo(1)
        assertThat(cicloSalvo.ano).isEqualTo(anoAtual)
        assertThat(cicloSalvo.status).isEqualTo(StatusCicloAcerto.PLANEJADO)
    }

    @Test
    fun `criarCicloParaRota deve tratar erro gracefully`() = runTest {
        // Arrange
        val rotaId = 1L
        val anoAtual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val errorMessage = "Database error"
        
        // Mock: erro ao buscar próximo número
        whenever(appRepository.buscarProximoNumeroCiclo(rotaId, anoAtual))
            .thenThrow(RuntimeException(errorMessage))

        // Act
        viewModel.criarCicloParaRota(rotaId)

        // Assert
        verify(appRepository, never()).inserirCicloAcerto(any())
        
        // Verificar mensagem de erro
        assertThat(viewModel.message.value).isEqualTo("Erro ao criar ciclo: $errorMessage")
        
        // Verificar que ciclo não foi marcado como criado
        assertThat(viewModel.cicloCriado.value).isFalse()
    }

    @Test
    fun `resetarCicloCriado deve limpar estado`() = runTest {
        // Arrange: primeiro criar um ciclo para setar o estado
        val rotaId = 1L
        val anoAtual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        
        whenever(appRepository.buscarProximoNumeroCiclo(rotaId, anoAtual)).thenReturn(1)
        whenever(appRepository.inserirCicloAcerto(any())).thenReturn(123L)
        whenever(appRepository.buscarCiclosPorRota(rotaId)).thenReturn(emptyList())
        
        viewModel.criarCicloParaRota(rotaId)
        
        // Verificar que foi criado
        assertThat(viewModel.cicloCriado.value).isTrue()

        // Act
        viewModel.resetarCicloCriado()

        // Assert
        assertThat(viewModel.cicloCriado.value).isFalse()
    }
}
