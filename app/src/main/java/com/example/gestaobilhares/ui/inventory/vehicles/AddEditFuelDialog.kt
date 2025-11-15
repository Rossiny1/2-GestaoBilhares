package com.example.gestaobilhares.ui.inventory.vehicles

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.gestaobilhares.databinding.DialogAddEditFuelBinding
import com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo
import com.example.gestaobilhares.data.factory.RepositoryFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

class AddEditFuelDialog : DialogFragment() {
    
    private var _binding: DialogAddEditFuelBinding? = null
    private val binding get() = _binding!!
    
    private var vehicleId: Long = 0L

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddEditFuelBinding.inflate(layoutInflater)
        
        // ✅ NOVO: Obter vehicleId dos argumentos
        vehicleId = arguments?.getLong("vehicleId", 0L) ?: 0L
        
        if (vehicleId == 0L) {
            android.util.Log.e("AddEditFuelDialog", "⚠️ vehicleId não fornecido!")
        }
        
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
        val dateStr = binding.etFuelDate.text.toString().trim()
        val litersStr = binding.etFuelLiters.text.toString().trim()
        val valueStr = binding.etFuelValue.text.toString().trim()
        val kmStr = binding.etFuelKm.text.toString().trim()
        val gasStation = binding.etFuelGasStation.text.toString().trim()

        if (dateStr.isEmpty()) {
            binding.etFuelDate.error = "Data é obrigatória"
            return
        }

        if (litersStr.isEmpty()) {
            binding.etFuelLiters.error = "Litros é obrigatório"
            return
        }

        if (valueStr.isEmpty()) {
            binding.etFuelValue.error = "Valor é obrigatório"
            return
        }

        if (kmStr.isEmpty()) {
            binding.etFuelKm.error = "Quilometragem é obrigatória"
            return
        }

        if (gasStation.isEmpty()) {
            binding.etFuelGasStation.error = "Posto é obrigatório"
            return
        }

        if (vehicleId == 0L) {
            android.util.Log.e("AddEditFuelDialog", "❌ vehicleId inválido!")
            Snackbar.make(binding.root, "Erro: Veículo não identificado", Snackbar.LENGTH_SHORT).show()
            return
        }

        // ✅ IMPLEMENTADO: Converter e salvar no banco de dados
        try {
            // Converter data de dd/MM/yyyy para Date
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val localDate = LocalDate.parse(dateStr, dateFormatter)
            val dataAbastecimento = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            
            // Converter valores
            val litros = litersStr.replace(",", ".").toDoubleOrNull() ?: 0.0
            val valor = valueStr.replace(",", ".").toDoubleOrNull() ?: 0.0
            val kmVeiculo = kmStr.toLongOrNull() ?: 0L
            
            if (litros <= 0) {
                binding.etFuelLiters.error = "Litros deve ser maior que zero"
                return
            }
            
            if (valor <= 0) {
                binding.etFuelValue.error = "Valor deve ser maior que zero"
                return
            }
            
            if (kmVeiculo <= 0) {
                binding.etFuelKm.error = "Quilometragem deve ser maior que zero"
                return
            }
            
            // Criar histórico de combustível
            val historicoCombustivel = HistoricoCombustivelVeiculo(
                veiculoId = vehicleId,
                dataAbastecimento = dataAbastecimento,
                litros = litros,
                valor = valor,
                kmVeiculo = kmVeiculo,
                kmRodado = 0.0, // Será calculado automaticamente se necessário
                posto = gasStation,
                observacoes = null,
                dataCriacao = Date()
            )
            
            // Salvar no banco de dados
            lifecycleScope.launch {
                try {
                    val appRepository = RepositoryFactory.getAppRepository(requireContext())
                    android.util.Log.d("AddEditFuelDialog", "📝 Salvando abastecimento: Veículo=$vehicleId, Litros=$litros, Valor=$valor, KM=$kmVeiculo")
                    val idInserido = appRepository.inserirHistoricoCombustivel(historicoCombustivel)
                    android.util.Log.d("AddEditFuelDialog", "✅ Abastecimento salvo com ID: $idInserido")
                    
                    // ✅ NOVO: Verificar se foi salvo corretamente
                    val historicoSalvo = appRepository.obterHistoricoCombustivelPorVeiculo(vehicleId).first()
                    android.util.Log.d("AddEditFuelDialog", "📊 Total de abastecimentos após salvar: ${historicoSalvo.size}")
                    
                    Snackbar.make(binding.root, "Abastecimento salvo com sucesso", Snackbar.LENGTH_SHORT).show()
                    dismiss()
                } catch (e: Exception) {
                    android.util.Log.e("AddEditFuelDialog", "❌ Erro ao salvar abastecimento: ${e.message}", e)
                    Snackbar.make(binding.root, "Erro ao salvar: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AddEditFuelDialog", "❌ Erro ao processar dados: ${e.message}", e)
            Snackbar.make(binding.root, "Erro ao processar dados: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
