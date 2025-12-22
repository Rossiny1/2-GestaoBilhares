package com.example.gestaobilhares.ui.settlement

import android.content.Intent
import android.os.Bundle
import timber.log.Timber
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
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.ui.databinding.FragmentSettlementDetailBinding
import com.example.gestaobilhares.core.utils.ReciboPrinterHelper
import com.example.gestaobilhares.core.utils.StringUtils
import com.example.gestaobilhares.core.utils.DateUtils
import com.example.gestaobilhares.ui.R
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
// BluetoothPrinterHelper removido - usar ReciboPrinterHelper
import androidx.core.app.ActivityCompat
import java.io.File
import androidx.fragment.app.viewModels

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fragment para exibir detalhes de um acerto específico.
 * FASE 4B+ - Detalhes e edição de acertos
 */
@AndroidEntryPoint
class SettlementDetailFragment : Fragment() {

    private var _binding: FragmentSettlementDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettlementDetailViewModel by viewModels()
    
    @Inject
    lateinit var appRepository: AppRepository

    private val args: SettlementDetailFragmentArgs by navArgs()

    private var mesaDetailAdapter: AcertoMesaDetailAdapter? = null
    private var currentSettlement: SettlementDetailViewModel.SettlementDetail? = null
    
    // ✅ NOVO: Callback para retry após permissões concedidas
    var pendingPrintCallback: (() -> Unit)? = null
        private set
    val REQUEST_BLUETOOTH_PERMISSIONS = 1001
    val bluetoothPermissions = arrayOf(
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_SCAN
    )
    
    /**
     * ✅ NOVO: Define callback para ser executado após permissões concedidas
     */
    fun setPendingPrintCallback(callback: () -> Unit) {
        pendingPrintCallback = callback
    }

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
        // initializeViewModel() -> Hilt initializes it
        setupViewModelAndObservers()
        setupUI()
        loadData()
        
        // ✅ REMOVIDO: setupBackButtonHandler() - Navigation Component gerencia automaticamente
    }
    
    // ✅ NOVO: Tratamento de permissões Bluetooth para impressão
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                // Permissões concedidas - executar callback pendente
                pendingPrintCallback?.invoke()
                pendingPrintCallback = null
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Permissões Bluetooth necessárias para impressão",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                pendingPrintCallback = null
            }
        }
    }

    // @Suppress("DEPRECATION") -> Not needed anymore
    // private fun initializeViewModel() { ... } -> Removed

    private fun setupViewModelAndObservers() {
        // Observer para dados do acerto
        viewModel.settlementDetail.observe(viewLifecycleOwner) { settlement ->
            settlement?.let {
                Timber.d("SettlementDetailFragment", "Detalhes carregados: $it")
                currentSettlement = it
                updateUI(it)
            }
        }
        
        // Observer para loading
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // TODO: Mostrar loading se necessário
                if (isLoading) {
                    Timber.d("SettlementDetailFragment", "Carregando detalhes...")
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
                    Timber.e("SettlementDetailFragment", "Erro ao voltar: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Timber.e("SettlementDetailFragment", "Erro ao configurar UI: ${e.message}")
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
            else -> binding.tvSettlementStatus.setTextColor(ContextCompat.getColor(requireContext(), com.example.gestaobilhares.ui.R.color.colorOnSurface))
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
        Timber.d("SettlementDetailFragment", "=== CONFIGURANDO RECYCLERVIEW DAS MESAS ===")
        Timber.d("SettlementDetailFragment", "Acerto ID: ${settlement.id}")
        Timber.d("SettlementDetailFragment", "Total mesas no settlement: ${settlement.acertoMesas.size}")
        Timber.d("SettlementDetailFragment", "Total mesas esperadas (totalMesas): ${settlement.totalMesas}")
        
        // ✅ VERIFICAÇÃO CRÍTICA: Se não há mesas, isso é um problema
        if (settlement.acertoMesas.isEmpty()) {
            Timber.d("SettlementDetailFragment", "❌ PROBLEMA CRÍTICO: settlement.acertoMesas está vazio!")
            Timber.d("SettlementDetailFragment", "Isso indica problema na busca das mesas no banco de dados")
            return
        }
        
        settlement.acertoMesas.forEachIndexed { index, acertoMesa ->
            Timber.d("SettlementDetailFragment", "=== MESA ${index + 1} PARA EXIBIÇÃO ===")
            Timber.d("SettlementDetailFragment", "Mesa ID: ${acertoMesa.mesaId}")
            Timber.d("SettlementDetailFragment", "Acerto ID: ${acertoMesa.acertoId}")
            Timber.d("SettlementDetailFragment", "Relógio: ${acertoMesa.relogioInicial} → ${acertoMesa.relogioFinal}")
            Timber.d("SettlementDetailFragment", "Fichas jogadas: ${acertoMesa.fichasJogadas}")
            Timber.d("SettlementDetailFragment", "Valor fixo: R$ ${acertoMesa.valorFixo}")
            Timber.d("SettlementDetailFragment", "Subtotal: R$ ${acertoMesa.subtotal}")
            Timber.d("SettlementDetailFragment", "Com defeito: ${acertoMesa.comDefeito}")
            Timber.d("SettlementDetailFragment", "Relógio reiniciou: ${acertoMesa.relogioReiniciou}")
        }
        
        // ✅ MELHORIA: Buscar dados completos das mesas para exibir numeração correta
        lifecycleScope.launch {
            try {
                // val appRepository - USAR INJECTED (Já injetado via Hilt)
                val mesasCompletas = mutableMapOf<Long, AcertoMesaDetailAdapter.MesaCompleta>()
                
                Timber.d("SettlementDetailFragment", "=== BUSCANDO DADOS COMPLETOS DAS MESAS ===")
                for (acertoMesa in settlement.acertoMesas) {
                    Timber.d("SettlementDetailFragment", "Buscando mesa ID: ${acertoMesa.mesaId}")
                    val mesaCompleta = appRepository.obterMesaPorId(acertoMesa.mesaId)
                    if (mesaCompleta != null) {
                        Timber.d("SettlementDetailFragment", "Mesa encontrada: ${mesaCompleta.numero} (${mesaCompleta.tipoMesa.name})")
                        mesasCompletas[acertoMesa.mesaId] = AcertoMesaDetailAdapter.MesaCompleta(
                            numero = mesaCompleta.numero,
                            tipo = mesaCompleta.tipoMesa.name
                        )
                    } else {
                        Timber.d("SettlementDetailFragment", "Mesa não encontrada para ID: ${acertoMesa.mesaId}")
                    }
                }
                
                Timber.d("SettlementDetailFragment", "=== CONFIGURANDO ADAPTER ===")
                Timber.d("SettlementDetailFragment", "Mesas para adapter: ${settlement.acertoMesas.size}")
                Timber.d("SettlementDetailFragment", "Mesas completas encontradas: ${mesasCompletas.size}")
                
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
                            Timber.e("SettlementDetailFragment", "Erro ao visualizar foto: ${e.message}")
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Erro ao visualizar foto: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                
                Timber.d("SettlementDetailFragment", "=== CONFIGURANDO RECYCLERVIEW ===")
                binding.rvMesasDetalhe.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = mesaDetailAdapter
                    // ✅ CORREÇÃO OFICIAL: Configuração para NestedScrollView
                    isNestedScrollingEnabled = false
                    setHasFixedSize(false)
                }
                
                Timber.d("SettlementDetailFragment", "Adapter configurado com ${mesaDetailAdapter?.itemCount ?: 0} itens")
                
            } catch (e: Exception) {
                Timber.d("SettlementDetailFragment", "Erro ao carregar dados das mesas: ${e.message}")
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
                            Timber.e("SettlementDetailFragment", "Erro ao visualizar foto: ${e.message}")
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
                
                // ✅ CORREÇÃO: Passar Fragment para permitir solicitação de permissões
                ReciboPrinterHelper.imprimirReciboUnificado(
                    context = this@SettlementDetailFragment, // Passar Fragment em vez de Context
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
                Timber.d("SettlementDetailFragment", "Erro ao preparar impressão: ${e.message}")
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
                Timber.d("SettlementDetailFragment", "=== DADOS PARA WHATSAPP ===")
                Timber.d("SettlementDetailFragment", "Cliente Nome: '${settlement.clienteNome}'")
                Timber.d("SettlementDetailFragment", "Cliente CPF: '${settlement.clienteCpf}'")
                Timber.d("SettlementDetailFragment", "ValorFicha original: ${settlement.valorFicha}")
                Timber.d("SettlementDetailFragment", "ComissaoFicha original: ${settlement.comissaoFicha}")
                Timber.d("SettlementDetailFragment", "Valor Ficha Exibir (CORRIGIDO): $valorFichaExibir")
                Timber.d("SettlementDetailFragment", "Acerto ID: ${settlement.id}")
                Timber.d("SettlementDetailFragment", "Número Contrato: '$numeroContrato'")
                Timber.d("SettlementDetailFragment", "Mesas Completas: ${mesasCompletas.size}")
                
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
                Timber.d("SettlementDetailFragment", "Erro ao compartilhar via WhatsApp: ${e.message}")
                android.widget.Toast.makeText(requireContext(), "Erro ao compartilhar via WhatsApp", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * ✅ MÉTODO CENTRALIZADO: Obtém mesas completas (FONTE ÚNICA DE VERDADE)
     */
    private suspend fun obterMesasCompletas(settlement: SettlementDetailViewModel.SettlementDetail): List<Mesa> {
        // val appRepository - USAR INJECTED (Já injetado via Hilt)
        val mesasCompletas = mutableListOf<Mesa>()
        
        Timber.d("SettlementDetailFragment", "=== BUSCANDO MESAS COMPLETAS ===")
        Timber.d("SettlementDetailFragment", "Total acertoMesas: ${settlement.acertoMesas.size}")
        
        for (acertoMesa in settlement.acertoMesas) {
            Timber.d("SettlementDetailFragment", "Buscando mesa ID: ${acertoMesa.mesaId}")
            val mesaCompleta = appRepository.obterMesaPorId(acertoMesa.mesaId)
            if (mesaCompleta != null) {
                Timber.d("SettlementDetailFragment", "Mesa encontrada: ${mesaCompleta.numero} (${mesaCompleta.tipoMesa})")
                val mesaComAcerto = mesaCompleta.copy(
                    relogioInicial = acertoMesa.relogioInicial,
                    relogioFinal = acertoMesa.relogioFinal
                )
                mesasCompletas.add(mesaComAcerto)
                Timber.d("SettlementDetailFragment", "Mesa adicionada: ${mesaComAcerto.numero} - ${mesaComAcerto.relogioInicial} → ${mesaComAcerto.relogioFinal}")
            } else {
                Timber.d("SettlementDetailFragment", "❌ Mesa não encontrada: ${acertoMesa.mesaId}")
            }
        }
        
        Timber.d("SettlementDetailFragment", "Total mesas completas: ${mesasCompletas.size}")
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
        Timber.d("SettlementDetailFragment", "=== BUSCA CONTRATO ===")
        Timber.d("SettlementDetailFragment", "Acerto ID: ${settlement.id}")
        Timber.d("SettlementDetailFragment", "Cliente ID: ${acertoCompleto?.clienteId}")
        Timber.d("SettlementDetailFragment", "Contrato encontrado: ${contratoAtivo != null}")
        Timber.d("SettlementDetailFragment", "Número do contrato: ${contratoAtivo?.numeroContrato}")
        return contratoAtivo?.numeroContrato
    }


    /**
     * ✅ FASE 12.5: Busca informações completas da mesa (versão suspend - removido runBlocking)
     */
    private suspend fun buscarMesaCompleta(mesaId: Long): Mesa? {
        return try {
            // val appRepository - USAR INJECTED (Já injetado via Hilt)
            appRepository.obterMesaPorId(mesaId)
        } catch (e: Exception) {
            Timber.e("SettlementDetailFragment", "Erro ao buscar mesa: ${e.message}")
            null
        }
    }

    // ✅ REMOVIDO: Funções de WhatsApp duplicadas - agora centralizadas no ReciboPrinterHelper

    /**
     * ✅ NOVA FUNCIONALIDADE: Verifica se o acerto pode ser editado
     * Usa AppRepository em vez de repositórios deprecated
     */
    private sealed class PermissaoEdicao {
        object Permitido : PermissaoEdicao()
        object AcertoNaoEncontrado : PermissaoEdicao()
        data class CicloInativo(val motivo: String) : PermissaoEdicao()
        data class NaoEhUltimoAcerto(val motivo: String) : PermissaoEdicao()
        data class ErroValidacao(val motivo: String) : PermissaoEdicao()
    }
    
    private fun verificarPermissaoEdicao() {
        lifecycleScope.launch {
            try {
                Timber.d("SettlementDetailFragment", "=== VERIFICANDO PERMISSÃO DE EDIÇÃO ===")
                Timber.d("SettlementDetailFragment", "Acerto ID: ${args.acertoId}")
                
                // Mostrar loading
                binding.btnEdit.isEnabled = false
                
                // Usar AppRepository em vez de repositórios deprecated
                // val appRepository - USAR INJECTED (Já injetado via Hilt)
                
                // Verificar permissão usando AppRepository
                val permissao = verificarPermissaoEdicaoComAppRepository(appRepository, args.acertoId)
                
                when (permissao) {
                    is PermissaoEdicao.Permitido -> {
                        Timber.d("SettlementDetailFragment", "✅ Edição permitida. Navegando para tela de edição...")
                        navegarParaEdicao()
                    }
                    is PermissaoEdicao.CicloInativo -> {
                        mostrarDialogoPermissaoNegada(
                            "Ciclo Inativo",
                            permissao.motivo + "\n\nApenas acertos do ciclo atual podem ser editados."
                        )
                    }
                    is PermissaoEdicao.NaoEhUltimoAcerto -> {
                        mostrarDialogoPermissaoNegada(
                            "Edição Não Permitida",
                            permissao.motivo + "\n\nPara editar um acerto anterior, você deve primeiro excluir os acertos posteriores."
                        )
                    }
                    is PermissaoEdicao.AcertoNaoEncontrado -> {
                        mostrarDialogoPermissaoNegada(
                            "Erro",
                            "O acerto não foi encontrado no banco de dados."
                        )
                    }
                    is PermissaoEdicao.ErroValidacao -> {
                        mostrarDialogoPermissaoNegada(
                            "Erro de Validação",
                            "Ocorreu um erro ao validar a permissão de edição:\n${permissao.motivo}"
                        )
                    }
                }
                
            } catch (e: Exception) {
                Timber.d("SettlementDetailFragment", "❌ Erro ao verificar permissão: ${e.message}")
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
     * Reimplementa a lógica de podeEditarAcerto usando AppRepository
     * Regras: Apenas o último acerto da rota no ciclo ativo pode ser editado
     */
    private suspend fun verificarPermissaoEdicaoComAppRepository(
        appRepository: AppRepository,
        acertoId: Long
    ): PermissaoEdicao {
        return try {
            val acerto = appRepository.buscarPorId(acertoId)
            if (acerto == null) {
                Timber.d("SettlementDetailFragment", "❌ Acerto não encontrado para edição: ID=$acertoId")
                return PermissaoEdicao.AcertoNaoEncontrado
            }

            // Verificar se o ciclo está ativo
            val rotaId = acerto.rotaId ?: return PermissaoEdicao.ErroValidacao("Acerto não possui rota associada")
            val cicloAtivo = appRepository.buscarCicloAtivo(rotaId)
            if (cicloAtivo == null || cicloAtivo.id != acerto.cicloId) {
                Timber.d("SettlementDetailFragment", "❌ Ciclo não está ativo para edição: cicloId=${acerto.cicloId}, rotaId=$rotaId")
                return PermissaoEdicao.CicloInativo("O ciclo deste acerto não está mais ativo.")
            }

            // Buscar o último acerto da rota no ciclo ativo
            val acertosRota = appRepository.buscarPorRotaECicloId(rotaId, cicloAtivo.id)
            val ultimoAcerto = acertosRota.maxByOrNull { it.dataAcerto.time }
            
            if (ultimoAcerto == null || ultimoAcerto.id != acertoId) {
                Timber.d("SettlementDetailFragment", "❌ Não é o último acerto da rota. Último: ${ultimoAcerto?.id}, Solicitado: $acertoId")
                return PermissaoEdicao.NaoEhUltimoAcerto("Apenas o último acerto da rota pode ser editado.")
            }

            Timber.d("SettlementDetailFragment", "✅ Acerto pode ser editado: ID=$acertoId")
            PermissaoEdicao.Permitido
            
        } catch (e: Exception) {
            Timber.e("SettlementDetailFragment", "Erro ao verificar permissão: ${e.message}", e)
            PermissaoEdicao.ErroValidacao(e.message ?: "Erro desconhecido")
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
            Timber.d("SettlementDetailFragment", "Erro ao navegar para edição: ${e.message}")
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
            Timber.d("SettlementDetailFragment", "=== VISUALIZANDO FOTO ===")
            Timber.d("SettlementDetailFragment", "Caminho da foto: $caminhoFoto")

            // ✅ CORREÇÃO: Verificar se é URL do Firebase Storage
            val isFirebaseUrl = caminhoFoto.startsWith("https://") && 
                                (caminhoFoto.contains("firebasestorage.googleapis.com") || 
                                 caminhoFoto.contains("firebase"))
            
            // ✅ CORREÇÃO: PRIORIDADE 1 - Verificar se é arquivo local e existe
            if (!isFirebaseUrl && !caminhoFoto.startsWith("content://")) {
                val file = java.io.File(caminhoFoto)
                if (file.exists() && file.isFile) {
                    Timber.d("SettlementDetailFragment", "✅ Carregando foto local: ${file.absolutePath}")
                    mostrarFotoDialog(file, dataFoto)
                    return
                } else {
                    Timber.d("SettlementDetailFragment", "⚠️ Arquivo local não existe: ${file.absolutePath}")
                }
            }
            
            // ✅ CORREÇÃO: PRIORIDADE 2 - Se for URI content://, tentar carregar do content provider
            if (caminhoFoto.startsWith("content://")) {
                Timber.d("SettlementDetailFragment", "Tentando carregar foto do content provider...")
                try {
                    val uri = android.net.Uri.parse(caminhoFoto)
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        Timber.d("SettlementDetailFragment", "✅ Carregando foto diretamente do URI")
                        mostrarFotoDialog(inputStream, dataFoto)
                        return
                    }
                } catch (e: Exception) {
                    Timber.d("SettlementDetailFragment", "Erro ao carregar do URI: ${e.message}")
                }
                
                // Tentar converter URI para caminho real
                try {
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
                                Timber.d("SettlementDetailFragment", "Caminho real encontrado via cursor: $path")
                                val file = java.io.File(path)
                                if (file.exists()) {
                                    mostrarFotoDialog(file, dataFoto)
                                    return
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.d("SettlementDetailFragment", "Erro ao converter URI: ${e.message}")
                }
            }
            
            // ✅ CORREÇÃO: PRIORIDADE 3 - Se for URL do Firebase Storage, fazer download
            if (isFirebaseUrl) {
                Timber.d("SettlementDetailFragment", "Detectada URL do Firebase Storage, fazendo download...")
                lifecycleScope.launch {
                    try {
                        val bitmap = downloadImageFromUrl(caminhoFoto)
                        if (bitmap != null) {
                            mostrarFotoDialog(bitmap, dataFoto)
                            Timber.d("SettlementDetailFragment", "✅ Foto carregada do Firebase Storage")
                        } else {
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Erro ao carregar foto do servidor",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Timber.d("SettlementDetailFragment", "Erro ao fazer download da foto: ${e.message}")
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Erro ao carregar foto: ${e.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
                return
            }
            
            // ✅ Se chegou aqui, não conseguiu carregar a foto
            Timber.d("SettlementDetailFragment", "❌ Não foi possível carregar a foto: $caminhoFoto")
            android.widget.Toast.makeText(
                requireContext(),
                "Arquivo de foto não encontrado: $caminhoFoto",
                android.widget.Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Timber.d("SettlementDetailFragment", "Erro ao visualizar foto: ${e.message}")
            android.widget.Toast.makeText(
                requireContext(),
                "Erro ao visualizar foto: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * ✅ NOVO: Faz download de imagem de uma URL
     */
    private suspend fun downloadImageFromUrl(urlString: String): android.graphics.Bitmap? {
        return try {
            val url = java.net.URL(urlString)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doInput = true
            connection.connect()
            
            val inputStream = connection.inputStream
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            connection.disconnect()
            bitmap
        } catch (e: Exception) {
            Timber.d("SettlementDetailFragment", "Erro ao fazer download da imagem: ${e.message}")
            null
        }
    }
    
    /**
     * ✅ NOVO: Método separado para mostrar o diálogo da foto (sobrecarga com Bitmap)
     */
    private fun mostrarFotoDialog(bitmap: android.graphics.Bitmap, dataFoto: Date?) {
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
            
            imageView.setImageBitmap(bitmap)

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
                setTextColor(ContextCompat.getColor(requireContext(), com.example.gestaobilhares.ui.R.color.colorOnSurface))
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
                setTextColor(ContextCompat.getColor(requireContext(), com.example.gestaobilhares.ui.R.color.colorOnSurface))
            }

            // Adicionar views ao layout
            layout.addView(imageView)
            layout.addView(textView)
            layout.addView(dataTextView)

            // Configurar diálogo
            dialog.setView(layout)
            dialog.setTitle("Foto do Relógio")
            dialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, "Fechar") { _, _ ->
                dialog.dismiss()
            }
            dialog.show()
        } catch (e: Exception) {
            Timber.d("SettlementDetailFragment", "Erro ao mostrar diálogo da foto: ${e.message}")
            android.widget.Toast.makeText(
                requireContext(),
                "Erro ao exibir foto: ${e.message}",
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
                Timber.d("SettlementDetailFragment", "Tentando carregar foto do arquivo: ${file.absolutePath}")
                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    Timber.d("SettlementDetailFragment", "✅ Foto carregada com sucesso")
                    imageView.setImageBitmap(bitmap)
                } else {
                    Timber.d("SettlementDetailFragment", "❌ Bitmap é nulo")
                    imageView.setImageResource(com.example.gestaobilhares.ui.R.drawable.ic_camera)
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Erro ao carregar a foto: bitmap nulo",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Timber.d("SettlementDetailFragment", "❌ Erro ao carregar foto: ${e.message}")
                imageView.setImageResource(com.example.gestaobilhares.ui.R.drawable.ic_camera)
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
                setTextColor(ContextCompat.getColor(requireContext(), com.example.gestaobilhares.ui.R.color.colorOnSurface))
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
                setTextColor(ContextCompat.getColor(requireContext(), com.example.gestaobilhares.ui.R.color.text_secondary))
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
            Timber.d("SettlementDetailFragment", "Erro ao criar diálogo: ${e.message}")
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
                Timber.d("SettlementDetailFragment", "Tentando carregar foto do InputStream")
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    Timber.d("SettlementDetailFragment", "✅ Foto carregada com sucesso do InputStream")
                    imageView.setImageBitmap(bitmap)
                } else {
                    Timber.d("SettlementDetailFragment", "❌ Bitmap é nulo do InputStream")
                    imageView.setImageResource(com.example.gestaobilhares.ui.R.drawable.ic_camera)
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Erro ao carregar a foto: bitmap nulo",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Timber.d("SettlementDetailFragment", "❌ Erro ao carregar foto do InputStream: ${e.message}")
                imageView.setImageResource(com.example.gestaobilhares.ui.R.drawable.ic_camera)
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
                setTextColor(ContextCompat.getColor(requireContext(), com.example.gestaobilhares.ui.R.color.colorOnSurface))
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
                setTextColor(ContextCompat.getColor(requireContext(), com.example.gestaobilhares.ui.R.color.text_secondary))
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
            Timber.d("SettlementDetailFragment", "Erro ao criar diálogo do InputStream: ${e.message}")
            android.widget.Toast.makeText(
                requireContext(),
                "Erro ao exibir foto: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * ✅ REMOVIDO: setupBackButtonHandler() - Navigation Component gerencia automaticamente
     * O botão voltar agora é gerenciado globalmente pelo MainActivity, que mostra
     * diálogo de saída apenas na tela de rotas. Em todas as outras telas, o Navigation
     * Component gerencia o comportamento padrão de voltar para a tela anterior.
     */

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 

