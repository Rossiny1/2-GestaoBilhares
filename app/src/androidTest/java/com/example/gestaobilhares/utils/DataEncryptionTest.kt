package com.example.gestaobilhares.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ✅ FASE 12.2: Testes instrumentados para DataEncryption
 * 
 * Testa funcionalidades críticas de criptografia:
 * - Criptografia de dados sensíveis
 * - Descriptografia de dados criptografados
 * - Integridade dos dados
 * 
 * Nota: Testes instrumentados são necessários porque DataEncryption usa Android Keystore
 */
@RunWith(AndroidJUnit4::class)
class DataEncryptionTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        // Limpar qualquer chave existente antes dos testes (se necessário)
        // Nota: Em produção, não devemos limpar chaves do Keystore
    }

    @Test
    fun `encrypt deve criptografar string válida`() {
        val plaintext = "12345678901" // CPF de exemplo
        val encrypted = DataEncryption.encrypt(plaintext)
        
        assertNotNull("Dados criptografados não devem ser nulos", encrypted)
        assertNotEquals("Dados criptografados devem ser diferentes do texto original", plaintext, encrypted)
        assertTrue("Dados criptografados devem ser diferentes do texto original", encrypted != plaintext)
    }

    @Test
    fun `decrypt deve descriptografar dados criptografados corretamente`() {
        val plaintext = "12345678901" // CPF de exemplo
        val encrypted = DataEncryption.encrypt(plaintext)
        
        assertNotNull("Criptografia deve retornar valor não nulo", encrypted)
        
        val decrypted = DataEncryption.decrypt(encrypted)
        
        assertNotNull("Descriptografia deve retornar valor não nulo", decrypted)
        assertEquals("Texto descriptografado deve ser igual ao original", plaintext, decrypted)
    }

    @Test
    fun `encrypt e decrypt devem funcionar com strings vazias`() {
        val plaintext = ""
        val encrypted = DataEncryption.encrypt(plaintext)
        
        assertNotNull("String vazia deve ser criptografada", encrypted)
        
        val decrypted = DataEncryption.decrypt(encrypted)
        assertEquals("String vazia descriptografada deve ser igual ao original", plaintext, decrypted)
    }

    @Test
    fun `encrypt e decrypt devem funcionar com strings longas`() {
        val plaintext = "A".repeat(1000) // String longa (ex: assinatura Base64)
        val encrypted = DataEncryption.encrypt(plaintext)
        
        assertNotNull("String longa deve ser criptografada", encrypted)
        
        val decrypted = DataEncryption.decrypt(encrypted)
        assertEquals("String longa descriptografada deve ser igual ao original", plaintext, decrypted)
    }

    @Test
    fun `decrypt deve retornar null para dados inválidos`() {
        val invalidData = "dados_invalidos_nao_criptografados"
        val decrypted = DataEncryption.decrypt(invalidData)
        
        assertNull("Dados inválidos devem retornar null", decrypted)
    }

    @Test
    fun `decrypt deve retornar null para string vazia`() {
        val decrypted = DataEncryption.decrypt("")
        assertNull("String vazia deve retornar null", decrypted)
    }

    @Test
    fun `decrypt deve retornar null para null`() {
        val decrypted = DataEncryption.decrypt(null)
        assertNull("Null deve retornar null", decrypted)
    }

    @Test
    fun `encrypt deve retornar null para null`() {
        val encrypted = DataEncryption.encrypt(null)
        assertNull("Null deve retornar null", encrypted)
    }

    @Test
    fun `múltiplas criptografias da mesma string devem produzir resultados diferentes`() {
        val plaintext = "12345678901"
        val encrypted1 = DataEncryption.encrypt(plaintext)
        val encrypted2 = DataEncryption.encrypt(plaintext)
        
        // Devido ao IV aleatório, cada criptografia deve produzir resultado diferente
        assertNotEquals("Criptografias diferentes devem produzir resultados diferentes", encrypted1, encrypted2)
        
        // Mas ambas devem descriptografar para o mesmo valor
        assertEquals("Ambas devem descriptografar para o mesmo valor", plaintext, DataEncryption.decrypt(encrypted1))
        assertEquals("Ambas devem descriptografar para o mesmo valor", plaintext, DataEncryption.decrypt(encrypted2))
    }

    @Test
    fun `criptografia deve funcionar com caracteres especiais`() {
        val plaintext = "CPF: 123.456.789-01 / CNPJ: 12.345.678/0001-90"
        val encrypted = DataEncryption.encrypt(plaintext)
        
        assertNotNull("Caracteres especiais devem ser criptografados", encrypted)
        
        val decrypted = DataEncryption.decrypt(encrypted)
        assertEquals("Caracteres especiais devem ser preservados", plaintext, decrypted)
    }
}

