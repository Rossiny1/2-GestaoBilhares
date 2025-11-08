package com.example.gestaobilhares.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * ✅ FASE 12.2: Testes unitários para StringUtils
 * 
 * Testa operações com strings:
 * - Validações (vazia, tamanho)
 * - Formatação (capitalização, maiúsculas/minúsculas)
 * - Formatação específica (CPF, CNPJ, telefone, CEP)
 * - Limpeza (remover acentos, caracteres especiais)
 * - Conversão (para Double, Int, Long)
 * - Formatação de valores monetários
 */
class StringUtilsTest {

    // ==================== VALIDAÇÕES ====================

    @Test
    fun `isVazia deve retornar true para string vazia`() {
        assertTrue("String vazia deve retornar true", StringUtils.isVazia(""))
    }

    @Test
    fun `isVazia deve retornar true para string nula`() {
        assertTrue("String nula deve retornar true", StringUtils.isVazia(null))
    }

    @Test
    fun `isVazia deve retornar true para string com espaços`() {
        assertTrue("String com espaços deve retornar true", StringUtils.isVazia("   "))
    }

    @Test
    fun `isVazia deve retornar false para string preenchida`() {
        assertFalse("String preenchida deve retornar false", StringUtils.isVazia("teste"))
    }

    @Test
    fun `isNaoVazia deve retornar false para string vazia`() {
        assertFalse("String vazia deve retornar false", StringUtils.isNaoVazia(""))
    }

    @Test
    fun `isNaoVazia deve retornar true para string preenchida`() {
        assertTrue("String preenchida deve retornar true", StringUtils.isNaoVazia("teste"))
    }

    @Test
    fun `temTamanhoMinimo deve retornar true quando tamanho é suficiente`() {
        assertTrue("String com tamanho suficiente deve retornar true", 
            StringUtils.temTamanhoMinimo("teste", 3))
    }

    @Test
    fun `temTamanhoMinimo deve retornar false quando tamanho é insuficiente`() {
        assertFalse("String com tamanho insuficiente deve retornar false", 
            StringUtils.temTamanhoMinimo("te", 3))
    }

    // ==================== FORMATAÇÃO ====================

    @Test
    fun `capitalizar deve capitalizar primeira letra de cada palavra`() {
        val resultado = StringUtils.capitalizar("joão silva")
        assertEquals("João Silva", resultado)
    }

    @Test
    fun `capitalizar deve retornar vazio para string vazia`() {
        val resultado = StringUtils.capitalizar("")
        assertEquals("", resultado)
    }

    @Test
    fun `paraMaiusculas deve converter para maiúsculas`() {
        val resultado = StringUtils.paraMaiusculas("teste")
        assertEquals("TESTE", resultado)
    }

    @Test
    fun `paraMinusculas deve converter para minúsculas`() {
        val resultado = StringUtils.paraMinusculas("TESTE")
        assertEquals("teste", resultado)
    }

    @Test
    fun `removerAcentos deve remover acentos corretamente`() {
        val resultado = StringUtils.removerAcentos("João José")
        assertEquals("Joao Jose", resultado)
    }

    @Test
    fun `removerCaracteresEspeciais deve remover caracteres especiais`() {
        val resultado = StringUtils.removerCaracteresEspeciais("teste@123#")
        assertEquals("teste123", resultado)
    }

    @Test
    fun `removerEspacosExtras deve remover espaços extras`() {
        val resultado = StringUtils.removerEspacosExtras("teste    com    espaços")
        assertEquals("teste com espaços", resultado)
    }

    // ==================== FORMATAÇÃO ESPECÍFICA ====================

    @Test
    fun `formatarCPF deve formatar CPF corretamente`() {
        val resultado = StringUtils.formatarCPF("11144477735")
        assertEquals("111.444.777-35", resultado)
    }

    @Test
    fun `formatarCPF deve retornar original para CPF inválido`() {
        val resultado = StringUtils.formatarCPF("123")
        assertEquals("123", resultado)
    }

    @Test
    fun `formatarCNPJ deve formatar CNPJ corretamente`() {
        val resultado = StringUtils.formatarCNPJ("11222333000181")
        assertEquals("11.222.333/0001-81", resultado)
    }

    @Test
    fun `formatarCNPJ deve retornar original para CNPJ inválido`() {
        val resultado = StringUtils.formatarCNPJ("123")
        assertEquals("123", resultado)
    }

    @Test
    fun `formatarTelefone deve formatar telefone de 10 dígitos`() {
        val resultado = StringUtils.formatarTelefone("1198765432")
        assertEquals("(11) 9876-5432", resultado)
    }

    @Test
    fun `formatarTelefone deve formatar telefone de 11 dígitos`() {
        val resultado = StringUtils.formatarTelefone("11987654321")
        assertEquals("(11) 98765-4321", resultado)
    }

    @Test
    fun `formatarCEP deve formatar CEP corretamente`() {
        val resultado = StringUtils.formatarCEP("12345678")
        assertEquals("12345-678", resultado)
    }

    @Test
    fun `formatarCEP deve retornar original para CEP inválido`() {
        val resultado = StringUtils.formatarCEP("123")
        assertEquals("123", resultado)
    }

    // ==================== LIMPEZA ====================

    @Test
    fun `removerNumeros deve remover números`() {
        val resultado = StringUtils.removerNumeros("teste123")
        assertEquals("teste", resultado)
    }

    @Test
    fun `removerLetras deve remover letras`() {
        val resultado = StringUtils.removerLetras("teste123")
        assertEquals("123", resultado)
    }

    @Test
    fun `manterApenasNumeros deve manter apenas números`() {
        val resultado = StringUtils.manterApenasNumeros("teste123@#")
        assertEquals("123", resultado)
    }

    @Test
    fun `manterApenasAlfanumericos deve manter apenas letras e números`() {
        val resultado = StringUtils.manterApenasAlfanumericos("teste123@#")
        assertEquals("teste123", resultado)
    }

    // ==================== TRUNCAMENTO ====================

    @Test
    fun `truncar deve truncar string longa`() {
        val resultado = StringUtils.truncar("teste muito longo", 10)
        assertEquals("teste m...", resultado)
    }

    @Test
    fun `truncar deve retornar original para string curta`() {
        val resultado = StringUtils.truncar("teste", 10)
        assertEquals("teste", resultado)
    }

    @Test
    fun `truncarNoMeio deve truncar no meio`() {
        val resultado = StringUtils.truncarNoMeio("teste muito longo", 10)
        assertTrue("Deve conter reticências no meio", resultado.contains("..."))
    }

    // ==================== CONVERSÃO ====================

    @Test
    fun `paraDouble deve converter string para Double`() {
        val resultado = StringUtils.paraDouble("123.45")
        assertEquals(123.45, resultado, 0.01)
    }

    @Test
    fun `paraDouble deve converter string com vírgula`() {
        val resultado = StringUtils.paraDouble("123,45")
        assertEquals(123.45, resultado, 0.01)
    }

    @Test
    fun `paraDouble deve retornar valor padrão para string inválida`() {
        val resultado = StringUtils.paraDouble("inválido", 99.0)
        assertEquals(99.0, resultado, 0.01)
    }

    @Test
    fun `paraInt deve converter string para Int`() {
        val resultado = StringUtils.paraInt("123")
        assertEquals(123, resultado)
    }

    @Test
    fun `paraInt deve retornar valor padrão para string inválida`() {
        val resultado = StringUtils.paraInt("inválido", 99)
        assertEquals(99, resultado)
    }

    @Test
    fun `paraLong deve converter string para Long`() {
        val resultado = StringUtils.paraLong("123456789")
        assertEquals(123456789L, resultado)
    }

    @Test
    fun `paraLong deve retornar valor padrão para string inválida`() {
        val resultado = StringUtils.paraLong("inválido", 99L)
        assertEquals(99L, resultado)
    }

    // ==================== FORMATAÇÃO DE VALORES ====================

    @Test
    fun `formatarMoeda deve formatar valor monetário`() {
        val resultado = StringUtils.formatarMoeda(123.45)
        assertEquals("R$ 123,45", resultado)
    }

    @Test
    fun `formatarValor deve formatar valor sem símbolo`() {
        val resultado = StringUtils.formatarValor(123.45)
        assertEquals("123,45", resultado)
    }

    @Test
    fun `formatarNumero deve formatar número com separadores`() {
        val resultado = StringUtils.formatarNumero(1234567L)
        assertEquals("1.234.567", resultado)
    }
}

