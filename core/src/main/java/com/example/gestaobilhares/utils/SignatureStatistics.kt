package com.example.gestaobilhares.core.utils

/**
 * Estatísticas da assinatura para análise de autenticidade
 * Usado para validação jurídica de assinaturas eletrônicas simples
 */
data class SignatureStatistics(
    val totalPoints: Int,
    val duration: Long,
    val averagePressure: Float,
    val averageVelocity: Float,
    val startTime: Long
) {
    /**
     * Verifica se a assinatura tem características válidas
     * Critérios ajustados para assinaturas reais (rubricas e nomes completos)
     */
    fun isValidSignature(): Boolean {
        return totalPoints >= 3 && // Mínimo de 3 pontos (ajustado para rubricas simples)
                duration >= 200 && // Mínimo de 0.2 segundos (ajustado para assinaturas muito rápidas)
                duration <= 60000 && // Máximo de 60 segundos
                averagePressure >= 0.0001f && // Pressão mínima ultra reduzida para compatibilidade com canetas
                averagePressure <= 1.0f // Pressão máxima
    }
    
    /**
     * Gera resumo das estatísticas para logs
     */
    fun generateSummary(): String {
        return buildString {
            appendLine("=== ESTATÍSTICAS DA ASSINATURA ===")
            appendLine("Total de Pontos: $totalPoints")
            appendLine("Duração: ${duration}ms")
            appendLine("Pressão Média: ${String.format("%.3f", averagePressure)}")
            appendLine("Velocidade Média: ${String.format("%.3f", averageVelocity)}")
            appendLine("Tempo de Início: $startTime")
            appendLine("Assinatura Válida: ${isValidSignature()}")
            appendLine("=== FIM ESTATÍSTICAS ===")
        }
    }
}
