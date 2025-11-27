package com.example.gestaobilhares.ui.expenses.dialog
import com.example.gestaobilhares.ui.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.ui.databinding.DialogAddEditTypeBinding
import com.example.gestaobilhares.data.entities.CategoriaDespesa
import com.example.gestaobilhares.data.entities.TipoDespesaComCategoria

class AddEditTypeDialog : DialogFragment() {

    private var _binding: DialogAddEditTypeBinding? = null
    private val binding get() = _binding!!

    private var type: TipoDespesaComCategoria? = null
    private var categories: List<CategoriaDespesa> = emptyList()
    private var onSave: ((String, Long) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        // Configurar título baseado se é edição ou adição
        if (type != null) {
            binding.tvDialogTitle.text = "Editar Tipo"
            binding.etTypeName.setText(type!!.nome)
        } else {
            binding.tvDialogTitle.text = "Adicionar Tipo"
        }

        // Configurar spinner de categorias
        setupCategorySpinner()

        // Configurar botões
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveType()
        }
    }

    private fun setupCategorySpinner() {
        val categoryNames = categories.map { it.nome }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
        binding.spinnerCategory.setAdapter(adapter)

        // Se for edição, selecionar a categoria atual
        if (type != null) {
            val currentCategory = categories.find { it.id == type!!.categoriaId }
            currentCategory?.let { category ->
                binding.spinnerCategory.setText(category.nome, false)
            }
        }
    }

    private fun saveType() {
        val name = binding.etTypeName.text.toString().trim()
        val categoryName = binding.spinnerCategory.text.toString().trim()
        
        if (name.isEmpty()) {
            binding.etTypeName.error = "Nome do tipo é obrigatório"
            return
        }

        if (categoryName.isEmpty()) {
            binding.spinnerCategory.error = "Categoria é obrigatória"
            return
        }

        val selectedCategory = categories.find { it.nome == categoryName }
        if (selectedCategory == null) {
            binding.spinnerCategory.error = "Categoria inválida"
            return
        }

        onSave?.invoke(name, selectedCategory.id)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            type: TipoDespesaComCategoria?,
            categories: List<CategoriaDespesa>,
            onSave: (String, Long) -> Unit
        ) {
            val dialog = AddEditTypeDialog().apply {
                this.type = type
                this.categories = categories
                this.onSave = onSave
            }
            dialog.show(fragmentManager, "AddEditTypeDialog")
        }
    }
}

