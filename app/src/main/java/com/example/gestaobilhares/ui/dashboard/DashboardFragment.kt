package com.example.gestaobilhares.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.NumberFormat
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel

    private val moeda: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ CORREÇÃO: Inicializar ViewModel manualmente
        val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
        viewModel = DashboardViewModel(appRepository)

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        
        // ✅ CORREÇÃO: Configurar botão voltar do header se existir
        try {
            val btnBack = binding.root.findViewById<android.widget.ImageButton>(com.example.gestaobilhares.R.id.btnBack)
            btnBack?.setOnClickListener {
                findNavController().popBackStack()
            }
        } catch (e: Exception) {
            // btnBack não existe neste layout, usar apenas toolbar
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.ciclos.observe(viewLifecycleOwner) { ciclos ->
            val labels = ciclos.map { it.descricao }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, labels)
            binding.spinnerCiclo.setAdapter(adapter)
            binding.spinnerCiclo.threshold = 0
            // Define texto inicial (ex.: "Todos") sem disparar listener
            if (labels.isNotEmpty()) {
                binding.spinnerCiclo.setText(labels.first(), false)
            }
            binding.spinnerCiclo.setOnClickListener { binding.spinnerCiclo.showDropDown() }
            binding.spinnerCiclo.setOnItemClickListener { _, _, pos, _ ->
                viewModel.selecionarCiclo(pos)
            }
        }

        viewModel.anos.observe(viewLifecycleOwner) { anos ->
            val labels = anos.map { it.toString() }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, labels)
            binding.spinnerAno.setAdapter(adapter)
            binding.spinnerAno.threshold = 0
            // Define ano atual como texto inicial
            binding.spinnerAno.setText(labels.firstOrNull() ?: "", false)
            binding.spinnerAno.setOnClickListener { binding.spinnerAno.showDropDown() }
            binding.spinnerAno.setOnItemClickListener { _, _, pos, _ ->
                viewModel.selecionarAno(pos)
            }
        }

        viewModel.faturamentoPorRota.observe(viewLifecycleOwner) { fatias ->
            val entries = fatias.map { PieEntry(it.valor.toFloat(), it.label) }
            val dataSet = PieDataSet(entries, "Faturamento por Rota")
            dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
            dataSet.valueTextSize = 12f
            val data = PieData(dataSet)
            binding.chartFaturamentoRotas.data = data
            binding.chartFaturamentoRotas.description.isEnabled = false
            binding.chartFaturamentoRotas.invalidate()

            val total = fatias.sumOf { it.valor }
            binding.txtTotalFaturamento.text = "Total: ${moeda.format(total)}"
        }

        viewModel.despesaPorCategoria.observe(viewLifecycleOwner) { fatias ->
            val entries = fatias.map { PieEntry(it.valor.toFloat(), it.label) }
            val dataSet = PieDataSet(entries, "Despesas por Categoria")
            dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
            dataSet.valueTextSize = 12f
            val data = PieData(dataSet)
            binding.chartDespesasCategorias.data = data
            binding.chartDespesasCategorias.description.isEnabled = false
            binding.chartDespesasCategorias.invalidate()

            val total = fatias.sumOf { it.valor }
            binding.txtTotalDespesas.text = "Total: ${moeda.format(total)}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



