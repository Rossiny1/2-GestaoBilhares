package com.example.gestaobilhares.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sistema de logs jurídicos para garantir validade jurídica das assinaturas eletrônicas
 * Implementa requisitos da Lei 14.063/2020 para assinatura eletrônica simples
 */
@Singleton
class LegalLogger @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "LegalLogger"
        private const val LOG_DIR = "legal_logs"
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"
    }
    
    private val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale("pt", "BR"))
    
    /**
     * Registra evento de assinatura com metadados jurídicos obrigatórios
     */
    suspend fun logSignatureEvent(
        contratoId: Long,
        userId: String,
        action: String,
        metadata: SignatureMetadata
    ) = withContext(Dispatchers.IO) {
        try {
            val logEntry = createLogEntry(contratoId, userId, action, metadata)
            writeLogToFile(logEntry, contratoId)
            Log.d(TAG, "Log jurídico registrado: $action para contrato $contratoId")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao registrar log jurídico", e)
        }
    }
    
    /**
     * Cria entrada de log com formato jurídico
     */
    private fun createLogEntry(
        contratoId: Long,
        userId: String,
        action: String,
        metadata: SignatureMetadata
    ): String {
        val timestamp = dateFormatter.format(Date())
        
        return buildString {
            appendLine("=== LOG JURÍDICO - ASSINATURA ELETRÔNICA ===")
            appendLine("Data/Hora: $timestamp")
            appendLine("Contrato ID: $contratoId")
            appendLine("Usuário ID: $userId")
            appendLine("Ação: $action")
            appendLine("--- METADADOS JURÍDICOS ---")
            appendLine("Timestamp: ${metadata.timestamp}")
            appendLine("Device ID: ${metadata.deviceId}")
            appendLine("IP Address: ${metadata.ipAddress}")
            appendLine("Geolocalização: ${metadata.geolocation ?: "N/A"}")
            appendLine("Hash Documento: ${metadata.documentHash}")
            appendLine("Hash Assinatura: ${metadata.signatureHash}")
            appendLine("User Agent: ${metadata.userAgent}")
            appendLine("Resolução Tela: ${metadata.screenResolution}")
            appendLine("--- FIM METADADOS ---")
            appendLine("=== FIM LOG JURÍDICO ===")
            appendLine()
        }
    }
    
    /**
     * Escreve log em arquivo específico do contrato
     */
    private fun writeLogToFile(logEntry: String, contratoId: Long) {
        val logDir = File(context.filesDir, LOG_DIR)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        
        val logFile = File(logDir, "contrato_${contratoId}_legal.log")
        logFile.appendText(logEntry)
    }
    
    /**
     * Gera trilha de auditoria completa para um contrato
     */
    suspend fun generateAuditTrail(contratoId: Long): List<AuditEntry> = withContext(Dispatchers.IO) {
        try {
            val logFile = File(context.filesDir, "$LOG_DIR/contrato_${contratoId}_legal.log")
            if (!logFile.exists()) {
                return@withContext emptyList()
            }
            
            val logContent = logFile.readText()
            return@withContext parseLogContent(logContent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao gerar trilha de auditoria", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Parse do conteúdo do log para criar trilha de auditoria
     */
    private fun parseLogContent(logContent: String): List<AuditEntry> {
        val entries = mutableListOf<AuditEntry>()
        val logBlocks = logContent.split("=== LOG JURÍDICO - ASSINATURA ELETRÔNICA ===")
        
        logBlocks.forEach { block ->
            if (block.isNotBlank()) {
                val entry = parseLogBlock(block)
                if (entry != null) {
                    entries.add(entry)
                }
            }
        }
        
        return entries.sortedBy { it.timestamp }
    }
    
    /**
     * Parse de um bloco de log individual
     */
    private fun parseLogBlock(block: String): AuditEntry? {
        try {
            val lines = block.split("\n")
            var timestamp = ""
            var contratoId = ""
            var userId = ""
            var action = ""
            var documentHash = ""
            var signatureHash = ""
            
            lines.forEach { line ->
                when {
                    line.startsWith("Data/Hora:") -> timestamp = line.substringAfter("Data/Hora: ").trim()
                    line.startsWith("Contrato ID:") -> contratoId = line.substringAfter("Contrato ID: ").trim()
                    line.startsWith("Usuário ID:") -> userId = line.substringAfter("Usuário ID: ").trim()
                    line.startsWith("Ação:") -> action = line.substringAfter("Ação: ").trim()
                    line.startsWith("Hash Documento:") -> documentHash = line.substringAfter("Hash Documento: ").trim()
                    line.startsWith("Hash Assinatura:") -> signatureHash = line.substringAfter("Hash Assinatura: ").trim()
                }
            }
            
            return if (timestamp.isNotBlank() && contratoId.isNotBlank()) {
                AuditEntry(
                    timestamp = timestamp,
                    contratoId = contratoId.toLongOrNull() ?: 0L,
                    userId = userId,
                    action = action,
                    documentHash = documentHash,
                    signatureHash = signatureHash
                )
            } else null
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer parse do bloco de log", e)
            return null
        }
    }
    
    /**
     * Verifica integridade dos logs de um contrato
     */
    suspend fun verifyLogIntegrity(contratoId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val logFile = File(context.filesDir, "$LOG_DIR/contrato_${contratoId}_legal.log")
            if (!logFile.exists()) {
                return@withContext false
            }
            
            val logContent = logFile.readText()
            val entries = parseLogContent(logContent)
            
            // Verificar se há pelo menos um evento de assinatura
            val hasSignatureEvent = entries.any { it.action.contains("assinatura", ignoreCase = true) }
            
            // Verificar se todos os hashes estão presentes
            val hasValidHashes = entries.all { 
                it.documentHash.isNotBlank() && it.signatureHash.isNotBlank() 
            }
            
            return@withContext hasSignatureEvent && hasValidHashes
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar integridade dos logs", e)
            return@withContext false
        }
    }
}

/**
 * Metadados jurídicos obrigatórios para assinatura eletrônica simples
 * Conforme Lei 14.063/2020
 */
data class SignatureMetadata(
    val timestamp: Long,
    val deviceId: String,
    val ipAddress: String,
    val geolocation: String?,
    val documentHash: String,
    val signatureHash: String,
    val userAgent: String,
    val screenResolution: String
)

/**
 * Entrada de auditoria para trilha jurídica
 */
data class AuditEntry(
    val timestamp: String,
    val contratoId: Long,
    val userId: String,
    val action: String,
    val documentHash: String,
    val signatureHash: String
)
