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
import com.example.gestaobilhares.databinding.FragmentClientDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment para exibir detalhes do cliente e histórico de acertos
 * FASE 4A - Implementação crítica para desbloqueio do fluxo
 */
@AndroidEntryPoint
class ClientDetailFragment : Fragment() {

    private var _binding: FragmentClientDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ClientDetailViewModel by viewModels()
    private val args: ClientDetailFragmentArgs by navArgs()
    
    private lateinit var settlementHistoryAdapter: SettlementHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupRecyclerView()
        observeViewModel()
        
        // Carregar dados do cliente
        viewModel.loadClientDetails(args.clienteId)
    }

    private fun setupUI() {
        // Botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Botão Novo Acerto - FASE 4A: Navegar para SettlementFragment ✅
        binding.btnNewSettlement.setOnClickListener {
            val action = ClientDetailFragmentDirections
                .actionClientDetailFragmentToSettlementFragment(args.clienteId)
            findNavController().navigate(action)
        }
        
        // Botões de contato
        binding.fabWhatsApp.setOnClickListener {
            // TODO: Implementar chamada WhatsApp
        }
        
        binding.fabPhone.setOnClickListener {
            // TODO: Implementar chamada telefônica
        }
    }

    private fun setupRecyclerView() {
        settlementHistoryAdapter = SettlementHistoryAdapter { acerto ->
            // Navegar para detalhes do acerto - FASE 4B+ IMPLEMENTADO! ✅
            val action = ClientDetailFragmentDirections
                .actionClientDetailFragmentToSettlementDetailFragment(acerto.id)
            findNavController().navigate(action)
        }
        
        binding.rvSettlementHistory.apply {
            adapter = settlementHistoryAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.clientDetails.collect { client ->
                client?.let { updateClientUI(it) }
            }
        }
        
        lifecycleScope.launch {
            viewModel.settlementHistory.collect { settlements ->
                settlementHistoryAdapter.submitList(settlements)
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { _ ->
                // TODO: Adicionar ProgressBar no layout se necessário
            }
        }
    }

    private fun updateClientUI(client: ClienteResumo) {
        binding.apply {
            tvClientName.text = client.nome
            tvClientId.text = "ID: #${client.id}"
            tvClientAddress.text = client.endereco
            tvLastVisit.text = "Última visita: ${client.ultimaVisita}"
            tvActiveTables.text = "${client.mesasAtivas} Mesas ativas"
            
            // Observações do cliente
            tvObservations.text = client.observacoes
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
