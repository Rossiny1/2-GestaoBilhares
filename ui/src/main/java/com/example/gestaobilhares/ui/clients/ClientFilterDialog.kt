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
    private val currentFilter: FiltroGeralCliente,
    private val onFilterSelected: (FiltroGeralCliente) -> Unit
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
            setSingleSelection(FiltroGeralCliente.ATIVOS)
        }

        binding.containerInativos.setOnClickListener {
            setSingleSelection(FiltroGeralCliente.INATIVOS)
        }

        binding.containerTodos.setOnClickListener {
            setSingleSelection(FiltroGeralCliente.TODOS)
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

    private fun setCurrentFilter(filter: FiltroGeralCliente) {
        // Desmarcar todos primeiro
        binding.cbAtivos.isChecked = false
        binding.cbInativos.isChecked = false
        binding.cbTodos.isChecked = false

        // Marcar o filtro atual
        when (filter) {
            FiltroGeralCliente.ATIVOS -> binding.cbAtivos.isChecked = true
            FiltroGeralCliente.INATIVOS -> binding.cbInativos.isChecked = true
            FiltroGeralCliente.TODOS -> binding.cbTodos.isChecked = true
        }
    }

    private fun setSingleSelection(filter: FiltroGeralCliente) {
        // Desmarcar todos
        binding.cbAtivos.isChecked = false
        binding.cbInativos.isChecked = false
        binding.cbTodos.isChecked = false

        // Marcar apenas o selecionado
        when (filter) {
            FiltroGeralCliente.ATIVOS -> binding.cbAtivos.isChecked = true
            FiltroGeralCliente.INATIVOS -> binding.cbInativos.isChecked = true
            FiltroGeralCliente.TODOS -> binding.cbTodos.isChecked = true
        }
    }

    private fun getSelectedFilter(): FiltroGeralCliente {
        return when {
            binding.cbAtivos.isChecked -> FiltroGeralCliente.ATIVOS
            binding.cbInativos.isChecked -> FiltroGeralCliente.INATIVOS
            binding.cbTodos.isChecked -> FiltroGeralCliente.TODOS
            else -> FiltroGeralCliente.TODOS // Padrão
        }
    }

    companion object {
        /**
         * Mostra o diálogo de filtros
         */
        fun show(
            context: Context,
            currentFilter: FiltroGeralCliente,
            onFilterSelected: (FiltroGeralCliente) -> Unit
        ) {
            ClientFilterDialog(context, currentFilter, onFilterSelected).show()
        }
    }
}
