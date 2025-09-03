package com.example.gestaobilhares.ui.reports

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentRelatorioClientesBinding
import com.example.gestaobilhares.ui.reports.viewmodel.RelatorioClientesViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class RelatorioClientesFragment : Fragment() {

    private var _binding: FragmentRelatorioClientesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RelatorioClientesViewModel by viewModels()
    private val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRelatorioClientesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        // Configurar botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Configurar botão de filtro
        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        // Configurar RecyclerView
        binding.recyclerViewClientes.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Implementar adapter para lista de clientes
    }

    private fun setupObservers() {
        // Observar estatísticas
        viewModel.estatisticas.observe(viewLifecycleOwner) { estatisticas ->
            updateEstatisticas(estatisticas)
        }

        // Observar clientes
        viewModel.clientes.observe(viewLifecycleOwner) { clientes ->
            // TODO: Atualizar adapter com lista de clientes
        }

        // Observar ciclos
        viewModel.ciclos.observe(viewLifecycleOwner) { ciclos ->
            configurarSpinners()
        }

        // Observar rotas
        viewModel.rotas.observe(viewLifecycleOwner) { rotas ->
            configurarSpinners()
        }

        // Observar loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observar erros
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                showErrorDialog(error)
            }
        }
    }

    private fun updateEstatisticas(estatisticas: RelatorioClientesViewModel.EstatisticasClientes) {
        binding.txtTotalClientes.text = estatisticas.totalClientes.toString()
        binding.txtClientesAtivos.text = estatisticas.clientesAtivos.toString()
        binding.txtClientesDebito.text = estatisticas.clientesDebito.toString()
        binding.txtMesasLocadas.text = estatisticas.mesasLocadas.toString()
        binding.txtTicketMedio.text = formatter.format(estatisticas.ticketMedio)
        binding.txtTaxaConversao.text = String.format("%.1f%%", estatisticas.taxaConversao)
    }

    private fun configurarSpinners() {
        // Configurar spinner de ciclos
        val ciclos = viewModel.ciclos.value ?: emptyList()
        val ciclosAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            ciclos.map { it.descricao }
        )
        ciclosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCiclo.setAdapter(ciclosAdapter)

        // Configurar spinner de rotas
        val rotas = viewModel.rotas.value ?: emptyList()
        val rotasAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            rotas.map { it.nome }
        )
        rotasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRota.setAdapter(rotasAdapter)
    }

    private fun showFilterDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtros Avançados")
            .setMessage("Funcionalidade em desenvolvimento")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showErrorDialog(error: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Erro")
            .setMessage(error)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun exportarRelatorio() {
        val relatorio = viewModel.exportarRelatorio()
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Relatório de Clientes")
            putExtra(Intent.EXTRA_TEXT, relatorio)
        }
        
        startActivity(Intent.createChooser(intent, "Compartilhar Relatório"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
