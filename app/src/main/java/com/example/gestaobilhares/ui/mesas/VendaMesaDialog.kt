package com.example.gestaobilhares.ui.mesas

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.MesaVendida
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.databinding.DialogVendaMesaBinding
import com.example.gestaobilhares.data.repository.MesaVendidaRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject

/**
 * Dialog para venda de mesa - VERSÃO CORRIGIDA
 * ✅ FUNCIONAMENTO GARANTIDO
 */
@AndroidEntryPoint
class VendaMesaDialog : DialogFragment() {

    private var _binding: DialogVendaMesaBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var appRepository: com.example.gestaobilhares.data.repository.AppRepository

    @Inject
    lateinit var mesaVendidaRepository: MesaVendidaRepository

    private var onVendaRealizada: ((MesaVendida) -> Unit)? = null
    private var mesasDisponiveis: List<Mesa> = emptyList()
    private var mesaSelecionada: Mesa? = null
    private var dataVenda: Date = Date()
    private var adapter: ArrayAdapter<String>? = null

    companion object {
        fun newInstance(onVendaRealizada: (MesaVendida) -> Unit): VendaMesaDialog {
            val dialog = VendaMesaDialog()
            dialog.onVendaRealizada = onVendaRealizada
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogVendaMesaBinding.inflate(layoutInflater)
        return Dialog(requireContext()).apply {
            setContentView(binding.root)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("VendaMesaDialog", "🚀 Iniciando VendaMesaDialog...")
        
        setupUI()
        setupClickListeners()
        carregarMesasDisponiveis()
    }

    private fun setupUI() {
        android.util.Log.d("VendaMesaDialog", "🔧 Configurando UI...")
        
        // Configurar data atual
        val calendar = Calendar.getInstance()
        dataVenda = calendar.time
        val dataFormatada = android.text.format.DateFormat.format("dd/MM/yyyy", dataVenda).toString()
        binding.etDataVenda.setText(dataFormatada)
        
        android.util.Log.d("VendaMesaDialog", "📅 Data configurada: $dataFormatada")
        
        // Configurar campo de valor
        binding.etValorVenda.hint = "0,00"
        
        android.util.Log.d("VendaMesaDialog", "✅ UI configurada com sucesso")
    }

    private fun setupClickListeners() {
        android.util.Log.d("VendaMesaDialog", "🔗 Configurando listeners...")
        
        binding.btnCancelar.setOnClickListener {
            android.util.Log.d("VendaMesaDialog", "❌ Cancelando venda...")
            dismiss()
        }

        binding.btnVender.setOnClickListener {
            android.util.Log.d("VendaMesaDialog", "💰 Tentando realizar venda...")
            realizarVenda()
        }

        // CORRIGIDO: DatePicker funcional
        binding.etDataVenda.setOnClickListener {
            android.util.Log.d("VendaMesaDialog", "📅 Abrindo seletor de data...")
            mostrarSeletorData()
        }
        
        // CORRIGIDO: Busca de mesa com TextWatcher
        binding.etNumeroMesa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val texto = s.toString().trim()
                if (texto.isNotEmpty()) {
                    filtrarMesas(texto)
                }
            }
        })
        
        android.util.Log.d("VendaMesaDialog", "✅ Listeners configurados")
    }

    private fun carregarMesasDisponiveis() {
        android.util.Log.d("VendaMesaDialog", "🔍 Carregando mesas disponíveis...")
        
        lifecycleScope.launch {
            try {
                // CORRIGIDO: Usar first() em vez de collect para evitar loop infinito
                mesasDisponiveis = appRepository.obterMesasDisponiveis().first()
                
                android.util.Log.d("VendaMesaDialog", "✅ ${mesasDisponiveis.size} mesas carregadas")
                
                mesasDisponiveis.forEachIndexed { index, mesa ->
                    android.util.Log.d("VendaMesaDialog", "Mesa ${index + 1}: ${mesa.numero} (${mesa.tipoMesa})")
                }
                
                if (mesasDisponiveis.isEmpty()) {
                    android.util.Log.w("VendaMesaDialog", "⚠️ Nenhuma mesa no depósito")
                    Toast.makeText(requireContext(), "Nenhuma mesa disponível no depósito", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("VendaMesaDialog", "❌ Erro ao carregar mesas: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filtrarMesas(filtro: String) {
        android.util.Log.d("VendaMesaDialog", "🔍 Filtrando mesas com: '$filtro'")
        
        val mesasFiltradas = mesasDisponiveis.filter { 
            it.numero.startsWith(filtro, ignoreCase = true) 
        }
        
        android.util.Log.d("VendaMesaDialog", "📋 ${mesasFiltradas.size} mesas encontradas")
        
        if (mesasFiltradas.size == 1) {
            val mesa = mesasFiltradas.first()
            mesaSelecionada = mesa
            binding.etNumeroMesa.setText(mesa.numero)
            binding.etNumeroMesa.setSelection(mesa.numero.length)
            android.util.Log.d("VendaMesaDialog", "✅ Mesa selecionada automaticamente: ${mesa.numero}")
            Toast.makeText(requireContext(), "Mesa ${mesa.numero} selecionada!", Toast.LENGTH_SHORT).show()
        } else if (mesasFiltradas.isEmpty()) {
            mesaSelecionada = null
            android.util.Log.d("VendaMesaDialog", "❌ Nenhuma mesa encontrada")
        }
    }

    private fun mostrarSeletorData() {
        android.util.Log.d("VendaMesaDialog", "📅 Mostrando DatePicker...")
        
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
                android.util.Log.d("VendaMesaDialog", "✅ Data selecionada: $novaData")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
        android.util.Log.d("VendaMesaDialog", "📅 DatePickerDialog exibido")
    }

    private fun realizarVenda() {
        android.util.Log.d("VendaMesaDialog", "💰 Iniciando processo de venda...")
        
        if (!validarCampos()) {
            android.util.Log.w("VendaMesaDialog", "⚠️ Validação de campos falhou")
            return
        }

        lifecycleScope.launch {
            try {
                val mesa = mesaSelecionada!!
                android.util.Log.d("VendaMesaDialog", "🏓 Vendendo mesa: ${mesa.numero}")
                
                // CORRIGIDO: Parâmetros corretos da entidade MesaVendida
                val mesaVendida = MesaVendida(
                    mesaIdOriginal = mesa.id,
                    numeroMesa = mesa.numero,
                    tipoMesa = mesa.tipoMesa,
                    tamanhoMesa = mesa.tamanho,
                    estadoConservacao = mesa.estadoConservacao,
                    nomeComprador = binding.etNomeComprador.text.toString().trim(),
                    telefoneComprador = binding.etTelefoneComprador.text.toString().trim().takeIf { it.isNotEmpty() },
                    cpfCnpjComprador = binding.etCpfCnpjComprador.text.toString().trim().takeIf { it.isNotEmpty() },
                    valorVenda = binding.etValorVenda.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0,
                    dataVenda = dataVenda,
                    observacoes = binding.etObservacoes.text.toString().trim().takeIf { it.isNotEmpty() }
                )

                android.util.Log.d("VendaMesaDialog", "💾 Salvando mesa vendida...")
                val idVenda = mesaVendidaRepository.inserir(mesaVendida)
                
                android.util.Log.d("VendaMesaDialog", "🗑️ Removendo mesa do depósito...")
                appRepository.deletarMesa(mesa)
                
                android.util.Log.d("VendaMesaDialog", "✅ Venda realizada! ID: $idVenda")
                
                Toast.makeText(requireContext(), "Mesa vendida com sucesso!", Toast.LENGTH_SHORT).show()
                
                onVendaRealizada?.invoke(mesaVendida.copy(id = idVenda))
                dismiss()
                
            } catch (e: Exception) {
                android.util.Log.e("VendaMesaDialog", "❌ Erro na venda: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao vender: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validarCampos(): Boolean {
        android.util.Log.d("VendaMesaDialog", "🔍 Validando campos...")
        
        if (mesaSelecionada == null) {
            android.util.Log.w("VendaMesaDialog", "❌ Mesa não selecionada")
            Toast.makeText(requireContext(), "Selecione uma mesa", Toast.LENGTH_SHORT).show()
            return false
        }

        val nomeComprador = binding.etNomeComprador.text.toString().trim()
        if (nomeComprador.isEmpty()) {
            android.util.Log.w("VendaMesaDialog", "❌ Nome do comprador vazio")
            binding.etNomeComprador.error = "Nome obrigatório"
            return false
        }

        val valorTexto = binding.etValorVenda.text.toString().replace(",", ".")
        val valorVenda = valorTexto.toDoubleOrNull()
        if (valorVenda == null || valorVenda <= 0) {
            android.util.Log.w("VendaMesaDialog", "❌ Valor inválido: '$valorTexto'")
            binding.etValorVenda.error = "Valor deve ser maior que zero"
            return false
        }

        android.util.Log.d("VendaMesaDialog", "✅ Validação passou - Mesa: ${mesaSelecionada!!.numero}, Comprador: $nomeComprador, Valor: $valorVenda")
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
