package com.example.gestaobilhares.ui.optimization

import android.view.View
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.lang.ref.WeakReference

/**
 * ✅ FASE 4D: ViewHolder Pattern Otimizado
 * Seguindo Android 2025 best practices para RecyclerView performance
 * 
 * Funcionalidades:
 * - Pool de ViewHolders reutilizáveis
 * - WeakReference para prevenir memory leaks
 * - Binding otimizado com cache
 * - Prevenção de findViewById repetitivo
 */
class OptimizedViewHolder private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: OptimizedViewHolder? = null

        fun getInstance(): OptimizedViewHolder {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OptimizedViewHolder().also { INSTANCE = it }
            }
        }

        private const val TAG = "OptimizedViewHolder"
        private const val MAX_POOL_SIZE = 50
    }

    // Pool de ViewHolders por tipo
    private val viewHolderPools = ConcurrentHashMap<Class<*>, MutableList<WeakReference<Any>>>()
    
    // Cache de views por ViewHolder
    private val viewCache = ConcurrentHashMap<String, WeakReference<View>>()

    /**
     * Adiciona um ViewHolder ao pool para reutilização
     */
    fun <T : Any> addToPool(viewHolder: T) {
        val clazz = viewHolder.javaClass
        val pool = viewHolderPools.getOrPut(clazz) { mutableListOf() }
        
        // Limpar referências nulas do pool
        pool.removeAll { it.get() == null }
        
        // Adicionar se não exceder o limite
        if (pool.size < MAX_POOL_SIZE) {
            pool.add(WeakReference(viewHolder))
            Log.d(TAG, "ViewHolder adicionado ao pool: ${clazz.simpleName}, total: ${pool.size}")
        } else {
            Log.d(TAG, "Pool cheio para ${clazz.simpleName}, descartando ViewHolder")
        }
    }

    /**
     * Obtém um ViewHolder do pool ou cria um novo
     */
    fun <T : Any> getFromPool(clazz: Class<T>, factory: () -> T): T {
        val pool = viewHolderPools[clazz] ?: mutableListOf()
        
        // Procurar ViewHolder válido no pool
        val iterator = pool.iterator()
        while (iterator.hasNext()) {
            val weakRef = iterator.next()
            val viewHolder = weakRef.get()
            
            if (viewHolder != null) {
                iterator.remove()
                @Suppress("UNCHECKED_CAST")
                Log.d(TAG, "ViewHolder reutilizado do pool: ${clazz.simpleName}")
                return viewHolder as T
            } else {
                iterator.remove() // Remover referência nula
            }
        }
        
        // Criar novo ViewHolder se não encontrado no pool
        val newViewHolder = factory()
        Log.d(TAG, "Novo ViewHolder criado: ${clazz.simpleName}")
        return newViewHolder
    }

    /**
     * Cacheia uma view para evitar findViewById repetitivo
     */
    fun cacheView(viewHolderTag: String, viewId: Int, view: View) {
        val cacheKey = "${viewHolderTag}_$viewId"
        viewCache[cacheKey] = WeakReference(view)
        Log.d(TAG, "View cacheada: $cacheKey")
    }

    /**
     * Obtém uma view do cache
     */
    fun getCachedView(viewHolderTag: String, viewId: Int): View? {
        val cacheKey = "${viewHolderTag}_$viewId"
        val weakRef = viewCache[cacheKey]
        val view = weakRef?.get()
        
        if (view == null) {
            viewCache.remove(cacheKey) // Limpar referência nula
            Log.d(TAG, "View não encontrada no cache: $cacheKey")
        } else {
            Log.d(TAG, "View encontrada no cache: $cacheKey")
        }
        
        return view
    }

    /**
     * Limpa o cache de views de um ViewHolder específico
     */
    fun clearViewHolderCache(viewHolderTag: String) {
        val keysToRemove = viewCache.keys.filter { it.startsWith(viewHolderTag) }
        keysToRemove.forEach { key ->
            viewCache.remove(key)
        }
        Log.d(TAG, "Cache limpo para ViewHolder: $viewHolderTag, ${keysToRemove.size} views removidas")
    }

    /**
     * Limpa todos os pools e caches
     */
    fun clearAll() {
        // Limpar pools
        viewHolderPools.values.forEach { pool ->
            pool.clear()
        }
        viewHolderPools.clear()
        
        // Limpar cache de views
        viewCache.clear()
        
        Log.d(TAG, "Todos os pools e caches limpos")
    }

    /**
     * Limpa referências nulas de todos os pools
     */
    fun cleanupNullReferences() {
        var totalRemoved = 0
        
        viewHolderPools.values.forEach { pool ->
            val iterator = pool.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().get() == null) {
                    iterator.remove()
                    totalRemoved++
                }
            }
        }
        
        // Limpar cache de views
        val viewIterator = viewCache.entries.iterator()
        while (viewIterator.hasNext()) {
            if (viewIterator.next().value.get() == null) {
                viewIterator.remove()
                totalRemoved++
            }
        }
        
        Log.d(TAG, "Limpeza concluída: $totalRemoved referências nulas removidas")
    }

    /**
     * Obtém estatísticas dos pools
     */
    fun getPoolStats(): ViewHolderStats {
        val poolStats = viewHolderPools.mapValues { (_, pool) ->
            pool.count { it.get() != null }
        }
        
        val totalCachedViews = viewCache.count { it.value.get() != null }
        
        return ViewHolderStats(
            totalPools = viewHolderPools.size,
            poolStats = poolStats,
            totalCachedViews = totalCachedViews,
            totalCacheEntries = viewCache.size
        )
    }

    /**
     * Data class para estatísticas do ViewHolder
     */
    data class ViewHolderStats(
        val totalPools: Int,
        val poolStats: Map<Class<*>, Int>,
        val totalCachedViews: Int,
        val totalCacheEntries: Int
    )
}
