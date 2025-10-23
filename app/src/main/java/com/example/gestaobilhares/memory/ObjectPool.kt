package com.example.gestaobilhares.memory

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * ✅ FASE 4D: Pool de Objetos Reutilizáveis
 * 
 * Gerencia pools de objetos para reduzir garbage collection
 * Seguindo Android 2025 best practices
 */
class ObjectPool<T : Any> private constructor(
    private val factory: () -> T,
    private val reset: (T) -> Unit = {},
    private val maxSize: Int = 100
) {
    
    companion object {
        private const val TAG = "ObjectPool"
        private val pools = ConcurrentHashMap<Class<*>, ObjectPool<*>>()
        
        /**
         * Cria ou obtém um pool para uma classe específica
         */
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> getPool(
            clazz: Class<T>,
            factory: () -> T,
            reset: (T) -> Unit = {},
            maxSize: Int = 100
        ): ObjectPool<T> {
            return pools.getOrPut(clazz) {
                ObjectPool(factory, reset, maxSize)
            } as ObjectPool<T>
        }
        
        /**
         * Limpa todos os pools
         */
        fun clearAllPools() {
            pools.clear()
            Log.d(TAG, "All object pools cleared")
        }
    }
    
    private val pool = mutableListOf<T>()
    private val lock = ReentrantLock()
    private val count = AtomicInteger(0)
    private val created = AtomicInteger(0)
    private val reused = AtomicInteger(0)
    
    /**
     * Obtém um objeto do pool
     */
    fun acquire(): T {
        return lock.withLock {
            if (pool.isNotEmpty()) {
                val obj = pool.removeAt(pool.size - 1)
                count.decrementAndGet()
                reused.incrementAndGet()
                Log.d(TAG, "Object reused from pool: ${obj::class.simpleName}")
                obj
            } else {
                val obj = factory()
                created.incrementAndGet()
                Log.d(TAG, "New object created: ${obj::class.simpleName}")
                obj
            }
        }
    }
    
    /**
     * Retorna um objeto para o pool
     */
    fun release(obj: T) {
        lock.withLock {
            if (count.get() < maxSize) {
                reset(obj)
                pool.add(obj)
                count.incrementAndGet()
                Log.d(TAG, "Object returned to pool: ${obj::class.simpleName}")
            } else {
                Log.d(TAG, "Pool full, discarding object: ${obj::class.simpleName}")
            }
        }
    }
    
    /**
     * Obtém estatísticas do pool
     */
    fun getStats(): PoolStats {
        return PoolStats(
            currentSize = count.get(),
            maxSize = maxSize,
            totalCreated = created.get(),
            totalReused = reused.get(),
            poolUtilization = (count.get().toFloat() / maxSize.toFloat()) * 100
        )
    }
    
    /**
     * Limpa o pool
     */
    fun clear() {
        lock.withLock {
            pool.clear()
            count.set(0)
            Log.d(TAG, "Pool cleared")
        }
    }
    
    /**
     * Data class para estatísticas
     */
    data class PoolStats(
        val currentSize: Int,
        val maxSize: Int,
        val totalCreated: Int,
        val totalReused: Int,
        val poolUtilization: Float
    )
}

/**
 * Extensões para facilitar o uso do ObjectPool
 */
inline fun <reified T : Any> ObjectPool.Companion.getPool(
    noinline factory: () -> T,
    noinline reset: (T) -> Unit = {},
    maxSize: Int = 100
): ObjectPool<T> {
    return getPool(T::class.java, factory, reset, maxSize)
}

/**
 * Pool específico para StringBuilders
 */
object StringBuilderPool {
    private val pool = ObjectPool.getPool(
        clazz = StringBuilder::class.java,
        factory = { StringBuilder() },
        reset = { it.clear() },
        maxSize = 50
    )
    
    fun acquire(): StringBuilder = pool.acquire()
    fun release(sb: StringBuilder) = pool.release(sb)
    fun getStats() = pool.getStats()
}

/**
 * Pool específico para ArrayLists
 */
object ArrayListPool {
    private val pool = ObjectPool.getPool(
        clazz = ArrayList::class.java,
        factory = { ArrayList<Any>() },
        reset = { it.clear() },
        maxSize = 30
    )
    
    @Suppress("UNCHECKED_CAST")
    fun <T> acquire(): ArrayList<T> = pool.acquire() as ArrayList<T>
    
    @Suppress("UNCHECKED_CAST")
    fun <T> release(list: ArrayList<T>) = pool.release(list as ArrayList<Any>)
    
    fun getStats() = pool.getStats()
}
