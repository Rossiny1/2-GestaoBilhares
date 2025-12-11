package com.example.gestaobilhares.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Testes instrumentados para AppRepository
 * 
 * Usa banco de dados em memória para testar operações reais
 * sem afetar dados de produção.
 */
@RunWith(AndroidJUnit4::class)
class AppRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setup() {
        // Criar database em memória (não persiste)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        repository = AppRepository.create(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun inserirCliente_deveRetornarIdValido() = runTest {
        // Arrange
        val rota = Rota(
            nome = "Rota Teste",
            codigo = "RT001",
            descricao = "Teste",
            ativo = true,
            dataCriacao = Date(),
            dataUltimaAtualizacao = Date()
        )
        val rotaId = repository.inserirRota(rota)
        
        val cliente = Cliente(
            nome = "Cliente Teste",
            cpfCnpj = "12345678900",
            rotaId = rotaId,
            valorFicha = 0.50,
            comissaoFicha = 0.10,
            dataCadastro = Date(),
            dataUltimaAtualizacao = Date()
        )
        
        // Act
        val clienteId = repository.inserirCliente(cliente)
        
        // Assert
        assertThat(clienteId).isGreaterThan(0)
    }

    @Test
    fun buscarClientesPorRota_deveRetornarApenasClientesDaRota() = runTest {
        // Arrange
        val rota1 = Rota(
            nome = "Rota 1",
            codigo = " RT001",
            descricao = "Teste 1",
            ativo = true,
            dataCriacao = Date(),
            dataUltimaAtualizacao = Date()
        )
        val rota2 = Rota(
            nome = "Rota 2",
            codigo = "RT002",
            descricao = "Teste 2",
            ativo = true,
            dataCriacao = Date(),
            dataUltimaAtualizacao = Date()
        )
        
        val rotaId1 = repository.inserirRota(rota1)
        val rotaId2 = repository.inserirRota(rota2)
        
        val cliente1 = Cliente(
            nome = "Cliente Rota 1",
            cpfCnpj = "11111111111",
            rotaId = rotaId1,
            valorFicha = 0.50,
            comissaoFicha = 0.10,
            dataCadastro = Date(),
            dataUltimaAtualizacao = Date()
        )
        
        val cliente2 = Cliente(
            nome = "Cliente Rota 2",
            cpfCnpj = "22222222222",
            rotaId = rotaId2,
            valorFicha = 0.50,
            comissaoFicha = 0.10,
            dataCadastro = Date(),
            dataUltimaAtualizacao = Date()
        )
        
        repository.inserirCliente(cliente1)
        repository.inserirCliente(cliente2)
        
        // Act
        val clientesRota1 = repository.buscarClientesPorRota(rotaId1).first()
        
        // Assert
        assertThat(clientesRota1).hasSize(1)
        assertThat(clientesRota1.first().nome).isEqualTo("Cliente Rota 1")
    }

    @Test
    fun atualizarDebitoCliente_deveAtualizarCorretamente() = runTest {
        // Arrange
        val rota = Rota(
            nome = "Rota Teste",
            codigo = "RT001",
            descricao = "Teste",
            ativo = true,
            dataCriacao = Date(),
            dataUltimaAtualizacao = Date()
        )
        val rotaId = repository.inserirRota(rota)
        
        val cliente = Cliente(
            nome = "Cliente Teste",
            cpfCnpj = "12345678900",
            rotaId = rotaId,
            valorFicha = 0.50,
            comissaoFicha = 0.10,
            debitoAtual = 0.0,
            dataCadastro = Date(),
            dataUltimaAtualizacao = Date()
        )
        val clienteId = repository.inserirCliente(cliente)
        
        // Act
        repository.atualizarDebitoCliente(clienteId, 150.50)
        
        // Assert
        val clienteAtualizado = repository.buscarClientePorId(clienteId)
        assertThat(clienteAtualizado?.debitoAtual).isWithin(0.01).of(150.50)
    }
}
