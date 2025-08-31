package com.example.gestaobilhares.ui.expenses.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.databinding.DialogAddEditCategoryBinding
import com.example.gestaobilhares.data.entities.CategoriaDespesa
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddEditCategoryDialog : DialogFragment() {

    private var _binding: DialogAddEditCategoryBinding? = null
    private val binding get() = _binding!!

    private var category: CategoriaDespesa? = null
    private var onSave: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        // Configurar título baseado se é edição ou adição
        if (category != null) {
            binding.tvDialogTitle.text = "Editar Categoria"
            binding.etCategoryName.setText(category!!.nome)
        } else {
            binding.tvDialogTitle.text = "Adicionar Categoria"
        }

        // Configurar botões
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveCategory()
        }
    }

    private fun saveCategory() {
        val name = binding.etCategoryName.text.toString().trim()
        
        if (name.isEmpty()) {
            binding.etCategoryName.error = "Nome da categoria é obrigatório"
            return
        }

        onSave?.invoke(name)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            category: CategoriaDespesa?,
            onSave: (String) -> Unit
        ) {
            val dialog = AddEditCategoryDialog().apply {
                this.category = category
                this.onSave = onSave
            }
            dialog.show(fragmentManager, "AddEditCategoryDialog")
        }
    }
}
