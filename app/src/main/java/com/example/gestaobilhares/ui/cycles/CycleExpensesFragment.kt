package com.example.gestaobilhares.ui.cycles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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

    companion object {
        fun newInstance(cicloId: Long, rotaId: Long, isCicloFinalizado: Boolean): CycleExpensesFragment {
            return CycleExpensesFragment().apply {
                arguments = Bundle().apply {
                    putLong("cicloId", cicloId)
                    putLong("rotaId", rotaId)
                    putBoolean("isCicloFinalizado", isCicloFinalizado)
                }
            }
        }
    }

    private var _binding: FragmentCycleExpensesBinding? = null
    private val binding get() = _binding!!
    
    private var cicloId: Long = 0L
    private var rotaId: Long = 0L
    private var isCicloFinalizado: Boolean = false
    
    private val viewModel: CycleExpensesViewModel by viewModels {
        CycleExpensesViewModelFactory(
            CicloAcertoRepository(
                AppDatabase.getDatabase(requireContext()).cicloAcertoDao(),
                DespesaRepository(AppDatabase.getDatabase(requireContext()).despesaDao()),
                AcertoRepository(AppDatabase.getDatabase(requireContext()).acertoDao(), AppDatabase.getDatabase(requireContext()).clienteDao()),
                ClienteRepository(AppDatabase.getDatabase(requireContext()).clienteDao()),
                AppDatabase.getDatabase(requireContext()).rotaDao()
            ),
            DespesaRepository(AppDatabase.getDatabase(requireContext()).despesaDao())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cicloId = arguments?.getLong("cicloId", 0L) ?: 0L
        rotaId = arguments?.getLong("rotaId", 0L) ?: 0L
        isCicloFinalizado = arguments?.getBoolean("isCicloFinalizado", false) ?: false
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
                isCicloFinalizado = isCicloFinalizado,
                onExpenseClick = { despesa ->
                    if (!isCicloFinalizado) {
                        mostrarDialogoEditarDespesa(despesa)
                    }
                },
                onExpenseDelete = { despesa ->
                    if (!isCicloFinalizado) {
                        mostrarDialogoConfirmarExclusao(despesa)
                    }
                }
            )
        }
    }

    /**
     * Função pública para ser chamada pelo parent fragment
     */
    fun mostrarDialogoDespesaUnificada() {
        mostrarDialogoDespesa()
    }

    /**
     * Mostra diálogo unificado para editar/adicionar despesa
     * ✅ CORREÇÃO: Usar a mesma tela da Nova Despesa
     */
    private fun mostrarDialogoDespesa(despesa: CycleExpenseItem? = null) {
        if (despesa != null) {
            // ✅ NOVO: Navegar para a tela de Nova Despesa com dados pré-preenchidos
            navegarParaNovaDespesaComEdicao(despesa)
        } else {
            // ✅ NOVO: Navegar para a tela de Nova Despesa para adicionar
            navegarParaNovaDespesa()
        }
    }

    /**
     * ✅ NOVO: Navega para a tela de Nova Despesa para adicionar
     */
    private fun navegarParaNovaDespesa() {
        // Obter rotaId do parent fragment
        val rotaId = (parentFragment as? CycleManagementFragment)?.rotaId ?: 0L
        
        if (rotaId > 0) {
            // ✅ CORREÇÃO: Usar a rota correta do nav_graph
            val action = CycleManagementFragmentDirections.actionCycleManagementFragmentToExpenseRegisterFragment(
                rotaId = rotaId,
                despesaId = 0L,
                modoEdicao = false
            )
            findNavController().navigate(action)
        } else {
            // Fallback se não conseguir obter rotaId
            mostrarDialogoDespesaFallback()
        }
    }

    /**
     * ✅ NOVO: Navega para a tela de Nova Despesa com dados pré-preenchidos para edição
     */
    private fun navegarParaNovaDespesaComEdicao(despesa: CycleExpenseItem) {
        // Obter rotaId do parent fragment
        val rotaId = (parentFragment as? CycleManagementFragment)?.rotaId ?: 0L
        
        if (rotaId > 0) {
            // ✅ CORREÇÃO: Usar a rota correta do nav_graph
            val action = CycleManagementFragmentDirections.actionCycleManagementFragmentToExpenseRegisterFragment(
                rotaId = rotaId,
                despesaId = despesa.id,
                modoEdicao = true
            )
            findNavController().navigate(action)
        } else {
            // Fallback se não conseguir obter rotaId
            mostrarDialogoDespesaFallback(despesa)
        }
    }

    /**
     * ✅ NOVO: Diálogo fallback enquanto a navegação não está implementada
     */
    private fun mostrarDialogoDespesaFallback(despesa: CycleExpenseItem? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_expense, null)
        
        // Campos do diálogo
        val etDescricao = dialogView.findViewById<android.widget.EditText>(R.id.etDescricao)
        val etValor = dialogView.findViewById<android.widget.EditText>(R.id.etValor)
        val etCategoria = dialogView.findViewById<android.widget.EditText>(R.id.etCategoria)
        val etObservacoes = dialogView.findViewById<android.widget.EditText>(R.id.etObservacoes)
        
        // Se for edição, preencher com dados existentes
        if (despesa != null) {
            etDescricao.setText(despesa.descricao)
            etValor.setText(String.format("%.2f", despesa.valor))
            etCategoria.setText(despesa.categoria)
            etObservacoes.setText(despesa.observacoes)
        }
        
        val isEdicao = despesa != null
        val titulo = if (isEdicao) "Editar Despesa" else "Adicionar Nova Despesa"
        val botaoPositivo = if (isEdicao) "Salvar" else "Adicionar"
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(titulo)
            .setView(dialogView)
            .setPositiveButton(botaoPositivo) { _, _ ->
                val descricao = etDescricao.text.toString()
                val valor = etValor.text.toString().toDoubleOrNull() ?: 0.0
                val categoria = etCategoria.text.toString()
                val observacoes = etObservacoes.text.toString()
                
                if (descricao.isNotBlank() && valor > 0) {
                    if (isEdicao) {
                        despesa?.let { despesaNotNull ->
                            viewModel.editarDespesa(despesaNotNull.id, descricao, valor, categoria, observacoes)
                            mostrarFeedback("Despesa editada com sucesso!", Snackbar.LENGTH_SHORT)
                        }
                    } else {
                        // TODO: Implementar adição de despesa
                        mostrarFeedback("Funcionalidade de adicionar despesa será implementada em breve", Snackbar.LENGTH_LONG)
                    }
                } else {
                    mostrarFeedback("Preencha todos os campos obrigatórios", Snackbar.LENGTH_SHORT)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Mostra diálogo para editar despesa (usando função unificada)
     */
    private fun mostrarDialogoEditarDespesa(despesa: CycleExpenseItem) {
        mostrarDialogoDespesa(despesa)
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

        // ✅ NOVO: Observer para notificar mudanças ao parent fragment
        lifecycleScope.launch {
            viewModel.despesaModificada.collect { modificada ->
                if (modificada) {
                    // Notificar parent fragment para recarregar estatísticas
                    (parentFragment as? CycleManagementFragment)?.viewModel?.recarregarEstatisticas()
                    viewModel.limparNotificacaoMudanca()
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


}