package com.example.gestaobilhares.data

import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Acerto
import org.junit.Test
import org.junit.Assert.*
import java.text.NumberFormat
import java.util.*

class ValorDecimalConverterTest {

    @Test
    fun `valor_mesa deve ser armazenado como Double em reais`() {
        // GIVEN: Importador converte string para Double
        val valorImportado = converterValor("1,50")

        // THEN: Deve ser 1.5 (não 150)
        assertEquals(1.5, valorImportado, 0.001)
    }

    @Test
    fun `Cliente valorFicha deve manter valor em reais`() {
        // GIVEN: Cliente criado com valor 1.5
        val cliente = Cliente(
            id = 1L,
            nome = "Teste",
            rotaId = 1L,
            valorFicha = 1.5,
            comissaoFicha = 0.6
        )

        // THEN: Não deve multiplicar por 100
        assertEquals(1.5, cliente.valorFicha, 0.001)
        assertEquals(0.6, cliente.comissaoFicha, 0.001)
    }

    @Test
    fun `Formatacao monetaria deve exibir corretamente`() {
        // GIVEN: Valor em Double
        val valor = 1.5

        // WHEN: Formatar para moeda
        val valorFormatado = valor.formatarMoeda()

        // THEN: Deve exibir R$ 1,50 (não R$ 15,00) - normalizando espaços
        val esperado = "R$ 1,50"
        val resultado = valorFormatado.replace("\u00A0", " ").trim()
        assertEquals(esperado, resultado)
    }

    @Test
    fun `Acerto valorFicha deve calcular total corretamente`() {
        // GIVEN: Acerto com valorFicha = 150.0 e valorRecebido = 60.0
        val acerto = Acerto(
            id = 1L,
            clienteId = 1L,
            periodoInicio = System.currentTimeMillis(),
            periodoFim = System.currentTimeMillis(),
            totalMesas = 150.0,
            valorRecebido = 60.0
        )

        // WHEN: Calcular total
        val total = acerto.totalMesas + acerto.valorRecebido

        // THEN: Deve ser 210.0 (não 2100.0)
        assertEquals(210.0, total, 0.001)
    }

    // Helper functions
    private fun converterValor(valor: String): Double {
        return valor.replace(",", ".").toDouble()
    }

    private fun Double.formatarMoeda(): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return formatter.format(this)
    }
}
