package com.example.gestaobilhares.ui.settlement

import com.example.gestaobilhares.data.entities.Mesa
import org.junit.Test
import org.junit.Assert.*

class SettlementSummaryDialogTest {

    @Test
    fun `test gerarTextoReciboProfissional formata corretamente`() {
        // Arrange
        val clienteNome = "João Silva"
        val mesas = listOf(
            Mesa(1, "Mesa 1", "Sinuca", "Boa", 10, 15, 1),
            Mesa(2, "Mesa 2", "Pool", "Boa", 5, 12, 1)
        )
        val total = 150.0

        // Act
        val recibo = SettlementSummaryDialog().gerarTextoReciboProfissional(clienteNome, mesas, total)

        // Assert
        assertTrue("Recibo deve conter título", recibo.contains("********* RECIBO DE ACERTO *********"))
        assertTrue("Recibo deve conter nome do cliente", recibo.contains("João Silva"))
        assertTrue("Recibo deve conter total formatado", recibo.contains("R$ 150,00"))
        assertTrue("Recibo deve conter agradecimento", recibo.contains("Obrigado por confiar!"))
    }

    @Test
    fun `test gerarTextoReciboProfissional com mesas vazias`() {
        // Arrange
        val clienteNome = "Maria Santos"
        val mesas = emptyList<Mesa>()
        val total = 0.0

        // Act
        val recibo = SettlementSummaryDialog().gerarTextoReciboProfissional(clienteNome, mesas, total)

        // Assert
        assertTrue("Recibo deve conter cliente mesmo sem mesas", recibo.contains("Maria Santos"))
        assertTrue("Recibo deve conter total zero", recibo.contains("R$ 0,00"))
        assertFalse("Recibo não deve conter detalhes de mesas", recibo.contains("Mesa"))
    }

    @Test
    fun `test gerarTextoReciboProfissional calcula fichas corretamente`() {
        // Arrange
        val clienteNome = "Pedro Costa"
        val mesas = listOf(
            Mesa(1, "Mesa 1", "Sinuca", "Boa", 10, 25, 1) // 15 fichas jogadas
        )
        val total = 75.0

        // Act
        val recibo = SettlementSummaryDialog().gerarTextoReciboProfissional(clienteNome, mesas, total)

        // Assert
        assertTrue("Recibo deve mostrar fichas jogadas corretas", recibo.contains("15 fichas"))
    }

    @Test
    fun `test gerarTextoReciboProfissional com valores nulos`() {
        // Arrange
        val clienteNome = "Ana Oliveira"
        val mesas = listOf(
            Mesa(1, "Mesa 1", "Sinuca", "Boa", null, null, 1) // fichas nulas
        )
        val total = 50.0

        // Act
        val recibo = SettlementSummaryDialog().gerarTextoReciboProfissional(clienteNome, mesas, total)

        // Assert
        assertTrue("Recibo deve tratar fichas nulas como 0", recibo.contains("0 fichas"))
    }

    @Test
    fun `test centerText centraliza corretamente`() {
        // Arrange
        val dialog = SettlementSummaryDialog()
        val texto = "TESTE"
        val largura = 10

        // Act
        val resultado = dialog.centerText(texto, largura)

        // Assert
        assertEquals("Texto deve estar centralizado", "  TESTE   ", resultado)
    }

    @Test
    fun `test padRight e padLeft funcionam corretamente`() {
        // Arrange
        val dialog = SettlementSummaryDialog()
        val texto = "ABC"
        val tamanho = 6

        // Act
        val padRight = dialog.padRight(texto, tamanho)
        val padLeft = dialog.padLeft(texto, tamanho)

        // Assert
        assertEquals("padRight deve adicionar espaços à direita", "ABC   ", padRight)
        assertEquals("padLeft deve adicionar espaços à esquerda", "   ABC", padLeft)
    }
} 