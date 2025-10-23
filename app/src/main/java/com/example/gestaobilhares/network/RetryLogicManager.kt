package com.example.gestaobilhares.network

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * ✅ FASE 4D: Gerenciador de Lógica de Retry
 * Seguindo Android 2025 best practices para otimização de rede
 * 
 * Funcionalidades:
 * - Retry automático com backoff exponencial
 * - Circuit breaker pattern
 * - Rate limiting
 * - Estatísticas de retry
 */
class RetryLogicManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: RetryLogicManager? = null

        fun getInstance(): RetryLogicManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RetryLogicManager().also { INSTANCE = it }
            }
        }

        private const val TAG = "RetryLogicManager"
        private const val DEFAULT_MAX_RETRIES = 3
        private const val DEFAULT_BASE_DELAY = 1000L // 1 segundo
        private const val DEFAULT_MAX_DELAY = 30000L // 30 segundos
        private const val DEFAULT_CIRCUIT_BREAKER_THRESHOLD = 5
        private const val DEFAULT_CIRCUIT_BREAKER_TIMEOUT = 60000L // 1 minuto
    }

    // Circuit breaker para cada endpoint
    private val circuitBreakers = ConcurrentHashMap<String, CircuitBreaker>()
    
    // Rate limiting
    private val rateLimiters = ConcurrentHashMap<String, RateLimiter>()
    
    // Estatísticas
    private val totalRetries = AtomicInteger(0)
    private val successfulRetries = AtomicInteger(0)
    private val failedRetries = AtomicInteger(0)
    private val totalRetryTime = AtomicLong(0)

    /**
     * Executa operação com retry automático
     */
    suspend fun <T> executeWithRetry(
        operation: suspend () -> T,
        endpoint: String = "default",
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        baseDelay: Long = DEFAULT_BASE_DELAY,
        maxDelay: Long = DEFAULT_MAX_DELAY,
        backoffMultiplier: Double = 2.0,
        jitter: Boolean = true
    ): Result<T> {
        return try {
            // Verificar circuit breaker
            if (!isCircuitBreakerOpen(endpoint)) {
                executeWithRateLimit(operation, endpoint, maxRetries, baseDelay, maxDelay, backoffMultiplier, jitter)
            } else {
                Log.w(TAG, "Circuit breaker aberto para endpoint: $endpoint")
                Result.failure(Exception("Circuit breaker aberto para $endpoint"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro na execução com retry: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Executa operação com rate limiting
     */
    private suspend fun <T> executeWithRateLimit(
        operation: suspend () -> T,
        endpoint: String,
        maxRetries: Int,
        baseDelay: Long,
        maxDelay: Long,
        backoffMultiplier: Double,
        jitter: Boolean
    ): Result<T> {
        val rateLimiter = getRateLimiter(endpoint)
        
        return if (rateLimiter.canExecute()) {
            executeWithRetryInternal(operation, endpoint, maxRetries, baseDelay, maxDelay, backoffMultiplier, jitter)
        } else {
            Log.w(TAG, "Rate limit atingido para endpoint: $endpoint")
            Result.failure(Exception("Rate limit atingido para $endpoint"))
        }
    }

    /**
     * Executa operação com retry interno
     */
    private suspend fun <T> executeWithRetryInternal(
        operation: suspend () -> T,
        endpoint: String,
        maxRetries: Int,
        baseDelay: Long,
        maxDelay: Long,
        backoffMultiplier: Double,
        jitter: Boolean
    ): Result<T> {
        var lastException: Exception? = null
        val startTime = System.currentTimeMillis()
        
        for (attempt in 0..maxRetries) {
            try {
                val result = operation()
                
                if (attempt > 0) {
                    successfulRetries.incrementAndGet()
                    val retryTime = System.currentTimeMillis() - startTime
                    totalRetryTime.addAndGet(retryTime)
                    Log.d(TAG, "Operação bem-sucedida após $attempt tentativas em ${retryTime}ms")
                }
                
                // Reset circuit breaker em caso de sucesso
                resetCircuitBreaker(endpoint)
                
                return Result.success(result)
                
            } catch (e: Exception) {
                lastException = e
                totalRetries.incrementAndGet()
                
                Log.w(TAG, "Tentativa $attempt falhou para $endpoint: ${e.message}")
                
                if (attempt < maxRetries) {
                    val delay = calculateDelay(attempt, baseDelay, maxDelay, backoffMultiplier, jitter)
                    Log.d(TAG, "Aguardando ${delay}ms antes da próxima tentativa")
                    delay(delay)
                } else {
                    failedRetries.incrementAndGet()
                    recordCircuitBreakerFailure(endpoint)
                }
            }
        }
        
        return Result.failure(lastException ?: Exception("Máximo de tentativas excedido"))
    }

    /**
     * Calcula delay para retry com backoff exponencial
     */
    private fun calculateDelay(
        attempt: Int,
        baseDelay: Long,
        maxDelay: Long,
        backoffMultiplier: Double,
        jitter: Boolean
    ): Long {
        val exponentialDelay = (baseDelay * Math.pow(backoffMultiplier, attempt.toDouble())).toLong()
        val cappedDelay = minOf(exponentialDelay, maxDelay)
        
        return if (jitter) {
            // Adicionar jitter para evitar thundering herd
            val jitterAmount = (cappedDelay * 0.1).toLong()
            cappedDelay + (Math.random() * jitterAmount).toLong()
        } else {
            cappedDelay
        }
    }

    /**
     * Verifica se circuit breaker está aberto
     */
    private fun isCircuitBreakerOpen(endpoint: String): Boolean {
        val circuitBreaker = circuitBreakers[endpoint]
        return circuitBreaker?.isOpen() == true
    }

    /**
     * Registra falha no circuit breaker
     */
    private fun recordCircuitBreakerFailure(endpoint: String) {
        val circuitBreaker = circuitBreakers.getOrPut(endpoint) {
            CircuitBreaker(DEFAULT_CIRCUIT_BREAKER_THRESHOLD, DEFAULT_CIRCUIT_BREAKER_TIMEOUT)
        }
        circuitBreaker.recordFailure()
    }

    /**
     * Reseta circuit breaker
     */
    private fun resetCircuitBreaker(endpoint: String) {
        circuitBreakers[endpoint]?.reset()
    }

    /**
     * Obtém rate limiter para endpoint
     */
    private fun getRateLimiter(endpoint: String): RateLimiter {
        return rateLimiters.getOrPut(endpoint) {
            RateLimiter(10, 60000) // 10 requisições por minuto
        }
    }

    /**
     * Configura circuit breaker para endpoint
     */
    fun configureCircuitBreaker(
        endpoint: String,
        failureThreshold: Int = DEFAULT_CIRCUIT_BREAKER_THRESHOLD,
        timeout: Long = DEFAULT_CIRCUIT_BREAKER_TIMEOUT
    ) {
        circuitBreakers[endpoint] = CircuitBreaker(failureThreshold, timeout)
        Log.d(TAG, "Circuit breaker configurado para $endpoint: threshold=$failureThreshold, timeout=${timeout}ms")
    }

    /**
     * Configura rate limiter para endpoint
     */
    fun configureRateLimiter(
        endpoint: String,
        maxRequests: Int,
        timeWindow: Long
    ) {
        rateLimiters[endpoint] = RateLimiter(maxRequests, timeWindow)
        Log.d(TAG, "Rate limiter configurado para $endpoint: $maxRequests requests em ${timeWindow}ms")
    }

    /**
     * Obtém estatísticas de retry
     */
    fun getRetryStats(): RetryStats {
        return RetryStats(
            totalRetries = totalRetries.get(),
            successfulRetries = successfulRetries.get(),
            failedRetries = failedRetries.get(),
            successRate = if (totalRetries.get() > 0) {
                successfulRetries.get().toDouble() / totalRetries.get() * 100
            } else 0.0,
            averageRetryTime = if (successfulRetries.get() > 0) {
                totalRetryTime.get().toDouble() / successfulRetries.get()
            } else 0.0,
            circuitBreakerStates = circuitBreakers.mapValues { it.value.getState() },
            rateLimiterStates = rateLimiters.mapValues { it.value.getState() }
        )
    }

    /**
     * Limpa estatísticas
     */
    fun clearStats() {
        totalRetries.set(0)
        successfulRetries.set(0)
        failedRetries.set(0)
        totalRetryTime.set(0)
        Log.d(TAG, "Estatísticas de retry limpas")
    }

    /**
     * Circuit Breaker implementation
     */
    private class CircuitBreaker(
        private val failureThreshold: Int,
        private val timeout: Long
    ) {
        private var failureCount = 0
        private var lastFailureTime = 0L
        private var state = CircuitBreakerState.CLOSED

        fun isOpen(): Boolean {
            return when (state) {
                CircuitBreakerState.OPEN -> {
                    if (System.currentTimeMillis() - lastFailureTime > timeout) {
                        state = CircuitBreakerState.HALF_OPEN
                        false
                    } else {
                        true
                    }
                }
                CircuitBreakerState.HALF_OPEN -> false
                CircuitBreakerState.CLOSED -> false
            }
        }

        fun recordFailure() {
            failureCount++
            lastFailureTime = System.currentTimeMillis()
            
            if (failureCount >= failureThreshold) {
                state = CircuitBreakerState.OPEN
            }
        }

        fun reset() {
            failureCount = 0
            state = CircuitBreakerState.CLOSED
        }

        fun getState(): CircuitBreakerState = state
    }

    /**
     * Rate Limiter implementation
     */
    private class RateLimiter(
        private val maxRequests: Int,
        private val timeWindow: Long
    ) {
        private val requests = mutableListOf<Long>()

        fun canExecute(): Boolean {
            val now = System.currentTimeMillis()
            
            // Remove requests antigas
            requests.removeAll { it < now - timeWindow }
            
            return if (requests.size < maxRequests) {
                requests.add(now)
                true
            } else {
                false
            }
        }

        fun getState(): RateLimiterState {
            val now = System.currentTimeMillis()
            requests.removeAll { it < now - timeWindow }
            
            return RateLimiterState(
                currentRequests = requests.size,
                maxRequests = maxRequests,
                timeWindow = timeWindow
            )
        }
    }

    /**
     * Enums e data classes
     */
    enum class CircuitBreakerState { CLOSED, OPEN, HALF_OPEN }

    data class RetryStats(
        val totalRetries: Int,
        val successfulRetries: Int,
        val failedRetries: Int,
        val successRate: Double,
        val averageRetryTime: Double,
        val circuitBreakerStates: Map<String, CircuitBreakerState>,
        val rateLimiterStates: Map<String, RateLimiterState>
    )

    data class RateLimiterState(
        val currentRequests: Int,
        val maxRequests: Int,
        val timeWindow: Long
    )
}
