package com.example.gestaobilhares.ui.colaboradores

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentColaboradorManagementBinding
import com.example.gestaobilhares.data.entities.Colaborador
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.database.AppDatabase
import com.google.android.material.chip.Chip

/**
 * Fragment para gerenciamento de colaboradores (CRUD).
 * Permite visualizar, aprovar, ativar/desativar e excluir colaboradores.
 * Acesso restrito apenas para administradores.
 */
class ColaboradorManagementFragment : Fragment() {

    private var _binding: FragmentColaboradorManagementBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ColaboradorManagementViewModel
    private lateinit var colaboradorAdapter: ColaboradorAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColaboradorManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
        viewModel = ColaboradorManagementViewModel(appRepository)
        
        setupRecyclerView()
        setupClickListeners()
        setupObservers()
    }

    private fun setupRecyclerView() {
        colaboradorAdapter = ColaboradorAdapter(
            onEditClick = { colaborador ->
                try {
                    val bundle = Bundle().apply {
                        putLong("colaborador_id", colaborador.id)
                    }
                    findNavController().navigate(R.id.colaboradorRegisterFragment, bundle)
                } catch (e: Exception) {
                    Log.e("ColaboradorManagementFragment", "Erro ao navegar para edição: ${e.message}", e)
                    Toast.makeText(requireContext(), "Erro ao abrir edição de colaborador", Toast.LENGTH_SHORT).show()
                }
            },
            onMoreClick = { colaborador ->
                mostrarMenuOpcoes(colaborador)
            }
        )

        binding.rvColaboradores.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = colaboradorAdapter
        }
    }

    private fun setupClickListeners() {
        // Botão voltar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // FAB para adicionar colaborador
        binding.fabAddColaborador.setOnClickListener {
            try {
                findNavController().navigate(R.id.colaboradorRegisterFragment)
            } catch (e: Exception) {
                Log.e("ColaboradorManagementFragment", "Erro ao navegar para cadastro: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao abrir cadastro de colaborador", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ CORREÇÃO: Menu da toolbar removido - sem botões de filtro, configurações e pesquisa

        // Chips de filtro
        binding.chipTodos.setOnClickListener { viewModel.aplicarFiltro(FiltroColaborador.TODOS) }
        binding.chipAtivos.setOnClickListener { viewModel.aplicarFiltro(FiltroColaborador.ATIVOS) }
        binding.chipPendentes.setOnClickListener { viewModel.aplicarFiltro(FiltroColaborador.PENDENTES) }
        binding.chipAdmins.setOnClickListener { viewModel.aplicarFiltro(FiltroColaborador.ADMINISTRADORES) }
    }

    private fun setupObservers() {
        // Observa lista de colaboradores
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.colaboradores.collect { colaboradores ->
                    colaboradorAdapter.submitList(colaboradores)
                    atualizarEstadoVazio(colaboradores.isEmpty())
                }
            }
        }

        // Observa estatísticas
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.totalColaboradores.collect { total ->
                    binding.tvTotalColaboradores.text = total.toString()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.colaboradoresAtivos.collect { ativos ->
                    binding.tvColaboradoresAtivos.text = ativos.toString()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pendentesAprovacao.collect { pendentes ->
                    binding.tvPendentesAprovacao.text = pendentes.toString()
                }
            }
        }

        // Observa estado de loading
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.fabAddColaborador.isEnabled = !isLoading
                }
            }
        }

        // Observa mensagens
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.message.collect { message ->
                    message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearMessage()
                    }
                }
            }
        }

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

        // Observa verificação de acesso admin
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasAdminAccess.collect { hasAccess ->
                    if (!hasAccess) {
                        Toast.makeText(requireContext(), "Acesso negado. Apenas administradores podem gerenciar colaboradores.", Toast.LENGTH_LONG).show()
                        findNavController().navigateUp()
                    }
                }
            }
        }

        // Observa filtro atual
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filtroAtual.collect { filtro ->
                    atualizarChipsFiltro(filtro)
                }
            }
        }
    }

    private fun atualizarEstadoVazio(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvColaboradores.visibility = if (isEmpty) View.GONE else View.VISIBLE
        
        if (isEmpty) {
            val filtroAtual = viewModel.filtroAtual.value
            val mensagem = when (filtroAtual) {
                FiltroColaborador.TODOS -> "Nenhum colaborador cadastrado"
                FiltroColaborador.ATIVOS -> "Nenhum colaborador ativo"
                FiltroColaborador.PENDENTES -> "Nenhum colaborador pendente de aprovação"
                FiltroColaborador.ADMINISTRADORES -> "Nenhum administrador encontrado"
                else -> "Nenhum colaborador encontrado"
            }
            binding.tvEmptyMessage.text = mensagem
        }
    }

    private fun atualizarChipsFiltro(filtroAtual: FiltroColaborador) {
        binding.chipTodos.isChecked = filtroAtual == FiltroColaborador.TODOS
        binding.chipAtivos.isChecked = filtroAtual == FiltroColaborador.ATIVOS
        binding.chipPendentes.isChecked = filtroAtual == FiltroColaborador.PENDENTES
        binding.chipAdmins.isChecked = filtroAtual == FiltroColaborador.ADMINISTRADORES
    }

    private fun mostrarMenuOpcoes(colaborador: Colaborador) {
        val options = mutableListOf<String>()
        
        // Opções baseadas no status do colaborador
        if (!colaborador.aprovado) {
            options.add("Aprovar Colaborador")
        }
        
        if (colaborador.ativo) {
            options.add("Desativar Colaborador")
        } else {
            options.add("Ativar Colaborador")
        }
        
        options.add("Editar Colaborador")
        options.add("Excluir Colaborador")

        AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
            .setTitle("Opções para ${colaborador.nome}")
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Aprovar Colaborador" -> {
                        aprovarColaborador(colaborador)
                    }
                    "Ativar Colaborador" -> {
                        viewModel.alterarStatusColaborador(colaborador.id, true)
                    }
                    "Desativar Colaborador" -> {
                        viewModel.alterarStatusColaborador(colaborador.id, false)
                    }
                    "Editar Colaborador" -> {
                        // ✅ CORREÇÃO: Navegar para edição do colaborador
                        try {
                            val bundle = Bundle().apply {
                                putLong("colaborador_id", colaborador.id)
                            }
                            findNavController().navigate(R.id.colaboradorRegisterFragment, bundle)
                        } catch (e: Exception) {
                            Log.e("ColaboradorManagementFragment", "Erro ao navegar para edição: ${e.message}", e)
                            Toast.makeText(requireContext(), "Erro ao abrir edição de colaborador", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Excluir Colaborador" -> {
                        confirmarExclusao(colaborador)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun aprovarColaborador(colaborador: Colaborador) {
        // Usar o novo diálogo de aprovação com credenciais
        val approvalDialog = ColaboradorApprovalDialog(
            context = requireContext(),
            colaborador = colaborador,
            onApprovalConfirmed = { email, senha, nivelAcesso, observacoes ->
                viewModel.aprovarColaboradorComCredenciais(
                    colaboradorId = colaborador.id,
                    email = email,
                    senha = senha,
                    nivelAcesso = nivelAcesso,
                    observacoes = observacoes,
                    aprovadoPor = "Admin" // TODO: Pegar usuário atual
                )
            }
        )
        approvalDialog.show()
    }

    private fun confirmarExclusao(colaborador: Colaborador) {
        AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
            .setTitle("Excluir Colaborador")
            .setMessage("Tem certeza que deseja excluir o colaborador \"${colaborador.nome}\"?\n\nEsta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.deletarColaborador(colaborador)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



