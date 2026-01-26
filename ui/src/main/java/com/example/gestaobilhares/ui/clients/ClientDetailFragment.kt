package com.example.gestaobilhares.ui.clients
import com.example.gestaobilhares.ui.R

import android.Manifest
import android.animation.ObjectAnimator
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import timber.log.Timber
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.ui.databinding.FragmentClientDetailBinding
import com.example.gestaobilhares.ui.clients.MesasAdapter
import com.example.gestaobilhares.ui.clients.SettlementHistoryAdapter
import com.example.gestaobilhares.ui.clients.HistoryFilterState
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.sync.SyncRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.fragment.app.viewModels
// Dialogs
import com.example.gestaobilhares.ui.clients.ConfirmarRetiradaMesaDialogFragment
import kotlinx.coroutines.flow.first
import com.example.gestaobilhares.ui.clients.RetiradaStatus
import com.example.gestaobilhares.data.entities.ContratoLocacao

@AndroidEntryPoint
class ClientDetailFragment : Fragment(), ConfirmarRetiradaMesaDialogFragment.ConfirmarRetiradaDialogListener {

    private var _binding: FragmentClientDetailBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null. Fragment may be destroyed.")
    private val args: ClientDetailFragmentArgs by navArgs()
    
    private val viewModel: ClientDetailViewModel by viewModels()
    
    @Inject
    lateinit var appRepository: AppRepository
    
    @Inject
    lateinit var syncRepository: SyncRepository
    
    private lateinit var mesasAdapter: MesasAdapter
    private lateinit var historicoAdapter: SettlementHistoryAdapter
    private var isFabMenuOpen = false
    private var mesaParaRemover: Mesa? = null
    private val clientId: Long get() = args.clienteId

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientDetailBinding.inflate(inflater, container, false)

        val clientId = args.clienteId

        setupRecyclerView()
        observeViewModel()
        setupListeners(clientId)
        
        // ✅ REMOVIDO: setupBackButtonHandler() - Navigation Component gerencia automaticamente
        
        // Carregar dados do cliente
        viewModel.loadClientDetails(clientId)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // ✅ CORREÇÃO: Recarregar detalhes completos do cliente para atualizar "Última visita"
        // quando voltar de outras telas (especialmente após salvar um acerto)
        viewModel.loadClientDetails(args.clienteId)
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
        
        Timber.d("ClientDetailFragment", "RecyclerViews configurados - Mesas e Histórico")
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.clientDetails.collect { resumo ->
                        resumo?.let { updateClientUI(it) }
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
                        Timber.d("ClientDetailFragment", "=== HISTÓRICO RECEBIDO ===")
                        Timber.d("ClientDetailFragment", "Quantidade de acertos: ${historico.size}")
                        Timber.d("ClientDetailFragment", "Adapter inicializado? ${::historicoAdapter.isInitialized}")
                        historico.forEachIndexed { index, acerto ->
                            Timber.d("ClientDetailFragment", "Acerto $index: ID=${acerto.id}, Data=${acerto.data}, Valor=${acerto.valorTotal}")
                        }
                        if (::historicoAdapter.isInitialized) {
                            historicoAdapter.submitList(historico)
                            Timber.d("ClientDetailFragment", "Lista enviada para o adapter")
                            // Garantir que o card e a lista fiquem visíveis quando houver dados
                            if (historico.isNotEmpty()) {
                                binding.rvSettlementHistory.visibility = View.VISIBLE
                            }
                        } else {
                            Timber.e("ClientDetailFragment", "Adapter não inicializado!")
                        }
                    }
                }
                launch {
                    viewModel.historyFilter.collect { state ->
                        updateHistoryFilterUI(state)
                    }
                }
                launch {
                    viewModel.historyLoading.collect { isLoading ->
                        binding.historyProgress.isVisible = isLoading
                        binding.btnHistoryLimit.isEnabled = !isLoading
                    }
                }
                launch {
                    viewModel.historyError.collect { errorMessage ->
                        errorMessage?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            viewModel.consumirHistoryError()
                        }
                    }
                }
                launch {
                    // ✅ NOVO: Observer para pendências do cliente - mostrar diálogo de alerta
                    viewModel.pendenciasCliente.collect { pendencias ->
                        if (pendencias.isNotEmpty() && isAdded && !requireActivity().isFinishing) {
                            mostrarDialogoPendencias(pendencias)
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners(clientId: Long) {
        // ✅ CORREÇÃO: Configurar botão voltar do header
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        binding.fabMain.setOnClickListener { toggleFabMenu() }
        binding.btnHistoryLimit.setOnClickListener {
            mostrarDialogQuantidade(clientId)
        }

        binding.fabEdit.setOnClickListener {
            Timber.d("ClientDetailFragment", "=== NAVEGAÇÃO PARA EDIÇÃO ===")
            Timber.d("ClientDetailFragment", "clientId sendo passado: $clientId")
            
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
                    Timber.d("ClientDetailFragment", "Action criada com rotaId: $rotaId, clienteId: $clientId")
                    findNavController().navigate(action)
                    recolherFabMenu()
                } catch (e: Exception) {
                    Timber.e("ClientDetailFragment", "Erro ao buscar rotaId do cliente: ${e.message}", e)
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
                        // TODO: RotaNaoIniciadaDialogFragment não existe - usar Toast temporariamente
                        Toast.makeText(requireContext(), "Rota não iniciada. Inicie a rota antes de adicionar mesas.", Toast.LENGTH_LONG).show()
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
                    Timber.e("ClientDetailFragment", "Erro ao iniciar novo acerto: ${e.message}")
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
                    // ✅ CORREÇÃO: Obter mesas do cliente e passar IDs corretos
                    val mesasDoCliente = viewModel.mesasCliente.first()
                    
                    if (mesasDoCliente.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "Este cliente não possui mesas locadas. Vincule mesas antes de gerar o contrato.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val bundle = android.os.Bundle().apply {
                            putLong("cliente_id", clientId)
                            putLongArray("mesas_vinculadas", mesasDoCliente.map { it.id }.toLongArray())
                            putBoolean("tipo_fixo", false)
                            putDouble("valor_fixo", 0.0)
                        }
                        findNavController().navigate(
                            com.example.gestaobilhares.ui.R.id.contractGenerationFragment,
                            bundle
                        )
                    }
                }
                recolherFabMenu()
            }
        }

        binding.fabWhatsApp.setOnClickListener {
            abrirWhatsApp()
            recolherFabMenu()
        }

        binding.fabPhone.setOnClickListener {
            abrirTelefone()
            recolherFabMenu()
        }

        // ✅ NOVO: Listener para ícone de localização
        binding.ivLocationIcon.setOnClickListener {
            abrirNavegacaoParaLocalizacao()
        }
    }

    private fun updateClientUI(cliente: ClienteResumo) {
        binding.tvClientName.text = cliente.nome
        binding.tvClientAddress.text = cliente.endereco
        val formattedDebt = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(cliente.debitoAtual)
        binding.tvClientCurrentDebt.text = formattedDebt
        binding.tvLastVisit.text = cliente.ultimaVisita
        val locationTint = if (cliente.latitude != null && cliente.longitude != null) {
            ContextCompat.getColor(requireContext(), R.color.green_500)
        } else {
            ContextCompat.getColor(requireContext(), R.color.white)
        }
        binding.ivLocationIcon.setColorFilter(locationTint)
        
        // ✅ NOVO: Exibir mensagem de pendência se houver
        if (cliente.mensagemPendencia != null && cliente.mensagemPendencia.isNotBlank()) {
            binding.tvMensagemPendencia.text = cliente.mensagemPendencia
            binding.tvMensagemPendencia.visibility = View.VISIBLE
        } else {
            binding.tvMensagemPendencia.visibility = View.GONE
        }
    }
    
    /**
     * ✅ NOVO: Mostra diálogo de alerta com as pendências do cliente
     * Exibe: dados faltantes (CPF, Telefone, Contrato), débito alto, sem acerto há mais de 4 meses
     * O diálogo só aparece se houver pelo menos uma pendência
     */
    private fun mostrarDialogoPendencias(pendencias: List<String>) {
        // ✅ Só mostrar se houver pendências
        if (pendencias.isEmpty() || !isAdded || requireActivity().isFinishing) return
        
        try {
            val mensagem = buildString {
                append("⚠️ Este cliente possui as seguintes pendências:\n\n")
                pendencias.forEachIndexed { index, pendencia ->
                    append("${index + 1}. $pendencia\n")
                }
                append("\n💡 Recomendamos resolver essas pendências para manter o cadastro atualizado.")
            }
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("⚠️ Pendências do Cliente")
                .setMessage(mensagem)
                .setPositiveButton("OK", null)
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            Timber.e("ClientDetailFragment", "Erro ao mostrar diálogo de pendências: ${e.message}", e)
        }
    }

    private fun toggleFabMenu() {
        isFabMenuOpen = !isFabMenuOpen
        if (isFabMenuOpen) {
            expandirFabMenu()
        } else {
            recolherFabMenu()
        }
    }

    private fun mostrarDialogQuantidade(clienteId: Long) {
        val context = requireContext()
        val inputLayout = TextInputLayout(context).apply {
            hint = "Quantidade de acertos"
        }
        val editText = TextInputEditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            val atual = viewModel.obterLimiteHistoricoAtual().toString()
            setText(atual)
            setSelection(atual.length)
        }
        inputLayout.addView(
            editText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val container = FrameLayout(context).apply {
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, 0, padding, 0)
            addView(inputLayout)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Buscar últimos acertos")
            .setMessage("Informe quantos acertos deseja visualizar (mínimo 3).")
            .setView(container)
            .setPositiveButton("Buscar") { _, _ ->
                val quantidade = editText.text?.toString()?.trim()?.toIntOrNull()
                if (quantidade == null || quantidade < 3) {
                    Toast.makeText(context, "Informe um número válido (mínimo 3).", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.carregarHistoricoPorQuantidade(clienteId, quantidade)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateHistoryFilterUI(@Suppress("UNUSED_PARAMETER") _state: HistoryFilterState) {
        // UI simplificada: não há mais label de status, apenas o título e botão "Mais"
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

    private fun animateFabContainer(container: View, @Suppress("UNUSED_PARAMETER") _index: Int, startDelay: Long, show: Boolean = true, onEndAction: (() -> Unit)? = null) {
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
                    // appRepository já injetado via Hilt
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
                    Timber.e("ClientDetailFragment", "Erro ao retirar mesa e gerar documento", e)
                    Toast.makeText(requireContext(), "Erro ao processar retirada da mesa: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // TODO: Removido override - interface não existe mais
    private fun onGerarRelatorioUltimoAcerto() {
        Toast.makeText(requireContext(), "Relatório de acerto será habilitado em seguida.", Toast.LENGTH_SHORT).show()
    }

    // TODO: Removido override - interface não existe mais
    private fun onGerarRelatorioAnual() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecione o Ano")
            .build()
        datePicker.addOnPositiveButtonClickListener {
            Toast.makeText(requireContext(), "Relatório anual será habilitado em seguida.", Toast.LENGTH_SHORT).show()
        }
        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    // TODO: Removido override - interface não existe mais
    private fun onObservacaoAdicionada(textoObservacao: String) {
        Timber.d("ClientDetailFragment", "Observação adicionada: $textoObservacao")
    }

    private suspend fun gerarDistratoAposRetiradaMesa(contrato: ContratoLocacao, @Suppress("UNUSED_PARAMETER") _mesaRemovida: Mesa) {
        try {
            // Buscar último acerto do cliente usando AppRepository
            // appRepository já injetado via Hilt
            // ✅ FASE 12.7: Variável removida (não utilizada)
            
            // Atualizar status do contrato para aguardando assinatura
            val agora = System.currentTimeMillis()
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
                com.example.gestaobilhares.ui.R.id.signatureCaptureFragment,
                bundle
            )
            
            Toast.makeText(
                requireContext(),
                "Mesa retirada. Assine o distrato para finalizar.",
                Toast.LENGTH_LONG
            ).show()
            
        } catch (e: Exception) {
            Timber.e("ClientDetailFragment", "Erro ao processar distrato", e)
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
            // appRepository já injetado via Hilt
            
        // Navegar para tela de assinatura do aditivo - Deixar a criação para o ViewModel da tela de destino
        val bundle = android.os.Bundle().apply {
            putLong("contratoId", contrato.id)
            putString("aditivoTipo", "RETIRADA")
            putLongArray("mesasVinculadas", longArrayOf(mesaRemovida.id))
        }
        findNavController().navigate(
            com.example.gestaobilhares.ui.R.id.aditivoSignatureFragment,
            bundle
        )
            
        } catch (e: Exception) {
            Timber.e("ClientDetailFragment", "Erro ao processar aditivo", e)
            Toast.makeText(
                requireContext(),
                "Erro ao processar aditivo: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private suspend fun gerarNumeroAditivo(): String {
        val ano = java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        // appRepository já injetado via Hilt
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
     * Abre o discador do Android com o número de telefone do cliente.
     * Usa Intent.ACTION_DIAL para abrir o discador sem fazer a chamada automaticamente.
     */
    private fun abrirTelefone() {
        val cliente = viewModel.cliente.value
        val telefone = cliente?.telefone
        if (!telefone.isNullOrBlank()) {
            // Limpar formatação do telefone (remover espaços, parênteses, hífens, etc)
            val numeroLimpo = telefone.filter { it.isDigit() }
            
            try {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$numeroLimpo")
                startActivity(intent)
            } catch (e: Exception) {
                Timber.e(e, "Erro ao abrir discador: %s", e.message)
                Toast.makeText(requireContext(), "Não foi possível abrir o discador.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Cliente não possui número de telefone cadastrado.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ✅ REMOVIDO: setupBackButtonHandler() - Navigation Component gerencia automaticamente
     * O botão voltar agora é gerenciado globalmente pelo MainActivity, que mostra
     * diálogo de saída apenas na tela de rotas. Em todas as outras telas, o Navigation
     * Component gerencia o comportamento padrão de voltar para a tela anterior.
     */

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
                    Timber.d("ClientDetailFragment", "✅ Google Maps aberto via esquema nativo")
                    return
                } catch (e: Exception) {
                    Timber.d("ClientDetailFragment", "Google Maps nativo não funcionou: ${e.message}")
                }
                
                // ✅ ESTRATÉGIA 2: Geo URI (funciona com qualquer app de mapas)
                try {
                    val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
                    val intentGeo = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intentGeo)
                    Timber.d("ClientDetailFragment", "✅ App de mapas aberto via geo URI")
                    return
                } catch (e: Exception) {
                    Timber.d("ClientDetailFragment", "Geo URI não funcionou: ${e.message}")
                }
                
                // ✅ ESTRATÉGIA 3: Google Maps via URL web
                try {
                    val url = "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude"
                    val intentWeb = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(url)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intentWeb)
                    Timber.d("ClientDetailFragment", "✅ Google Maps aberto via URL web")
                    return
                } catch (e: Exception) {
                    Timber.d("ClientDetailFragment", "URL web não funcionou: ${e.message}")
                }
                
                // ✅ ESTRATÉGIA 4: Waze (se disponível)
                try {
                    val uri = Uri.parse("waze://?ll=$latitude,$longitude&navigate=yes")
                    val intentWaze = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.waze")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intentWaze)
                    Timber.d("ClientDetailFragment", "✅ Waze aberto via esquema nativo")
                    return
                } catch (e: Exception) {
                    Timber.d("ClientDetailFragment", "Waze não disponível: ${e.message}")
                }
                
                // ✅ ÚLTIMA OPÇÃO: Mostrar mensagem de erro
                Toast.makeText(requireContext(), "Não foi possível abrir aplicativo de navegação. Verifique se há algum app de mapas instalado.", Toast.LENGTH_LONG).show()
                Timber.e("ClientDetailFragment", "❌ Todas as estratégias de navegação falharam")
                
            } catch (e: Exception) {
                Timber.e("ClientDetailFragment", "Erro geral ao abrir navegação: ${e.message}")
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

