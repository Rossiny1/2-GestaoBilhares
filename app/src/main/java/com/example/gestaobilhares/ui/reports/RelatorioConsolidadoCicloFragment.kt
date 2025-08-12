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
import com.example.gestaobilhares.databinding.FragmentRelatorioConsolidadoCicloBinding
import com.example.gestaobilhares.ui.reports.viewmodel.RelatorioConsolidadoCicloViewModel
import com.example.gestaobilhares.ui.reports.adapter.DetalhamentoRotasAdapter

/**
 * Fragment para relatório consolidado por ciclo com comparação entre anos.
 * Permite comparar o mesmo ciclo de anos diferentes (ex: 1º ciclo 2024 vs 1º ciclo 2025).
 */
class RelatorioConsolidadoCicloFragment : Fragment() {
    
    private var _binding: FragmentRelatorioConsolidadoCicloBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: RelatorioConsolidadoCicloViewModel
    private lateinit var adapter: DetalhamentoRotasAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRelatorioConsolidadoCicloBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupRecyclerView()
        setupToolbar()
        setupFilters()
        observeData()
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[RelatorioConsolidadoCicloViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        adapter = DetalhamentoRotasAdapter()
        
        binding.recyclerViewRotas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RelatorioConsolidadoCicloFragment.adapter
        }
    }
    
    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnExport.setOnClickListener {
            // TODO: Implementar exportação de relatório
            Toast.makeText(requireContext(), "Exportação será implementada em breve", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupFilters() {
        // Setup do spinner de ciclos
        viewModel.ciclos.observe(viewLifecycleOwner) { ciclos ->
            val cicloAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ciclos.map { it.descricao }
            )
            binding.spinnerCiclo.setAdapter(cicloAdapter)
            
            // Selecionar primeiro ciclo por padrão
            if (ciclos.isNotEmpty()) {
                binding.spinnerCiclo.setText(cicloAdapter.getItem(0) ?: "", false)
                viewModel.selecionarCiclo(ciclos[0].numero)
            }
        }
        
        // Setup do spinner de anos base
        viewModel.anos.observe(viewLifecycleOwner) { anos ->
            val anoBaseAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                anos.map { it.toString() }
            )
            binding.spinnerAnoBase.setAdapter(anoBaseAdapter)
            
            // Selecionar ano atual por padrão
            val anoAtual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val indexAtual = anos.indexOf(anoAtual)
            if (indexAtual >= 0) {
                binding.spinnerAnoBase.setText(anoBaseAdapter.getItem(indexAtual), false)
                viewModel.selecionarAnoBase(anos[indexAtual])
            }
        }
        
        // Setup do spinner de anos de comparação
        viewModel.anos.observe(viewLifecycleOwner) { anos ->
            val anoComparacaoAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                anos.map { it.toString() }
            )
            binding.spinnerAnoComparacao.setAdapter(anoComparacaoAdapter)
            
            // Selecionar ano anterior por padrão
            val anoAtual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val indexAnterior = anos.indexOf(anoAtual - 1)
            if (indexAnterior >= 0) {
                binding.spinnerAnoComparacao.setText(anoComparacaoAdapter.getItem(indexAnterior), false)
                viewModel.selecionarAnoComparacao(anos[indexAnterior])
            }
        }
        
        // Listeners dos spinners
        binding.spinnerCiclo.setOnItemClickListener { _, _, position, _ ->
            val ciclos = viewModel.ciclos.value
            if (ciclos != null && position < ciclos.size) {
                viewModel.selecionarCiclo(ciclos[position].numero)
            }
        }
        
        binding.spinnerAnoBase.setOnItemClickListener { _, _, position, _ ->
            val anos = viewModel.anos.value
            if (anos != null && position < anos.size) {
                viewModel.selecionarAnoBase(anos[position])
            }
        }
        
        binding.spinnerAnoComparacao.setOnItemClickListener { _, _, position, _ ->
            val anos = viewModel.anos.value
            if (anos != null && position < anos.size) {
                viewModel.selecionarAnoComparacao(anos[position])
            }
        }
    }
    
    private fun observeData() {
        // Dados do ano base
        viewModel.dadosAnoBase.observe(viewLifecycleOwner) { dados ->
            if (dados != null) {
                binding.txtFaturamentoAtual.text = viewModel.formatarMoeda(dados.faturamento)
                binding.txtClientesAcertadosAtual.text = dados.clientesAcertados.toString()
                binding.txtMesasLocadasAtual.text = dados.mesasLocadas.toString()
                binding.txtTicketMedioAtual.text = viewModel.formatarMoeda(dados.ticketMedio)
                
                // Calcular e exibir variações
                val dadosComparacao = viewModel.dadosAnoComparacao.value
                if (dadosComparacao != null) {
                    val variacaoFaturamento = viewModel.calcularVariacao(dados.faturamento, dadosComparacao.faturamento)
                    val variacaoClientes = viewModel.calcularVariacao(dados.clientesAcertados.toDouble(), dadosComparacao.clientesAcertados.toDouble())
                    val variacaoMesas = viewModel.calcularVariacao(dados.mesasLocadas.toDouble(), dadosComparacao.mesasLocadas.toDouble())
                    val variacaoTicket = viewModel.calcularVariacao(dados.ticketMedio, dadosComparacao.ticketMedio)
                    
                    binding.txtVariacaoFaturamento.text = viewModel.formatarVariacao(variacaoFaturamento)
                    binding.txtVariacaoClientes.text = viewModel.formatarVariacao(variacaoClientes)
                    binding.txtVariacaoMesas.text = viewModel.formatarVariacao(variacaoMesas)
                    binding.txtVariacaoTicket.text = viewModel.formatarVariacao(variacaoTicket)
                }
            }
        }
        
        // Detalhamento por rota
        viewModel.detalhamentoRotas.observe(viewLifecycleOwner) { rotas ->
            adapter.submitList(rotas)
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
