package com.example.gestaobilhares.ui.routes

import android.app.AlertDialog
import android.os.Bundle
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
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment que exibe a tela de rotas.
 * Mostra lista de rotas, estatísticas e permite adicionar novas rotas.
 * Implementa o padrão MVVM usando ViewBinding e ViewModel.
 * 
 * FASE 3: Inclui controle de acesso admin, card de valor acertado e relatório de fechamento.
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
        viewModel = RoutesViewModel(RotaRepository(AppDatabase.getDatabase(requireContext()).rotaDao()))
        
        setupRecyclerView()
        setupClickListeners()
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
     * FASE 3: Configura os listeners de clique dos botões incluindo controle de acesso.
     */
    private fun setupClickListeners() {
        // FASE 3: Botão de adicionar rota (FAB) - com controle de acesso
        binding.addRouteFab.setOnClickListener {
            viewModel.showAddRouteDialog()
        }

        // FASE 3: Botão de relatório de fechamento
        binding.reportFab.setOnClickListener {
            showReportConfirmationDialog()
        }

        // Botão de gerenciar rotas
        binding.manageButton.setOnClickListener {
            findNavController().navigate(R.id.action_routesFragment_to_routeManagementFragment)
        }

        // Botão Histórico de Despesas
        binding.expenseHistoryFab.setOnClickListener {
            findNavController().navigate(R.id.action_routesFragment_to_expenseHistoryFragment)
        }

        // Botão de pesquisa
        binding.searchButton.setOnClickListener {
            // TODO: Implementar busca na próxima fase
            Toast.makeText(requireContext(), "Busca será implementada em breve", Toast.LENGTH_SHORT).show()
        }

        // Botão de filtro
        binding.filterButton.setOnClickListener {
            // TODO: Implementar filtros na próxima fase
            Toast.makeText(requireContext(), "Filtros serão implementados em breve", Toast.LENGTH_SHORT).show()
        }

        // Botão de diagnóstico da impressora
        binding.printerDiagnosticButton.setOnClickListener {
            findNavController().navigate(R.id.action_routesFragment_to_printerDiagnosticFragment)
        }

        // ✅ FASE 9C: Botão de histórico de ciclos
        binding.cycleHistoryButton.setOnClickListener {
            // Por enquanto, navegar para histórico da primeira rota (se houver)
            val rotas = viewModel.rotasResumo.value
            if (rotas != null && rotas.isNotEmpty()) {
                val action = RoutesFragmentDirections
                    .actionRoutesFragmentToCycleHistoryFragment(rotas.first().rota.id)
                findNavController().navigate(action)
            } else {
                Toast.makeText(requireContext(), "Nenhuma rota disponível para histórico", Toast.LENGTH_SHORT).show()
            }
        }

        // Link "Ver todas"
        binding.verTodasButton.setOnClickListener {
            // TODO: Navegar para tela de todas as rotas
            Toast.makeText(requireContext(), "Lista completa será implementada em breve", Toast.LENGTH_SHORT).show()
        }

        // Navegação do card em destaque
        binding.rotaPreviousButton.setOnClickListener {
            // TODO: Implementar navegação entre rotas em destaque
        }

        binding.rotaNextButton.setOnClickListener {
            // TODO: Implementar navegação entre rotas em destaque
        }

        binding.rotaDestaqueCard.setOnClickListener {
            // TODO: Navegar para a rota em destaque
        }
    }

    /**
     * FASE 3: Mostra diálogo de confirmação para geração de relatório.
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
                    // TODO: Navegar para acertos
                    Toast.makeText(requireContext(), "Acertos será implementado em breve", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.navigation_cadastros -> {
                    // TODO: Navegar para cadastros
                    Toast.makeText(requireContext(), "Cadastros será implementado em breve", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.navigation_relatorios -> {
                    // TODO: Navegar para relatórios
                    Toast.makeText(requireContext(), "Relatórios será implementado em breve", Toast.LENGTH_SHORT).show()
                    false
                }
                else -> false
            }
        }
    }

    /**
     * FASE 3: Observa as mudanças no ViewModel e atualiza a UI com novas funcionalidades.
     */
    private fun observeViewModel() {
        // Observa a lista de rotas
        viewModel.rotasResumo.observe(viewLifecycleOwner) { rotas ->
            routesAdapter.submitList(rotas)
            
            // Atualiza o card em destaque com a primeira rota (se houver)
            if (rotas.isNotEmpty()) {
                val rotaDestaque = rotas.first()
                binding.rotaDestaqueTitulo.text = rotaDestaque.rota.nome
                binding.rotaDestaqueInfo.text = "${rotaDestaque.clientesAtivos} clientes • ${rotaDestaque.pendencias} pendências"
            }
        }

        // FASE 3: Observa as estatísticas gerais incluindo valor acertado
        viewModel.estatisticas.observe(viewLifecycleOwner) { stats ->
            binding.clientesAtivosCount.text = stats.totalClientesAtivos.toString()
            binding.pendenciasCount.text = stats.totalPendencias.toString()
            // FASE 3: Atualiza o card de valor acertado
            binding.valorAcertadoAmount.text = currencyFormatter.format(stats.valorAcertadoNaoFinalizado)
        }

        // FASE 3: Observa controle de acesso admin
        viewModel.isAdmin.observe(viewLifecycleOwner) { isAdmin ->
            // FASE 3: Controla visibilidade do FAB baseado no nível de acesso
            binding.addRouteFab.visibility = if (isAdmin) View.VISIBLE else View.GONE
        }

        // FASE 3: Observa estado de loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Mostra/esconde indicador de loading nos botões
            binding.reportFab.isEnabled = !isLoading
            binding.addRouteFab.isEnabled = !isLoading
        }

        // FASE 3: Observa eventos de geração de relatório
        viewModel.generateReport.observe(viewLifecycleOwner) { shouldGenerate ->
            if (shouldGenerate) {
                // TODO: Aqui poderia abrir a tela de relatório ou baixar o arquivo
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
                // FASE 3: Navegar para a tela de clientes da rota com argumento
                // Usando Bundle temporariamente até que Safe Args gere as classes
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
