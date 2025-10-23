package com.example.gestaobilhares.memory

import android.util.Log
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * ✅ FASE 4D: Gerenciador de WeakReferences
 * 
 * Centraliza o gerenciamento de referências fracas para evitar memory leaks
 * Seguindo Android 2025 best practices
 */
class WeakReferenceManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: WeakReferenceManager? = null
        
        fun getInstance(): WeakReferenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WeakReferenceManager().also { INSTANCE = it }
            }
        }
        
        private const val TAG = "WeakReferenceManager"
    }
    
    // Mapa de referências fracas por categoria
    private val weakReferences = ConcurrentHashMap<String, WeakReference<Any>>()
    
    // Callbacks para limpeza automática
    private val cleanupCallbacks = mutableListOf<() -> Unit>()
    
    /**
     * Adiciona uma referência fraca
     */
    fun <T : Any> addWeakReference(key: String, obj: T): WeakReference<T> {
        val weakRef = WeakReference(obj)
        weakReferences[key] = weakRef as WeakReference<Any>
        Log.d(TAG, "Weak reference added: $key")
        return weakRef
    }
    
    /**
     * Obtém uma referência fraca
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getWeakReference(key: String): T? {
        val weakRef = weakReferences[key] ?: return null
        val obj = weakRef.get()
        if (obj == null) {
            weakReferences.remove(key)
            Log.d(TAG, "Weak reference cleared: $key")
        }
        return obj as? T
    }
    
    /**
     * Remove uma referência fraca
     */
    fun removeWeakReference(key: String) {
        weakReferences.remove(key)
        Log.d(TAG, "Weak reference removed: $key")
    }
    
    /**
     * Verifica se uma referência ainda existe
     */
    fun isReferenceAlive(key: String): Boolean {
        val weakRef = weakReferences[key] ?: return false
        val isAlive = weakRef.get() != null
        if (!isAlive) {
            weakReferences.remove(key)
        }
        return isAlive
    }
    
    /**
     * Limpa todas as referências nulas
     */
    fun cleanupNullReferences() {
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
            Log.d(TAG, "Cleaned up $cleaned null references")
        }
    }
    
    /**
     * Adiciona callback de limpeza
     */
    fun addCleanupCallback(callback: () -> Unit) {
        cleanupCallbacks.add(callback)
    }
    
    /**
     * Executa callbacks de limpeza
     */
    fun executeCleanupCallbacks() {
        cleanupCallbacks.forEach { callback ->
            try {
                callback()
            } catch (e: Exception) {
                Log.e(TAG, "Erro em callback de limpeza: ${e.message}")
            }
        }
    }
    
    /**
     * Obtém estatísticas das referências
     */
    fun getReferenceStats(): ReferenceStats {
        val total = weakReferences.size
        var alive = 0
        var nullRefs = 0
        
        weakReferences.values.forEach { weakRef ->
            if (weakRef.get() != null) {
                alive++
            } else {
                nullRefs++
            }
        }
        
        return ReferenceStats(
            totalReferences = total,
            aliveReferences = alive,
            nullReferences = nullRefs,
            cleanupCallbacks = cleanupCallbacks.size
        )
    }
    
    /**
     * Limpa todas as referências
     */
    fun clearAllReferences() {
        weakReferences.clear()
        cleanupCallbacks.clear()
        Log.d(TAG, "All references cleared")
    }
    
    /**
     * Data class para estatísticas
     */
    data class ReferenceStats(
        val totalReferences: Int,
        val aliveReferences: Int,
        val nullReferences: Int,
        val cleanupCallbacks: Int
    )
}
