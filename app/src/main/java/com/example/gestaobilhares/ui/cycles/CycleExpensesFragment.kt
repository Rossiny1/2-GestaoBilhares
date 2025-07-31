package com.example.gestaobilhares.ui.cycles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentCycleExpensesBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.DespesaRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.ui.cycles.adapter.CycleExpensesAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment para listar despesas do ciclo
 */
class CycleExpensesFragment : Fragment() {

    private var _binding: FragmentCycleExpensesBinding? = null
    private val binding get() = _binding!!
    
    private var cicloId: Long = 0L
    
    private val viewModel: CycleExpensesViewModel by viewModels {
        CycleExpensesViewModelFactory(
            CicloAcertoRepository(
                AppDatabase.getDatabase(requireContext()).cicloAcertoDao(),
                DespesaRepository(AppDatabase.getDatabase(requireContext()).despesaDao()),
                AcertoRepository(AppDatabase.getDatabase(requireContext()).acertoDao(), AppDatabase.getDatabase(requireContext()).clienteDao()),
                ClienteRepository(AppDatabase.getDatabase(requireContext()).clienteDao()),
                AppDatabase.getDatabase(requireContext()).rotaDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cicloId = arguments?.getLong("cicloId", 0L) ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCycleExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        
        // Carregar despesas
        viewModel.carregarDespesas(cicloId)
    }

    private fun setupRecyclerView() {
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = CycleExpensesAdapter(
                onExpenseClick = { despesa ->
                    mostrarDialogoEditarDespesa(despesa)
                },
                onExpenseDelete = { despesa ->
                    mostrarDialogoConfirmarExclusao(despesa)
                }
            )
        }
    }

    /**
     * Mostra diálogo para editar despesa
     */
    private fun mostrarDialogoEditarDespesa(despesa: CycleExpenseItem) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_expense, null)
        
        // Preencher campos com dados atuais
        val etDescricao = dialogView.findViewById<android.widget.EditText>(R.id.etDescricao)
        val etValor = dialogView.findViewById<android.widget.EditText>(R.id.etValor)
        val etCategoria = dialogView.findViewById<android.widget.EditText>(R.id.etCategoria)
        val etObservacoes = dialogView.findViewById<android.widget.EditText>(R.id.etObservacoes)
        
        etDescricao.setText(despesa.descricao)
        etValor.setText(String.format("%.2f", despesa.valor))
        etCategoria.setText(despesa.categoria)
        etObservacoes.setText(despesa.observacoes)
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Editar Despesa")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val novaDescricao = etDescricao.text.toString()
                val novoValor = etValor.text.toString().toDoubleOrNull() ?: 0.0
                val novaCategoria = etCategoria.text.toString()
                val novasObservacoes = etObservacoes.text.toString()
                
                if (novaDescricao.isNotBlank() && novoValor > 0) {
                    viewModel.editarDespesa(despesa.id, novaDescricao, novoValor, novaCategoria, novasObservacoes)
                } else {
                    mostrarFeedback("Preencha todos os campos obrigatórios", Snackbar.LENGTH_SHORT)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Mostra diálogo para confirmar exclusão de despesa
     */
    private fun mostrarDialogoConfirmarExclusao(despesa: CycleExpenseItem) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir a despesa '${despesa.descricao}'?")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.removerDespesa(despesa.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.despesas.collect { despesas ->
                (binding.rvExpenses.adapter as? CycleExpensesAdapter)?.submitList(despesas)
                atualizarEmptyState(despesas.isEmpty())
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { carregando ->
                binding.progressBar.visibility = if (carregando) View.VISIBLE else View.GONE
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { mensagem ->
                mensagem?.let {
                    mostrarFeedback("Erro: $it", Snackbar.LENGTH_LONG)
                    viewModel.limparErro()
                }
            }
        }
    }

    private fun atualizarEmptyState(mostrar: Boolean) {
        binding.apply {
            if (mostrar) {
                emptyStateLayout.visibility = View.VISIBLE
                rvExpenses.visibility = View.GONE
            } else {
                emptyStateLayout.visibility = View.GONE
                rvExpenses.visibility = View.VISIBLE
            }
        }
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

    companion object {
        fun newInstance(cicloId: Long): CycleExpensesFragment {
            val fragment = CycleExpensesFragment()
            val args = Bundle()
            args.putLong("cicloId", cicloId)
            fragment.arguments = args
            return fragment
        }
    }
}