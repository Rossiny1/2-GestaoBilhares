package com.example.gestaobilhares.ui.routes

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
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentRoutesBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.utils.UserSessionManager
import com.google.android.material.navigation.NavigationView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.Mesa
import java.text.NumberFormat
import java.util.Locale
// import dagger.hilt.android.AndroidEntryPoint // REMOVIDO: Hilt nao e mais usado
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

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

    // ViewModel com Hilt
    private val viewModel: RoutesViewModel by viewModels()

    // Adapter para a lista de rotas
    private lateinit var routesAdapter: RoutesAdapter

    // Formatador de moeda
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    
    // Gerenciador de sessão do usuário
    private lateinit var userSessionManager: UserSessionManager

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
        
        // Inicializar gerenciador de sessão primeiro
        userSessionManager = UserSessionManager.getInstance(requireContext())
        
        setupRecyclerView()
        setupClickListeners()
        setupNavigationDrawer()
        observeViewModel()

        // Submenu de despesas começa recolhido
        collapseExpenseSubmenu()
    }

    override fun onResume() {
        super.onResume()
        // ✅ CORREÇÃO: Atualizar dados das rotas quando retorna de outras telas
        android.util.Log.d("RoutesFragment", "🔄 onResume - Forçando atualização dos dados das rotas")
        viewModel.refresh()
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
        val menu = binding.navigationView.menu
        val hasMenuAccess = userSessionManager.hasMenuAccess()
        
        // Ocultar menu completo para usuários USER
        if (!hasMenuAccess) {
            binding.navigationView.visibility = View.GONE
            binding.btnMenu.visibility = View.GONE
            android.util.Log.d("RoutesFragment", "🔒 Menu oculto para usuário: ${userSessionManager.getCurrentUserName()}")
            return
        }
        
        android.util.Log.d("RoutesFragment", "🔓 Menu disponível para ADMIN: ${userSessionManager.getCurrentUserName()}")
        
        // Configurar listener do menu lateral
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_manage_collaborator -> {
                    // Verificar permissão para gerenciar colaboradores
                    if (!userSessionManager.canManageCollaborators()) {
                        Toast.makeText(requireContext(), "Acesso negado: Apenas administradores podem gerenciar colaboradores", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        return@setNavigationItemSelectedListener false
                    }
                    
                    // Navegar para a tela de gerenciamento de colaboradores
                    try {
                        findNavController().navigate(R.id.colaboradorManagementFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Log.e("RoutesFragment", "Erro ao navegar para gerenciamento de colaboradores: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir gerenciamento de colaboradores: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }

                R.id.nav_expense_categories -> {
                    try {
                        findNavController().navigate(R.id.expenseCategoriesFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Log.e("RoutesFragment", "Erro ao navegar para categorias de despesa: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir categorias de despesa: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                R.id.nav_expense_types -> {
                    try {
                        findNavController().navigate(R.id.expenseTypesFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Log.e("RoutesFragment", "Erro ao navegar para tipos de despesa: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir tipos de despesa: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                R.id.nav_manage_tables -> {
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
                R.id.nav_expense_management -> {
                    // Expandir/colapsar sem fechar o drawer
                    toggleExpenseSubmenu()
                    // Não fechar o drawer
                    true
                }
                R.id.nav_expense_quick_add -> {
                    try {
                        // Navegar para tela de despesas globais
                        findNavController().navigate(R.id.globalExpensesFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Log.e("RoutesFragment", "Erro ao abrir +Despesa: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir +Despesa: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                R.id.nav_manage_routes -> {
                    // Verificar permissão para gerenciar rotas
                    if (!userSessionManager.canManageRoutes()) {
                        Toast.makeText(requireContext(), "Acesso negado: Apenas administradores podem gerenciar rotas", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        return@setNavigationItemSelectedListener false
                    }
                    
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
                R.id.nav_manage_contracts -> {
                    // Verificar permissão para gerenciar contratos
                    if (!userSessionManager.canManageContracts()) {
                        Toast.makeText(requireContext(), "Acesso negado: Apenas administradores podem gerenciar contratos", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        return@setNavigationItemSelectedListener false
                    }
                    
                    // Navegar para a tela de gerenciamento de contratos
                    try {
                        findNavController().navigate(R.id.contractManagementFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Log.e("RoutesFragment", "Erro ao navegar para gerenciamento de contratos: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir gerenciamento de contratos: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                R.id.nav_inventory -> {
                    try {
                        findNavController().navigate(R.id.inventorySelectionDialog)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Log.e("RoutesFragment", "Erro ao abrir Inventário: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir Inventário: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                R.id.nav_metas -> {
                    try {
                        findNavController().navigate(R.id.metasFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Log.e("RoutesFragment", "Erro ao abrir metas: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir metas: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                R.id.nav_dashboard -> {
                    try {
                        findNavController().navigate(R.id.dashboardFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Log.e("RoutesFragment", "Erro ao navegar para dashboard: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir dashboard: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                }
                R.id.nav_closure_report -> {
                    try {
                        findNavController().navigate(R.id.closureReportFragment)
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Log.e("RoutesFragment", "Erro ao navegar para fechamento: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir fechamento: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.drawerLayout.closeDrawers()
                        false
                    }
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
                R.id.nav_logout -> {
                    // ✅ NOVO: Implementar logout completo
                    android.util.Log.d("RoutesFragment", "=== INICIANDO LOGOUT ===")
                    android.util.Log.d("RoutesFragment", "Usuário atual: ${userSessionManager.getCurrentUserName()}")
                    
                    try {
                        // 1. Fazer logout do Google Sign-In
                        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), 
                            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken("1089459035145-d55o1h307gaedp4v03cuchr6s6nn2lhg.apps.googleusercontent.com")
                                .requestEmail()
                                .build())
                        
                        googleSignInClient.signOut().addOnCompleteListener {
                            android.util.Log.d("RoutesFragment", "✅ Google Sign-Out realizado")
                            
                            // 2. Encerrar sessão local
                            userSessionManager.endSession()
                            android.util.Log.d("RoutesFragment", "✅ Sessão local encerrada")
                            
                            // 3. Fechar drawer
                            binding.drawerLayout.closeDrawers()
                            
                            // 4. Mostrar mensagem de sucesso
                            Toast.makeText(requireContext(), "Logout realizado com sucesso!", Toast.LENGTH_SHORT).show()
                            
                            // 5. Navegar de volta para login
                            findNavController().navigate(R.id.action_routesFragment_to_loginFragment)
                        }
                        
                        true
                    } catch (e: Exception) {
                        android.util.Log.e("RoutesFragment", "Erro no logout: ${e.message}", e)
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
        val ids = listOf(R.id.nav_expense_categories, R.id.nav_expense_types, R.id.nav_expense_quick_add)
        val visible = !menu.findItem(R.id.nav_expense_categories).isVisible
        ids.forEach { id -> menu.findItem(id).isVisible = visible }
        styleExpenseSubmenu()
    }

    private fun collapseExpenseSubmenu() {
        val menu = binding.navigationView.menu
        listOf(R.id.nav_expense_categories, R.id.nav_expense_types, R.id.nav_expense_quick_add).forEach { id ->
            menu.findItem(id).isVisible = false
        }
    }

    private fun styleExpenseSubmenu() {
        val menu = binding.navigationView.menu
        val colorWhite = ContextCompat.getColor(requireContext(), android.R.color.white)
        val indentPx = (24 * resources.displayMetrics.density).toInt()
        listOf(R.id.nav_expense_categories, R.id.nav_expense_types, R.id.nav_expense_quick_add).forEach { id ->
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
        // Botão de menu lateral
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(binding.navigationView)
        }





        // Botão de reforma de mesas
        binding.reformaButton.setOnClickListener {
            // Navegar para a tela de mesas reformadas
            try {
                findNavController().navigate(R.id.mesasReformadasFragment)
            } catch (e: Exception) {
                Log.e("RoutesFragment", "Erro ao navegar para mesas reformadas: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao abrir reforma de mesas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Botão de transferência
        binding.transferButton.setOnClickListener {
            showTransferClientDialog()
        }
    }





    /**
     * Observa as mudanças no ViewModel e atualiza a UI.
     */
    private fun observeViewModel() {
        // ✅ MODERNIZADO: Observa a lista de rotas com StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rotasResumo.collect { rotas ->
                    routesAdapter.submitList(rotas)
                }
            }
        }

        // ✅ MODERNIZADO: Observa as estatísticas gerais com StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estatisticas.collect { stats ->
                    binding.totalMesasCount.text = stats.totalMesas.toString()
                    binding.totalClientesCount.text = stats.totalClientesAtivos.toString()
                    binding.totalPendenciasCount.text = stats.totalPendencias.toString()
                }
            }
        }





        // ✅ MODERNIZADO: Observa mensagens de erro com StateFlow
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
                        findNavController().navigate(R.id.action_routesFragment_to_clientListFragment, bundle)
                        viewModel.navigationToClientsCompleted()
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
            android.util.Log.d("RoutesFragment", "✅ Transferência concluída - Forçando atualização dos dados")
            viewModel.refresh()
        }
        dialog.show(parentFragmentManager, "TransferClientDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 

