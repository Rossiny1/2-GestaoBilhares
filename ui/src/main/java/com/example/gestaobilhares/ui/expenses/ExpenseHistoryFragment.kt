package com.example.gestaobilhares.ui.expenses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.ui.R
import com.example.gestaobilhares.data.entities.CategoriaDespesaEnum
import com.example.gestaobilhares.data.entities.DespesaResumo
import com.example.gestaobilhares.ui.databinding.FragmentExpenseHistoryBinding
import com.google.android.material.chip.Chip
// Hilt removido - usando instanciação direta
import androidx.core.view.children
import kotlinx.coroutines.launch
import androidx.core.view.children
import java.text.NumberFormat
import java.util.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fragment para exibir o histórico de despesas.
 * Implementa funcionalidades de listagem, filtros e estatísticas.
 */
@AndroidEntryPoint
class ExpenseHistoryFragment : Fragment() {

    // ViewBinding para acessar as views
    private var _binding: FragmentExpenseHistoryBinding? = null
    private val binding get() = _binding!!



    // ViewModel injetado pelo Hilt
    private val viewModel: ExpenseHistoryViewModel by viewModels()

    // Adapter para a lista de despesas
    private lateinit var expenseAdapter: ExpenseAdapter

    // Formatador de moeda brasileiro
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        // ✅ MIGRADO: Usa AppRepository centralizado - Injetado via Hilt
        // val appRepository = RepositoryFactory.getAppRepository(requireContext())
        // viewModel = ExpenseHistoryViewModel(appRepository)
        
        setupRecyclerView()
        setupClickListeners()
        setupCategoryFilters()
        observeViewModel()
    }

    /**
     * Configura o RecyclerView com o adapter.
     */
    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter { despesaResumo ->
            onExpenseClick(despesaResumo)
        }

        binding.expensesRecyclerView.apply {
            adapter = expenseAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    /**
     * Configura os listeners de clique dos botões.
     */
    private fun setupClickListeners() {
        // Botão voltar
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // Botão de filtro
        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }
    }

    /**
     * Configura os chips de filtro por categoria.
     */
    private fun setupCategoryFilters() {
        // Chip "Todas"
        binding.chipTodas.setOnClickListener {
            viewModel.clearFilters()
            updateChipSelection(null)
        }

        // Chips de categorias
                binding.chipCombustivel.setOnClickListener {
            viewModel.filterByCategory(CategoriaDespesaEnum.COMBUSTIVEL.displayName)
            updateChipSelection(CategoriaDespesaEnum.COMBUSTIVEL.displayName)
        }
        
        binding.chipAlimentacao.setOnClickListener {
            viewModel.filterByCategory(CategoriaDespesaEnum.ALIMENTACAO.displayName)
            updateChipSelection(CategoriaDespesaEnum.ALIMENTACAO.displayName)
        }
        
        binding.chipTransporte.setOnClickListener {
            viewModel.filterByCategory(CategoriaDespesaEnum.TRANSPORTE.displayName)
            updateChipSelection(CategoriaDespesaEnum.TRANSPORTE.displayName)
        }
        
        binding.chipManutencao.setOnClickListener {
            viewModel.filterByCategory(CategoriaDespesaEnum.MANUTENCAO.displayName)
            updateChipSelection(CategoriaDespesaEnum.MANUTENCAO.displayName)
        }
        
        binding.chipMateriais.setOnClickListener {
            viewModel.filterByCategory(CategoriaDespesaEnum.MATERIAIS.displayName)
            updateChipSelection(CategoriaDespesaEnum.MATERIAIS.displayName)
        }
    }

    /**
     * Atualiza a seleção visual dos chips.
     */
    private fun updateChipSelection(selectedCategory: String?) {
        // Primeiro desmarca todos
        binding.categoryChipGroup.children.forEach { view ->
            if (view is Chip) {
                view.isChecked = false
            }
        }

        // Marca o chip selecionado
        when (selectedCategory) {
            null -> binding.chipTodas.isChecked = true
            CategoriaDespesaEnum.COMBUSTIVEL.displayName -> binding.chipCombustivel.isChecked = true
            CategoriaDespesaEnum.ALIMENTACAO.displayName -> binding.chipAlimentacao.isChecked = true
            CategoriaDespesaEnum.TRANSPORTE.displayName -> binding.chipTransporte.isChecked = true
            CategoriaDespesaEnum.MANUTENCAO.displayName -> binding.chipManutencao.isChecked = true
            CategoriaDespesaEnum.MATERIAIS.displayName -> binding.chipMateriais.isChecked = true
        }
    }

    /**
     * Observa mudanças no ViewModel e atualiza a UI.
     */
    private fun observeViewModel() {
        // Observa as despesas filtradas
        lifecycleScope.launch {
            viewModel.filteredExpenses.collect { expenses ->
                expenseAdapter.submitList(expenses)
                updateEmptyState(expenses.isEmpty())
                updateStatisticsForFiltered(expenses)
            }
        }

        // TODO: Implementar observação de estatísticas quando necessário
        // viewModel.statistics.observe(viewLifecycleOwner) { stats ->
        //     updateStatistics(stats)
        // }

        // Observa estado de carregamento
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        // Observa categoria selecionada
        lifecycleScope.launch {
            viewModel.selectedCategory.collect { category ->
                updateChipSelection(category)
            }
        }

        // Observa mensagens de erro
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }

        // Observa mensagens de sucesso
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
    }

    /**
     * Atualiza as estatísticas na UI com dados filtrados.
     */
    private fun updateStatisticsForFiltered(expenses: List<DespesaResumo>) {
        val total = expenses.sumOf { it.valor }
        val count = expenses.size
        
        binding.totalExpensesAmount.text = currencyFormatter.format(total)
        binding.expenseCount.text = count.toString()
        
        // Calcula período dos dados filtrados
        if (expenses.isNotEmpty()) {
            val minDate = expenses.minByOrNull { it.dataHora }?.dataHora
            val maxDate = expenses.maxByOrNull { it.dataHora }?.dataHora
            
            if (minDate != null && maxDate != null) {
                val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(minDate, maxDate) + 1
                binding.periodText.text = if (daysDiff == 1L) "1 dia" else "$daysDiff dias"
            }
        } else {
            binding.periodText.text = "0 dias"
        }
    }

    /**
     * Atualiza as estatísticas gerais na UI.
     */
    private fun updateStatistics(stats: com.example.gestaobilhares.data.entities.EstatisticasDespesas) {
        // Se não há filtros ativos, usa as estatísticas gerais
        if (!viewModel.hasActiveFilters()) {
            binding.totalExpensesAmount.text = currencyFormatter.format(stats.totalDespesas)
            binding.expenseCount.text = stats.quantidadeDespesas.toString()
            
            if (stats.periodoInicio != null && stats.periodoFim != null) {
                val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(stats.periodoInicio, stats.periodoFim) + 1
                binding.periodText.text = if (daysDiff == 1L) "1 dia" else "$daysDiff dias"
            }
        }
    }

    /**
     * Atualiza o estado vazio da tela.
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.expensesRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    /**
     * Mostra diálogo de filtros avançados.
     */
    private fun showFilterDialog() {
        // TODO: Implementar diálogo com filtros por data, valor, etc.
        Toast.makeText(requireContext(), "Filtros avançados em desenvolvimento", Toast.LENGTH_SHORT).show()
    }

    /**
     * Manipula clique em item de despesa.
     */
    private fun onExpenseClick(despesaResumo: DespesaResumo) {
        // TODO: Navegar para detalhes da despesa ou permitir edição
        Toast.makeText(
            requireContext(), 
            "Despesa: ${despesaResumo.descricao}", 
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 

