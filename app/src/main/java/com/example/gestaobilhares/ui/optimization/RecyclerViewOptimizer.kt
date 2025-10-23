package com.example.gestaobilhares.ui.optimization

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * ✅ FASE 4D: Otimizador de RecyclerView
 * Seguindo Android 2025 best practices para performance de listas
 * 
 * Funcionalidades:
 * - Configuração automática de performance
 * - Otimização de scroll
 * - Cache inteligente de itens
 * - Prevenção de layout thrashing
 */
class RecyclerViewOptimizer private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: RecyclerViewOptimizer? = null

        fun getInstance(): RecyclerViewOptimizer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RecyclerViewOptimizer().also { INSTANCE = it }
            }
        }

        private const val TAG = "RecyclerViewOptimizer"
    }

    // Cache de configurações otimizadas
    private val optimizedRecyclerViews = ConcurrentHashMap<String, RecyclerViewConfig>()

    /**
     * Otimiza um RecyclerView automaticamente
     */
    fun optimizeRecyclerView(recyclerView: RecyclerView, config: RecyclerViewConfig = RecyclerViewConfig()) {
        try {
            val recyclerViewId = recyclerView.id.toString()
            
            // Configurações básicas de performance
            recyclerView.setHasFixedSize(true)
            recyclerView.setItemViewCacheSize(config.itemViewCacheSize)
            recyclerView.setDrawingCacheEnabled(true)
            recyclerView.setDrawingCacheQuality(RecyclerView.DRAWING_CACHE_QUALITY_HIGH)
            
            // Otimizar LayoutManager
            optimizeLayoutManager(recyclerView, config)
            
            // Configurar scroll otimizado
            optimizeScrolling(recyclerView, config)
            
            // Configurar nested scrolling
            recyclerView.isNestedScrollingEnabled = config.nestedScrollingEnabled
            
            // Cachear configuração
            optimizedRecyclerViews[recyclerViewId] = config
            
            Log.d(TAG, "RecyclerView otimizado: $recyclerViewId, cacheSize=${config.itemViewCacheSize}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao otimizar RecyclerView: ${e.message}", e)
        }
    }

    /**
     * Otimiza o LayoutManager baseado no tipo
     */
    private fun optimizeLayoutManager(recyclerView: RecyclerView, config: RecyclerViewConfig) {
        val layoutManager = recyclerView.layoutManager
        
        when (layoutManager) {
            is LinearLayoutManager -> {
                // Otimizar LinearLayoutManager
                layoutManager.isItemPrefetchEnabled = true
                // initialPrefetchItemCount não está disponível em todas as versões
                // layoutManager.initialPrefetchItemCount = config.prefetchItemCount
                
                // Configurar orientação
                if (config.orientation != null) {
                    layoutManager.orientation = config.orientation
                }
                
                // Configurar reverse layout
                if (config.reverseLayout) {
                    layoutManager.reverseLayout = true
                }
                
                Log.d(TAG, "LinearLayoutManager otimizado: prefetch=${config.prefetchItemCount}")
            }
            
            is GridLayoutManager -> {
                // Otimizar GridLayoutManager
                layoutManager.isItemPrefetchEnabled = true
                // initialPrefetchItemCount não está disponível em todas as versões
                // layoutManager.initialPrefetchItemCount = config.prefetchItemCount
                
                // Configurar span count
                if (config.spanCount > 0) {
                    layoutManager.spanCount = config.spanCount
                }
                
                Log.d(TAG, "GridLayoutManager otimizado: spanCount=${config.spanCount}, prefetch=${config.prefetchItemCount}")
            }
            
            is StaggeredGridLayoutManager -> {
                // Otimizar StaggeredGridLayoutManager
                layoutManager.isItemPrefetchEnabled = true
                // initialPrefetchItemCount não está disponível em todas as versões
                // layoutManager.initialPrefetchItemCount = config.prefetchItemCount
                
                Log.d(TAG, "StaggeredGridLayoutManager otimizado: prefetch=${config.prefetchItemCount}")
            }
        }
    }

    /**
     * Otimiza o comportamento de scroll
     */
    private fun optimizeScrolling(recyclerView: RecyclerView, config: RecyclerViewConfig) {
        // Configurar smooth scroll
        if (config.smoothScrollEnabled) {
            recyclerView.smoothScrollToPosition(0)
        }
        
        // Configurar scroll listener para otimizações dinâmicas
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        // Scroll parado - otimizar cache
                        optimizeCacheOnIdle(recyclerView)
                    }
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        // Scroll manual - reduzir cache
                        reduceCacheOnScrolling(recyclerView)
                    }
                    RecyclerView.SCROLL_STATE_SETTLING -> {
                        // Scroll automático - manter cache
                        maintainCacheOnSettling(recyclerView)
                    }
                }
            }
            
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                // Otimizações durante o scroll
                if (config.optimizeDuringScroll) {
                    optimizeDuringScroll(recyclerView, dx, dy)
                }
            }
        })
    }

    /**
     * Otimiza cache quando scroll está parado
     */
    private fun optimizeCacheOnIdle(recyclerView: RecyclerView) {
        // Aumentar cache quando parado
        recyclerView.setItemViewCacheSize(30)
        Log.d(TAG, "Cache otimizado para scroll idle: cacheSize=30")
    }

    /**
     * Reduz cache durante scroll manual
     */
    private fun reduceCacheOnScrolling(recyclerView: RecyclerView) {
        // Reduzir cache durante scroll para melhor performance
        recyclerView.setItemViewCacheSize(10)
        Log.d(TAG, "Cache reduzido para scroll manual: cacheSize=10")
    }

    /**
     * Mantém cache durante scroll automático
     */
    private fun maintainCacheOnSettling(recyclerView: RecyclerView) {
        // Manter cache intermediário
        recyclerView.setItemViewCacheSize(20)
        Log.d(TAG, "Cache mantido para scroll settling: cacheSize=20")
    }

    /**
     * Otimizações durante o scroll
     */
    private fun optimizeDuringScroll(recyclerView: RecyclerView, dx: Int, dy: Int) {
        // Verificar se scroll é muito rápido
        val scrollSpeed = kotlin.math.abs(dx) + kotlin.math.abs(dy)
        if (scrollSpeed > 50) {
            // Scroll rápido - reduzir qualidade de cache
            recyclerView.setDrawingCacheQuality(RecyclerView.DRAWING_CACHE_QUALITY_LOW)
        } else {
            // Scroll lento - manter qualidade alta
            recyclerView.setDrawingCacheQuality(RecyclerView.DRAWING_CACHE_QUALITY_HIGH)
        }
    }

    /**
     * Configura RecyclerView para listas grandes
     */
    fun optimizeForLargeLists(recyclerView: RecyclerView) {
        val config = RecyclerViewConfig(
            itemViewCacheSize = 50,
            prefetchItemCount = 4,
            nestedScrollingEnabled = true,
            smoothScrollEnabled = false, // Desabilitar para listas grandes
            optimizeDuringScroll = true
        )
        
        optimizeRecyclerView(recyclerView, config)
        Log.d(TAG, "RecyclerView otimizado para listas grandes")
    }

    /**
     * Configura RecyclerView para listas pequenas
     */
    fun optimizeForSmallLists(recyclerView: RecyclerView) {
        val config = RecyclerViewConfig(
            itemViewCacheSize = 20,
            prefetchItemCount = 2,
            nestedScrollingEnabled = true,
            smoothScrollEnabled = true,
            optimizeDuringScroll = false
        )
        
        optimizeRecyclerView(recyclerView, config)
        Log.d(TAG, "RecyclerView otimizado para listas pequenas")
    }

    /**
     * Obtém configuração de um RecyclerView
     */
    fun getRecyclerViewConfig(recyclerView: RecyclerView): RecyclerViewConfig? {
        val recyclerViewId = recyclerView.id.toString()
        return optimizedRecyclerViews[recyclerViewId]
    }

    /**
     * Limpa todas as configurações
     */
    fun clearAllConfigs() {
        optimizedRecyclerViews.clear()
        Log.d(TAG, "Todas as configurações de RecyclerView limpas")
    }

    /**
     * Data class para configuração de RecyclerView
     */
    data class RecyclerViewConfig(
        val itemViewCacheSize: Int = 20,
        val prefetchItemCount: Int = 2,
        val spanCount: Int = 1,
        val orientation: Int? = null,
        val reverseLayout: Boolean = false,
        val nestedScrollingEnabled: Boolean = true,
        val smoothScrollEnabled: Boolean = true,
        val optimizeDuringScroll: Boolean = true
    )
}
