package com.example.gestaobilhares.ui.routes
import com.example.gestaobilhares.ui.R

import android.os.Bundle
import timber.log.Timber
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import com.example.gestaobilhares.ui.databinding.FragmentRoutesBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.Mesa
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth

import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment que exibe a tela de rotas.
 * Mostra lista de rotas, estatísticas e permite adicionar novas rotas.
 * Implementa o padrão MVVM usando ViewBinding e ViewModel.
 * 
 * FASE 4: Inclui Navigation Drawer, dados reais e menu lateral.
 */
@AndroidEntryPoint
class RoutesFragment : Fragment() {

    // ViewBinding para acessar as views de forma type-safe
    private var _binding: FragmentRoutesBinding? = null
    private val binding get() = _binding!!
    private var syncDialog: androidx.appcompat.app.AlertDialog? = null

    // ✅ CORREÇÃO: ViewModel injetado pelo Hilt
    private val viewModel: RoutesViewModel by viewModels()
    private var isSyncing = false
    private var isExpanded = false

    // Adapter para a lista de rotas
    private lateinit var routesAdapter: RoutesAdapter

    // Formatador de moeda
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    
    // Gerenciador de sessão do usuário
    private lateinit var userSessionManager: UserSessionManager

    @javax.inject.Inject
    lateinit var syncRepository: com.example.gestaobilhares.sync.SyncRepository

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
        
        // ✅ LOG CRASH: Início da tela
        Timber.d("LOG_CRASH", "RoutesFragment.onViewCreated - INÍCIO")
        
        try {
            // Inicializar gerenciador de sessão primeiro
            Timber.d("LOG_CRASH", "RoutesFragment.onViewCreated - Inicializando UserSessionManager")
            userSessionManager = UserSessionManager.getInstance(requireContext())
            
            Timber.d("LOG_CRASH", "RoutesFragment.onViewCreated - Configurando RecyclerView")
            setupRecyclerView()
            
            Timber.d("LOG_CRASH", "RoutesFragment.onViewCreated - Configurando ClickListeners")
            setupClickListeners()
            
            Timber.d("LOG_CRASH", "RoutesFragment.onViewCreated - Configurando NavigationDrawer")
            setupNavigationDrawer()
            
            Timber.d("LOG_CRASH", "RoutesFragment.onViewCreated - Configurando Observers")
            observeViewModel()
            
            Timber.d("LOG_CRASH", "RoutesFragment.onViewCreated - Configurando botão voltar")
            // ✅ REMOVIDO: setupBackButtonHandler() - MainActivity agora gerencia o botão voltar globalmente
            
            Timber.d("LOG_CRASH", "RoutesFragment.onViewCreated - CONFIGURAÇÃO COMPLETA")
        } catch (e: Exception) {
            Timber.e("LOG_CRASH", "RoutesFragment.onViewCreated - ERRO: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao configurar tela de rotas: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // Submenu de despesas começa recolhido
        collapseExpenseSubmenu()

        // ✅ SOLUÇÃO DEFINITIVA: Verificar sincronização APENAS se viemos diretamente do login
        // Usa Navigation Component para detectar se a tela anterior é o LoginFragment
        // Isso PROÍBE completamente que o diálogo apareça ao retornar de outras telas
        try {
            val navController = findNavController()
            val previousBackStackEntry = navController.previousBackStackEntry
            val previousDestinationId = previousBackStackEntry?.destination?.id
            
            val isComingFromLogin = previousDestinationId == com.example.gestaobilhares.ui.R.id.loginFragment ||
                    previousDestinationId == com.example.gestaobilhares.ui.R.id.changePasswordFragment
            
            Timber.d("RoutesFragment", "🔍 Verificando origem da navegação:")
            Timber.d("RoutesFragment", "   Destino anterior: $previousDestinationId")
            Timber.d("RoutesFragment", "   Vindo do login: $isComingFromLogin")
            
            if (isComingFromLogin) {
                // ✅ APENAS se viemos do login - verificar sincronização
                Timber.d("RoutesFragment", "✅ Vindo do login - verificando sincronização após delay...")
                viewLifecycleOwner.lifecycleScope.launch {
                    kotlinx.coroutines.delay(500) // Delay para garantir que tudo esteja inicializado
                    viewModel.checkSyncPendencies(requireContext())
                }
            } else {
                // ✅ PROIBIDO: Se não viemos do login, NÃO verificar sincronização
                Timber.d("RoutesFragment", "🚫 NÃO vindo do login - PROIBIDO verificar sincronização")
                // O observer do syncDialogState já protege contra diálogos não autorizados
            }
        } catch (e: Exception) {
            // Se não conseguir verificar navegação, não verificar sincronização (seguro)
            Timber.w("RoutesFragment", "⚠️ Erro ao verificar navegação: ${e.message} - não verificando sincronização")
        }
    }

    override fun onResume() {
        super.onResume()
        // ✅ CORREÇÃO: Atualizar dados das rotas quando retorna de outras telas
        Timber.d("RoutesFragment", "🔄 onResume - Forçando atualização dos dados das rotas")
        
        // ✅ CORREÇÃO: Verificar sessão antes de atualizar
        val userId = userSessionManager.getCurrentUserId()
        Timber.d("RoutesFragment", "🔍 onResume - Usuário logado: ${userId != 0L}")
        
        viewModel.refresh()
        // ✅ CORREÇÃO CRÍTICA: NÃO verificar sincronização no onResume
        // O diálogo de sincronização só deve aparecer após o login (onViewCreated)
        // Se aparecer no onResume, fica em loop quando o usuário navega entre telas
        Timber.d("RoutesFragment", "ℹ️ onResume - Não verificando sincronização (só aparece após login)")
    }


    /**
     * Configura o RecyclerView com o adapter.
     */
    private fun setupRecyclerView() {
        routesAdapter = RoutesAdapter(
            onItemClick = { rotaResumo ->
                // Quando uma rota é clicada, navega para a lista de clientes
                viewModel.navigateToClients(rotaResumo)
            }
        )

        binding.rotasRecyclerView.apply {
            adapter = routesAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Não usar nested scrolling já que está dentro de NestedScrollView
            isNestedScrollingEnabled = false
        }
    }

    /**
     * Configura o Navigation Drawer com controle de acesso baseado no nível do usuário.
     */
    private fun setupNavigationDrawer() {
        // ✅ NOVO: Controlar visibilidade do menu baseado no nível do usuário
        val hasMenuAccess = userSessionManager.hasMenuAccess()
        
        // Ocultar menu completo para usuários USER
        if (!hasMenuAccess) {
            binding.navigationView.visibility = View.GONE
            binding.btnMenu.visibility = View.GONE
            Timber.d("RoutesFragment", "🔒 Menu oculto para usuário: ${userSessionManager.getCurrentUserName()}")
            return
        }
        
        Timber.d("RoutesFragment", "🔓 Menu disponível para ADMIN: ${userSessionManager.getCurrentUserName()}")
        
        // Configurar listener do menu lateral
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                com.example.gestaobilhares.ui.R.id.nav_manage_collaborator -> {
                    // Verificar permissão para gerenciar colaboradores
                    if (!userSessionManager.canManageCollaborators()) {
                        Toast.makeText(requireContext(), "Acesso negado: Apenas administradores podem gerenciar colaboradores", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        return@setNavigationItemSelectedListener false
                    }
                    
                    // Navegar para a tela de gerenciamento de colaboradores
                    try {
                        findNavController().navigate(com.example.gestaobilhares.ui.R.id.colaboradorManagementFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Timber.e("RoutesFragment", "Erro ao navegar para gerenciamento de colaboradores: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir gerenciamento de colaboradores: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }

                com.example.gestaobilhares.ui.R.id.nav_expense_categories -> {
                    try {
                        findNavController().navigate(com.example.gestaobilhares.ui.R.id.expenseCategoriesFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Timber.e("RoutesFragment", "Erro ao navegar para categorias de despesa: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir categorias de despesa: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                com.example.gestaobilhares.ui.R.id.nav_expense_types -> {
                    try {
                        findNavController().navigate(com.example.gestaobilhares.ui.R.id.expenseTypesFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Timber.e("RoutesFragment", "Erro ao navegar para tipos de despesa: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir tipos de despesa: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                com.example.gestaobilhares.ui.R.id.nav_manage_tables -> {
                    // Verificar permissão para gerenciar mesas
                    if (!userSessionManager.canManageTables()) {
                        Toast.makeText(requireContext(), "Acesso negado: Apenas administradores podem gerenciar mesas", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        return@setNavigationItemSelectedListener false
                    }
                    
                    // Navegar para a tela de gerenciar mesas
                    val action = RoutesFragmentDirections.actionRoutesFragmentToGerenciarMesasFragment()
                    findNavController().navigate(action)
                    binding.drawerLayout.closeDrawers()
                    true
                }
                com.example.gestaobilhares.ui.R.id.nav_expense_management -> {
                    // Expandir/colapsar sem fechar o drawer
                    toggleExpenseSubmenu()
                    // Não fechar o drawer
                    true
                }
                com.example.gestaobilhares.ui.R.id.nav_expense_quick_add -> {
                    try {
                        // Navegar para tela de despesas globais
                        findNavController().navigate(com.example.gestaobilhares.ui.R.id.globalExpensesFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Timber.e("RoutesFragment", "Erro ao abrir +Despesa: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir +Despesa: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                com.example.gestaobilhares.ui.R.id.nav_manage_routes -> {
                    // Verificar permissão para gerenciar rotas
                    if (!userSessionManager.canManageRoutes()) {
                        Toast.makeText(requireContext(), "Acesso negado: Apenas administradores podem gerenciar rotas", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        return@setNavigationItemSelectedListener false
                    }
                    
                    // Navegar para a tela de gerenciamento de rotas
                    try {
                        findNavController().navigate(com.example.gestaobilhares.ui.R.id.routeManagementFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Timber.e("RoutesFragment", "Erro ao navegar para gerenciamento de rotas: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir gerenciamento de rotas: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                com.example.gestaobilhares.ui.R.id.nav_manage_contracts -> {
                    // Verificar permissão para gerenciar contratos
                    if (!userSessionManager.canManageContracts()) {
                        Toast.makeText(requireContext(), "Acesso negado: Apenas administradores podem gerenciar contratos", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        return@setNavigationItemSelectedListener false
                    }
                    
                    // Navegar para a tela de gerenciamento de contratos
                    try {
                        findNavController().navigate(com.example.gestaobilhares.ui.R.id.contractManagementFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Timber.e("RoutesFragment", "Erro ao navegar para gerenciamento de contratos: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir gerenciamento de contratos: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                com.example.gestaobilhares.ui.R.id.nav_inventory -> {
                    try {
                        findNavController().navigate(com.example.gestaobilhares.ui.R.id.inventorySelectionDialog)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Timber.e("RoutesFragment", "Erro ao abrir Inventário: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir Inventário: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                com.example.gestaobilhares.ui.R.id.nav_metas -> {
                    try {
                        findNavController().navigate(com.example.gestaobilhares.ui.R.id.metasFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Timber.e("RoutesFragment", "Erro ao abrir metas: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir metas: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                com.example.gestaobilhares.ui.R.id.nav_dashboard -> {
                    try {
                        findNavController().navigate(com.example.gestaobilhares.ui.R.id.dashboardFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Timber.e("RoutesFragment", "Erro ao navegar para dashboard: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir dashboard: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                com.example.gestaobilhares.ui.R.id.nav_closure_report -> {
                    try {
                        findNavController().navigate(com.example.gestaobilhares.ui.R.id.closureReportFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Timber.e("RoutesFragment", "Erro ao navegar para fechamento: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir fechamento: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                com.example.gestaobilhares.ui.R.id.nav_message_settings -> {
                    Toast.makeText(requireContext(), "Configurações de Mensagens será implementado em breve", Toast.LENGTH_SHORT).show()
                    binding.drawerLayout.closeDrawers()
                    true
                }
                com.example.gestaobilhares.ui.R.id.nav_system_settings -> {
                    Toast.makeText(requireContext(), "Configuração do Sistema será implementado em breve", Toast.LENGTH_SHORT).show()
                    binding.drawerLayout.closeDrawers()
                    true
                }
                com.example.gestaobilhares.ui.R.id.nav_logout -> {
                    // ✅ NOVO: Implementar logout completo
                    Timber.d("RoutesFragment", "=== INICIANDO LOGOUT ===")
                    Timber.d("RoutesFragment", "Usuário atual: ${userSessionManager.getCurrentUserName()}")
                    
                    try {
                        // Encerrar sessão local
                        userSessionManager.endSession()
                        Timber.d("RoutesFragment", "✅ Sessão local encerrada")
                        
                        // Fechar drawer
                        binding.drawerLayout.closeDrawers()
                        
                        // Mostrar mensagem de sucesso
                        Toast.makeText(requireContext(), "Logout realizado com sucesso!", Toast.LENGTH_SHORT).show()
                        
                        // Navegar de volta para login
                        findNavController().navigate(com.example.gestaobilhares.ui.R.id.action_routesFragment_to_loginFragment)
                        
                        true
                    } catch (e: Exception) {
                        Timber.e("RoutesFragment", "Erro no logout: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao fazer logout: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                else -> false
            }
        }

        // Estilizar inicialmente o submenu recolhido
        collapseExpenseSubmenu()
        styleExpenseSubmenu()
    }

    private fun toggleExpenseSubmenu() {
        val menu = binding.navigationView.menu
        val ids = listOf(com.example.gestaobilhares.ui.R.id.nav_expense_categories, com.example.gestaobilhares.ui.R.id.nav_expense_types, com.example.gestaobilhares.ui.R.id.nav_expense_quick_add)
        val visible = !menu.findItem(com.example.gestaobilhares.ui.R.id.nav_expense_categories).isVisible
        ids.forEach { id -> menu.findItem(id).isVisible = visible }
        styleExpenseSubmenu()
    }

    private fun collapseExpenseSubmenu() {
        val menu = binding.navigationView.menu
        listOf(com.example.gestaobilhares.ui.R.id.nav_expense_categories, com.example.gestaobilhares.ui.R.id.nav_expense_types, com.example.gestaobilhares.ui.R.id.nav_expense_quick_add).forEach { id ->
            menu.findItem(id).isVisible = false
        }
    }

    private fun styleExpenseSubmenu() {
        val menu = binding.navigationView.menu
        val colorWhite = ContextCompat.getColor(requireContext(), android.R.color.white)
        val indentPx = (24 * resources.displayMetrics.density).toInt()
        listOf(com.example.gestaobilhares.ui.R.id.nav_expense_categories, com.example.gestaobilhares.ui.R.id.nav_expense_types, com.example.gestaobilhares.ui.R.id.nav_expense_quick_add).forEach { id ->
            val item = menu.findItem(id)
            val title = item.title?.toString() ?: return@forEach
            val ss = SpannableString(title)
            ss.setSpan(ForegroundColorSpan(colorWhite), 0, ss.length, 0)
            ss.setSpan(LeadingMarginSpan.Standard(indentPx, indentPx), 0, ss.length, 0)
            item.title = ss
        }
    }

    /**
     * Configura os listeners de clique dos botões.
     */
    private fun setupClickListeners() {
        // ✅ LOG CRASH: Início do setup de click listeners
        Timber.d("LOG_CRASH", "RoutesFragment.setupClickListeners - INÍCIO")
        
        try {
            // Botão de menu lateral
            Timber.d("LOG_CRASH", "RoutesFragment.setupClickListeners - Configurando botão de menu")
            binding.btnMenu.setOnClickListener {
                Timber.d("LOG_CRASH", "RoutesFragment.setupClickListeners - Clique no botão de menu")
                binding.drawerLayout.openDrawer(binding.navigationView)
            }


        // ✅ NOVO: Configurar FAB expansível
        configurarFabExpandivel()

        // Botão de sincronização
            binding.syncButton.setOnClickListener {
                performManualSync()
            }
        } catch (e: Exception) {
            Timber.e("LOG_CRASH", "RoutesFragment.setupClickListeners - ERRO: ${e.message}", e)
        }
    }


    /**
     * Observa as mudanças no ViewModel e atualiza a UI.
     */
    private fun observeViewModel() {
        // ✅ LOG CRASH: Início do observeViewModel
        Timber.d("LOG_CRASH", "RoutesFragment.observeViewModel - INÍCIO")
        
        try {
            // ✅ MODERNIZADO: Observa a lista de rotas com StateFlow
            Timber.d("LOG_CRASH", "RoutesFragment.observeViewModel - Configurando observer de rotas")
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.rotasResumo.collect { rotas ->
                        Timber.d("LOG_CRASH", "RoutesFragment.observeViewModel - Recebidas ${rotas.size} rotas")
                        routesAdapter.submitList(rotas)
                    }
                }
            }

            // ✅ MODERNIZADO: Observa as estatísticas gerais com StateFlow
            Timber.d("LOG_CRASH", "RoutesFragment.observeViewModel - Configurando observer de estatísticas")
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.estatisticas.collect { stats ->
                        Timber.d("LOG_CRASH", "RoutesFragment.observeViewModel - Recebidas estatísticas: mesas=${stats.totalMesas}, clientes=${stats.totalClientesAtivos}, pendências=${stats.totalPendencias}")
                        binding.totalMesasCount.text = stats.totalMesas.toString()
                        binding.totalClientesCount.text = stats.totalClientesAtivos.toString()
                        binding.totalPendenciasCount.text = stats.totalPendencias.toString()
                    }
                }
            }


            // ✅ MODERNIZADO: Observa mensagens de erro com StateFlow
            Timber.d("LOG_CRASH", "RoutesFragment.observeViewModel - Configurando observer de mensagens de erro")
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.errorMessage.collect { message ->
                        message?.let {
                            Timber.e("LOG_CRASH", "RoutesFragment.observeViewModel - ERRO recebido: $it")
                            Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                            viewModel.clearMessages()
                        }
                    }
                }
            }
            
            Timber.d("LOG_CRASH", "RoutesFragment.observeViewModel - CONFIGURAÇÃO COMPLETA")
        } catch (e: Exception) {
            Timber.e("LOG_CRASH", "RoutesFragment.observeViewModel - ERRO: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao configurar observadores: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // ✅ MODERNIZADO: Observa mensagens de sucesso com StateFlow
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

        // ✅ MODERNIZADO: Observa navegação para clientes com StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigateToClients.collect { rotaId ->
                    rotaId?.let {
                        val bundle = Bundle().apply {
                            putLong("rotaId", it)
                        }
                        findNavController().navigate(com.example.gestaobilhares.ui.R.id.action_routesFragment_to_clientListFragment, bundle)
                        viewModel.navigationToClientsCompleted()
                    }
                }
            }
        }

        // ✅ REFATORADO: Observa estado do diálogo de sincronização
        // PROIBE que apareça se não viemos do login
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.syncDialogState.collect { state ->
                    if (state == null) {
                        syncDialog?.dismiss()
                        syncDialog = null
                    } else {
                        // ✅ PROTEÇÃO ADICIONAL: Verificar se ainda estamos vindo do login
                        // Se não estivermos, descartar o estado do diálogo
                        try {
                            val navController = findNavController()
                            val previousBackStackEntry = navController.previousBackStackEntry
                            val previousDestinationId = previousBackStackEntry?.destination?.id
                            val isComingFromLogin = previousDestinationId == com.example.gestaobilhares.ui.R.id.loginFragment ||
                                    previousDestinationId == com.example.gestaobilhares.ui.R.id.changePasswordFragment
                            
                            if (!isComingFromLogin) {
                                Timber.d("RoutesFragment", "🚫 Diálogo tentou aparecer mas não viemos do login - PROIBIDO")
                                viewModel.dismissSyncDialog(requireContext())
                                return@collect
                            }
                        } catch (e: Exception) {
                            Timber.w("RoutesFragment", "⚠️ Erro ao verificar navegação no observer: ${e.message}")
                            // Em caso de erro, não mostrar diálogo (seguro)
                            viewModel.dismissSyncDialog(requireContext())
                            return@collect
                        }
                        
                        if (syncDialog?.isShowing == true) {
                            syncDialog?.dismiss()
                        }
                        // ✅ CORREÇÃO: Mensagem mais clara indicando o que precisa ser sincronizado
                        val message = when {
                            state.hasLocalPending && state.isCloudData -> {
                                "Há dados para importar da nuvem e exportar para a nuvem. Deseja sincronizar agora?"
                            }
                            state.isCloudData -> {
                                "Há dados disponíveis na nuvem que podem ser importados. Deseja sincronizar agora?"
                            }
                            state.hasLocalPending -> {
                                resources.getQuantityString(
                                    com.example.gestaobilhares.ui.R.plurals.sync_pending_message,
                                    state.pendingCount,
                                    state.pendingCount
                                )
                            }
                            else -> {
                                "Há dados pendentes de sincronização. Deseja sincronizar agora?"
                            }
                        }
                        syncDialog = MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Sincronização pendente")
                            .setMessage(message)
                            .setPositiveButton("Sincronizar agora") { _, _ ->
                                // ✅ REFATORADO: Marcar como mostrado usando SharedPreferences
                                viewModel.dismissSyncDialog(requireContext())
                                performManualSync()
                            }
                            .setNegativeButton("Agora não") { dialog, _ ->
                                // ✅ REFATORADO: Marcar como mostrado usando SharedPreferences
                                viewModel.dismissSyncDialog(requireContext())
                                dialog.dismiss()
                            }
                            .setCancelable(false)
                            .create()
                        syncDialog?.show()
                    }
                }
            }
        }
    }


    /**
     * Limpa o binding quando o fragment é destruído.
     */
    /**
     * Mostra o diálogo de seleção de cliente para transferência.
     */
    private fun showTransferClientDialog() {
        val dialog = ClientSelectionDialog.newInstance()
        dialog.setOnClientSelectedListener { cliente, rota, mesas ->
            // Mostrar o diálogo de transferência com os dados selecionados
            showTransferDialog(cliente, rota, mesas)
        }
        dialog.show(parentFragmentManager, "ClientSelectionDialog")
    }

    /**
     * Mostra o diálogo de transferência com os dados do cliente selecionado.
     */
    private fun showTransferDialog(cliente: Cliente, rota: Rota, mesas: List<Mesa>) {
        val dialog = TransferClientDialog.newInstance(cliente, rota, mesas)
        dialog.setOnTransferSuccessListener {
            // ✅ CORREÇÃO: Forçar atualização dos dados após transferência
            Timber.d("RoutesFragment", "✅ Transferência concluída - Forçando atualização dos dados")
            viewModel.refresh()
        }
        dialog.show(parentFragmentManager, "TransferClientDialog")
    }

    /**
     * Executa sincronização manual dos dados com o Firestore.
     * Mostra feedback visual e status da operação.
     */
    private fun performManualSync() {
        var progressDialog: androidx.appcompat.app.AlertDialog? = null
        try {
            Timber.d("RoutesFragment", "🔄 Iniciando sincronização manual")
            
            if (isSyncing) {
                Toast.makeText(requireContext(), "Sincronização já em andamento.", Toast.LENGTH_SHORT).show()
                return
            }
            isSyncing = true

            // ✅ CORREÇÃO CRÍTICA: Verificar sessão local em vez de Firebase Auth
            // O login híbrido pode funcionar offline sem autenticação Firebase
            val userId = userSessionManager.getCurrentUserId()
            if (userId == 0L) {
                Timber.w("RoutesFragment", "⚠️ Nenhum usuário logado na sessão local")
                Toast.makeText(requireContext(), "⚠️ Faça login para sincronizar dados", Toast.LENGTH_LONG).show()
                isSyncing = false
                return
            }
            
            Timber.d("RoutesFragment", "✅ Usuário logado detectado (ID: $userId)")
            
            // Mostrar feedback visual
            _binding?.let { b ->
                b.syncButton.isEnabled = false
                b.syncButton.alpha = 0.5f
            }

            val progressView = layoutInflater.inflate(com.example.gestaobilhares.ui.R.layout.dialog_sync_progress, null)
            val progressBar = progressView.findViewById<ProgressBar>(com.example.gestaobilhares.ui.R.id.syncProgressBar)
            val progressPercent = progressView.findViewById<TextView>(com.example.gestaobilhares.ui.R.id.tvSyncProgressPercent)
            val progressStatus = progressView.findViewById<TextView>(com.example.gestaobilhares.ui.R.id.tvSyncProgressStatus)
            progressBar.progress = 0
            progressPercent.text = "0%"
            progressStatus.text = getString(com.example.gestaobilhares.ui.R.string.sync_status_preparing)

            progressDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(com.example.gestaobilhares.ui.R.string.sync_progress_title)
                .setView(progressView)
                .setCancelable(false)
                .create()
            progressDialog.show()

            val uiScope = viewLifecycleOwner.lifecycleScope

            // Executar sincronização em background
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // ✅ REFATORADO: Usar SyncRepository injetado
                    // O SyncRepository agora é injetado pelo Hilt no Fragment
                    
                    // Executar sincronização bidirecional
                    Timber.d("RoutesFragment", "🔄 Iniciando sincronização bidirecional...")
                    val result = withContext(Dispatchers.IO) {
                        syncRepository.syncBidirectional { progress ->
                            uiScope.launch {
                                // Check if view is still available before updating UI
                                if (_binding != null) {
                                    progressBar.progress = progress.percent
                                    progressPercent.text = "${progress.percent}%"
                                    progressStatus.text = progress.message
                                }
                            }
                        }
                    }
                    
                    if (result.isSuccess) {
                        val status = syncRepository.getSyncStatus()
                        Timber.d("RoutesFragment", "✅ Sincronização concluída com sucesso")
                        Timber.d("RoutesFragment", "   Pendentes: ${status.pendingOperations}")
                        Timber.d("RoutesFragment", "   Falhas: ${status.failedOperations}")

                        // Check if view is still available before updating UI
                        if (_binding != null) {
                            progressBar.progress = 100
                            progressPercent.text = "100%"
                            progressStatus.text = getString(com.example.gestaobilhares.ui.R.string.sync_status_completed) + 
                                "\nPendentes: ${status.pendingOperations}\n" +
                                "Falhas: ${status.failedOperations}"
                        }
                        
                        // Forçar atualização completa dos dados das rotas após sincronização
                        Timber.d("RoutesFragment", "🔄 Aguardando processamento dos dados...")
                        kotlinx.coroutines.delay(2000)
                        
                        Timber.d("RoutesFragment", "🔄 Forçando refresh dos dados...")
                        viewModel.refresh()
                        
                        kotlinx.coroutines.delay(1000)
                        viewModel.refresh()
                        
                        Timber.d("RoutesFragment", "✅ Refresh concluído")
                    } else {
                        val status = syncRepository.getSyncStatus()
                        Timber.e("RoutesFragment", "❌ Sincronização falhou: ${status.error ?: "Erro desconhecido"}")
                        // Check if view is still available before updating UI
                        if (_binding != null) {
                            progressStatus.text = "⚠️ Sincronização falhou: ${status.error ?: "Erro desconhecido"}\n" +
                                "Verifique os logs para mais detalhes"
                        }
                    }
                    
                } catch (e: Exception) {
                    Timber.e("RoutesFragment", "Erro na sincronização: ${e.message}", e)
                    // Check if view is still available before updating UI
                    if (_binding != null) {
                        progressStatus.text = "❌ Erro na sincronização: ${e.message ?: "Erro desconhecido"}"
                    }
                } finally {
                    progressDialog.dismiss()
                    // Restaurar UI - Use _binding? para evitar NPE se navegar durante o sync
                    _binding?.let { b ->
                        b.syncButton.isEnabled = true
                        b.syncButton.alpha = 1.0f
                    }
                    isSyncing = false
                    // ✅ CORREÇÃO: Não verificar pendências após sincronização manual
                    // Isso evita que o diálogo reapareça em loop
                    // O diálogo só aparecerá novamente no próximo login ou se o usuário solicitar
                }
            }
            
        } catch (e: Exception) {
            Timber.e("RoutesFragment", "Erro ao iniciar sincronização: ${e.message}", e)
            Toast.makeText(requireContext(), "❌ Erro ao sincronizar: ${e.message}", Toast.LENGTH_LONG).show()
            progressDialog?.dismiss()
            _binding?.let { b ->
                b.syncButton.isEnabled = true
                b.syncButton.alpha = 1.0f
            }
            isSyncing = false
            // ✅ CORREÇÃO: Não verificar pendências após erro na sincronização
            // Isso evita que o diálogo reapareça em loop
        }
    }

    /**
     * ✅ NOVO: Configura o FAB expandível com animações
     */
    private fun configurarFabExpandivel() {
        var isExpanded = false
        
        // FAB Principal
        binding.fabMain.setOnClickListener {
            if (isExpanded) {
                // Recolher FABs
                recolherFabMenu()
                isExpanded = false
            } else {
                // Expandir FABs
                expandirFabMenu()
                isExpanded = true
            }
        }
        
        // Garantir que o FAB interno não capture cliques (deve ser apenas visual)
        binding.fabMaintenance.setOnClickListener(null)
        binding.fabMaintenance.isClickable = false
        binding.fabMaintenance.isFocusable = false
        
        // Container Manutenção Mesa
        binding.fabMaintenanceContainer.setOnClickListener { view ->
            // Navegar para a tela de mesas reformadas
            try {
                Timber.d("LOG_CRASH", "RoutesFragment - Clique em Manutenção Mesa")
                
                // Verificar se o fragmento está anexado e o NavController está disponível
                if (!isAdded || this.view == null) {
                    Timber.e("LOG_CRASH", "RoutesFragment - Fragment não está anexado ou view é null")
                    return@setOnClickListener
                }
                
                // Verificar se o NavController está disponível
                val navController = try {
                    findNavController()
                } catch (e: IllegalStateException) {
                    Timber.e("LOG_CRASH", "RoutesFragment - NavController não disponível: ${e.message}", e)
                    Toast.makeText(requireContext(), "Erro ao navegar. Tente novamente.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // ✅ CORREÇÃO: Recolher menu ANTES da navegação para evitar crash
                // Quando navegamos, o fragment é destruído e a animação tenta acessar binding null
                Timber.d("LOG_CRASH", "RoutesFragment - Recolhendo menu antes da navegação")
                if (isAdded && view != null && _binding != null) {
                    // Ocultar container imediatamente sem animação para evitar problemas
                    _binding?.fabExpandedContainer?.visibility = View.GONE
                    _binding?.fabMain?.rotation = 0f
                    isExpanded = false
                }
                
                // Usar a action definida no nav_graph ao invés de navegar diretamente pelo ID
                val action = RoutesFragmentDirections.actionRoutesFragmentToMesasReformadasFragment()
                navController.navigate(action)
                
                Timber.d("LOG_CRASH", "RoutesFragment - Navegação para mesas reformadas concluída")
            } catch (e: IllegalStateException) {
                Timber.e("LOG_CRASH", "RoutesFragment - Erro de estado ao navegar: ${e.message}", e)
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "Erro ao abrir reforma de mesas. Tente novamente.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Timber.e("LOG_CRASH", "RoutesFragment - Erro ao navegar para mesas reformadas: ${e.message}", e)
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "Erro ao abrir reforma de mesas: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Container Transferir Cliente
        _binding?.fabTransferContainer?.setOnClickListener {
            showTransferClientDialog()
            // Recolher menu após clicar
            recolherFabMenu()
            isExpanded = false
        }
    }
    
    /**
     * ✅ NOVO: Expande o menu FAB com animação
     */
    private fun expandirFabMenu() {
        val b = _binding ?: return
        Timber.d("LOG_CRASH", "RoutesFragment.expandirFabMenu - INÍCIO")
        try {
            b.fabExpandedContainer.visibility = View.VISIBLE
            Timber.d("LOG_CRASH", "RoutesFragment.expandirFabMenu - Container expandido visível")
            
            // Animar entrada dos containers
            b.fabMaintenanceContainer.alpha = 0f
            b.fabTransferContainer.alpha = 0f
            
            // Animar "Transferir Cliente" primeiro (mais próximo do FAB principal)
            b.fabTransferContainer.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(100)
                .start()
                
            // Animar "Manutenção Mesa" depois (mais afastado do FAB principal)
            b.fabMaintenanceContainer.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(200)
                .start()
                
            // Rotacionar ícone do FAB principal
            b.fabMain.animate()
                .rotation(45f)
                .setDuration(200)
                .start()
            
            Timber.d("LOG_CRASH", "RoutesFragment.expandirFabMenu - Animações iniciadas")
        } catch (e: Exception) {
            Timber.e("LOG_CRASH", "RoutesFragment.expandirFabMenu - ERRO: ${e.message}", e)
        }
    }
    
    /**
     * ✅ NOVO: Recolhe o menu FAB com animação
     */
    private fun recolherFabMenu() {
        Timber.d("LOG_CRASH", "RoutesFragment.recolherFabMenu - INÍCIO")
        try {
            // Verificar se o binding ainda está disponível
            val currentBinding = _binding
            if (currentBinding == null) {
                Timber.w("LOG_CRASH", "RoutesFragment.recolherFabMenu - Binding é null, pulando animação")
                return
            }
            
            // Verificar se o fragment ainda está ativo
            if (!isAdded || view == null) {
                Timber.w("LOG_CRASH", "RoutesFragment.recolherFabMenu - Fragment não está ativo, pulando animação")
                return
            }
            
            // Animar saída dos containers
            currentBinding.fabMaintenanceContainer.animate()
                .alpha(0f)
                .setDuration(200)
                .start()
                
            currentBinding.fabTransferContainer.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    // ✅ CORREÇÃO: Verificar se o fragment ainda está ativo antes de acessar o binding
                    try {
                        if (isAdded && view != null && _binding != null) {
                            _binding?.fabExpandedContainer?.visibility = View.GONE
                            Timber.d("LOG_CRASH", "RoutesFragment.recolherFabMenu - Container ocultado")
                        } else {
                            Timber.w("LOG_CRASH", "RoutesFragment.recolherFabMenu - Fragment destruído, pulando ocultação do container")
                        }
                    } catch (e: Exception) {
                        Timber.e("LOG_CRASH", "RoutesFragment.recolherFabMenu - Erro no withEndAction: ${e.message}", e)
                    }
                }
                .start()
                
            // Rotacionar ícone do FAB principal de volta
            currentBinding.fabMain.animate()
                .rotation(0f)
                .setDuration(200)
                .start()
            
            Timber.d("LOG_CRASH", "RoutesFragment.recolherFabMenu - Animações iniciadas")
        } catch (e: Exception) {
            Timber.e("LOG_CRASH", "RoutesFragment.recolherFabMenu - ERRO: ${e.message}", e)
        }
    }

    // ✅ REMOVIDO: setupBackButtonHandler() e showExitConfirmationDialog()
    // MainActivity agora gerencia o botão voltar globalmente

    override fun onDestroyView() {
        super.onDestroyView()
        syncDialog?.dismiss()
        syncDialog = null
        // ✅ REMOVIDO: Remoção de callback - MainActivity gerencia globalmente
        _binding = null
    }
} 

