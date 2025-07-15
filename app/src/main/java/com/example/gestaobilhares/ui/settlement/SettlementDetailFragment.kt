package com.example.gestaobilhares.ui.settlement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.gestaobilhares.databinding.FragmentSettlementDetailBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.AcertoMesaRepository
import java.text.NumberFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.data.repository.MesaRepository
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.R

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
        
        // Inicializar ViewModel aqui onde o contexto está disponível
        viewModel = SettlementDetailViewModel(
            AcertoRepository(AppDatabase.getDatabase(requireContext()).acertoDao()),
            AcertoMesaRepository(AppDatabase.getDatabase(requireContext()).acertoMesaDao())
        )
        
        setupUI()
        observeViewModel()
        
        // Carregar detalhes do acerto
        viewModel.loadSettlementDetails(args.acertoId)
    }

    private fun setupUI() {
        // Botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Botão editar
        binding.btnEdit.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("✏️ Editar Acerto")
                .setMessage("Funcionalidade de edição será implementada na próxima fase!\n\n🚀 Em breve você poderá:\n• Editar valores e fichas\n• Alterar status de pagamento\n• Adicionar observações")
                .setPositiveButton("OK", null)
                .show()
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

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.settlementDetails.collect { settlement ->
                settlement?.let {
                    Log.d("SettlementDetailFragment", "Detalhes carregados: $it")
                    currentSettlement = it
                    updateUI(it)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // TODO: Mostrar loading se necessário
                if (isLoading) {
                    Log.d("SettlementDetailFragment", "Carregando detalhes...")
                }
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
        
        // ✅ MELHORIA: Configurar RecyclerView com adapter melhorado
        setupMesasRecyclerView(settlement)
    }

    private fun setupMesasRecyclerView(settlement: SettlementDetailViewModel.SettlementDetail) {
        // ✅ DIAGNÓSTICO: Logs detalhados para identificar o problema
        Log.d("SettlementDetailFragment", "=== SETUP MESAS RECYCLERVIEW ===")
        Log.d("SettlementDetailFragment", "Total de mesas no settlement: ${settlement.acertoMesas.size}")
        settlement.acertoMesas.forEachIndexed { index, acertoMesa ->
            Log.d("SettlementDetailFragment", "Mesa $index: ID=${acertoMesa.mesaId}, Relógio=${acertoMesa.relogioInicial}-${acertoMesa.relogioFinal}, Subtotal=${acertoMesa.subtotal}")
        }
        
        // ✅ MELHORIA: Buscar dados completos das mesas para exibir numeração correta
        lifecycleScope.launch {
            try {
                val mesaRepository = MesaRepository(AppDatabase.getDatabase(requireContext()).mesaDao())
                val mesasCompletas = mutableMapOf<Long, AcertoMesaDetailAdapter.MesaCompleta>()
                
                Log.d("SettlementDetailFragment", "=== BUSCANDO DADOS COMPLETOS DAS MESAS ===")
                for (acertoMesa in settlement.acertoMesas) {
                    Log.d("SettlementDetailFragment", "Buscando mesa ID: ${acertoMesa.mesaId}")
                    val mesaCompleta = mesaRepository.buscarPorId(acertoMesa.mesaId)
                    if (mesaCompleta != null) {
                        Log.d("SettlementDetailFragment", "Mesa encontrada: ${mesaCompleta.numero} (${mesaCompleta.tipoMesa.name})")
                        mesasCompletas[acertoMesa.mesaId] = AcertoMesaDetailAdapter.MesaCompleta(
                            numero = mesaCompleta.numero,
                            tipo = mesaCompleta.tipoMesa.name
                        )
                    } else {
                        Log.w("SettlementDetailFragment", "Mesa não encontrada para ID: ${acertoMesa.mesaId}")
                    }
                }
                
                Log.d("SettlementDetailFragment", "=== CONFIGURANDO ADAPTER ===")
                Log.d("SettlementDetailFragment", "Mesas para adapter: ${settlement.acertoMesas.size}")
                Log.d("SettlementDetailFragment", "Mesas completas encontradas: ${mesasCompletas.size}")
                
                // Configurar adapter com dados completos
                mesaDetailAdapter = AcertoMesaDetailAdapter(
                    mesas = settlement.acertoMesas,
                    tipoAcerto = settlement.tipoAcerto,
                    panoTrocado = settlement.panoTrocado,
                    numeroPano = settlement.numeroPano,
                    mesasCompletas = mesasCompletas
                )
                
                Log.d("SettlementDetailFragment", "=== CONFIGURANDO RECYCLERVIEW ===")
                binding.rvMesasDetalhe.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = mesaDetailAdapter
                }
                
                Log.d("SettlementDetailFragment", "Adapter configurado com ${mesaDetailAdapter?.itemCount ?: 0} itens")
                
            } catch (e: Exception) {
                Log.e("SettlementDetailFragment", "Erro ao carregar dados das mesas", e)
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
                }
            }
        }
    }

    // ✅ NOVA FUNCIONALIDADE: Imprimir recibo
    private fun imprimirRecibo(settlement: SettlementDetailViewModel.SettlementDetail) {
        lifecycleScope.launch {
            try {
                val clienteRepository = ClienteRepository(AppDatabase.getDatabase(requireContext()).clienteDao())
                val mesaRepository = MesaRepository(AppDatabase.getDatabase(requireContext()).mesaDao())
                val cliente = settlement.acertoMesas.firstOrNull()?.let { acertoMesa ->
                    val mesa = mesaRepository.buscarPorId(acertoMesa.mesaId)
                    mesa?.clienteId?.let { clienteId ->
                        clienteRepository.obterPorId(clienteId)
                    }
                }
                val clienteNome = cliente?.nome ?: "Cliente não encontrado"
                // Buscar número real de cada mesa
                val mesasParaImpressao = settlement.acertoMesas.map { acertoMesa ->
                    val mesaDb = mesaRepository.buscarPorId(acertoMesa.mesaId)
                    Mesa(
                        id = acertoMesa.mesaId,
                        numero = mesaDb?.numero ?: acertoMesa.mesaId.toString(),
                        fichasInicial = acertoMesa.relogioInicial,
                        fichasFinal = acertoMesa.relogioFinal,
                        valorFixo = acertoMesa.valorFixo
                    )
                }
                val dialog = SettlementSummaryDialog.newInstance(
                    clienteNome = clienteNome,
                    mesas = mesasParaImpressao,
                    total = settlement.valorTotal,
                    metodosPagamento = settlement.metodosPagamento,
                    observacao = settlement.observacoes,
                    debitoAtual = settlement.debitoAtual,
                    debitoAnterior = settlement.debitoAnterior,
                    desconto = settlement.desconto,
                    valorTotalMesas = settlement.valorTotal - settlement.debitoAnterior
                )
                dialog.show(parentFragmentManager, "SettlementSummaryDialog")
            } catch (e: Exception) {
                Log.e("SettlementDetailFragment", "Erro ao preparar impressão", e)
                android.widget.Toast.makeText(requireContext(), "Erro ao preparar impressão", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ NOVA FUNCIONALIDADE: Compartilhar via WhatsApp
    private fun compartilharViaWhatsApp(settlement: SettlementDetailViewModel.SettlementDetail) {
        lifecycleScope.launch {
            try {
                // Buscar nome do cliente
                val clienteRepository = ClienteRepository(AppDatabase.getDatabase(requireContext()).clienteDao())
                val cliente = settlement.acertoMesas.firstOrNull()?.let { acertoMesa ->
                    val mesaRepository = MesaRepository(AppDatabase.getDatabase(requireContext()).mesaDao())
                    val mesa = mesaRepository.buscarPorId(acertoMesa.mesaId)
                    mesa?.clienteId?.let { clienteId ->
                        clienteRepository.obterPorId(clienteId)
                    }
                }
                
                val clienteNome = cliente?.nome ?: "Cliente não encontrado"
                
                // Gerar texto do resumo
                val textoResumo = gerarTextoResumo(settlement, clienteNome)
                
                // Enviar via WhatsApp
                enviarViaWhatsApp(textoResumo)
                
            } catch (e: Exception) {
                Log.e("SettlementDetailFragment", "Erro ao compartilhar via WhatsApp", e)
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
            Log.e("SettlementDetailFragment", "Erro ao enviar via WhatsApp", e)
            android.widget.Toast.makeText(requireContext(), "Erro ao abrir WhatsApp", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 