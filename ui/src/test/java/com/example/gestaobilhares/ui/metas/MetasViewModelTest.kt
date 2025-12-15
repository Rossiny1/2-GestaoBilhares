package com.example.gestaobilhares.ui.metas

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.example.gestaobilhares.data.entities.TipoMeta
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
import org.mockito.kotlin.whenever
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class MetasViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var appRepository: AppRepository

    private lateinit var viewModel: MetasViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // CORRECT INSTANTIATION for Hilt @Inject constructor
        viewModel = MetasViewModel(appRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `carregarMetasRotas deve popular metasRotas com sucesso`() = runTest {
        // Arrange
        val rota = Rota(id = 1L, nome = "Rota 1", ativa = true)
        val ciclo = CicloAcertoEntity(
            id = 1L, rotaId = 1L, numeroCiclo = 1, ano = 2025,
            status = StatusCicloAcerto.EM_ANDAMENTO,
            dataInicio = Date(), dataFim = Date(),
            criadoPor = "Tester"
        )
        val meta = MetaColaborador(
            id = 1L, colaboradorId = 1L, tipoMeta = TipoMeta.FATURAMENTO,
            valorMeta = 1000.0, valorAtual = 500.0,
            rotaId = 1L, cicloId = 1L
        )

        // Mocks para o fluxo complexo
        whenever(appRepository.obterTodasRotas()).thenReturn(flowOf(listOf(rota)))
        whenever(appRepository.buscarCicloAtivo(rota.id)).thenReturn(ciclo)
        // whenever(appRepository.buscarColaboradorResponsavelPrincipal(rota.id)).thenReturn(null) // Deprecated or changed method name? 
        // Assuming we need to mock whatever is calling inside
        
        // Mock do buscarMetasPorRotaECicloAtivo
        whenever(appRepository.buscarMetasPorRotaECicloAtivo(rota.id, ciclo.id)).thenReturn(listOf(meta))
        
        // Mocks adicionais para calculo de progresso (se necessarios)
        whenever(appRepository.buscarAcertosPorRotaECiclo(rota.id, ciclo.id)).thenReturn(emptyList())

        // Act
        viewModel.carregarMetasRotas()

        // Assert
        viewModel.metasRotas.test {
            val list = awaitItem()
            assertThat(list).isNotEmpty()
            assertThat(list[0].rota.nome).isEqualTo("Rota 1")
            assertThat(list[0].metas).isNotEmpty()
            assertThat(list[0].metas[0].valorMeta).isEqualTo(1000.0)
        }
    }
}
