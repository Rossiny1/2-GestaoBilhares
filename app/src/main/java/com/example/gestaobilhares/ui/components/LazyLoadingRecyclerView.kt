package com.example.gestaobilhares.ui.components

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.core.utils.PaginationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ✅ FASE 4B: RecyclerView com Lazy Loading
 * 
 * RecyclerView otimizado com carregamento sob demanda
 * - Detecção automática de scroll
 * - Pré-carregamento inteligente
 * - Otimização de performance
 * - Integração com PaginationManager
 */
class LazyLoadingRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {
    
    private var paginationManager: PaginationManager<*>? = null
    private var coroutineScope: CoroutineScope? = null
    private var isLoading = false
    
    // Callback para carregar mais dados
    private var onLoadMoreCallback: (() -> Unit)? = null
    
    // Threshold para pré-carregamento (últimos 5 itens)
    private var preloadThreshold = 5
    
    init {
        setupScrollListener()
    }
    
    /**
     * Configurar PaginationManager
     */
    fun <T> setPaginationManager(
        manager: PaginationManager<T>,
        scope: CoroutineScope
    ) {
        this.paginationManager = manager
        this.coroutineScope = scope
    }
    
    /**
     * Configurar callback para carregar mais dados
     */
    fun setOnLoadMoreCallback(callback: () -> Unit) {
        onLoadMoreCallback = callback
    }
    
    /**
     * Configurar threshold de pré-carregamento
     */
    fun setPreloadThreshold(threshold: Int) {
        preloadThreshold = threshold
    }
    
    /**
     * Configurar listener de scroll
     */
    private fun setupScrollListener() {
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                if (dy > 0) { // Scrolling down
                    checkAndLoadMore()
                }
            }
        })
    }
    
    /**
     * Verificar se deve carregar mais dados
     */
    private fun checkAndLoadMore() {
        val layoutManager = layoutManager as? LinearLayoutManager ?: return
        val paginationManager = this.paginationManager ?: return
        val coroutineScope = this.coroutineScope ?: return
        
        val totalItemCount = layoutManager.itemCount
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
        
        // Verificar se está próximo do final
        if (lastVisibleItemPosition >= totalItemCount - preloadThreshold) {
            if (!isLoading && paginationManager.hasMoreData.value) {
                loadMoreData(coroutineScope, paginationManager)
            }
        }
    }
    
    /**
     * Carregar mais dados
     */
    private fun loadMoreData(
        scope: CoroutineScope,
        paginationManager: PaginationManager<*>
    ) {
        if (isLoading) return
        
        isLoading = true
        Log.d("LazyLoadingRecyclerView", "🔄 Carregando mais dados...")
        
        scope.launch(Dispatchers.Main) {
            try {
                // Chamar callback personalizado se configurado
                onLoadMoreCallback?.invoke()
                
                Log.d("LazyLoadingRecyclerView", "✅ Mais dados carregados")
            } catch (e: Exception) {
                Log.e("LazyLoadingRecyclerView", "❌ Erro ao carregar mais dados: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Marcar como carregando
     */
    fun setLoading(loading: Boolean) {
        isLoading = loading
    }
    
    /**
     * Verificar se está carregando
     */
    fun isCurrentlyLoading(): Boolean = isLoading
    
    /**
     * Scroll suave para posição específica
     */
    fun smoothScrollToPositionSafely(position: Int) {
        if (position >= 0 && position < (adapter?.itemCount ?: 0)) {
            smoothScrollToPosition(position)
        }
    }
    
    /**
     * Scroll para o topo
     */
    fun scrollToTop() {
        smoothScrollToPositionSafely(0)
    }
    
    /**
     * Scroll para o final
     */
    fun scrollToBottom() {
        val itemCount = adapter?.itemCount ?: 0
        if (itemCount > 0) {
            smoothScrollToPositionSafely(itemCount - 1)
        }
    }
    
    /**
     * Obter posição do primeiro item visível
     */
    fun getFirstVisiblePosition(): Int {
        val layoutManager = layoutManager as? LinearLayoutManager ?: return 0
        return layoutManager.findFirstVisibleItemPosition()
    }
    
    /**
     * Obter posição do último item visível
     */
    fun getLastVisiblePosition(): Int {
        val layoutManager = layoutManager as? LinearLayoutManager ?: return 0
        return layoutManager.findLastVisibleItemPosition()
    }
    
    /**
     * Verificar se está no topo
     */
    fun isAtTop(): Boolean {
        return getFirstVisiblePosition() == 0
    }
    
    /**
     * Verificar se está no final
     */
    fun isAtBottom(): Boolean {
        val itemCount = adapter?.itemCount ?: 0
        return getLastVisiblePosition() >= itemCount - 1
    }
}
