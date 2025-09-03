package com.example.gestaobilhares.ui.routes

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentRoutesBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.utils.UserSessionManager
import com.google.android.material.navigation.NavigationView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
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
        
        // Inicializar ViewModel aqui onde o contexto está disponível
        val database = AppDatabase.getDatabase(requireContext())
        viewModel = RoutesViewModel(
            AppRepository(
                database.clienteDao(),
                database.acertoDao(),
                database.mesaDao(),
                database.rotaDao(),
                database.despesaDao(),
                database.colaboradorDao(),
                database.cicloAcertoDao()
            ),
            userSessionManager
        )
        
        setupRecyclerView()
        setupClickListeners()
        setupNavigationDrawer()
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
                    
                    Toast.makeText(requireContext(), "Gerenciar Mesas será implementado em breve", Toast.LENGTH_SHORT).show()
                    binding.drawerLayout.closeDrawers()
                    true
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
                R.id.nav_management_report -> {
                    try {
                        android.util.Log.d("RoutesFragment", "Navegando para relatórios via drawer")
                        findNavController().navigate(R.id.reportsFragment)
                        android.util.Log.d("RoutesFragment", "Navegação para relatórios via drawer concluída")
                        binding.drawerLayout.closeDrawers()
                        true
                    } catch (e: Exception) {
                        Log.e("RoutesFragment", "Erro ao navegar para relatórios via drawer: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao abrir relatórios: ${e.message}", Toast.LENGTH_SHORT).show()
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
    }

    /**
     * Configura os listeners de clique dos botões.
     */
    private fun setupClickListeners() {
        // Botão de menu lateral
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(binding.navigationView)
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


    }



    /**
     * Limpa o binding quando o fragment é destruído.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
