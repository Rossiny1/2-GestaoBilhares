package com.example.gestaobilhares.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.StatusRota
import com.example.gestaobilhares.data.dao.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * ✅ FASE 12.2: Testes de integração para AppRepository
 * 
 * Testa operações CRUD reais com banco de dados:
 * - Inserção de entidades
 * - Busca de entidades
 * - Atualização de entidades
 * - Deleção de entidades
 * - Fluxos reativos (Flow)
 */
@RunWith(AndroidJUnit4::class)
class AppRepositoryIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setup() {
        // Limpar singleton do RepositoryFactory antes de cada teste
        com.example.gestaobilhares.data.factory.RepositoryFactory.clearRepository()
        
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        // ✅ FASE 12.2: Criar AppRepository diretamente com banco in-memory para testes
        val context = ApplicationProvider.getApplicationContext()
        repository = AppRepository(
            clienteDao = database.clienteDao(),
            acertoDao = database.acertoDao(),
            mesaDao = database.mesaDao(),
            syncLogDao = database.syncLogDao(),
            syncQueueDao = database.syncQueueDao(),
            syncConfigDao = database.syncConfigDao(),
            context = context,
            rotaDao = database.rotaDao(),
            despesaDao = database.despesaDao(),
            colaboradorDao = database.colaboradorDao(),
            cicloAcertoDao = database.cicloAcertoDao(),
            acertoMesaDao = database.acertoMesaDao(),
            contratoLocacaoDao = database.contratoLocacaoDao(),
            aditivoContratoDao = database.aditivoContratoDao(),
            assinaturaRepresentanteLegalDao = database.assinaturaRepresentanteLegalDao(),
            logAuditoriaAssinaturaDao = database.logAuditoriaAssinaturaDao(),
            panoEstoqueDao = database.panoEstoqueDao(),
            panoMesaDao = database.panoMesaDao(),
            mesaVendidaDao = database.mesaVendidaDao(),
            stockItemDao = database.stockItemDao(),
            equipmentDao = database.equipmentDao(),
            veiculoDao = database.veiculoDao(),
            categoriaDespesaDao = database.categoriaDespesaDao(),
            tipoDespesaDao = database.tipoDespesaDao(),
            historicoManutencaoVeiculoDao = database.historicoManutencaoVeiculoDao(),
            historicoCombustivelVeiculoDao = database.historicoCombustivelVeiculoDao(),
            historicoManutencaoMesaDao = database.historicoManutencaoMesaDao(),
            mesaReformadaDao = database.mesaReformadaDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
        com.example.gestaobilhares.data.factory.RepositoryFactory.clearRepository()
    }

    @Test
    fun `inserirCliente deve salvar cliente no banco`() = runTest {
        // Arrange
        val cliente = Cliente(
            nome = "Cliente Teste",
            endereco = "Rua Teste, 123",
            debitoAtual = 100.0,
            ativo = true
        )

        // Act
        val id = repository.inserirCliente(cliente)

        // Assert
        assertTrue("ID deve ser maior que zero", id > 0)
        val clienteSalvo = repository.obterClientePorId(id)
        assertNotNull("Cliente deve ser encontrado", clienteSalvo)
        assertEquals("Nome deve ser igual", "Cliente Teste", clienteSalvo?.nome)
    }

    @Test
    fun `obterClientesPorRota deve retornar Flow com clientes`() = runTest {
        // Arrange
        val rota = Rota(
            nome = "Rota Teste",
            statusAtual = StatusRota.EM_ANDAMENTO,
            ativa = true
        )
        val rotaId = repository.inserirRota(rota)
        
        val cliente1 = Cliente(
            nome = "Cliente 1",
            rotaId = rotaId,
            debitoAtual = 100.0,
            ativo = true
        )
        val cliente2 = Cliente(
            nome = "Cliente 2",
            rotaId = rotaId,
            debitoAtual = 200.0,
            ativo = true
        )
        repository.inserirCliente(cliente1)
        repository.inserirCliente(cliente2)

        // Act
        val clientes = repository.obterClientesPorRota(rotaId).first()

        // Assert
        assertEquals("Deve retornar 2 clientes", 2, clientes.size)
        assertTrue("Deve conter Cliente 1", clientes.any { it.nome == "Cliente 1" })
        assertTrue("Deve conter Cliente 2", clientes.any { it.nome == "Cliente 2" })
    }

    @Test
    fun `atualizarCliente deve atualizar dados do cliente`() = runTest {
        // Arrange
        val cliente = Cliente(
            nome = "Cliente Original",
            endereco = "Endereço Original",
            debitoAtual = 100.0,
            ativo = true
        )
        val id = repository.inserirCliente(cliente)

        // Act
        val clienteAtualizado = cliente.copy(
            id = id,
            nome = "Cliente Atualizado",
            endereco = "Endereço Atualizado"
        )
        repository.atualizarCliente(clienteAtualizado)

        // Assert
        val clienteSalvo = repository.obterClientePorId(id)
        assertNotNull("Cliente deve ser encontrado", clienteSalvo)
        assertEquals("Nome deve ser atualizado", "Cliente Atualizado", clienteSalvo?.nome)
        assertEquals("Endereço deve ser atualizado", "Endereço Atualizado", clienteSalvo?.endereco)
    }

    @Test
    fun `deletarCliente deve remover cliente do banco`() = runTest {
        // Arrange
        val cliente = Cliente(
            nome = "Cliente para Deletar",
            debitoAtual = 100.0,
            ativo = true
        )
        val id = repository.inserirCliente(cliente)

        // Act
        val clienteParaDeletar = cliente.copy(id = id)
        repository.deletarCliente(clienteParaDeletar)

        // Assert
        val clienteDeletado = repository.obterClientePorId(id)
        assertNull("Cliente deve ser deletado", clienteDeletado)
    }

    @Test
    fun `inserirRota deve salvar rota no banco`() = runTest {
        // Arrange
        val rota = Rota(
            nome = "Rota Teste",
            statusAtual = StatusRota.EM_ANDAMENTO,
            ativa = true
        )

        // Act
        val id = repository.inserirRota(rota)

        // Assert
        assertTrue("ID deve ser maior que zero", id > 0)
        val rotaSalva = repository.obterRotaPorId(id)
        assertNotNull("Rota deve ser encontrada", rotaSalva)
        assertEquals("Nome deve ser igual", "Rota Teste", rotaSalva?.nome)
    }
}

