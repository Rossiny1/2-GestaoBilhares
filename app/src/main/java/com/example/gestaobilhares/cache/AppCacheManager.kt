package com.example.gestaobilhares.cache

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * ✅ FASE 4A: Sistema de Cache Inteligente
 * 
 * Gerencia cache de dados frequentes para otimizar performance
 * - Cache para rotas ativas
 * - Cache para clientes por rota
 * - Cache para estatísticas financeiras
 * - Invalidação inteligente baseada em tempo e eventos
 */
class AppCacheManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: AppCacheManager? = null
        
        fun getInstance(): AppCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppCacheManager().also { INSTANCE = it }
            }
        }
    }
    
    // Cache de dados com timestamp
    private val cache = ConcurrentHashMap<String, CacheEntry<Any>>()
    
    // Configurações de cache
    private val defaultTtl = TimeUnit.MINUTES.toMillis(5) // 5 minutos
    private val maxCacheSize = 100 // Máximo 100 itens em cache
    
    // Estado do cache
    private val _cacheStats = MutableStateFlow(CacheStats())
    val cacheStats: StateFlow<CacheStats> = _cacheStats.asStateFlow()
    
    /**
     * Entrada do cache com timestamp e TTL
     */
    data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
        val ttl: Long = TimeUnit.MINUTES.toMillis(5)
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > ttl
    }
    
    /**
     * Estatísticas do cache
     */
    data class CacheStats(
        val hits: Int = 0,
        val misses: Int = 0,
        val evictions: Int = 0,
        val size: Int = 0
    )
    
    /**
     * Armazenar dados no cache
     */
    fun <T> put(key: String, data: T, ttl: Long = defaultTtl): T {
        // Limpar cache se necessário
        if (cache.size >= maxCacheSize) {
            evictExpiredEntries()
        }
        
        cache[key] = CacheEntry(data as Any, System.currentTimeMillis(), ttl)
        updateStats()
        
        Log.d("AppCacheManager", "✅ Cache PUT: $key (TTL: ${ttl}ms)")
        return data
    }
    
    /**
     * Recuperar dados do cache
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cache[key] as? CacheEntry<Any>
        
        return when {
            entry == null -> {
                _cacheStats.value = _cacheStats.value.copy(misses = _cacheStats.value.misses + 1)
                Log.d("AppCacheManager", "❌ Cache MISS: $key")
                null
            }
            entry.isExpired() -> {
                cache.remove(key)
                _cacheStats.value = _cacheStats.value.copy(misses = _cacheStats.value.misses + 1)
                Log.d("AppCacheManager", "⏰ Cache EXPIRED: $key")
                null
            }
            else -> {
                _cacheStats.value = _cacheStats.value.copy(hits = _cacheStats.value.hits + 1)
                Log.d("AppCacheManager", "✅ Cache HIT: $key")
                entry.data as? T
            }
        }
    }
    
    /**
     * Verificar se existe no cache (sem recuperar)
     */
    fun contains(key: String): Boolean {
        val entry = cache[key]
        return when {
            entry == null -> false
            entry.isExpired() -> {
                cache.remove(key)
                false
            }
            else -> true
        }
    }
    
    /**
     * Invalidar entrada específica
     */
    fun invalidate(key: String) {
        cache.remove(key)
        updateStats()
        Log.d("AppCacheManager", "🗑️ Cache INVALIDATED: $key")
    }
    
    /**
     * Invalidar múltiplas entradas por padrão
     */
    fun invalidatePattern(pattern: String) {
        val keysToRemove = cache.keys.filter { it.contains(pattern) }
        keysToRemove.forEach { cache.remove(it) }
        updateStats()
        Log.d("AppCacheManager", "🗑️ Cache INVALIDATED PATTERN: $pattern (${keysToRemove.size} entries)")
    }
    
    /**
     * Limpar cache expirado
     */
    fun evictExpiredEntries() {
        val expiredKeys = cache.entries
            .filter { it.value.isExpired() }
            .map { it.key }
        
        expiredKeys.forEach { cache.remove(it) }
        
        if (expiredKeys.isNotEmpty()) {
            _cacheStats.value = _cacheStats.value.copy(evictions = _cacheStats.value.evictions + expiredKeys.size)
            Log.d("AppCacheManager", "🧹 Cache EVICTED: ${expiredKeys.size} expired entries")
        }
    }
    
    /**
     * Limpar todo o cache
     */
    fun clear() {
        cache.clear()
        updateStats()
        Log.d("AppCacheManager", "🧹 Cache CLEARED: All entries removed")
    }
    
    /**
     * Atualizar estatísticas
     */
    private fun updateStats() {
        _cacheStats.value = _cacheStats.value.copy(size = cache.size)
    }
    
    /**
     * Obter estatísticas do cache
     */
    fun getStats(): CacheStats = _cacheStats.value
    
    /**
     * Verificar saúde do cache
     */
    fun getHealthStatus(): String {
        val stats = getStats()
        val hitRate = if (stats.hits + stats.misses > 0) {
            (stats.hits.toFloat() / (stats.hits + stats.misses) * 100).toInt()
        } else 0
        
        return "Cache Health: $hitRate% hit rate, ${stats.size} entries, ${stats.evictions} evictions"
    }
}
