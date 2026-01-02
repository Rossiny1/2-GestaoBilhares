package com.example.gestaobilhares.core.utils

import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.Despesa
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Testes unitários para FinancialCalculator
 * Valida o core de cálculos financeiros do sistema
 */
class FinancialCalculatorTest {

    @Test
    fun `calcularDebitoAtual deve retornar valor correto`() {
        // Arrange
        val debitoAnterior = 100.0
        val valorTotal = 500.0
        val desconto = 50.0
        val valorRecebido = 400.0
        val esperado = 150.0 // 100 + 500 - 50 - 400
        
        // Act
        val resultado = FinancialCalculator.calcularDebitoAtual(
            debitoAnterior, valorTotal, desconto, valorRecebido
        )
        
        // Assert
        assertThat(resultado).isEqualTo(esperado)
    }

    @Test
    fun `calcularFichasJogadasMesa deve usar media quando com defeito`() {
        // Arrange
        val mesa = FinancialCalculator.MesaAcertoCalculo(
            relogioInicial = 1000,
            relogioFinal = 1200, // Diferença de 200
            valorFixo = 0.0,
            comDefeito = true,
            mediaFichasJogadas = 150.0
        )
        
        // Act
        val resultado = FinancialCalculator.calcularFichasJogadasMesa(mesa)
        
        // Assert
        assertThat(resultado).isEqualTo(150)
    }

    @Test
    fun `calcularFichasJogadasMesa deve tratar wrap-around de 4 digitos`() {
        // Arrange
        val mesa = FinancialCalculator.MesaAcertoCalculo(
            relogioInicial = 9900,
            relogioFinal = 100, // Reiniciou o relógio
            valorFixo = 0.0,
            relogioReiniciou = true
        )
        // Esperado: (100 + 10000) - 9900 = 200
        
        // Act
        val resultado = FinancialCalculator.calcularFichasJogadasMesa(mesa)
        
        // Assert
        assertThat(resultado).isEqualTo(200)
    }

    @Test
    fun `calcularFichasJogadasMesa deve tratar wrap-around de 5 digitos`() {
        // Arrange
        val mesa = FinancialCalculator.MesaAcertoCalculo(
            relogioInicial = 99900,
            relogioFinal = 200,
            valorFixo = 0.0,
            relogioReiniciou = true
        )
        // Esperado: (200 + 100000) - 99900 = 300
        
        // Act
        val resultado = FinancialCalculator.calcularFichasJogadasMesa(mesa)
        
        // Assert
        assertThat(resultado).isEqualTo(300)
    }

    @Test
    fun `calcularFichasJogadasMesa deve retornar zero se final for menor que inicial sem flag de reinicio`() {
        // Arrange
        val mesa = FinancialCalculator.MesaAcertoCalculo(
            relogioInicial = 1000,
            relogioFinal = 800,
            valorFixo = 0.0,
            relogioReiniciou = false
        )
        
        // Act
        val resultado = FinancialCalculator.calcularFichasJogadasMesa(mesa)
        
        // Assert
        assertThat(resultado).isEqualTo(0)
    }

    @Test
    fun `calcularValorTotalMesas deve somar valor fixo e fichas corretamente`() {
        // Arrange
        val mesas = listOf(
            FinancialCalculator.MesaAcertoCalculo(0, 0, valorFixo = 100.0), // Mesa Fixa
            FinancialCalculator.MesaAcertoCalculo(1000, 1050, valorFixo = 0.0) // 50 fichas
        )
        val comissaoFicha = 2.0
        val esperado = 100.0 + (50 * 2.0) // 200.0
        
        // Act
        val resultado = FinancialCalculator.calcularValorTotalMesas(mesas, comissaoFicha)
        
        // Assert
        assertThat(resultado).isEqualTo(esperado)
    }

    @Test
    fun `calcularValorRecebido deve somar metodos de pagamento corretamente`() {
        // Arrange
        val metodos = mapOf(
            "Dinheiro" to 100.0,
            "PIX" to 150.0,
            "Cartão" to 50.0
        )
        
        // Act
        val resultado = FinancialCalculator.calcularValorRecebido(metodos)
        
        // Assert
        assertThat(resultado).isEqualTo(300.0)
    }

    @Test
    fun `calcularEstatisticasCiclo deve aplicar formulas corretamente`() {
        // Simular um cenário base
        // Acertos: Total 1000.0 (PIX=200, Cartão=100, Dinheiro=700)
        // Despesas: Total 200.0 (Viagem=50, Outras=150)
        
        val now = System.currentTimeMillis()
        val acertos = listOf(
            Acerto(
                id = 1,
                clienteId = 1,
                periodoInicio = now,
                periodoFim = now,
                valorRecebido = 1000.0,
                metodosPagamentoJson = "{\"PIX\":200.0,\"Cartão\":100.0,\"Dinheiro\":700.0}"
            )
        )
        
        val despesas = listOf(
            Despesa(id = 1, rotaId = 1, descricao = "Combustivel", valor = 50.0, categoria = "Viagem"),
            Despesa(id = 2, rotaId = 1, descricao = "Lanche", valor = 150.0, categoria = "Outras")
        )
        
        // Act
        val stats = FinancialCalculator.calcularEstatisticasCiclo(acertos, despesas)
        
        // Assert
        assertThat(stats.totalRecebido).isEqualTo(1000.0)
        assertThat(stats.despesasViagem).isEqualTo(50.0)
        assertThat(stats.subtotal).isEqualTo(950.0) // 1000 - 50
        assertThat(stats.comissaoMotorista).isEqualTo(950.0 * 0.03) // 28.5
        assertThat(stats.comissaoIltair).isEqualTo(1000.0 * 0.02) // 20.0
        assertThat(stats.somaPix).isEqualTo(200.0)
        assertThat(stats.somaCartao).isEqualTo(100.0)
        assertThat(stats.somaDespesas).isEqualTo(150.0)
        
        // totalGeral = subtotal - comissaoMotorista - comissaoIltair - somaPix - somaCartao - somaDespesas - cheques
        // totalGeral = 950 - 28.5 - 20.0 - 200 - 100 - 150 - 0 = 451.5
        assertThat(stats.totalGeral).isWithin(0.01).of(451.5)
    }

    @Test
    fun `validarValoresAcerto deve retornar erro para desconto maior que total`() {
        // Act
        val resultado = FinancialCalculator.validarValoresAcerto(
            debitoAnterior = 0.0,
            valorTotal = 100.0,
            desconto = 150.0,
            valorRecebido = 0.0
        )
        
        // Assert
        assertThat(resultado).isInstanceOf(FinancialCalculator.ResultadoValidacao.Erro::class.java)
        val erro = resultado as FinancialCalculator.ResultadoValidacao.Erro
        assertThat(erro.mensagens).contains("Desconto não pode ser maior que o valor total")
    }
}
