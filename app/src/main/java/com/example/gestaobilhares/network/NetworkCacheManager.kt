package com.example.gestaobilhares.network

import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * ✅ FASE 4D: Gerenciador de Cache de Rede
 * Seguindo Android 2025 best practices para otimização de rede
 * 
 * Funcionalidades:
 * - Cache inteligente com TTL
 * - Invalidação automática
 * - Compressão de dados em cache
 * - Estatísticas de hit/miss
 */
class NetworkCacheManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: NetworkCacheManager? = null

        fun getInstance(): NetworkCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkCacheManager().also { INSTANCE = it }
            }
        }

        private const val TAG = "NetworkCacheManager"
        private const val DEFAULT_CACHE_SIZE = 50
        private const val DEFAULT_TTL = 300000L // 5 minutos
        private const val CLEANUP_INTERVAL = 60000L // 1 minuto
    }

    // Cache principal com TTL
    private val cache = LruCache<String, CacheEntry>(DEFAULT_CACHE_SIZE)
    
    // Cache de metadados
    private val metadataCache = ConcurrentHashMap<String, CacheMetadata>()
    
    // Estatísticas
    private val cacheHits = AtomicInteger(0)
    private val cacheMisses = AtomicInteger(0)
    private val cacheEvictions = AtomicInteger(0)
    private val totalCacheSize = AtomicLong(0)
    
    // Job de limpeza automática
    private var cleanupJob: Job? = null
    
    // Compression manager
    private val compressionManager = NetworkCompressionManager.getInstance()

    init {
        startCleanupJob()
    }

    /**
     * Armazena dados no cache
     */
    fun put(
        key: String,
        data: ByteArray,
        ttl: Long = DEFAULT_TTL,
        compress: Boolean = true
    ) {
        try {
            val compressedData = if (compress && data.size > 1024) {
                compressionManager.compressData(data, key)
            } else {
                NetworkCompressionManager.CompressedData(data, false, 0.0)
            }
            
            val entry = CacheEntry(
                data = compressedData,
                timestamp = System.currentTimeMillis(),
                ttl = ttl,
                accessCount = 0
            )
            
            val oldEntry = cache.put(key, entry)
            if (oldEntry != null) {
                cacheEvictions.incrementAndGet()
                totalCacheSize.addAndGet(-oldEntry.data.data.size.toLong())
            }
            
            totalCacheSize.addAndGet(entry.data.data.size.toLong())
            
            // Atualizar metadados
            metadataCache[key] = CacheMetadata(
                key = key,
                size = entry.data.data.size,
                compressed = entry.data.isCompressed,
                compressionRatio = entry.data.compressionRatio,
                createdAt = entry.timestamp,
                ttl = ttl
            )
            
            Log.d(TAG, "Dados armazenados no cache: $key, tamanho: ${entry.data.data.size} bytes")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao armazenar no cache: ${e.message}", e)
        }
    }

    /**
     * Armazena string no cache
     */
    fun putString(
        key: String,
        value: String,
        ttl: Long = DEFAULT_TTL,
        compress: Boolean = true
    ) {
        put(key, value.toByteArray(Charsets.UTF_8), ttl, compress)
    }

    /**
     * Obtém dados do cache
     */
    fun get(key: String): ByteArray? {
        return try {
            val entry = cache.get(key)
            
            if (entry != null && !isExpired(entry)) {
                // Atualizar contador de acesso
                entry.accessCount++
                
                // Atualizar estatísticas
                cacheHits.incrementAndGet()
                
                // Descomprimir se necessário
                val data = if (entry.data.isCompressed) {
                    compressionManager.decompressData(entry.data)
                } else {
                    entry.data.data
                }
                
                Log.d(TAG, "Cache hit: $key, acessos: ${entry.accessCount}")
                data
                
            } else {
                if (entry != null) {
                    // Remover entrada expirada
                    cache.remove(key)
                    metadataCache.remove(key)
                    totalCacheSize.addAndGet(-entry.data.data.size.toLong())
                    Log.d(TAG, "Entrada expirada removida: $key")
                }
                
                cacheMisses.incrementAndGet()
                Log.d(TAG, "Cache miss: $key")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter do cache: ${e.message}", e)
            cacheMisses.incrementAndGet()
            null
        }
    }

    /**
     * Obtém string do cache
     */
    fun getString(key: String): String? {
        val data = get(key)
        return data?.let { String(it, Charsets.UTF_8) }
    }

    /**
     * Verifica se chave existe no cache
     */
    fun contains(key: String): Boolean {
        val entry = cache.get(key)
        return entry != null && !isExpired(entry)
    }

    /**
     * Remove entrada do cache
     */
    fun remove(key: String) {
        val entry = cache.remove(key)
        if (entry != null) {
            totalCacheSize.addAndGet(-entry.data.data.size.toLong())
            metadataCache.remove(key)
            Log.d(TAG, "Entrada removida do cache: $key")
        }
    }

    /**
     * Limpa todo o cache
     */
    fun clear() {
        cache.evictAll()
        metadataCache.clear()
        totalCacheSize.set(0)
        Log.d(TAG, "Cache limpo completamente")
    }

    /**
     * Verifica se entrada está expirada
     */
    private fun isExpired(entry: CacheEntry): Boolean {
        return System.currentTimeMillis() - entry.timestamp > entry.ttl
    }

    /**
     * Inicia job de limpeza automática
     */
    private fun startCleanupJob() {
        cleanupJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    delay(CLEANUP_INTERVAL)
                    cleanupExpiredEntries()
                } catch (e: Exception) {
                    Log.e(TAG, "Erro na limpeza automática: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Limpa entradas expiradas
     */
    private fun cleanupExpiredEntries() {
        val snapshot = cache.snapshot()
        var cleanedCount = 0
        
        for ((key, entry) in snapshot) {
            if (isExpired(entry)) {
                cache.remove(key)
                metadataCache.remove(key)
                totalCacheSize.addAndGet(-entry.data.data.size.toLong())
                cleanedCount++
            }
        }
        
        if (cleanedCount > 0) {
            Log.d(TAG, "Limpeza automática: $cleanedCount entradas expiradas removidas")
        }
    }

    /**
     * Obtém estatísticas do cache
     */
    fun getCacheStats(): CacheStats {
        val hits = cacheHits.get()
        val misses = cacheMisses.get()
        val total = hits + misses
        
        return CacheStats(
            totalEntries = cache.size(),
            maxSize = cache.maxSize(),
            totalSize = totalCacheSize.get(),
            cacheHits = hits,
            cacheMisses = misses,
            hitRate = if (total > 0) (hits.toDouble() / total * 100) else 0.0,
            evictions = cacheEvictions.get(),
            compressionStats = compressionManager.getCompressionStats()
        )
    }

    /**
     * Obtém informações detalhadas do cache
     */
    fun getCacheInfo(): List<CacheMetadata> {
        return metadataCache.values.toList()
    }

    /**
     * Configura tamanho do cache
     */
    fun setCacheSize(size: Int) {
        // Note: LruCache não permite alterar o tamanho após criação
        // Esta função é para documentação e futuras implementações
        Log.d(TAG, "Tamanho do cache solicitado: $size (não implementado para LruCache)")
    }

    /**
     * Para job de limpeza
     */
    fun stop() {
        cleanupJob?.cancel()
        Log.d(TAG, "Job de limpeza parado")
    }

    /**
     * Data class para entrada do cache
     */
    private data class CacheEntry(
        val data: NetworkCompressionManager.CompressedData,
        val timestamp: Long,
        val ttl: Long,
        var accessCount: Int
    )

    /**
     * Data class para metadados do cache
     */
    data class CacheMetadata(
        val key: String,
        val size: Int,
        val compressed: Boolean,
        val compressionRatio: Double,
        val createdAt: Long,
        val ttl: Long
    )

    /**
     * Data class para estatísticas do cache
     */
    data class CacheStats(
        val totalEntries: Int,
        val maxSize: Int,
        val totalSize: Long,
        val cacheHits: Int,
        val cacheMisses: Int,
        val hitRate: Double,
        val evictions: Int,
        val compressionStats: NetworkCompressionManager.CompressionStats
    )
}
