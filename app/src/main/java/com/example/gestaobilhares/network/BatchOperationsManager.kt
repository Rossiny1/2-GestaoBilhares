package com.example.gestaobilhares.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * ✅ FASE 4D: Gerenciador de Operações em Lote
 * Seguindo Android 2025 best practices para otimização de rede
 * 
 * Funcionalidades:
 * - Agrupamento de operações para reduzir requisições
 * - Processamento em lote inteligente
 * - Retry automático para operações falhadas
 * - Estatísticas de performance
 */
class BatchOperationsManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: BatchOperationsManager? = null

        fun getInstance(): BatchOperationsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BatchOperationsManager().also { INSTANCE = it }
            }
        }

        private const val TAG = "BatchOperationsManager"
        private const val DEFAULT_BATCH_SIZE = 10
        private const val DEFAULT_BATCH_TIMEOUT = 5000L // 5 segundos
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    // Fila de operações pendentes
    private val pendingOperations = ConcurrentLinkedQueue<BatchOperation>()
    
    // Controle de estado
    private val _batchState = MutableStateFlow(BatchState.IDLE)
    val batchState: StateFlow<BatchState> = _batchState.asStateFlow()
    
    // Configurações
    private var batchSize = DEFAULT_BATCH_SIZE
    private var batchTimeout = DEFAULT_BATCH_TIMEOUT
    private var maxRetryAttempts = MAX_RETRY_ATTEMPTS
    
    // Estatísticas
    private val totalOperations = AtomicInteger(0)
    private val successfulOperations = AtomicInteger(0)
    private val failedOperations = AtomicInteger(0)
    private val totalBatchTime = AtomicLong(0)
    
    // Job de processamento em lote
    private var batchJob: Job? = null

    /**
     * Adiciona uma operação ao lote
     */
    suspend fun addOperation(
        operation: suspend () -> Result<Any>,
        priority: OperationPriority = OperationPriority.NORMAL,
        retryOnFailure: Boolean = true
    ): Deferred<Result<Any>> {
        val deferred = CompletableDeferred<Result<Any>>()
        
        val batchOperation = BatchOperation(
            id = generateOperationId(),
            operation = operation,
            deferred = deferred,
            priority = priority,
            retryOnFailure = retryOnFailure,
            attempts = 0
        )
        
        pendingOperations.offer(batchOperation)
        totalOperations.incrementAndGet()
        
        Log.d(TAG, "Operação adicionada ao lote: ${batchOperation.id}, prioridade: $priority")
        
        // Iniciar processamento se necessário
        startBatchProcessingIfNeeded()
        
        return deferred
    }

    /**
     * Processa operações em lote
     */
    private fun startBatchProcessingIfNeeded() {
        if (batchJob?.isActive == true) return
        
        batchJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                _batchState.value = BatchState.PROCESSING
                
                while (pendingOperations.isNotEmpty()) {
                    val batch = collectBatch()
                    if (batch.isNotEmpty()) {
                        processBatch(batch)
                    }
                }
                
                _batchState.value = BatchState.IDLE
                
            } catch (e: Exception) {
                Log.e(TAG, "Erro no processamento em lote: ${e.message}", e)
                _batchState.value = BatchState.ERROR
            }
        }
    }

    /**
     * Coleta operações para um lote
     */
    private suspend fun collectBatch(): List<BatchOperation> {
        val batch = mutableListOf<BatchOperation>()
        val startTime = System.currentTimeMillis()
        
        // Coletar operações até atingir o tamanho do lote ou timeout
        while (batch.size < batchSize && 
               pendingOperations.isNotEmpty() && 
               (System.currentTimeMillis() - startTime) < batchTimeout) {
            
            val operation = pendingOperations.poll()
            if (operation != null) {
                batch.add(operation)
            } else {
                delay(100) // Pequena pausa se não há operações
            }
        }
        
        Log.d(TAG, "Lote coletado: ${batch.size} operações")
        return batch
    }

    /**
     * Processa um lote de operações
     */
    private suspend fun processBatch(batch: List<BatchOperation>) {
        val batchStartTime = System.currentTimeMillis()
        
        try {
            // Ordenar por prioridade
            val sortedBatch = batch.sortedBy { it.priority.ordinal }
            
            // Processar operações em paralelo
            val results = sortedBatch.map { operation ->
                CoroutineScope(Dispatchers.IO).async {
                    processOperation(operation)
                }
            }
            
            // Aguardar todos os resultados
            results.awaitAll()
            
            val batchTime = System.currentTimeMillis() - batchStartTime
            totalBatchTime.addAndGet(batchTime)
            
            Log.d(TAG, "Lote processado: ${batch.size} operações em ${batchTime}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar lote: ${e.message}", e)
            
            // Marcar operações como falhadas
            batch.forEach { operation ->
                operation.deferred.complete(Result.failure(e))
                failedOperations.incrementAndGet()
            }
        }
    }

    /**
     * Processa uma operação individual
     */
    private suspend fun processOperation(operation: BatchOperation): Result<Any> {
        return try {
            val result = operation.operation()
            
            if (result.isSuccess) {
                operation.deferred.complete(result)
                successfulOperations.incrementAndGet()
                Log.d(TAG, "Operação ${operation.id} executada com sucesso")
            } else {
                handleOperationFailure(operation, result.exceptionOrNull())
            }
            
            result
            
        } catch (e: Exception) {
            handleOperationFailure(operation, e)
            Result.failure(e)
        }
    }

    /**
     * Trata falha de operação
     */
    private suspend fun handleOperationFailure(operation: BatchOperation, exception: Throwable?) {
        operation.attempts++
        
        if (operation.retryOnFailure && operation.attempts < maxRetryAttempts) {
            Log.w(TAG, "Operação ${operation.id} falhou (tentativa ${operation.attempts}), tentando novamente")
            
            // Reagendar operação
            delay(1000L * operation.attempts) // Backoff exponencial
            pendingOperations.offer(operation)
            
        } else {
            Log.e(TAG, "Operação ${operation.id} falhou definitivamente após ${operation.attempts} tentativas")
            operation.deferred.complete(Result.failure(exception ?: Exception("Operação falhou")))
            failedOperations.incrementAndGet()
        }
    }

    /**
     * Configura parâmetros do lote
     */
    fun configureBatch(
        batchSize: Int = DEFAULT_BATCH_SIZE,
        batchTimeout: Long = DEFAULT_BATCH_TIMEOUT,
        maxRetryAttempts: Int = MAX_RETRY_ATTEMPTS
    ) {
        this.batchSize = batchSize
        this.batchTimeout = batchTimeout
        this.maxRetryAttempts = maxRetryAttempts
        
        Log.d(TAG, "Configuração do lote atualizada: size=$batchSize, timeout=${batchTimeout}ms, retries=$maxRetryAttempts")
    }

    /**
     * Força processamento de todas as operações pendentes
     */
    suspend fun flushPendingOperations() {
        Log.d(TAG, "Forçando processamento de operações pendentes: ${pendingOperations.size}")
        
        while (pendingOperations.isNotEmpty()) {
            val batch = collectBatch()
            if (batch.isNotEmpty()) {
                processBatch(batch)
            }
        }
    }

    /**
     * Cancela todas as operações pendentes
     */
    fun cancelAllOperations() {
        val cancelledCount = pendingOperations.size
        pendingOperations.clear()
        batchJob?.cancel()
        
        Log.d(TAG, "Canceladas $cancelledCount operações pendentes")
    }

    /**
     * Obtém estatísticas de performance
     */
    fun getPerformanceStats(): BatchPerformanceStats {
        val totalOps = totalOperations.get()
        val successfulOps = successfulOperations.get()
        val failedOps = failedOperations.get()
        val totalTime = totalBatchTime.get()
        
        return BatchPerformanceStats(
            totalOperations = totalOps,
            successfulOperations = successfulOps,
            failedOperations = failedOps,
            successRate = if (totalOps > 0) (successfulOps.toDouble() / totalOps * 100) else 0.0,
            averageBatchTime = if (totalOps > 0) (totalTime.toDouble() / totalOps) else 0.0,
            pendingOperations = pendingOperations.size,
            currentBatchState = _batchState.value
        )
    }

    /**
     * Gera ID único para operação
     */
    private fun generateOperationId(): String {
        return "op_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }

    /**
     * Enum para prioridade de operação
     */
    enum class OperationPriority {
        LOW, NORMAL, HIGH, CRITICAL
    }

    /**
     * Enum para estado do lote
     */
    enum class BatchState {
        IDLE, PROCESSING, ERROR
    }

    /**
     * Data class para operação em lote
     */
    private data class BatchOperation(
        val id: String,
        val operation: suspend () -> Result<Any>,
        val deferred: CompletableDeferred<Result<Any>>,
        val priority: OperationPriority,
        val retryOnFailure: Boolean,
        var attempts: Int
    )

    /**
     * Data class para estatísticas de performance
     */
    data class BatchPerformanceStats(
        val totalOperations: Int,
        val successfulOperations: Int,
        val failedOperations: Int,
        val successRate: Double,
        val averageBatchTime: Double,
        val pendingOperations: Int,
        val currentBatchState: BatchState
    )
}
