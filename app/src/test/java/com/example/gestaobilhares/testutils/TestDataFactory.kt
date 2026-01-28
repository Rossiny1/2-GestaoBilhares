package com.example.gestaobilhares.testutils

import com.example.gestaobilhares.data.entities.*
import java.util.Date
import java.time.LocalDateTime

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
        dataCadastro: Long = System.currentTimeMillis(),
        dataUltimaAtualizacao: Long = System.currentTimeMillis()
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
        colaboradorId: Long? = 1L,
        dataAcerto: Long = System.currentTimeMillis(),
        periodoInicio: Long = System.currentTimeMillis(),
        periodoFim: Long = System.currentTimeMillis(),
        totalMesas: Double = 100.0,
        debitoAnterior: Double = 0.0,
        valorTotal: Double = 100.0,
        desconto: Double = 0.0,
        valorComDesconto: Double = 100.0,
        valorRecebido: Double = 100.0,
        debitoAtual: Double = 0.0,
        status: StatusAcerto = StatusAcerto.FINALIZADO,
        observacoes: String? = null,
        metodoPagamento: String = "PIX",
        rotaId: Long? = 1L,
        cicloId: Long? = 1L
    ) = Acerto(
        id = id,
        clienteId = clienteId,
        colaboradorId = colaboradorId,
        dataAcerto = dataAcerto,
        periodoInicio = periodoInicio,
        periodoFim = periodoFim,
        totalMesas = totalMesas,
        debitoAnterior = debitoAnterior,
        valorTotal = valorTotal,
        desconto = desconto,
        valorComDesconto = valorComDesconto,
        valorRecebido = valorRecebido,
        debitoAtual = debitoAtual,
        status = status,
        observacoes = observacoes,
        dataCriacao = System.currentTimeMillis(),
        dataFinalizacao = null,
        metodosPagamentoJson = "{\"$metodoPagamento\": $valorRecebido}",
        rotaId = rotaId,
        cicloId = cicloId
    )
    
    fun createCiclo(
        id: Long = 1L,
        rotaId: Long = 1L,
        numeroCiclo: Int = 1,
        ano: Int = 2025,
        dataInicio: Long = System.currentTimeMillis(),
        dataFim: Long = System.currentTimeMillis(),
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
        dataCriacao: Long = System.currentTimeMillis()
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
        tipoDespesa: String = "Combustível",
        valor: Double = 100.0,
        descricao: String = "Despesa teste",
        categoria: String = "Transporte",
        dataHora: Long = System.currentTimeMillis()
    ) = Despesa(
        id = id,
        rotaId = rotaId,
        cicloId = cicloId,
        tipoDespesa = tipoDespesa,
        valor = valor,
        descricao = descricao,
        categoria = categoria,
        dataHora = dataHora,
        observacoes = ""
    )
    
    fun createMesa(
        id: Long = 1L,
        clienteId: Long = 1L,
        numeroMesa: String = "Mesa 1",
        ativa: Boolean = true,
        dataCadastro: Long = System.currentTimeMillis()
    ) = Mesa(
        id = id,
        clienteId = clienteId,
        numero = numeroMesa,
        ativa = ativa,
        dataInstalacao = dataCadastro,
        tipoMesa = TipoMesa.SINUCA,
        tamanho = TamanhoMesa.GRANDE,
        estadoConservacao = EstadoConservacao.BOM
    )
}

