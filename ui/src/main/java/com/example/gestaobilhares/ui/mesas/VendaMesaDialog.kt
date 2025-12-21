package com.example.gestaobilhares.ui.mesas

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.MesaVendida
import com.example.gestaobilhares.ui.databinding.DialogVendaMesaBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.CpfCnpjTextWatcher
import com.example.gestaobilhares.core.utils.MoneyTextWatcher
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.room.withTransaction
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.take
import java.util.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Dialog para venda de mesa - VERSÃO SIMPLIFICADA
 * ✅ FLUXO MAIS DIRETO E FUNCIONAL
 */
@AndroidEntryPoint
class VendaMesaDialog : DialogFragment() {

    private var _binding: DialogVendaMesaBinding? = null
    private val binding get() = _binding!!

    // ✅ SIMPLIFICADO: Usar instâncias diretas ao invés de injeção -> Agora com Hilt
    @Inject
    lateinit var appRepository: AppRepository

    private var onVendaRealizada: ((MesaVendida) -> Unit)? = null
    private var mesasDisponiveis: List<Mesa> = emptyList()
    private var mesaSelecionada: Mesa? = null
    private var dataVenda: Date = Date()
    private var initialized: Boolean = false
    private var isUpdatingNumeroMesa: Boolean = false

    companion object {
        private const val TAG = "VendaMesaDialog"
        fun newInstance(onVendaRealizada: (MesaVendida) -> Unit): VendaMesaDialog {
            val dialog = VendaMesaDialog()
            dialog.onVendaRealizada = onVendaRealizada
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        android.util.Log.d(TAG, "onCreateDialog() - criando dialog")
        _binding = DialogVendaMesaBinding.inflate(layoutInflater)
        this.isCancelable = true // ✅ CORREÇÃO: Permitir cancelamento
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        // ✅ CORREÇÃO: Configurar botão voltar do Android
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                android.util.Log.d(TAG, "Botão voltar pressionado - fechando dialog")
                dismiss()
                true
            } else {
                false
            }
        }
        // Remover título do dialog (layout já tem título próprio)
        // ✅ IMPORTANTE: Em DialogFragment, quando se usa setView em onCreateDialog,
        // onViewCreated normalmente NÃO é chamado. Portanto, inicializamos aqui.
        try {
            android.util.Log.d(TAG, "onCreateDialog() - inicializando dependencias e UI")
            setupUI()
            setupClickListeners()
            carregarMesasDisponiveis()
            android.util.Log.d(TAG, "onCreateDialog() - inicializacao concluida")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Falha na inicializacao do dialog: ${e.message}", e)
        }
        android.util.Log.d(TAG, "onCreateDialog() - dialog criado")
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        android.util.Log.d(TAG, "onDestroyView() - limpando binding")
        _binding = null
    }

    private fun setupUI() {
        android.util.Log.d(TAG, "setupUI() - configurando data e campos")
        // Configurar data atual
        val calendar = Calendar.getInstance()
        dataVenda = calendar.time
        val dataFormatada = android.text.format.DateFormat.format("dd/MM/yyyy", dataVenda).toString()
        binding.etDataVenda.setText(dataFormatada)

        // Configurar campo de valor
        binding.etValorVenda.hint = "0,00"
        
        // ✅ NOVO: Aplicar máscara CPF/CNPJ
        binding.etCpfCnpjComprador.addTextChangedListener(CpfCnpjTextWatcher(binding.etCpfCnpjComprador))
        
        // ✅ NOVO: Aplicar formatação monetária no campo de valor da venda
        binding.etValorVenda.addTextChangedListener(MoneyTextWatcher(binding.etValorVenda))
    }

    private fun setupClickListeners() {
        android.util.Log.d(TAG, "setupClickListeners() - registrando listeners")
        binding.btnCancelar.setOnClickListener {
            android.util.Log.d(TAG, "Clique em Cancelar - fechando dialog")
            dismiss()
        }

        binding.btnVender.setOnClickListener {
            android.util.Log.d(TAG, "Clique em Vender - iniciando validação/venda")
            realizarVenda()
        }

        // DatePicker
        binding.etDataVenda.setOnClickListener {
            mostrarSeletorData()
        }

        // Busca de mesa
        binding.etNumeroMesa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val texto = s.toString().trim()
                if (isUpdatingNumeroMesa) {
                    android.util.Log.d(TAG, "afterTextChanged() - ignorado (atualização programática)")
                    return
                }
                android.util.Log.d(TAG, "afterTextChanged() - filtro='${texto}'")
                if (texto.isNotEmpty()) {
                    filtrarMesas(texto)
                } else {
                    mesaSelecionada = null
                }
            }
        })

        // Seleção de mesa
        binding.etNumeroMesa.setOnClickListener {
            android.util.Log.d(TAG, "Clique no campo número - abrindo seletor de mesa")
            mostrarSeletorMesa()
        }
    }

    private fun carregarMesasDisponiveis() {
        android.util.Log.d(TAG, "carregarMesasDisponiveis() - iniciando")
        lifecycleScope.launch {
            try {
                // ✅ CORREÇÃO: Verificar se appRepository foi inicializado
                if (!::appRepository.isInitialized) {
                    android.util.Log.e(TAG, "AppRepository não foi inicializado!")
                    context?.let { Toast.makeText(it, "Erro de inicialização. Tente novamente.", Toast.LENGTH_SHORT).show() }
                    return@launch
                }
                
                // Pegar apenas o primeiro snapshot sem cancelar explicitamente (evita CancellationException)
                appRepository
                    .obterMesasDisponiveis()
                    .take(1)
                    .collect { mesas ->
                        if (!isAdded || _binding == null) return@collect
                        mesasDisponiveis = mesas
                        android.util.Log.d(TAG, "Mesas carregadas=${mesasDisponiveis.size}")
                        if (mesasDisponiveis.isEmpty()) {
                            context?.let {
                                Toast.makeText(it, "Nenhuma mesa disponível no depósito", Toast.LENGTH_SHORT).show()
                            }
                            binding.etNumeroMesa.hint = "Nenhuma mesa disponível"
                            binding.etNumeroMesa.isEnabled = false
                        } else {
                            binding.etNumeroMesa.hint = "Digite o número da mesa"
                            binding.etNumeroMesa.isEnabled = true
                            android.util.Log.d(TAG, "Mesas disponíveis: ${mesasDisponiveis.map { it.numero }}")
                        }
                    }

            } catch (e: Exception) {
                if (e is CancellationException) {
                    android.util.Log.d(TAG, "carregarMesasDisponiveis() cancelado")
                    return@launch
                }
                android.util.Log.e(TAG, "Erro ao carregar mesas: ${e.message}", e)
                context?.let { Toast.makeText(it, "Erro ao carregar mesas: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun filtrarMesas(filtro: String) {
        android.util.Log.d(TAG, "filtrarMesas() - filtro='${filtro}'")
        val mesasFiltradas = mesasDisponiveis.filter {
            it.numero.startsWith(filtro, ignoreCase = true)
        }

        if (mesasFiltradas.size == 1) {
            val mesa = mesasFiltradas.first()
            mesaSelecionada = mesa
            if (binding.etNumeroMesa.text?.toString() != mesa.numero) {
                isUpdatingNumeroMesa = true
                try {
                    binding.etNumeroMesa.setText(mesa.numero)
                    binding.etNumeroMesa.setSelection(mesa.numero.length)
                } finally {
                    isUpdatingNumeroMesa = false
                }
            }
            android.util.Log.d(TAG, "Mesa selecionada automaticamente='${mesa.numero}'")
            Toast.makeText(requireContext(), "Mesa ${mesa.numero} selecionada!", Toast.LENGTH_SHORT).show()
        } else if (mesasFiltradas.isEmpty()) {
            android.util.Log.d(TAG, "Nenhuma mesa encontrada para o filtro")
            mesaSelecionada = null
        }
    }

    private fun mostrarSeletorData() {
        android.util.Log.d(TAG, "mostrarSeletorData() - exibindo DatePicker")
        val calendar = Calendar.getInstance()
        calendar.time = dataVenda

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                dataVenda = selectedCalendar.time
                val novaData = android.text.format.DateFormat.format("dd/MM/yyyy", dataVenda).toString()
                binding.etDataVenda.setText(novaData)
                android.util.Log.d(TAG, "Data selecionada='${novaData}'")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun mostrarSeletorMesa() {
        android.util.Log.d(TAG, "mostrarSeletorMesa() - tamanho lista=${mesasDisponiveis.size}")
        if (mesasDisponiveis.isEmpty()) {
            Toast.makeText(requireContext(), "Nenhuma mesa disponível", Toast.LENGTH_SHORT).show()
            return
        }

        val numerosMesas = mesasDisponiveis.map { it.numero }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Selecionar Mesa")
            .setItems(numerosMesas) { _, which ->
                try {
                    val mesaSelecionada = mesasDisponiveis[which]
                    this.mesaSelecionada = mesaSelecionada
                    isUpdatingNumeroMesa = true
                    try {
                        binding.etNumeroMesa.setText(mesaSelecionada.numero)
                    } finally {
                        isUpdatingNumeroMesa = false
                    }
                    android.util.Log.d(TAG, "Mesa selecionada no seletor='${mesaSelecionada.numero}'")
                    Toast.makeText(requireContext(), "Mesa ${mesaSelecionada.numero} selecionada!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Erro ao selecionar mesa: ${e.message}", e)
                    Toast.makeText(requireContext(), "Erro ao selecionar mesa", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun realizarVenda() {
        android.util.Log.d(TAG, "realizarVenda() - iniciando")
        if (!validarCampos()) {
            android.util.Log.w(TAG, "realizarVenda() - validação falhou")
            return
        }

        lifecycleScope.launch {
            try {
                val mesa = mesaSelecionada!!
                android.util.Log.d(TAG, "Mesa alvo id=${mesa.id} numero='${mesa.numero}'")

                // Criar objeto MesaVendida
                val novaVenda = MesaVendida(
                    mesaIdOriginal = mesa.id,
                    numeroMesa = mesa.numero,
                    tipoMesa = mesa.tipoMesa,
                    tamanhoMesa = mesa.tamanho,
                    estadoConservacao = mesa.estadoConservacao,
                    nomeComprador = binding.etNomeComprador.text.toString().trim(),
                    telefoneComprador = binding.etTelefoneComprador.text.toString().trim().takeIf { it.isNotEmpty() },
                    // ✅ NOVO: Remover máscara do CPF/CNPJ (manter apenas números)
                    cpfCnpjComprador = binding.etCpfCnpjComprador.text.toString().trim().replace(Regex("[^0-9]"), "").takeIf { it.isNotEmpty() },
                    enderecoComprador = binding.etEnderecoComprador.text.toString().trim().takeIf { it.isNotEmpty() },
                    // ✅ NOVO: Obter valor monetário usando MoneyTextWatcher
                    valorVenda = MoneyTextWatcher.parseValue(binding.etValorVenda.text.toString()),
                    dataVenda = dataVenda,
                    observacoes = binding.etObservacoes.text.toString().trim().takeIf { it.isNotEmpty() }
                )

                // ✅ Transação atômica: inserir venda + remover mesa do depósito
                var vendaId = 0L
                val database = AppDatabase.getDatabase(requireContext())
                database.withTransaction {
                    vendaId = appRepository.inserirMesaVendida(novaVenda)
                    appRepository.deletarMesa(mesa)
                }
                android.util.Log.d(TAG, "Transação concluída - vendaId=${vendaId}")

                // Verificações pós-transação para diagnóstico
                val vendaRetornada = appRepository.buscarMesaVendidaPorId(vendaId)
                val mesaAindaExiste = appRepository.obterMesaPorId(mesa.id) != null
                android.util.Log.d(TAG, "Pós-transação: vendaRetornadaNull=${vendaRetornada==null} mesaAindaExiste=${mesaAindaExiste}")
                if (vendaRetornada == null || mesaAindaExiste) {
                    android.util.Log.e(TAG, "Inconsistência pós-transação - venda nula ou mesa ainda presente")
                    Toast.makeText(requireContext(), "Falha ao confirmar venda. Tente novamente.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val vendaConfirmada = novaVenda.copy(id = vendaId)
                Toast.makeText(requireContext(), "Mesa vendida com sucesso!", Toast.LENGTH_SHORT).show()
                onVendaRealizada?.invoke(vendaConfirmada)
                android.util.Log.d(TAG, "Venda concluída - fechando dialog")
                dismiss()

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Erro ao vender mesa: ${e.message}", e)
                context?.let { Toast.makeText(it, "Erro ao vender mesa: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun validarCampos(): Boolean {
        android.util.Log.d(TAG, "validarCampos() - iniciando")
        // Limpar erros anteriores
        binding.etNumeroMesa.error = null
        binding.etNomeComprador.error = null
        binding.etValorVenda.error = null

        if (mesaSelecionada == null) {
            android.util.Log.w(TAG, "validarCampos() - mesa não selecionada")
            binding.etNumeroMesa.error = "Selecione uma mesa"
            context?.let { Toast.makeText(it, "Selecione uma mesa", Toast.LENGTH_SHORT).show() }
            return false
        }

        val nomeComprador = binding.etNomeComprador.text.toString().trim()
        if (nomeComprador.isEmpty()) {
            android.util.Log.w(TAG, "validarCampos() - nome vazio")
            binding.etNomeComprador.error = "Nome obrigatório"
            context?.let { Toast.makeText(it, "Nome do comprador é obrigatório", Toast.LENGTH_SHORT).show() }
            return false
        }

        val valorTexto = binding.etValorVenda.text.toString().trim()
        if (valorTexto.isEmpty()) {
            android.util.Log.w(TAG, "validarCampos() - valor vazio")
            binding.etValorVenda.error = "Valor obrigatório"
            context?.let { Toast.makeText(it, "Valor da venda é obrigatório", Toast.LENGTH_SHORT).show() }
            return false
        }

        val valorVenda = valorTexto.replace(",", ".").toDoubleOrNull()
        if (valorVenda == null || valorVenda <= 0) {
            android.util.Log.w(TAG, "validarCampos() - valor inválido='$valorTexto'")
            binding.etValorVenda.error = "Valor deve ser maior que zero"
            context?.let { Toast.makeText(it, "Valor inválido", Toast.LENGTH_SHORT).show() }
            return false
        }

        android.util.Log.d(TAG, "validarCampos() - OK")
        return true
    }

}

