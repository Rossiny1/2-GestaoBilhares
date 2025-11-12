package com.example.gestaobilhares.ui.memory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.factory.RepositoryFactory
import com.example.gestaobilhares.memory.MemoryOptimizer
import com.example.gestaobilhares.memory.WeakReferenceManager
import kotlinx.coroutines.launch
import java.text.DecimalFormat

/**
 * ✅ FASE 4D: Fragment de Demonstração de Otimizações de Memória
 * 
 * Demonstra o uso das otimizações de memória implementadas
 */
class MemoryOptimizationFragment : Fragment() {
    
    private lateinit var tvMemoryStats: TextView
    private lateinit var tvReferenceStats: TextView
    private lateinit var btnRefreshStats: Button
    private lateinit var btnClearCaches: Button
    private lateinit var btnForceGC: Button
    private lateinit var btnStartMonitoring: Button
    
    private val appRepository by lazy { 
        RepositoryFactory.getAppRepository(requireContext()) 
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_memory_optimization, container, false)
        
        tvMemoryStats = view.findViewById(R.id.tvMemoryStats)
        tvReferenceStats = view.findViewById(R.id.tvReferenceStats)
        btnRefreshStats = view.findViewById(R.id.btnRefreshStats)
        btnClearCaches = view.findViewById(R.id.btnClearCaches)
        btnForceGC = view.findViewById(R.id.btnForceGC)
        btnStartMonitoring = view.findViewById(R.id.btnStartMonitoring)
        
        setupClickListeners()
        refreshStats()
        
        return view
    }
    
    private fun setupClickListeners() {
        btnRefreshStats.setOnClickListener {
            refreshStats()
        }
        
        btnClearCaches.setOnClickListener {
            clearCaches()
        }
        
        btnForceGC.setOnClickListener {
            forceGarbageCollection()
        }
        
        btnStartMonitoring.setOnClickListener {
            startMemoryMonitoring()
        }
    }
    
    private fun refreshStats() {
        lifecycleScope.launch {
            try {
                // Obter estatísticas de memória
                val memoryStats = appRepository.obterEstatisticasMemoria()
                val referenceStats = appRepository.obterEstatisticasReferencias()
                
                // Exibir estatísticas (já retornam String formatada)
                tvMemoryStats.text = memoryStats
                tvReferenceStats.text = referenceStats
                
            } catch (e: Exception) {
                tvMemoryStats.text = "Erro ao obter estatísticas: ${e.message}"
                tvReferenceStats.text = ""
            }
        }
    }
    
    private fun formatMemoryStats(stats: MemoryOptimizer.MemoryStats): String {
        val df = DecimalFormat("#.##")
        return """
            📊 ESTATÍSTICAS DE MEMÓRIA
            
            💾 Memória Total: ${formatBytes(stats.maxMemory)}
            📈 Memória Usada: ${formatBytes(stats.usedMemory)} (${df.format(stats.memoryUsagePercent)}%)
            🆓 Memória Livre: ${formatBytes(stats.freeMemory)}
            
            🗂️ Cache de Bitmaps:
            • Itens em Cache: ${stats.cacheSize}/${stats.cacheMaxSize}
            • Utilização: ${df.format(stats.cacheUsagePercent)}%
            
            🔄 Pool de Objetos: ${stats.poolCount} objetos
            🔗 Referências Fracas: ${stats.weakReferenceCount}
        """.trimIndent()
    }
    
    private fun formatReferenceStats(stats: WeakReferenceManager.ReferenceStats): String {
        return """
            🔗 ESTATÍSTICAS DE REFERÊNCIAS
            
            📊 Total de Referências: ${stats.totalReferences}
            ✅ Referências Ativas: ${stats.aliveReferences}
            ❌ Referências Nulas: ${stats.nullReferences}
            🔄 Callbacks de Limpeza: ${stats.cleanupCallbacks}
        """.trimIndent()
    }
    
    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$bytes bytes"
        }
    }
    
    private fun clearCaches() {
        lifecycleScope.launch {
            try {
                appRepository.limparCachesMemoria()
                refreshStats()
                // Mostrar feedback
                tvMemoryStats.text = "✅ Caches limpos com sucesso!\n\n" + tvMemoryStats.text
            } catch (e: Exception) {
                tvMemoryStats.text = "❌ Erro ao limpar caches: ${e.message}"
            }
        }
    }
    
    private fun forceGarbageCollection() {
        lifecycleScope.launch {
            try {
                appRepository.forcarGarbageCollection()
                // Aguardar um pouco e atualizar estatísticas
                kotlinx.coroutines.delay(1000)
                refreshStats()
                // Mostrar feedback
                tvMemoryStats.text = "🗑️ Garbage Collection executado!\n\n" + tvMemoryStats.text
            } catch (e: Exception) {
                tvMemoryStats.text = "❌ Erro ao forçar GC: ${e.message}"
            }
        }
    }
    
    private fun startMemoryMonitoring() {
        lifecycleScope.launch {
            try {
                appRepository.iniciarMonitoramentoMemoria()
                // Mostrar feedback
                tvMemoryStats.text = "📊 Monitoramento iniciado!\n\n" + tvMemoryStats.text
            } catch (e: Exception) {
                tvMemoryStats.text = "❌ Erro ao iniciar monitoramento: ${e.message}"
            }
        }
    }
}
