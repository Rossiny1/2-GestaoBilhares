package com.example.gestaobilhares.ui.inventory.stock

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.databinding.DialogAddEditStockItemBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddEditStockItemDialog : DialogFragment() {
    
    private var _binding: DialogAddEditStockItemBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddEditStockItemBinding.inflate(layoutInflater)
        
        setupSpinners()
        setupClickListeners()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle("Adicionar Item ao Estoque")
            .setPositiveButton("Salvar") { _, _ ->
                saveStockItem()
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun setupSpinners() {
        // Configurar spinner de categoria
        val categories = listOf("Acessórios", "Ferramentas", "Materiais", "Outros")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter
    }

    private fun setupClickListeners() {
        // TODO: Implementar listeners se necessário
    }

    private fun saveStockItem() {
        val name = binding.etItemName.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()
        val quantity = binding.etQuantity.text.toString().trim()
        val unitPrice = binding.etUnitPrice.text.toString().trim()
        val supplier = binding.etSupplier.text.toString().trim()

        if (name.isEmpty()) {
            binding.etItemName.error = "Nome é obrigatório"
            return
        }

        if (quantity.isEmpty()) {
            binding.etQuantity.error = "Quantidade é obrigatória"
            return
        }

        if (unitPrice.isEmpty()) {
            binding.etUnitPrice.error = "Preço unitário é obrigatório"
            return
        }

        // TODO: Implementar salvamento no banco de dados
        // Por enquanto, apenas fechar o diálogo
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
