package com.example.gestaobilhares.ui.clients
import com.example.gestaobilhares.ui.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.ui.databinding.FragmentCycleHistoryBinding
import com.example.gestaobilhares.ui.clients.adapter.CycleHistoryAdapter
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.data.repository.AppRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import com.example.gestaobilhares.ui.clients.CycleHistoryItem
import com.example.gestaobilhares.ui.clients.CycleHistoryViewModelFactory

/**
 * Fragment para exibir o histórico de ciclos de acerto de uma rota
 * ✅ FASE 9C: HISTÓRICO DE CICLOS E RELATÓRIOS FINANCEIROS
 */
class CycleHistoryFragment : Fragment() {

    private var _binding: FragmentCycleHistoryBinding? = null
    private val binding get() = _binding!!
    
    // ✅ CORREÇÃO: Usar Bundle em vez de navArgs para evitar problemas de geração
    private var rotaId: Long = 0L
    
    private val viewModel: CycleHistoryViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        val appRepository = com.example.gestaobilhares.factory.RepositoryFactory.getAppRepository(requireContext())
        CycleHistoryViewModelFactory(
            CicloAcertoRepository(
                database.cicloAcertoDao(),
                database.despesaDao(), // ✅ CORRIGIDO: Passar DespesaDao diretamente
                AcertoRepository(database.acertoDao(), database.clienteDao()),
                ClienteRepository(database.clienteDao(), appRepository),
                database.rotaDao()
            ),
            appRepository
        )
    }
    
    private lateinit var cycleAdapter: CycleHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ✅ CORREÇÃO: Obter rotaId dos argumentos
        rotaId = arguments?.getLong("rotaId", 0L) ?: 0L
    }

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
        viewModel.carregarHistoricoCiclos(rotaId)
    }

    private fun configurarRecyclerView() {
        cycleAdapter = CycleHistoryAdapter { ciclo ->
            // ✅ CORREÇÃO: Todos os ciclos (em andamento e finalizados) navegam para Gerenciar Ciclo
            when (ciclo.status) {
                com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO,
                com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO -> {
                    // Navegar para tela de gerenciamento do ciclo
                    navegarParaGerenciamentoCiclo(ciclo)
                }
                com.example.gestaobilhares.data.entities.StatusCicloAcerto.PLANEJADO -> {
                    mostrarFeedback("Ciclo planejado não pode ser gerenciado ainda", Snackbar.LENGTH_SHORT)
                }
                else -> {
                    mostrarFeedback("Ciclo ${ciclo.status.name.lowercase()} não pode ser gerenciado", Snackbar.LENGTH_SHORT)
                }
            }
        }
        
        binding.rvCycles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cycleAdapter
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
        // ✅ CORREÇÃO: Usar repeatOnLifecycle para garantir que observers só rodem quando fragment está visível
        // Observar lista de ciclos
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ciclos.collect { ciclos ->
                    _binding?.let { binding ->
                        cycleAdapter.submitList(ciclos)
                        atualizarEmptyState(ciclos.isEmpty())
                    }
                }
            }
        }
        
        // Observar estatísticas
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estatisticas.collect { stats ->
                    _binding?.let {
                        atualizarEstatisticas(stats)
                    }
                }
            }
        }
        
        // Observar estado de carregamento
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { carregando ->
                    _binding?.let { binding ->
                        binding.progressBar.visibility = if (carregando) View.VISIBLE else View.GONE
                    }
                }
            }
        }
        
        // Observar mensagens de erro
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { mensagem ->
                    mensagem?.let {
                        _binding?.let {
                            mostrarFeedback("Erro: $it", Snackbar.LENGTH_LONG)
                            viewModel.limparErro()
                        }
                    }
                }
            }
        }

        // ✅ NOVO: Observar evento de acerto salvo para atualizar dados em tempo real
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("acerto_salvo")?.observe(
            viewLifecycleOwner
        ) { acertoSalvo ->
            if (acertoSalvo == true) {
                android.util.Log.d("CycleHistoryFragment", "🔄 Evento acerto_salvo recebido, recarregando dados em tempo real para rotaId=$rotaId")
                viewModel.recarregarDadosTempoReal(rotaId)
                findNavController().currentBackStackEntry?.savedStateHandle?.set("acerto_salvo", false)
            }
        }
    }

    private fun atualizarEstatisticas(stats: CycleStatistics) {
        // ✅ CORREÇÃO: Verificar se binding não é null antes de usar
        _binding?.let { binding ->
            binding.apply {
                tvTotalCycles.text = "${stats.totalCiclos} ciclos"
                tvTotalRevenue.text = formatarMoeda(stats.receitaTotal)
                tvTotalExpenses.text = formatarMoeda(stats.despesasTotal)
                tvNetProfit.text = formatarMoeda(stats.lucroLiquido)
                tvAvgProfit.text = formatarMoeda(stats.lucroMedioPorCiclo)
                
                // Atualizar cores baseado no lucro
                val color = if (stats.lucroLiquido >= 0) {
                    requireContext().getColor(com.example.gestaobilhares.ui.R.color.green_600)
                } else {
                    requireContext().getColor(com.example.gestaobilhares.ui.R.color.red_600)
                }
                tvNetProfit.setTextColor(color)
            }
        }
    }

    private fun atualizarEmptyState(mostrar: Boolean) {
        // ✅ CORREÇÃO: Verificar se binding não é null antes de usar
        _binding?.let { binding ->
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
    }

    private fun exportarRelatorio() {
        // Usar o rotaId do args que já está disponível
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

    /**
     * ✅ NOVO: Mostra diálogo para gerar relatório PDF do ciclo
     */
    private fun mostrarDialogoRelatorio(ciclo: CycleHistoryItem) {
        lifecycleScope.launch {
            try {
                // Buscar dados completos do ciclo
                val cicloEntity = viewModel.buscarCicloPorId(ciclo.id)
                val rota = viewModel.buscarRotaPorId(rotaId)
                val acertos = viewModel.buscarAcertosPorCiclo(ciclo.id)
                val despesas = viewModel.buscarDespesasPorCiclo(ciclo.id)
                val clientes = viewModel.buscarClientesPorRota(rotaId)
                
                if (cicloEntity != null && rota != null) {
                    val dialog = CycleReportDialog.newInstance(
                        ciclo = cicloEntity,
                        rota = rota,
                        acertos = acertos,
                        despesas = despesas,
                        clientes = clientes
                    )
                    dialog.show(parentFragmentManager, "CycleReportDialog")
                } else {
                    mostrarFeedback("Erro ao carregar dados do ciclo", Snackbar.LENGTH_LONG)
                }
            } catch (e: Exception) {
                android.util.Log.e("CycleHistoryFragment", "Erro ao mostrar diálogo de relatório", e)
                mostrarFeedback("Erro ao carregar dados: ${e.message}", Snackbar.LENGTH_LONG)
            }
        }
    }

    /**
     * Navega para a tela de gerenciamento do ciclo
     */
    private fun navegarParaGerenciamentoCiclo(ciclo: CycleHistoryItem) {
        try {
            val bundle = Bundle().apply {
                putLong("cicloId", ciclo.id)
                putLong("rotaId", rotaId)
            }
            findNavController().navigate(com.example.gestaobilhares.ui.R.id.cycleManagementFragment, bundle)
        } catch (e: Exception) {
            android.util.Log.e("CycleHistoryFragment", "Erro ao navegar para gerenciamento: ${e.message}", e)
            mostrarFeedback("Erro ao abrir gerenciamento: ${e.message}", Snackbar.LENGTH_LONG)
        }
    }

    private fun formatarMoeda(valor: Double): String {
        return java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
            .format(valor)
    }

    private fun mostrarFeedback(mensagem: String, duracao: Int) {
        // ✅ CORREÇÃO: Verificar se binding não é null antes de usar
        _binding?.let { binding ->
            Snackbar.make(binding.root, mensagem, duracao)
                .setBackgroundTint(requireContext().getColor(com.example.gestaobilhares.ui.R.color.purple_600))
                .setTextColor(requireContext().getColor(com.example.gestaobilhares.ui.R.color.white))
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
