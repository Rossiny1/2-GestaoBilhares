package com.example.gestaobilhares.ui.inventory.vehicles

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.databinding.DialogAddEditFuelBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AddEditFuelDialog : DialogFragment() {
    
    private var _binding: DialogAddEditFuelBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddEditFuelBinding.inflate(layoutInflater)
        
        setupClickListeners()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle("Adicionar Abastecimento")
            .setPositiveButton("Salvar") { _, _ ->
                saveFuel()
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun setupClickListeners() {
        // Configurar data atual por padrão
        binding.etFuelDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        
        // TODO: Implementar date picker se necessário
    }

    private fun saveFuel() {
        val date = binding.etFuelDate.text.toString().trim()
        val liters = binding.etFuelLiters.text.toString().trim()
        val value = binding.etFuelValue.text.toString().trim()
        val km = binding.etFuelKm.text.toString().trim()
        val gasStation = binding.etFuelGasStation.text.toString().trim()

        if (date.isEmpty()) {
            binding.etFuelDate.error = "Data é obrigatória"
            return
        }

        if (liters.isEmpty()) {
            binding.etFuelLiters.error = "Litros é obrigatório"
            return
        }

        if (value.isEmpty()) {
            binding.etFuelValue.error = "Valor é obrigatório"
            return
        }

        if (km.isEmpty()) {
            binding.etFuelKm.error = "Quilometragem é obrigatória"
            return
        }

        if (gasStation.isEmpty()) {
            binding.etFuelGasStation.error = "Posto é obrigatório"
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
