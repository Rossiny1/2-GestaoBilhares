package com.example.gestaobilhares.ui.optimization

import android.view.View
import android.view.ViewStub
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * ✅ FASE 4D: Gerenciador de ViewStub para carregamento lazy
 * Seguindo Android 2025 best practices para otimização de UI
 * 
 * Funcionalidades:
 * - Carregamento lazy de layouts pesados
 * - Cache de views infladas
 * - Gerenciamento de memória otimizado
 * - Prevenção de inflação desnecessária
 */
class ViewStubManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: ViewStubManager? = null

        fun getInstance(): ViewStubManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ViewStubManager().also { INSTANCE = it }
            }
        }

        private const val TAG = "ViewStubManager"
    }

    // Cache de views infladas para reutilização
    private val inflatedViews = ConcurrentHashMap<String, View>()
    
    // Controle de inflação em andamento
    private val inflatingViews = ConcurrentHashMap<String, Boolean>()

    /**
     * Infla um ViewStub de forma lazy e otimizada
     */
    fun inflateViewStub(viewStub: ViewStub, tag: String): View? {
        return try {
            // Verificar se já está inflado
            inflatedViews[tag]?.let { return it }

            // Verificar se já está sendo inflado
            if (inflatingViews[tag] == true) {
                Log.d(TAG, "ViewStub já está sendo inflado: $tag")
                return null
            }

            // Marcar como inflando
            inflatingViews[tag] = true

            // Inflar o ViewStub
            val inflatedView = viewStub.inflate()
            inflatedView.tag = tag

            // Cachear a view
            inflatedViews[tag] = inflatedView

            // Remover do controle de inflação
            inflatingViews.remove(tag)

            Log.d(TAG, "ViewStub inflado com sucesso: $tag")
            inflatedView

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inflar ViewStub $tag: ${e.message}", e)
            inflatingViews.remove(tag)
            null
        }
    }

    /**
     * Obtém uma view já inflada do cache
     */
    fun getInflatedView(tag: String): View? {
        return inflatedViews[tag]
    }

    /**
     * Verifica se uma view está inflada
     */
    fun isViewInflated(tag: String): Boolean {
        return inflatedViews.containsKey(tag)
    }

    /**
     * Remove uma view do cache (útil para liberação de memória)
     */
    fun removeInflatedView(tag: String) {
        inflatedViews.remove(tag)?.let { view ->
            // Limpar referências
            view.tag = null
            Log.d(TAG, "View removida do cache: $tag")
        }
    }

    /**
     * Limpa todas as views do cache
     */
    fun clearAllViews() {
        inflatedViews.values.forEach { view ->
            view.tag = null
        }
        inflatedViews.clear()
        inflatingViews.clear()
        Log.d(TAG, "Todos os caches de ViewStub limpos")
    }

    /**
     * Obtém estatísticas do cache
     */
    fun getCacheStats(): ViewStubStats {
        return ViewStubStats(
            totalInflatedViews = inflatedViews.size,
            inflatingViews = inflatingViews.size,
            cacheKeys = inflatedViews.keys.toList()
        )
    }

    /**
     * Data class para estatísticas do ViewStub
     */
    data class ViewStubStats(
        val totalInflatedViews: Int,
        val inflatingViews: Int,
        val cacheKeys: List<String>
    )
}
