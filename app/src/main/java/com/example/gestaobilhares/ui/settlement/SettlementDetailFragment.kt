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
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.databinding.FragmentSettlementDetailBinding
import com.example.gestaobilhares.utils.AppLogger
import com.example.gestaobilhares.utils.ReciboPrinterHelper
import com.example.gestaobilhares.utils.StringUtils
import com.example.gestaobilhares.utils.DateUtils
import com.example.gestaobilhares.R
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import com.example.gestaobilhares.utils.BluetoothPrinterHelper
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

    // ✅ REMOVIDO: Função obterValorFichaExibir() - agora usamos lógica inline idêntica ao SettlementSummaryDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel ANTES de configurar observers
        initializeViewModel()
        setupViewModelAndObservers()
        setupUI()
        loadData()
    }
    
    // ✅ REMOVIDO: Callback de permissões - agora centralizado no ReciboPrinterHelper

    private fun initializeViewModel() {
        // Inicializar ViewModel onde o contexto está disponível
        val database = AppDatabase.getDatabase(requireContext())
        val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
        viewModel = SettlementDetailViewModel(
            AcertoRepository(database.acertoDao(), database.clienteDao(), appRepository),
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
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // TODO: Mostrar loading se necessário
                if (isLoading) {
                    AppLogger.log("SettlementDetailFragment", "Carregando detalhes...")
                }
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
        // Formatação centralizada via utilitários
        // val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

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
        
        // Valores financeiros usando StringUtils
        binding.tvInitialChips.text = StringUtils.formatarMoedaComSeparadores(settlement.debitoAnterior)
        binding.tvFinalChips.text = StringUtils.formatarMoedaComSeparadores(settlement.valorTotal)
        binding.tvPlayedChips.text = StringUtils.formatarMoedaComSeparadores(settlement.valorRecebido)
        binding.tvPlayedChips.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
        binding.tvChipValue.text = StringUtils.formatarMoedaComSeparadores(settlement.desconto)
        binding.tvTotalValue.text = StringUtils.formatarMoedaComSeparadores(settlement.debitoAtual)
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
                val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
                val mesaRepository = MesaRepository(AppDatabase.getDatabase(requireContext()).mesaDao(), appRepository)
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

    // ✅ FUNCIONALIDADE UNIFICADA: Imprimir recibo usando função centralizada
    private fun imprimirRecibo(settlement: SettlementDetailViewModel.SettlementDetail) {
        lifecycleScope.launch {
            try {
                // Obter dados necessários
                val mesasCompletas = obterMesasCompletas(settlement)
                val numeroContrato = obterNumeroContrato(settlement)
                val valorFichaExibir = if (settlement.valorFicha > 0) settlement.valorFicha else if (settlement.comissaoFicha > 0) settlement.comissaoFicha else 0.0
                
                // ✅ USAR FUNÇÃO CENTRALIZADA
                ReciboPrinterHelper.imprimirReciboUnificado(
                    context = requireContext(),
                    clienteNome = settlement.clienteNome,
                    clienteCpf = settlement.clienteCpf,
                    clienteTelefone = settlement.clienteTelefone,
                    mesasCompletas = mesasCompletas,
                    debitoAnterior = settlement.debitoAnterior,
                    valorTotalMesas = settlement.valorTotal,
                    desconto = settlement.desconto,
                    metodosPagamento = settlement.metodosPagamento,
                    debitoAtual = settlement.debitoAtual,
                    observacao = settlement.observacoes,
                    valorFicha = valorFichaExibir,
                    acertoId = settlement.id,
                    numeroContrato = numeroContrato,
                    onSucesso = {
                        android.widget.Toast.makeText(requireContext(), "Recibo enviado para impressão!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onErro = { erro ->
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Erro na impressão")
                            .setMessage(erro)
                            .setPositiveButton("Tentar novamente") { _, _ ->
                                imprimirRecibo(settlement)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                )
                
            } catch (e: Exception) {
                AppLogger.log("SettlementDetailFragment", "Erro ao preparar impressão: ${e.message}")
                android.widget.Toast.makeText(requireContext(), "Erro ao preparar impressão: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // ✅ REMOVIDO: Funções duplicadas - agora usamos funções centralizadas do ReciboPrinterHelper
    
    // ✅ REMOVIDO: Funções de permissões Bluetooth - agora centralizadas no ReciboPrinterHelper

    // ✅ FUNCIONALIDADE UNIFICADA: Compartilhar via WhatsApp usando função centralizada
    private fun compartilharViaWhatsApp(settlement: SettlementDetailViewModel.SettlementDetail) {
        lifecycleScope.launch {
            try {
                // Obter dados necessários
                val mesasCompletas = obterMesasCompletas(settlement)
                val numeroContrato = obterNumeroContrato(settlement)
                val valorFichaExibir = if (settlement.valorFicha > 0) settlement.valorFicha else if (settlement.comissaoFicha > 0) settlement.comissaoFicha else 0.0
                
                // ✅ LOGS CRÍTICOS: Verificar dados antes do WhatsApp
                AppLogger.log("SettlementDetailFragment", "=== DADOS PARA WHATSAPP ===")
                AppLogger.log("SettlementDetailFragment", "Cliente Nome: '${settlement.clienteNome}'")
                AppLogger.log("SettlementDetailFragment", "Cliente CPF: '${settlement.clienteCpf}'")
                AppLogger.log("SettlementDetailFragment", "ValorFicha original: ${settlement.valorFicha}")
                AppLogger.log("SettlementDetailFragment", "ComissaoFicha original: ${settlement.comissaoFicha}")
                AppLogger.log("SettlementDetailFragment", "Valor Ficha Exibir (CORRIGIDO): $valorFichaExibir")
                AppLogger.log("SettlementDetailFragment", "Acerto ID: ${settlement.id}")
                AppLogger.log("SettlementDetailFragment", "Número Contrato: '$numeroContrato'")
                AppLogger.log("SettlementDetailFragment", "Mesas Completas: ${mesasCompletas.size}")
                
                // ✅ USAR FUNÇÃO CENTRALIZADA
                ReciboPrinterHelper.enviarWhatsAppUnificado(
                    context = requireContext(),
                    clienteNome = settlement.clienteNome,
                    clienteCpf = settlement.clienteCpf,
                    clienteTelefone = settlement.clienteTelefone,
                    mesasCompletas = mesasCompletas,
                    debitoAnterior = settlement.debitoAnterior,
                    valorTotalMesas = settlement.valorTotal,
                    desconto = settlement.desconto,
                    metodosPagamento = settlement.metodosPagamento,
                    debitoAtual = settlement.debitoAtual,
                    observacao = settlement.observacoes,
                    valorFicha = valorFichaExibir,
                    acertoId = settlement.id,
                    numeroContrato = numeroContrato,
                    onSucesso = {
                        // WhatsApp aberto com sucesso
                    },
                    onErro = { erro ->
                        android.widget.Toast.makeText(requireContext(), erro, android.widget.Toast.LENGTH_LONG).show()
                    }
                )
                
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
        val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
        val mesaRepository = MesaRepository(AppDatabase.getDatabase(requireContext()).mesaDao(), appRepository)
        val mesasCompletas = mutableListOf<Mesa>()
        
        AppLogger.log("SettlementDetailFragment", "=== BUSCANDO MESAS COMPLETAS ===")
        AppLogger.log("SettlementDetailFragment", "Total acertoMesas: ${settlement.acertoMesas.size}")
        
        for (acertoMesa in settlement.acertoMesas) {
            AppLogger.log("SettlementDetailFragment", "Buscando mesa ID: ${acertoMesa.mesaId}")
            val mesaCompleta = mesaRepository.buscarPorId(acertoMesa.mesaId)
            if (mesaCompleta != null) {
                AppLogger.log("SettlementDetailFragment", "Mesa encontrada: ${mesaCompleta.numero} (${mesaCompleta.tipoMesa})")
                val mesaComAcerto = mesaCompleta.copy(
                    relogioInicial = acertoMesa.relogioInicial,
                    relogioFinal = acertoMesa.relogioFinal
                )
                mesasCompletas.add(mesaComAcerto)
                AppLogger.log("SettlementDetailFragment", "Mesa adicionada: ${mesaComAcerto.numero} - ${mesaComAcerto.relogioInicial} → ${mesaComAcerto.relogioFinal}")
            } else {
                AppLogger.log("SettlementDetailFragment", "❌ Mesa não encontrada: ${acertoMesa.mesaId}")
            }
        }
        
        AppLogger.log("SettlementDetailFragment", "Total mesas completas: ${mesasCompletas.size}")
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
                val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
                val mesaRepository = MesaRepository(AppDatabase.getDatabase(requireContext()).mesaDao(), appRepository)
                mesaRepository.buscarPorId(mesaId)
            }
        } catch (e: Exception) {
            Log.e("SettlementDetailFragment", "Erro ao buscar mesa: ${e.message}")
            null
        }
    }

    // ✅ REMOVIDO: Funções de WhatsApp duplicadas - agora centralizadas no ReciboPrinterHelper

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
                
                // Usar o repositório já inicializado no ViewModel
                val database = AppDatabase.getDatabase(requireContext())
                val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
                val acertoRepo = AcertoRepository(
                    database.acertoDao(),
                    database.clienteDao(),
                    appRepository
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
                    AppDatabase.getDatabase(requireContext()).rotaDao(),
                    appRepository
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

            // ✅ NOVO: Verificar se é URL do Firebase Storage
            val isFirebaseUrl = caminhoFoto.startsWith("https://") && 
                                (caminhoFoto.contains("firebasestorage.googleapis.com") || 
                                 caminhoFoto.contains("firebase"))
            
            if (isFirebaseUrl) {
                AppLogger.log("SettlementDetailFragment", "⚠️ Recebida URL do Firebase Storage - isso não deveria acontecer")
                AppLogger.log("SettlementDetailFragment", "   O banco deveria ter caminho local, não URL do Firebase")
                android.widget.Toast.makeText(
                    requireContext(),
                    "Erro: Foto não disponível localmente. A foto será baixada na próxima sincronização.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            }

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
                    "Capturada em: ${DateUtils.formatarDataBrasileiraComHora(it)}"
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
                    "Capturada em: ${DateUtils.formatarDataBrasileiraComHora(it)}"
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







