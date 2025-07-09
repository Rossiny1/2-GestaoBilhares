package com.example.gestaobilhares.ui.clients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentCycleHistoryBinding
import com.example.gestaobilhares.ui.clients.adapter.CycleHistoryAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Fragment para exibir o histórico de ciclos de acerto de uma rota
 * ✅ FASE 9C: HISTÓRICO DE CICLOS E RELATÓRIOS FINANCEIROS
 */
class CycleHistoryFragment : Fragment() {

    private var _binding: FragmentCycleHistoryBinding? = null
    private val binding get() = _binding!!
    
    private val args: CycleHistoryFragmentArgs by navArgs()
    private val viewModel: CycleHistoryViewModel by activityViewModels()
    
    private lateinit var cycleAdapter: CycleHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCycleHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        configurarRecyclerView()
        configurarBotoes()
        configurarObservadores()
        
        // Carregar dados
        val rotaId = arguments?.getLong("rotaId", 0L) ?: 0L
        viewModel.carregarHistoricoCiclos(rotaId)
    }

    private fun configurarRecyclerView() {
        cycleAdapter = CycleHistoryAdapter { ciclo ->
            // Navegar para detalhes do ciclo
            val action = CycleHistoryFragmentDirections
                .actionCycleHistoryFragmentToCycleDetailFragment(ciclo.id)
            findNavController().navigate(action)
        }
        
        binding.rvCycles.apply {
            adapter = cycleAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun configurarBotoes() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        binding.btnExport.setOnClickListener {
            exportarRelatorio()
        }
        
        binding.btnFilter.setOnClickListener {
            mostrarDialogoFiltros()
        }
    }

    private fun configurarObservadores() {
        // Observar lista de ciclos
        lifecycleScope.launch {
            viewModel.ciclos.collect { ciclos ->
                cycleAdapter.submitList(ciclos)
                atualizarEmptyState(ciclos.isEmpty())
            }
        }
        
        // Observar estatísticas
        lifecycleScope.launch {
            viewModel.estatisticas.collect { stats ->
                atualizarEstatisticas(stats)
            }
        }
        
        // Observar estado de carregamento
        lifecycleScope.launch {
            viewModel.isLoading.collect { carregando ->
                binding.progressBar.visibility = if (carregando) View.VISIBLE else View.GONE
            }
        }
        
        // Observar mensagens de erro
        lifecycleScope.launch {
            viewModel.errorMessage.collect { mensagem ->
                mensagem?.let {
                    mostrarFeedback("Erro: $it", Snackbar.LENGTH_LONG)
                    viewModel.limparErro()
                }
            }
        }
    }

    private fun atualizarEstatisticas(stats: CycleStatistics) {
        binding.apply {
            tvTotalCycles.text = "${stats.totalCiclos} ciclos"
            tvTotalRevenue.text = formatarMoeda(stats.receitaTotal)
            tvTotalExpenses.text = formatarMoeda(stats.despesasTotal)
            tvNetProfit.text = formatarMoeda(stats.lucroLiquido)
            tvAvgProfit.text = formatarMoeda(stats.lucroMedioPorCiclo)
            
            // Atualizar cores baseado no lucro
            val color = if (stats.lucroLiquido >= 0) {
                requireContext().getColor(R.color.green_600)
            } else {
                requireContext().getColor(R.color.red_600)
            }
            tvNetProfit.setTextColor(color)
        }
    }

    private fun atualizarEmptyState(mostrar: Boolean) {
        binding.apply {
            if (mostrar) {
                emptyStateLayout.visibility = View.VISIBLE
                rvCycles.visibility = View.GONE
            } else {
                emptyStateLayout.visibility = View.GONE
                rvCycles.visibility = View.VISIBLE
            }
        }
    }

    private fun exportarRelatorio() {
        val rotaId = arguments?.getLong("rotaId", 0L) ?: 0L
        viewModel.exportarRelatorio(rotaId) { sucesso ->
            if (sucesso) {
                mostrarFeedback("Relatório exportado com sucesso!", Snackbar.LENGTH_SHORT)
            } else {
                mostrarFeedback("Erro ao exportar relatório", Snackbar.LENGTH_LONG)
            }
        }
    }

    private fun mostrarDialogoFiltros() {
        // TODO: Implementar diálogo de filtros por período
        mostrarFeedback("Filtros serão implementados na próxima fase", Snackbar.LENGTH_SHORT)
    }

    private fun formatarMoeda(valor: Double): String {
        return java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
            .format(valor)
    }

    private fun mostrarFeedback(mensagem: String, duracao: Int) {
        Snackbar.make(binding.root, mensagem, duracao)
            .setBackgroundTint(requireContext().getColor(R.color.purple_600))
            .setTextColor(requireContext().getColor(R.color.white))
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 