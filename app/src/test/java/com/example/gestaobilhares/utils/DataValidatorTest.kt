package com.example.gestaobilhares.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * ✅ FASE 12.2: Testes unitários para DataValidator
 * 
 * Testa validações de dados:
 * - Campos obrigatórios
 * - Validações de email, telefone, CPF, CNPJ
 * - Validações numéricas
 * - Validações compostas
 */
class DataValidatorTest {

    @Test
    fun `validarCampoObrigatorio deve retornar sucesso para campo preenchido`() {
        val resultado = DataValidator.validarCampoObrigatorio("teste", "Nome")
        assertTrue("Campo preenchido deve retornar sucesso", resultado.isSucesso())
    }

    @Test
    fun `validarCampoObrigatorio deve retornar erro para campo vazio`() {
        val resultado = DataValidator.validarCampoObrigatorio("", "Nome")
        assertTrue("Campo vazio deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarCampoObrigatorio deve retornar erro para campo nulo`() {
        val resultado = DataValidator.validarCampoObrigatorio(null, "Nome")
        assertTrue("Campo nulo deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarEmail deve retornar sucesso para email válido`() {
        val resultado = DataValidator.validarEmail("teste@example.com")
        assertTrue("Email válido deve retornar sucesso", resultado.isSucesso())
    }

    @Test
    fun `validarEmail deve retornar erro para email inválido`() {
        val resultado = DataValidator.validarEmail("email_invalido")
        assertTrue("Email inválido deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarEmail deve retornar sucesso para email vazio (opcional)`() {
        val resultado = DataValidator.validarEmail("")
        assertTrue("Email vazio deve retornar sucesso (opcional)", resultado.isSucesso())
    }

    @Test
    fun `validarTelefone deve retornar sucesso para telefone válido`() {
        val resultado = DataValidator.validarTelefone("(11) 98765-4321")
        assertTrue("Telefone válido deve retornar sucesso", resultado.isSucesso())
    }

    @Test
    fun `validarTelefone deve retornar erro para telefone muito curto`() {
        val resultado = DataValidator.validarTelefone("123456789")
        assertTrue("Telefone muito curto deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarCPF deve retornar sucesso para CPF válido`() {
        // CPF válido de exemplo (111.444.777-35)
        val resultado = DataValidator.validarCPF("11144477735")
        assertTrue("CPF válido deve retornar sucesso", resultado.isSucesso())
    }

    @Test
    fun `validarCPF deve retornar erro para CPF inválido`() {
        val resultado = DataValidator.validarCPF("12345678901")
        assertTrue("CPF inválido deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarCPF deve retornar erro para CPF com dígitos iguais`() {
        val resultado = DataValidator.validarCPF("11111111111")
        assertTrue("CPF com dígitos iguais deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarCNPJ deve retornar sucesso para CNPJ válido`() {
        // CNPJ válido de exemplo (11.222.333/0001-81)
        val resultado = DataValidator.validarCNPJ("11222333000181")
        assertTrue("CNPJ válido deve retornar sucesso", resultado.isSucesso())
    }

    @Test
    fun `validarCNPJ deve retornar erro para CNPJ inválido`() {
        val resultado = DataValidator.validarCNPJ("12345678000190")
        assertTrue("CNPJ inválido deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarValorPositivo deve retornar sucesso para valor positivo`() {
        val resultado = DataValidator.validarValorPositivo(10.0, "Valor")
        assertTrue("Valor positivo deve retornar sucesso", resultado.isSucesso())
    }

    @Test
    fun `validarValorPositivo deve retornar erro para valor zero`() {
        val resultado = DataValidator.validarValorPositivo(0.0, "Valor")
        assertTrue("Valor zero deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarValorPositivo deve retornar erro para valor negativo`() {
        val resultado = DataValidator.validarValorPositivo(-10.0, "Valor")
        assertTrue("Valor negativo deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarValorNaoNegativo deve retornar sucesso para valor zero`() {
        val resultado = DataValidator.validarValorNaoNegativo(0.0, "Valor")
        assertTrue("Valor zero deve retornar sucesso", resultado.isSucesso())
    }

    @Test
    fun `validarValorNaoNegativo deve retornar erro para valor negativo`() {
        val resultado = DataValidator.validarValorNaoNegativo(-10.0, "Valor")
        assertTrue("Valor negativo deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarRange deve retornar sucesso para valor dentro do range`() {
        val resultado = DataValidator.validarRange(5.0, 0.0, 10.0, "Valor")
        assertTrue("Valor dentro do range deve retornar sucesso", resultado.isSucesso())
    }

    @Test
    fun `validarRange deve retornar erro para valor abaixo do mínimo`() {
        val resultado = DataValidator.validarRange(-5.0, 0.0, 10.0, "Valor")
        assertTrue("Valor abaixo do mínimo deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarRange deve retornar erro para valor acima do máximo`() {
        val resultado = DataValidator.validarRange(15.0, 0.0, 10.0, "Valor")
        assertTrue("Valor acima do máximo deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarDadosMesa deve retornar sucesso para dados válidos`() {
        val resultado = DataValidator.validarDadosMesa(
            relogioInicial = 100,
            relogioFinal = 200,
            comDefeito = false,
            relogioReiniciou = false,
            fotoRelogioFinal = null
        )
        assertTrue("Dados válidos devem retornar sucesso", resultado.isSucesso())
    }

    @Test
    fun `validarDadosMesa deve retornar erro quando relógio final menor que inicial`() {
        val resultado = DataValidator.validarDadosMesa(
            relogioInicial = 200,
            relogioFinal = 100,
            comDefeito = false,
            relogioReiniciou = false,
            fotoRelogioFinal = null
        )
        assertTrue("Relógio final menor que inicial deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarDadosMesa deve retornar erro quando com defeito sem foto`() {
        val resultado = DataValidator.validarDadosMesa(
            relogioInicial = 100,
            relogioFinal = 200,
            comDefeito = true,
            relogioReiniciou = false,
            fotoRelogioFinal = null
        )
        assertTrue("Mesa com defeito sem foto deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarDespesa deve retornar sucesso para despesa válida`() {
        val resultado = DataValidator.validarDespesa(
            valor = 100.0,
            quantidade = 1,
            categoria = "Combustível",
            tipo = "Gasolina",
            veiculoId = null
        )
        assertTrue("Despesa válida deve retornar sucesso", resultado.isSucesso())
    }

    @Test
    fun `validarDespesa deve retornar erro para valor zero`() {
        val resultado = DataValidator.validarDespesa(
            valor = 0.0,
            quantidade = 1,
            categoria = "Combustível",
            tipo = "Gasolina",
            veiculoId = null
        )
        assertTrue("Despesa com valor zero deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarDespesa deve retornar erro para despesa de viagem sem veículo`() {
        val resultado = DataValidator.validarDespesa(
            valor = 100.0,
            quantidade = 1,
            categoria = "Viagem",
            tipo = "Pedágio",
            veiculoId = null
        )
        assertTrue("Despesa de viagem sem veículo deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarCliente deve retornar sucesso para cliente válido`() {
        val resultado = DataValidator.validarCliente(
            nome = "João Silva",
            endereco = "Rua Teste, 123",
            comissaoFicha = 0.5,
            email = "joao@example.com",
            telefone = "(11) 98765-4321",
            cpf = "11144477735"
        )
        assertTrue("Cliente válido deve retornar sucesso", resultado.isSucesso())
    }

    @Test
    fun `validarCliente deve retornar erro para nome muito curto`() {
        val resultado = DataValidator.validarCliente(
            nome = "Jo",
            endereco = "Rua Teste, 123",
            comissaoFicha = 0.5
        )
        assertTrue("Cliente com nome muito curto deve retornar erro", resultado.isErro())
    }

    @Test
    fun `validarCliente deve retornar erro para endereço muito curto`() {
        val resultado = DataValidator.validarCliente(
            nome = "João Silva",
            endereco = "Rua",
            comissaoFicha = 0.5
        )
        assertTrue("Cliente com endereço muito curto deve retornar erro", resultado.isErro())
    }
}

