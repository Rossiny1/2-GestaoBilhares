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
    
    // Data class para representar uma mesa no acerto
    data class MesaAcerto(
        val id: Int,
        val nome: String,
        var fichasInicial: Int = 0,
        var fichasFinal: Int = 0,
        var valorFicha: Double = 0.0
    )
    
    private val mesas = mutableListOf<MesaAcerto>()
    private val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private lateinit var mesasAcertoAdapter: MesasAcertoAdapter
    private var selectedPaymentMethods: List<String> = emptyList()
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
        
        Log.d("SettlementFragment", "Recebido clienteId=${args.clienteId}, mesas=${args.mesasCliente?.map { it.numero }}")
        setupUI()
        observeViewModel()
        
        // Buscar apenas nome/endereço do cliente
        viewModel.loadClientForSettlement(args.clienteId)
        mesasAcertoAdapter = MesasAcertoAdapter()
        binding.rvMesasAcerto.adapter = mesasAcertoAdapter
        // NÃO sobrescrever as mesas recebidas via argumento
        // viewModel.loadMesasCliente(args.clienteId) // REMOVIDO
        setupMesasSection()

        // Preencher campo Representante (mock)
        binding.tvRepresentante.text = "Administrador" // Trocar para nome real do usuário logado

        // Lógica do checkbox Pano trocado
        binding.cbPanoTrocado.setOnCheckedChangeListener { _, isChecked ->
            binding.etNumeroPano.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Importar mesas do cliente via SafeArgs
        val mesasCliente = args.mesasCliente?.toList() ?: emptyList()
        mesasAcertoAdapter.submitList(mesasCliente)
    }

    private fun setupUI() {
        // Botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Botão salvar acerto
        binding.btnSaveSettlement.setOnClickListener {
            salvarAcertoComCamposExtras()
        }
        
        // Configurar método de pagamento
        setupPaymentMethod()
        
        // Configurar listeners para cálculos em tempo real
        setupCalculationListeners()
        
        // Adicionar primeira mesa
        addMesa()
    }
    
    private fun setupCalculationListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCalculations()
            }
        }
        
        // Adicionar listeners nos campos que afetam os cálculos
        binding.etFichasInicial.addTextChangedListener(watcher)
        binding.etFichasFinal.addTextChangedListener(watcher)
        binding.etValorFicha.addTextChangedListener(watcher)
        binding.etDesconto.addTextChangedListener(watcher)
        binding.etAmountReceived.addTextChangedListener(watcher)
    }
    
    private fun updateCalculations() {
        try {
            // Obter valores dos campos
            val fichasInicial = binding.etFichasInicial.text.toString().toIntOrNull() ?: 0
            val fichasFinal = binding.etFichasFinal.text.toString().toIntOrNull() ?: 0
            val valorFicha = binding.etValorFicha.text.toString().toDoubleOrNull() ?: 0.0
            val desconto = binding.etDesconto.text.toString().toDoubleOrNull() ?: 0.0
            val valorRecebido = binding.etAmountReceived.text.toString().toDoubleOrNull() ?: 0.0
            
            // Calcular valores
            val fichasJogadas = maxOf(0, fichasFinal - fichasInicial)
            val subtotalMesas = fichasJogadas * valorFicha
            val debitoAnterior = 0.0 // TODO: Implementar busca do débito anterior
            val totalComDebito = subtotalMesas + debitoAnterior
            val totalComDesconto = maxOf(0.0, totalComDebito - desconto)
            val debitoAtual = totalComDesconto - valorRecebido
            
            // Atualizar subtotal da mesa
            binding.tvSubtotalMesa1.text = formatter.format(subtotalMesas)
            
            // Atualizar displays dos totais
            binding.tvTableTotal.text = formatter.format(subtotalMesas)
            binding.tvTotalWithDebt.text = formatter.format(totalComDebito)
            binding.tvCurrentDebt.text = formatter.format(debitoAtual)
            
        } catch (e: Exception) {
            // Em caso de erro, manter valores zerados
            binding.tvSubtotalMesa1.text = formatter.format(0.0)
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
                selectedPaymentMethods = selected
                if (selected.size > 1) {
                    showPaymentValuesDialog(selected)
                } else if (selected.size == 1) {
                    paymentValues.clear()
                    val valorTotal = binding.etAmountReceived.text.toString().toDoubleOrNull() ?: 0.0
                    paymentValues[selected[0]] = valorTotal
                    binding.actvPaymentMethod.setText(selected[0], false)
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
        val editTexts = selected.associateWith { metodo ->
            EditText(requireContext()).apply {
                hint = "Valor para $metodo"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                layout.addView(this)
            }
        }
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Informe o valor de cada método")
            .setView(layout)
            .setPositiveButton("OK") { _, _ ->
                paymentValues.clear()
                var totalInformado = 0.0
                selected.forEach { metodo ->
                    val valor = editTexts[metodo]?.text.toString().toDoubleOrNull() ?: 0.0
                    paymentValues[metodo] = valor
                    totalInformado += valor
                }
                val resumo = paymentValues.entries.joinToString(", ") { "${it.key}: R$ %.2f".format(it.value) }
                binding.actvPaymentMethod.setText(resumo, false)
                // Atualiza o campo Valor Recebido com a soma
                binding.etAmountReceived.setText(totalInformado.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeViewModel() {
        // Observar dados do cliente
        lifecycleScope.launch {
            viewModel.clientName.collect { name ->
                _binding?.let { binding ->
                    if (name.isNotBlank()) {
                        binding.tvClientName.text = name
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.clientAddress.collect { address ->
                _binding?.let { binding ->
                    if (address.isNotBlank()) {
                        binding.tvClientAddress.text = address
                    }
                }
            }
        }
        // Observar resultado do salvamento do acerto
        lifecycleScope.launch {
            viewModel.resultadoSalvamento.collect { resultado ->
                _binding?.let { binding ->
                    resultado?.let {
                        if (it.isSuccess) {
                            com.google.android.material.snackbar.Snackbar.make(
                                binding.root,
                                "Acerto salvo com sucesso!",
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                            ).show()
                            limparCamposAcerto()
                        } else {
                            com.google.android.material.snackbar.Snackbar.make(
                                binding.root,
                                "Erro ao salvar acerto: ${it.exceptionOrNull()?.localizedMessage}",
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                            ).show()
                        }
                        // Resetar resultado para não exibir múltiplas vezes
                        viewModel.resetarResultadoSalvamento()
                    }
                }
            }
        }
    }

    private fun limparCamposAcerto() {
        // Limpar campos principais
        binding.etFichasInicial.text?.clear()
        binding.etFichasFinal.text?.clear()
        binding.etValorFicha.text?.clear()
        binding.etDesconto.text?.clear()
        binding.etAmountReceived.text?.clear()
        binding.etObservacao.text?.clear()
        binding.cbPanoTrocado.isChecked = false
        binding.etNumeroPano.text?.clear()
        // Outros campos se necessário
    }

    private fun salvarAcertoComCamposExtras() {
        val representante = binding.tvRepresentante.text.toString()
        val panoTrocado = binding.cbPanoTrocado.isChecked
        val numeroPano = binding.etNumeroPano.text.toString().takeIf { panoTrocado }
        val tipoAcerto = binding.spTipoAcerto.selectedItem.toString()
        val observacao = binding.etObservacao.text.toString()
        val justificativa = null // Pode ser passado do dialog se necessário
        val mesas = mesasAcertoAdapter.currentList
        if (paymentValues.isNotEmpty()) {
            val valores = paymentValues.toMap()
            val totalRecebido = valores.values.sum()
            val dados = SettlementViewModel.DadosAcerto(
                mesas = mesas,
                representante = representante,
                panoTrocado = panoTrocado,
                numeroPano = numeroPano,
                tipoAcerto = tipoAcerto,
                observacao = observacao,
                justificativa = justificativa,
                metodosPagamento = valores
            )
            viewModel.salvarAcerto(dados, valores)
            // Exibir resumo do acerto em dialog
            val clienteNome = binding.tvClientName.text.toString()
            val dialog = com.example.gestaobilhares.ui.settlement.SettlementSummaryDialog.newInstance(
                clienteNome = clienteNome,
                mesas = mesas,
                total = totalRecebido,
                metodosPagamento = valores,
                observacao = observacao
            )
            dialog.show(parentFragmentManager, "SettlementSummaryDialog")
        } else {
            Toast.makeText(requireContext(), "Selecione o(s) método(s) de pagamento!", Toast.LENGTH_SHORT).show()
        }
    }

    // Função para adicionar nova mesa (implementação básica)
    private fun addMesa() {
        // Por enquanto, não implementamos múltiplas mesas dinamicamente
        // A interface já tem uma mesa fixa (Mesa 1)
        // TODO: Implementar adição dinâmica de mesas em versão futura
    }

    private fun setupMesasSection() {
        lifecycleScope.launch {
            viewModel.mesasCliente.collect { mesas ->
                mesasAcertoAdapter.submitList(mesas)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
