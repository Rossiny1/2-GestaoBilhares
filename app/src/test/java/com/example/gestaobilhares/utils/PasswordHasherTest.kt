package com.example.gestaobilhares.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * ✅ FASE 12.2: Testes unitários para PasswordHasher
 * 
 * Testa funcionalidades críticas de segurança:
 * - Geração de hash seguro
 * - Validação de senhas
 * - Formato de hash
 */
class PasswordHasherTest {

    @Test
    fun `hashPassword deve gerar hash válido`() {
        val password = "senha123"
        val hash = PasswordHasher.hashPassword(password)
        
        assertNotNull("Hash não deve ser nulo", hash)
        assertTrue("Hash deve conter dois pontos (salt:hash)", hash.contains(":"))
        assertTrue("Hash deve ter formato válido", PasswordHasher.isValidHashFormat(hash))
    }

    @Test
    fun `hashPassword deve gerar hash diferente para mesma senha`() {
        val password = "senha123"
        val hash1 = PasswordHasher.hashPassword(password)
        val hash2 = PasswordHasher.hashPassword(password)
        
        // Hashes devem ser diferentes devido ao salt aleatório
        assertNotEquals("Hashes devem ser diferentes (salt aleatório)", hash1, hash2)
    }

    @Test
    fun `verifyPassword deve retornar true para senha correta`() {
        val password = "senha123"
        val hash = PasswordHasher.hashPassword(password)
        
        assertTrue("Senha correta deve ser validada", PasswordHasher.verifyPassword(password, hash))
    }

    @Test
    fun `verifyPassword deve retornar false para senha incorreta`() {
        val password = "senha123"
        val wrongPassword = "senha456"
        val hash = PasswordHasher.hashPassword(password)
        
        assertFalse("Senha incorreta não deve ser validada", PasswordHasher.verifyPassword(wrongPassword, hash))
    }

    @Test
    fun `verifyPassword deve retornar false para hash nulo`() {
        assertFalse("Hash nulo não deve ser validado", PasswordHasher.verifyPassword("senha123", null))
    }

    @Test
    fun `verifyPassword deve retornar false para senha vazia`() {
        val hash = PasswordHasher.hashPassword("senha123")
        assertFalse("Senha vazia não deve ser validada", PasswordHasher.verifyPassword("", hash))
    }

    @Test
    fun `isValidHashFormat deve retornar true para hash válido`() {
        val password = "senha123"
        val hash = PasswordHasher.hashPassword(password)
        
        assertTrue("Hash válido deve ser reconhecido", PasswordHasher.isValidHashFormat(hash))
    }

    @Test
    fun `isValidHashFormat deve retornar false para hash inválido`() {
        assertFalse("Hash nulo não é válido", PasswordHasher.isValidHashFormat(null))
        assertFalse("String vazia não é hash válido", PasswordHasher.isValidHashFormat(""))
        assertFalse("Hash sem dois pontos não é válido", PasswordHasher.isValidHashFormat("hashsemdoispontos"))
        assertFalse("Hash com formato incorreto não é válido", PasswordHasher.isValidHashFormat("hash:invalido:com:mais:dois:pontos"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `hashPassword deve lançar exceção para senha vazia`() {
        PasswordHasher.hashPassword("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `hashPassword deve lançar exceção para senha em branco`() {
        PasswordHasher.hashPassword("   ")
    }
}

