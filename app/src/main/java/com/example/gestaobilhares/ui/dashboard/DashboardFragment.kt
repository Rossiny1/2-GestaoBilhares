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
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

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

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.ciclos.observe(viewLifecycleOwner) { ciclos ->
            val labels = ciclos.map { it.descricao }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, labels)
            binding.spinnerCiclo.setAdapter(adapter)
            binding.spinnerCiclo.setOnItemClickListener { _, _, pos, _ ->
                viewModel.selecionarCiclo(pos)
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


