package com.example.gestaobilhares.ui.inventory.vehicles

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.gestaobilhares.ui.databinding.DialogAddEditVehicleBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
class AddEditVehicleDialog : DialogFragment() {

    private var _binding: DialogAddEditVehicleBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: VehiclesViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddEditVehicleBinding.inflate(LayoutInflater.from(requireContext()))
        
        // ✅ CORREÇÃO: Inicializar ViewModel manualmente
        val appRepository = com.example.gestaobilhares.factory.RepositoryFactory.getAppRepository(requireContext())
        viewModel = VehiclesViewModel(appRepository)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Novo Veículo")
            .setView(binding.root)
            .setPositiveButton("Salvar") { d, _ ->
                val nome = binding.etNome.text.toString().trim()
                val placa = binding.etPlaca.text.toString().trim()
                val marca = binding.etMarca.text.toString().trim()
                val modelo = binding.etModelo.text.toString().trim()
                val ano = binding.etAno.text.toString().toIntOrNull() ?: 0
                val km = binding.etKm.text.toString().toLongOrNull() ?: 0L
                if (nome.isNotEmpty() && placa.isNotEmpty() && marca.isNotEmpty() && modelo.isNotEmpty() && ano > 0) {
                    viewModel.addVehicle(nome, placa, marca, modelo, ano, km)
                }
                d.dismiss()
            }
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

