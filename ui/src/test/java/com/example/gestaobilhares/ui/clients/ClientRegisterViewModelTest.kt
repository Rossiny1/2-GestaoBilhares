package com.example.gestaobilhares.ui.clients

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.repository.AppRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class ClientRegisterViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var appRepository: AppRepository

    private lateinit var viewModel: ClientRegisterViewModel
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Mock android.util.Log to avoid RuntimeException: Stub!
        try {
            val logMock = org.mockito.Mockito.mockStatic(android.util.Log::class.java)
            logMock.`when`<Int> { android.util.Log.d(any<String>(), any<String>()) }.thenReturn(0)
            logMock.`when`<Int> { android.util.Log.e(any<String>(), any<String>(), any()) }.thenReturn(0)
            logMock.`when`<Int> { android.util.Log.w(any<String>(), any<String>()) }.thenReturn(0)
            logMock.`when`<Int> { android.util.Log.i(any<String>(), any<String>()) }.thenReturn(0)
        } catch (e: Exception) {
            // Static mock might already be active or not supported in this environment
        }

        viewModel = ClientRegisterViewModel(appRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // Mockito.framework().clearInlineMocks() // Optional cleanup
    }

    @Test
    fun `cadastrarCliente deve inserir novo cliente`() = runTest {
        // Arrange
        val cliente = Cliente(
            id = 0L,
            rotaId = 1L,
            nome = "Novo Cliente",
            endereco = "Rua Teste", 
            telefone = "123", 
            dataCadastro = Date(),
            ativo = true
        )
        whenever(appRepository.inserirCliente(any())).thenReturn(100L)

        // Act
        viewModel.cadastrarCliente(cliente)
        advanceUntilIdle()

        // Assert
        assertThat(viewModel.novoClienteId.value).isEqualTo(100L)
        verify(appRepository).inserirCliente(any())
    }

    @Test
    fun `carregarClienteParaEdicao deve carregar dados`() = runTest {
        // Arrange
        val clienteId = 1L
        val cliente = Cliente(
            id = clienteId,
            rotaId = 1L,
            nome = "Cliente Existente",
            endereco = "Rua Teste", 
            telefone = "123", 
            dataCadastro = Date(),
            ativo = true
        )
        whenever(appRepository.obterClientePorId(clienteId)).thenReturn(cliente)
        whenever(appRepository.obterDebitoAtual(clienteId)).thenReturn(50.0)

        // Act
        viewModel.carregarClienteParaEdicao(clienteId)
        advanceUntilIdle()

        // Assert
        val item = viewModel.clienteParaEdicao.value
        assertThat(item).isNotNull()
        assertThat(item?.nome).isEqualTo("Cliente Existente")
        assertThat(viewModel.debitoAtual.value).isEqualTo(50.0)
    }
}
