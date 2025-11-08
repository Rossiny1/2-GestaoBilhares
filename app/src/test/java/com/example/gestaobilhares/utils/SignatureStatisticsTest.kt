package com.example.gestaobilhares.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * ✅ FASE 12.2: Testes unitários para SignatureStatistics
 * 
 * Testa validação de assinaturas e geração de estatísticas:
 * - Validação de assinaturas válidas/inválidas
 * - Geração de resumo de estatísticas
 * - Critérios de validação (pontos, duração, pressão)
 */
class SignatureStatisticsTest {

    @Test
    fun `isValidSignature deve retornar true para assinatura válida`() {
        val stats = SignatureStatistics(
            totalPoints = 10,
            duration = 1000L,
            averagePressure = 0.5f,
            averageVelocity = 5.0f,
            startTime = 1000L
        )
        
        assertTrue("Assinatura válida deve retornar true", stats.isValidSignature())
    }

    @Test
    fun `isValidSignature deve retornar false para poucos pontos`() {
        val stats = SignatureStatistics(
            totalPoints = 2, // Menos que o mínimo de 3
            duration = 1000L,
            averagePressure = 0.5f,
            averageVelocity = 5.0f,
            startTime = 1000L
        )
        
        assertFalse("Assinatura com poucos pontos deve retornar false", stats.isValidSignature())
    }

    @Test
    fun `isValidSignature deve retornar false para duração muito curta`() {
        val stats = SignatureStatistics(
            totalPoints = 10,
            duration = 100L, // Menos que o mínimo de 200ms
            averagePressure = 0.5f,
            averageVelocity = 5.0f,
            startTime = 1000L
        )
        
        assertFalse("Assinatura com duração muito curta deve retornar false", stats.isValidSignature())
    }

    @Test
    fun `isValidSignature deve retornar false para duração muito longa`() {
        val stats = SignatureStatistics(
            totalPoints = 10,
            duration = 70000L, // Mais que o máximo de 60000ms
            averagePressure = 0.5f,
            averageVelocity = 5.0f,
            startTime = 1000L
        )
        
        assertFalse("Assinatura com duração muito longa deve retornar false", stats.isValidSignature())
    }

    @Test
    fun `isValidSignature deve retornar false para pressão muito baixa`() {
        val stats = SignatureStatistics(
            totalPoints = 10,
            duration = 1000L,
            averagePressure = 0.00001f, // Menos que o mínimo de 0.0001f
            averageVelocity = 5.0f,
            startTime = 1000L
        )
        
        assertFalse("Assinatura com pressão muito baixa deve retornar false", stats.isValidSignature())
    }

    @Test
    fun `isValidSignature deve retornar false para pressão muito alta`() {
        val stats = SignatureStatistics(
            totalPoints = 10,
            duration = 1000L,
            averagePressure = 1.5f, // Mais que o máximo de 1.0f
            averageVelocity = 5.0f,
            startTime = 1000L
        )
        
        assertFalse("Assinatura com pressão muito alta deve retornar false", stats.isValidSignature())
    }

    @Test
    fun `isValidSignature deve retornar true para assinatura no limite mínimo`() {
        val stats = SignatureStatistics(
            totalPoints = 3, // Mínimo permitido
            duration = 200L, // Mínimo permitido
            averagePressure = 0.0001f, // Mínimo permitido
            averageVelocity = 5.0f,
            startTime = 1000L
        )
        
        assertTrue("Assinatura no limite mínimo deve ser válida", stats.isValidSignature())
    }

    @Test
    fun `isValidSignature deve retornar true para assinatura no limite máximo`() {
        val stats = SignatureStatistics(
            totalPoints = 1000,
            duration = 60000L, // Máximo permitido
            averagePressure = 1.0f, // Máximo permitido
            averageVelocity = 5.0f,
            startTime = 1000L
        )
        
        assertTrue("Assinatura no limite máximo deve ser válida", stats.isValidSignature())
    }

    @Test
    fun `generateSummary deve gerar resumo completo`() {
        val stats = SignatureStatistics(
            totalPoints = 10,
            duration = 1000L,
            averagePressure = 0.5f,
            averageVelocity = 5.0f,
            startTime = 1000L
        )
        
        val summary = stats.generateSummary()
        
        assertNotNull("Resumo não deve ser nulo", summary)
        assertTrue("Resumo deve conter total de pontos", summary.contains("Total de Pontos: 10"))
        assertTrue("Resumo deve conter duração", summary.contains("Duração: 1000ms"))
        assertTrue("Resumo deve conter pressão média", summary.contains("Pressão Média"))
        assertTrue("Resumo deve conter velocidade média", summary.contains("Velocidade Média"))
        assertTrue("Resumo deve conter tempo de início", summary.contains("Tempo de Início: 1000"))
        assertTrue("Resumo deve conter status de validação", summary.contains("Assinatura Válida"))
    }

    @Test
    fun `generateSummary deve indicar assinatura inválida quando apropriado`() {
        val stats = SignatureStatistics(
            totalPoints = 2, // Inválido
            duration = 1000L,
            averagePressure = 0.5f,
            averageVelocity = 5.0f,
            startTime = 1000L
        )
        
        val summary = stats.generateSummary()
        
        assertTrue("Resumo deve indicar assinatura inválida", summary.contains("Assinatura Válida: false"))
    }

    @Test
    fun `generateSummary deve indicar assinatura válida quando apropriado`() {
        val stats = SignatureStatistics(
            totalPoints = 10,
            duration = 1000L,
            averagePressure = 0.5f,
            averageVelocity = 5.0f,
            startTime = 1000L
        )
        
        val summary = stats.generateSummary()
        
        assertTrue("Resumo deve indicar assinatura válida", summary.contains("Assinatura Válida: true"))
    }
}

