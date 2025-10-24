package com.example.gestaobilhares.database

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger

/**
 * ✅ FASE 4D: Tuner de Performance de Banco de Dados
 * Seguindo Android 2025 best practices para otimização de banco
 * 
 * Funcionalidades:
 * - Configurações avançadas de performance
 * - Monitoramento de métricas
 * - Otimização automática
 * - Análise de performance
 */
class DatabasePerformanceTuner private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: DatabasePerformanceTuner? = null

        fun getInstance(): DatabasePerformanceTuner {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabasePerformanceTuner().also { INSTANCE = it }
            }
        }

        private const val TAG = "DatabasePerformanceTuner"
    }

    // Métricas de performance
    private val totalOperations = AtomicLong(0)
    private val totalExecutionTime = AtomicLong(0)
    private val slowOperations = AtomicInteger(0)
    private val optimizedOperations = AtomicInteger(0)
    
    // Configurações de performance
    private var isOptimized = false
    private var performanceLevel = PerformanceLevel.BALANCED

    /**
     * Aplica otimizações de performance ao banco
     */
    fun optimizeDatabase(database: SupportSQLiteDatabase) {
        try {
            Log.d(TAG, "Iniciando otimização de performance do banco")
            
            // Configurações básicas de performance
            applyBasicOptimizations(database)
            
            // Configurações avançadas baseadas no nível
            when (performanceLevel) {
                PerformanceLevel.BALANCED -> applyBalancedOptimizations(database)
                PerformanceLevel.HIGH_PERFORMANCE -> applyHighPerformanceOptimizations(database)
                PerformanceLevel.MEMORY_OPTIMIZED -> applyMemoryOptimizedSettings(database)
            }
            
            isOptimized = true
            Log.d(TAG, "Otimização de performance concluída")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao otimizar banco: ${e.message}", e)
        }
    }

    /**
     * Aplica otimizações básicas
     */
    private fun applyBasicOptimizations(database: SupportSQLiteDatabase) {
        // WAL Mode para melhor concorrência
        database.execSQL("PRAGMA journal_mode=WAL")
        
        // Synchronous mode otimizado
        database.execSQL("PRAGMA synchronous=NORMAL")
        
        // Cache size otimizado
        database.execSQL("PRAGMA cache_size=10000")
        
        // Temp store em memória
        database.execSQL("PRAGMA temp_store=MEMORY")
        
        // Page size otimizado
        database.execSQL("PRAGMA page_size=4096")
        
        Log.d(TAG, "Otimizações básicas aplicadas")
    }

    /**
     * Aplica otimizações balanceadas
     */
    private fun applyBalancedOptimizations(database: SupportSQLiteDatabase) {
        // Cache size balanceado
        database.execSQL("PRAGMA cache_size=5000")
        
        // Memory map otimizado
        database.execSQL("PRAGMA mmap_size=268435456") // 256MB
        
        // Query planner otimizado
        database.execSQL("PRAGMA optimize")
        
        Log.d(TAG, "Otimizações balanceadas aplicadas")
    }

    /**
     * Aplica otimizações de alta performance
     */
    private fun applyHighPerformanceOptimizations(database: SupportSQLiteDatabase) {
        // Cache size maior para performance
        database.execSQL("PRAGMA cache_size=20000")
        
        // Memory map maior
        database.execSQL("PRAGMA mmap_size=536870912") // 512MB
        
        // Threading otimizado
        database.execSQL("PRAGMA threads=4")
        
        // Query planner agressivo
        database.execSQL("PRAGMA optimize")
        database.execSQL("PRAGMA analysis_limit=1000")
        
        Log.d(TAG, "Otimizações de alta performance aplicadas")
    }

    /**
     * Aplica configurações otimizadas para memória
     */
    private fun applyMemoryOptimizedSettings(database: SupportSQLiteDatabase) {
        // Cache size menor
        database.execSQL("PRAGMA cache_size=2000")
        
        // Memory map menor
        database.execSQL("PRAGMA mmap_size=67108864") // 64MB
        
        // Threading limitado
        database.execSQL("PRAGMA threads=2")
        
        // Vacuum automático
        database.execSQL("PRAGMA auto_vacuum=INCREMENTAL")
        
        Log.d(TAG, "Configurações otimizadas para memória aplicadas")
    }

    /**
     * Registra operação de banco
     */
    fun recordOperation(operation: String, executionTime: Long) {
        totalOperations.incrementAndGet()
        totalExecutionTime.addAndGet(executionTime)
        
        if (executionTime > 1000) { // Operações > 1 segundo
            slowOperations.incrementAndGet()
            Log.w(TAG, "Operação lenta detectada: $operation - ${executionTime}ms")
        }
        
        if (isOptimized && executionTime < 100) { // Operações < 100ms após otimização
            optimizedOperations.incrementAndGet()
        }
    }

    /**
     * Analisa performance do banco
     */
    fun analyzePerformance(): PerformanceAnalysis {
        val totalOps = totalOperations.get()
        val totalTime = totalExecutionTime.get()
        val slowOps = slowOperations.get()
        val optimizedOps = optimizedOperations.get()
        
        return PerformanceAnalysis(
            totalOperations = totalOps,
            totalExecutionTime = totalTime,
            averageExecutionTime = if (totalOps > 0) (totalTime.toDouble() / totalOps) else 0.0,
            slowOperations = slowOps,
            slowOperationRate = if (totalOps > 0) (slowOps.toDouble() / totalOps * 100) else 0.0,
            optimizedOperations = optimizedOps,
            optimizationRate = if (totalOps > 0) (optimizedOps.toDouble() / totalOps * 100) else 0.0,
            isOptimized = isOptimized,
            performanceLevel = performanceLevel
        )
    }

    /**
     * Configura nível de performance
     */
    fun setPerformanceLevel(level: PerformanceLevel) {
        this.performanceLevel = level
        isOptimized = false // Requer re-otimização
        Log.d(TAG, "Nível de performance configurado: $level")
    }

    /**
     * Executa análise e otimização automática
     */
    fun performAutoOptimization(database: SupportSQLiteDatabase) {
        val analysis = analyzePerformance()
        
        when {
            analysis.slowOperationRate > 20 -> {
                Log.d(TAG, "Muitas operações lentas detectadas, aplicando otimizações de alta performance")
                setPerformanceLevel(PerformanceLevel.HIGH_PERFORMANCE)
                optimizeDatabase(database)
            }
            analysis.optimizationRate < 50 -> {
                Log.d(TAG, "Baixa taxa de otimização, aplicando configurações balanceadas")
                setPerformanceLevel(PerformanceLevel.BALANCED)
                optimizeDatabase(database)
            }
            else -> {
                Log.d(TAG, "Performance adequada, mantendo configurações atuais")
            }
        }
    }

    /**
     * Obtém estatísticas detalhadas
     */
    fun getDetailedStats(): DetailedPerformanceStats {
        return DetailedPerformanceStats(
            totalOperations = totalOperations.get(),
            totalExecutionTime = totalExecutionTime.get(),
            slowOperations = slowOperations.get(),
            optimizedOperations = optimizedOperations.get(),
            isOptimized = isOptimized,
            performanceLevel = performanceLevel,
            averageExecutionTime = if (totalOperations.get() > 0) {
                totalExecutionTime.get().toDouble() / totalOperations.get()
            } else 0.0,
            slowOperationRate = if (totalOperations.get() > 0) {
                slowOperations.get().toDouble() / totalOperations.get() * 100
            } else 0.0,
            optimizationRate = if (totalOperations.get() > 0) {
                optimizedOperations.get().toDouble() / totalOperations.get() * 100
            } else 0.0
        )
    }

    /**
     * Reseta estatísticas
     */
    fun resetStats() {
        totalOperations.set(0)
        totalExecutionTime.set(0)
        slowOperations.set(0)
        optimizedOperations.set(0)
        Log.d(TAG, "Estatísticas de performance resetadas")
    }

    /**
     * Enums e data classes
     */
    enum class PerformanceLevel {
        BALANCED,           // Balanceado entre performance e memória
        HIGH_PERFORMANCE,   // Máxima performance
        MEMORY_OPTIMIZED    // Otimizado para memória
    }

    data class PerformanceAnalysis(
        val totalOperations: Long,
        val totalExecutionTime: Long,
        val averageExecutionTime: Double,
        val slowOperations: Int,
        val slowOperationRate: Double,
        val optimizedOperations: Int,
        val optimizationRate: Double,
        val isOptimized: Boolean,
        val performanceLevel: PerformanceLevel
    )

    data class DetailedPerformanceStats(
        val totalOperations: Long,
        val totalExecutionTime: Long,
        val slowOperations: Int,
        val optimizedOperations: Int,
        val isOptimized: Boolean,
        val performanceLevel: PerformanceLevel,
        val averageExecutionTime: Double,
        val slowOperationRate: Double,
        val optimizationRate: Double
    )
}
