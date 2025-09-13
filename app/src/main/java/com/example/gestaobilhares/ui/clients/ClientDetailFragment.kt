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
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
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
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.data.repository.MesaRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.AcertoMesaRepository
import com.example.gestaobilhares.data.repository.RotaRepository
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.DespesaRepository
import com.example.gestaobilhares.data.entities.StatusCicloAcerto

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
    
    // Repositórios para verificação de status da rota
    private lateinit var rotaRepository: RotaRepository
    private lateinit var cicloAcertoRepository: CicloAcertoRepository

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
        
        // Inicializar ViewModel e repositórios aqui onde o contexto está disponível
        val database = AppDatabase.getDatabase(requireContext())
        viewModel = ClientDetailViewModel(
            ClienteRepository(database.clienteDao()),
            MesaRepository(database.mesaDao()),
            AcertoRepository(database.acertoDao(), database.clienteDao()),
            AcertoMesaRepository(database.acertoMesaDao())
        )
        
        // Inicializar repositórios para verificação de rota
        rotaRepository = RotaRepository(database.rotaDao())
        cicloAcertoRepository = CicloAcertoRepository(
            database.cicloAcertoDao(),
            DespesaRepository(database.despesaDao()),
            AcertoRepository(database.acertoDao(), database.clienteDao()),
            ClienteRepository(database.clienteDao()),
            database.rotaDao()
        )
        
        // Inicializar views e configurar listeners
        setupRecyclerView()
        setupMesasSection()
        setupUI() // Movido para depois da inicialização das outras views
        observeViewModel()
        
        // Carregar dados do cliente apenas se não estiverem carregados
        if (viewModel.clientDetails.value == null) {
            viewModel.loadClientDetails(args.clienteId)
        }
        
        // ✅ CORREÇÃO OFICIAL: Usar SavedStateHandle para controlar o diálogo de observações
        // Baseado na documentação oficial do Android Navigation Component
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle
        val shouldShowObservations = savedStateHandle?.getStateFlow("show_observations_dialog", false)
        
        if (shouldShowObservations != null) {
            lifecycleScope.launch {
                shouldShowObservations.collect { shouldShow ->
                    if (shouldShow) {
                        Log.d("ClientDetailFragment", "Mostrando diálogo de observações via SavedStateHandle")
                        verificarObservacoesUltimoAcerto()
                        // Limpar o flag para evitar que apareça novamente
                        savedStateHandle["show_observations_dialog"] = false
                    }
                }
            }
        }
        
        // ✅ LEGACY: Manter compatibilidade com o parâmetro de navegação
        // Mas apenas se não estiver vindo de uma navegação interna
        val previousFragment = findNavController().previousBackStackEntry?.destination?.route
        Log.d("ClientDetailFragment", "Fragmento anterior: $previousFragment")
        
        if (args.mostrarDialogoObservacoes && previousFragment == "clientListFragment") {
            Log.d("ClientDetailFragment", "Verificando observações do último acerto - veio da tela de clientes da rota")
            verificarObservacoesUltimoAcerto()
        } else {
            Log.d("ClientDetailFragment", "Não mostrando diálogo de observações - não veio da tela de clientes da rota")
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
     * ✅ NOVO: Configura o FAB expandível com animações
     */
    private fun configurarFabExpandivel() {
        binding.fabMain.setOnClickListener {
            if (binding.fabExpandedContainer.visibility == View.VISIBLE) {
                recolherFabMenu()
            } else {
                expandirFabMenu()
            }
        }
        
        // Configurar clicks dos FABs expandidos
        binding.fabAddTableContainer.setOnClickListener {
            // ✅ IMPLEMENTADO: Navegar para tela Depósito Mesas
            try {
                val action = ClientDetailFragmentDirections
                    .actionClientDetailFragmentToMesasDepositoFragment(args.clienteId)
                findNavController().navigate(action)
                Log.d("ClientDetailFragment", "Navegando para Depósito Mesas - Cliente ID: ${args.clienteId}")
            } catch (e: Exception) {
                Log.e("ClientDetailFragment", "Erro ao navegar para Depósito Mesas: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao abrir Depósito Mesas: ${e.message}", Toast.LENGTH_LONG).show()
            }
            recolherFabMenu()
        }
        
        // ✅ NOVO: Botão Contrato
        binding.fabContractContainer.setOnClickListener {
            // Verificar se o cliente tem mesas vinculadas
            lifecycleScope.launch {
                try {
                    val mesasAtivas = viewModel.mesasCliente.value
                    if (mesasAtivas.isNotEmpty()) {
                        // Navegar para tela de geração de contrato
                        val mesasIds = mesasAtivas.map { it.id }.toLongArray()
                        val action = ClientDetailFragmentDirections
                            .actionClientDetailFragmentToContractGenerationFragment(
                                clienteId = args.clienteId,
                                mesasVinculadas = mesasIds
                            )
                        findNavController().navigate(action)
                        Log.d("ClientDetailFragment", "Navegando para Geração de Contrato - Cliente ID: ${args.clienteId} com ${mesasIds.size} mesas")
                    } else {
                        Toast.makeText(requireContext(), "Cliente não possui mesas vinculadas. Vincule uma mesa primeiro.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("ClientDetailFragment", "Erro ao verificar mesas para contrato: ${e.message}", e)
                    Toast.makeText(requireContext(), "Erro ao verificar mesas: ${e.message}", Toast.LENGTH_LONG).show()
                }
                recolherFabMenu()
            }
        }
        
        binding.fabNewSettlementContainer.setOnClickListener {
            // ✅ NOVO: Verificar se a rota está em andamento antes de permitir novo acerto
            lifecycleScope.launch {
                try {
                    // Obter dados do cliente para verificar a rota
                    val cliente = viewModel.clientDetails.value
                    if (cliente == null) {
                        Toast.makeText(requireContext(), "Erro: dados do cliente não carregados.", Toast.LENGTH_LONG).show()
                        recolherFabMenu()
                        return@launch
                    }
                    
                    // Verificar se existe ciclo em andamento para a rota do cliente
                    val rotaId = viewModel.buscarRotaIdPorCliente(cliente.id)
                    if (rotaId == null) {
                        Toast.makeText(requireContext(), "Erro: não foi possível obter a rota do cliente.", Toast.LENGTH_LONG).show()
                        recolherFabMenu()
                        return@launch
                    }
                    
                    val cicloEmAndamento = cicloAcertoRepository.buscarCicloAtivo(rotaId)
                    
                    if (cicloEmAndamento == null || cicloEmAndamento.status != StatusCicloAcerto.EM_ANDAMENTO) {
                        // Mostrar diálogo de rota não iniciada
                        mostrarAlertaRotaNaoIniciada()
                        recolherFabMenu()
                        return@launch
                    }
                    
                    // Verificar se há mesas ativas
                    val mesasAtivas = viewModel.mesasCliente.value
                    Log.d("NovoAcerto", "Mesas ativas encontradas: ${mesasAtivas.size}")
                    
                    if (mesasAtivas.isEmpty()) {
                        Toast.makeText(requireContext(), "Este cliente não possui mesas ativas para acerto.", Toast.LENGTH_LONG).show()
                        recolherFabMenu()
                        return@launch
                    }
                    
                    // Obter dados do cliente para incluir nos MesaDTOs
                    val valorFicha = cliente.valorFicha ?: 0.0
                    val comissaoFicha = cliente.comissaoFicha ?: 0.0
                    
                    val mesasDTO = mesasAtivas.map { mesa ->
                        Log.d("NovoAcerto", "Convertendo mesa ${mesa.numero} - ID: ${mesa.id}")
                        MesaDTO(
                            id = mesa.id,
                            numero = mesa.numero,
                            fichasInicial = mesa.fichasInicial ?: 0,
                            fichasFinal = mesa.fichasFinal ?: 0,
                            tipoMesa = mesa.tipoMesa,
                            ativa = mesa.ativa,
                            valorFixo = mesa.valorFixo ?: 0.0,
                            valorFicha = valorFicha,
                            comissaoFicha = comissaoFicha
                        )
                    }.toTypedArray()
                    
                    Log.d("NovoAcerto", "Enviando ${mesasDTO.size} mesas para SettlementFragment")
                    Log.d("NovoAcerto", "Mesas: ${mesasDTO.joinToString { "Mesa ${it.numero} (ID: ${it.id})" }}")
                    Log.d("NovoAcerto", "Valor Ficha: $valorFicha, Comissão Ficha: $comissaoFicha")
                    
                    // Navegar para tela de acerto
                    val action = ClientDetailFragmentDirections
                        .actionClientDetailFragmentToSettlementFragment(
                            clienteId = args.clienteId,
                            mesasDTO = mesasDTO
                        )
                    findNavController().navigate(action)
                    Log.d("ClientDetailFragment", "Navegando para Novo Acerto - Cliente ID: ${args.clienteId} com ${mesasDTO.size} mesas")
                    
                } catch (e: Exception) {
                    Log.e("ClientDetailFragment", "Erro ao verificar status da rota: ${e.message}", e)
                    Toast.makeText(requireContext(), "Erro ao verificar status da rota: ${e.message}", Toast.LENGTH_LONG).show()
                }
                recolherFabMenu()
            }
        }
    }
    
    /**
     * ✅ NOVO: Mostra alerta quando a rota não está em andamento
     */
    private fun mostrarAlertaRotaNaoIniciada() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Novo Acerto Não Permitido")
            .setMessage("Para realizar acertos, a rota deve estar com status 'Em Andamento'. Inicie a rota primeiro na tela de clientes.")
            .setPositiveButton("Entendi", null)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setCancelable(true)
            .show()
    }
    
    /**
     * ✅ NOVO: Expande o menu FAB com animações
     */
    private fun expandirFabMenu() {
        binding.fabExpandedContainer.visibility = View.VISIBLE
        
        // Animar FAB principal
        binding.fabMain.animate()
            .rotation(45f)
            .setDuration(200)
            .start()
        
        // Animar FABs expandidos
        binding.fabAddTableContainer.animate()
            .alpha(1f)
            .translationY(-16f)
            .setDuration(200)
            .start()
        
        // ✅ NOVO: Animar botão Contrato
        binding.fabContractContainer.animate()
            .alpha(1f)
            .translationY(-32f)
            .setDuration(200)
            .setStartDelay(25)
            .start()
        
        binding.fabNewSettlementContainer.animate()
            .alpha(1f)
            .translationY(-48f)
            .setDuration(200)
            .setStartDelay(50)
            .start()
    }
    
    /**
     * ✅ NOVO: Recolhe o menu FAB com animações
     */
    private fun recolherFabMenu() {
        // Verificar se o binding ainda é válido
        val currentBinding = _binding
        if (currentBinding == null) {
            Log.d("ClientDetailFragment", "Binding é nulo, ignorando animação")
            return
        }
        
        // Animar FAB principal
        currentBinding.fabMain.animate()
            .rotation(0f)
            .setDuration(200)
            .start()
        
        // Animar FABs expandidos
        currentBinding.fabAddTableContainer.animate()
            .alpha(0f)
            .translationY(0f)
            .setDuration(200)
            .start()
        
        // ✅ NOVO: Animar botão Contrato
        currentBinding.fabContractContainer.animate()
            .alpha(0f)
            .translationY(0f)
            .setDuration(200)
            .start()
        
        currentBinding.fabNewSettlementContainer.animate()
            .alpha(0f)
            .translationY(0f)
            .setDuration(200)
            .withEndAction {
                // Verificar novamente se o binding ainda é válido
                _binding?.fabExpandedContainer?.visibility = View.GONE
            }
            .start()
    }

    /**
     * ✅ NOVO: Verifica se há observações no último acerto e mostra diálogo se necessário
     */
    private fun verificarObservacoesUltimoAcerto() {
        lifecycleScope.launch {
            try {
                val ultimoAcerto = viewModel.buscarUltimoAcerto(args.clienteId)
                ultimoAcerto?.let { acerto ->
                    if (!acerto.observacoes.isNullOrBlank()) {
                        Log.d("ClientDetailFragment", "Observações encontradas no último acerto: ${acerto.observacoes}")
                        mostrarDialogoObservacoes(acerto.observacoes)
                    } else {
                        Log.d("ClientDetailFragment", "Nenhuma observação no último acerto")
                    }
                }
            } catch (e: Exception) {
                Log.e("ClientDetailFragment", "Erro ao verificar observações do último acerto: ${e.message}", e)
            }
        }
    }
    
    /**
     * ✅ NOVO: Mostra diálogo com observações do último acerto
     */
    private fun mostrarDialogoObservacoes(observacoes: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Observação:")
            .setMessage(observacoes)
            .setPositiveButton("Confirmar Leitura") { dialog, _ ->
                dialog.dismiss()
                Log.d("ClientDetailFragment", "Diálogo de observações fechado pelo usuário")
            }
            .setCancelable(false)
            .show()
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
        
        // Botão editar cliente
        binding.fabEdit.setOnClickListener {
            val action = ClientDetailFragmentDirections
                .actionClientDetailFragmentToClientRegisterFragment(
                    rotaId = 0L, // Não precisa da rota para edição
                    clienteId = args.clienteId
                )
            findNavController().navigate(action)
        }
        
        // ✅ NOVO: Configurar FAB expandível
        configurarFabExpandivel()
        
        // ✅ NOVO: Configurar visibilidade inicial do FAB expandido
        binding.fabExpandedContainer.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        settlementHistoryAdapter = SettlementHistoryAdapter { acerto ->
            // ✅ CORREÇÃO: Sempre navegar para detalhes do acerto, independente do status
            val action = ClientDetailFragmentDirections.actionClientDetailFragmentToSettlementDetailFragment(acerto.id)
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
                                        tipoMesa = mesa.tipoMesa,
                                        ativa = mesa.ativa,
                                        valorFixo = mesa.valorFixo ?: 0.0,
                                        valorFicha = valorFicha,
                                        comissaoFicha = comissaoFicha
                                    )
                                }.toTypedArray()
                                
                                val action = ClientDetailFragmentDirections.actionClientDetailFragmentToSettlementFragment(
                                    clienteId = args.clienteId,
                                    mesasDTO = mesasDTO
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Cliente
                launch {
                    viewModel.clientDetails.collect { client ->
                        client?.let { updateClientUI(it) }
                    }
                }
                // Histórico de acertos
                launch {
                    viewModel.settlementHistory.collect { settlements ->
                        Log.d("ClientDetailFragment", "=== HISTÓRICO ATUALIZADO ===")
                        Log.d("ClientDetailFragment", "Quantidade de acertos: ${settlements.size}")
                        settlements.forEachIndexed { index, acerto ->
                            Log.d("ClientDetailFragment", "Acerto $index: ID=${acerto.id}, Data=${acerto.data}, Valor=${acerto.valorTotal}, Status=${acerto.status}")
                        }
                        settlementHistoryAdapter.submitList(settlements)
                    }
                }
                // Mesas do cliente
                launch {
                    viewModel.mesasCliente.collect { mesas ->
                        Log.d("ClientDetailFragment", "=== MESAS ATUALIZADAS ===")
                        Log.d("ClientDetailFragment", "Total de mesas: ${mesas.size}")
                        val mesasAtivas = mesas.filter { it.ativa }
                        Log.d("ClientDetailFragment", "Mesas ativas: ${mesasAtivas.size}")
                        _binding?.let { b ->
                            b.tvTotalMesasAtivas.text = mesasAtivas.size.toString()
                        }
                        mesasAdapter.submitList(mesas)
                    }
                }
            }
        }
    }

    private fun updateClientUI(client: ClienteResumo) {
        binding.apply {
            tvClientName.text = client.nome
            tvClientAddress.text = client.endereco
            tvClientAddress.setOnClickListener {
                try {
                    val lat = client.latitude
                    val lon = client.longitude
                    if (lat != null && lon != null) {
                        val uri = android.net.Uri.parse("google.navigation:q=$lat,$lon")
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        intent.setPackage("com.google.android.apps.maps")
                        startActivity(intent)
                    } else {
                        // Fallback: abrir busca por endereço
                        val uri = android.net.Uri.parse("geo:0,0?q=" + java.net.URLEncoder.encode(client.endereco, "UTF-8"))
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(requireContext(), "Não foi possível abrir o Maps", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            tvLastVisit.text = client.ultimaVisita
            
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
     * ✅ SOLUÇÃO DEFINITIVA: Abre WhatsApp diretamente com o número do cliente
     * Baseado na documentação oficial WhatsApp e Android Intents
     * ELIMINA COMPLETAMENTE o seletor de apps
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
            
            // ✅ ESTRATÉGIA 1: Esquema nativo whatsapp://send (FORÇA direcionamento direto)
            try {
                val uri = android.net.Uri.parse("whatsapp://send?phone=$numeroCompleto&text=${android.net.Uri.encode(mensagem)}")
                val intentWhatsApp = Intent(Intent.ACTION_VIEW, uri).apply {
                    // ✅ CRÍTICO: Força o direcionamento direto sem seletor
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                startActivity(intentWhatsApp)
                Log.d("ClientDetailFragment", "✅ WhatsApp aberto diretamente via esquema nativo")
                return
            } catch (e: Exception) {
                Log.d("ClientDetailFragment", "Esquema nativo não funcionou: ${e.message}")
            }
            
            // ✅ ESTRATÉGIA 2: URL wa.me (funciona mesmo sem app instalado)
            try {
                val url = "https://wa.me/$numeroCompleto?text=${android.net.Uri.encode(mensagem)}"
                val intentUrl = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(url)
                    // ✅ CRÍTICO: Força o direcionamento direto
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                startActivity(intentUrl)
                Log.d("ClientDetailFragment", "✅ WhatsApp aberto via URL wa.me")
                return
            } catch (e: Exception) {
                Log.d("ClientDetailFragment", "URL wa.me não funcionou: ${e.message}")
            }
            
            // ✅ ESTRATÉGIA 3: Tentar WhatsApp Business via esquema nativo
            try {
                val uri = android.net.Uri.parse("whatsapp://send?phone=$numeroCompleto&text=${android.net.Uri.encode(mensagem)}")
                val intentBusiness = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp.w4b")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                startActivity(intentBusiness)
                Log.d("ClientDetailFragment", "✅ WhatsApp Business aberto via esquema nativo")
                return
            } catch (e: Exception) {
                Log.d("ClientDetailFragment", "WhatsApp Business não disponível: ${e.message}")
            }
            
            // ✅ ESTRATÉGIA 4: Intent direto com ACTION_SEND mas SEM chooser
            try {
                val intentDirect = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, mensagem)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                startActivity(intentDirect)
                Log.d("ClientDetailFragment", "✅ WhatsApp aberto via intent direto")
                return
            } catch (e: Exception) {
                Log.d("ClientDetailFragment", "Intent direto falhou: ${e.message}")
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
