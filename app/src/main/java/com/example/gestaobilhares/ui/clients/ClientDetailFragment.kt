package com.example.gestaobilhares.ui.clients

import android.Manifest
import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.databinding.FragmentClientDetailBinding
import com.example.gestaobilhares.ui.clients.MesasAdapter
import com.example.gestaobilhares.ui.clients.SettlementHistoryAdapter
import com.google.android.material.datepicker.MaterialDatePicker
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch
import com.example.gestaobilhares.data.repository.AppRepository
import android.util.Log
import com.example.gestaobilhares.data.factory.RepositoryFactory
import com.example.gestaobilhares.ui.dialogs.AdicionarMesaDialogFragment
import com.example.gestaobilhares.ui.dialogs.AdicionarObservacaoDialogFragment
import com.example.gestaobilhares.ui.dialogs.ConfirmarRetiradaMesaDialogFragment
import com.example.gestaobilhares.ui.dialogs.GerarRelatorioDialogFragment
import com.example.gestaobilhares.ui.dialogs.RotaNaoIniciadaDialogFragment
import kotlinx.coroutines.flow.first
import com.example.gestaobilhares.ui.clients.RetiradaStatus
import com.example.gestaobilhares.data.entities.ContratoLocacao

class ClientDetailFragment : Fragment(), ConfirmarRetiradaMesaDialogFragment.ConfirmarRetiradaDialogListener, AdicionarObservacaoDialogFragment.AdicionarObservacaoDialogListener, GerarRelatorioDialogFragment.GerarRelatorioDialogListener {

    private var _binding: FragmentClientDetailBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null. Fragment may be destroyed.")
    private val args: ClientDetailFragmentArgs by navArgs()
    private lateinit var viewModel: ClientDetailViewModel
    private lateinit var mesasAdapter: MesasAdapter
    private lateinit var historicoAdapter: SettlementHistoryAdapter
    private var isFabMenuOpen = false
    private lateinit var appRepository: AppRepository
    private var mesaParaRemover: Mesa? = null
    private val clientId: Long get() = args.clienteId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appRepository = RepositoryFactory.getAppRepository(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientDetailBinding.inflate(inflater, container, false)

        val clientId = args.clienteId
        val userSessionManager = com.example.gestaobilhares.utils.UserSessionManager.getInstance(requireContext())
        viewModel = ClientDetailViewModel(appRepository, userSessionManager)
        setupRecyclerView()
        observeViewModel()
        setupListeners(clientId)
        
        // ✅ NOVO: Configurar botão de retorno para sempre voltar para ClientListFragment
        setupBackButtonHandler()
        
        // Carregar dados do cliente
        viewModel.loadClientDetails(clientId)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Recarregar apenas o histórico de acertos quando retornar de outras telas
        viewModel.loadSettlementHistory(args.clienteId)
    }

    private fun setupRecyclerView() {
        mesasAdapter = MesasAdapter(
            onRetirarMesa = { mesa ->
                // Verificar se a mesa pode ser retirada (se foi acertada hoje)
                lifecycleScope.launch {
                    val statusRetirada = viewModel.verificarSeRetiradaEPermitida(mesa.id, args.clienteId)
                    when (statusRetirada) {
                        RetiradaStatus.PODE_RETIRAR -> {
                            // Mesa foi acertada hoje - pode retirar
                            mesaParaRemover = mesa
                            ConfirmarRetiradaMesaDialogFragment.newInstance().show(childFragmentManager, ConfirmarRetiradaMesaDialogFragment.TAG)
                        }
                        RetiradaStatus.PRECISA_ACERTO -> {
                            // Mesa não foi acertada hoje - precisa acertar primeiro
                            Toast.makeText(
                                requireContext(),
                                "Esta mesa precisa ser acertada antes de ser removida. Faça um novo acerto primeiro.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        )
        binding.rvMesasCliente.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = mesasAdapter
        }

        historicoAdapter = SettlementHistoryAdapter { acerto ->
            val action = ClientDetailFragmentDirections.actionClientDetailFragmentToSettlementDetailFragment(acerto.id)
            findNavController().navigate(action)
        }
        binding.rvSettlementHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historicoAdapter
        }
        
        Log.d("ClientDetailFragment", "RecyclerViews configurados - Mesas e Histórico")
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.cliente.collect { cliente ->
                        cliente?.let { updateClientUI(it) }
                    }
                }
                launch {
                    viewModel.mesasCliente.collect { mesas ->
                        mesasAdapter.submitList(mesas)
                        binding.tvTotalMesasAtivas.text = mesas.size.toString()
                    }
                }
                launch {
                    viewModel.settlementHistory.collect { historico ->
                        Log.d("ClientDetailFragment", "=== HISTÓRICO RECEBIDO ===")
                        Log.d("ClientDetailFragment", "Quantidade de acertos: ${historico.size}")
                        Log.d("ClientDetailFragment", "Adapter inicializado? ${::historicoAdapter.isInitialized}")
                        historico.forEachIndexed { index, acerto ->
                            Log.d("ClientDetailFragment", "Acerto $index: ID=${acerto.id}, Data=${acerto.data}, Valor=${acerto.valorTotal}")
                        }
                        if (::historicoAdapter.isInitialized) {
                            historicoAdapter.submitList(historico)
                            Log.d("ClientDetailFragment", "Lista enviada para o adapter")
                            // Garantir que o card e a lista fiquem visíveis quando houver dados
                            if (historico.isNotEmpty()) {
                                binding.rvSettlementHistory.visibility = View.VISIBLE
                            }
                        } else {
                            Log.e("ClientDetailFragment", "Adapter não inicializado!")
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners(clientId: Long) {
        binding.fabMain.setOnClickListener { toggleFabMenu() }

        binding.fabEdit.setOnClickListener {
            Log.d("ClientDetailFragment", "=== NAVEGAÇÃO PARA EDIÇÃO ===")
            Log.d("ClientDetailFragment", "clientId sendo passado: $clientId")
            
            // ✅ CORREÇÃO CRÍTICA: Buscar rotaId diretamente do banco para evitar fallback hardcoded
            // Se o usuário clicar antes de loadClientDetails completar, viewModel.cliente.value será null
            lifecycleScope.launch {
                try {
                    val rotaId = viewModel.buscarRotaIdPorCliente(clientId)
                    if (rotaId == null) {
                        Toast.makeText(requireContext(), "Erro: Cliente não encontrado ou não associado a uma rota.", Toast.LENGTH_SHORT).show()
                        recolherFabMenu()
                        return@launch
                    }
                    
                    val action = ClientDetailFragmentDirections.actionClientDetailFragmentToClientRegisterFragment(
                        rotaId = rotaId,
                        clienteId = clientId
                    )
                    Log.d("ClientDetailFragment", "Action criada com rotaId: $rotaId, clienteId: $clientId")
                    findNavController().navigate(action)
                    recolherFabMenu()
                } catch (e: Exception) {
                    Log.e("ClientDetailFragment", "Erro ao buscar rotaId do cliente: ${e.message}", e)
                    Toast.makeText(requireContext(), "Erro ao carregar dados do cliente.", Toast.LENGTH_SHORT).show()
                    recolherFabMenu()
                }
            }
        }

        binding.fabAddTableContainer.setOnClickListener {
            val action = ClientDetailFragmentDirections.actionClientDetailFragmentToMesasDepositoFragment(
                clienteId = clientId,
                isFromGerenciarMesas = false,
                isFromClientRegister = false
            )
            findNavController().navigate(action)
            recolherFabMenu()
        }

        binding.fabNewSettlementContainer.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val rotaId = viewModel.buscarRotaIdPorCliente(clientId) ?: -1L
                    if (rotaId == -1L) {
                        Toast.makeText(requireContext(), "Cliente não associado a uma rota.", Toast.LENGTH_SHORT).show()
                        recolherFabMenu()
                        return@launch
                    }

                    val cicloEmAndamento = viewModel.buscarCicloAtualPorRota(rotaId)
                    if (cicloEmAndamento == null || !cicloEmAndamento.estaEmAndamento) {
                        RotaNaoIniciadaDialogFragment().show(childFragmentManager, RotaNaoIniciadaDialogFragment.TAG)
                        recolherFabMenu()
                        return@launch
                    }

                    val mesasCliente = viewModel.mesasCliente.first()
                    val mesasDTO = mesasCliente.map { mesa ->
                        com.example.gestaobilhares.ui.settlement.MesaDTO(
                            id = mesa.id,
                            numero = mesa.numero,
                            relogioInicial = mesa.relogioInicial,
                            relogioFinal = mesa.relogioFinal,
                            tipoMesa = mesa.tipoMesa,
                            tamanho = mesa.tamanho,
                            estadoConservacao = mesa.estadoConservacao,
                            ativa = mesa.ativa,
                            valorFixo = mesa.valorFixo,
                            valorFicha = 0.0,
                            comissaoFicha = 0.0
                        )
                    }.toTypedArray()

                    val action = ClientDetailFragmentDirections.actionClientDetailFragmentToSettlementFragment(
                        clienteId = clientId,
                        acertoIdParaEdicao = 0L,
                        mesasDTO = mesasDTO
                    )
                    findNavController().navigate(action)
                    recolherFabMenu()

                } catch (e: Exception) {
                    Log.e("ClientDetailFragment", "Erro ao iniciar novo acerto: ${e.message}")
                    Toast.makeText(requireContext(), "Erro ao iniciar novo acerto.", Toast.LENGTH_SHORT).show()
                    recolherFabMenu()
                }
            }
        }

        binding.fabContractContainer.setOnClickListener {
            // Verificar se o cliente já tem contrato ativo
            lifecycleScope.launch {
                viewModel.verificarContratoAtivo(clientId)
                val temContratoAtivo = viewModel.temContratoAtivo.first()
                
                if (temContratoAtivo) {
                    Toast.makeText(
                        requireContext(),
                        "Este cliente já possui um contrato ativo. Não é possível gerar um novo contrato.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Navegar para ContractGenerationFragment com parâmetros corretos
                    val bundle = android.os.Bundle().apply {
                        putLong("cliente_id", clientId)
                        putLongArray("mesas_vinculadas", longArrayOf()) // Array vazio para novo contrato
                        putBoolean("tipo_fixo", false) // Padrão: fichas jogadas
                        putDouble("valor_fixo", 0.0) // Padrão: 0.0
                    }
                    findNavController().navigate(
                        com.example.gestaobilhares.R.id.contractGenerationFragment,
                        bundle
                    )
                }
                recolherFabMenu()
            }
        }

        binding.fabWhatsApp.setOnClickListener {
            abrirWhatsApp()
            recolherFabMenu()
        }

        // ✅ NOVO: Listener para ícone de localização
        binding.ivLocationIcon.setOnClickListener {
            abrirNavegacaoParaLocalizacao()
        }
    }

    private fun updateClientUI(cliente: Cliente) {
        binding.tvClientName.text = cliente.nome
        binding.tvClientAddress.text = cliente.endereco ?: ""
        val formattedDebt = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(cliente.debitoAtual)
        binding.tvClientCurrentDebt.text = formattedDebt
        binding.tvLastVisit.text = "N/A"
    }

    private fun toggleFabMenu() {
        isFabMenuOpen = !isFabMenuOpen
        if (isFabMenuOpen) {
            expandirFabMenu()
        } else {
            recolherFabMenu()
        }
    }

    private fun expandirFabMenu() {
        if (_binding == null) return // Verificar se o binding ainda existe
        
        ObjectAnimator.ofFloat(binding.fabMain, "rotation", 0f, 45f).setDuration(300).start()
        binding.fabExpandedContainer.visibility = View.VISIBLE
        animateFabContainer(binding.fabAddTableContainer, 0, 50)
        animateFabContainer(binding.fabContractContainer, 1, 100)
        animateFabContainer(binding.fabNewSettlementContainer, 2, 150)
    }

    private fun recolherFabMenu() {
        if (_binding == null) return // Verificar se o binding ainda existe
        
        ObjectAnimator.ofFloat(binding.fabMain, "rotation", 45f, 0f).setDuration(300).start()
        animateFabContainer(binding.fabNewSettlementContainer, 2, 150, false)
        animateFabContainer(binding.fabContractContainer, 1, 100, false)
        animateFabContainer(binding.fabAddTableContainer, 0, 50, false) {
            if (_binding != null) { // Verificar novamente antes de acessar binding
                binding.fabExpandedContainer.visibility = View.GONE
            }
        }
    }

    private fun animateFabContainer(container: View, _index: Int, startDelay: Long, show: Boolean = true, onEndAction: (() -> Unit)? = null) {
        if (_binding == null) return // Verificar se o binding ainda existe
        
        val translationY = if (show) 0f else 100f
        val alpha = if (show) 1f else 0f
        container.animate()
            .translationY(translationY)
            .alpha(alpha)
            .setDuration(200)
            .setStartDelay(startDelay)
            .withEndAction { 
                if (_binding != null) { // Verificar novamente antes de executar callback
                    onEndAction?.invoke() 
                }
            }
            .start()
    }


    override fun onDialogPositiveClick(dialog: DialogFragment) {
        mesaParaRemover?.let { mesa ->
            lifecycleScope.launch {
                try {
                    // Verificar quantas mesas o cliente tem antes de retirar
                    val mesasAtuais = viewModel.mesasCliente.first()
                    val totalMesas = mesasAtuais.size
                    
                    // Retirar mesa do cliente
                    viewModel.retirarMesaDoCliente(mesa.id, args.clienteId, relogioFinal = mesa.relogioFinal, valorRecebido = 0.0)
                    
                    // Verificar se cliente tem contrato ativo
                    val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
                    val contratoAtivo = appRepository.buscarContratoAtivoPorCliente(args.clienteId)
                    
                    if (contratoAtivo != null) {
                        if (totalMesas > 1) {
                            // Cliente tem 2+ mesas: gerar ADITIVO de retirada
                            gerarAditivoAposRetiradaMesa(contratoAtivo, mesa)
                        } else {
                            // Cliente tem apenas 1 mesa: gerar DISTRATO
                            gerarDistratoAposRetiradaMesa(contratoAtivo, mesa)
                        }
                    }
                    
                    mesaParaRemover = null
                } catch (e: Exception) {
                    Log.e("ClientDetailFragment", "Erro ao retirar mesa e gerar documento", e)
                    Toast.makeText(requireContext(), "Erro ao processar retirada da mesa: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onGerarRelatorioUltimoAcerto() {
        Toast.makeText(requireContext(), "Relatório de acerto será habilitado em seguida.", Toast.LENGTH_SHORT).show()
    }

    override fun onGerarRelatorioAnual() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecione o Ano")
            .build()
        datePicker.addOnPositiveButtonClickListener {
            Toast.makeText(requireContext(), "Relatório anual será habilitado em seguida.", Toast.LENGTH_SHORT).show()
        }
        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    override fun onObservacaoAdicionada(textoObservacao: String) {
        Log.d("ClientDetailFragment", "Observação adicionada: $textoObservacao")
    }

    private suspend fun gerarDistratoAposRetiradaMesa(contrato: ContratoLocacao, _mesaRemovida: Mesa) {
        try {
            // Buscar último acerto do cliente usando AppRepository
            val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
            // ✅ FASE 12.7: Variável removida (não utilizada)
            
            // Atualizar status do contrato para aguardando assinatura
            val agora = java.util.Date()
            val contratoAtualizado = contrato.copy(
                status = "AGUARDANDO_ASSINATURA_DISTRATO",
                dataAtualizacao = agora
            )
            appRepository.atualizarContrato(contratoAtualizado)
            
            // Navegar para tela de assinatura do distrato
            // Como não há ação direta, vamos usar navegação programática
            val bundle = android.os.Bundle().apply {
                putLong("contrato_id", contrato.id)
                putString("assinatura_contexto", "DISTRATO")
            }
            findNavController().navigate(
                com.example.gestaobilhares.R.id.signatureCaptureFragment,
                bundle
            )
            
            Toast.makeText(
                requireContext(),
                "Mesa retirada. Assine o distrato para finalizar.",
                Toast.LENGTH_LONG
            ).show()
            
        } catch (e: Exception) {
            Log.e("ClientDetailFragment", "Erro ao processar distrato", e)
            Toast.makeText(
                requireContext(),
                "Erro ao processar distrato: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private suspend fun gerarAditivoAposRetiradaMesa(contrato: ContratoLocacao, mesaRemovida: Mesa) {
        try {
            // Criar aditivo de retirada diretamente
            val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
            
            // Gerar número do aditivo
            val numeroAditivo = gerarNumeroAditivo()
            
            // Criar aditivo de retirada
            val aditivo = com.example.gestaobilhares.data.entities.AditivoContrato(
                numeroAditivo = numeroAditivo,
                contratoId = contrato.id,
                dataAditivo = java.util.Date(),
                observacoes = "Retirada da mesa ${mesaRemovida.numero}",
                tipo = "RETIRADA"
            )
            
            // Salvar aditivo
            val aditivoId = appRepository.inserirAditivo(aditivo)
            
            // Vincular mesa ao aditivo
            val aditivoMesa = com.example.gestaobilhares.data.entities.AditivoMesa(
                aditivoId = aditivoId,
                mesaId = mesaRemovida.id,
                tipoEquipamento = mesaRemovida.tipoMesa.name,
                numeroSerie = mesaRemovida.numero,
                valorFicha = 0.0,
                valorFixo = mesaRemovida.valorFixo
            )
            
            appRepository.inserirAditivoMesas(listOf(aditivoMesa))
            
            // Navegar para tela de assinatura do aditivo
            val bundle = android.os.Bundle().apply {
                putLong("contratoId", contrato.id)
                putString("aditivoTipo", "RETIRADA")
                putLongArray("mesasVinculadas", longArrayOf(mesaRemovida.id))
            }
            findNavController().navigate(
                com.example.gestaobilhares.R.id.aditivoSignatureFragment,
                bundle
            )
            
            Toast.makeText(
                requireContext(),
                "Mesa retirada. Assine o aditivo para finalizar.",
                Toast.LENGTH_LONG
            ).show()
            
        } catch (e: Exception) {
            Log.e("ClientDetailFragment", "Erro ao processar aditivo", e)
            Toast.makeText(
                requireContext(),
                "Erro ao processar aditivo: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private suspend fun gerarNumeroAditivo(): String {
        val ano = java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
        val numeroSequencial = appRepository.contarAditivosPorAno(ano) + 1
        return "ADT-$ano-${String.format("%04d", numeroSequencial)}"
    }

    private fun abrirWhatsApp() {
        val cliente = viewModel.cliente.value
        val telefone = cliente?.telefone
        if (!telefone.isNullOrBlank()) {
            val numeroLimpo = telefone.filter { it.isDigit() }
            val ddi = "55"
            val url = "https://api.whatsapp.com/send?phone=$ddi$numeroLimpo"
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Não foi possível abrir o WhatsApp.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Cliente não possui número de telefone cadastrado.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ✅ NOVO: Configura o botão de retorno para sempre voltar para ClientListFragment
     */
    private fun setupBackButtonHandler() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Sempre navegar para ClientListFragment, ignorando o stack atual
                // ✅ CORREÇÃO CRÍTICA: Buscar rotaId diretamente do banco para evitar fallback hardcoded
                lifecycleScope.launch {
                    try {
                        val rotaId = viewModel.buscarRotaIdPorCliente(clientId)
                        if (rotaId != null) {
                            val action = ClientDetailFragmentDirections
                                .actionClientDetailFragmentToClientListFragment(rotaId = rotaId)
                            findNavController().navigate(action)
                        } else {
                            // Se não encontrar rotaId, usar popUpTo para limpar o stack
                            findNavController().popBackStack(
                                com.example.gestaobilhares.R.id.clientListFragment, 
                                false
                            )
                        }
                    } catch (e: Exception) {
                        // Se a ação não existir, usar popUpTo para limpar o stack
                        findNavController().popBackStack(
                            com.example.gestaobilhares.R.id.clientListFragment, 
                            false
                        )
                    }
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    /**
     * ✅ NOVO: Abre aplicativos de navegação com as coordenadas do cliente
     * Baseado no padrão de implementação robusta usado no projeto
     */
    private fun abrirNavegacaoParaLocalizacao() {
        val cliente = viewModel.cliente.value
        val latitude = cliente?.latitude
        val longitude = cliente?.longitude
        
        if (latitude != null && longitude != null) {
            try {
                // ✅ ESTRATÉGIA 1: Google Maps (esquema nativo)
                try {
                    val uri = Uri.parse("google.navigation:q=$latitude,$longitude")
                    val intentMaps = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intentMaps)
                    android.util.Log.d("ClientDetailFragment", "✅ Google Maps aberto via esquema nativo")
                    return
                } catch (e: Exception) {
                    android.util.Log.d("ClientDetailFragment", "Google Maps nativo não funcionou: ${e.message}")
                }
                
                // ✅ ESTRATÉGIA 2: Geo URI (funciona com qualquer app de mapas)
                try {
                    val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
                    val intentGeo = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intentGeo)
                    android.util.Log.d("ClientDetailFragment", "✅ App de mapas aberto via geo URI")
                    return
                } catch (e: Exception) {
                    android.util.Log.d("ClientDetailFragment", "Geo URI não funcionou: ${e.message}")
                }
                
                // ✅ ESTRATÉGIA 3: Google Maps via URL web
                try {
                    val url = "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude"
                    val intentWeb = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(url)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intentWeb)
                    android.util.Log.d("ClientDetailFragment", "✅ Google Maps aberto via URL web")
                    return
                } catch (e: Exception) {
                    android.util.Log.d("ClientDetailFragment", "URL web não funcionou: ${e.message}")
                }
                
                // ✅ ESTRATÉGIA 4: Waze (se disponível)
                try {
                    val uri = Uri.parse("waze://?ll=$latitude,$longitude&navigate=yes")
                    val intentWaze = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.waze")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intentWaze)
                    android.util.Log.d("ClientDetailFragment", "✅ Waze aberto via esquema nativo")
                    return
                } catch (e: Exception) {
                    android.util.Log.d("ClientDetailFragment", "Waze não disponível: ${e.message}")
                }
                
                // ✅ ÚLTIMA OPÇÃO: Mostrar mensagem de erro
                Toast.makeText(requireContext(), "Não foi possível abrir aplicativo de navegação. Verifique se há algum app de mapas instalado.", Toast.LENGTH_LONG).show()
                android.util.Log.e("ClientDetailFragment", "❌ Todas as estratégias de navegação falharam")
                
            } catch (e: Exception) {
                android.util.Log.e("ClientDetailFragment", "Erro geral ao abrir navegação: ${e.message}")
                Toast.makeText(requireContext(), "Erro ao abrir navegação: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Cliente não possui localização cadastrada.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
