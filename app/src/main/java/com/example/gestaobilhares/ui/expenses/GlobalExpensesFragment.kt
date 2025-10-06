package com.example.gestaobilhares.ui.expenses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.Despesa
import com.example.gestaobilhares.databinding.FragmentGlobalExpensesBinding
import com.example.gestaobilhares.ui.expenses.adapter.GlobalExpensesAdapter
import com.example.gestaobilhares.ui.expenses.dialog.CycleSelectionDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

/**
 * Fragment para listagem e gerenciamento de despesas globais
 * Permite filtrar por ciclo e adicionar novas despesas
 */
@AndroidEntryPoint
class GlobalExpensesFragment : Fragment() {

    private var _binding: FragmentGlobalExpensesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GlobalExpensesViewModel by viewModels()
    private lateinit var globalExpensesAdapter: GlobalExpensesAdapter

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGlobalExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupUI() {
        // Configurar toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Configurar campo de ano
        binding.etYearSelection.setOnClickListener {
            showYearSelectionDialog()
        }

        // Configurar botão de filtro por ciclo
        binding.etCycleSelection.setOnClickListener {
            showCycleSelectionDialog()
        }

        // Configurar botão filtrar
        binding.btnFilterByCycle.setOnClickListener {
            showCycleSelectionDialog()
        }

        // Configurar botão mostrar todas
        binding.btnShowAll.setOnClickListener {
            viewModel.loadAllGlobalExpenses()
        }

        // Configurar FAB para adicionar despesa
        binding.fabAddExpense.setOnClickListener {
            navigateToAddExpense()
        }
    }

    private fun setupObservers() {
        // Observar despesas globais
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.globalExpenses.collect { expenses ->
                    globalExpensesAdapter.submitList(expenses)
                    updateEmptyState(expenses.isEmpty())
                }
            }
        }

        // Observar ano selecionado
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedYear.collect { year ->
                    binding.etYearSelection.setText(year.toString())
                }
            }
        }

        // Observar ciclo selecionado
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedCycle.collect { cycle ->
                    val cycleText = if (cycle != null) {
                        "${cycle.numeroCiclo}º Acerto"
                    } else {
                        "Selecionar Ciclo"
                    }
                    binding.etCycleSelection.setText(cycleText)
                }
            }
        }

        // Observar total de despesas
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.totalExpenses.collect { total ->
                    binding.tvTotalExpenses.text = currencyFormat.format(total)
                }
            }
        }

        // Observar quantidade de despesas
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.expensesCount.collect { count ->
                    binding.tvExpensesCount.text = count.toString()
                }
            }
        }

        // Observar estado de carregamento
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    // Aqui você pode mostrar/esconder um progress indicator se necessário
                }
            }
        }

        // Observar mensagens de erro
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { error ->
                    error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        globalExpensesAdapter = GlobalExpensesAdapter(
            onItemClick = { expense ->
                // Aqui você pode implementar navegação para detalhes da despesa
                showExpenseDetails(expense)
            },
            onItemLongClick = { expense ->
                showDeleteConfirmation(expense)
            }
        )

        binding.rvGlobalExpenses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = globalExpensesAdapter
        }
    }

    private fun showYearSelectionDialog() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear + 1).toList()
        
        val yearArray = years.map { it.toString() }.toTypedArray()
        val currentYearIndex = years.indexOf(currentYear)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Selecionar Ano")
            .setSingleChoiceItems(yearArray, currentYearIndex) { dialog, which ->
                val selectedYear = years[which]
                viewModel.setSelectedYear(selectedYear)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showCycleSelectionDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val cycles = viewModel.availableCycles.value
            if (cycles.isNotEmpty()) {
                CycleSelectionDialog.show(
                    fragmentManager = parentFragmentManager,
                    cycles = cycles,
                    onCycleSelected = { cycle ->
                        viewModel.filterByCycle(cycle)
                    }
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    "Nenhum ciclo disponível",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showExpenseDetails(expense: Despesa) {
        // Implementar navegação para detalhes da despesa
        // Por enquanto, apenas mostra um toast
        Toast.makeText(
            requireContext(),
            "Detalhes da despesa: ${expense.descricao}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showDeleteConfirmation(expense: Despesa) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Excluir Despesa")
            .setMessage("Deseja realmente excluir a despesa '${expense.descricao}'?")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.deleteGlobalExpense(expense)
                Snackbar.make(
                    binding.root,
                    "Despesa excluída com sucesso",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun navigateToAddExpense() {
        // Navegar para a tela de adicionar despesa global
        // O ExpenseRegisterFragment já está configurado para despesas globais quando rotaId = 0L
        findNavController().navigate(
            R.id.action_globalExpensesFragment_to_expenseRegisterFragment
        )
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvGlobalExpenses.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvGlobalExpenses.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
