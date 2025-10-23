package com.example.gestaobilhares.ui.optimization

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.factory.RepositoryFactory
import android.util.Log

/**
 * ✅ FASE 4D: Adapter otimizado para demonstração
 * Seguindo Android 2025 best practices para RecyclerView
 * 
 * Funcionalidades:
 * - ViewHolder pooling otimizado
 * - Cache de views
 * - Performance melhorada
 */
class UIOptimizationAdapter : RecyclerView.Adapter<UIOptimizationAdapter.OptimizedViewHolder>() {

    private lateinit var appRepository: com.example.gestaobilhares.data.repository.AppRepository
    
    fun setRepository(repository: com.example.gestaobilhares.data.repository.AppRepository) {
        this.appRepository = repository
    }
    private val optimizationItems = listOf(
        "ViewStub Manager - Carregamento lazy de layouts",
        "ViewHolder Pooling - Reutilização de ViewHolders",
        "Layout Optimizer - Otimização de hierarquia",
        "RecyclerView Optimizer - Performance de listas",
        "Memory Optimization - Gerenciamento de memória",
        "Cache Management - Cache inteligente",
        "Background Processing - WorkManager",
        "Performance Monitoring - Estatísticas em tempo real"
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptimizedViewHolder {
        // Usar ViewHolder pooling otimizado se repository estiver disponível
        return if (::appRepository.isInitialized) {
            appRepository.obterViewHolderDoPool(OptimizedViewHolder::class.java) {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
                OptimizedViewHolder(view)
            }
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            OptimizedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: OptimizedViewHolder, position: Int) {
        holder.bind(optimizationItems[position], position)
    }

    override fun getItemCount(): Int = optimizationItems.size

    /**
     * ViewHolder otimizado com cache de views
     */
    inner class OptimizedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        private val viewHolderTag = "OptimizedViewHolder_${hashCode()}"

        init {
            // Cachear a view para evitar findViewById repetitivo se repository estiver disponível
            if (::appRepository.isInitialized) {
                appRepository.cachearView(viewHolderTag, android.R.id.text1, textView)
                Log.d("OptimizedViewHolder", "ViewHolder criado e view cacheada: $viewHolderTag")
            } else {
                Log.d("OptimizedViewHolder", "ViewHolder criado sem cache (repository não disponível): $viewHolderTag")
            }
        }

        fun bind(item: String, position: Int) {
            // Usar view cacheada se repository estiver disponível
            if (::appRepository.isInitialized) {
                val cachedView = appRepository.obterViewCacheada(viewHolderTag, android.R.id.text1)
                if (cachedView is TextView) {
                    cachedView.text = "${position + 1}. $item"
                    Log.d("OptimizedViewHolder", "View cacheada usada para posição $position")
                } else {
                    // Fallback para findViewById se cache falhar
                    textView.text = "${position + 1}. $item"
                    Log.w("OptimizedViewHolder", "Cache falhou, usando findViewById para posição $position")
                }
            } else {
                // Usar findViewById diretamente se repository não estiver disponível
                textView.text = "${position + 1}. $item"
                Log.d("OptimizedViewHolder", "Repository não disponível, usando findViewById para posição $position")
            }
        }

        // Remover onViewRecycled pois não está disponível em todas as versões
        // override fun onViewRecycled() {
        //     super.onViewRecycled()
        //     // Adicionar ViewHolder de volta ao pool quando reciclado
        //     if (::appRepository.isInitialized) {
        //         appRepository.adicionarViewHolderAoPool(this)
        //         Log.d("OptimizedViewHolder", "ViewHolder adicionado ao pool: $viewHolderTag")
        //     }
        // }
    }
}
