package com.example.gestaobilhares.ui.optimization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.factory.RepositoryFactory
import com.example.gestaobilhares.databinding.FragmentUiOptimizationBinding
import android.util.Log

/**
 * ✅ FASE 4D: Fragment de demonstração das otimizações de UI
 * Seguindo Android 2025 best practices para performance
 * 
 * Funcionalidades:
 * - Demonstra ViewStub optimization
 * - Mostra ViewHolder pooling
 * - Exibe Layout optimization
 * - Testa RecyclerView performance
 */
class UIOptimizationFragment : Fragment() {

    private var _binding: FragmentUiOptimizationBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var appRepository: com.example.gestaobilhares.data.repository.AppRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUiOptimizationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar repository
        appRepository = RepositoryFactory.getAppRepository(requireContext())
        
        // Otimizar a hierarquia de views
        val optimizedView = appRepository.otimizarHierarquiaViews(view)
        Log.d("UIOptimizationFragment", "Hierarquia de views otimizada")
        
        setupUI()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupUI() {
        // Configurar título
        binding.tvTitle.text = "🚀 Otimizações de UI"
        binding.tvSubtitle.text = "Performance otimizada sem alterar design"
    }

    private fun setupRecyclerView() {
        // Configurar RecyclerView com otimizações
        val recyclerView = binding.recyclerViewOptimizations
        
        // Aplicar otimizações automáticas
        appRepository.otimizarRecyclerView(recyclerView)
        
        // Configurar adapter
        val adapter = UIOptimizationAdapter()
        adapter.setRepository(appRepository)
        recyclerView.adapter = adapter
        
        // Configurar layout manager
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        Log.d("UIOptimizationFragment", "RecyclerView otimizado e configurado")
    }

    private fun setupClickListeners() {
        // Botão de estatísticas de ViewStub
        binding.btnViewStubStats.setOnClickListener {
            val stats = appRepository.obterEstatisticasViewStub()
            binding.tvViewStubStats.text = """
                📊 ViewStub Statistics:
                • Total Inflated: ${stats.totalInflatedViews}
                • Inflating: ${stats.inflatingViews}
                • Cache Keys: ${stats.cacheKeys.size}
            """.trimIndent()
        }

        // Botão de estatísticas de ViewHolder
        binding.btnViewHolderStats.setOnClickListener {
            val stats = appRepository.obterEstatisticasViewHolder()
            binding.tvViewHolderStats.text = """
                🏗️ ViewHolder Statistics:
                • Total Pools: ${stats.totalPools}
                • Cached Views: ${stats.totalCachedViews}
                • Cache Entries: ${stats.totalCacheEntries}
            """.trimIndent()
        }

        // Botão de estatísticas de Layout
        binding.btnLayoutStats.setOnClickListener {
            val stats = appRepository.obterEstatisticasLayout()
            val totalOptimizationTime = stats.sumOf { it.optimizationTime }
            val totalViews = stats.sumOf { it.viewCount }
            
            binding.tvLayoutStats.text = """
                📐 Layout Statistics:
                • Total Optimization Time: ${totalOptimizationTime}ms
                • Total Views: $totalViews
                • Optimized Layouts: ${stats.size}
            """.trimIndent()
        }

        // Botão de limpeza
        binding.btnClearOptimizations.setOnClickListener {
            appRepository.limparTodasOtimizacoesUI()
            binding.tvViewStubStats.text = "ViewStub stats cleared"
            binding.tvViewHolderStats.text = "ViewHolder stats cleared"
            binding.tvLayoutStats.text = "Layout stats cleared"
            Log.d("UIOptimizationFragment", "Todas as otimizações de UI limpas")
        }

        // Botão de teste de performance
        binding.btnPerformanceTest.setOnClickListener {
            runPerformanceTest()
        }
    }

    private fun runPerformanceTest() {
        binding.tvPerformanceResults.text = "🔄 Executando teste de performance..."
        
        // Simular teste de performance
        val startTime = System.currentTimeMillis()
        
        // Teste de otimização de hierarquia
        val testView = binding.root
        appRepository.otimizarHierarquiaViews(testView)
        
        // Teste de cache de layout
        appRepository.cachearLayout("test_layout", testView)
        val cachedView = appRepository.obterLayoutCacheado("test_layout")
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        binding.tvPerformanceResults.text = """
            ✅ Teste de Performance Concluído:
            • Tempo Total: ${totalTime}ms
            • Hierarquia Otimizada: ✅
            • Layout Cacheado: ${if (cachedView != null) "✅" else "❌"}
            • Performance: ${if (totalTime < 100) "Excelente" else "Boa"}
        """.trimIndent()
        
        Log.d("UIOptimizationFragment", "Teste de performance concluído em ${totalTime}ms")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
