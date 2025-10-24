package com.example.gestaobilhares.database

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger

/**
 * ✅ FASE 4D: Gerenciador de Otimização de Queries
 * Seguindo Android 2025 best practices para performance de banco
 * 
 * Funcionalidades:
 * - Cache de queries otimizadas
 * - Análise de performance de queries
 * - Sugestões de otimização
 * - Estatísticas detalhadas
 */
class QueryOptimizationManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: QueryOptimizationManager? = null

        fun getInstance(): QueryOptimizationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: QueryOptimizationManager().also { INSTANCE = it }
            }
        }

        private const val TAG = "QueryOptimizationManager"
        private const val MAX_CACHE_SIZE = 100
        private const val SLOW_QUERY_THRESHOLD = 1000L // 1 segundo
    }

    // Cache de queries otimizadas
    private val queryCache = ConcurrentHashMap<String, OptimizedQuery>()
    
    // Estatísticas de performance
    private val totalQueries = AtomicLong(0)
    private val slowQueries = AtomicLong(0)
    private val totalExecutionTime = AtomicLong(0)
    private val cacheHits = AtomicInteger(0)
    private val cacheMisses = AtomicInteger(0)
    
    // Análise de queries
    private val queryAnalysis = ConcurrentHashMap<String, QueryAnalysis>()

    /**
     * Otimiza uma query SQL
     */
    fun optimizeQuery(originalQuery: String, parameters: Map<String, Any> = emptyMap()): OptimizedQuery {
        val queryKey = generateQueryKey(originalQuery, parameters)
        
        // Verificar cache primeiro
        queryCache[queryKey]?.let { cached ->
            cacheHits.incrementAndGet()
            Log.d(TAG, "Query otimizada encontrada no cache: $queryKey")
            return cached
        }
        
        cacheMisses.incrementAndGet()
        
        // Analisar e otimizar query
        val analysis = analyzeQuery(originalQuery)
        val optimizedQuery = createOptimizedQuery(originalQuery, analysis, parameters)
        
        // Cachear resultado
        if (queryCache.size < MAX_CACHE_SIZE) {
            queryCache[queryKey] = optimizedQuery
        }
        
        Log.d(TAG, "Query otimizada: $queryKey")
        return optimizedQuery
    }

    /**
     * Analisa uma query para identificar problemas
     */
    private fun analyzeQuery(query: String): QueryAnalysis {
        val analysis = QueryAnalysis(
            originalQuery = query,
            hasIndexes = checkForIndexes(query),
            hasJoins = checkForJoins(query),
            hasSubqueries = checkForSubqueries(query),
            hasOrderBy = checkForOrderBy(query),
            hasGroupBy = checkForGroupBy(query),
            hasLimit = checkForLimit(query),
            estimatedComplexity = estimateComplexity(query),
            suggestions = generateSuggestions(query)
        )
        
        queryAnalysis[query] = analysis
        return analysis
    }

    /**
     * Cria query otimizada baseada na análise
     */
    private fun createOptimizedQuery(
        originalQuery: String,
        analysis: QueryAnalysis,
        parameters: Map<String, Any>
    ): OptimizedQuery {
        var optimizedSql = originalQuery
        
        // Aplicar otimizações baseadas na análise
        if (analysis.suggestions.contains("ADD_INDEX")) {
            optimizedSql = addIndexHints(optimizedSql)
        }
        
        if (analysis.suggestions.contains("OPTIMIZE_JOIN")) {
            optimizedSql = optimizeJoins(optimizedSql)
        }
        
        if (analysis.suggestions.contains("ADD_LIMIT")) {
            optimizedSql = addLimitIfMissing(optimizedSql)
        }
        
        if (analysis.suggestions.contains("OPTIMIZE_ORDER_BY")) {
            optimizedSql = optimizeOrderBy(optimizedSql)
        }
        
        return OptimizedQuery(
            originalQuery = originalQuery,
            optimizedQuery = optimizedSql,
            parameters = parameters,
            analysis = analysis,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Verifica se query tem índices adequados
     */
    private fun checkForIndexes(query: String): Boolean {
        val lowerQuery = query.lowercase()
        return lowerQuery.contains("where") && 
               (lowerQuery.contains("id") || 
                lowerQuery.contains("_id") || 
                lowerQuery.contains("index"))
    }

    /**
     * Verifica se query tem JOINs
     */
    private fun checkForJoins(query: String): Boolean {
        return query.lowercase().contains("join")
    }

    /**
     * Verifica se query tem subqueries
     */
    private fun checkForSubqueries(query: String): Boolean {
        return query.lowercase().contains("select") && 
               query.lowercase().split("select").size > 2
    }

    /**
     * Verifica se query tem ORDER BY
     */
    private fun checkForOrderBy(query: String): Boolean {
        return query.lowercase().contains("order by")
    }

    /**
     * Verifica se query tem GROUP BY
     */
    private fun checkForGroupBy(query: String): Boolean {
        return query.lowercase().contains("group by")
    }

    /**
     * Verifica se query tem LIMIT
     */
    private fun checkForLimit(query: String): Boolean {
        return query.lowercase().contains("limit")
    }

    /**
     * Estima complexidade da query
     */
    private fun estimateComplexity(query: String): QueryComplexity {
        var complexity = 0
        
        if (checkForJoins(query)) complexity += 2
        if (checkForSubqueries(query)) complexity += 3
        if (checkForGroupBy(query)) complexity += 2
        if (checkForOrderBy(query)) complexity += 1
        
        return when {
            complexity >= 5 -> QueryComplexity.HIGH
            complexity >= 3 -> QueryComplexity.MEDIUM
            else -> QueryComplexity.LOW
        }
    }

    /**
     * Gera sugestões de otimização
     */
    private fun generateSuggestions(query: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (!checkForIndexes(query) && query.lowercase().contains("where")) {
            suggestions.add("ADD_INDEX")
        }
        
        if (checkForJoins(query)) {
            suggestions.add("OPTIMIZE_JOIN")
        }
        
        if (!checkForLimit(query) && !checkForGroupBy(query)) {
            suggestions.add("ADD_LIMIT")
        }
        
        if (checkForOrderBy(query)) {
            suggestions.add("OPTIMIZE_ORDER_BY")
        }
        
        return suggestions
    }

    /**
     * Adiciona hints de índice
     */
    private fun addIndexHints(query: String): String {
        // Implementação simplificada - em produção seria mais complexa
        return query.replace("WHERE", "WHERE /*+ INDEX */")
    }

    /**
     * Otimiza JOINs
     */
    private fun optimizeJoins(query: String): String {
        // Implementação simplificada - em produção seria mais complexa
        return query.replace("JOIN", "INNER JOIN")
    }

    /**
     * Adiciona LIMIT se não existir
     */
    private fun addLimitIfMissing(query: String): String {
        return if (!checkForLimit(query)) {
            "$query LIMIT 1000"
        } else {
            query
        }
    }

    /**
     * Otimiza ORDER BY
     */
    private fun optimizeOrderBy(query: String): String {
        // Implementação simplificada - em produção seria mais complexa
        return query.replace("ORDER BY", "ORDER BY /*+ INDEX */")
    }

    /**
     * Registra execução de query
     */
    fun recordQueryExecution(query: String, executionTime: Long) {
        totalQueries.incrementAndGet()
        totalExecutionTime.addAndGet(executionTime)
        
        if (executionTime > SLOW_QUERY_THRESHOLD) {
            slowQueries.incrementAndGet()
            Log.w(TAG, "Query lenta detectada: ${executionTime}ms - $query")
        }
        
        // Atualizar análise da query
        queryAnalysis[query]?.let { analysis ->
            analysis.executionCount++
            analysis.totalExecutionTime += executionTime
            analysis.averageExecutionTime = analysis.totalExecutionTime.toDouble() / analysis.executionCount
        }
    }

    /**
     * Gera chave única para query
     */
    private fun generateQueryKey(query: String, parameters: Map<String, Any>): String {
        val paramsString = parameters.entries.sortedBy { it.key }
            .joinToString("|") { "${it.key}=${it.value}" }
        return "${query.hashCode()}_$paramsString"
    }

    /**
     * Obtém estatísticas de otimização
     */
    fun getOptimizationStats(): QueryOptimizationStats {
        val totalQueriesCount = totalQueries.get()
        val slowQueriesCount = slowQueries.get()
        val totalTime = totalExecutionTime.get()
        
        return QueryOptimizationStats(
            totalQueries = totalQueriesCount,
            slowQueries = slowQueriesCount,
            totalExecutionTime = totalTime,
            averageExecutionTime = if (totalQueriesCount > 0) (totalTime.toDouble() / totalQueriesCount) else 0.0,
            cacheHits = cacheHits.get(),
            cacheMisses = cacheMisses.get(),
            cacheHitRate = if (cacheHits.get() + cacheMisses.get() > 0) {
                (cacheHits.get().toDouble() / (cacheHits.get() + cacheMisses.get()) * 100)
            } else 0.0,
            cachedQueries = queryCache.size,
            analyzedQueries = queryAnalysis.size
        )
    }

    /**
     * Limpa cache de queries
     */
    fun clearQueryCache() {
        queryCache.clear()
        queryAnalysis.clear()
        Log.d(TAG, "Cache de queries limpo")
    }

    /**
     * Enums e data classes
     */
    enum class QueryComplexity { LOW, MEDIUM, HIGH }

    data class OptimizedQuery(
        val originalQuery: String,
        val optimizedQuery: String,
        val parameters: Map<String, Any>,
        val analysis: QueryAnalysis,
        val createdAt: Long
    )

    data class QueryAnalysis(
        val originalQuery: String,
        val hasIndexes: Boolean,
        val hasJoins: Boolean,
        val hasSubqueries: Boolean,
        val hasOrderBy: Boolean,
        val hasGroupBy: Boolean,
        val hasLimit: Boolean,
        val estimatedComplexity: QueryComplexity,
        val suggestions: List<String>,
        var executionCount: Int = 0,
        var totalExecutionTime: Long = 0L,
        var averageExecutionTime: Double = 0.0
    )

    data class QueryOptimizationStats(
        val totalQueries: Long,
        val slowQueries: Long,
        val totalExecutionTime: Long,
        val averageExecutionTime: Double,
        val cacheHits: Int,
        val cacheMisses: Int,
        val cacheHitRate: Double,
        val cachedQueries: Int,
        val analyzedQueries: Int
    )
}
