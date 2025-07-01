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
        
        // Receber as mesas do cliente via SafeArgs
        val mesasDTO = args.mesasCliente?.toList() ?: emptyList()
        Log.d("SettlementFragment", "=== INÍCIO CARREGAMENTO MESAS ===")
        Log.d("SettlementFragment", "Args recebidos - ClienteId: ${args.clienteId}")
        Log.d("SettlementFragment", "Mesas recebidas: ${mesasDTO.size}")
        
        if (mesasDTO.isEmpty()) {
            Log.w("SettlementFragment", "ATENÇÃO: Nenhuma mesa foi recebida via argumentos!")
            Toast.makeText(requireContext(), "Erro: Nenhuma mesa encontrada para acerto.", Toast.LENGTH_LONG).show()
        } else {
            mesasDTO.forEachIndexed { index, mesa ->
                Log.d("SettlementFragment", "Mesa $index: ${mesa.numero} (ID: ${mesa.id}, Tipo: ${mesa.tipoMesa})")
            }
        }
        
        setupUI(mesasDTO)
        observeViewModel()
        viewModel.loadClientForSettlement(args.clienteId)
        
        // Carregar dados do cliente para obter valorFicha e comissaoFicha
        carregarDadosCliente(args.clienteId)
        
        // Carregar mesas do cliente
        viewModel.loadMesasCliente(args.clienteId)
        
        // Buscar débito anterior do último acerto
        viewModel.buscarDebitoAnterior(args.clienteId)
        
        // Preparar mesas para acerto (com relógios iniciais)
        lifecycleScope.launch {
            viewModel.mesasCliente.collect { mesas ->
                if (mesas.isNotEmpty()) {
                    val mesasPreparadas = viewModel.prepararMesasParaAcerto(mesas)
                    // Converter Mesa para MesaDTO
                    val mesasDTO = mesasPreparadas.map { mesa ->
                        MesaDTO(
                            id = mesa.id,
                            numero = mesa.numero,
                            fichasInicial = mesa.fichasInicial ?: 0,
                            fichasFinal = mesa.fichasFinal ?: 0,
                            tipoMesa = mesa.tipoMesa.name,
                            ativa = mesa.ativa,
                            valorFixo = mesa.valorFixo ?: 0.0,
                            valorFicha = 0.0, // Será preenchido pelo adapter
                            comissaoFicha = 0.0 // Será preenchido pelo adapter
                        )
                    }
                    mesasAcertoAdapter.updateMesas(mesasDTO)
                }
            }
        }
    }

    private fun setupUI(mesasDTO: List<MesaDTO>) {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnSaveSettlement.setOnClickListener {
            salvarAcertoComCamposExtras()
        }
        setupRecyclerView(mesasDTO)
        setupPaymentMethod()
        setupCalculationListeners()
        binding.tvRepresentante.text = "Administrador"
        binding.cbPanoTrocado.setOnCheckedChangeListener { _, isChecked ->
            binding.etNumeroPano.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupRecyclerView(mesasDTO: List<MesaDTO>) {
        Log.d("SettlementFragment", "=== CONFIGURANDO RECYCLERVIEW ===")
        
        mesasAcertoAdapter = MesasAcertoAdapter {
            Log.d("SettlementFragment", "Callback onDataChanged acionado - recalculando totais")
            updateCalculations()
        }
        
        // Configurar RecyclerView com LinearLayoutManager
        binding.rvMesasAcerto.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = mesasAcertoAdapter
            setHasFixedSize(false) // Permite altura dinâmica
        }
        
        if (mesasDTO.isNotEmpty()) {
            Log.d("SettlementFragment", "Populando adapter com ${mesasDTO.size} mesas")
            
            // Preparar mesas com relógios calculados automaticamente
            lifecycleScope.launch {
                val mesasPreparadas = viewModel.prepararMesasParaAcerto(mesasDTO.map { it.toMesa() })
                
                Log.d("SettlementFragment", "Mesas preparadas para acerto: ${mesasPreparadas.size}")
                mesasPreparadas.forEach { mesa ->
                    Log.d("SettlementFragment", "Mesa ${mesa.numero}: Relógio inicial=${mesa.fichasInicial}, Final=${mesa.fichasFinal}")
                }
                
                // Converter de volta para MesaDTO e popular adapter
                val mesasDTOPreparadas = mesasPreparadas.map { mesa ->
                    MesaDTO(
                        id = mesa.id,
                        numero = mesa.numero,
                        tipoMesa = mesa.tipoMesa.toString(),
                        valorFixo = mesa.valorFixo,
                        fichasInicial = mesa.fichasInicial ?: 0,
                        fichasFinal = mesa.fichasFinal ?: 0,
                        ativa = true,
                        valorFicha = 0.0, // Será preenchido pelo carregarDadosCliente
                        comissaoFicha = 0.0 // Será preenchido pelo carregarDadosCliente
                    )
                }
                
                mesasAcertoAdapter.submitList(mesasDTOPreparadas)
            
            // Forçar atualização inicial dos cálculos após popular o adapter
            binding.rvMesasAcerto.post {
                updateCalculations()
                Log.d("SettlementFragment", "Cálculos iniciais executados")
                }
            }
        } else {
            Log.e("SettlementFragment", "ERRO: Lista de mesas vazia - adapter não será populado")
            Toast.makeText(requireContext(), "Erro: Nenhuma mesa disponível para acerto.", Toast.LENGTH_LONG).show()
        }
        
        Log.d("SettlementFragment", "=== RECYCLERVIEW CONFIGURADO ===")
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
        binding.etAmountReceived.addTextChangedListener(watcher)
    }
    
    private fun updateCalculations() {
        try {
            val desconto = binding.etDesconto.text.toString().toDoubleOrNull() ?: 0.0
            val valorRecebido = binding.etAmountReceived.text.toString().toDoubleOrNull() ?: 0.0

            // O subtotal agora vem diretamente do adapter, que soma os subtotais de todas as mesas
            val subtotalMesas = mesasAcertoAdapter.getSubtotal()
            
            val debitoAnterior = 0.0 // TODO: Implementar busca do débito anterior
            val totalComDebito = subtotalMesas + debitoAnterior
            val totalComDesconto = maxOf(0.0, totalComDebito - desconto)
            val debitoAtual = totalComDesconto - valorRecebido
            
            // Atualizar displays dos totais
            binding.tvTableTotal.text = formatter.format(subtotalMesas)
            binding.tvTotalWithDebt.text = formatter.format(totalComDebito)
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
                
                Log.d("SettlementFragment", "Métodos de pagamento configurados: $paymentValues, Total: $totalInformado")
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
        
        // Observer para resultado do salvamento
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resultadoSalvamento.collect { resultado ->
                resultado?.let {
                    if (it.isSuccess) {
                        mostrarDialogoResumo()
                    } else {
                        Toast.makeText(requireContext(), "Erro ao salvar acerto: ${it.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /**
     * Mostra o diálogo de resumo do acerto com opções de impressão e WhatsApp
     */
    private fun mostrarDialogoResumo() {
        val clienteNome = binding.tvClientName.text.toString()
        val valorRecebido = binding.etAmountReceived.text.toString().toDoubleOrNull() ?: 0.0
        val desconto = binding.etDesconto.text.toString().toDoubleOrNull() ?: 0.0
        val debitoAtual = binding.tvCurrentDebt.text.toString().replace("R$", "").replace(",", ".").trim().toDoubleOrNull() ?: 0.0
        val observacao = "Acerto realizado via app" // Campo de observações será implementado futuramente
        
        // Calcular valor total real do acerto (subtotal das mesas)
        val subtotalMesas = mesasAcertoAdapter.getSubtotal()
        val valorTotalAcerto = subtotalMesas - desconto
        
        Log.d("SettlementFragment", "Resumo do acerto - Subtotal: $subtotalMesas, Desconto: $desconto, Total: $valorTotalAcerto, Recebido: $valorRecebido, Débito: $debitoAtual")
        
        // Converter MesaAcertoState para Mesa para o diálogo
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
        
        val dialog = SettlementSummaryDialog.newInstance(
            clienteNome = clienteNome,
            mesas = mesasDoAcerto,
            total = valorTotalAcerto, // Usar valor total do acerto, não valor recebido
            metodosPagamento = paymentValues,
            observacao = observacao,
            debitoAtual = debitoAtual // Adicionar débito atual
        )
        
        dialog.show(parentFragmentManager, "SettlementSummaryDialog")
        
        // Após mostrar o diálogo, atualizar o histórico do cliente
        atualizarHistoricoCliente(valorTotalAcerto, mesasDoAcerto.size)
    }
    
    /**
     * Atualiza o histórico de acertos do cliente via SharedViewModel ou callback
     */
    private fun atualizarHistoricoCliente(valorTotal: Double, quantidadeMesas: Int) {
        // Criar um novo resumo de acerto com ID real do banco
        val novoAcerto = AcertoResumo(
            id = System.currentTimeMillis(), // ID temporário baseado em timestamp
            data = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
            valorTotal = valorTotal,
            status = "Finalizado",
            mesasAcertadas = quantidadeMesas,
            debitoAtual = 0.0
        )
        
        Log.d("SettlementFragment", "Novo acerto criado: $novoAcerto")
        
        // Salvar resultado para ser usado quando voltar ao ClientDetailFragment
        // Usando SharedPreferences como cache temporário
        val sharedPref = requireActivity().getSharedPreferences("acerto_temp", android.content.Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putLong("novo_acerto_id", novoAcerto.id)
            putString("novo_acerto_data", novoAcerto.data)
            putFloat("novo_acerto_valor", novoAcerto.valorTotal.toFloat())
            putString("novo_acerto_status", novoAcerto.status)
            putInt("novo_acerto_mesas", novoAcerto.mesasAcertadas)
            putLong("cliente_id", args.clienteId)
            putBoolean("acerto_salvo", true) // Flag para indicar que um acerto foi salvo
            apply()
        }
        
        // Navegar de volta para ClientDetailFragment
        // O histórico será recarregado automaticamente
        findNavController().popBackStack()
    }

    private fun salvarAcertoComCamposExtras() {
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

        viewModel.salvarAcerto(
            clienteId = args.clienteId,
            dadosAcerto = dadosAcerto,
            metodosPagamento = paymentValues
        )
    }

    private fun carregarDadosCliente(clienteId: Long) {
        viewModel.carregarDadosCliente(clienteId) { cliente ->
            if (cliente != null) {
                Log.d("SettlementFragment", "Dados do cliente carregados: valorFicha=${cliente.valorFicha}, comissaoFicha=${cliente.comissaoFicha}")
                
                // Atualizar os MesaDTOs com os dados do cliente
                val mesasAtualizadas = args.mesasCliente?.map { mesa ->
                    MesaDTO(
                        id = mesa.id,
                        numero = mesa.numero,
                        tipoMesa = mesa.tipoMesa.toString(),
                        fichasInicial = mesa.fichasInicial ?: 0,
                        fichasFinal = mesa.fichasFinal ?: 0,
                        valorFixo = mesa.valorFixo ?: 0.0,
                        valorFicha = cliente.valorFicha,
                        comissaoFicha = cliente.comissaoFicha,
                        ativa = mesa.ativa
                    )
                } ?: emptyList()
                
                // Atualizar o adapter com os dados corretos
                mesasAcertoAdapter.submitList(mesasAtualizadas)
                
                // Forçar recálculo dos totais
                binding.rvMesasAcerto.post {
                    updateCalculations()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
