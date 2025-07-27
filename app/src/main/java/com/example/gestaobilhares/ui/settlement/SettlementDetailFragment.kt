package com.example.gestaobilhares.ui.settlement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.gestaobilhares.R
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel ANTES de configurar observers
        initializeViewModel()
        setupViewModelAndObservers()
        setupUI()
        loadData()
    }

    private fun initializeViewModel() {
        // Inicializar ViewModel onde o contexto está disponível
        viewModel = SettlementDetailViewModel(
            AcertoRepository(AppDatabase.getDatabase(requireContext()).acertoDao(), AppDatabase.getDatabase(requireContext()).clienteDao()),
            AcertoMesaRepository(AppDatabase.getDatabase(requireContext()).acertoMesaDao())
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
        // Botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
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
                    mesasCompletas = mesasCompletas
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
                    numeroPano = settlement.numeroPano
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
                val clienteNome = "Cliente" // Usar nome genérico por enquanto
                
                AppLogger.log("SettlementDetailFragment", "Preparando impressão para $clienteNome")
                AppLogger.log("SettlementDetailFragment", "Total de mesas: ${settlement.acertoMesas.size}")
                AppLogger.log("SettlementDetailFragment", "Valor total: R$ ${settlement.valorTotal}")
                
                // Simples - apenas mostrar toast por enquanto para testar logs
                android.widget.Toast.makeText(requireContext(), "Impressão preparada - ver logs", android.widget.Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                AppLogger.log("SettlementDetailFragment", "Erro ao preparar impressão: ${e.message}")
                android.widget.Toast.makeText(requireContext(), "Erro ao preparar impressão", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ NOVA FUNCIONALIDADE: Compartilhar via WhatsApp
    private fun compartilharViaWhatsApp(settlement: SettlementDetailViewModel.SettlementDetail) {
        lifecycleScope.launch {
            try {
                val clienteNome = "Cliente" // Usar nome genérico por enquanto
                
                // Gerar texto do resumo
                val textoResumo = gerarTextoResumo(settlement, clienteNome)
                
                // Enviar via WhatsApp
                enviarViaWhatsApp(textoResumo)
                
            } catch (e: Exception) {
                AppLogger.log("SettlementDetailFragment", "Erro ao compartilhar via WhatsApp: ${e.message}")
                android.widget.Toast.makeText(requireContext(), "Erro ao compartilhar via WhatsApp", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun gerarTextoResumo(settlement: SettlementDetailViewModel.SettlementDetail, clienteNome: String): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val texto = StringBuilder()
        
        texto.append("🎱 *ACERTO DE BILHAR*\n")
        texto.append("================================\n\n")
        texto.append("👤 *Cliente:* $clienteNome\n")
        texto.append("📅 *Data:* ${settlement.date}\n")
        texto.append("🆔 *Acerto:* #${settlement.id.toString().padStart(4, '0')}\n\n")

        texto.append("🎯 *MESAS ACERTADAS:*\n")
        var totalFichasJogadas = 0
        settlement.acertoMesas.forEach { mesa ->
            val numeroMesa = mesa.mesaId.toString()
            if (mesa.valorFixo > 0) {
                // Para valor fixo
                texto.append("• *Mesa $numeroMesa*: ${formatter.format(mesa.valorFixo)}/mês\n")
            } else {
                // Para fichas jogadas
                val fichasJogadas = mesa.relogioFinal - mesa.relogioInicial
                totalFichasJogadas += fichasJogadas
                texto.append("• *Mesa $numeroMesa*: ${mesa.relogioInicial} → ${mesa.relogioFinal} ($fichasJogadas fichas)\n")
            }
        }
        if (totalFichasJogadas > 0) {
            texto.append("\n*Total de fichas jogadas: $totalFichasJogadas*\n")
        }
        texto.append("\n")

        texto.append("💰 *RESUMO FINANCEIRO:*\n")
        if (settlement.debitoAnterior > 0) {
            texto.append("• Débito anterior: ${formatter.format(settlement.debitoAnterior)}\n")
        }
        texto.append("• Valor total: ${formatter.format(settlement.valorTotal)}\n")
        if (settlement.desconto > 0) {
            texto.append("• Desconto: ${formatter.format(settlement.desconto)}\n")
        }
        if (settlement.valorRecebido > 0) {
            texto.append("• Valor recebido: ${formatter.format(settlement.valorRecebido)}\n")
        }
        if (settlement.debitoAtual > 0) {
            texto.append("• Débito atual: ${formatter.format(settlement.debitoAtual)}\n")
        }
        texto.append("\n")

        if (settlement.metodosPagamento.isNotEmpty()) {
            texto.append("💳 *FORMA DE PAGAMENTO:*\n")
            settlement.metodosPagamento.forEach { (metodo, valor) ->
                texto.append("• $metodo: ${formatter.format(valor)}\n")
            }
            texto.append("\n")
        }

        if (settlement.observacoes.isNotBlank()) {
            texto.append("📝 *Observações:* ${settlement.observacoes}\n\n")
        }

        texto.append("--------------------------------\n")
        texto.append("✅ Acerto realizado via GestaoBilhares")
        return texto.toString()
    }

    private fun enviarViaWhatsApp(texto: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(android.content.Intent.EXTRA_TEXT, texto)
            intent.setPackage("com.whatsapp")
            
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback para qualquer app de compartilhamento
                val shareIntent = android.content.Intent.createChooser(intent, "Compartilhar via")
                startActivity(shareIntent)
            }
        } catch (e: Exception) {
            AppLogger.log("SettlementDetailFragment", "Erro ao enviar via WhatsApp: ${e.message}")
            android.widget.Toast.makeText(requireContext(), "Erro ao abrir WhatsApp", android.widget.Toast.LENGTH_SHORT).show()
        }
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
                val clienteRepo = com.example.gestaobilhares.data.repository.ClienteRepository(
                    AppDatabase.getDatabase(requireContext()).clienteDao()
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
                        val action = SettlementDetailFragmentDirections
                            .actionSettlementDetailFragmentToSettlementFragment(
                                clienteId = acertoCompleto.clienteId,
                                acertoIdParaEdicao = args.acertoId
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 