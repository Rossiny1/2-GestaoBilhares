package com.example.gestaobilhares.core.utils

import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.Mesa
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Testes críticos para FinancialCalculator
 * Cobrem cálculos financeiros que afetam dinheiro real
 */
class FinancialCalculatorTest {

    private lateinit var mesasTeste: List<FinancialCalculator.MesaAcertoCalculo>

    @Before
    fun setup() {
        mesasTeste = listOf(
            FinancialCalculator.MesaAcertoCalculo(
                relogioInicial = 100,
                relogioFinal = 150,
                valorFixo = 0.0,
                comDefeito = false,
                relogioReiniciou = false,
                mediaFichasJogadas = 0.0
            ),
            FinancialCalculator.MesaAcertoCalculo(
                relogioInicial = 200,
                relogioFinal = 250,
                valorFixo = 0.0,
                comDefeito = false,
                relogioReiniciou = false,
                mediaFichasJogadas = 0.0
            )
        )
    }

    @Test
    fun `calcularDebitoAtual deve aplicar formula corretamente`() {
        // GIVEN: Parâmetros do acerto
        val debitoAnterior = 100.0
        val valorTotal = 200.0
        val desconto = 30.0
        val valorRecebido = 50.0

        // WHEN: Calcular débito
        val debitoAtual = FinancialCalculator.calcularDebitoAtual(
            debitoAnterior, valorTotal, desconto, valorRecebido
        )

        // THEN: Debito = 100 + 200 - 30 - 50 = 220
        assertEquals(220.0, debitoAtual, 0.001)
    }

    @Test
    fun `calcularValorTotalMesas deve somar fichas jogadas por comissao`() {
        // GIVEN: Comissão padrão
        val comissaoFicha = 0.6

        // WHEN: Calcular valor total
        val valorTotal = FinancialCalculator.calcularValorTotalMesas(mesasTeste, comissaoFicha)

        // THEN: (50 fichas * 0.6) + (50 fichas * 0.6) = 30 + 30 = 60
        assertEquals(60.0, valorTotal, 0.001)
    }

    @Test
    fun `calcularValorTotalMesas deve usar valor fixo quando definido`() {
        // GIVEN: Mesa com valor fixo
        val mesasComFixo = listOf(
            FinancialCalculator.MesaAcertoCalculo(
                relogioInicial = 100,
                relogioFinal = 150,
                valorFixo = 100.0, // Valor fixo ignora fichas
                comDefeito = false,
                relogioReiniciou = false,
                mediaFichasJogadas = 0.0
            )
        )

        // WHEN: Calcular valor total
        val valorTotal = FinancialCalculator.calcularValorTotalMesas(mesasComFixo, 0.6)

        // THEN: Deve usar valor fixo, não calcular fichas
        assertEquals(100.0, valorTotal, 0.001)
    }

    @Test
    fun `calcularValorComDesconto deve aplicar desconto corretamente`() {
        // GIVEN: Valor total e desconto
        val valorTotal = 200.0
        val desconto = 30.0

        // WHEN: Calcular valor com desconto
        val valorComDesconto = FinancialCalculator.calcularValorComDesconto(valorTotal, desconto)

        // THEN: 200 - 30 = 170
        assertEquals(170.0, valorComDesconto, 0.001)
    }

    @Test
    fun `calcularValorComDesconto deve retornar zero se desconto maior que valor`() {
        // GIVEN: Desconto maior que valor total
        val valorTotal = 100.0
        val desconto = 150.0

        // WHEN: Calcular valor com desconto
        val valorComDesconto = FinancialCalculator.calcularValorComDesconto(valorTotal, desconto)

        // THEN: Deve retornar zero (não pode ser negativo)
        assertEquals(0.0, valorComDesconto, 0.001)
    }

    @Test
    fun `calcularValorRecebido deve somar metodos de pagamento`() {
        // GIVEN: Métodos de pagamento
        val metodosPagamento = mapOf(
            "PIX" to 50.0,
            "Cartão" to 30.0,
            "Dinheiro" to 20.0
        )

        // WHEN: Calcular valor recebido
        val valorRecebido = FinancialCalculator.calcularValorRecebido(metodosPagamento)

        // THEN: 50 + 30 + 20 = 100
        assertEquals(100.0, valorRecebido, 0.001)
    }

    @Test
    fun `calcularDebitoAtual deve lidar com valores zero`() {
        // GIVEN: Todos valores zero
        val resultado = FinancialCalculator.calcularDebitoAtual(0.0, 0.0, 0.0, 0.0)

        // THEN: Deve ser zero
        assertEquals(0.0, resultado, 0.001)
    }

    @Test
    fun `calcularDebitoAtual deve lidar com valores decimais`() {
        // GIVEN: Valores decimais precisos
        val resultado = FinancialCalculator.calcularDebitoAtual(
            debitoAnterior = 100.50,
            valorTotal = 200.75,
            desconto = 30.25,
            valorRecebido = 50.30
        )

        // THEN: 100.50 + 200.75 - 30.25 - 50.30 = 220.70
        assertEquals(220.70, resultado, 0.001)
    }
}
