package com.example.gestaobilhares.memory

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * ✅ FASE 4D: Otimizador de Memória
 * 
 * Gerencia memória de forma inteligente seguindo Android 2025 best practices:
 * - WeakReference para evitar memory leaks
 * - Object pooling para reutilização
 * - LruCache para bitmaps
 * - Garbage collection otimizado
 */
class MemoryOptimizer private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: MemoryOptimizer? = null
        
        fun getInstance(): MemoryOptimizer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MemoryOptimizer().also { INSTANCE = it }
            }
        }
        
        private const val TAG = "MemoryOptimizer"
        private const val MAX_MEMORY_CACHE_SIZE = 50 * 1024 * 1024 // 50MB
        private const val MAX_OBJECT_POOL_SIZE = 100
    }
    
    // Cache LRU para bitmaps
    private val bitmapCache = LruCache<String, Bitmap>(MAX_MEMORY_CACHE_SIZE / 8)
    
    // Pool de objetos reutilizáveis
    private val objectPools = ConcurrentHashMap<Class<*>, MutableList<Any>>()
    
    // Contador de objetos em pool
    private val poolCounters = ConcurrentHashMap<Class<*>, AtomicInteger>()
    
    // Referências fracas para evitar memory leaks
    private val weakReferences = ConcurrentHashMap<String, WeakReference<Any>>()
    
    /**
     * Cache de bitmap com LRU
     */
    fun cacheBitmap(key: String, bitmap: Bitmap) {
        try {
            bitmapCache.put(key, bitmap)
            Log.d(TAG, "Bitmap cached: $key (${bitmap.width}x${bitmap.height})")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao cachear bitmap: ${e.message}")
        }
    }
    
    /**
     * Obtém bitmap do cache
     */
    fun getCachedBitmap(key: String): Bitmap? {
        return try {
            val bitmap = bitmapCache.get(key)
            if (bitmap != null) {
                Log.d(TAG, "Bitmap cache hit: $key")
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter bitmap do cache: ${e.message}")
            null
        }
    }
    
    /**
     * Pool de objetos reutilizáveis
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getFromPool(clazz: Class<T>): T? {
        return try {
            val pool = objectPools.getOrPut(clazz) { mutableListOf() }
            val counter = poolCounters.getOrPut(clazz) { AtomicInteger(0) }
            
            if (pool.isNotEmpty()) {
                val obj = pool.removeAt(pool.size - 1) as T
                counter.decrementAndGet()
                Log.d(TAG, "Object retrieved from pool: ${clazz.simpleName}")
                obj
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter objeto do pool: ${e.message}")
            null
        }
    }
    
    /**
     * Retorna objeto para o pool
     */
    fun <T : Any> returnToPool(obj: T, clazz: Class<T>) {
        try {
            val pool = objectPools.getOrPut(clazz) { mutableListOf() }
            val counter = poolCounters.getOrPut(clazz) { AtomicInteger(0) }
            
            if (counter.get() < MAX_OBJECT_POOL_SIZE) {
                pool.add(obj)
                counter.incrementAndGet()
                Log.d(TAG, "Object returned to pool: ${clazz.simpleName}")
            } else {
                Log.d(TAG, "Pool full, discarding object: ${clazz.simpleName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao retornar objeto ao pool: ${e.message}")
        }
    }
    
    /**
     * Gerencia referências fracas
     */
    fun setWeakReference(key: String, obj: Any) {
        weakReferences[key] = WeakReference(obj)
        Log.d(TAG, "Weak reference set: $key")
    }
    
    /**
     * Obtém referência fraca
     */
    fun getWeakReference(key: String): Any? {
        val weakRef = weakReferences[key]
        val obj = weakRef?.get()
        if (obj == null) {
            weakReferences.remove(key)
            Log.d(TAG, "Weak reference cleared: $key")
        }
        return obj
    }
    
    /**
     * Limpa referências fracas nulas
     */
    fun cleanupWeakReferences() {
        val iterator = weakReferences.iterator()
        var cleaned = 0
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.get() == null) {
                iterator.remove()
                cleaned++
            }
        }
        if (cleaned > 0) {
            Log.d(TAG, "Cleaned up $cleaned weak references")
        }
    }
    
    /**
     * Força garbage collection
     */
    fun forceGarbageCollection() {
        try {
            System.gc()
            Log.d(TAG, "Garbage collection triggered")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao forçar garbage collection: ${e.message}")
        }
    }
    
    /**
     * Obtém estatísticas de memória
     */
    fun getMemoryStats(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        
        return MemoryStats(
            maxMemory = maxMemory,
            totalMemory = totalMemory,
            freeMemory = freeMemory,
            usedMemory = usedMemory,
            cacheSize = bitmapCache.snapshot().size,
            cacheMaxSize = bitmapCache.maxSize(),
            poolCount = objectPools.values.sumOf { it.size },
            weakReferenceCount = weakReferences.size
        )
    }
    
    /**
     * Limpa todos os caches
     */
    fun clearAllCaches() {
        try {
            // Limpar cache de bitmaps
            val snapshot = bitmapCache.snapshot()
            for (key in snapshot.keys) {
                val bitmap = bitmapCache.get(key)
                if (bitmap != null) {
                    bitmap.recycle()
                }
            }
            objectPools.clear()
            poolCounters.clear()
            weakReferences.clear()
            forceGarbageCollection()
            Log.d(TAG, "All caches cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao limpar caches: ${e.message}")
        }
    }
    
    /**
     * Data class para estatísticas de memória
     */
    data class MemoryStats(
        val maxMemory: Long,
        val totalMemory: Long,
        val freeMemory: Long,
        val usedMemory: Long,
        val cacheSize: Int,
        val cacheMaxSize: Int,
        val poolCount: Int,
        val weakReferenceCount: Int
    ) {
        val memoryUsagePercent: Float = (usedMemory.toFloat() / maxMemory.toFloat()) * 100
        val cacheUsagePercent: Float = (cacheSize.toFloat() / cacheMaxSize.toFloat()) * 100
    }
}
