package com.example.gestaobilhares.utils

import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.Despesa
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.TipoMeta
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

/**
 * ✅ FASE 12.2: Testes unitários para FinancialCalculator
 * 
 * Testa cálculos financeiros:
 * - Cálculo de débito atual
 * - Cálculo de valor total das mesas
 * - Cálculo de valor com desconto
 * - Cálculo de valor recebido
 * - Estatísticas do ciclo
 * - Validações financeiras
 */
class FinancialCalculatorTest {

    @Test
    fun `calcularDebitoAtual deve calcular corretamente`() {
        val debitoAnterior = 100.0
        val valorTotal = 200.0
        val desconto = 10.0
        val valorRecebido = 250.0
        
        val resultado = FinancialCalculator.calcularDebitoAtual(
            debitoAnterior,
            valorTotal,
            desconto,
            valorRecebido
        )
        
        // Fórmula: débitoAnterior + valorTotal - desconto - valorRecebido
        // 100 + 200 - 10 - 250 = 40
        assertEquals(40.0, resultado, 0.01)
    }

    @Test
    fun `calcularDebitoAtual deve retornar valor negativo quando valor recebido é maior`() {
        val debitoAnterior = 100.0
        val valorTotal = 200.0
        val desconto = 10.0
        val valorRecebido = 400.0
        
        val resultado = FinancialCalculator.calcularDebitoAtual(
            debitoAnterior,
            valorTotal,
            desconto,
            valorRecebido
        )
        
        // 100 + 200 - 10 - 400 = -110
        assertEquals(-110.0, resultado, 0.01)
    }

    @Test
    fun `calcularValorTotalMesas deve calcular com valor fixo`() {
        val mesas = listOf(
            FinancialCalculator.MesaAcertoCalculo(
                relogioInicial = 100,
                relogioFinal = 200,
                valorFixo = 50.0
            )
        )
        val comissaoFicha = 0.5
        
        val resultado = FinancialCalculator.calcularValorTotalMesas(mesas, comissaoFicha)
        
        // Deve usar valor fixo, não calcular por fichas
        assertEquals(50.0, resultado, 0.01)
    }

    @Test
    fun `calcularValorTotalMesas deve calcular por fichas quando valor fixo é zero`() {
        val mesas = listOf(
            FinancialCalculator.MesaAcertoCalculo(
                relogioInicial = 100,
                relogioFinal = 200,
                valorFixo = 0.0
            )
        )
        val comissaoFicha = 0.5
        
        val resultado = FinancialCalculator.calcularValorTotalMesas(mesas, comissaoFicha)
        
        // (200 - 100) * 0.5 = 50
        assertEquals(50.0, resultado, 0.01)
    }

    @Test
    fun `calcularValorTotalMesas deve somar múltiplas mesas`() {
        val mesas = listOf(
            FinancialCalculator.MesaAcertoCalculo(100, 200, 0.0),
            FinancialCalculator.MesaAcertoCalculo(50, 100, 0.0)
        )
        val comissaoFicha = 0.5
        
        val resultado = FinancialCalculator.calcularValorTotalMesas(mesas, comissaoFicha)
        
        // (200-100)*0.5 + (100-50)*0.5 = 50 + 25 = 75
        assertEquals(75.0, resultado, 0.01)
    }

    @Test
    fun `calcularValorTotalMesas deve usar zero quando relógio final menor que inicial`() {
        val mesas = listOf(
            FinancialCalculator.MesaAcertoCalculo(
                relogioInicial = 200,
                relogioFinal = 100,
                valorFixo = 0.0
            )
        )
        val comissaoFicha = 0.5
        
        val resultado = FinancialCalculator.calcularValorTotalMesas(mesas, comissaoFicha)
        
        // Deve usar coerceAtLeast(0), então fichasJogadas = 0
        assertEquals(0.0, resultado, 0.01)
    }

    @Test
    fun `calcularValorComDesconto deve subtrair desconto`() {
        val valorTotal = 100.0
        val desconto = 10.0
        
        val resultado = FinancialCalculator.calcularValorComDesconto(valorTotal, desconto)
        
        assertEquals(90.0, resultado, 0.01)
    }

    @Test
    fun `calcularValorComDesconto deve retornar zero quando desconto maior que valor`() {
        val valorTotal = 100.0
        val desconto = 150.0
        
        val resultado = FinancialCalculator.calcularValorComDesconto(valorTotal, desconto)
        
        // maxOf(0.0, valorTotal - desconto) = maxOf(0.0, -50) = 0
        assertEquals(0.0, resultado, 0.01)
    }

    @Test
    fun `calcularValorRecebido deve somar valores dos métodos de pagamento`() {
        val metodosPagamento = mapOf(
            "PIX" to 100.0,
            "Cartão" to 50.0,
            "Dinheiro" to 25.0
        )
        
        val resultado = FinancialCalculator.calcularValorRecebido(metodosPagamento)
        
        assertEquals(175.0, resultado, 0.01)
    }

    @Test
    fun `calcularValorRecebido deve retornar zero para mapa vazio`() {
        val resultado = FinancialCalculator.calcularValorRecebido(emptyMap())
        
        assertEquals(0.0, resultado, 0.01)
    }

    @Test
    fun `calcularEstatisticasCiclo deve calcular estatísticas corretamente`() {
        val acertos = listOf(
            createAcerto(1L, 100.0, """{"PIX": 60.0, "Cartão": 40.0}"""),
            createAcerto(2L, 200.0, """{"PIX": 100.0, "Dinheiro": 100.0}""")
        )
        val despesas = listOf(
            createDespesa("Viagem", 50.0),
            createDespesa("Combustível", 30.0)
        )
        
        val resultado = FinancialCalculator.calcularEstatisticasCiclo(acertos, despesas)
        
        // Total recebido: 100 + 200 = 300
        assertEquals(300.0, resultado.totalRecebido, 0.01)
        
        // Despesas viagem: 50
        assertEquals(50.0, resultado.despesasViagem, 0.01)
        
        // Subtotal: 300 - 50 = 250
        assertEquals(250.0, resultado.subtotal, 0.01)
        
        // Comissão motorista: 250 * 0.03 = 7.5
        assertEquals(7.5, resultado.comissaoMotorista, 0.01)
        
        // Comissão Iltair: 300 * 0.02 = 6
        assertEquals(6.0, resultado.comissaoIltair, 0.01)
        
        // Soma PIX: 60 + 100 = 160
        assertEquals(160.0, resultado.somaPix, 0.01)
        
        // Soma Cartão: 40
        assertEquals(40.0, resultado.somaCartao, 0.01)
        
        // Soma despesas: 80 - 50 = 30
        assertEquals(30.0, resultado.somaDespesas, 0.01)
    }

    @Test
    fun `validarValoresAcerto deve retornar sucesso para valores válidos`() {
        val resultado = FinancialCalculator.validarValoresAcerto(
            debitoAnterior = 100.0,
            valorTotal = 200.0,
            desconto = 10.0,
            valorRecebido = 250.0
        )
        
        assertTrue("Valores válidos devem retornar sucesso", resultado is FinancialCalculator.ResultadoValidacao.Sucesso)
    }

    @Test
    fun `validarValoresAcerto deve retornar erro para débito anterior negativo`() {
        val resultado = FinancialCalculator.validarValoresAcerto(
            debitoAnterior = -100.0,
            valorTotal = 200.0,
            desconto = 10.0,
            valorRecebido = 250.0
        )
        
        assertTrue("Débito anterior negativo deve retornar erro", resultado is FinancialCalculator.ResultadoValidacao.Erro)
    }

    @Test
    fun `validarValoresAcerto deve retornar erro para desconto maior que valor total`() {
        val resultado = FinancialCalculator.validarValoresAcerto(
            debitoAnterior = 100.0,
            valorTotal = 200.0,
            desconto = 300.0,
            valorRecebido = 250.0
        )
        
        assertTrue("Desconto maior que valor total deve retornar erro", resultado is FinancialCalculator.ResultadoValidacao.Erro)
        if (resultado is FinancialCalculator.ResultadoValidacao.Erro) {
            assertTrue("Deve conter mensagem sobre desconto", 
                resultado.mensagens.any { it.contains("Desconto não pode ser maior") })
        }
    }

    @Test
    fun `validarValoresAcerto deve retornar erro para valor recebido negativo`() {
        val resultado = FinancialCalculator.validarValoresAcerto(
            debitoAnterior = 100.0,
            valorTotal = 200.0,
            desconto = 10.0,
            valorRecebido = -50.0
        )
        
        assertTrue("Valor recebido negativo deve retornar erro", resultado is FinancialCalculator.ResultadoValidacao.Erro)
    }

    // ==================== HELPERS ====================

    private fun createAcerto(clienteId: Long, valorRecebido: Double, metodosPagamentoJson: String): Acerto {
        val agora = Date()
        return Acerto(
            id = 0L,
            clienteId = clienteId,
            rotaId = 1L,
            cicloId = 1L,
            dataAcerto = agora,
            periodoInicio = agora,
            periodoFim = agora,
            valorRecebido = valorRecebido,
            debitoAnterior = 0.0,
            debitoAtual = 0.0,
            observacoes = "",
            metodosPagamentoJson = metodosPagamentoJson,
            dataCriacao = agora
        )
    }

    private fun createDespesa(categoria: String, valor: Double): Despesa {
        return Despesa(
            id = 0L,
            rotaId = 1L,
            descricao = "Despesa de $categoria",
            valor = valor,
            categoria = categoria,
            cicloId = 1L,
            observacoes = "",
            veiculoId = null
        )
    }
}

