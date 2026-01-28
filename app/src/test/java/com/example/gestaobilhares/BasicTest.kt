package com.example.gestaobilhares

import org.junit.Test
import org.junit.Assert.*

/**
 * Teste básico para verificar se a infraestrutura de testes funciona.
 */
class BasicTest {
    
    @Test
    fun `teste basico deve passar`() {
        // GIVEN: Valores simples
        val a = 2
        val b = 3
        
        // WHEN: Somar
        val result = a + b
        
        // THEN: Resultado deve ser 5
        assertEquals(5, result)
    }
}
