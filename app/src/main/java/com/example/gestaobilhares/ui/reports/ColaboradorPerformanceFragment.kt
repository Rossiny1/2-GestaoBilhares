package com.example.gestaobilhares.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentColaboradorPerformanceBinding
import com.example.gestaobilhares.ui.reports.adapter.ColaboradorPerformanceAdapter
import com.example.gestaobilhares.ui.reports.viewmodel.ColaboradorPerformanceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColaboradorPerformanceFragment : Fragment() {

    private var _binding: FragmentColaboradorPerformanceBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ColaboradorPerformanceViewModel by viewModels()
    private lateinit var adapter: ColaboradorPerformanceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColaboradorPerformanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
        // Carregar dados iniciais via método público
        viewModel.aplicarFiltros()
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

        // Configurar RecyclerView - SEM REATRIBUIÇÃO
        binding.recyclerViewColaboradores.layoutManager = LinearLayoutManager(requireContext())
        adapter = ColaboradorPerformanceAdapter()
        binding.recyclerViewColaboradores.adapter = adapter

        // Configurar spinners
        configurarSpinners()
    }

    private fun configurarSpinners() {
        // Configurar spinner de ciclos
        val ciclos = viewModel.ciclos.value ?: emptyList()
        val ciclosAdapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            ciclos.map { it.descricao }
        )
        ciclosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCiclo.setAdapter(ciclosAdapter)

        // Configurar spinner de rotas
        val rotas = viewModel.rotas.value ?: emptyList()
        val rotasAdapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            rotas.map { it.nome }
        )
        rotasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRota.setAdapter(rotasAdapter)
    }

    private fun setupObservers() {
        // Observar ciclos
        viewModel.ciclos.observe(viewLifecycleOwner) { ciclos ->
            configurarSpinners()
        }

        // Observar rotas
        viewModel.rotas.observe(viewLifecycleOwner) { rotas ->
            configurarSpinners()
        }

        // Observar dados de performance
        viewModel.performanceData.observe(viewLifecycleOwner) { performanceData ->
            if (performanceData.isNotEmpty()) {
                adapter.submitList(performanceData)
                binding.recyclerViewColaboradores.visibility = View.VISIBLE
            } else {
                binding.recyclerViewColaboradores.visibility = View.GONE
            }
        }

        // Observar estatísticas
        viewModel.estatisticas.observe(viewLifecycleOwner) { estatisticas ->
            binding.txtTotalColaboradores.text = estatisticas.totalColaboradores.toString()
            binding.txtColaboradoresAtivos.text = estatisticas.colaboradoresAtivos.toString()
            binding.txtMetaAtingida.text = "${String.format("%.1f", (estatisticas.colaboradoresAtivos.toDouble() / estatisticas.totalColaboradores) * 100)}%"
            binding.txtPendentesAprovacao.text = (estatisticas.totalColaboradores - estatisticas.colaboradoresAtivos).toString()
        }

        // Observar erros
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                android.widget.Toast.makeText(requireContext(), error, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showFilterDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtros Avançados")
            .setMessage("Funcionalidade em desenvolvimento")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
