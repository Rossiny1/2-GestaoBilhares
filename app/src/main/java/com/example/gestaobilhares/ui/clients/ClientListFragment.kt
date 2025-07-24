package com.example.gestaobilhares.ui.clients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.StatusRota
import com.example.gestaobilhares.databinding.FragmentClientListBinding
import kotlinx.coroutines.launch
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.data.repository.RotaRepository
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.DespesaRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.AppRepository
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import java.text.NumberFormat
import android.view.animation.AnimationUtils
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import android.util.Log

/**
 * Fragment modernizado para lista de clientes com controle de status da rota
 */
class ClientListFragment : Fragment() {

    private var _binding: FragmentClientListBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding não está disponível")

    private lateinit var viewModel: ClientListViewModel
    private val args: ClientListFragmentArgs by navArgs()
    private lateinit var clientAdapter: ClientAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("DEBUG_DIAG", "onViewCreated chamado - TESTE DE LOG")
        
        // ✅ FASE 8C: Inicializar ViewModel com todos os repositórios necessários
        val database = AppDatabase.getDatabase(requireContext())
        viewModel = ClientListViewModel(
            ClienteRepository(database.clienteDao()),
            RotaRepository(database.rotaDao()),
            CicloAcertoRepository(
                database.cicloAcertoDao(),
                DespesaRepository(database.despesaDao()),
                AcertoRepository(database.acertoDao(), database.clienteDao()),
                ClienteRepository(database.clienteDao()) // NOVO
            ),
            AcertoRepository(database.acertoDao(), database.clienteDao()),
            AppRepository(
                database.clienteDao(),
                database.acertoDao(),
                database.mesaDao(),
                database.rotaDao(),
                database.despesaDao(),
                null // ColaboradorDao não implementado ainda
            )
        )
        
        try {
            // Verificar se binding está disponível
            if (_binding == null) {
                android.util.Log.e("ClientListFragment", "Binding é null em onViewCreated")
                return
            }
            
                    configurarRecyclerView()
        configurarBotoes()
        configurarBusca() // ✅ FASE 8C: Configurar busca
        observarViewModel()
            
            // Carregar dados da rota
            val rotaId = args.rotaId
            viewModel.carregarRota(rotaId)
            viewModel.carregarClientes(rotaId)
        } catch (e: Exception) {
            android.util.Log.e("ClientListFragment", "Erro na inicialização: ${e.message}")
            // Mostrar erro para o usuário
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Erro")
                .setMessage("Erro ao carregar dados da rota. Tente novamente.")
                .setPositiveButton("OK") { _, _ ->
                    findNavController().popBackStack()
                }
                .show()
        }
    }

    private fun configurarRecyclerView() {
        clientAdapter = ClientAdapter { cliente ->
            // Verificar se a rota está em andamento antes de permitir navegação
            if (viewModel.podeAcessarCliente()) {
            val action = ClientListFragmentDirections
                .actionClientListFragmentToClientDetailFragment(cliente.id)
            findNavController().navigate(action)
            } else {
                mostrarAlertaRotaNaoIniciada()
            }
        }
        
        _binding?.rvClients?.apply {
            adapter = clientAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun configurarBotoes() {
        _binding?.let { binding ->
            // Botão voltar
            binding.btnBack.setOnClickListener {
                findNavController().popBackStack()
            }
            
            // ✅ FASE 8C: Botão buscar - ativar/desativar busca
            binding.btnSearch.setOnClickListener {
                toggleBusca()
            }
            
            // Botão filtrar
            binding.btnFilter.setOnClickListener {
                // TODO: Implementar filtro
            }
            
            // ✅ NOVO: Botão de relatórios de ciclos
            binding.btnReports.setOnClickListener {
                try {
                    val action = ClientListFragmentDirections
                        .actionClientListFragmentToCycleHistoryFragment(args.rotaId)
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    android.util.Log.e("ClientListFragment", "Erro ao navegar para relatórios: ${e.message}")
                    mostrarFeedback("Erro ao abrir relatórios: ${e.message}", Snackbar.LENGTH_LONG)
                }
            }
            
            // ✅ FASE 9A: Controle de status da rota com feedback
            binding.btnStartRoute.setOnClickListener {
                viewModel.iniciarRota()
                mostrarFeedback("Rota iniciada com sucesso!", Snackbar.LENGTH_SHORT)
            }
            
            binding.btnFinishRoute.setOnClickListener {
                viewModel.finalizarRota()
                mostrarFeedback("Rota finalizada com sucesso!", Snackbar.LENGTH_SHORT)
            }
            
            // Ações rápidas (substituindo FABs)
            binding.btnAddClient.setOnClickListener {
                val action = ClientListFragmentDirections
                    .actionClientListFragmentToClientRegisterFragment(args.rotaId)
                findNavController().navigate(action)
            }
            
            binding.btnAddExpense.setOnClickListener {
                val action = ClientListFragmentDirections
                    .actionClientListFragmentToExpenseRegisterFragment(args.rotaId)
                findNavController().navigate(action)
            }
            
            // Filtros rápidos
            binding.btnFilterActive.setOnClickListener {
                viewModel.aplicarFiltro(FiltroCliente.ATIVOS)
                atualizarEstadoFiltros(binding.btnFilterActive)
            }
            
            binding.btnFilterDebtors.setOnClickListener {
                viewModel.aplicarFiltro(FiltroCliente.DEVEDORES)
                atualizarEstadoFiltros(binding.btnFilterDebtors)
            }
            
            binding.btnFilterAll.setOnClickListener {
                viewModel.aplicarFiltro(FiltroCliente.TODOS)
                atualizarEstadoFiltros(binding.btnFilterAll)
            }

            val fabLogs: FloatingActionButton? = view?.findViewById(R.id.fab_logs)
            fabLogs?.setOnClickListener {
                findNavController().navigate(R.id.logViewerFragment)
            }
        }
    }

    private fun observarViewModel() {
        // Observar dados da rota
        lifecycleScope.launch {
            viewModel.rotaInfo.collect { rota ->
                try {
                    rota?.let { atualizarInfoRota(it) }
                } catch (e: Exception) {
                    android.util.Log.e("ClientListFragment", "Erro ao atualizar info da rota: ${e.message}")
                }
            }
        }
        
        // Observar status da rota
        lifecycleScope.launch {
            viewModel.statusRota.collect { status ->
                try {
                    atualizarStatusRota(status)
                } catch (e: Exception) {
                    android.util.Log.e("ClientListFragment", "Erro ao atualizar status da rota: ${e.message}")
                }
            }
        }
        
        // ✅ FASE 8C: Observar ciclo ativo real
        lifecycleScope.launch {
            viewModel.cicloAtivo.collect { cicloEntity ->
                try {
                    cicloEntity?.let { ciclo ->
                        _binding?.tvCycleTitle?.text = "${ciclo.numeroCiclo}º Acerto"
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ClientListFragment", "Erro ao atualizar ciclo de acerto: ${e.message}")
                }
            }
        }
        
        // Remover observação dos StateFlows antigos do card de progresso
        // lifecycleScope.launch { viewModel.progressoCiclo.collect { ... } }
        // lifecycleScope.launch { viewModel.percentualAcertados.collect { ... } }
        // lifecycleScope.launch { viewModel.totalClientes.collect { ... } }
        // lifecycleScope.launch { viewModel.faturamento.collect { ... } }
        // lifecycleScope.launch { viewModel.pendencias.collect { ... } }
        // lifecycleScope.launch { viewModel.despesas.collect { ... } }
        // O card de progresso deve ser atualizado apenas via cicloProgressoCard
        lifecycleScope.launch {
            viewModel.cicloProgressoCard.collect { card ->
                try {
                    card?.let {
                        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
                        _binding?.tvPercentualAcertados?.text = "${it.percentual}%"
                        _binding?.tvTotalClientes?.text = "de ${it.totalClientes} clientes"
                        _binding?.tvFaturamento?.text = formatter.format(it.receita)
                        _binding?.tvDespesas?.text = formatter.format(it.despesas)
                        _binding?.tvPendencias?.text = it.pendencias.toString()
                        _binding?.tvDebitoTotal?.text = formatter.format(it.debitoTotal)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ClientListFragment", "Erro ao atualizar card de progresso: ${e.message}")
                }
            }
        }
        
        // ✅ FASE 9A: Observar clientes com empty state melhorado
        lifecycleScope.launch {
            viewModel.clientes.collect { clientes ->
                try {
                    clientAdapter.submitList(clientes)
                    // Card de progresso é atualizado automaticamente via fluxo reativo
                    // Mostrar/esconder empty state com animação
        _binding?.let { binding ->
                        if (clientes.isEmpty()) {
                            atualizarEmptyState(true)
                        } else {
                            atualizarEmptyState(false)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ClientListFragment", "Erro ao atualizar lista de clientes: ${e.message}")
                }
            }
        }
        
        // ✅ FASE 9A: Observar mensagens de erro e feedback
        lifecycleScope.launch {
            viewModel.errorMessage.collect { mensagem ->
                mensagem?.let {
                    mostrarFeedback("Erro: $it", Snackbar.LENGTH_LONG)
                    viewModel.limparErro()
                }
            }
        }
        
        // NOVO: Observa evento de acerto salvo para atualizar card de progresso
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("acerto_salvo")?.observe(
            viewLifecycleOwner
        ) { acertoSalvo ->
            if (acertoSalvo == true) {
                Log.d("ClientListFragment", "[DEBUG] Evento acerto_salvo recebido, recarregando clientes para rotaId=${args.rotaId}")
                viewModel.carregarClientes(args.rotaId)
                findNavController().currentBackStackEntry?.savedStateHandle?.set("acerto_salvo", false)
            }
        }

    }
    
    private fun atualizarInfoRota(rota: com.example.gestaobilhares.data.entities.Rota) {
        _binding?.let { binding ->
            binding.tvTitle.text = rota.nome
            // TODO: Carregar informações dinâmicas (clientes ativos, mesas)
            binding.tvRouteInfo.text = "12 clientes ativos • 24 mesas"
        }
    }
    
    private fun atualizarStatusRota(status: StatusRota) {
        _binding?.let { binding ->
            val context = requireContext()
            
            when (status) {
                StatusRota.EM_ANDAMENTO -> {
                    binding.tvRouteStatus.text = "Em Andamento"
                    binding.btnStartRoute.isEnabled = false
                    binding.btnFinishRoute.isEnabled = true
                }
                StatusRota.FINALIZADA -> {
                    binding.tvRouteStatus.text = "Finalizada"
                    binding.btnStartRoute.isEnabled = true
                    binding.btnFinishRoute.isEnabled = false
                }
                StatusRota.PAUSADA -> {
                    binding.tvRouteStatus.text = "Não Iniciada"
                    binding.btnStartRoute.isEnabled = true
                    binding.btnFinishRoute.isEnabled = false
                }
                else -> {
                    binding.tvRouteStatus.text = "Não Iniciada"
                    binding.btnStartRoute.isEnabled = true
                    binding.btnFinishRoute.isEnabled = false
                }
            }
        }
    }
    
    private fun atualizarEstadoFiltros(botaoSelecionado: com.google.android.material.button.MaterialButton) {
        _binding?.let { binding ->
            val context = requireContext()
            val corSelecionada = context.getColor(R.color.purple_600)
            val corNormal = context.getColor(android.R.color.transparent)
            val textColorSelected = context.getColor(R.color.white)
            val textColorNormal = context.getColor(R.color.purple_600)
            
            // Resetar todos os botões
            listOf(binding.btnFilterActive, binding.btnFilterDebtors, binding.btnFilterAll).forEach { btn ->
                btn.setBackgroundColor(corNormal)
                btn.setTextColor(textColorNormal)
            }
            
            // Destacar o selecionado
            botaoSelecionado.setBackgroundColor(corSelecionada)
            botaoSelecionado.setTextColor(textColorSelected)
        }
    }
    

    

    

    
    // ✅ FASE 9A: Toggle da busca com animação
    private fun toggleBusca() {
        // Busca removida do layout, apenas log para debug
        android.util.Log.d("ClientListFragment", "Busca não disponível - layout simplificado")
    }
    
    // ✅ FASE 9A: Configurar busca em tempo real com debounce
    private fun configurarBusca() {
        // Busca removida do layout, apenas log para debug
        android.util.Log.d("ClientListFragment", "Busca não disponível - layout simplificado")
    }
    
    // ✅ FASE 9B: Atualizar empty state com filtros combinados
    private fun atualizarEmptyState(mostrar: Boolean) {
        _binding?.let { binding ->
            if (mostrar) {
                // Determinar contexto do empty state
                val filtroAtual = viewModel.getFiltroAtual()
                
                when {
                    filtroAtual == FiltroCliente.DEVEDORES -> {
                        binding.ivEmptyStateIcon.setImageResource(R.drawable.ic_money)
                        binding.tvEmptyStateTitle.text = "Nenhum devedor encontrado"
                        binding.tvEmptyStateMessage.text = "Todos os clientes estão em dia!"
                        binding.btnEmptyStateAction.visibility = View.GONE
                    }
                    filtroAtual == FiltroCliente.ATIVOS -> {
                        binding.ivEmptyStateIcon.setImageResource(R.drawable.ic_user)
                        binding.tvEmptyStateTitle.text = "Nenhum cliente ativo"
                        binding.tvEmptyStateMessage.text = "Todos os clientes estão inativos."
                        binding.btnEmptyStateAction.visibility = View.GONE
                    }
                    else -> {
                        binding.ivEmptyStateIcon.setImageResource(R.drawable.ic_user)
                        binding.tvEmptyStateTitle.text = "Nenhum cliente cadastrado"
                        binding.tvEmptyStateMessage.text = "Comece adicionando o primeiro cliente desta rota."
                        binding.btnEmptyStateAction.visibility = View.VISIBLE
                        binding.btnEmptyStateAction.setOnClickListener {
                            val action = ClientListFragmentDirections
                                .actionClientListFragmentToClientRegisterFragment(args.rotaId)
                            findNavController().navigate(action)
                        }
                    }
                }
                
                // Animação de entrada
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.rvClients.visibility = View.GONE
                
                val fadeIn = AlphaAnimation(0.0f, 1.0f)
                fadeIn.duration = 300
                binding.emptyStateLayout.startAnimation(fadeIn)
                
            } else {
                // Animação de saída
                val fadeOut = AlphaAnimation(1.0f, 0.0f)
                fadeOut.duration = 200
                fadeOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        binding.emptyStateLayout.visibility = View.GONE
                        binding.rvClients.visibility = View.VISIBLE
                    }
                    override fun onAnimationRepeat(animation: Animation?) {}
                })
                binding.emptyStateLayout.startAnimation(fadeOut)
            }
        }
    }
    
    // ✅ FASE 9A: Mostrar feedback visual para o usuário
    private fun mostrarFeedback(mensagem: String, duracao: Int) {
        _binding?.let { binding ->
            Snackbar.make(binding.root, mensagem, duracao)
                .setBackgroundTint(requireContext().getColor(R.color.purple_600))
                .setTextColor(requireContext().getColor(R.color.white))
                .show()
        }
    }
    
    private fun mostrarAlertaRotaNaoIniciada() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Rota Não Iniciada")
            .setMessage("Para acessar os detalhes do cliente e fazer acertos, é necessário iniciar a rota primeiro.")
            .setPositiveButton("Entendi", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
