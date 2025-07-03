package com.example.gestaobilhares.ui.settlement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentSettlementBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import com.example.gestaobilhares.ui.settlement.MesasAcertoAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.LinearLayout
import android.widget.Toast
import com.example.gestaobilhares.data.entities.Mesa
import android.util.Log
import com.example.gestaobilhares.ui.settlement.MesaDTO
import com.example.gestaobilhares.ui.clients.AcertoResumo
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Fragment para registrar novos acertos
 * FASE 4A - Implementação crítica do core business
 */
@AndroidEntryPoint
class SettlementFragment : Fragment() {

    private var _binding: FragmentSettlementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettlementViewModel by viewModels()
    private val args: SettlementFragmentArgs by navArgs()
    
    private val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private lateinit var mesasAcertoAdapter: MesasAcertoAdapter
    private var paymentValues: MutableMap<String, Double> = mutableMapOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettlementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("SettlementFragment", "=== INICIANDO SETTLEMENT FRAGMENT ===")
        Log.d("SettlementFragment", "Cliente ID: ${args.clienteId}")
        
        // Primeiro: verificar permissões
        verificarPermissaoAcerto()
        
        // Segundo: configurar observers
        observeViewModel()
        
        // Terceiro: carregar dados do cliente PRIMEIRO (crítico para comissão)
        carregarDadosClienteESincronizar()
        
        // Quarto: configurar UI básica
        configurarUIBasica()
        
        // Quinto: buscar débito anterior
        viewModel.buscarDebitoAnterior(args.clienteId)
        
        // Sexto: carregar dados básicos do cliente para header
        viewModel.loadClientForSettlement(args.clienteId)
    }

    private fun verificarPermissaoAcerto() {
        // TODO: Implementar verificação de status da rota
        // Por enquanto, sempre permitir (será integrado com ClientListViewModel)
        val podeAcertar = true // viewModel.podeRealizarAcerto()
        
        if (!podeAcertar) {
            mostrarAlertaRotaNaoIniciada()
            return
        }
    }

    private fun mostrarAlertaRotaNaoIniciada() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Acerto Não Permitido")
            .setMessage("Para realizar acertos, a rota deve estar com status 'Em Andamento'. Inicie a rota primeiro na tela de clientes.")
            .setPositiveButton("Entendi") { _, _ ->
                findNavController().popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    private fun carregarDadosClienteESincronizar() {
        Log.d("SettlementFragment", "Iniciando carregamento sincronizado dos dados do cliente")
        
        viewModel.carregarDadosCliente(args.clienteId) { cliente ->
            if (cliente != null) {
                Log.d("SettlementFragment", "✅ Cliente carregado: valorFicha=${cliente.valorFicha}, comissaoFicha=${cliente.comissaoFicha}")
                
                // Agora que temos os dados do cliente, preparar as mesas
                lifecycleScope.launch {
                    try {
                        // Carregar mesas do cliente através do ViewModel
                        viewModel.loadMesasCliente(args.clienteId)
                        
                        // ✅ CORREÇÃO: Usar timeout para evitar "job was canceled"
                        val mesasCliente = withTimeoutOrNull(5000) {
                            viewModel.mesasCliente.first { it.isNotEmpty() }
                        }
                        
                        if (mesasCliente != null && mesasCliente.isNotEmpty()) {
                            Log.d("SettlementFragment", "✅ Mesas do cliente carregadas: ${mesasCliente.size}")
                    
                            // Preparar mesas para acerto
                            val mesasPreparadas = viewModel.prepararMesasParaAcerto(mesasCliente)
                            
                            // Converter para DTO com dados do cliente já carregados
                            val mesasDTO = mesasPreparadas.map { mesa ->
                                MesaDTO(
                                    id = mesa.id,
                                    numero = mesa.numero,
                                    tipoMesa = mesa.tipoMesa.name,
                                    tamanho = mesa.tamanho.name,
                                    estadoConservacao = mesa.estadoConservacao.name,
                                    fichasInicial = mesa.fichasInicial,
                                    fichasFinal = mesa.fichasFinal,
                                    valorFixo = mesa.valorFixo,
                                    valorFicha = cliente.valorFicha,  // ✅ Dados do cliente
                                    comissaoFicha = cliente.comissaoFicha,  // ✅ Dados do cliente
                                    ativa = mesa.ativa
                                )
                            }
                            
                            Log.d("SettlementFragment", "MesasDTO criadas com sucesso: ${mesasDTO.size}")
                            mesasDTO.forEach { mesa ->
                                Log.d("SettlementFragment", "Mesa ${mesa.numero}: valorFicha=${mesa.valorFicha}, comissaoFicha=${mesa.comissaoFicha}")
                            }
                            
                            // Configurar RecyclerView com dados completos
                            setupRecyclerViewComDados(mesasDTO)
                            
                        } else {
                            Log.w("SettlementFragment", "⚠️ Timeout ou nenhuma mesa encontrada, tentando carregar dados básicos...")
                            // Fallback: tentar carregar mesas diretamente sem aguardar Flow
                            carregarMesasFallback(cliente)
                        }
                        
                    } catch (e: Exception) {
                        Log.e("SettlementFragment", "❌ Erro ao carregar mesas: ${e.message}", e)
                        // Fallback em caso de erro
                        carregarMesasFallback(cliente)
                    }
                }
            } else {
                Log.e("SettlementFragment", "❌ Erro: Cliente não encontrado")
                Toast.makeText(requireContext(), "Erro: Cliente não encontrado", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * ✅ FUNÇÃO FALLBACK: Carrega mesas quando o Flow falha
     */
    private suspend fun carregarMesasFallback(cliente: com.example.gestaobilhares.data.entities.Cliente) {
        try {
            Log.d("SettlementFragment", "🔄 Executando fallback para carregar mesas...")
            
            // Usar repositório diretamente através do ViewModel
            val mesasCliente = viewModel.carregarMesasClienteDireto(args.clienteId)
            
            if (mesasCliente.isNotEmpty()) {
                Log.d("SettlementFragment", "✅ Fallback: ${mesasCliente.size} mesas carregadas")
                
                val mesasDTO = mesasCliente.map { mesa ->
                    MesaDTO(
                        id = mesa.id,
                        numero = mesa.numero,
                        tipoMesa = mesa.tipoMesa.name,
                        tamanho = mesa.tamanho.name,
                        estadoConservacao = mesa.estadoConservacao.name,
                        fichasInicial = mesa.fichasInicial ?: 0,
                        fichasFinal = mesa.fichasFinal ?: 0,
                        valorFixo = mesa.valorFixo,
                        valorFicha = cliente.valorFicha,
                        comissaoFicha = cliente.comissaoFicha,
                        ativa = mesa.ativa
                    )
                }
                
                setupRecyclerViewComDados(mesasDTO)
            } else {
                Log.e("SettlementFragment", "❌ Fallback: Nenhuma mesa encontrada")
                Toast.makeText(requireContext(), "Cliente não possui mesas para acerto", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("SettlementFragment", "❌ Erro no fallback: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao carregar dados: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun configurarUIBasica() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnSaveSettlement.setOnClickListener {
            salvarAcertoComCamposExtras()
        }
        setupPaymentMethod()
        setupCalculationListeners()
        binding.tvRepresentante.text = "Administrador"
        binding.cbPanoTrocado.setOnCheckedChangeListener { _, isChecked ->
            binding.etNumeroPano.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        // ✅ Bloquear edição manual do campo Valor Recebido
        binding.etAmountReceived.isFocusable = false
        binding.etAmountReceived.isClickable = false
        binding.etAmountReceived.isLongClickable = false
        binding.etAmountReceived.keyListener = null
    }
    
    private fun setupRecyclerViewComDados(mesasDTO: List<MesaDTO>) {
        Log.d("SettlementFragment", "=== CONFIGURANDO RECYCLERVIEW COM DADOS COMPLETOS ===")
        
        mesasAcertoAdapter = MesasAcertoAdapter {
            Log.d("SettlementFragment", "Callback onDataChanged acionado - recalculando totais")
            updateCalculations()
        }
        
        // Configurar RecyclerView
        binding.rvMesasAcerto.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = mesasAcertoAdapter
            setHasFixedSize(false)
        }
        
        // Popular adapter com dados completos
        Log.d("SettlementFragment", "Populando adapter com ${mesasDTO.size} mesas com dados completos")
        mesasAcertoAdapter.submitList(mesasDTO)
        
        // Forçar atualização dos cálculos após popular
            binding.rvMesasAcerto.post {
                updateCalculations()
            Log.d("SettlementFragment", "✅ RecyclerView configurado e cálculos atualizados")
        }
    }
    
    private fun setupCalculationListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCalculations()
            }
        }
        
        binding.etDesconto.addTextChangedListener(watcher)
        // binding.etAmountReceived.removeTextChangedListener(watcher) // Não adicionar watcher!
    }
    
    private fun updateCalculations() {
        try {
            val desconto = binding.etDesconto.text.toString().toDoubleOrNull() ?: 0.0
            val valorRecebido = binding.etAmountReceived.text.toString().toDoubleOrNull() ?: 0.0

            // O subtotal agora vem diretamente do adapter, que soma os subtotais de todas as mesas
            val subtotalMesas = mesasAcertoAdapter.getSubtotal()
            
            // Usar o débito anterior carregado do ViewModel
            val debitoAnterior = viewModel.debitoAnterior.value
            val totalComDebito = subtotalMesas + debitoAnterior
            val totalComDesconto = maxOf(0.0, totalComDebito - desconto)
            
            // ✅ FÓRMULA CORRIGIDA: Débito atual = Valor total - Valor recebido
            val debitoAtual = totalComDesconto - valorRecebido
            
            Log.d("SettlementFragment", "=== CÁLCULOS ATUALIZADOS ===")
            Log.d("SettlementFragment", "Subtotal mesas: R$ $subtotalMesas")
            Log.d("SettlementFragment", "Débito anterior: R$ $debitoAnterior")
            Log.d("SettlementFragment", "Total com débito: R$ $totalComDebito")
            Log.d("SettlementFragment", "Desconto: R$ $desconto")
            Log.d("SettlementFragment", "Total com desconto: R$ $totalComDesconto")
            Log.d("SettlementFragment", "Valor recebido: R$ $valorRecebido")
            Log.d("SettlementFragment", "✅ DÉBITO ATUAL: R$ $debitoAtual")
            
            // Atualizar displays dos totais
            binding.tvTableTotal.text = formatter.format(subtotalMesas)
            binding.tvTotalWithDebt.text = formatter.format(totalComDesconto) // Mostrar valor total final
            binding.tvCurrentDebt.text = formatter.format(debitoAtual)
            
        } catch (e: Exception) {
            Log.e("UpdateCalculations", "Erro ao calcular totais", e)
            binding.tvTableTotal.text = formatter.format(0.0)
            binding.tvTotalWithDebt.text = formatter.format(0.0)
            binding.tvCurrentDebt.text = formatter.format(0.0)
        }
    }
    
    private fun setupPaymentMethod() {
        val paymentMethods = arrayOf("Dinheiro", "PIX", "Cartão Débito", "Cartão Crédito", "Transferência")
        binding.actvPaymentMethod.keyListener = null // Impede digitação manual
        binding.actvPaymentMethod.setOnClickListener {
            showPaymentMethodsDialog(paymentMethods)
        }
    }

    private fun showPaymentMethodsDialog(paymentMethods: Array<String>) {
        val checkedItems = BooleanArray(paymentMethods.size) { false }
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Selecione os métodos de pagamento")
            .setMultiChoiceItems(paymentMethods, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("OK") { _, _ ->
                val selected = paymentMethods.filterIndexed { idx, _ -> checkedItems[idx] }
                if (selected.isNotEmpty()) {
                    // SEMPRE mostrar diálogo de valores, mesmo para um método
                    showPaymentValuesDialog(selected)
                } else {
                    paymentValues.clear()
                    binding.actvPaymentMethod.setText("", false)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPaymentValuesDialog(selected: List<String>) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }
        
        // Adicionar título explicativo
        val titleText = TextView(requireContext()).apply {
            text = if (selected.size == 1) {
                "Informe o valor recebido"
            } else {
                "Informe o valor de cada método"
            }
            textSize = 16f
            setTextColor(resources.getColor(com.google.android.material.R.color.material_on_surface_emphasis_high_type, null))
            setPadding(0, 0, 0, 16)
        }
        layout.addView(titleText)
        
        val editTexts = selected.associateWith { metodo ->
            EditText(requireContext()).apply {
                hint = if (selected.size == 1) {
                    "Valor recebido"
                } else {
                    "Valor para $metodo"
                }
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                layout.addView(this)
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Métodos de Pagamento")
            .setView(layout)
            .setPositiveButton("OK") { _, _ ->
                paymentValues.clear()
                var totalInformado = 0.0
                selected.forEach { metodo ->
                    val valor = editTexts[metodo]?.text.toString().toDoubleOrNull() ?: 0.0
                    paymentValues[metodo] = valor
                    totalInformado += valor
                }
                
                // Atualizar texto do campo de método de pagamento
                val resumo = if (selected.size == 1) {
                    selected[0]
                } else {
                    paymentValues.entries.joinToString(", ") { "${it.key}: R$ %.2f".format(it.value) }
                }
                binding.actvPaymentMethod.setText(resumo, false)
                
                // Atualiza o campo Valor Recebido com a soma
                binding.etAmountReceived.setText(String.format("%.2f", totalInformado))
                
                // ✅ CORREÇÃO: Recálculo imediato sem post{}
                updateCalculations()
                
                Log.d("SettlementFragment", "Métodos de pagamento atualizados - Total: R$ $totalInformado")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun salvarAcertoComCamposExtras() {
        // Impedir múltiplos cliques
        if (viewModel.isLoading.value) {
            Log.d("SettlementFragment", "Já está salvando, ignorando clique adicional")
            return
        }
        // Desabilitar botão imediatamente
        binding.btnSaveSettlement.isEnabled = false
        viewModel.setLoading(true)
        
        if (!mesasAcertoAdapter.isDataValid()) {
            Toast.makeText(requireContext(), "Verifique os valores das mesas. O relógio final deve ser maior ou igual ao inicial.", Toast.LENGTH_LONG).show()
            return
        }

        val valorRecebido = binding.etAmountReceived.text.toString().toDoubleOrNull() ?: 0.0
        val desconto = binding.etDesconto.text.toString().toDoubleOrNull() ?: 0.0
        val observacao = "" // Ajuste: campo removido do layout
        val panoTrocado = binding.cbPanoTrocado.isChecked
        val numeroPano = if (panoTrocado) binding.etNumeroPano.text.toString() else null
        val tipoAcerto = "Presencial" // Ajuste: campo removido do layout
        val representante = binding.tvRepresentante.text.toString()

        val mesasDoAcerto = mesasAcertoAdapter.getMesasAcerto().mapIndexed { idx, mesaState ->
            // Buscar a mesa original para obter o valorFixo
            val mesaOriginal = args.mesasCliente?.find { it.id == mesaState.mesaId }
            Mesa(
                id = mesaState.mesaId,
                numero = (idx + 1).toString(),
                fichasInicial = mesaState.relogioInicial,
                fichasFinal = mesaState.relogioFinal,
                valorFixo = mesaOriginal?.valorFixo ?: 0.0,
                tipoMesa = com.example.gestaobilhares.data.entities.TipoMesa.SINUCA
            )
        }

        val dadosAcerto = SettlementViewModel.DadosAcerto(
            mesas = mesasDoAcerto,
            representante = representante,
            panoTrocado = panoTrocado,
            numeroPano = numeroPano,
            tipoAcerto = tipoAcerto,
            observacao = observacao,
            justificativa = null,
            metodosPagamento = paymentValues
        )

        Log.d("SettlementFragment", "Iniciando salvamento do acerto...")
        viewModel.salvarAcerto(
            clienteId = args.clienteId,
            dadosAcerto = dadosAcerto,
            metodosPagamento = paymentValues
        )
    }

    private fun observeViewModel() {
        // Observer para dados do cliente
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.clientName.collect { nome ->
                binding.tvClientName.text = nome
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.clientAddress.collect { endereco ->
                binding.tvClientAddress.text = endereco
            }
        }
        
        // Observer para débito anterior
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.debitoAnterior.collect { debito ->
                val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
                binding.tvPreviousDebt.text = formatter.format(debito)
            }
        }
        
        // Observer para resultado do salvamento - CRÍTICO PARA O DIÁLOGO
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resultadoSalvamento.collect { resultado ->
                binding.btnSaveSettlement.isEnabled = true
                viewModel.setLoading(false)
                resultado?.let {
                    val acertoId = it.getOrNull() ?: return@let
                    mostrarDialogoResumoComAcerto(acertoId)
                }
            }
        }
    }

    private fun mostrarDialogoResumoComAcerto(acertoId: Long) {
        lifecycleScope.launch {
            val acerto = viewModel.buscarAcertoPorId(acertoId)
            if (acerto != null) {
                val mesas = viewModel.buscarMesasDoAcerto(acerto.id)
                val metodosPagamento: Map<String, Double> = acerto.metodosPagamentoJson?.let {
                    Gson().fromJson(it, object : TypeToken<Map<String, Double>>() {}.type)
                } ?: emptyMap()
                val dialog = SettlementSummaryDialog.newInstance(
                    clienteNome = viewModel.clientName.value,
                    mesas = mesas.map { mesa ->
                        Mesa(
                            id = mesa.mesaId,
                            numero = mesa.mesaId.toString(),
                            fichasInicial = mesa.relogioInicial,
                            fichasFinal = mesa.relogioFinal,
                            valorFixo = mesa.valorFixo,
                            tipoMesa = com.example.gestaobilhares.data.entities.TipoMesa.SINUCA
                        )
                    },
                    total = acerto.valorTotal,
                    metodosPagamento = metodosPagamento,
                    observacao = acerto.observacoes,
                    debitoAtual = acerto.debitoAtual
                )
                dialog.acertoCompartilhadoListener = object : SettlementSummaryDialog.OnAcertoCompartilhadoListener {
                    override fun onAcertoCompartilhado() {
                        // Voltar para tela Detalhes do Cliente
                        findNavController().popBackStack(R.id.clientDetailFragment, false)
                    }
                }
                dialog.show(parentFragmentManager, "SettlementSummaryDialog")
            } else {
                Toast.makeText(requireContext(), "Erro ao carregar acerto salvo", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 🎯 FUNÇÃO CRÍTICA: Mostra o diálogo de resumo do acerto
     */
    private fun mostrarDialogoResumo() {
        try {
            Log.d("SettlementFragment", "=== INICIANDO DIÁLOGO DE RESUMO ===")
            
            val clienteNome = binding.tvClientName.text.toString()
            val valorRecebido = binding.etAmountReceived.text.toString().toDoubleOrNull() ?: 0.0
            val desconto = binding.etDesconto.text.toString().toDoubleOrNull() ?: 0.0
            val debitoAtual = binding.tvCurrentDebt.text.toString()
                .replace("R$", "").replace(".", "").replace(",", ".")
                .trim().toDoubleOrNull() ?: 0.0
            val observacao = "Acerto realizado via app"
            
            // Calcular valor total real do acerto
            val subtotalMesas = mesasAcertoAdapter.getSubtotal()
            val valorTotalAcerto = subtotalMesas - desconto
            
            Log.d("SettlementFragment", "Dados do resumo:")
            Log.d("SettlementFragment", "- Cliente: $clienteNome")
            Log.d("SettlementFragment", "- Subtotal: R$ $subtotalMesas")
            Log.d("SettlementFragment", "- Desconto: R$ $desconto")
            Log.d("SettlementFragment", "- Total: R$ $valorTotalAcerto")
            Log.d("SettlementFragment", "- Recebido: R$ $valorRecebido")
            Log.d("SettlementFragment", "- Débito: R$ $debitoAtual")
            
            // Converter estados das mesas para o diálogo
            val mesasDoAcerto = mesasAcertoAdapter.getMesasAcerto().mapIndexed { idx, mesaState ->
                Mesa(
                    id = mesaState.mesaId,
                    numero = "Mesa ${idx + 1}",
                    fichasInicial = mesaState.relogioInicial,
                    fichasFinal = mesaState.relogioFinal,
                    valorFixo = mesaState.valorFixo,
                    tipoMesa = com.example.gestaobilhares.data.entities.TipoMesa.SINUCA
                )
            }
            
            Log.d("SettlementFragment", "Mesas do acerto: ${mesasDoAcerto.size}")
            
            // Criar e mostrar diálogo
            val dialog = SettlementSummaryDialog.newInstance(
                clienteNome = clienteNome,
                mesas = mesasDoAcerto,
                total = valorTotalAcerto,
                metodosPagamento = paymentValues,
                observacao = observacao,
                debitoAtual = debitoAtual
            )
            
            Log.d("SettlementFragment", "Mostrando diálogo...")
            dialog.show(parentFragmentManager, "SettlementSummaryDialog")
            
            // Atualizar histórico do cliente
            atualizarHistoricoCliente(valorTotalAcerto, mesasDoAcerto.size)
            
        } catch (e: Exception) {
            Log.e("SettlementFragment", "❌ Erro ao mostrar diálogo: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao exibir resumo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Atualiza o histórico de acertos do cliente
     */
    private fun atualizarHistoricoCliente(valorTotal: Double, quantidadeMesas: Int) {
        try {
            val novoAcerto = AcertoResumo(
                id = System.currentTimeMillis(),
                data = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                valorTotal = valorTotal,
                status = "Finalizado",
                mesasAcertadas = quantidadeMesas,
                debitoAtual = 0.0
            )
            
            Log.d("SettlementFragment", "Salvando acerto no histórico: $novoAcerto")
            
            // Salvar via SharedPreferences para comunicação com ClientDetailFragment
            val sharedPref = requireActivity().getSharedPreferences("acerto_temp", android.content.Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putLong("novo_acerto_id", novoAcerto.id)
                putString("novo_acerto_data", novoAcerto.data)
                putFloat("novo_acerto_valor", novoAcerto.valorTotal.toFloat())
                putString("novo_acerto_status", novoAcerto.status)
                putInt("novo_acerto_mesas", novoAcerto.mesasAcertadas)
                putLong("cliente_id", args.clienteId)
                putBoolean("acerto_salvo", true)
                apply()
            }
            
            // Navegar de volta
            findNavController().popBackStack()
            
        } catch (e: Exception) {
            Log.e("SettlementFragment", "Erro ao atualizar histórico: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
