package com.example.gestaobilhares.ui.inventory.stock

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.databinding.DialogAddEditStockItemBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
class AddEditStockItemDialog : DialogFragment() {
    
    private var _binding: DialogAddEditStockItemBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: StockViewModel
    private var onItemSaved: (() -> Unit)? = null

    companion object {
        fun newInstance(onItemSaved: (() -> Unit)? = null): AddEditStockItemDialog {
            val dialog = AddEditStockItemDialog()
            dialog.onItemSaved = onItemSaved
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddEditStockItemBinding.inflate(layoutInflater)
        
        // ✅ CORREÇÃO: Inicializar ViewModel manualmente
        val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
        viewModel = StockViewModel(appRepository)
        
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

        try {
            val quantityInt = quantity.toInt()
            val unitPriceDouble = unitPrice.toDouble()
            
            val stockItem = StockItem(
                id = 0L, // Será gerado pelo banco
                name = name,
                category = category,
                quantity = quantityInt,
                unitPrice = unitPriceDouble,
                supplier = supplier
            )
            
            // Salvar no banco de dados via ViewModel
            viewModel.adicionarItemEstoque(stockItem)
            
            // Mostrar sucesso e fechar diálogo
            Toast.makeText(requireContext(), "Item adicionado ao estoque com sucesso!", Toast.LENGTH_SHORT).show()
            
            // ✅ CORREÇÃO: Notificar callback para atualizar a lista
            onItemSaved?.invoke()
            
            dismiss()
            
        } catch (e: NumberFormatException) {
            if (quantity.toIntOrNull() == null) {
                binding.etQuantity.error = "Quantidade deve ser um número válido"
            }
            if (unitPrice.toDoubleOrNull() == null) {
                binding.etUnitPrice.error = "Preço deve ser um número válido"
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao salvar item: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

