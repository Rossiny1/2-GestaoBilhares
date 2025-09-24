package com.example.gestaobilhares.ui.inventory.equipments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.databinding.DialogAddEditEquipmentBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddEditEquipmentDialog : DialogFragment() {
    
    private var _binding: DialogAddEditEquipmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddEditEquipmentBinding.inflate(layoutInflater)
        
        setupSpinners()
        setupClickListeners()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle("Adicionar Equipamento")
            .setPositiveButton("Salvar") { _, _ ->
                saveEquipment()
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun setupSpinners() {
        // Configurar spinner de tipo
        val types = listOf("Mesa", "Acessório", "Ferramenta", "Outros")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = typeAdapter

        // Configurar spinner de status
        val statuses = listOf("Ativo", "Inativo", "Manutenção", "Vendido")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = statusAdapter
    }

    private fun setupClickListeners() {
        // TODO: Implementar listeners se necessário
    }

    private fun saveEquipment() {
        val name = binding.etEquipmentName.text.toString().trim()
        val type = binding.spinnerType.selectedItem.toString()
        val status = binding.spinnerStatus.selectedItem.toString()
        val location = binding.etLocation.text.toString().trim()

        if (name.isEmpty()) {
            binding.etEquipmentName.error = "Nome é obrigatório"
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
