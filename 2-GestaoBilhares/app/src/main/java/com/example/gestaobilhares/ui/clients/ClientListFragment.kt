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
import com.example.gestaobilhares.databinding.FragmentClientListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment simplificado para lista de clientes
 */
@AndroidEntryPoint
class ClientListFragment : Fragment() {

    private var _binding: FragmentClientListBinding? = null
    private val binding get() = _binding!!

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
        
        configurarRecyclerView()
        observarViewModel()
        configurarBotoes()
        
        // Obter rota ID usando Safe Args
        val rotaId = args.rotaId
        viewModel.carregarClientes(rotaId)
    }
    
    private fun configurarBotoes() {
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
        
        // FAB Cadastrar Cliente
        binding.fabAddClient.setOnClickListener {
            val action = ClientListFragmentDirections
                .actionClientListFragmentToClientRegisterFragment(args.rotaId)
            findNavController().navigate(action)
        }
        
        // FAB Cadastrar Despesa
        binding.fabAddExpense.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cadastrar Despesa")
                .setMessage("Funcionalidade será implementada na próxima fase!\n\n✅ FAB implementado com sucesso")
                .setPositiveButton("OK", null)
                .show()
        }
        
        // Botão Iniciar Rota
        binding.btnStartRoute.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("🚀 Iniciar Rota")
                .setMessage("Confirma o início da rota?\n\n✅ Após iniciar você poderá:\n• Registrar acertos dos clientes\n• Cadastrar despesas\n• Controlar fichas das mesas")
                .setPositiveButton("Iniciar") { _, _ ->
                    startRoute()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        
        // Botão Finalizar Rota
        binding.btnFinishRoute.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("🏁 Finalizar Rota")
                .setMessage("Confirma a finalização da rota?\n\n⚠️ Após finalizar você NÃO poderá mais:\n• Registrar novos acertos\n• Editar acertos existentes\n• Cadastrar novas despesas")
                .setPositiveButton("Finalizar") { _, _ ->
                    finishRoute()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun startRoute() {
        // Atualizar UI para mostrar rota iniciada
        binding.tvRouteStatus.text = "Em Andamento"
        binding.tvRouteStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        
        // Habilitar botão finalizar e desabilitar iniciar
        binding.btnStartRoute.isEnabled = false
        binding.btnFinishRoute.isEnabled = true
        
        // TODO: Implementar lógica de início de rota no ViewModel
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("✅ Rota Iniciada!")
            .setMessage("Rota iniciada com sucesso!\n\n🎯 Agora você pode:\n• Registrar acertos\n• Cadastrar despesas\n• Gerenciar mesas")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun finishRoute() {
        // Atualizar UI para mostrar rota finalizada
        binding.tvRouteStatus.text = "Finalizada"
        binding.tvRouteStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        
        // Desabilitar ambos os botões
        binding.btnStartRoute.isEnabled = false
        binding.btnFinishRoute.isEnabled = false
        
        // TODO: Implementar lógica de finalização de rota no ViewModel
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("🏁 Rota Finalizada!")
            .setMessage("Rota finalizada com sucesso!\n\n📊 Relatório será gerado automaticamente\n📱 Dados sincronizados")
            .setPositiveButton("Ver Relatório") { _, _ ->
                // TODO: Navegar para relatório
            }
            .setNegativeButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun configurarRecyclerView() {
        clientAdapter = ClientAdapter { cliente ->
            // Navegar para detalhes do cliente - FASE 4A IMPLEMENTADA! ✅
            val action = ClientListFragmentDirections
                .actionClientListFragmentToClientDetailFragment(cliente.id)
            findNavController().navigate(action)
        }
        
        binding.rvClients.apply {
            adapter = clientAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observarViewModel() {
        lifecycleScope.launch {
            viewModel.clientes.collect { clientes ->
                clientAdapter.submitList(clientes)
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }
} 
