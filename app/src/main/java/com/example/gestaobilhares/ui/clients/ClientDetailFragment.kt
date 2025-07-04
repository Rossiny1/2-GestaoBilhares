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
// Hilt removido - usando instanciação direta
import kotlinx.coroutines.launch
import com.example.gestaobilhares.R
import android.util.Log
import com.example.gestaobilhares.ui.settlement.MesaDTO
import android.widget.Toast

import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repositories.ClienteRepository
import com.example.gestaobilhares.data.repository.MesaRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.AcertoMesaRepository

/**
 * Fragment para exibir detalhes do cliente e histórico de acertos
 * FASE 4A - Implementação crítica para desbloqueio do fluxo
 */
class ClientDetailFragment : Fragment() {

    private var _binding: FragmentClientDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ClientDetailViewModel
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
        
        // Inicializar ViewModel aqui onde o contexto está disponível
        viewModel = ClientDetailViewModel(
            ClienteRepository(AppDatabase.getDatabase(requireContext()).clienteDao()),
            MesaRepository(AppDatabase.getDatabase(requireContext()).mesaDao()),
            AcertoRepository(AppDatabase.getDatabase(requireContext()).acertoDao()),
            AcertoMesaRepository(AppDatabase.getDatabase(requireContext()).acertoMesaDao())
        )
        
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
     * ✅ SIMPLIFICADO: Diálogo simples de confirmação para retirada de mesa
     */
    private fun mostrarDialogoRetiradaComRelogioFinal(mesa: Mesa) {
        lifecycleScope.launch {
            try {
                // Buscar o relógio final do último acerto da mesa (usado internamente)
                val relogioFinalUltimoAcerto = viewModel.buscarRelogioFinalUltimoAcerto(mesa.id)
                val relogioFinal = relogioFinalUltimoAcerto ?: mesa.relogioFinal ?: mesa.fichasFinal ?: 0
                
                // ✅ NOVO: Diálogo simples de confirmação
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Retirar Mesa")
                    .setMessage("Você tem certeza que deseja retirar essa mesa?")
                    .setPositiveButton("Confirmar") { _, _ ->
                        // Usar o relógio final do último acerto automaticamente
                        viewModel.retirarMesaDoCliente(mesa.id, args.clienteId, relogioFinal, 0.0)
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
            tvLastVisit.text = client.ultimaVisita
            tvActiveTables.text = client.mesasAtivas.toString()
            
            // ✅ CORREÇÃO CRÍTICA: Exibir débito atual sincronizado
            val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
            if (client.debitoAtual <= 0) {
                tvClientCurrentDebt.text = "Sem Débito"
                tvClientCurrentDebt.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
            } else {
                tvClientCurrentDebt.text = formatter.format(client.debitoAtual)
                // Cores baseadas no valor do débito
                val debtColor = when {
                    client.debitoAtual > 300.0 -> requireContext().getColor(com.example.gestaobilhares.R.color.red_600)
                    client.debitoAtual > 100.0 -> requireContext().getColor(com.example.gestaobilhares.R.color.orange_500)
                    else -> requireContext().getColor(com.example.gestaobilhares.R.color.purple_600)
                }
                tvClientCurrentDebt.setTextColor(debtColor)
            }
            Log.d("ClientDetailFragment", "Débito atual exibido na tela: R$ ${client.debitoAtual}")
            
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
     * Baseado na documentação oficial Android e WhatsApp Business API
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
            val numeroCompleto = if (numeroLimpo.length == 11) {
                "55$numeroLimpo" // Adiciona código do Brasil
            } else if (numeroLimpo.length == 10) {
                "55$numeroLimpo" // Adiciona código do Brasil
            } else {
                numeroLimpo
            }
            
            Log.d("ClientDetailFragment", "Número original: $telefone")
            Log.d("ClientDetailFragment", "Número limpo: $numeroLimpo")
            Log.d("ClientDetailFragment", "Número completo: $numeroCompleto")
            
            // ✅ CORREÇÃO: Mensagem padrão para contato
            val mensagem = "Olá! Entro em contato sobre suas mesas de bilhar."
            
            // ✅ ESTRATÉGIA 1: Tentar WhatsApp nativo primeiro
            try {
                val intentWhatsApp = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, mensagem)
                    setPackage("com.whatsapp")
                }
                
                if (intentWhatsApp.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intentWhatsApp)
                    Log.d("ClientDetailFragment", "✅ WhatsApp nativo aberto com sucesso")
                    return
                }
            } catch (e: Exception) {
                Log.d("ClientDetailFragment", "WhatsApp nativo não disponível: ${e.message}")
            }
            
            // ✅ ESTRATÉGIA 2: Tentar WhatsApp Business
            try {
                val intentBusiness = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, mensagem)
                    setPackage("com.whatsapp.w4b")
                }
                
                if (intentBusiness.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intentBusiness)
                    Log.d("ClientDetailFragment", "✅ WhatsApp Business aberto com sucesso")
                    return
                }
            } catch (e: Exception) {
                Log.d("ClientDetailFragment", "WhatsApp Business não disponível: ${e.message}")
            }
            
            // ✅ ESTRATÉGIA 3: Usar URL wa.me (funciona mesmo sem app instalado)
            try {
                val url = "https://wa.me/$numeroCompleto?text=${android.net.Uri.encode(mensagem)}"
                val intentUrl = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(url)
                }
                
                if (intentUrl.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(intentUrl)
                    Log.d("ClientDetailFragment", "✅ WhatsApp aberto via URL")
                    return
                }
            } catch (e: Exception) {
                Log.d("ClientDetailFragment", "URL wa.me não funcionou: ${e.message}")
            }
            
            // ✅ ESTRATÉGIA 4: Compartilhamento genérico
            try {
                val intentGeneric = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "$mensagem\n\nContato: $telefone")
                }
                
                val chooser = Intent.createChooser(intentGeneric, "Compartilhar via WhatsApp")
                startActivity(chooser)
                Log.d("ClientDetailFragment", "✅ Compartilhamento genérico aberto")
                return
            } catch (e: Exception) {
                Log.d("ClientDetailFragment", "Compartilhamento genérico falhou: ${e.message}")
            }
            
            // ✅ ÚLTIMA OPÇÃO: Mostrar mensagem de erro
            Toast.makeText(requireContext(), "Não foi possível abrir o WhatsApp. Verifique se está instalado.", Toast.LENGTH_LONG).show()
            Log.e("ClientDetailFragment", "❌ Todas as estratégias falharam")
            
        } catch (e: Exception) {
            Log.e("ClientDetailFragment", "Erro geral ao abrir WhatsApp: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao abrir WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
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
