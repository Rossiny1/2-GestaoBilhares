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
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import java.text.NumberFormat
import android.view.animation.AnimationUtils
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import com.google.android.material.snackbar.Snackbar

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
        
        // ✅ FASE 8C: Inicializar ViewModel com CicloAcertoRepository
        val database = AppDatabase.getDatabase(requireContext())
        viewModel = ClientListViewModel(
            ClienteRepository(database.clienteDao()),
            RotaRepository(database.rotaDao()),
            CicloAcertoRepository(database.cicloAcertoDao())
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
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Cadastrar Despesa")
                    .setMessage("Funcionalidade será implementada na próxima fase!")
                    .setPositiveButton("OK", null)
                    .show()
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
        
        // ✅ FASE 8C: Observar ciclo de acerto real
        lifecycleScope.launch {
            viewModel.cicloAcerto.collect { ciclo ->
                try {
                    _binding?.tvCycleTitle?.text = "${ciclo}º Acerto"
                } catch (e: Exception) {
                    android.util.Log.e("ClientListFragment", "Erro ao atualizar ciclo de acerto: ${e.message}")
                }
            }
        }
        
        // ✅ FASE 8C: Observar entidade do ciclo (datas, status, progresso)
        lifecycleScope.launch {
            viewModel.cicloAcertoEntity.collect { cicloEntity ->
                try {
                    atualizarInformacoesCiclo(cicloEntity)
                } catch (e: Exception) {
                    android.util.Log.e("ClientListFragment", "Erro ao atualizar entidade do ciclo: ${e.message}")
                }
            }
        }
        
        // ✅ FASE 8C: Observar status do ciclo
        lifecycleScope.launch {
            viewModel.statusCiclo.collect { statusCiclo ->
                try {
                    atualizarStatusCiclo(statusCiclo)
                } catch (e: Exception) {
                    android.util.Log.e("ClientListFragment", "Erro ao atualizar status do ciclo: ${e.message}")
                }
            }
        }
        
        // ✅ FASE 8C: Observar progresso do ciclo
        lifecycleScope.launch {
            viewModel.progressoCiclo.collect { progresso ->
                try {
                    atualizarProgressoCiclo(progresso)
                } catch (e: Exception) {
                    android.util.Log.e("ClientListFragment", "Erro ao atualizar progresso do ciclo: ${e.message}")
                }
            }
        }
        
        // ✅ FASE 9A: Observar clientes com empty state melhorado
        lifecycleScope.launch {
            viewModel.clientes.collect { clientes ->
                try {
                    clientAdapter.submitList(clientes)
                    
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
        
        // ✅ FASE 9A: Observar estado de carregamento
        lifecycleScope.launch {
            viewModel.isLoading.collect { carregando ->
                _binding?.progressBar?.visibility = if (carregando) View.VISIBLE else View.GONE
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
                    binding.statusIndicator.backgroundTintList = 
                        android.content.res.ColorStateList.valueOf(context.getColor(R.color.green_600))
                    binding.btnStartRoute.isEnabled = false
                    binding.btnFinishRoute.isEnabled = true
                }
                StatusRota.FINALIZADA -> {
                    binding.tvRouteStatus.text = "Finalizada"
                    binding.statusIndicator.backgroundTintList = 
                        android.content.res.ColorStateList.valueOf(context.getColor(R.color.purple_600))
                    binding.btnStartRoute.isEnabled = true
                    binding.btnFinishRoute.isEnabled = false
                }
                StatusRota.PAUSADA -> {
                    binding.tvRouteStatus.text = "Não Iniciada"
                    binding.statusIndicator.backgroundTintList = 
                        android.content.res.ColorStateList.valueOf(context.getColor(R.color.orange_600))
                    binding.btnStartRoute.isEnabled = true
                    binding.btnFinishRoute.isEnabled = false
                }
                else -> {
                    binding.tvRouteStatus.text = "Não Iniciada"
                    binding.statusIndicator.backgroundTintList = 
                        android.content.res.ColorStateList.valueOf(context.getColor(R.color.orange_600))
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
    
    // ✅ FASE 8C: Atualizar informações detalhadas do ciclo
    private fun atualizarInformacoesCiclo(cicloEntity: com.example.gestaobilhares.data.entities.CicloAcertoEntity?) {
        _binding?.let { binding ->
            cicloEntity?.let { ciclo ->
                val formatter = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))
                val dataInicio = ciclo.dataInicio?.let { formatter.format(it) } ?: "Não iniciado"
                val dataFim = ciclo.dataFim?.let { formatter.format(it) } ?: "Em andamento"
                binding.tvCycleInfo.text = "Início: $dataInicio • Fim: $dataFim"
                val valor = ciclo.valorTotalAcertado
                val valorFormatado = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)
                binding.tvCycleValue.text = "Total: $valorFormatado"
            } ?: run {
                binding.tvCycleInfo.text = "Ciclo não iniciado"
                binding.tvCycleValue.text = "Total: R$ 0,00"
            }
        }
    }
    
    // ✅ FASE 9A: Atualizar status visual do ciclo com animação
    private fun atualizarStatusCiclo(statusCiclo: StatusCicloAcerto?) {
        _binding?.let { binding ->
            val context = requireContext()
            val statusText: String
            val color: Int
            
            when (statusCiclo) {
                StatusCicloAcerto.EM_ANDAMENTO -> {
                    statusText = "Em Andamento"
                    color = context.getColor(R.color.green_600)
                }
                StatusCicloAcerto.FINALIZADO -> {
                    statusText = "Finalizado"
                    color = context.getColor(R.color.purple_600)
                }
                StatusCicloAcerto.CANCELADO -> {
                    statusText = "Cancelado"
                    color = context.getColor(R.color.red_600)
                }
                else -> {
                    statusText = "Não Iniciado"
                    color = context.getColor(R.color.gray_600)
                }
            }
            
            // Atualizar textos
            binding.tvCycleStatusLabel.text = statusText
            binding.tvCycleStatus.text = statusText
            
            // Animar mudança de cor do indicador
            binding.cycleStatusIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            
            // Animação de pulse para status em andamento
            if (statusCiclo == StatusCicloAcerto.EM_ANDAMENTO) {
                val pulse = AnimationUtils.loadAnimation(context, R.anim.pulse)
                binding.cycleStatusIndicator.startAnimation(pulse)
            } else {
                binding.cycleStatusIndicator.clearAnimation()
            }
        }
    }
    
    // ✅ FASE 9A: Atualizar progresso visual do ciclo com animação
    private fun atualizarProgressoCiclo(percentual: Int?) {
        _binding?.let { binding ->
            val percent = percentual ?: 0
            binding.tvCycleProgress.text = "$percent% concluído"
            binding.tvCyclePercent.text = "$percent%"
            
            // Animação da barra de progresso
            binding.progressBarCycle?.let { progressBar ->
                progressBar.visibility = View.VISIBLE
                
                // Animar a mudança de progresso
                val animator = android.animation.ValueAnimator.ofInt(progressBar.progress, percent)
                animator.duration = 500
                animator.addUpdateListener { animation ->
                    progressBar.progress = animation.animatedValue as Int
                }
                animator.start()
                
                // Mudar cor baseado no progresso
                val context = requireContext()
                val color = when {
                    percent >= 100 -> context.getColor(R.color.green_600)
                    percent >= 50 -> context.getColor(R.color.orange_600)
                    else -> context.getColor(R.color.blue_600)
                }
                progressBar.progressTintList = android.content.res.ColorStateList.valueOf(color)
            }
        }
    }
    
    // ✅ FASE 9A: Toggle da busca com animação
    private fun toggleBusca() {
        _binding?.let { binding ->
            if (binding.searchLayout.visibility == View.VISIBLE) {
                // Desativar busca com animação de fade out
                val fadeOut = AlphaAnimation(1.0f, 0.0f)
                fadeOut.duration = 200
                fadeOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        binding.searchLayout.visibility = View.GONE
                    }
                    override fun onAnimationRepeat(animation: Animation?) {}
                })
                binding.searchLayout.startAnimation(fadeOut)
                binding.btnSearch.setImageResource(R.drawable.ic_search)
                binding.etSearch.setText("")
                viewModel.limparBusca()
            } else {
                // Ativar busca com animação de fade in
                binding.searchLayout.visibility = View.VISIBLE
                val fadeIn = AlphaAnimation(0.0f, 1.0f)
                fadeIn.duration = 200
                binding.searchLayout.startAnimation(fadeIn)
                binding.btnSearch.setImageResource(R.drawable.ic_close)
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
            var searchJob: kotlinx.coroutines.Job? = null
            
            binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    // Cancelar busca anterior
                    searchJob?.cancel()
                    
                    // Nova busca com delay de 300ms
                    searchJob = lifecycleScope.launch {
                        kotlinx.coroutines.delay(300)
                        val query = s?.toString()?.trim() ?: ""
                        viewModel.buscarClientes(query)
                    }
                }
            })
        }
    }
    
    // ✅ FASE 9B: Atualizar empty state com filtros combinados
    private fun atualizarEmptyState(mostrar: Boolean) {
        _binding?.let { binding ->
            if (mostrar) {
                // Determinar contexto do empty state
                val isBusca = binding.searchLayout.visibility == View.VISIBLE
                val filtroAtual = viewModel.getFiltroAtual()
                val temBusca = viewModel.buscaAtual.value.isNotBlank()
                
                when {
                    isBusca && temBusca -> {
                        binding.ivEmptyStateIcon.setImageResource(R.drawable.ic_search)
                        binding.tvEmptyStateTitle.text = "Nenhum resultado encontrado"
                        binding.tvEmptyStateMessage.text = "Tente usar termos diferentes na busca ou ajustar os filtros."
                        binding.btnEmptyStateAction.visibility = View.GONE
                    }
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
