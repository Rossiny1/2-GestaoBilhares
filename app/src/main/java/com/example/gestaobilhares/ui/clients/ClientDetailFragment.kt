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
        viewModel = ClientDetailViewModel(appRepository)
        setupRecyclerView()
        observeViewModel()
        setupListeners(clientId)
        
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
            
            // ✅ CORREÇÃO: Passar ambos os argumentos (rotaId e clienteId)
            val action = ClientDetailFragmentDirections.actionClientDetailFragmentToClientRegisterFragment(
                rotaId = viewModel.cliente.value?.rotaId ?: 1L,
                clienteId = clientId
            )
            Log.d("ClientDetailFragment", "Action criada com rotaId: ${viewModel.cliente.value?.rotaId ?: 1L}, clienteId: $clientId")
            findNavController().navigate(action)
            recolherFabMenu()
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
                            fichasInicial = mesa.fichasInicial,
                            fichasFinal = mesa.fichasFinal,
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

    private fun animateFabContainer(container: View, index: Int, startDelay: Long, show: Boolean = true, onEndAction: (() -> Unit)? = null) {
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

    private suspend fun gerarDistratoAposRetiradaMesa(contrato: ContratoLocacao, mesaRemovida: Mesa) {
        try {
            // Buscar mesas restantes do cliente
            val mesasRestantes = viewModel.mesasCliente.first()
            
            // Buscar último acerto do cliente usando AppRepository
            val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
            val ultimoAcerto = appRepository.buscarUltimoAcertoPorCliente(args.clienteId)
            val totalRecebido = ultimoAcerto?.valorRecebido ?: 0.0
            val despesasViagem = 0.0
            val subtotal = totalRecebido - despesasViagem
            val comissaoMotorista = subtotal * 0.03
            val comissaoIltair = totalRecebido * 0.02
            val totalGeral = subtotal - comissaoMotorista - comissaoIltair
            val saldo = ultimoAcerto?.debitoAtual ?: 0.0
            
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
