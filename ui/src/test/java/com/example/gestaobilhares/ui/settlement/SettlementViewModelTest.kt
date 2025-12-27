package com.example.gestaobilhares.ui.settlement

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.entities.*
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
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
        
        timber.log.Timber.plant(object : timber.log.Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                println("[$tag] $message")
                // t?.printStackTrace() // Removido para manter padrão de produção mesmo em testes
            }
        })
        
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
    fun `salvarAcerto deve inserir acerto e acerto_mesas com sucesso`() = runTest {
        // Arrange
        val clienteId = 1L
        val rotaId = 10L
        val cicloId = 100L
        
        val cliente = Cliente(id = clienteId, rotaId = rotaId, nome = "Tião", comissaoFicha = 0.5, valorFicha = 2.0, dataCadastro = Date(), ativo = true, debitoAtual = 0.0)
        val cicloAtivo = CicloAcertoEntity(id = cicloId, rotaId = rotaId, numeroCiclo = 1, ano = 2025, status = StatusCicloAcerto.EM_ANDAMENTO, dataInicio = Date(), dataFim = Date())
        
        whenever(appRepository.obterClientePorId(clienteId)).thenReturn(cliente)
        whenever(appRepository.buscarCicloAtivo(rotaId)).thenReturn(cicloAtivo)
        whenever(appRepository.buscarAcertosPorClienteECicloId(clienteId, cicloId)).thenReturn(flowOf(emptyList()))
        whenever(appRepository.obterAcertoPorId(any())).thenAnswer { invocation ->
             val id = invocation.arguments[0] as Long
             Acerto(id = id, clienteId = clienteId, rotaId = rotaId, cicloId = cicloId, valorRecebido = 100.0, periodoInicio = Date(), periodoFim = Date())
        }
        whenever(appRepository.buscarAcertosPorRotaECicloId(rotaId, cicloId)).thenReturn(flowOf(emptyList()))
        whenever(appRepository.buscarDespesasPorCicloId(cicloId)).thenReturn(flowOf(emptyList()))
        whenever(appRepository.inserirAcerto(any())).thenReturn(500L)
        whenever(appRepository.obterAcertoPorId(500L)).thenReturn(Acerto(id = 500L, clienteId = clienteId, rotaId = rotaId, cicloId = cicloId, valorRecebido = 100.0, periodoInicio = Date(), periodoFim = Date()))
        whenever(appRepository.inserirAcertoMesa(any())).thenReturn(1000L)
        whenever(appRepository.atualizarDebitoAtualCliente(any(), any())).thenReturn(Unit)
        whenever(appRepository.atualizarValoresCiclo(any())).thenReturn(Unit)
        whenever(appRepository.inserirHistoricoManutencaoMesa(any())).thenReturn(2000L)
        
        val dadosAcerto = SettlementViewModel.DadosAcerto(
            mesas = listOf(
                SettlementViewModel.MesaAcerto(id = 1, numero = "M1", relogioInicial = 100, relogioFinal = 200, tipoMesa = TipoMesa.SINUCA)
            ),
            representante = "Rossiny",
            panoTrocado = false,
            numeroPano = null,
            tipoAcerto = "NORMAL",
            observacao = "Teste",
            justificativa = null,
            metodosPagamento = mapOf("DINHEIRO" to 100.0)
        )

        // Act
        viewModel.definirDebitoAnteriorParaEdicao(0.0)
        viewModel.salvarAcerto(clienteId, dadosAcerto, dadosAcerto.metodosPagamento)
        advanceUntilIdle()

        // Assert
        val resultado = viewModel.resultadoSalvamento.value
        if (resultado is SettlementViewModel.ResultadoSalvamento.Erro) {
            println("❌ Erro no salvamento: ${resultado.mensagem}")
        }
        
        verify(appRepository).inserirAcerto(any())
        verify(appRepository, atLeastOnce()).inserirAcertoMesa(any())
        assertThat(resultado).isInstanceOf(SettlementViewModel.ResultadoSalvamento.Sucesso::class.java)
        assertThat((resultado as SettlementViewModel.ResultadoSalvamento.Sucesso).acertoId).isEqualTo(500L)
    }

    @Test
    fun `salvarAcerto deve falhar se nao houver ciclo ativo`() = runTest {
        // Arrange
        val clienteId = 1L
        val rotaId = 10L
        val cliente = Cliente(id = clienteId, rotaId = rotaId, nome = "Tião", dataCadastro = Date(), ativo = true)
        
        whenever(appRepository.obterClientePorId(clienteId)).thenReturn(cliente)
        whenever(appRepository.buscarCicloAtivo(rotaId)).thenReturn(null)
        
        val dadosAcerto = SettlementViewModel.DadosAcerto(
            mesas = emptyList(), representante = "R", panoTrocado = false, numeroPano = null, tipoAcerto = "N", observacao = "", justificativa = null, metodosPagamento = emptyMap()
        )

        // Act
        viewModel.salvarAcerto(clienteId, dadosAcerto, emptyMap())
        advanceUntilIdle()

        // Assert
        assertThat(viewModel.resultadoSalvamento.value).isInstanceOf(SettlementViewModel.ResultadoSalvamento.Erro::class.java)
        val erro = viewModel.resultadoSalvamento.value as SettlementViewModel.ResultadoSalvamento.Erro
        assertThat(erro.mensagem).contains("Não há ciclo em andamento")
    }

    @Test
    fun `salvarAcerto em modo EDICAO deve atualizar acerto existente`() = runTest {
        // Arrange
        val acertoId = 500L
        val clienteId = 1L
        val rotaId = 10L
        val cicloId = 100L
        
        val cliente = Cliente(id = clienteId, rotaId = rotaId, nome = "Tião", dataCadastro = Date(), ativo = true)
        val cicloAtivo = CicloAcertoEntity(id = cicloId, rotaId = rotaId, numeroCiclo = 1, ano = 2025, status = StatusCicloAcerto.EM_ANDAMENTO, dataInicio = Date(), dataFim = Date())
        val acertoExistente = Acerto(id = acertoId, clienteId = clienteId, status = StatusAcerto.PENDENTE, rotaId = rotaId, cicloId = cicloId, periodoInicio = Date(), periodoFim = Date())
        
        whenever(appRepository.obterClientePorId(clienteId)).thenReturn(cliente)
        whenever(appRepository.buscarCicloAtivo(rotaId)).thenReturn(cicloAtivo)
        whenever(appRepository.buscarAcertosPorClienteECicloId(clienteId, cicloId)).thenReturn(flowOf(listOf(acertoExistente)))
        whenever(appRepository.obterAcertoPorId(acertoId)).thenReturn(acertoExistente)
        whenever(appRepository.atualizarAcerto(any())).thenReturn(1)
        whenever(appRepository.atualizarDebitoAtualCliente(any(), any())).thenReturn(Unit)
        whenever(appRepository.atualizarValoresCiclo(any())).thenReturn(Unit)
        whenever(appRepository.buscarAcertosPorRotaECicloId(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(appRepository.buscarDespesasPorCicloId(any())).thenReturn(flowOf(emptyList()))

        val dadosAcerto = SettlementViewModel.DadosAcerto(
            mesas = emptyList(), representante = "R", panoTrocado = false, numeroPano = null, tipoAcerto = "N", observacao = "", justificativa = null, metodosPagamento = emptyMap()
        )

        // Act
        viewModel.salvarAcerto(clienteId, dadosAcerto, emptyMap(), acertoIdParaEdicao = acertoId)
        advanceUntilIdle()

        // Assert
        verify(appRepository).atualizarAcerto(any())
        assertThat(viewModel.resultadoSalvamento.value).isInstanceOf(SettlementViewModel.ResultadoSalvamento.Sucesso::class.java)
    }

    @Test
    fun `salvarAcerto deve registrar troca de pano quando panoTrocado for true`() = runTest {
        // Arrange
        val clienteId = 1L
        val rotaId = 10L
        val cicloId = 100L
        
        val cliente = Cliente(id = clienteId, rotaId = rotaId, nome = "Tião", comissaoFicha = 0.5, valorFicha = 2.0, dataCadastro = Date(), ativo = true)
        val cicloAtivo = CicloAcertoEntity(id = cicloId, rotaId = rotaId, numeroCiclo = 1, ano = 2025, status = StatusCicloAcerto.EM_ANDAMENTO, dataInicio = Date(), dataFim = Date())
        
        whenever(appRepository.obterClientePorId(clienteId)).thenReturn(cliente)
        whenever(appRepository.buscarCicloAtivo(rotaId)).thenReturn(cicloAtivo)
        whenever(appRepository.buscarAcertosPorClienteECicloId(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(appRepository.buscarAcertosPorRotaECicloId(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(appRepository.buscarDespesasPorCicloId(any())).thenReturn(flowOf(emptyList()))
        whenever(appRepository.inserirAcerto(any())).thenReturn(500L)
        whenever(appRepository.obterAcertoPorId(any())).thenReturn(Acerto(id = 500L, clienteId = clienteId, periodoInicio = Date(), periodoFim = Date()))
        whenever(appRepository.inserirAcertoMesa(any())).thenReturn(1000L)
        whenever(appRepository.atualizarDebitoAtualCliente(any(), any())).thenReturn(Unit)
        whenever(appRepository.atualizarValoresCiclo(any())).thenReturn(Unit)
        whenever(appRepository.inserirHistoricoManutencaoMesa(any())).thenReturn(2000L)
        
        val dadosAcerto = SettlementViewModel.DadosAcerto(
            mesas = listOf(
                SettlementViewModel.MesaAcerto(id = 1, numero = "M1", relogioInicial = 100, relogioFinal = 200, tipoMesa = TipoMesa.SINUCA)
            ),
            representante = "R",
            panoTrocado = true,
            numeroPano = "P123",
            tipoAcerto = "N",
            observacao = "",
            justificativa = null,
            metodosPagamento = emptyMap()
        )

        // Act
        viewModel.salvarAcerto(clienteId, dadosAcerto, emptyMap())
        advanceUntilIdle()

        // Assert
        verify(appRepository).inserirAcerto(any()) // Should pass
        verify(appRepository, atLeastOnce()).inserirHistoricoManutencaoMesa(any())
    }

    @Test
    fun `prepararMesasParaAcerto deve usar relogio final do ultimo acerto como inicial`() = runTest {
        // Arrange
        val mesaId = 1L
        val mesa = Mesa(id = mesaId, clienteId = 1L, numero = "01", relogioInicial = 0, tipoMesa = TipoMesa.SINUCA, dataInstalacao = Date(), ativa = true)
        val ultimoAcertoMesa = com.example.gestaobilhares.data.entities.AcertoMesa(
            id = 10, acertoId = 100, mesaId = mesaId, relogioInicial = 50, relogioFinal = 150, subtotal = 0.0, fichasJogadas = 100
        )
        
        whenever(appRepository.buscarUltimoAcertoMesaItem(mesaId)).thenReturn(ultimoAcertoMesa)

        // Act
        val result = viewModel.prepararMesasParaAcerto(listOf(mesa))
        
        // Assert
        assertThat(result[0].relogioInicial).isEqualTo(150)
    }
}
