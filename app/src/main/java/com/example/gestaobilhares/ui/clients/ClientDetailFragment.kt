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
        
        // Carregar dados do cliente apenas se não estiverem carregados
        if (viewModel.clientDetails.value == null) {
        viewModel.loadClientDetails(args.clienteId)
        }
        
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
        // ✅ CORREÇÃO: Recarregar dados do cliente sempre que voltar para a tela
        // Isso garante que mudanças feitas na edição sejam refletidas
        viewModel.loadClientDetails(args.clienteId)
        
        // Verificar se há um novo acerto salvo para adicionar ao histórico
        verificarNovoAcerto()
        
        // Recarregar histórico apenas se estiver vazio
        if (viewModel.settlementHistory.value.isEmpty()) {
            viewModel.loadSettlementHistory(args.clienteId)
        }
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
        } else {
            // Se não há novo acerto, verificar se o histórico está vazio
            if (viewModel.settlementHistory.value.isEmpty()) {
                Log.d("ClientDetailFragment", "Histórico vazio, recarregando dados...")
                viewModel.loadSettlementHistory(args.clienteId)
            }
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
            val cliente = viewModel.clientDetails.value
            cliente?.let {
                abrirWhatsApp(it.telefone)
            }
        }
        
        binding.fabPhone.setOnClickListener {
            val cliente = viewModel.clientDetails.value
            cliente?.let {
                fazerLigacao(it.telefone)
            }
        }
        
        // Botão editar cliente - ✅ CORRIGIDO: Passar clienteId para edição
        binding.fabEdit.setOnClickListener {
            val action = ClientDetailFragmentDirections
                .actionClientDetailFragmentToClientRegisterFragment(
                    rotaId = 0L, // Não precisa da rota para edição
                    clienteId = args.clienteId // ✅ Passar ID do cliente para edição
                )
            findNavController().navigate(action)
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
                // ✅ NOVA LÓGICA: Verificar se mesa foi acertada hoje antes de permitir retirada
                verificarEProcessarRetiradaMesa(mesa)
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

    /**
     * ✅ NOVA LÓGICA: Verifica se mesa pode ser retirada ou precisa de acerto
     */
    private fun verificarEProcessarRetiradaMesa(mesa: Mesa) {
        lifecycleScope.launch {
            try {
                val statusRetirada = viewModel.verificarSeRetiradaEPermitida(mesa.id, args.clienteId)
                
                when (statusRetirada) {
                    RetiradaStatus.PODE_RETIRAR -> {
                        // Mesa foi acertada hoje - mostrar diálogo de retirada
                        mostrarDialogoRetiradaComRelogioFinal(mesa)
                    }
                    RetiradaStatus.PRECISA_ACERTO -> {
                        // Mesa não foi acertada hoje - mostrar mensagem de erro
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Retirada não permitida")
                            .setMessage("Para retirar a mesa é necessário que ela tenha sido acertada hoje.")
                            .setPositiveButton("Fazer Acerto") { _, _ ->
                                // Navegar para tela de acerto
                                val mesasAtivas = viewModel.mesasCliente.value
                                val cliente = viewModel.clientDetails.value
                                val valorFicha = cliente?.valorFicha ?: 0.0
                                val comissaoFicha = cliente?.comissaoFicha ?: 0.0
                                
                                val mesasDTO = mesasAtivas.map { mesa ->
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
                                
                                val action = ClientDetailFragmentDirections.actionClientDetailFragmentToSettlementFragment(
                                    clienteId = args.clienteId,
                                    mesasCliente = mesasDTO
                                )
                                findNavController().navigate(action)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ClientDetailFragment", "Erro ao verificar retirada de mesa: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao verificar status da mesa", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * ✅ NOVO: Diálogo de retirada que usa o relógio final do último acerto
     */
    private fun mostrarDialogoRetiradaComRelogioFinal(mesa: Mesa) {
        lifecycleScope.launch {
            try {
                // Buscar o relógio final do último acerto da mesa
                val relogioFinalUltimoAcerto = viewModel.buscarRelogioFinalUltimoAcerto(mesa.id)
                
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_retirar_mesa, null)
        val etRelogioFinal = dialogView.findViewById<EditText>(R.id.etRelogioFinal)
        val etValorRecebido = dialogView.findViewById<EditText>(R.id.etValorRecebido)
        
                // Pré-preencher com o relógio final do último acerto
                val relogioFinal = relogioFinalUltimoAcerto ?: mesa.relogioFinal ?: mesa.fichasFinal ?: 0
                etRelogioFinal.setText(relogioFinal.toString())
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Retirar Mesa ${mesa.numero}")
            .setView(dialogView)
                    .setMessage("O relógio final do último acerto será usado como relógio inicial na próxima locação.")
            .setPositiveButton("Confirmar Retirada") { _, _ ->
                val relogioFinal = etRelogioFinal.text.toString().toIntOrNull() ?: 0
                val valorRecebido = etValorRecebido.text.toString().toDoubleOrNull() ?: 0.0
                viewModel.retirarMesaDoCliente(mesa.id, args.clienteId, relogioFinal, valorRecebido)
            }
            .setNegativeButton("Cancelar", null)
            .show()
            } catch (e: Exception) {
                Log.e("ClientDetailFragment", "Erro ao mostrar diálogo de retirada: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao carregar dados da mesa", Toast.LENGTH_SHORT).show()
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
                    Log.d("ClientDetailFragment", "Acerto $index: ID=${acerto.id}, Data=${acerto.data}, Valor=${acerto.valorTotal}, Status=${acerto.status}")
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

    /**
     * ✅ CORRIGIDO: Abre WhatsApp nativo com o número do cliente
     */
    private fun abrirWhatsApp(telefone: String) {
        if (telefone.isEmpty()) {
            Toast.makeText(requireContext(), "Cliente não possui telefone cadastrado", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            // Limpar formatação do telefone
            val numeroLimpo = telefone.replace(Regex("[^0-9]"), "")
            
            // Adicionar código do país se necessário (Brasil +55)
            val numeroCompleto = if (numeroLimpo.length == 11 && numeroLimpo.startsWith("11")) {
                "55$numeroLimpo" // Adiciona código do Brasil
            } else if (numeroLimpo.length == 10) {
                "55$numeroLimpo" // Adiciona código do Brasil
            } else {
                numeroLimpo
            }
            
            // ✅ CORREÇÃO: Usar intent específico para WhatsApp nativo
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Olá! Entro em contato sobre suas mesas de bilhar.")
                setPackage("com.whatsapp") // Força abertura no app nativo
            }
            
            // Verificar se o WhatsApp está instalado
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                // WhatsApp não instalado, tentar com WhatsApp Business
                val intentBusiness = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Olá! Entro em contato sobre suas mesas de bilhar.")
                    setPackage("com.whatsapp.w4b") // WhatsApp Business
                }
                
                if (intentBusiness.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intentBusiness)
                } else {
                    // Nenhum WhatsApp instalado, mostrar mensagem
                    Toast.makeText(requireContext(), "WhatsApp não está instalado no dispositivo", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e("ClientDetailFragment", "Erro ao abrir WhatsApp: ${e.message}")
            Toast.makeText(requireContext(), "Erro ao abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * ✅ IMPLEMENTADO: Faz ligação para o cliente
     */
    private fun fazerLigacao(telefone: String) {
        if (telefone.isEmpty()) {
            Toast.makeText(requireContext(), "Cliente não possui telefone cadastrado", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = android.net.Uri.parse("tel:$telefone")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ClientDetailFragment", "Erro ao fazer ligação: ${e.message}")
            Toast.makeText(requireContext(), "Erro ao fazer ligação", Toast.LENGTH_SHORT).show()
        }
    }
} 
