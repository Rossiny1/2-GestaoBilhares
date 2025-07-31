package com.example.gestaobilhares.ui.routes

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentRoutesBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.RotaRepository
import com.google.android.material.navigation.NavigationView
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment que exibe a tela de rotas.
 * Mostra lista de rotas, estatísticas e permite adicionar novas rotas.
 * Implementa o padrão MVVM usando ViewBinding e ViewModel.
 * 
 * FASE 4: Inclui Navigation Drawer, dados reais e menu lateral.
 */
class RoutesFragment : Fragment() {

    // ViewBinding para acessar as views de forma type-safe
    private var _binding: FragmentRoutesBinding? = null
    private val binding get() = _binding!!

    // ViewModel instanciado diretamente
    private lateinit var viewModel: RoutesViewModel

    // Adapter para a lista de rotas
    private lateinit var routesAdapter: RoutesAdapter

    // Formatador de moeda
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoutesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel aqui onde o contexto está disponível
        val database = AppDatabase.getDatabase(requireContext())
        viewModel = RoutesViewModel(
            RotaRepository(
                database.rotaDao(),
                database.clienteDao(),
                database.mesaDao(),
                database.acertoDao(),
                database.cicloAcertoDao()
            )
        )
        
        setupRecyclerView()
        setupClickListeners()
        setupNavigationDrawer()
        setupBottomNavigation()
        observeViewModel()
    }

    /**
     * Configura o RecyclerView com o adapter.
     */
    private fun setupRecyclerView() {
        routesAdapter = RoutesAdapter { rotaResumo ->
            // Quando uma rota é clicada, navega para a lista de clientes
            viewModel.navigateToClients(rotaResumo)
        }

        binding.rotasRecyclerView.apply {
            adapter = routesAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Não usar nested scrolling já que está dentro de NestedScrollView
            isNestedScrollingEnabled = false
        }
    }

    /**
     * Configura o Navigation Drawer.
     */
    private fun setupNavigationDrawer() {
        // Configurar listener do menu lateral
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_manage_collaborator -> {
                    Toast.makeText(requireContext(), "Gerenciar Colaborador será implementado em breve", Toast.LENGTH_SHORT).show()
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_manage_expenses -> {
                    Toast.makeText(requireContext(), "Gerenciar Despesas será implementado em breve", Toast.LENGTH_SHORT).show()
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_manage_tables -> {
                    Toast.makeText(requireContext(), "Gerenciar Mesas será implementado em breve", Toast.LENGTH_SHORT).show()
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_manage_routes -> {
                    // Navegar para a tela de gerenciamento de rotas
                    try {
                        findNavController().navigate(R.id.routeManagementFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Log.e("RoutesFragment", "Erro ao navegar para gerenciamento de rotas: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir gerenciamento de rotas: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                R.id.nav_management_report -> {
                    Toast.makeText(requireContext(), "Relatório Gerencial será implementado em breve", Toast.LENGTH_SHORT).show()
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_message_settings -> {
                    Toast.makeText(requireContext(), "Configurações de Mensagens será implementado em breve", Toast.LENGTH_SHORT).show()
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_system_settings -> {
                    Toast.makeText(requireContext(), "Configuração do Sistema será implementado em breve", Toast.LENGTH_SHORT).show()
                    binding.drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Configura os listeners de clique dos botões.
     */
    private fun setupClickListeners() {
        // Botão de menu lateral
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(binding.navigationView)
        }

        // Botão de adicionar rota (FAB) - com controle de acesso
        binding.addRouteFab.setOnClickListener {
            viewModel.showAddRouteDialog()
        }

        // Botão de relatório de fechamento
        binding.reportFab.setOnClickListener {
            showReportConfirmationDialog()
        }

        // Botão de pesquisa
        binding.searchButton.setOnClickListener {
            Toast.makeText(requireContext(), "Busca será implementada em breve", Toast.LENGTH_SHORT).show()
        }

        // Botão de filtro
        binding.filterButton.setOnClickListener {
            Toast.makeText(requireContext(), "Filtros serão implementados em breve", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Mostra diálogo de confirmação para geração de relatório.
     */
    private fun showReportConfirmationDialog() {
        AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
            .setTitle("Gerar Relatório de Fechamento")
            .setMessage("Deseja gerar o relatório de fechamento das rotas? Esta ação pode demorar alguns minutos.")
            .setPositiveButton("Gerar") { _, _ ->
                viewModel.generateRouteClosureReport()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Configura a navegação inferior.
     */
    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.navigation_rotas
        
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_rotas -> {
                    // Já estamos na tela de rotas
                    true
                }
                R.id.navigation_acertos -> {
                    Toast.makeText(requireContext(), "Acertos será implementado em breve", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.navigation_cadastros -> {
                    Toast.makeText(requireContext(), "Cadastros será implementado em breve", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.navigation_relatorios -> {
                    try {
                        findNavController().navigate(R.id.reportsFragment)
                        true
                    } catch (e: Exception) {
                        Log.e("RoutesFragment", "Erro ao navegar para relatórios: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir relatórios: ${e.message}", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                else -> false
            }
        }
    }

    /**
     * Observa as mudanças no ViewModel e atualiza a UI.
     */
    private fun observeViewModel() {
        // Observa a lista de rotas
        viewModel.rotasResumo.observe(viewLifecycleOwner) { rotas ->
            routesAdapter.submitList(rotas)
        }

        // Observa as estatísticas gerais com dados reais
        viewModel.estatisticas.observe(viewLifecycleOwner) { stats ->
            binding.totalMesasCount.text = stats.totalMesas.toString()
            binding.totalClientesCount.text = stats.totalClientesAtivos.toString()
            binding.totalPendenciasCount.text = stats.totalPendencias.toString()
        }

        // Observa controle de acesso admin
        viewModel.isAdmin.observe(viewLifecycleOwner) { isAdmin ->
            binding.addRouteFab.visibility = if (isAdmin) View.VISIBLE else View.GONE
        }

        // Observa estado de loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.reportFab.isEnabled = !isLoading
            binding.addRouteFab.isEnabled = !isLoading
        }

        // Observa eventos de geração de relatório
        viewModel.generateReport.observe(viewLifecycleOwner) { shouldGenerate ->
            if (shouldGenerate) {
                Toast.makeText(requireContext(), "Relatório salvo em Documentos/GestaoBilhares", Toast.LENGTH_LONG).show()
                viewModel.reportGenerationCompleted()
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

        // Observa navegação para clientes
        viewModel.navigateToClients.observe(viewLifecycleOwner) { rotaId ->
            rotaId?.let {
                val bundle = Bundle().apply {
                    putLong("rotaId", it)
                }
                findNavController().navigate(R.id.action_routesFragment_to_clientListFragment, bundle)
                viewModel.navigationToClientsCompleted()
            }
        }

        // Observa quando mostrar diálogo de nova rota
        viewModel.showAddRouteDialog.observe(viewLifecycleOwner) { show ->
            if (show) {
                showAddRouteDialog()
            }
        }
    }

    /**
     * Mostra diálogo para adicionar nova rota.
     */
    private fun showAddRouteDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Nome da rota"
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Nova Rota")
            .setMessage("Digite o nome da nova rota:")
            .setView(editText)
            .setPositiveButton("Criar") { _, _ ->
                val nome = editText.text.toString().trim()
                if (nome.isNotEmpty()) {
                    viewModel.addNewRoute(nome)
                } else {
                    Toast.makeText(requireContext(), "Nome não pode estar vazio", Toast.LENGTH_SHORT).show()
                }
                viewModel.hideAddRouteDialog()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                viewModel.hideAddRouteDialog()
            }
            .setOnCancelListener {
                viewModel.hideAddRouteDialog()
            }
            .show()
    }

    /**
     * Limpa o binding quando o fragment é destruído.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
