package com.example.gestaobilhares.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.databinding.FragmentDashboardGeralBinding
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.DespesaRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.ui.reports.viewmodel.DashboardGeralViewModel
import com.example.gestaobilhares.ui.reports.viewmodel.DashboardGeralViewModelFactory
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.ui.reports.adapter.DetalhamentoRotasAdapter

/**
 * Fragment para dashboard geral.
 */
class DashboardGeralFragment : Fragment() {
    
    private var _binding: FragmentDashboardGeralBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardGeralViewModel by viewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val appRepo = AppRepository(
            db.clienteDao(), db.acertoDao(), db.mesaDao(), db.rotaDao(), db.despesaDao(), db.colaboradorDao(), db.cicloAcertoDao()
        )
        val cicloRepo = CicloAcertoRepository(
            db.cicloAcertoDao(),
            DespesaRepository(db.despesaDao()),
            AcertoRepository(db.acertoDao(), db.clienteDao()),
            ClienteRepository(db.clienteDao()),
            db.rotaDao()
        )
        DashboardGeralViewModelFactory(appRepo, cicloRepo, db)
    }
    private lateinit var rotasAdapter: DetalhamentoRotasAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardGeralBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("DashboardGeralFragment", "onViewCreated: Fragment carregado")
        setupUI()
        // Observers dos dados
        viewModel.metrics.observe(viewLifecycleOwner) { m ->
            val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
            binding.txtFaturamentoTotal.text = currency.format(m.faturamentoTotal)
            binding.txtTotalClientes.text = m.totalClientes.toString()
            binding.txtMesasAtivas.text = m.mesasAtivas.toString()
            binding.txtTicketMedio.text = currency.format(m.ticketMedio)
            binding.txtAcertosRealizados.text = m.acertosRealizados.toString()
            binding.txtTaxaConversao.text = "${String.format("%.1f", m.taxaConversao)}%"
        }
        viewModel.detalhamentoRotas.observe(viewLifecycleOwner) { lista ->
            rotasAdapter.submitList(lista)
        }
    }
    
    private fun setupUI() {
        android.util.Log.d("DashboardGeralFragment", "setupUI: Configurando UI")
        
        // Botão voltar
        binding.btnBack.setOnClickListener {
            android.util.Log.d("DashboardGeralFragment", "Botão voltar clicado")
            findNavController().popBackStack()
        }

        // Spinner de Ciclo (1º a 12º Acerto)
        val ciclos = (1..12).map { numero -> "${numero}\u00BA Acerto" }
        val cicloAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ciclos)
        binding.spinnerCiclo.setAdapter(cicloAdapter)
        binding.spinnerCiclo.setText(ciclos.first(), false)

        // Popular anos base e comparação
        viewModel.anos.observe(viewLifecycleOwner) { anos ->
            val anosAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, anos.map { it.toString() })
            binding.spinnerAnoBase.setAdapter(anosAdapter)
            binding.spinnerAnoComparacao.setAdapter(anosAdapter)
            if (anos.isNotEmpty()) {
                binding.spinnerAnoBase.setText(anos.first().toString(), false)
                val comp = anos.getOrNull(1) ?: (anos.first() - 1)
                binding.spinnerAnoComparacao.setText(comp.toString(), false)
            }
        }

        // Listeners dos filtros
        var cicloSelecionado = 1
        var anoBase = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        var anoComparacao = anoBase - 1

        binding.spinnerCiclo.setOnItemClickListener { _, _, position, _ ->
            cicloSelecionado = position + 1
            viewModel.carregarDados(cicloSelecionado, anoBase, anoComparacao)
        }
        binding.spinnerAnoBase.setOnItemClickListener { _, _, position, _ ->
            val anos = viewModel.anos.value ?: return@setOnItemClickListener
            anoBase = anos[position]
            viewModel.carregarDados(cicloSelecionado, anoBase, anoComparacao)
        }
        binding.spinnerAnoComparacao.setOnItemClickListener { _, _, position, _ ->
            val anos = viewModel.anos.value ?: return@setOnItemClickListener
            anoComparacao = anos[position]
            viewModel.carregarDados(cicloSelecionado, anoBase, anoComparacao)
        }

        // Recycler de performance por rota
        rotasAdapter = DetalhamentoRotasAdapter()
        binding.recyclerViewRotas.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewRotas.adapter = rotasAdapter

        // Carregar inicial após anos disponíveis
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
