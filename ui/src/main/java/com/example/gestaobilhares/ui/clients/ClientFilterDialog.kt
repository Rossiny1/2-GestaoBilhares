package com.example.gestaobilhares.ui.clients

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import com.example.gestaobilhares.ui.R
import com.example.gestaobilhares.ui.databinding.DialogClientFilterBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Diálogo para filtrar clientes por status (Ativos, Inativos, Todos)
 */
class ClientFilterDialog(
    context: Context,
    private val currentFilter: FiltroCliente,
    private val onFilterSelected: (FiltroCliente) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogClientFilterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogClientFilterBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        setupUI()
        setupListeners()
        setCurrentFilter(currentFilter)
    }

    private fun setupUI() {
        // Configurar largura do diálogo
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupListeners() {
        // Containers clicáveis
        binding.containerAtivos.setOnClickListener {
            setSingleSelection(FiltroCliente.ACERTADOS)
        }

        binding.containerInativos.setOnClickListener {
            setSingleSelection(FiltroCliente.NAO_ACERTADOS)
        }

        binding.containerTodos.setOnClickListener {
            setSingleSelection(FiltroCliente.TODOS)
        }

        // Botões
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnApply.setOnClickListener {
            val selectedFilter = getSelectedFilter()
            onFilterSelected(selectedFilter)
            dismiss()
        }
    }

    private fun setCurrentFilter(filter: FiltroCliente) {
        // Desmarcar todos primeiro
        binding.cbAtivos.isChecked = false
        binding.cbInativos.isChecked = false
        binding.cbTodos.isChecked = false

        // Marcar o filtro atual
        when (filter) {
            FiltroCliente.ACERTADOS -> binding.cbAtivos.isChecked = true
            FiltroCliente.NAO_ACERTADOS -> binding.cbInativos.isChecked = true
            FiltroCliente.TODOS -> binding.cbTodos.isChecked = true
            FiltroCliente.PENDENCIAS -> binding.cbAtivos.isChecked = true // Fallback para acertados
        }
    }

    private fun setSingleSelection(filter: FiltroCliente) {
        // Desmarcar todos
        binding.cbAtivos.isChecked = false
        binding.cbInativos.isChecked = false
        binding.cbTodos.isChecked = false

        // Marcar apenas o selecionado
        when (filter) {
            FiltroCliente.ACERTADOS -> binding.cbAtivos.isChecked = true
            FiltroCliente.NAO_ACERTADOS -> binding.cbInativos.isChecked = true
            FiltroCliente.TODOS -> binding.cbTodos.isChecked = true
            FiltroCliente.PENDENCIAS -> binding.cbAtivos.isChecked = true
        }
    }

    private fun getSelectedFilter(): FiltroCliente {
        return when {
            binding.cbAtivos.isChecked -> FiltroCliente.ACERTADOS
            binding.cbInativos.isChecked -> FiltroCliente.NAO_ACERTADOS
            binding.cbTodos.isChecked -> FiltroCliente.TODOS
            else -> FiltroCliente.ACERTADOS // Padrão
        }
    }

    companion object {
        /**
         * Mostra o diálogo de filtros
         */
        fun show(
            context: Context,
            currentFilter: FiltroCliente,
            onFilterSelected: (FiltroCliente) -> Unit
        ) {
            ClientFilterDialog(context, currentFilter, onFilterSelected).show()
        }
    }
}
