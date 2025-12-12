package com.example.gestaobilhares.testutils

import com.example.gestaobilhares.data.entities.*
import java.util.Date

/**
 * Helper object para criar mocks de entidades em testes.
 * Centraliza criação de dados de teste com valores padrão.
 */
object TestDataFactory {
    
    fun createCliente(
        id: Long = 1L,
        rotaId: Long = 1L,
        nome: String = "Cliente Teste",
        cpfCnpj: String = "12345678900",
        telefone: String = "(11) 98765-4321",
        endereco: String = "Rua Teste, 123",
        cidade: String = "São Paulo",
        estado: String = "SP",
        cep: String = "01234-567",
        valorFicha: Double = 2.0,
        comissaoFicha: Double = 0.5,
        ativo: Boolean = true,
        dataCadastro: Date = Date(),
        dataUltimaAtualizacao: Date = Date()
    ) = Cliente(
        id = id,
        rotaId = rotaId,
        nome = nome,
        cpfCnpj = cpfCnpj,
        telefone = telefone,
        endereco = endereco,
        cidade = cidade,
        estado = estado,
        cep = cep,
        valorFicha = valorFicha,
        comissaoFicha = comissaoFicha,
        ativo = ativo,
        dataCadastro = dataCadastro,
        dataUltimaAtualizacao = dataUltimaAtualizacao
    )
    
    fun createAcerto(
        id: Long = 1L,
        clienteId: Long = 1L,
        rotaId: Long = 1L,
        cicloId: Long = 1L,
        fichasJogadas: Int = 100,
        valorFicha: Double = 2.0,
        totalFichas: Double = 200.0,
        comissaoFicha: Double = 0.5,
        totalComissao: Double = 50.0,
        valorRecebido: Double = 150.0,
        metodoPagamento: String = "PIX",
        periodoInicio: Date = Date(),
        periodoFim: Date = Date(),
        dataAcerto: Date = Date()
    ) = Acerto(
        id = id,
        clienteId = clienteId,
        rotaId = rotaId,
        cicloId = cicloId,
        fichasJogadas = fichasJogadas,
        valorFicha = valorFicha,
        totalFichas = totalFichas,
        comissaoFicha = comissaoFicha,
        totalComissao = totalComissao,
        valorRecebido = valorRecebido,
        metodosPagamentoJson = "{\"$metodoPagamento\": $valorRecebido}",
        periodoInicio = periodoInicio,
        periodoFim = periodoFim,
        dataAcerto = dataAcerto
    )
    
    fun createCiclo(
        id: Long = 1L,
        rotaId: Long = 1L,
        numeroCiclo: Int = 1,
        ano: Int = 2025,
        dataInicio: Date = Date(),
        dataFim: Date = Date(),
        status: StatusCicloAcerto = StatusCicloAcerto.EM_ANDAMENTO,
        totalClientes: Int = 10,
        observacoes: String = "",
        criadoPor: String = "Sistema"
    ) = CicloAcertoEntity(
        id = id,
        rotaId = rotaId,
        numeroCiclo = numeroCiclo,
        ano = ano,
        dataInicio = dataInicio,
        dataFim = dataFim,
        status = status,
        totalClientes = totalClientes,
        observacoes = observacoes,
        criadoPor = criadoPor
    )
    
    fun createRota(
        id: Long = 1L,
        nome: String = "Rota Teste",
        cidades: String = "São Paulo, Campinas",
        ativa: Boolean = true,
        dataCriacao: Date = Date()
    ) = Rota(
        id = id,
        nome = nome,
        cidades = cidades,
        ativa = ativa,
        dataCriacao = dataCriacao
    )
    
    fun createDespesa(
        id: Long = 1L,
        rotaId: Long = 1L,
        cicloId: Long = 1L,
        tipoDespesaId: Long = 1L,
        valor: Double = 100.0,
        descricao: String = "Despesa teste",
        data: Date = Date()
    ) = Despesa(
        id = id,
        rotaId = rotaId,
        cicloId = cicloId,
        tipoDespesaId = tipoDespesaId,
        valor = valor,
        descricao = descricao,
        data = data,
        observacoes = null
    )
    
    fun createMesa(
        id: Long = 1L,
        clienteId: Long = 1L,
        numeroMesa: String = "Mesa 1",
        ativa: Boolean = true,
        dataCadastro: Date = Date()
    ) = Mesa(
        id = id,
        clienteId = clienteId,
        numeroMesa = numeroMesa,
        ativa = ativa,
        dataCadastro = dataCadastro
    )
}

