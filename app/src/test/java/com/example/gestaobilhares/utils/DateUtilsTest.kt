package com.example.gestaobilhares.utils

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

/**
 * ✅ FASE 12.2: Testes unitários para DateUtils
 * 
 * Testa funcionalidades de manipulação de datas:
 * - Cálculo de range de ano
 * - Formatação de datas
 */
class DateUtilsTest {

    @Test
    fun `calcularRangeAno deve retornar range correto para ano válido`() {
        val ano = "2025"
        val (inicio, fim) = DateUtils.calcularRangeAno(ano)
        
        assertNotNull("Início não deve ser nulo", inicio)
        assertNotNull("Fim não deve ser nulo", fim)
        assertTrue("Início deve ser menor que fim", inicio < fim)
        
        // Verificar que o início é 1º de janeiro do ano
        val calInicio = Calendar.getInstance().apply { timeInMillis = inicio }
        assertEquals("Ano de início deve ser 2025", 2025, calInicio.get(Calendar.YEAR))
        assertEquals("Mês de início deve ser janeiro (0)", 0, calInicio.get(Calendar.MONTH))
        assertEquals("Dia de início deve ser 1", 1, calInicio.get(Calendar.DAY_OF_MONTH))
        
        // Verificar que o fim é 31 de dezembro do ano às 23:59:59
        val calFim = Calendar.getInstance().apply { timeInMillis = fim }
        assertEquals("Ano de fim deve ser 2025", 2025, calFim.get(Calendar.YEAR))
        assertEquals("Mês de fim deve ser dezembro (11)", 11, calFim.get(Calendar.MONTH))
        assertEquals("Dia de fim deve ser 31", 31, calFim.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `calcularRangeAno deve funcionar com diferentes anos`() {
        val anos = listOf("2020", "2021", "2022", "2023", "2024", "2025", "2026")
        
        anos.forEach { ano ->
            val (inicio, fim) = DateUtils.calcularRangeAno(ano)
            
            val calInicio = Calendar.getInstance().apply { timeInMillis = inicio }
            val calFim = Calendar.getInstance().apply { timeInMillis = fim }
            
            assertEquals("Ano de início deve ser $ano", ano.toInt(), calInicio.get(Calendar.YEAR))
            assertEquals("Ano de fim deve ser $ano", ano.toInt(), calFim.get(Calendar.YEAR))
            assertTrue("Início deve ser menor que fim para $ano", inicio < fim)
        }
    }

    @Test
    fun `calcularRangeAno deve retornar range válido mesmo para ano inválido`() {
        // Testar com string inválida - deve tratar graciosamente
        val ano = "ano_invalido"
        val (inicio, fim) = DateUtils.calcularRangeAno(ano)
        
        // Mesmo com entrada inválida, deve retornar valores válidos (pode usar ano atual ou padrão)
        assertNotNull("Início não deve ser nulo mesmo com entrada inválida", inicio)
        assertNotNull("Fim não deve ser nulo mesmo com entrada inválida", fim)
    }

    @Test
    fun `calcularRangeAno deve retornar range válido para ano vazio`() {
        val ano = ""
        val (inicio, fim) = DateUtils.calcularRangeAno(ano)
        
        assertNotNull("Início não deve ser nulo mesmo com ano vazio", inicio)
        assertNotNull("Fim não deve ser nulo mesmo com ano vazio", fim)
    }

    @Test
    fun `formatarDataBrasileira deve formatar corretamente`() {
        val calendar = Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 15)
        }
        val data = calendar.time
        val formatada = DateUtils.formatarDataBrasileira(data)
        
        assertEquals("Data deve estar no formato dd/MM/yyyy", "15/01/2025", formatada)
    }

    @Test
    fun `parseDataBrasileira deve converter string válida para Date`() {
        val dataString = "15/01/2025"
        val data = DateUtils.parseDataBrasileira(dataString)
        
        assertNotNull("Data válida deve ser convertida", data)
        
        val calendar = Calendar.getInstance().apply { time = data!! }
        assertEquals("Ano deve ser 2025", 2025, calendar.get(Calendar.YEAR))
        assertEquals("Mês deve ser janeiro (0)", 0, calendar.get(Calendar.MONTH))
        assertEquals("Dia deve ser 15", 15, calendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `parseDataBrasileira deve retornar null para string inválida`() {
        val dataString = "data_invalida"
        val data = DateUtils.parseDataBrasileira(dataString)
        
        assertNull("String inválida deve retornar null", data)
    }

    @Test
    fun `adicionarDias deve adicionar dias corretamente`() {
        val calendar = Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 15)
        }
        val data = calendar.time
        val dataFutura = DateUtils.adicionarDias(data, 10)
        
        val calFutura = Calendar.getInstance().apply { time = dataFutura }
        assertEquals("Dia deve ser 25", 25, calFutura.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `isMesmoDia deve retornar true para datas no mesmo dia`() {
        val calendar1 = Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 15, 10, 30)
        }
        val calendar2 = Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 15, 20, 45)
        }
        
        assertTrue("Datas no mesmo dia devem ser iguais", DateUtils.isMesmoDia(calendar1.time, calendar2.time))
    }

    @Test
    fun `isMesmoDia deve retornar false para datas em dias diferentes`() {
        val calendar1 = Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 15)
        }
        val calendar2 = Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 16)
        }
        
        assertFalse("Datas em dias diferentes não devem ser iguais", DateUtils.isMesmoDia(calendar1.time, calendar2.time))
    }

    @Test
    fun `calcularDiferencaEmDias deve calcular corretamente`() {
        val calendar1 = Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 15)
        }
        val calendar2 = Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 25)
        }
        
        val diferenca = DateUtils.calcularDiferencaEmDias(calendar1.time, calendar2.time)
        assertEquals("Diferença deve ser 10 dias", 10L, diferenca)
    }
}

