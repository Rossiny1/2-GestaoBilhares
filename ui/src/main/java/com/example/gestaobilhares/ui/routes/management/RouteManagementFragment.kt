package com.example.gestaobilhares.ui.routes.management

import android.os.Bundle
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
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.ui.R
import com.example.gestaobilhares.ui.databinding.FragmentRouteManagementBinding
import com.example.gestaobilhares.ui.databinding.DialogAddEditRouteBinding
import com.example.gestaobilhares.data.entities.Rota
import com.google.android.material.dialog.MaterialAlertDialogBuilder
/**
 * Fragment para gerenciamento de rotas (CRUD).
 * Permite criar, editar e excluir rotas.
 * Acesso restrito apenas para administradores.
 */
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment para gerenciamento de rotas (CRUD).
 * Permite criar, editar e excluir rotas.
 * Acesso restrito apenas para administradores.
 */
@AndroidEntryPoint
class RouteManagementFragment : Fragment() {

    private var _binding: FragmentRouteManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RouteManagementViewModel by viewModels()
    
    private lateinit var routeAdapter: RouteManagementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Verificar se o usuário é admin
        viewModel.checkAdminAccess()
        
        // ✅ NOVO: Carregar rotas ao inicializar o Fragment
        viewModel.loadRotas()
    }

    /**
     * Configura o RecyclerView com o adapter para gerenciamento de rotas.
     */
    private fun setupRecyclerView() {
        routeAdapter = RouteManagementAdapter(
            onEditClick = { rota ->
                showAddEditRouteDialog(rota)
            },
            onDeleteClick = { rota ->
                showDeleteConfirmationDialog(rota)
            }
        )

        binding.routesRecyclerView.apply {
            adapter = routeAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    /**
     * Configura os listeners de clique dos botões.
     */
    private fun setupClickListeners() {
        // Botão voltar
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Botão adicionar nova rota
        binding.addRouteFab.setOnClickListener {
            showAddEditRouteDialog(null)
        }
    }

    /**
     * Observa as mudanças no ViewModel e atualiza a UI.
     */
    private fun observeViewModel() {
        // Observa a lista de rotas
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rotas.collect { rotas ->
                    routeAdapter.submitList(rotas)
                    
                    // Atualiza contador
                    binding.totalRoutesCount.text = rotas.size.toString()
                }
            }
        }

        // Observa mensagens de erro
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { message ->
                    message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                        viewModel.clearMessages()
                    }
                }
            }
        }

        // Observa mensagens de sucesso
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.successMessage.collect { message ->
                    message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearMessages()
                    }
                }
            }
        }

        // Observa estado de loading
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    // ProgressBar não existe no layout, apenas desabilitar FAB durante loading
                    binding.addRouteFab.isEnabled = !isLoading
                }
            }
        }

        // Observa verificação de acesso admin
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasAdminAccess.collect { hasAccess ->
                    if (!hasAccess) {
                        Toast.makeText(requireContext(), "Acesso negado. Apenas administradores podem gerenciar rotas.", Toast.LENGTH_LONG).show()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    /**
     * Mostra o diálogo para adicionar ou editar uma rota.
     */
    private fun showAddEditRouteDialog(rota: Rota?) {
        val dialogBinding = DialogAddEditRouteBinding.inflate(layoutInflater)
        
        // Se estiver editando, preencher os campos
        rota?.let {
            dialogBinding.nomeEditText.setText(it.nome)
            dialogBinding.cidadesEditText.setText(it.cidades)
            dialogBinding.colaboradorEditText.setText(it.colaboradorResponsavel)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (rota == null) "Nova Rota" else "Editar Rota")
            .setView(dialogBinding.root)
            .setPositiveButton(if (rota == null) "Criar" else "Salvar") { _, _ ->
                val nome = dialogBinding.nomeEditText.text.toString().trim()
                val cidades = dialogBinding.cidadesEditText.text.toString().trim()
                val colaborador = dialogBinding.colaboradorEditText.text.toString().trim()

                if (nome.isBlank()) {
                    Toast.makeText(requireContext(), "Nome da rota é obrigatório", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (rota == null) {
                    // Criar nova rota
                    viewModel.createRoute(nome, colaborador, cidades)
                } else {
                    // Editar rota existente
                    viewModel.updateRoute(rota.copy(
                        nome = nome,
                        colaboradorResponsavel = colaborador,
                        cidades = cidades,
                        dataAtualizacao = System.currentTimeMillis()
                    ))
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    /**
     * Mostra o diálogo de confirmação para excluir uma rota.
     */
    private fun showDeleteConfirmationDialog(rota: Rota) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Excluir Rota")
            .setMessage("Tem certeza que deseja excluir a rota \"${rota.nome}\"?\n\nEsta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.deleteRoute(rota)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 

