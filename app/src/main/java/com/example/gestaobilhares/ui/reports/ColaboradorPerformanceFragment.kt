package com.example.gestaobilhares.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentColaboradorPerformanceBinding
import com.example.gestaobilhares.ui.reports.viewmodel.ColaboradorPerformanceViewModel
import com.example.gestaobilhares.ui.reports.adapter.ColaboradorPerformanceAdapter

/**
 * Fragment para relatório de performance dos colaboradores.
 * Baseado em ciclos e rotas com métricas detalhadas.
 */
class ColaboradorPerformanceFragment : Fragment() {
    
    private var _binding: FragmentColaboradorPerformanceBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ColaboradorPerformanceViewModel
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
        
        setupViewModel()
        setupRecyclerView()
        setupToolbar()
        setupFilters()
        observeData()
        
        // Carregar dados iniciais
        viewModel.carregarDados()
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[ColaboradorPerformanceViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        adapter = ColaboradorPerformanceAdapter { colaborador ->
            // Navegar para detalhes do colaborador
            // TODO: Implementar navegação para detalhes
            Toast.makeText(requireContext(), "Detalhes de ${colaborador.nome}", Toast.LENGTH_SHORT).show()
        }
        
        binding.recyclerViewColaboradores.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ColaboradorPerformanceFragment.adapter
        }
    }
    
    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnFilter.setOnClickListener {
            // TODO: Implementar filtros avançados
            Toast.makeText(requireContext(), "Filtros avançados em breve", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupFilters() {
        // Setup do spinner de ciclos
        viewModel.ciclos.observe(viewLifecycleOwner) { ciclos ->
            val cicloAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ciclos.map { "Ciclo ${it.numero} - ${it.periodo}" }
            )
            binding.spinnerCiclo.setAdapter(cicloAdapter)
            
            // Selecionar primeiro ciclo por padrão
            if (ciclos.isNotEmpty()) {
                binding.spinnerCiclo.setText(cicloAdapter.getItem(0), false)
                viewModel.selecionarCiclo(ciclos[0].id)
            }
        }
        
        // Setup do spinner de rotas
        viewModel.rotas.observe(viewLifecycleOwner) { rotas ->
            val rotaAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                listOf("Todas as Rotas") + rotas.map { it.nome }
            )
            binding.spinnerRota.setAdapter(rotaAdapter)
            
            // Selecionar "Todas as Rotas" por padrão
            binding.spinnerRota.setText(rotaAdapter.getItem(0), false)
            viewModel.selecionarRota(null) // null = todas as rotas
        }
        
        // Listeners dos spinners
        binding.spinnerCiclo.setOnItemClickListener { _, _, position, _ ->
            val ciclos = viewModel.ciclos.value
            if (ciclos != null && position < ciclos.size) {
                viewModel.selecionarCiclo(ciclos[position].id)
            }
        }
        
        binding.spinnerRota.setOnItemClickListener { _, _, position, _ ->
            val rotas = viewModel.rotas.value
            if (position == 0) {
                // "Todas as Rotas"
                viewModel.selecionarRota(null)
            } else if (rotas != null && position - 1 < rotas.size) {
                viewModel.selecionarRota(rotas[position - 1].id)
            }
        }
    }
    
    private fun observeData() {
        // Estatísticas gerais
        viewModel.estatisticasGerais.observe(viewLifecycleOwner) { stats ->
            binding.txtTotalColaboradores.text = stats.totalColaboradores.toString()
            binding.txtColaboradoresAtivos.text = stats.colaboradoresAtivos.toString()
            binding.txtMetaAtingida.text = "${stats.percentualMetaAtingida}%"
            binding.txtPendentesAprovacao.text = stats.pendentesAprovacao.toString()
        }
        
        // Lista de performance
        viewModel.colaboradoresPerformance.observe(viewLifecycleOwner) { colaboradores ->
            adapter.submitList(colaboradores)
        }
        
        // Loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Mensagens de erro
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
