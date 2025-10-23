package com.example.gestaobilhares.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ✅ FASE 4B: Gerenciador de Paginação Inteligente
 * 
 * Gerencia paginação de dados para otimizar performance
 * - Carregamento sob demanda
 * - Paginação inteligente
 * - Cache de páginas
 * - Otimização de RecyclerView
 */
class PaginationManager<T>(
    private val pageSize: Int = 20,
    private val preloadThreshold: Int = 5
) {
    
    // Estado da paginação
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _hasMoreData = MutableStateFlow(true)
    val hasMoreData: StateFlow<Boolean> = _hasMoreData.asStateFlow()
    
    private val _totalItems = MutableStateFlow(0)
    val totalItems: StateFlow<Int> = _totalItems.asStateFlow()
    
    // Cache de páginas carregadas
    private val loadedPages = mutableMapOf<Int, List<T>>()
    
    // Callback para carregar dados
    private var loadDataCallback: suspend (Int, Int) -> List<T> = { _, _ -> emptyList() }
    
    /**
     * Configurar callback para carregar dados
     */
    fun setLoadDataCallback(callback: suspend (offset: Int, limit: Int) -> List<T>) {
        loadDataCallback = callback
    }
    
    /**
     * Carregar página específica
     */
    suspend fun loadPage(page: Int): List<T> {
        if (loadedPages.containsKey(page)) {
            Log.d("PaginationManager", "✅ Página $page já carregada (cache)")
            return loadedPages[page]!!
        }
        
        _isLoading.value = true
        try {
            val offset = page * pageSize
            val data = loadDataCallback(offset, pageSize)
            
            loadedPages[page] = data
            _currentPage.value = page
            _hasMoreData.value = data.size == pageSize
            _totalItems.value = offset + data.size
            
            Log.d("PaginationManager", "✅ Página $page carregada: ${data.size} itens")
            return data
        } catch (e: Exception) {
            Log.e("PaginationManager", "❌ Erro ao carregar página $page: ${e.message}")
            throw e
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Carregar próxima página
     */
    suspend fun loadNextPage(): List<T> {
        val nextPage = _currentPage.value + 1
        return loadPage(nextPage)
    }
    
    /**
     * Carregar página inicial
     */
    suspend fun loadInitialPage(): List<T> {
        return loadPage(0)
    }
    
    /**
     * Verificar se deve pré-carregar próxima página
     */
    fun shouldPreloadNextPage(currentPosition: Int): Boolean {
        val currentPageStart = _currentPage.value * pageSize
        val positionInPage = currentPosition - currentPageStart
        return positionInPage >= (pageSize - preloadThreshold) && _hasMoreData.value
    }
    
    /**
     * Obter todos os dados carregados até agora
     */
    fun getAllLoadedData(): List<T> {
        val allData = mutableListOf<T>()
        for (page in 0.._currentPage.value) {
            loadedPages[page]?.let { allData.addAll(it) }
        }
        return allData
    }
    
    /**
     * Obter dados de uma página específica
     */
    fun getPageData(page: Int): List<T>? {
        return loadedPages[page]
    }
    
    /**
     * Limpar cache de páginas
     */
    fun clearCache() {
        loadedPages.clear()
        _currentPage.value = 0
        _hasMoreData.value = true
        _totalItems.value = 0
        Log.d("PaginationManager", "🧹 Cache de paginação limpo")
    }
    
    /**
     * Resetar paginação
     */
    fun reset() {
        clearCache()
        _isLoading.value = false
        Log.d("PaginationManager", "🔄 Paginação resetada")
    }
    
    /**
     * Obter estatísticas da paginação
     */
    fun getStats(): String {
        return "Página: ${_currentPage.value}, Itens: ${_totalItems.value}, Páginas carregadas: ${loadedPages.size}, Tem mais: ${_hasMoreData.value}"
    }
}
