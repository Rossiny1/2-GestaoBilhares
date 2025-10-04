package com.example.gestaobilhares.ui.settlement

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AcertoMesaRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.MesaRepository
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.databinding.FragmentSettlementDetailBinding
import com.example.gestaobilhares.utils.AppLogger
import com.example.gestaobilhares.utils.ReciboPrinterHelper
import com.example.gestaobilhares.R
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import com.example.gestaobilhares.ui.settlement.BluetoothPrinterHelper
import androidx.core.app.ActivityCompat
import java.io.File

/**
 * Fragment para exibir detalhes de um acerto específico.
 * FASE 4B+ - Detalhes e edição de acertos
 */
class SettlementDetailFragment : Fragment() {

    private var _binding: FragmentSettlementDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SettlementDetailViewModel
    private val args: SettlementDetailFragmentArgs by navArgs()

    private var mesaDetailAdapter: AcertoMesaDetailAdapter? = null
    private var currentSettlement: SettlementDetailViewModel.SettlementDetail? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettlementDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    // ✅ CORREÇÃO: Regra unificada para exibição do valor da ficha (igual ao SummaryDialog)
    private fun obterValorFichaExibir(settlement: SettlementDetailViewModel.SettlementDetail): Double {
        // ✅ CORREÇÃO: Usar a mesma lógica do SummaryDialog: prioriza valorFicha; se 0, usa comissaoFicha
        return if (settlement.valorFicha > 0) settlement.valorFicha else if (settlement.comissaoFicha > 0) settlement.comissaoFicha else 0.0
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel ANTES de configurar observers
        initializeViewModel()
        setupViewModelAndObservers()
        setupUI()
        loadData()
    }
    
    /**
     * ✅ NOVA FUNÇÃO: Callback para resultado das permissões Bluetooth
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == 1001) {
            val allPermissionsGranted = grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
            
            if (allPermissionsGranted) {
                android.widget.Toast.makeText(requireContext(), "Permissões concedidas! Tentando imprimir novamente...", android.widget.Toast.LENGTH_SHORT).show()
                // ✅ CORREÇÃO: Tentar imprimir novamente automaticamente (igual SettlementSummaryDialog)
                currentSettlement?.let { settlement ->
                    continuarImpressao(settlement)
                }
            } else {
                android.widget.Toast.makeText(requireContext(), "Permissões negadas. Não é possível imprimir.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initializeViewModel() {
        // Inicializar ViewModel onde o contexto está disponível
        val database = AppDatabase.getDatabase(requireContext())
        val appRepository = com.example.gestaobilhares.data.repository.AppRepository(
            database.clienteDao(),
            database.acertoDao(),
            database.mesaDao(),
            database.rotaDao(),
            database.despesaDao(),
            database.colaboradorDao(),
            database.cicloAcertoDao(),
            database.acertoMesaDao(),
            database.contratoLocacaoDao(),
            database.aditivoContratoDao(),
            database.assinaturaRepresentanteLegalDao(),
            database.logAuditoriaAssinaturaDao(),
            database.procuraçãoRepresentanteDao()
        )
        viewModel = SettlementDetailViewModel(
            AcertoRepository(database.acertoDao(), database.clienteDao()),
            AcertoMesaRepository(database.acertoMesaDao()),
            com.example.gestaobilhares.data.repository.ClienteRepository(
                database.clienteDao(),
                appRepository
            )
        )
    }

    private fun setupViewModelAndObservers() {
        // Observer para dados do acerto
        viewModel.settlementDetail.observe(viewLifecycleOwner) { settlement ->
            settlement?.let {
                AppLogger.log("SettlementDetailFragment", "Detalhes carregados: $it")
                currentSettlement = it
                updateUI(it)
            }
        }
        
        // Observer para loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: Mostrar loading se necessário
            if (isLoading) {
                AppLogger.log("SettlementDetailFragment", "Carregando detalhes...")
            }
        }
    }

    private fun loadData() {
        // Carregar dados do acerto
        viewModel.loadSettlementDetails(args.acertoId)
    }

    private fun setupUI() {
        // ✅ CORREÇÃO: Proteção contra crash no ScrollView
        try {
            // Botão voltar
            binding.btnBack.setOnClickListener {
                try {
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Log.e("SettlementDetailFragment", "Erro ao voltar: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("SettlementDetailFragment", "Erro ao configurar UI: ${e.message}")
        }
        
        // Botão editar
        binding.btnEdit.setOnClickListener {
            verificarPermissaoEdicao()
        }

        // ✅ NOVO: Botão Imprimir
        binding.btnImprimir.setOnClickListener {
            currentSettlement?.let { settlement ->
                imprimirRecibo(settlement)
            }
        }

        // ✅ NOVO: Botão WhatsApp
        binding.btnWhatsapp.setOnClickListener {
            currentSettlement?.let { settlement ->
                compartilharViaWhatsApp(settlement)
            }
        }
    }

    private fun updateUI(settlement: SettlementDetailViewModel.SettlementDetail) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        // Informações básicas
        binding.tvSettlementId.text = "#${settlement.id.toString().padStart(4, '0')}"
        binding.tvSettlementDate.text = settlement.date
            
        // Status com cor
        binding.tvSettlementStatus.text = settlement.status
        when (settlement.status) {
            "PAGO" -> binding.tvSettlementStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            "PENDENTE" -> binding.tvSettlementStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark))
            "ATRASADO" -> binding.tvSettlementStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            else -> binding.tvSettlementStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnSurface))
        }
        
        // Valores financeiros
        binding.tvInitialChips.text = formatter.format(settlement.debitoAnterior)
        binding.tvFinalChips.text = formatter.format(settlement.valorTotal)
        binding.tvPlayedChips.text = formatter.format(settlement.valorRecebido)
        binding.tvPlayedChips.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
        binding.tvChipValue.text = formatter.format(settlement.desconto)
        binding.tvTotalValue.text = formatter.format(settlement.debitoAtual)
        binding.tvTotalValue.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        
        // Observações
        binding.tvObservations.text = settlement.observacoes
        
        // Configurar RecyclerView das mesas
        setupMesasRecyclerView(settlement)
    }

    private fun setupMesasRecyclerView(settlement: SettlementDetailViewModel.SettlementDetail) {
        // ✅ DIAGNÓSTICO CRÍTICO: Logs extensos para identificar problema com múltiplas mesas
        AppLogger.log("SettlementDetailFragment", "=== CONFIGURANDO RECYCLERVIEW DAS MESAS ===")
        AppLogger.log("SettlementDetailFragment", "Acerto ID: ${settlement.id}")
        AppLogger.log("SettlementDetailFragment", "Total mesas no settlement: ${settlement.acertoMesas.size}")
        AppLogger.log("SettlementDetailFragment", "Total mesas esperadas (totalMesas): ${settlement.totalMesas}")
        
        // ✅ VERIFICAÇÃO CRÍTICA: Se não há mesas, isso é um problema
        if (settlement.acertoMesas.isEmpty()) {
            AppLogger.log("SettlementDetailFragment", "❌ PROBLEMA CRÍTICO: settlement.acertoMesas está vazio!")
            AppLogger.log("SettlementDetailFragment", "Isso indica problema na busca das mesas no banco de dados")
            return
        }
        
        settlement.acertoMesas.forEachIndexed { index, acertoMesa ->
            AppLogger.log("SettlementDetailFragment", "=== MESA ${index + 1} PARA EXIBIÇÃO ===")
            AppLogger.log("SettlementDetailFragment", "Mesa ID: ${acertoMesa.mesaId}")
            AppLogger.log("SettlementDetailFragment", "Acerto ID: ${acertoMesa.acertoId}")
            AppLogger.log("SettlementDetailFragment", "Relógio: ${acertoMesa.relogioInicial} → ${acertoMesa.relogioFinal}")
            AppLogger.log("SettlementDetailFragment", "Fichas jogadas: ${acertoMesa.fichasJogadas}")
            AppLogger.log("SettlementDetailFragment", "Valor fixo: R$ ${acertoMesa.valorFixo}")
            AppLogger.log("SettlementDetailFragment", "Subtotal: R$ ${acertoMesa.subtotal}")
            AppLogger.log("SettlementDetailFragment", "Com defeito: ${acertoMesa.comDefeito}")
            AppLogger.log("SettlementDetailFragment", "Relógio reiniciou: ${acertoMesa.relogioReiniciou}")
        }
        
        // ✅ MELHORIA: Buscar dados completos das mesas para exibir numeração correta
        lifecycleScope.launch {
            try {
                val mesaRepository = MesaRepository(AppDatabase.getDatabase(requireContext()).mesaDao())
                val mesasCompletas = mutableMapOf<Long, AcertoMesaDetailAdapter.MesaCompleta>()
                
                AppLogger.log("SettlementDetailFragment", "=== BUSCANDO DADOS COMPLETOS DAS MESAS ===")
                for (acertoMesa in settlement.acertoMesas) {
                    AppLogger.log("SettlementDetailFragment", "Buscando mesa ID: ${acertoMesa.mesaId}")
                    val mesaCompleta = mesaRepository.buscarPorId(acertoMesa.mesaId)
                    if (mesaCompleta != null) {
                        AppLogger.log("SettlementDetailFragment", "Mesa encontrada: ${mesaCompleta.numero} (${mesaCompleta.tipoMesa.name})")
                        mesasCompletas[acertoMesa.mesaId] = AcertoMesaDetailAdapter.MesaCompleta(
                            numero = mesaCompleta.numero,
                            tipo = mesaCompleta.tipoMesa.name
                        )
                    } else {
                        AppLogger.log("SettlementDetailFragment", "Mesa não encontrada para ID: ${acertoMesa.mesaId}")
                    }
                }
                
                AppLogger.log("SettlementDetailFragment", "=== CONFIGURANDO ADAPTER ===")
                AppLogger.log("SettlementDetailFragment", "Mesas para adapter: ${settlement.acertoMesas.size}")
                AppLogger.log("SettlementDetailFragment", "Mesas completas encontradas: ${mesasCompletas.size}")
                
                // Configurar adapter com dados completos
                mesaDetailAdapter = AcertoMesaDetailAdapter(
                    mesas = settlement.acertoMesas,
                    tipoAcerto = settlement.tipoAcerto,
                    panoTrocado = settlement.panoTrocado,
                    numeroPano = settlement.numeroPano,
                    mesasCompletas = mesasCompletas,
                    onVerFotoRelogio = { caminhoFoto, dataFoto ->
                        try {
                            visualizarFotoRelogio(caminhoFoto, dataFoto)
                        } catch (e: Exception) {
                            Log.e("SettlementDetailFragment", "Erro ao visualizar foto: ${e.message}")
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Erro ao visualizar foto: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                
                AppLogger.log("SettlementDetailFragment", "=== CONFIGURANDO RECYCLERVIEW ===")
                binding.rvMesasDetalhe.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = mesaDetailAdapter
                    // ✅ CORREÇÃO OFICIAL: Configuração para NestedScrollView
                    isNestedScrollingEnabled = false
                    setHasFixedSize(false)
                }
                
                AppLogger.log("SettlementDetailFragment", "Adapter configurado com ${mesaDetailAdapter?.itemCount ?: 0} itens")
                
            } catch (e: Exception) {
                AppLogger.log("SettlementDetailFragment", "Erro ao carregar dados das mesas: ${e.message}")
                // Fallback para adapter básico
                mesaDetailAdapter = AcertoMesaDetailAdapter(
                    mesas = settlement.acertoMesas,
                    tipoAcerto = settlement.tipoAcerto,
                    panoTrocado = settlement.panoTrocado,
                    numeroPano = settlement.numeroPano,
                    onVerFotoRelogio = { caminhoFoto, dataFoto ->
                        try {
                            visualizarFotoRelogio(caminhoFoto, dataFoto)
                        } catch (e: Exception) {
                            Log.e("SettlementDetailFragment", "Erro ao visualizar foto: ${e.message}")
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Erro ao visualizar foto: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                
                binding.rvMesasDetalhe.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = mesaDetailAdapter
                    // ✅ CORREÇÃO OFICIAL: Configuração para NestedScrollView
                    isNestedScrollingEnabled = false
                    setHasFixedSize(false)
                }
            }
        }
    }

    // ✅ NOVA FUNCIONALIDADE: Imprimir recibo
    private fun imprimirRecibo(settlement: SettlementDetailViewModel.SettlementDetail) {
        lifecycleScope.launch {
            try {
                // Verificar permissões Bluetooth
                if (!hasBluetoothPermissions()) {
                    requestBluetoothPermissions(settlement)
                    return@launch
                }
                
                // Se chegou aqui, as permissões estão OK, continuar com a impressão
                continuarImpressao(settlement)
                
            } catch (e: Exception) {
                AppLogger.log("SettlementDetailFragment", "Erro ao preparar impressão: ${e.message}")
                android.widget.Toast.makeText(requireContext(), "Erro ao preparar impressão: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * ✅ NOVA FUNÇÃO: Continua o processo de impressão após verificar permissões
     */
    private fun continuarImpressao(settlement: SettlementDetailViewModel.SettlementDetail) {
        try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                android.widget.Toast.makeText(requireContext(), "Bluetooth não disponível neste dispositivo", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            if (!bluetoothAdapter.isEnabled) {
                android.widget.Toast.makeText(requireContext(), "Ative o Bluetooth para imprimir", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            val pairedDevices = bluetoothAdapter.bondedDevices
            if (pairedDevices.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Nenhuma impressora Bluetooth pareada", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            // Diálogo de seleção de impressora
            val deviceList = pairedDevices.toList()
            val deviceNames = deviceList.map { it.name ?: it.address }.toTypedArray()
            
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Selecione a impressora")
                .setItems(deviceNames) { _, which ->
                    val printerDevice = deviceList[which]
                    imprimirComImpressoraSelecionada(settlement, printerDevice)
                }
                .setNegativeButton("Cancelar", null)
                .show()
                
        } catch (e: Exception) {
            AppLogger.log("SettlementDetailFragment", "Erro ao continuar impressão: ${e.message}")
            android.widget.Toast.makeText(requireContext(), "Erro ao continuar impressão: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * ✅ NOVA FUNÇÃO: Imprime com a impressora selecionada
     */
    private fun imprimirComImpressoraSelecionada(settlement: SettlementDetailViewModel.SettlementDetail, printerDevice: android.bluetooth.BluetoothDevice) {
        // Mostrar diálogo de loading
        val loadingDialog = android.app.AlertDialog.Builder(requireContext())
            .setMessage("Imprimindo recibo...")
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        // Executar impressão em thread separada
        Thread {
            var erro: String? = null
            try {
                val printerHelper = BluetoothPrinterHelper(printerDevice)
                if (printerHelper.connect()) {
                    // Inflar o layout do recibo
                    val inflater = android.view.LayoutInflater.from(requireContext())
                    val reciboView = inflater.inflate(com.example.gestaobilhares.R.layout.layout_recibo_impressao, null) as android.view.ViewGroup
                    
                    // Preencher campos do recibo
                    preencherLayoutRecibo(reciboView, settlement)
                    
                    // Imprimir
                    printerHelper.printReciboLayoutBitmap(reciboView)
                    printerHelper.disconnect()
                } else {
                    erro = "Falha ao conectar à impressora"
                }
            } catch (e: Exception) {
                erro = when {
                    e.message?.contains("socket") == true -> "Impressora desligada ou fora de alcance"
                    e.message?.contains("broken pipe") == true -> "Falha ao enviar dados. Impressora pode estar desconectada"
                    else -> "Erro inesperado: ${e.message ?: "Desconhecido"}"
                }
            }
            
            // Atualizar UI na thread principal
            requireActivity().runOnUiThread {
                loadingDialog.dismiss()
                if (erro == null) {
                    android.widget.Toast.makeText(requireContext(), "Recibo enviado para impressão!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Erro na impressão")
                        .setMessage(erro)
                        .setPositiveButton("Tentar novamente") { _, _ ->
                            imprimirComImpressoraSelecionada(settlement, printerDevice)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        }.start()
    }
    
    /**
     * ✅ NOVA FUNÇÃO: Preenche o layout do recibo com os dados do acerto
     */
    private fun preencherLayoutRecibo(reciboView: android.view.ViewGroup, settlement: SettlementDetailViewModel.SettlementDetail) {
        // ✅ CORREÇÃO: Usar lifecycleScope para chamadas suspensas
        lifecycleScope.launch {
            try {
                // ✅ CORREÇÃO: Usar métodos centralizados para garantir consistência
                val mesasCompletas = obterMesasCompletas(settlement)
                val numeroContrato = obterNumeroContrato(settlement)
                
                // ✅ LOGS CRÍTICOS: Verificar dados antes de imprimir
                AppLogger.log("SettlementDetailFragment", "=== DADOS PARA IMPRESSÃO ===")
                AppLogger.log("SettlementDetailFragment", "Cliente Nome: '${settlement.clienteNome}'")
                AppLogger.log("SettlementDetailFragment", "Cliente CPF: '${settlement.clienteCpf}'")
                AppLogger.log("SettlementDetailFragment", "ValorFicha original: ${settlement.valorFicha}")
                AppLogger.log("SettlementDetailFragment", "ComissaoFicha original: ${settlement.comissaoFicha}")
                AppLogger.log("SettlementDetailFragment", "Valor Ficha Exibir: ${obterValorFichaExibir(settlement)}")
                AppLogger.log("SettlementDetailFragment", "Acerto ID: ${settlement.id}")
                AppLogger.log("SettlementDetailFragment", "Número Contrato: '$numeroContrato'")
                AppLogger.log("SettlementDetailFragment", "Mesas Completas: ${mesasCompletas.size}")
                
                // Usar a função centralizada com informações completas das mesas
                ReciboPrinterHelper.preencherReciboImpressaoCompleto(
                    context = requireContext(),
                    reciboView = reciboView,
                    clienteNome = settlement.clienteNome,
                    clienteCpf = settlement.clienteCpf,
                    mesasCompletas = mesasCompletas,
                    debitoAnterior = settlement.debitoAnterior,
                    valorTotalMesas = settlement.valorTotal,
                    desconto = settlement.desconto,
                    metodosPagamento = settlement.metodosPagamento,
                    debitoAtual = settlement.debitoAtual,
                    observacao = settlement.observacoes,
                    valorFicha = obterValorFichaExibir(settlement),
                    acertoId = settlement.id,
                    numeroContrato = numeroContrato
                )
            } catch (e: Exception) {
                Log.e("SettlementDetailFragment", "Erro ao preencher layout do recibo: ${e.message}")
            }
        }
    }
    
    /**
     * ✅ NOVA FUNÇÃO: Verifica permissões Bluetooth (mesmo que SettlementSummaryDialog)
     */
    private fun hasBluetoothPermissions(): Boolean {
        val bluetoothPermissions = arrayOf(
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN
        )
        
        return bluetoothPermissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * ✅ NOVA FUNÇÃO: Solicita permissões Bluetooth (mesmo que SettlementSummaryDialog)
     */
    private fun requestBluetoothPermissions(settlement: SettlementDetailViewModel.SettlementDetail) {
        val bluetoothPermissions = arrayOf(
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN
        )
        
        androidx.core.app.ActivityCompat.requestPermissions(requireActivity(), bluetoothPermissions, 1001)
        
        // ✅ CORREÇÃO: Mostrar mensagem explicativa
        android.widget.Toast.makeText(requireContext(), "Permissões Bluetooth necessárias para impressão", android.widget.Toast.LENGTH_LONG).show()
    }

    // ✅ NOVA FUNCIONALIDADE: Compartilhar via WhatsApp
    private fun compartilharViaWhatsApp(settlement: SettlementDetailViewModel.SettlementDetail) {
        lifecycleScope.launch {
            try {
                // ✅ CORREÇÃO: Usar exatamente os mesmos dados da impressão
                val mesasCompletas = obterMesasCompletas(settlement)
                val numeroContrato = obterNumeroContrato(settlement)
                val valorFichaExibirLocal = obterValorFichaExibir(settlement)
                
                // ✅ LOGS CRÍTICOS: Verificar dados antes do WhatsApp
                AppLogger.log("SettlementDetailFragment", "=== DADOS PARA WHATSAPP ===")
                AppLogger.log("SettlementDetailFragment", "Cliente Nome: '${settlement.clienteNome}'")
                AppLogger.log("SettlementDetailFragment", "Cliente CPF: '${settlement.clienteCpf}'")
                AppLogger.log("SettlementDetailFragment", "ValorFicha original: ${settlement.valorFicha}")
                AppLogger.log("SettlementDetailFragment", "ComissaoFicha original: ${settlement.comissaoFicha}")
                AppLogger.log("SettlementDetailFragment", "Valor Ficha Exibir: $valorFichaExibirLocal")
                AppLogger.log("SettlementDetailFragment", "Acerto ID: ${settlement.id}")
                AppLogger.log("SettlementDetailFragment", "Número Contrato: '$numeroContrato'")
                AppLogger.log("SettlementDetailFragment", "Mesas Completas: ${mesasCompletas.size}")
                
                // ✅ CORREÇÃO: Usar ReciboPrinterHelper para gerar texto WhatsApp (mesma fonte de verdade)
                val textoResumo = ReciboPrinterHelper.gerarTextoWhatsApp(
                    clienteNome = settlement.clienteNome,
                    clienteCpf = settlement.clienteCpf,
                    mesasCompletas = mesasCompletas,
                    debitoAnterior = settlement.debitoAnterior,
                    valorTotalMesas = settlement.valorTotal,
                    desconto = settlement.desconto,
                    metodosPagamento = settlement.metodosPagamento,
                    debitoAtual = settlement.debitoAtual,
                    observacao = settlement.observacoes,
                    valorFicha = valorFichaExibirLocal,
                    acertoId = settlement.id,
                    numeroContrato = numeroContrato
                )
                
                // ✅ NOVO: Enviar via WhatsApp direto com telefone do cliente
                enviarViaWhatsAppDireto(settlement.clienteTelefone, textoResumo)
                
            } catch (e: Exception) {
                AppLogger.log("SettlementDetailFragment", "Erro ao compartilhar via WhatsApp: ${e.message}")
                android.widget.Toast.makeText(requireContext(), "Erro ao compartilhar via WhatsApp", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * ✅ MÉTODO CENTRALIZADO: Obtém mesas completas (FONTE ÚNICA DE VERDADE)
     */
    private suspend fun obterMesasCompletas(settlement: SettlementDetailViewModel.SettlementDetail): List<Mesa> {
        val mesaRepository = MesaRepository(AppDatabase.getDatabase(requireContext()).mesaDao())
        val mesasCompletas = mutableListOf<Mesa>()
        
        for (acertoMesa in settlement.acertoMesas) {
            val mesaCompleta = mesaRepository.buscarPorId(acertoMesa.mesaId)
            if (mesaCompleta != null) {
                val mesaComAcerto = mesaCompleta.copy(
                    fichasInicial = acertoMesa.relogioInicial,
                    fichasFinal = acertoMesa.relogioFinal
                )
                mesasCompletas.add(mesaComAcerto)
            }
        }
        
        return mesasCompletas
    }
    
    /**
     * ✅ MÉTODO CENTRALIZADO: Obtém número do contrato (FONTE ÚNICA DE VERDADE)
     */
    private suspend fun obterNumeroContrato(settlement: SettlementDetailViewModel.SettlementDetail): String? {
        // ✅ CORREÇÃO: Usar settlement.id em vez de args.acertoId para garantir consistência
        val acertoCompleto = viewModel.buscarAcertoPorId(settlement.id)
        val contratoAtivo = acertoCompleto?.let { 
            viewModel.buscarContratoAtivoPorCliente(it.clienteId) 
        }
        AppLogger.log("SettlementDetailFragment", "=== BUSCA CONTRATO ===")
        AppLogger.log("SettlementDetailFragment", "Acerto ID: ${settlement.id}")
        AppLogger.log("SettlementDetailFragment", "Cliente ID: ${acertoCompleto?.clienteId}")
        AppLogger.log("SettlementDetailFragment", "Contrato encontrado: ${contratoAtivo != null}")
        AppLogger.log("SettlementDetailFragment", "Número do contrato: ${contratoAtivo?.numeroContrato}")
        return contratoAtivo?.numeroContrato
    }

    
    /**
     * ✅ NOVA FUNÇÃO: Busca informações completas da mesa (versão síncrona)
     */
    private fun buscarMesaCompleta(mesaId: Long): Mesa? {
        return try {
            // Usar runBlocking para chamar função suspensa de forma síncrona
            kotlinx.coroutines.runBlocking {
                val mesaRepository = MesaRepository(AppDatabase.getDatabase(requireContext()).mesaDao())
                mesaRepository.buscarPorId(mesaId)
            }
        } catch (e: Exception) {
            Log.e("SettlementDetailFragment", "Erro ao buscar mesa: ${e.message}")
            null
        }
    }

    /**
     * ✅ SOLUÇÃO DEFINITIVA: Abre WhatsApp diretamente com o número do cliente
     * Baseado na documentação oficial WhatsApp e Android Intents
     * ELIMINA COMPLETAMENTE o seletor de apps
     */
    private fun enviarViaWhatsAppDireto(telefone: String?, texto: String) {
        if (telefone.isNullOrEmpty()) {
            android.widget.Toast.makeText(requireContext(), "Cliente não possui telefone cadastrado", android.widget.Toast.LENGTH_SHORT).show()
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
            
            AppLogger.log("SettlementDetailFragment", "Número original: $telefone")
            AppLogger.log("SettlementDetailFragment", "Número limpo: $numeroLimpo")
            AppLogger.log("SettlementDetailFragment", "Número completo: $numeroCompleto")
            
            // ✅ ESTRATÉGIA 1: Esquema nativo whatsapp://send (FORÇA direcionamento direto)
            try {
                val uri = android.net.Uri.parse("whatsapp://send?phone=$numeroCompleto&text=${android.net.Uri.encode(texto)}")
                val intentWhatsApp = Intent(Intent.ACTION_VIEW, uri).apply {
                    // ✅ CRÍTICO: Força o direcionamento direto sem seletor
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                startActivity(intentWhatsApp)
                AppLogger.log("SettlementDetailFragment", "✅ WhatsApp aberto diretamente via esquema nativo")
                return
            } catch (e: Exception) {
                AppLogger.log("SettlementDetailFragment", "Esquema nativo não funcionou: ${e.message}")
            }
            
            // ✅ ESTRATÉGIA 2: URL wa.me (funciona mesmo sem app instalado)
            try {
                val url = "https://wa.me/$numeroCompleto?text=${android.net.Uri.encode(texto)}"
                val intentUrl = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(url)
                    // ✅ CRÍTICO: Força o direcionamento direto
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                startActivity(intentUrl)
                AppLogger.log("SettlementDetailFragment", "✅ WhatsApp aberto via URL wa.me")
                return
            } catch (e: Exception) {
                AppLogger.log("SettlementDetailFragment", "URL wa.me não funcionou: ${e.message}")
            }
            
            // ✅ ESTRATÉGIA 3: Tentar WhatsApp Business via esquema nativo
            try {
                val uri = android.net.Uri.parse("whatsapp://send?phone=$numeroCompleto&text=${android.net.Uri.encode(texto)}")
                val intentBusiness = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp.w4b")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                startActivity(intentBusiness)
                AppLogger.log("SettlementDetailFragment", "✅ WhatsApp Business aberto via esquema nativo")
                return
            } catch (e: Exception) {
                AppLogger.log("SettlementDetailFragment", "WhatsApp Business não disponível: ${e.message}")
            }
            
            // ✅ ESTRATÉGIA 4: Intent direto com ACTION_SEND mas SEM chooser
            try {
                val intentDirect = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, texto)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                startActivity(intentDirect)
                AppLogger.log("SettlementDetailFragment", "✅ WhatsApp aberto via intent direto")
                return
            } catch (e: Exception) {
                AppLogger.log("SettlementDetailFragment", "Intent direto falhou: ${e.message}")
            }
            
            // ✅ ÚLTIMA OPÇÃO: Mostrar mensagem de erro
            android.widget.Toast.makeText(requireContext(), "Não foi possível abrir o WhatsApp. Verifique se está instalado.", android.widget.Toast.LENGTH_LONG).show()
            AppLogger.log("SettlementDetailFragment", "❌ Todas as estratégias falharam")
            
        } catch (e: Exception) {
            AppLogger.log("SettlementDetailFragment", "Erro geral ao abrir WhatsApp: ${e.message}")
            android.widget.Toast.makeText(requireContext(), "Erro ao abrir WhatsApp: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ✅ MANTIDO: Método original para compatibilidade
     */
    private fun enviarViaWhatsApp(texto: String) {
        enviarViaWhatsAppDireto(null, texto)
    }

    /**
     * ✅ NOVA FUNCIONALIDADE: Verifica se o acerto pode ser editado
     */
    private fun verificarPermissaoEdicao() {
        lifecycleScope.launch {
            try {
                AppLogger.log("SettlementDetailFragment", "=== VERIFICANDO PERMISSÃO DE EDIÇÃO ===")
                AppLogger.log("SettlementDetailFragment", "Acerto ID: ${args.acertoId}")
                
                // Mostrar loading
                binding.btnEdit.isEnabled = false
                
                // Inicializar repositórios com construtores corretos
                val acertoRepo = AcertoRepository(
                    AppDatabase.getDatabase(requireContext()).acertoDao(),
                    AppDatabase.getDatabase(requireContext()).clienteDao()
                )
                val database = AppDatabase.getDatabase(requireContext())
                val appRepository = com.example.gestaobilhares.data.repository.AppRepository(
                    database.clienteDao(),
                    database.acertoDao(),
                    database.mesaDao(),
                    database.rotaDao(),
                    database.despesaDao(),
                    database.colaboradorDao(),
                    database.cicloAcertoDao(),
                    database.acertoMesaDao(),
                    database.contratoLocacaoDao(),
                    database.aditivoContratoDao(),
                    database.assinaturaRepresentanteLegalDao(),
                    database.logAuditoriaAssinaturaDao(),
                    database.procuraçãoRepresentanteDao()
                )
                val clienteRepo = com.example.gestaobilhares.data.repository.ClienteRepository(
                    database.clienteDao(),
                    appRepository
                )
                val despesaRepo = com.example.gestaobilhares.data.repository.DespesaRepository(
                    AppDatabase.getDatabase(requireContext()).despesaDao()
                )
                val cicloAcertoRepository = CicloAcertoRepository(
                    AppDatabase.getDatabase(requireContext()).cicloAcertoDao(),
                    despesaRepo,
                    acertoRepo,
                    clienteRepo,
                    AppDatabase.getDatabase(requireContext()).rotaDao()
                )
                
                // Verificar permissão
                val permissao = acertoRepo.podeEditarAcerto(args.acertoId, cicloAcertoRepository)
                
                when (permissao) {
                    is AcertoRepository.PermissaoEdicao.Permitido -> {
                        AppLogger.log("SettlementDetailFragment", "✅ Edição permitida. Navegando para tela de edição...")
                        navegarParaEdicao()
                    }
                    is AcertoRepository.PermissaoEdicao.CicloInativo -> {
                        mostrarDialogoPermissaoNegada(
                            "Ciclo Inativo",
                            permissao.motivo + "\n\nApenas acertos do ciclo atual podem ser editados."
                        )
                    }
                    is AcertoRepository.PermissaoEdicao.NaoEhUltimoAcerto -> {
                        mostrarDialogoPermissaoNegada(
                            "Edição Não Permitida",
                            permissao.motivo + "\n\nPara editar um acerto anterior, você deve primeiro excluir os acertos posteriores."
                        )
                    }
                    is AcertoRepository.PermissaoEdicao.AcertoNaoEncontrado -> {
                        mostrarDialogoPermissaoNegada(
                            "Erro",
                            "O acerto não foi encontrado no banco de dados."
                        )
                    }
                    is AcertoRepository.PermissaoEdicao.ErroValidacao -> {
                        mostrarDialogoPermissaoNegada(
                            "Erro de Validação",
                            "Ocorreu um erro ao validar a permissão de edição:\n${permissao.motivo}"
                        )
                    }
                }
                
            } catch (e: Exception) {
                AppLogger.log("SettlementDetailFragment", "❌ Erro ao verificar permissão: ${e.message}")
                mostrarDialogoPermissaoNegada(
                    "Erro",
                    "Ocorreu um erro inesperado: ${e.message}"
                )
            } finally {
                binding.btnEdit.isEnabled = true
            }
        }
    }

    /**
     * ✅ NOVA FUNCIONALIDADE: Mostra diálogo quando edição não é permitida
     */
    private fun mostrarDialogoPermissaoNegada(titulo: String, mensagem: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("⚠️ $titulo")
            .setMessage(mensagem)
            .setPositiveButton("Entendi", null)
            .show()
    }

    /**
     * ✅ NOVA FUNCIONALIDADE: Navega para tela de edição
     */
    private fun navegarParaEdicao() {
        try {
            val acerto = currentSettlement
            if (acerto != null) {
                // Descobrir o clienteId a partir do acerto
                lifecycleScope.launch {
                    val acertoCompleto = viewModel.buscarAcertoPorId(args.acertoId)
                    
                    if (acertoCompleto != null) {
                        // Criar array vazio de MesaDTO já que estamos editando um acerto existente
                        val mesasDTO = emptyArray<MesaDTO>()
                        
                        val action = SettlementDetailFragmentDirections
                            .actionSettlementDetailFragmentToSettlementFragment(
                                clienteId = acertoCompleto.clienteId,
                                acertoIdParaEdicao = args.acertoId,
                                mesasDTO = mesasDTO
                            )
                        findNavController().navigate(action)
                    } else {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Erro: Não foi possível carregar dados do acerto",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.log("SettlementDetailFragment", "Erro ao navegar para edição: ${e.message}")
            android.widget.Toast.makeText(
                requireContext(),
                "Erro ao abrir tela de edição: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * ✅ NOVA FUNCIONALIDADE: Visualizar foto do relógio final
     */
    private fun visualizarFotoRelogio(caminhoFoto: String, dataFoto: Date?) {
        try {
            AppLogger.log("SettlementDetailFragment", "=== VISUALIZANDO FOTO ===")
            AppLogger.log("SettlementDetailFragment", "Caminho da foto: $caminhoFoto")

            // ✅ CORREÇÃO MELHORADA: Converter URI do content provider para caminho real
            val caminhoReal = if (caminhoFoto.startsWith("content://")) {
                try {
                    // Converter URI para caminho real usando ContentResolver
                    val uri = android.net.Uri.parse(caminhoFoto)
                    val cursor = requireContext().contentResolver.query(
                        uri, 
                        arrayOf(android.provider.MediaStore.Images.Media.DATA), 
                        null, 
                        null, 
                        null
                    )
                    
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val columnIndex = it.getColumnIndex(android.provider.MediaStore.Images.Media.DATA)
                            if (columnIndex != -1) {
                                val path = it.getString(columnIndex)
                                AppLogger.log("SettlementDetailFragment", "Caminho real encontrado via cursor: $path")
                                path
                            } else {
                                AppLogger.log("SettlementDetailFragment", "Coluna DATA não encontrada")
                                caminhoFoto
                            }
                        } else {
                            AppLogger.log("SettlementDetailFragment", "Cursor vazio")
                            caminhoFoto
                        }
                    } ?: caminhoFoto
                } catch (e: Exception) {
                    AppLogger.log("SettlementDetailFragment", "Erro ao converter URI: ${e.message}")
                    // ✅ NOVA TENTATIVA: Usar FileProvider para obter o caminho real
                    try {
                        val uri = android.net.Uri.parse(caminhoFoto)
                        val inputStream = requireContext().contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            // Criar arquivo temporário
                            val tempFile = File.createTempFile("temp_photo", ".jpg", requireContext().cacheDir)
                            tempFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                            AppLogger.log("SettlementDetailFragment", "Arquivo temporário criado: ${tempFile.absolutePath}")
                            tempFile.absolutePath
                        } else {
                            caminhoFoto
                        }
                    } catch (e2: Exception) {
                        AppLogger.log("SettlementDetailFragment", "Erro na segunda tentativa: ${e2.message}")
                        caminhoFoto
                    }
                }
            } else {
                caminhoFoto
            }

            AppLogger.log("SettlementDetailFragment", "Caminho real da foto: $caminhoReal")

            val file = java.io.File(caminhoReal)
            if (!file.exists()) {
                AppLogger.log("SettlementDetailFragment", "❌ Arquivo não existe: $caminhoReal")
                
                // ✅ NOVA TENTATIVA: Tentar carregar diretamente do URI
                try {
                    val uri = android.net.Uri.parse(caminhoFoto)
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        AppLogger.log("SettlementDetailFragment", "✅ Carregando foto diretamente do URI")
                        mostrarFotoDialog(inputStream, dataFoto)
                        return
                    }
                } catch (e: Exception) {
                    AppLogger.log("SettlementDetailFragment", "Erro ao carregar do URI: ${e.message}")
                }
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "Arquivo de foto não encontrado: ${file.absolutePath}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            }

            // ✅ CORREÇÃO: Usar método separado para mostrar o diálogo
            mostrarFotoDialog(file, dataFoto)

        } catch (e: Exception) {
            AppLogger.log("SettlementDetailFragment", "Erro ao visualizar foto: ${e.message}")
            android.widget.Toast.makeText(
                requireContext(),
                "Erro ao visualizar foto: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * ✅ NOVO: Método separado para mostrar o diálogo da foto
     */
    private fun mostrarFotoDialog(file: java.io.File, dataFoto: Date?) {
        try {
            // Criar diálogo para exibir a foto
            val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .create()

            // Layout para o diálogo
            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
            }

            // ImageView para a foto
            val imageView = android.widget.ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                maxHeight = 800
            }

            // Carregar e exibir a foto
            try {
                AppLogger.log("SettlementDetailFragment", "Tentando carregar foto do arquivo: ${file.absolutePath}")
                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    AppLogger.log("SettlementDetailFragment", "✅ Foto carregada com sucesso")
                    imageView.setImageBitmap(bitmap)
                } else {
                    AppLogger.log("SettlementDetailFragment", "❌ Bitmap é nulo")
                    imageView.setImageResource(R.drawable.ic_camera)
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Erro ao carregar a foto: bitmap nulo",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                AppLogger.log("SettlementDetailFragment", "❌ Erro ao carregar foto: ${e.message}")
                imageView.setImageResource(R.drawable.ic_camera)
                android.widget.Toast.makeText(
                    requireContext(),
                    "Erro ao carregar a foto: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }

            // TextView para informações da foto
            val textView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
                text = "Foto do Relógio Final"
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnSurface))
            }

            // TextView para data da foto
            val dataTextView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 8
                }
                text = dataFoto?.let {
                    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR"))
                    "Capturada em: ${dateFormat.format(it)}"
                } ?: "Data não disponível"
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }

            // Adicionar views ao layout
            layout.addView(textView)
            layout.addView(imageView)
            layout.addView(dataTextView)

            // Configurar diálogo
            dialog.setView(layout)
            dialog.setButton(
                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE,
                "Fechar"
            ) { _, _ -> dialog.dismiss() }

            // Mostrar diálogo
            dialog.show()

        } catch (e: Exception) {
            AppLogger.log("SettlementDetailFragment", "Erro ao criar diálogo: ${e.message}")
            android.widget.Toast.makeText(
                requireContext(),
                "Erro ao exibir foto: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * ✅ NOVO: Método sobrecarregado para InputStream
     */
    private fun mostrarFotoDialog(inputStream: java.io.InputStream, dataFoto: Date?) {
        try {
            // Criar diálogo para exibir a foto
            val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .create()

            // Layout para o diálogo
            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
            }

            // ImageView para a foto
            val imageView = android.widget.ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                maxHeight = 800
            }

            // Carregar e exibir a foto do InputStream
            try {
                AppLogger.log("SettlementDetailFragment", "Tentando carregar foto do InputStream")
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    AppLogger.log("SettlementDetailFragment", "✅ Foto carregada com sucesso do InputStream")
                    imageView.setImageBitmap(bitmap)
                } else {
                    AppLogger.log("SettlementDetailFragment", "❌ Bitmap é nulo do InputStream")
                    imageView.setImageResource(R.drawable.ic_camera)
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Erro ao carregar a foto: bitmap nulo",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                AppLogger.log("SettlementDetailFragment", "❌ Erro ao carregar foto do InputStream: ${e.message}")
                imageView.setImageResource(R.drawable.ic_camera)
                android.widget.Toast.makeText(
                    requireContext(),
                    "Erro ao carregar a foto: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }

            // TextView para informações da foto
            val textView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
                text = "Foto do Relógio Final"
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnSurface))
            }

            // TextView para data da foto
            val dataTextView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 8
                }
                text = dataFoto?.let {
                    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR"))
                    "Capturada em: ${dateFormat.format(it)}"
                } ?: "Data não disponível"
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }

            // Adicionar views ao layout
            layout.addView(textView)
            layout.addView(imageView)
            layout.addView(dataTextView)

            // Configurar diálogo
            dialog.setView(layout)
            dialog.setButton(
                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE,
                "Fechar"
            ) { _, _ -> dialog.dismiss() }

            // Mostrar diálogo
            dialog.show()

        } catch (e: Exception) {
            AppLogger.log("SettlementDetailFragment", "Erro ao criar diálogo do InputStream: ${e.message}")
            android.widget.Toast.makeText(
                requireContext(),
                "Erro ao exibir foto: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 