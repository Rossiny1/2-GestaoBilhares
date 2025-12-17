package com.example.gestaobilhares.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.gestaobilhares.ui.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BackupViewModel by viewModels()

    // SAF Launcher para salvar arquivo
    private val createFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            val currentState = viewModel.uiState.value
            if (currentState is BackupViewModel.BackupUiState.Success) {
                 try {
                     requireContext().contentResolver.openOutputStream(it)?.use { outputStream ->
                         OutputStreamWriter(outputStream).use { writer ->
                             writer.write(currentState.jsonData)
                         }
                     }
                     Toast.makeText(requireContext(), "Backup salvo com sucesso!", Toast.LENGTH_LONG).show()
                     viewModel.resetState()
                 } catch (e: Exception) {
                     Toast.makeText(requireContext(), "Erro ao salvar arquivo: ${e.message}", Toast.LENGTH_LONG).show()
                 }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnExportarDados.setOnClickListener {
            viewModel.exportData()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is BackupViewModel.BackupUiState.Loading -> {
                        binding.progressBarExport.visibility = View.VISIBLE
                        binding.btnExportarDados.isEnabled = false
                        binding.tvExportResult.visibility = View.GONE
                    }
                    is BackupViewModel.BackupUiState.Success -> {
                        binding.progressBarExport.visibility = View.GONE
                        binding.btnExportarDados.isEnabled = true
                        binding.tvExportResult.text = "Dados preparados. Escolha onde salvar."
                        binding.tvExportResult.visibility = View.VISIBLE
                        
                        // Lançar SAF para escolher local de salvamento
                        val fileName = "backup_gestaobilhares_${System.currentTimeMillis()}.json"
                        createFileLauncher.launch(fileName)
                    }
                    is BackupViewModel.BackupUiState.Error -> {
                        binding.progressBarExport.visibility = View.GONE
                        binding.btnExportarDados.isEnabled = true
                        binding.tvExportResult.text = "Erro: ${state.message}"
                        binding.tvExportResult.visibility = View.VISIBLE
                        // binding.tvExportResult.setTextColor(android.graphics.Color.RED)
                    }
                    is BackupViewModel.BackupUiState.Idle -> {
                        binding.progressBarExport.visibility = View.GONE
                        binding.tvExportResult.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
