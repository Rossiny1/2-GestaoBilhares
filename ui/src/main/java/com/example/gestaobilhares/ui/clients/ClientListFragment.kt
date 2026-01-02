package com.example.gestaobilhares.ui.clients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.ui.R
import com.example.gestaobilhares.data.entities.StatusRota
import com.example.gestaobilhares.ui.databinding.FragmentClientListBinding
import kotlinx.coroutines.launch
import com.example.gestaobilhares.data.database.AppDatabase
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
import timber.log.Timber
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fragment modernizado para lista de clientes com controle de status da rota
 */
@AndroidEntryPoint
class ClientListFragment : Fragment() {

    private var _binding: FragmentClientListBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding não está disponível")

    private val viewModel: ClientListViewModel by viewModels()
    private val args: ClientListFragmentArgs by navArgs()
    private lateinit var clientAdapter: ClientAdapter
    
    @Inject
    lateinit var appRepository: AppRepository

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
        
        // ✅ LOG CRASH: Início da tela
        Timber.d("LOG_CRASH", "ClientListFragment.onViewCreated - INÍCIO")
        Timber.d("DEBUG_DIAG", "onViewCreated chamado - TESTE DE LOG")
        
        // ✅ REMOVIDO: Callback de botão voltar - Navigation Component gerencia automaticamente
        // O botão voltar agora é gerenciado globalmente pelo MainActivity
        
        // ✅ FASE 8C: Inicialização automatica pelo Hilt
        
        try {
            // Verificar se binding está disponível
            if (_binding == null) {
                Timber.e("ClientListFragment", "Binding é null em onViewCreated")
                return
            }
            
                    configurarRecyclerView()
        configurarBotoes()
        configurarBusca() // ✅ FASE 8C: Configurar busca
        observarViewModel()
        observarDadosRotaReais() // ✅ NOVO: Observar dados reais da rota
            
            // Carregar dados da rota
            val rotaId = args.rotaId
            viewModel.carregarRota(rotaId)
            // ✅ CORREÇÃO CRÍTICA: Usar método otimizado que calcula débito em tempo real
            // Isso garante que após importação, o débito seja recalculado corretamente
            viewModel.carregarClientesOtimizado(rotaId)
        } catch (e: Exception) {
            Timber.e("ClientListFragment", "Erro na inicialização: ${e.message}")
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

    override fun onResume() {
        super.onResume()
        // ✅ CORREÇÃO: Usar a MESMA lógica que funciona no onViewCreated
        Timber.d("ClientListFragment", "🔄 onResume - Recarregando dados completos (mesma lógica do onViewCreated)")
        
        // Recarregar dados da rota e clientes
        val rotaId = args.rotaId
        
        // ✅ CORREÇÃO: Usar a MESMA lógica que funciona quando vem do RoutesFragment
        viewModel.carregarRota(rotaId)
        // ✅ CORREÇÃO CRÍTICA: Usar método otimizado que calcula débito em tempo real
        // Isso garante que após importação, o débito seja recalculado corretamente
        viewModel.forcarRecarregamentoClientesOtimizado(rotaId)
        
        Timber.d("ClientListFragment", "✅ onResume - Dados forçados recarregados (com débito calculado) para rotaId=$rotaId")
    }

    override fun onStart() {
        super.onStart()
        // ✅ NOVO: Garantir que os dados sejam carregados quando o fragment fica visível
        Timber.d("ClientListFragment", "🔄 onStart - Garantindo carregamento de dados")
        
        val rotaId = args.rotaId
        // ✅ CORREÇÃO CRÍTICA: Usar método otimizado que calcula débito em tempo real
        // Isso garante que após importação, o débito seja recalculado corretamente
        viewModel.forcarRecarregamentoClientesOtimizado(rotaId)
        Timber.d("ClientListFragment", "✅ onStart - Dados forçados recarregados (com débito calculado) para rotaId=$rotaId")
    }

    private fun configurarRecyclerView() {
        // ✅ LOG CRASH: Início da configuração do RecyclerView
        Timber.d("LOG_CRASH", "ClientListFragment.configurarRecyclerView - INÍCIO")
        
        try {
            // ✅ NOVO: Usar appRepository já inicializado para verificar se cliente nunca foi acertado
            clientAdapter = ClientAdapter(
                onClientClick = { cliente ->
                    // ✅ LOG CRASH: Clique em cliente
                    Timber.d("LOG_CRASH", "ClientListFragment.configurarRecyclerView - Clique em cliente: ${cliente.id}")
                    
                    // ✅ NOVO: Sempre permitir navegação para detalhes do cliente, independente do status da rota
                    // O bloqueio deve acontecer apenas no botão "Novo Acerto" dentro dos detalhes
                    try {
                        val action = ClientListFragmentDirections
                            .actionClientListFragmentToClientDetailFragment(
                                clienteId = cliente.id,
                                mostrarDialogoObservacoes = false // Não usar mais este parâmetro
                            )
                        Timber.d("LOG_CRASH", "ClientListFragment.configurarRecyclerView - Navegando para detalhes do cliente")
                        findNavController().navigate(action)
                        
                        // Definir o flag no SavedStateHandle do destino
                        findNavController().currentBackStackEntry?.savedStateHandle?.set("show_observations_dialog", true)
                        Timber.d("LOG_CRASH", "ClientListFragment.configurarRecyclerView - Navegação bem-sucedida")
                    } catch (e: Exception) {
                        Timber.e("LOG_CRASH", "ClientListFragment.configurarRecyclerView - ERRO ao navegar para detalhes: ${e.message}", e)
                    }
                },
                verificarNuncaAcertado = { clienteId ->
                    // ✅ NOVO: Verificar se o cliente nunca foi acertado
                    kotlinx.coroutines.runBlocking {
                        val ultimoAcerto = appRepository.buscarUltimoAcertoPorCliente(clienteId)
                        ultimoAcerto == null
                    }
                }
            )
            
            _binding?.rvClients?.apply {
                adapter = clientAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }
        } catch (e: Exception) {
            Timber.e("LOG_CRASH", "ClientListFragment.configurarRecyclerView - ERRO: ${e.message}", e)
        }
    }
    
    private fun configurarBotoes() {
        // ✅ LOG CRASH: Início da configuração dos botões
        Timber.d("LOG_CRASH", "ClientListFragment.configurarBotoes - INÍCIO")
        
        _binding?.let { binding ->
            // ✅ CORREÇÃO: Botão voltar navega para tela de rotas
            Timber.d("LOG_CRASH", "ClientListFragment.configurarBotoes - Configurando botão voltar")
            binding.btnBack.setOnClickListener {
                Timber.d("LOG_CRASH", "ClientListFragment.configurarBotoes - Clique no botão voltar")
                navegarParaRotas()
            }
            
            // ✅ NOVO: Botão buscar - abrir diálogo de pesquisa avançada
            binding.btnSearch.setOnClickListener {
                mostrarDialogoPesquisaAvancada()
            }
            
            // Botão filtrar
            binding.btnFilter.setOnClickListener {
                mostrarDialogoFiltros()
            }
            
            // ✅ NOVO: Botão de relatórios de ciclos
            binding.btnReports.setOnClickListener {
                try {
                    val action = ClientListFragmentDirections
                        .actionClientListFragmentToCycleHistoryFragment(args.rotaId)
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    Timber.e("ClientListFragment", "Erro ao navegar para relatórios: ${e.message}")
                    mostrarFeedback("Erro ao abrir relatórios: ${e.message}", Snackbar.LENGTH_LONG)
                }
            }
            
            // ✅ FASE 9A: Controle de status da rota com feedback
            binding.btnStartRoute.setOnClickListener {
                viewModel.iniciarRota()
                mostrarFeedback("Rota iniciada com sucesso!", Snackbar.LENGTH_SHORT)
            }
            
            binding.btnFinishRoute.setOnClickListener {
                mostrarDialogoConfirmacaoFinalizar()
            }
            
            // Configurar FAB expandível
            configurarFabExpandivel()
            
            // Filtros rápidos
            binding.btnFilterAcertados.setOnClickListener {
                viewModel.aplicarFiltro(FiltroCliente.ACERTADOS)
                atualizarEstadoFiltros(binding.btnFilterAcertados)
            }
            
            binding.btnFilterNaoAcertados.setOnClickListener {
                viewModel.aplicarFiltro(FiltroCliente.NAO_ACERTADOS)
                atualizarEstadoFiltros(binding.btnFilterNaoAcertados)
            }
            
            binding.btnFilterPendencias.setOnClickListener {
                viewModel.aplicarFiltro(FiltroCliente.PENDENCIAS)
                atualizarEstadoFiltros(binding.btnFilterPendencias)
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
                    Timber.e("ClientListFragment", "Erro ao atualizar info da rota: ${e.message}")
                }
            }
        }
        
        // ✅ NOVO: Observar mudanças no filtro atual para sincronizar estado visual
        lifecycleScope.launch {
            viewModel.filtroAtual.collect { filtroAtual ->
                try {
                    sincronizarEstadoVisualFiltros(filtroAtual)
                } catch (e: Exception) {
                    Timber.e("ClientListFragment", "Erro ao sincronizar filtros: ${e.message}")
                }
            }
        }
        
        // Observar status da rota
        lifecycleScope.launch {
            viewModel.statusRota.collect { status ->
                try {
                    atualizarStatusRota(status)
                } catch (e: Exception) {
                    Timber.e("ClientListFragment", "Erro ao atualizar status da rota: ${e.message}")
                }
            }
        }
        
        // ✅ FASE 8C: Observar ciclo ativo real
        lifecycleScope.launch {
            viewModel.cicloAtivo.collect { cicloEntity ->
                try {
                    cicloEntity?.let { ciclo ->
                        val tituloCiclo = "${ciclo.numeroCiclo}º Acerto"
                        _binding?.tvCycleTitle?.text = tituloCiclo
                        
                        // ✅ NOVO: Log para debug do ciclo
                        Timber.d("ClientListFragment", "🔄 Atualizando card do ciclo: $tituloCiclo (ID: ${ciclo.id}, Status: ${ciclo.status})")
                        
                        // ✅ NOVO: Tornar o título clicável
                        _binding?.tvCycleTitle?.setOnClickListener {
                            mostrarDialogoProgressoCiclo()
                        }
                    } ?: run {
                        // ✅ CORREÇÃO: Quando não há ciclo ativo, exibir o ÚLTIMO ciclo finalizado (espelhando AppRepository)
                        lifecycleScope.launch {
                            val ultimoCiclo = viewModel.buscarUltimoCicloFinalizado()
                            val tituloCiclo = if (ultimoCiclo != null) "${ultimoCiclo.numeroCiclo}º Acerto" else "1º Acerto"
                            _binding?.tvCycleTitle?.text = tituloCiclo
                            Timber.d("ClientListFragment", "🔄 Exibindo ciclo finalizado (fallback): $tituloCiclo")
                        }
                        
                        // ✅ NOVO: Tornar o título clicável
                        _binding?.tvCycleTitle?.setOnClickListener {
                            mostrarDialogoProgressoCiclo()
                        }
                    }
                } catch (e: Exception) {
                    Timber.e("ClientListFragment", "Erro ao atualizar ciclo de acerto: ${e.message}")
                }
            }
        }
        
        // Remover observação dos StateFlows antigos do card de progresso
        // lifecycleScope.launch { viewModel.progressoCiclo.collect { ... } }
        // lifecycleScope.launch { viewModel.percentualAcertados.collect { ... } }
        // lifecycleScope.launch { viewModel.totalClientes.collect { ... } }


        // ✅ FASE 9A: Observar clientes com empty state melhorado
        lifecycleScope.launch {
            viewModel.clientes.collect { clientes ->
                try {
                    clientAdapter.submitList(clientes)
                    // Card de progresso é atualizado automaticamente via fluxo reativo
                    // Mostrar/esconder empty state com animação
        _binding?.let { _ ->
                        if (clientes.isEmpty()) {
                            atualizarEmptyState(true)
                        } else {
                            atualizarEmptyState(false)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e("ClientListFragment", "Erro ao atualizar lista de clientes: ${e.message}")
                }
            }
        }
        
        // ✅ FASE 9A: Observar mensagens de erro e feedback
        lifecycleScope.launch {
            viewModel.error.collect { mensagem ->
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
                Timber.d("ClientListFragment", "[DEBUG] Evento acerto_salvo recebido, recarregando clientes para rotaId=${args.rotaId}")
                // ✅ CORREÇÃO CRÍTICA: Usar método otimizado que calcula débito em tempo real
                // Isso garante que após salvar acerto, o débito seja recalculado corretamente
                viewModel.carregarClientesOtimizado(args.rotaId)
                findNavController().currentBackStackEntry?.savedStateHandle?.set("acerto_salvo", false)
            }
        }

    }
    
    private fun atualizarInfoRota(rota: com.example.gestaobilhares.data.entities.Rota) {
        _binding?.let { binding ->
            Timber.d("ClientListFragment", "=== ATUALIZANDO INFO DA ROTA ===")
            Timber.d("ClientListFragment", "Rota: ${rota.nome} (ID: ${rota.id})")

            binding.tvTitle.text = rota.nome

            // ✅ NOVO: Carregar dados reais em tempo real
            viewModel.carregarDadosRotaEmTempoReal(rota.id)
        }
    }
    
    // ✅ NOVO: Observar dados reais da rota
    private fun observarDadosRotaReais() {
        lifecycleScope.launch {
            viewModel.dadosRotaReais.collect { dados ->
                Timber.d("ClientListFragment", "=== RECEBENDO DADOS ROTA NO FRAGMENT ===")
                Timber.d("ClientListFragment", "Dados recebidos: ${dados.totalClientes} clientes, ${dados.totalMesas} mesas")

                try {
                    _binding?.let { binding ->
                        val textoAnterior = binding.tvRouteInfo.text.toString()
                        binding.tvRouteInfo.text = "${dados.totalClientes} clientes ativos • ${dados.totalMesas} mesas"
                        Timber.d("ClientListFragment", "UI atualizada: '$textoAnterior' -> '${binding.tvRouteInfo.text}'")
                    }
                } catch (e: Exception) {
                    Timber.e("ClientListFragment", "Erro ao atualizar dados da rota: ${e.message}")
                }
            }
        }
    }
    
    private fun atualizarStatusRota(status: StatusRota) {
        _binding?.let { binding ->
            when (status) {
                StatusRota.EM_ANDAMENTO -> {
                    binding.tvRouteStatus.text = "Em Andamento"
                    binding.btnStartRoute.isEnabled = false
                    binding.btnFinishRoute.isEnabled = true
                    atualizarEstadoBotoesRota(binding.btnFinishRoute, binding.btnStartRoute)
                }
                StatusRota.FINALIZADA -> {
                    binding.tvRouteStatus.text = "Finalizada"
                    binding.btnStartRoute.isEnabled = true
                    binding.btnFinishRoute.isEnabled = false
                    atualizarEstadoBotoesRota(binding.btnStartRoute, binding.btnFinishRoute)
                }
                StatusRota.PAUSADA -> {
                    binding.tvRouteStatus.text = "Não Iniciada"
                    binding.btnStartRoute.isEnabled = true
                    binding.btnFinishRoute.isEnabled = false
                    atualizarEstadoBotoesRota(binding.btnStartRoute, binding.btnFinishRoute)
                }
                else -> {
                    binding.tvRouteStatus.text = "Não Iniciada"
                    binding.btnStartRoute.isEnabled = true
                    binding.btnFinishRoute.isEnabled = false
                    atualizarEstadoBotoesRota(binding.btnStartRoute, binding.btnFinishRoute)
                }
            }
        }
    }
    
    private fun atualizarEstadoBotoesRota(botaoAtivo: com.google.android.material.button.MaterialButton?, _botaoInativo: com.google.android.material.button.MaterialButton?) {
        _binding?.let { binding ->
            val context = requireContext()
            val corSelecionada = context.getColor(com.example.gestaobilhares.ui.R.color.primary_blue)
            val corNormal = context.getColor(android.R.color.transparent)
            val textColorSelected = context.getColor(com.example.gestaobilhares.ui.R.color.white)
            val textColorNormal = context.getColor(com.example.gestaobilhares.ui.R.color.primary_blue)
            
            // Resetar ambos os botões para estado normal
            listOf(binding.btnStartRoute, binding.btnFinishRoute).forEach { btn ->
                btn.setBackgroundColor(corNormal)
                btn.setTextColor(textColorNormal)
            }
            
            // Destacar o botão ativo (habilitado)
            botaoAtivo?.let { btn ->
                if (btn.isEnabled) {
                    btn.setBackgroundColor(corSelecionada)
                    btn.setTextColor(textColorSelected)
                }
            }
        }
    }
    
    private fun atualizarEstadoFiltros(botaoSelecionado: com.google.android.material.button.MaterialButton) {
        _binding?.let { binding ->
            val context = requireContext()
            val corSelecionada = context.getColor(com.example.gestaobilhares.ui.R.color.primary_blue)
            val corNormal = context.getColor(android.R.color.transparent)
            val textColorSelected = context.getColor(com.example.gestaobilhares.ui.R.color.white)
            val textColorNormal = context.getColor(com.example.gestaobilhares.ui.R.color.text_secondary)
            
            // Resetar todos os botões para estado normal
            listOf(binding.btnFilterAcertados, binding.btnFilterNaoAcertados, binding.btnFilterPendencias).forEach { btn ->
                btn.setBackgroundColor(corNormal)
                btn.setTextColor(textColorNormal)
                btn.strokeColor = context.getColorStateList(com.example.gestaobilhares.ui.R.color.text_secondary)
            }
            
            // Destacar apenas o botão selecionado
            botaoSelecionado.setBackgroundColor(corSelecionada)
            botaoSelecionado.setTextColor(textColorSelected)
            botaoSelecionado.strokeColor = context.getColorStateList(com.example.gestaobilhares.ui.R.color.primary_blue)
        }
    }
    
    /**
     * ✅ NOVO: Sincroniza o estado visual dos filtros com o filtro ativo no ViewModel
     */
    private fun sincronizarEstadoVisualFiltros(filtroAtual: FiltroCliente?) {
        _binding?.let { binding ->
            val botaoAtivo = when (filtroAtual) {
                FiltroCliente.ACERTADOS -> binding.btnFilterAcertados
                FiltroCliente.NAO_ACERTADOS -> binding.btnFilterNaoAcertados
                FiltroCliente.PENDENCIAS -> binding.btnFilterPendencias
                else -> binding.btnFilterNaoAcertados // Padrão
            }
            atualizarEstadoFiltros(botaoAtivo)
        }
    }


    // ✅ FASE 9A: Toggle da busca com animação
    private fun toggleBusca() {
        _binding?.let { binding ->
            val searchLayout = binding.searchLayout
            val isVisible = searchLayout.visibility == View.VISIBLE
            
            if (isVisible) {
                // Esconder campo de pesquisa com animação de slide para cima
                val slideUp = android.view.animation.AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_out_right)
                slideUp.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        searchLayout.visibility = View.GONE
                        binding.etSearch.setText("")
                        viewModel.limparBusca()
                        
                        // Esconder teclado
                        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                    }
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                })
                searchLayout.startAnimation(slideUp)
            } else {
                // Mostrar campo de pesquisa com animação de slide para baixo
                searchLayout.visibility = View.VISIBLE
                val slideDown = android.view.animation.AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_in_left)
                searchLayout.startAnimation(slideDown)
                
                // Focar no campo de pesquisa
                binding.etSearch.requestFocus()
                
                // Mostrar teclado
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(binding.etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }
    
    // ✅ FASE 9A: Configurar busca em tempo real com debounce
    private fun configurarBusca() {
        _binding?.let { binding ->
            // Configurar TextWatcher com debounce
            var searchJob: kotlinx.coroutines.Job? = null
            
            binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    // Cancelar busca anterior
                    searchJob?.cancel()
                    
                    // Iniciar nova busca com delay de 300ms (debounce)
                    searchJob = lifecycleScope.launch {
                        kotlinx.coroutines.delay(300)
                        val query = s?.toString() ?: ""
                        viewModel.buscarClientes(query)
                    }
                }
            })
            
            // Configurar ícone de limpar
            binding.tilSearch.setEndIconOnClickListener {
                binding.etSearch.setText("")
                viewModel.limparBusca()
                binding.etSearch.requestFocus()
            }
        }
    }
    
    // ✅ FASE 9B: Atualizar empty state com filtros combinados
    private fun atualizarEmptyState(mostrar: Boolean) {
        _binding?.let { binding ->
            if (mostrar) {
                // Determinar contexto do empty state
                val filtroAtual = viewModel.getFiltroAtual()
                
                when {
                    filtroAtual == FiltroCliente.PENDENCIAS -> {
                        binding.ivEmptyStateIcon.setImageResource(com.example.gestaobilhares.ui.R.drawable.ic_warning)
                        binding.tvEmptyStateTitle.text = "Nenhuma pendência encontrada"
                        binding.tvEmptyStateMessage.text = "Todos os clientes estão em dia!"
                        binding.btnEmptyStateAction.visibility = View.GONE
                    }
                    filtroAtual == FiltroCliente.ACERTADOS -> {
                        binding.ivEmptyStateIcon.setImageResource(com.example.gestaobilhares.ui.R.drawable.ic_check)
                        binding.tvEmptyStateTitle.text = "Nenhum cliente pago"
                        binding.tvEmptyStateMessage.text = "Nenhum cliente foi pago neste ciclo."
                        binding.btnEmptyStateAction.visibility = View.GONE
                    }
                    filtroAtual == FiltroCliente.NAO_ACERTADOS -> {
                        binding.ivEmptyStateIcon.setImageResource(com.example.gestaobilhares.ui.R.drawable.ic_pending)
                        binding.tvEmptyStateTitle.text = "Nenhum cliente em aberto"
                        binding.tvEmptyStateMessage.text = "Todos os clientes foram pagos neste ciclo."
                        binding.btnEmptyStateAction.visibility = View.GONE
                    }
                    else -> {
                        binding.ivEmptyStateIcon.setImageResource(com.example.gestaobilhares.ui.R.drawable.ic_user)
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
                .setBackgroundTint(requireContext().getColor(com.example.gestaobilhares.ui.R.color.purple_600))
                .setTextColor(requireContext().getColor(com.example.gestaobilhares.ui.R.color.white))
                .show()
        }
    }

    /**
     * ✅ NOVO: Mostra o diálogo de progresso do ciclo
     */
    private fun mostrarDialogoProgressoCiclo() {
        try {
            val cicloProgressoCard = viewModel.cicloProgressoCard.value
            val cicloAtivo = viewModel.cicloAtivo.value
            val rotaNome = viewModel.rotaInfo.value?.nome ?: "Rota"

            if (cicloProgressoCard != null) {
                val dialog = CycleProgressDialog(
                    requireContext(),
                    cicloProgressoCard,
                    cicloAtivo,
                    rotaNome
                )
                dialog.show()
            } else {
                mostrarFeedback("Dados do ciclo não disponíveis", Snackbar.LENGTH_SHORT)
            }
        } catch (e: Exception) {
            Timber.e("ClientListFragment", "Erro ao mostrar diálogo de progresso: ${e.message}")
            mostrarFeedback("Erro ao abrir detalhes do ciclo", Snackbar.LENGTH_SHORT)
        }
    }

    /**
     * ✅ NOVO: Mostra o diálogo de confirmação para finalizar o ciclo
     */
    private fun mostrarDialogoConfirmacaoFinalizar() {
        val cicloAtivo = viewModel.cicloAtivo.value
        val rotaNome = viewModel.rotaInfo.value?.nome ?: "Rota"
        
        val mensagem = if (cicloAtivo != null) {
            "Tem certeza que deseja finalizar o ${cicloAtivo.numeroCiclo}º Acerto da rota \"$rotaNome\"?\n\n" +
            "⚠️ ATENÇÃO: Após o fechamento do ciclo:\n" +
            "• Não será mais possível lançar novos acertos\n" +
            "• Não será mais possível adicionar despesas\n" +
            "• O ciclo será marcado como finalizado\n\n" +
            "Esta ação não pode ser desfeita."
        } else {
            "Tem certeza que deseja finalizar o ciclo atual da rota \"$rotaNome\"?\n\n" +
            "⚠️ ATENÇÃO: Após o fechamento do ciclo:\n" +
            "• Não será mais possível lançar novos acertos\n" +
            "• Não será mais possível adicionar despesas\n" +
            "• O ciclo será marcado como finalizado\n\n" +
            "Esta ação não pode ser desfeita."
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Finalização do Ciclo")
            .setMessage(mensagem)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Sim, Finalizar") { _, _ ->
                viewModel.finalizarRota()
                mostrarFeedback("Ciclo finalizado com sucesso!", Snackbar.LENGTH_SHORT)
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(true)
            .show()
    }
    
    private fun mostrarAlertaRotaNaoIniciada() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Rota Não Iniciada")
            .setMessage("Para acessar os detalhes do cliente e fazer acertos, é necessário iniciar a rota primeiro.")
            .setPositiveButton("Entendi", null)
            .show()
    }

    /**
     * ✅ NOVO: Configura o FAB expandível com animações
     */
    private fun configurarFabExpandivel() {
        _binding?.let { binding ->
            var isExpanded = false
            
            // FAB Principal
            binding.fabMain.setOnClickListener {
                if (isExpanded) {
                    // Recolher FABs
                    recolherFabMenu(binding)
                    isExpanded = false
                } else {
                    // Expandir FABs
                    expandirFabMenu(binding)
                    isExpanded = true
                }
            }
            
            // Container Novo Cliente
            binding.fabNewClientContainer.setOnClickListener {
                val action = ClientListFragmentDirections
                    .actionClientListFragmentToClientRegisterFragment(args.rotaId)
                findNavController().navigate(action)
                // Recolher menu após clicar
                recolherFabMenu(binding)
                isExpanded = false
            }
            
            // Container Nova Despesa
            binding.fabNewExpenseContainer.setOnClickListener {
                val action = ClientListFragmentDirections
                    .actionClientListFragmentToExpenseRegisterFragment(args.rotaId)
                findNavController().navigate(action)
                // Recolher menu após clicar
                recolherFabMenu(binding)
                isExpanded = false
            }
        }
    }
    
    /**
     * ✅ NOVO: Expande o menu FAB com animação
     */
    private fun expandirFabMenu(binding: FragmentClientListBinding) {
        // Mostrar container
        binding.fabExpandedContainer.visibility = View.VISIBLE
        
        // Animar entrada dos containers
        binding.fabNewClientContainer.alpha = 0f
        binding.fabNewExpenseContainer.alpha = 0f
        
        // Animar "Nova Despesa" primeiro (mais próximo do FAB principal)
        binding.fabNewExpenseContainer.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(100)
            .start()
            
        // Animar "Novo Cliente" depois (mais afastado do FAB principal)
        binding.fabNewClientContainer.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(200)
            .start()
            
        // Rotacionar ícone do FAB principal
        binding.fabMain.animate()
            .rotation(45f)
            .setDuration(200)
            .start()
    }
    
    /**
     * ✅ NOVO: Recolhe o menu FAB com animação
     */
    private fun recolherFabMenu(binding: FragmentClientListBinding) {
        // Animar saída dos containers
        binding.fabNewClientContainer.animate()
            .alpha(0f)
            .setDuration(200)
            .start()
            
        binding.fabNewExpenseContainer.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.fabExpandedContainer.visibility = View.GONE
            }
            .start()
            
        // Rotacionar ícone do FAB principal de volta
        binding.fabMain.animate()
            .rotation(0f)
            .setDuration(200)
            .start()
    }

    /**
     * ✅ NOVO: Mostra o diálogo de filtros de clientes
     */
    private fun mostrarDialogoFiltros() {
        try {
            val filtroAtual = viewModel.getFiltroAtual()
            ClientFilterDialog.show(
                context = requireContext(),
                currentFilter = filtroAtual,
                onFilterSelected = { filtroSelecionado ->
                    viewModel.aplicarFiltro(filtroSelecionado)
                    mostrarFeedback("Filtro aplicado: ${getNomeFiltro(filtroSelecionado)}", Snackbar.LENGTH_SHORT)
                }
            )
        } catch (e: Exception) {
            Timber.e("ClientListFragment", "Erro ao mostrar diálogo de filtros: ${e.message}")
            mostrarFeedback("Erro ao abrir filtros: ${e.message}", Snackbar.LENGTH_LONG)
        }
    }

    /**
     * ✅ NOVO: Mostra o diálogo de pesquisa avançada
     */
    private fun mostrarDialogoPesquisaAvancada() {
        try {
            AdvancedSearchDialog.show(
                context = requireContext(),
                onSearch = { searchType, criteria ->
                    viewModel.pesquisarAvancada(searchType, criteria)
                    mostrarFeedback("Pesquisa: ${searchType.label} - $criteria", Snackbar.LENGTH_SHORT)
                }
            )
        } catch (e: Exception) {
            Timber.e("ClientListFragment", "Erro ao mostrar diálogo de pesquisa: ${e.message}")
            mostrarFeedback("Erro ao abrir pesquisa: ${e.message}", Snackbar.LENGTH_LONG)
        }
    }

    /**
     * ✅ NOVO: Retorna o nome amigável do filtro
     */
            private fun getNomeFiltro(filtro: FiltroCliente): String {
            return when (filtro) {
                FiltroCliente.ACERTADOS -> "Pago"
                FiltroCliente.NAO_ACERTADOS -> "Em aberto"
                FiltroCliente.TODOS -> "Todos"
                FiltroCliente.PENDENCIAS -> "Pendências"
            }
        }

    /**
     * ✅ NOVA FUNÇÃO: Navega para a tela de rotas
     */
    private fun navegarParaRotas() {
        try {
            // Navegar diretamente para a tela de rotas
            findNavController().navigate(com.example.gestaobilhares.ui.R.id.routesFragment)
            Timber.d("ClientListFragment", "✅ Navegando para tela de rotas")
        } catch (e: Exception) {
            Timber.w("ClientListFragment", "⚠️ Erro ao navegar para rotas: ${e.message}")
            // Fallback: popBackStack normal
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 

