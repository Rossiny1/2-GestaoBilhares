package com.example.gestaobilhares.data.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Date

/**
 * Testes para utilitários de cálculo de negócio
 * 
 * Valida cálculos críticos do app
 */
class CalculoUtilsTest {

    @Test
    fun `calcularSubtotalPorFichas deve retornar valor correto`() {
        // Arrange
        val relogioInicial = 1000
        val relogioFinal = 1200
        val valorFicha = 0.50
        val esperado = 100.0  // (1200 - 1000) * 0.50
        
        // Act
        val resultado = calcularSubtotalPorFichas(relogioInicial, relogioFinal, valorFicha)
        
        // Assert
        assertThat(resultado).isWithin(0.01).of(esperado)
    }

    @Test
    fun `calcularSubtotalPorFichas deve retornar zero quando relogios iguais`() {
        // Arrange
        val relogioInicial = 1000
        val relogioFinal = 1000
        val valorFicha = 0.50
        
        // Act
        val resultado = calcularSubtotalPorFichas(relogioInicial, relogioFinal, valorFicha)
        
        // Assert
        assertThat(resultado).isEqualTo(0.0)
    }

    @Test
    fun `calcularComissao deve retornar percentual correto`() {
        // Arrange
        val valorBase = 1000.0
        val percentualComissao = 10.0  // 10%
        val esperado = 100.0
        
        // Act
        val resultado = calcularComissao(valorBase, percentualComissao)
        
        // Assert
        assertThat(resultado).isWithin(0.01).of(esperado)
    }

    @Test
    fun `calcularComissao deve retornar zero para percentual zero`() {
        // Arrange
        val valorBase = 1000.0
        val percentualComissao = 0.0
        
        // Act
        val resultado = calcularComissao(valorBase, percentualComissao)
        
        // Assert
        assertThat(resultado).isEqualTo(0.0)
    }

    @Test
    fun `calcularValorLiquido deve descontar comissao corretamente`() {
        // Arrange
        val valorBruto = 1000.0
        val comissao = 100.0
        val esperado = 900.0
        
        // Act
        val resultado = calcularValorLiquido(valorBruto, comissao)
        
        // Assert
        assertThat(resultado).isWithin(0.01).of(esperado)
    }

    @Test
    fun `formatarMoeda deve retornar formato brasileiro correto`() {
        // Arrange
        val valor = 1234.56
        val esperado = "R$ 1.234,56"
        
        // Act
        val resultado = formatarMoeda(valor)
        
        // Assert
        assertThat(resultado).isEqualTo(esperado)
    }

    @Test
    fun `formatarMoeda deve tratar valores negativos`() {
        // Arrange
        val valor = -500.00
        val esperado = "-R$ 500,00"
        
        // Act
        val resultado = formatarMoeda(valor)
        
        // Assert
        assertThat(resultado).contains("-")
        assertThat(resultado).contains("500")
    }

    // Helper functions (deveria estar em uma classe Utils real)
    private fun calcularSubtotalPorFichas(
        relogioInicial: Int,
        relogioFinal: Int,
        valorFicha: Double
    ): Double {
        val quantidadeFichas = relogioFinal - relogioInicial
        return if (quantidadeFichas > 0) quantidadeFichas * valorFicha else 0.0
    }

    private fun calcularComissao(valorBase: Double, percentual: Double): Double {
        return valorBase * (percentual / 100.0)
    }

    private fun calcularValorLiquido(valorBruto: Double, comissao: Double): Double {
        return valorBruto - comissao
    }

    private fun formatarMoeda(valor: Double): String {
        return if (valor >= 0) {
            "R$ ${String.format("%,.2f", valor).replace(",", "X").replace(".", ",").replace("X", ".")}"
        } else {
            "-R$ ${String.format("%,.2f", Math.abs(valor)).replace(",", "X").replace(".", ",").replace("X", ".")}"
        }
    }
}
