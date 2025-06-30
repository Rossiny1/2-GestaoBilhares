package com.example.gestaobilhares.ui.clients

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentClientDetailBinding
import com.example.gestaobilhares.data.entities.Mesa
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.example.gestaobilhares.R
import android.util.Log
import com.example.gestaobilhares.ui.settlement.MesaDTO
import android.widget.Toast

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
    private lateinit var mesasAdapter: MesasAdapter

    private val REQUEST_CODE_NOVO_ACERTO = 1001

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
        setupMesasSection()
        // Desabilitar botão até mesas carregarem
        binding.btnNewSettlement.isEnabled = false
        lifecycleScope.launch {
            viewModel.mesasCliente.collect { mesas ->
                binding.btnNewSettlement.isEnabled = mesas.isNotEmpty()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Verificar se há um novo acerto salvo para adicionar ao histórico
        verificarNovoAcerto()
    }
    
    /**
     * Verifica se há um novo acerto salvo no cache temporário e recarrega o histórico
     */
    private fun verificarNovoAcerto() {
        val sharedPref = requireActivity().getSharedPreferences("acerto_temp", android.content.Context.MODE_PRIVATE)
        val clienteIdSalvo = sharedPref.getLong("cliente_id", -1L)
        val acertoSalvo = sharedPref.getBoolean("acerto_salvo", false)
        
        // Verificar se há um acerto salvo para este cliente
        if (clienteIdSalvo == args.clienteId && acertoSalvo) {
            Log.d("ClientDetailFragment", "Acerto salvo detectado para cliente: $clienteIdSalvo")
            
            // Recarregar histórico do banco de dados
            viewModel.loadSettlementHistory(args.clienteId)
            
            // Limpar cache
            with(sharedPref.edit()) {
                clear()
                apply()
            }
            
            // Mostrar toast de confirmação
            Toast.makeText(requireContext(), "✅ Acerto salvo com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUI() {
        // Botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Botão Novo Acerto - FASE 4A: Navegar para SettlementFragment ✅
        binding.btnNewSettlement.setOnClickListener {
            val mesasAtivas = viewModel.mesasCliente.value
            Log.d("NovoAcerto", "Mesas ativas encontradas: ${mesasAtivas.size}")
            
            if (mesasAtivas.isEmpty()) {
                Toast.makeText(requireContext(), "Este cliente não possui mesas ativas para acerto.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            // Obter dados do cliente para incluir nos MesaDTOs
            val cliente = viewModel.clientDetails.value
            val valorFicha = cliente?.valorFicha ?: 0.0
            val comissaoFicha = cliente?.comissaoFicha ?: 0.0
            
            val mesasDTO = mesasAtivas.map { mesa ->
                Log.d("NovoAcerto", "Convertendo mesa ${mesa.numero} - ID: ${mesa.id}")
                MesaDTO(
                    id = mesa.id,
                    numero = mesa.numero,
                    fichasInicial = mesa.fichasInicial ?: 0,
                    fichasFinal = mesa.fichasFinal ?: 0,
                    tipoMesa = mesa.tipoMesa.name,
                    ativa = mesa.ativa,
                    valorFixo = mesa.valorFixo ?: 0.0,
                    valorFicha = valorFicha,
                    comissaoFicha = comissaoFicha
                )
            }.toTypedArray()
            
            Log.d("NovoAcerto", "Enviando ${mesasDTO.size} mesas para SettlementFragment")
            Log.d("NovoAcerto", "Mesas: ${mesasDTO.joinToString { "Mesa ${it.numero} (ID: ${it.id})" }}")
            Log.d("NovoAcerto", "Valor Ficha: $valorFicha, Comissão Ficha: $comissaoFicha")
            
            val action = ClientDetailFragmentDirections
                .actionClientDetailFragmentToSettlementFragment(
                    mesasDTO,
                    args.clienteId
                )
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

    private fun setupMesasSection() {
        mesasAdapter = MesasAdapter(
            onRetirarMesa = { mesa ->
                val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_retirar_mesa, null)
                val etRelogioFinal = dialogView.findViewById<EditText>(R.id.etRelogioFinal)
                val etValorRecebido = dialogView.findViewById<EditText>(R.id.etValorRecebido)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Retirar Mesa")
                    .setView(dialogView)
                    .setMessage("Informe o relógio final e o valor recebido para retirar a mesa ${mesa.numero}.")
                    .setPositiveButton("Confirmar") { _, _ ->
                        val relogioFinal = etRelogioFinal.text.toString().toIntOrNull() ?: 0
                        val valorRecebido = etValorRecebido.text.toString().toDoubleOrNull() ?: 0.0
                        viewModel.retirarMesaDoCliente(mesa.id, args.clienteId, relogioFinal, valorRecebido)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvMesasCliente.layoutManager = gridLayoutManager
        binding.rvMesasCliente.adapter = mesasAdapter
        binding.btnAdicionarMesa.setOnClickListener {
            val action = ClientDetailFragmentDirections.actionClientDetailFragmentToMesasDepositoFragment(args.clienteId)
            findNavController().navigate(action)
        }
        lifecycleScope.launch {
            viewModel.mesasCliente.collect { mesas ->
                mesasAdapter.submitList(mesas)
            }
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
                Log.d("ClientDetailFragment", "=== HISTÓRICO ATUALIZADO ===")
                Log.d("ClientDetailFragment", "Quantidade de acertos: ${settlements.size}")
                settlements.forEachIndexed { index, acerto ->
                    Log.d("ClientDetailFragment", "Acerto $index: ID=${acerto.id}, Data=${acerto.data}, Valor=${acerto.valor}, Status=${acerto.status}")
                }
                settlementHistoryAdapter.submitList(settlements)
                Log.d("ClientDetailFragment", "Lista enviada para adapter: ${settlements.size} itens")
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
            tvClientAddress.text = client.endereco
            tvLastVisit.text = "Última visita: ${client.ultimaVisita}"
            tvActiveTables.text = "${client.mesasAtivas} Mesas ativas"
            // Observação do cliente
            if (viewModel.isAdminUser()) {
                etObservations.visibility = View.VISIBLE
                tvObservations.visibility = View.GONE
                etObservations.setText(client.observacoes)
                etObservations.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val novaObs = etObservations.text.toString()
                        if (novaObs != client.observacoes) {
                            viewModel.salvarObservacaoCliente(client.id, novaObs)
                        }
                    }
                }
            } else {
                etObservations.visibility = View.GONE
                tvObservations.visibility = View.VISIBLE
                tvObservations.text = client.observacoes
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_NOVO_ACERTO && resultCode == Activity.RESULT_OK) {
            val resumo = data?.getParcelableExtra<AcertoResumo>("resumoAcerto")
            resumo?.let {
                viewModel.adicionarAcertoNoHistorico(it)
            }
        }
    }
} 
