package com.example.gestaobilhares.ui.inventory.others

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.databinding.DialogAddEditOtherItemBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddEditOtherItemDialog : DialogFragment() {
    
    private var _binding: DialogAddEditOtherItemBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddEditOtherItemBinding.inflate(layoutInflater)
        
        setupClickListeners()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle("Adicionar Outro Item")
            .setPositiveButton("Salvar") { _, _ ->
                saveOtherItem()
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun setupClickListeners() {
        // TODO: Implementar listeners se necessário
    }

    private fun saveOtherItem() {
        val name = binding.etItemName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val quantity = binding.etQuantity.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()

        if (name.isEmpty()) {
            binding.etItemName.error = "Nome é obrigatório"
            return
        }

        if (quantity.isEmpty()) {
            binding.etQuantity.error = "Quantidade é obrigatória"
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
