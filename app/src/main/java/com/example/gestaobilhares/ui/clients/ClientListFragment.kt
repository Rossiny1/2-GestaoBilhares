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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        
        try {
            // Verificar se binding está disponível
            if (_binding == null) {
                android.util.Log.e("ClientListFragment", "Binding é null em onViewCreated")
                return
            }
            
            configurarRecyclerView()
            configurarBotoes()
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
            
            // Botão buscar
            binding.btnSearch.setOnClickListener {
                // TODO: Implementar busca
            }
            
            // Botão filtrar
            binding.btnFilter.setOnClickListener {
                // TODO: Implementar filtro
            }
            
            // Controle de status da rota
            binding.btnStartRoute.setOnClickListener {
                viewModel.iniciarRota()
            }
            
            binding.btnFinishRoute.setOnClickListener {
                viewModel.finalizarRota()
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
        
        // Observar ciclo de acerto
        lifecycleScope.launch {
            viewModel.cicloAcerto.collect { ciclo ->
                try {
                    _binding?.tvCycleTitle?.text = "${ciclo}º Acerto"
                } catch (e: Exception) {
                    android.util.Log.e("ClientListFragment", "Erro ao atualizar ciclo de acerto: ${e.message}")
                }
            }
        }
        
        // Observar clientes
        lifecycleScope.launch {
            viewModel.clientes.collect { clientes ->
                try {
                    clientAdapter.submitList(clientes)
                    
                    // Mostrar/esconder empty state
                    _binding?.let { binding ->
                        if (clientes.isEmpty()) {
                            binding.emptyStateLayout.visibility = View.VISIBLE
                            binding.rvClients.visibility = View.GONE
                        } else {
                            binding.emptyStateLayout.visibility = View.GONE
                            binding.rvClients.visibility = View.VISIBLE
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ClientListFragment", "Erro ao atualizar lista de clientes: ${e.message}")
                }
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
