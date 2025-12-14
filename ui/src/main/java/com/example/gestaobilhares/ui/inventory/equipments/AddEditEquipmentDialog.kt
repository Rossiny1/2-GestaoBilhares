package com.example.gestaobilhares.ui.inventory.equipments

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.ui.databinding.DialogAddEditEquipmentBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import javax.inject.Inject

/**
 * Diálogo para adicionar ou editar um equipamento.
 * Solicita: Nome do item, Descrição, Quantidade e Localização.
 */
@AndroidEntryPoint
class AddEditEquipmentDialog : DialogFragment() {
    
    private var _binding: DialogAddEditEquipmentBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: EquipmentsViewModel by viewModels()

    companion object {
        fun newInstance(): AddEditEquipmentDialog {
            return AddEditEquipmentDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddEditEquipmentBinding.inflate(layoutInflater)
        

        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle("Adicionar Equipamento")
            .setPositiveButton("Salvar") { _, _ ->
                saveEquipment()
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    /**
     * Valida e salva os dados do equipamento no banco de dados.
     * Verifica se os campos obrigatórios foram preenchidos.
     */
    private fun saveEquipment() {
        val name = binding.etEquipmentName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val quantityText = binding.etQuantity.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()

        // Validação do nome (obrigatório)
        if (name.isEmpty()) {
            binding.etEquipmentName.error = "Nome é obrigatório"
            return
        }

        // Validação da quantidade (obrigatória e deve ser um número válido)
        if (quantityText.isEmpty()) {
            binding.etQuantity.error = "Quantidade é obrigatória"
            return
        }

        val quantity = try {
            quantityText.toInt()
        } catch (e: NumberFormatException) {
            binding.etQuantity.error = "Quantidade deve ser um número válido"
            return
        }

        if (quantity < 0) {
            binding.etQuantity.error = "Quantidade não pode ser negativa"
            return
        }

        try {
            val equipment = Equipment(
                name = name,
                description = description,
                quantity = quantity,
                location = location
            )
            
            // ✅ CORRIGIDO: Salvar via ViewModel (StateFlow compartilhado atualiza automaticamente)
            viewModel.adicionarEquipment(equipment)
            
            // Mostrar sucesso e fechar diálogo
            Toast.makeText(requireContext(), "Equipamento adicionado com sucesso!", Toast.LENGTH_SHORT).show()
            
            // ✅ CORRIGIDO: O StateFlow já atualiza automaticamente, não precisa de callback
            dismiss()
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao salvar equipamento: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
