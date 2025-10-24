package com.example.gestaobilhares.database

import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.*
import java.util.concurrent.Semaphore

/**
 * ✅ FASE 4D: Pool de Conexões de Banco de Dados
 * Seguindo Android 2025 best practices para otimização de banco
 * 
 * Funcionalidades:
 * - Pool de conexões para otimizar acesso ao banco
 * - Controle de concorrência inteligente
 * - Estatísticas de performance
 * - Gerenciamento automático de recursos
 */
class DatabaseConnectionPool private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: DatabaseConnectionPool? = null

        fun getInstance(): DatabaseConnectionPool {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseConnectionPool().also { INSTANCE = it }
            }
        }

        private const val TAG = "DatabaseConnectionPool"
        private const val DEFAULT_POOL_SIZE = 10
        private const val MAX_POOL_SIZE = 20
        private const val CONNECTION_TIMEOUT = 30000L // 30 segundos
    }

    // Pool de conexões
    private val connectionPool = ConcurrentLinkedQueue<PooledConnection>()
    
    // Controle de concorrência
    private val availableConnections = Semaphore(DEFAULT_POOL_SIZE)
    private val activeConnections = AtomicInteger(0)
    private val totalConnections = AtomicInteger(0)
    
    // Estatísticas
    private val totalRequests = AtomicLong(0)
    private val successfulRequests = AtomicLong(0)
    private val failedRequests = AtomicLong(0)
    private val totalWaitTime = AtomicLong(0)
    private val totalExecutionTime = AtomicLong(0)
    
    // Configurações
    private var maxPoolSize = DEFAULT_POOL_SIZE
    private var connectionTimeout = CONNECTION_TIMEOUT
    private var isInitialized = false

    /**
     * Inicializa o pool de conexões
     */
    fun initialize(
        database: RoomDatabase,
        poolSize: Int = DEFAULT_POOL_SIZE,
        timeout: Long = CONNECTION_TIMEOUT
    ) {
        if (isInitialized) {
            Log.w(TAG, "Pool já foi inicializado")
            return
        }

        this.maxPoolSize = poolSize
        this.connectionTimeout = timeout
        
        // Criar conexões iniciais
        repeat(poolSize) {
            val connection = PooledConnection(
                id = "conn_${System.currentTimeMillis()}_$it",
                database = database,
                createdAt = System.currentTimeMillis()
            )
            connectionPool.offer(connection)
        }
        
        isInitialized = true
        Log.d(TAG, "Pool de conexões inicializado: $poolSize conexões")
    }

    /**
     * Obtém uma conexão do pool
     */
    suspend fun acquireConnection(): PooledConnection? {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            totalRequests.incrementAndGet()
            
            try {
                // Aguardar conexão disponível
                if (availableConnections.tryAcquire(connectionTimeout, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    val connection = connectionPool.poll()
                    
                    if (connection != null) {
                        activeConnections.incrementAndGet()
                        connection.isActive = true
                        connection.lastUsed = System.currentTimeMillis()
                        
                        val waitTime = System.currentTimeMillis() - startTime
                        totalWaitTime.addAndGet(waitTime)
                        successfulRequests.incrementAndGet()
                        
                        Log.d(TAG, "Conexão adquirida: ${connection.id}, tempo de espera: ${waitTime}ms")
                        connection
                    } else {
                        availableConnections.release()
                        Log.w(TAG, "Nenhuma conexão disponível no pool")
                        null
                    }
                } else {
                    failedRequests.incrementAndGet()
                    Log.e(TAG, "Timeout ao aguardar conexão")
                    null
                }
            } catch (e: Exception) {
                failedRequests.incrementAndGet()
                Log.e(TAG, "Erro ao adquirir conexão: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Libera uma conexão de volta para o pool
     */
    fun releaseConnection(connection: PooledConnection) {
        try {
            if (connection.isActive) {
                connection.isActive = false
                connection.lastReleased = System.currentTimeMillis()
                
                // Verificar se conexão ainda é válida
                if (isConnectionValid(connection)) {
                    connectionPool.offer(connection)
                    activeConnections.decrementAndGet()
                    availableConnections.release()
                    
                    Log.d(TAG, "Conexão liberada: ${connection.id}")
                } else {
                    // Criar nova conexão se a atual não é válida
                    createNewConnection(connection.database)
                    Log.d(TAG, "Conexão inválida substituída: ${connection.id}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao liberar conexão: ${e.message}", e)
        }
    }

    /**
     * Executa operação com conexão do pool
     */
    suspend fun <T> executeWithConnection(
        operation: suspend (PooledConnection) -> T
    ): T? {
        val connection = acquireConnection()
        return if (connection != null) {
            try {
                val startTime = System.currentTimeMillis()
                val result = operation(connection)
                val executionTime = System.currentTimeMillis() - startTime
                totalExecutionTime.addAndGet(executionTime)
                
                Log.d(TAG, "Operação executada em ${executionTime}ms")
                result
            } finally {
                releaseConnection(connection)
            }
        } else {
            Log.e(TAG, "Não foi possível obter conexão para operação")
            null
        }
    }

    /**
     * Verifica se conexão ainda é válida
     */
    private fun isConnectionValid(connection: PooledConnection): Boolean {
        val currentTime = System.currentTimeMillis()
        val connectionAge = currentTime - connection.createdAt
        val idleTime = currentTime - connection.lastUsed
        
        return connectionAge < connectionTimeout && idleTime < connectionTimeout
    }

    /**
     * Cria nova conexão
     */
    private fun createNewConnection(database: RoomDatabase) {
        if (totalConnections.get() < maxPoolSize) {
            val newConnection = PooledConnection(
                id = "conn_${System.currentTimeMillis()}_${totalConnections.get()}",
                database = database,
                createdAt = System.currentTimeMillis()
            )
            connectionPool.offer(newConnection)
            totalConnections.incrementAndGet()
            Log.d(TAG, "Nova conexão criada: ${newConnection.id}")
        }
    }

    /**
     * Obtém estatísticas do pool
     */
    fun getPoolStats(): ConnectionPoolStats {
        val totalReqs = totalRequests.get()
        val successfulReqs = successfulRequests.get()
        val failedReqs = failedRequests.get()
        
        return ConnectionPoolStats(
            totalConnections = totalConnections.get(),
            activeConnections = activeConnections.get(),
            availableConnections = connectionPool.size,
            maxPoolSize = maxPoolSize,
            totalRequests = totalReqs,
            successfulRequests = successfulReqs,
            failedRequests = failedReqs,
            successRate = if (totalReqs > 0) (successfulReqs.toDouble() / totalReqs * 100) else 0.0,
            averageWaitTime = if (successfulReqs > 0) (totalWaitTime.get().toDouble() / successfulReqs) else 0.0,
            averageExecutionTime = if (successfulReqs > 0) (totalExecutionTime.get().toDouble() / successfulReqs) else 0.0
        )
    }

    /**
     * Limpa o pool de conexões
     */
    fun clearPool() {
        connectionPool.clear()
        activeConnections.set(0)
        totalConnections.set(0)
        isInitialized = false
        Log.d(TAG, "Pool de conexões limpo")
    }

    /**
     * Data class para conexão do pool
     */
    data class PooledConnection(
        val id: String,
        val database: RoomDatabase,
        val createdAt: Long,
        var isActive: Boolean = false,
        var lastUsed: Long = 0L,
        var lastReleased: Long = 0L
    )

    /**
     * Data class para estatísticas do pool
     */
    data class ConnectionPoolStats(
        val totalConnections: Int,
        val activeConnections: Int,
        val availableConnections: Int,
        val maxPoolSize: Int,
        val totalRequests: Long,
        val successfulRequests: Long,
        val failedRequests: Long,
        val successRate: Double,
        val averageWaitTime: Double,
        val averageExecutionTime: Double
    )
}
