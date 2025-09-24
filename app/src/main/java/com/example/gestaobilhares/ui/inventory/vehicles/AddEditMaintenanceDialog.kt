package com.example.gestaobilhares.ui.inventory.vehicles

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.databinding.DialogAddEditMaintenanceBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AddEditMaintenanceDialog : DialogFragment() {
    
    private var _binding: DialogAddEditMaintenanceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddEditMaintenanceBinding.inflate(layoutInflater)
        
        setupSpinners()
        setupClickListeners()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle("Adicionar Manutenção")
            .setPositiveButton("Salvar") { _, _ ->
                saveMaintenance()
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun setupSpinners() {
        // Configurar spinner de tipo de manutenção
        val types = listOf("Preventiva", "Corretiva", "Emergencial", "Revisão")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMaintenanceType.adapter = typeAdapter
    }

    private fun setupClickListeners() {
        // Configurar data atual por padrão
        binding.etMaintenanceDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        
        // TODO: Implementar date picker se necessário
    }

    private fun saveMaintenance() {
        val date = binding.etMaintenanceDate.text.toString().trim()
        val description = binding.etMaintenanceDescription.text.toString().trim()
        val value = binding.etMaintenanceValue.text.toString().trim()
        val mileage = binding.etMaintenanceMileage.text.toString().trim()
        val type = binding.spinnerMaintenanceType.selectedItem.toString()

        if (date.isEmpty()) {
            binding.etMaintenanceDate.error = "Data é obrigatória"
            return
        }

        if (description.isEmpty()) {
            binding.etMaintenanceDescription.error = "Descrição é obrigatória"
            return
        }

        if (value.isEmpty()) {
            binding.etMaintenanceValue.error = "Valor é obrigatório"
            return
        }

        if (mileage.isEmpty()) {
            binding.etMaintenanceMileage.error = "Quilometragem é obrigatória"
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
