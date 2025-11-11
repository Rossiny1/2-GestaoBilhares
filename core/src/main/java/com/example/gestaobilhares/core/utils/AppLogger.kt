package com.example.gestaobilhares.core.utils

import android.icu.text.SimpleDateFormat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * ✅ FASE 12.4: Sistema de Logging Condicional e Seguro
 * 
 * Características:
 * - Logs desabilitados em produção (RELEASE)
 * - Níveis de log apropriados (DEBUG, INFO, WARN, ERROR)
 * - Sanitização automática de dados sensíveis (CPF, senhas, etc.)
 * - Performance otimizada (sem overhead em produção)
 */
object AppLogger {

    private val _logs = MutableLiveData<MutableList<String>>(mutableListOf())
    val logs: LiveData<MutableList<String>> = _logs

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    // ✅ FASE 12.4: Habilitar logs apenas em DEBUG (usando isLoggable)
    private val isLoggingEnabled: Boolean
        get() = android.util.Log.isLoggable("AppLogger", android.util.Log.DEBUG)
    
    // ✅ FASE 12.4: Padrões para detectar dados sensíveis
    private val sensitivePatterns = listOf(
        Pattern.compile("(?i)(cpf|cnpj|senha|password|token|key|secret|assinatura|hash)\\s*[:=]\\s*([\\d\\w\\-]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b"), // CPF
        Pattern.compile("\\b\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}\\b"), // CNPJ
        Pattern.compile("(?i)(password|senha|pwd)\\s*[:=]\\s*[^\\s]+", Pattern.CASE_INSENSITIVE)
    )

    /**
     * ✅ FASE 12.4: Log de nível DEBUG (apenas em modo DEBUG)
     */
    fun d(tag: String, message: String) {
        if (isLoggingEnabled) {
            val sanitized = sanitizeSensitiveData(message)
            android.util.Log.d(tag, sanitized)
            addToLogs(tag, "DEBUG", sanitized)
        }
    }

    /**
     * ✅ FASE 12.4: Log de nível INFO (apenas em modo DEBUG)
     */
    fun i(tag: String, message: String) {
        if (isLoggingEnabled) {
            val sanitized = sanitizeSensitiveData(message)
            android.util.Log.i(tag, sanitized)
            addToLogs(tag, "INFO", sanitized)
        }
    }

    /**
     * ✅ FASE 12.4: Log de nível WARN (sempre habilitado para erros importantes)
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val sanitized = sanitizeSensitiveData(message)
        if (isLoggingEnabled) {
            if (throwable != null) {
                android.util.Log.w(tag, sanitized, throwable)
            } else {
                android.util.Log.w(tag, sanitized)
            }
            addToLogs(tag, "WARN", sanitized)
        } else {
            // Em produção, apenas logar erros críticos sem dados sensíveis
            android.util.Log.w(tag, "[PRODUCTION] $sanitized")
        }
    }

    /**
     * ✅ FASE 12.4: Log de nível ERROR (sempre habilitado)
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val sanitized = sanitizeSensitiveData(message)
        if (throwable != null) {
            android.util.Log.e(tag, sanitized, throwable)
        } else {
            android.util.Log.e(tag, sanitized)
        }
        if (isLoggingEnabled) {
            addToLogs(tag, "ERROR", sanitized)
        }
    }

    /**
     * ✅ FASE 12.4: Método legado mantido para compatibilidade (usa DEBUG)
     */
    fun log(tag: String, message: String) {
        d(tag, message)
    }

    /**
     * ✅ FASE 12.4: Sanitiza dados sensíveis das mensagens de log
     */
    private fun sanitizeSensitiveData(message: String): String {
        if (!isLoggingEnabled) {
            // Em produção, sanitizar sempre
            return sanitizeMessage(message)
        }
        
        // Em DEBUG, verificar se deve sanitizar
        // Por padrão, sanitizar em DEBUG também para segurança
        return sanitizeMessage(message)
    }

    /**
     * ✅ FASE 12.4: Remove ou mascara dados sensíveis
     */
    private fun sanitizeMessage(message: String): String {
        var sanitized = message
        
        // Remover CPF
        sanitized = sanitized.replace(Regex("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b"), "***.***.***-**")
        
        // Remover CNPJ
        sanitized = sanitized.replace(Regex("\\b\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}\\b"), "**.***.***/****-**")
        
        // Remover senhas/passwords
        sanitized = sanitized.replace(Regex("(?i)(password|senha|pwd)\\s*[:=]\\s*[^\\s]+", RegexOption.IGNORE_CASE), "$1: [REDACTED]")
        
        // Remover tokens/chaves
        sanitized = sanitized.replace(Regex("(?i)(token|key|secret|api[_-]?key)\\s*[:=]\\s*[^\\s]+", RegexOption.IGNORE_CASE), "$1: [REDACTED]")
        
        // Remover assinaturas base64 (muito longas)
        sanitized = sanitized.replace(Regex("data:image/[^;]+;base64,[A-Za-z0-9+/=]{50,}"), "data:image/[REDACTED]")
        
        // Remover hashes longos (manter apenas primeiros caracteres)
        sanitized = sanitized.replace(Regex("([A-Za-z0-9+/=]{32,})")) { matchResult ->
            val hash = matchResult.value
            if (hash.length > 20) {
                "${hash.take(8)}...[REDACTED]"
            } else {
                hash
            }
        }
        
        return sanitized
    }

    /**
     * ✅ FASE 12.4: Adiciona log à lista interna (apenas em DEBUG)
     */
    private fun addToLogs(tag: String, level: String, message: String) {
        if (!isLoggingEnabled) return
        
        val timestamp = dateFormat.format(Date())
        val logMessage = "$timestamp [$level] [$tag] $message"
        
        val currentLogs = _logs.value ?: mutableListOf()
        currentLogs.add(0, logMessage) // Adiciona no topo
        if (currentLogs.size > 1000) {
            // Limitar a 1000 logs para não consumir muita memória
            currentLogs.removeAt(currentLogs.size - 1)
        }
        _logs.postValue(currentLogs)
    }

    fun clearLogs() {
        _logs.postValue(mutableListOf())
    }
}

