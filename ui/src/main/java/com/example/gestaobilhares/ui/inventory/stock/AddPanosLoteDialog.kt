package com.example.gestaobilhares.ui.inventory.stock
import com.example.gestaobilhares.ui.R

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.data.entities.PanoEstoque
import com.example.gestaobilhares.ui.databinding.DialogAddPanosLoteBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.activityViewModels
import javax.inject.Inject

/**
 * Dialog para adicionar panos em lote no estoque.
 * Permite criar múltiplos panos com numeração sequencial.
 */
@AndroidEntryPoint
class AddPanosLoteDialog : DialogFragment() {

    private var _binding: DialogAddPanosLoteBinding? = null
    private val binding get() = _binding!!

    // ✅ CORREÇÃO: activityViewModels() para não cancelar quando Dialog fecha
    private val viewModel: StockViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddPanosLoteBinding.inflate(LayoutInflater.from(requireContext()))

        setupUI()
        setupClickListeners()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Adicionar Panos em Lote")
            .setView(binding.root)
            .setPositiveButton("Criar Panos", null) // ✅ CORRIGIDO: null para evitar auto-dismiss
            .setNegativeButton("Cancelar") { _, _ ->
                dismiss()
            }
            .create()

        // ✅ V10: Impedir auto-dismiss e controlar manualmente
        dialog.setOnShowListener { dialogInterface ->
            val positiveButton = (dialogInterface as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                criarPanos()
            }
        }

        return dialog
    }

    private fun setupUI() {
        // Configurar spinner de tamanhos
        val tamanhos = arrayOf("Pequeno", "Médio", "Grande")
        val tamanhoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tamanhos)
        binding.etTamanhoPano.setAdapter(tamanhoAdapter)
        
        // Removido: material não é mais utilizado
        binding.etNumeroInicial.setText("1")
        
        // Configurar listeners para cálculo automático
        setupCalculoAutomatico()
        
        // Configurar listener para o campo tamanho
        binding.etTamanhoPano.setOnItemClickListener { _, _, position, _ ->
            val tamanhoSelecionado = tamanhos[position]
            binding.etTamanhoPano.setText(tamanhoSelecionado, false)
            atualizarResumo()
        }
    }

    private fun setupCalculoAutomatico() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calcularNumeroFinal()
                atualizarResumo()
            }
        }

        binding.etQuantidade.addTextChangedListener(textWatcher)
        binding.etNumeroInicial.addTextChangedListener(textWatcher)
    }

    private fun calcularNumeroFinal() {
        val quantidade = binding.etQuantidade.text.toString().toIntOrNull() ?: 0
        val numeroInicial = binding.etNumeroInicial.text.toString().toIntOrNull() ?: 1
        
        if (quantidade > 0) {
            val numeroFinal = numeroInicial + quantidade - 1
            binding.etNumeroFinal.setText(numeroFinal.toString())
        } else {
            binding.etNumeroFinal.setText("")
        }
    }

    private fun atualizarResumo() {
        val quantidade = binding.etQuantidade.text.toString().toIntOrNull() ?: 0
        val numeroInicial = binding.etNumeroInicial.text.toString().toIntOrNull() ?: 1
        // Removido: cor não utilizada
        val tamanho = binding.etTamanhoPano.text.toString().trim()
        
        if (quantidade > 0 && tamanho.isNotEmpty()) {
            val numeroFinal = numeroInicial + quantidade - 1
            val resumo = "Serão criados $quantidade panos $tamanho com numeração de $numeroInicial a $numeroFinal"
            binding.tvResumo.text = resumo
        } else {
            binding.tvResumo.text = "Preencha todos os campos para ver o resumo"
        }
    }

    private fun setupClickListeners() {
        // TODO: Implementar listeners se necessário
    }

    /**
     * ✅ V10: Padrão simplificado igual ao item genérico
     * - ViewModel já gerencia coroutine internamente
     * - SEM lifecycleScope.launch aqui
     */
    private fun criarPanos() {
        val tamanho = binding.etTamanhoPano.text.toString().trim()
        val quantidade = binding.etQuantidade.text.toString().toIntOrNull() ?: 0
        val numeroInicial = binding.etNumeroInicial.text.toString().toIntOrNull() ?: 1
        val observacoes = binding.etObservacoes.text.toString().trim()

        if (tamanho.isEmpty() || quantidade <= 0) {
            android.widget.Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val panos = mutableListOf<PanoEstoque>()
        
        for (i in 0 until quantidade) {
            val numero = numeroInicial + i
            val pano = PanoEstoque(
                numero = "P$numero",
                cor = "",
                tamanho = tamanho,
                material = "",
                disponivel = true,
                observacoes = if (observacoes.isNotEmpty()) observacoes else null
            )
            android.util.Log.d("AddPanosLoteDialog", "Criando pano ${pano.numero}: disponivel=${pano.disponivel}")
            panos.add(pano)
        }
        
        android.util.Log.d("AddPanosLoteDialog", "Total de panos criados: ${panos.size}")
        panos.forEach { pano ->
            android.util.Log.d("AddPanosLoteDialog", "Pano ${pano.numero}: disponivel=${pano.disponivel}, cor='${pano.cor}', tamanho='${pano.tamanho}', material='${pano.material}'")
        }

        // ✅ V10: Chama ViewModel diretamente (igual item genérico)
        android.util.Log.d("AddPanosLoteDialog", "Chamando viewModel.adicionarPanosLote()...")
        viewModel.adicionarPanosLote(panos)
        
        // ✅ V10: Toast e dismiss (igual item genérico)
        android.widget.Toast.makeText(requireContext(), "$quantidade panos criados!", android.widget.Toast.LENGTH_SHORT).show()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

