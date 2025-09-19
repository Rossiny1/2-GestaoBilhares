package com.example.gestaobilhares.ui.mesas

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.MesaVendida
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.entities.TamanhoMesa
import com.example.gestaobilhares.data.entities.EstadoConservacao
import com.example.gestaobilhares.databinding.DialogVendaMesaBinding
import com.example.gestaobilhares.data.repository.MesaRepository
import com.example.gestaobilhares.data.repository.MesaVendidaRepository
import com.example.gestaobilhares.data.database.AppDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject

/**
 * Dialog para venda de mesa
 * ✅ NOVO: SISTEMA DE VENDA DE MESAS
 */
@AndroidEntryPoint
class VendaMesaDialog : DialogFragment() {

    private var _binding: DialogVendaMesaBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var mesaRepository: MesaRepository

    @Inject
    lateinit var mesaVendidaRepository: MesaVendidaRepository

    private var onVendaRealizada: ((MesaVendida) -> Unit)? = null
    private var mesasDisponiveis: List<Mesa> = emptyList()
    private var mesaSelecionada: Mesa? = null
    private var dataVenda: Date = Date()

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
        
        setupUI()
        setupClickListeners()
        carregarMesasDisponiveis()
    }

    private fun setupUI() {
        // Configurar data atual
        val calendar = Calendar.getInstance()
        dataVenda = calendar.time
        binding.etDataVenda.setText(android.text.format.DateFormat.format("dd/MM/yyyy", dataVenda))
        
        // Configurar campo de valor
        binding.etValorVenda.hint = "0,00"
    }

    private fun setupClickListeners() {
        binding.btnCancelar.setOnClickListener {
            dismiss()
        }

        binding.btnVender.setOnClickListener {
            realizarVenda()
        }

        binding.etDataVenda.setOnClickListener {
            mostrarSeletorData()
        }

        binding.etNumeroMesa.setOnClickListener {
            mostrarSeletorMesa()
        }
    }

    private fun carregarMesasDisponiveis() {
        lifecycleScope.launch {
            try {
                // Buscar mesas que estão no depósito (sem cliente vinculado)
                mesasDisponiveis = mesaRepository.obterMesasDisponiveis().first()
                android.util.Log.d("VendaMesaDialog", "Mesas disponíveis: ${mesasDisponiveis.size}")
            } catch (e: Exception) {
                android.util.Log.e("VendaMesaDialog", "Erro ao carregar mesas: ${e.message}")
                Toast.makeText(requireContext(), "Erro ao carregar mesas disponíveis", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarSeletorMesa() {
        if (mesasDisponiveis.isEmpty()) {
            Toast.makeText(requireContext(), "Nenhuma mesa disponível no depósito", Toast.LENGTH_SHORT).show()
            return
        }

        val numerosMesas = mesasDisponiveis.map { "${it.numero} - ${getTipoMesaNome(it.tipoMesa)}" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, numerosMesas)
        
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Selecionar Mesa")
            .setAdapter(adapter) { _, position ->
                mesaSelecionada = mesasDisponiveis[position]
                binding.etNumeroMesa.setText("${mesaSelecionada!!.numero} - ${getTipoMesaNome(mesaSelecionada!!.tipoMesa)}")
            }
            .setNegativeButton("Cancelar", null)
            .create()
        
        dialog.show()
    }

    private fun mostrarSeletorData() {
        val calendar = Calendar.getInstance()
        calendar.time = dataVenda
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                dataVenda = selectedCalendar.time
                binding.etDataVenda.setText(android.text.format.DateFormat.format("dd/MM/yyyy", dataVenda))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }

    private fun realizarVenda() {
        if (!validarCampos()) return

        lifecycleScope.launch {
            try {
                val mesa = mesaSelecionada ?: return@launch
                
                // Criar registro da mesa vendida
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

                // Salvar no banco de dados
                val idVenda = mesaVendidaRepository.inserir(mesaVendida)
                
                // Remover mesa do depósito (deletar da tabela de mesas)
                mesaRepository.deletar(mesa)
                
                android.util.Log.d("VendaMesaDialog", "✅ Mesa vendida com sucesso! ID: $idVenda")
                
                Toast.makeText(requireContext(), "Mesa vendida com sucesso!", Toast.LENGTH_SHORT).show()
                
                onVendaRealizada?.invoke(mesaVendida.copy(id = idVenda))
                dismiss()
                
            } catch (e: Exception) {
                android.util.Log.e("VendaMesaDialog", "Erro ao realizar venda: ${e.message}")
                Toast.makeText(requireContext(), "Erro ao realizar venda: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validarCampos(): Boolean {
        if (mesaSelecionada == null) {
            Toast.makeText(requireContext(), "Selecione uma mesa", Toast.LENGTH_SHORT).show()
            return false
        }

        val nomeComprador = binding.etNomeComprador.text.toString().trim()
        if (nomeComprador.isEmpty()) {
            binding.etNomeComprador.error = "Nome do comprador é obrigatório"
            return false
        }

        val valorVenda = binding.etValorVenda.text.toString().replace(",", ".").toDoubleOrNull()
        if (valorVenda == null || valorVenda <= 0) {
            binding.etValorVenda.error = "Valor da venda deve ser maior que zero"
            return false
        }

        return true
    }

    private fun getTipoMesaNome(tipoMesa: TipoMesa): String {
        return when (tipoMesa) {
            TipoMesa.SINUCA -> "Sinuca"
            TipoMesa.PEMBOLIM -> "Pembolim"
            TipoMesa.JUKEBOX -> "Jukebox"
            TipoMesa.OUTROS -> "Outros"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
