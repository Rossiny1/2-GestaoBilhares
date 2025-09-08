package com.example.gestaobilhares.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.databinding.FragmentClosureReportBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class ClosureReportFragment : Fragment() {

    private var _binding: FragmentClosureReportBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ClosureReportViewModel by viewModels()
    private val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    private lateinit var adapter: DetalheAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClosureReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        adapter = DetalheAdapter(moeda)
        binding.recyclerDetalhes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDetalhes.adapter = adapter

        setupObservers()

        // Garantir clique tanto no card de filtros quanto no card de dados
        binding.cardResumo.setOnClickListener { mostrarDialogoResumoDetalhe() }
        binding.cardResumoDados?.setOnClickListener { mostrarDialogoResumoDetalhe() }
    }

    private fun setupObservers() {
        viewModel.anos.observe(viewLifecycleOwner) { anos ->
            val labels = anos.map { it.toString() }
            val a = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, labels)
            binding.spinnerAno.setAdapter(a)
            binding.spinnerAno.setText(labels.firstOrNull() ?: "", false)
            binding.spinnerAno.threshold = 0
            binding.spinnerAno.setOnClickListener { binding.spinnerAno.showDropDown() }
            binding.spinnerAno.setOnItemClickListener { _, _, pos, _ -> viewModel.selecionarAno(pos) }
        }

        viewModel.ciclos.observe(viewLifecycleOwner) { ciclos ->
            val labels = ciclos.map { it.descricao }
            val a = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, labels)
            binding.spinnerCiclo.setAdapter(a)
            binding.spinnerCiclo.setText(labels.firstOrNull() ?: "", false)
            binding.spinnerCiclo.threshold = 0
            binding.spinnerCiclo.setOnClickListener { binding.spinnerCiclo.showDropDown() }
            binding.spinnerCiclo.setOnItemClickListener { _, _, pos, _ -> viewModel.selecionarCiclo(pos) }
        }

        viewModel.resumo.observe(viewLifecycleOwner) { r ->
            binding.txtFaturamentoTotal.text = "Faturamento Total: ${moeda.format(r.faturamentoTotal)}"
            binding.txtDespesasTotal.text = "Despesas Total: ${moeda.format(r.despesasTotal)}"
            binding.txtLucroLiquido.text = "Lucro Líquido: ${moeda.format(r.lucroLiquido)}"
            binding.txtLucroRossiny.text = "Lucro Rossiny: ${moeda.format(r.lucroRossiny)}"
            binding.txtLucroPetrina.text = "Lucro Petrina: ${moeda.format(r.lucroPetrina)}"
        }

        viewModel.detalhes.observe(viewLifecycleOwner) { linhas ->
            adapter.submit(linhas)
        }
    }

    private fun mostrarDialogoResumoDetalhe() {
        val resumo = viewModel.resumo.value ?: return
        viewModel.detalhes.value?.let { linhas ->
            val totalMesas = viewModel.totalMesasLocadas.value ?: 0
            val faturamentoTotal = resumo.faturamentoTotal
            val despesasTotal = resumo.despesasTotal
            val lucro = resumo.lucroLiquido
            val lucroRossiny = resumo.lucroRossiny
            val lucroPetrina = resumo.lucroPetrina
            val faturamentoPorMesa = if (totalMesas > 0) faturamentoTotal / totalMesas else 0.0
            val lucroPorMesa = if (totalMesas > 0) lucro / totalMesas else 0.0
            val despesaPorMesa = if (totalMesas > 0) despesasTotal / totalMesas else 0.0
            val margem = if (faturamentoTotal > 0) (lucro / faturamentoTotal) * 100.0 else 0.0

            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Resumo do Ciclo")
                .setMessage(
                    "Total de mesas: $totalMesas\n" +
                    "Faturamento por mesa: ${moeda.format(faturamentoPorMesa)}\n" +
                    "Lucro por mesa: ${moeda.format(lucroPorMesa)}\n" +
                    "Despesa por mesa: ${moeda.format(despesaPorMesa)}\n" +
                    "Margem líquida: ${String.format(Locale("pt", "BR"), "%.1f%%", margem)}\n\n" +
                    "Composição das Despesas:\n" +
                    "• Despesas de Rotas: ${moeda.format(despesasTotal)}\n" +
                    "• Comissão Motorista (3%): Incluída\n" +
                    "• Comissão Iltair (2%): Incluída\n\n" +
                    "Distribuição do Lucro:\n" +
                    "Lucro Rossiny (60%): ${moeda.format(lucroRossiny)}\n" +
                    "Lucro Petrina (40%): ${moeda.format(lucroPetrina)}"
                )
                .setPositiveButton("OK", null)
                .show()
        }
    }

    // Removido cálculo estimado para evitar erros: usamos valor real do ViewModel

    private class DetalheAdapter(private val moeda: NumberFormat) : RecyclerView.Adapter<DetalheVH>() {
        private val itens = mutableListOf<ClosureReportViewModel.LinhaDetalhe>()
        fun submit(novos: List<ClosureReportViewModel.LinhaDetalhe>) {
            itens.clear(); itens.addAll(novos); notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetalheVH {
            val inflater = LayoutInflater.from(parent.context)
            val view = com.google.android.material.card.MaterialCardView(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (8 * parent.resources.displayMetrics.density).toInt()
                }
                val content = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(24, 24, 24, 24)
                    addView(android.widget.TextView(context).apply { id = android.view.View.generateViewId(); tag = "rota" })
                    addView(android.widget.TextView(context).apply { id = android.view.View.generateViewId(); tag = "fat" })
                    addView(android.widget.TextView(context).apply { id = android.view.View.generateViewId(); tag = "des" })
                    addView(android.widget.TextView(context).apply { id = android.view.View.generateViewId(); tag = "luc" })
                }
                addView(content)
            }
            return DetalheVH(view, moeda)
        }
        override fun getItemCount(): Int = itens.size
        override fun onBindViewHolder(holder: DetalheVH, position: Int) = holder.bind(itens[position])
    }

    private class DetalheVH(view: View, private val moeda: NumberFormat) : RecyclerView.ViewHolder(view) {
        private val rota: android.widget.TextView = (view as com.google.android.material.card.MaterialCardView).findViewWithTag("rota")
        private val fat: android.widget.TextView = view.findViewWithTag("fat")
        private val des: android.widget.TextView = view.findViewWithTag("des")
        private val luc: android.widget.TextView = view.findViewWithTag("luc")
        fun bind(l: ClosureReportViewModel.LinhaDetalhe) {
            rota.text = l.rota
            fat.text = "Faturamento: ${moeda.format(l.faturamento)}"
            des.text = "Despesas: ${moeda.format(l.despesas)}"
            luc.text = "Lucro: ${moeda.format(l.lucro)}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


