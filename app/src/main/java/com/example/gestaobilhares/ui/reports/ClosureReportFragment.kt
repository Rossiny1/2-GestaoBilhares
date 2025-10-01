package com.example.gestaobilhares.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.example.gestaobilhares.databinding.FragmentClosureReportBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        adapter = DetalheAdapter(moeda)
        binding.recyclerDetalhes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDetalhes.adapter = adapter

        setupObservers()
        setupClickListeners()

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

        viewModel.acertos.observe(viewLifecycleOwner) { acertos ->
            val labels = acertos.map { it.descricao }
            val a = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, labels)
            binding.spinnerCiclo.setAdapter(a)
            binding.spinnerCiclo.setText(labels.firstOrNull() ?: "", false)
            binding.spinnerCiclo.threshold = 0
            binding.spinnerCiclo.setOnClickListener { binding.spinnerCiclo.showDropDown() }
            binding.spinnerCiclo.setOnItemClickListener { _, _, pos, _ -> viewModel.selecionarAcerto(pos) }
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

    private fun setupClickListeners() {
        binding.btnGenerateReport.setOnClickListener {
            showReportOptionsDialog()
        }
    }

    private fun showReportOptionsDialog() {
        val resumo = viewModel.resumo.value
        val detalhes = viewModel.detalhes.value
        val totalMesas = viewModel.totalMesasLocadas.value ?: 0
        val anoSelecionado = viewModel.anos.value?.firstOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        
        // ✅ CORRIGIDO: Pegar o acerto realmente selecionado no spinner
        val acertoSelecionado = viewModel.acertos.value?.find { acerto ->
            acerto.descricao == binding.spinnerCiclo.text.toString()
        } ?: viewModel.acertos.value?.firstOrNull()

        if (resumo == null || detalhes == null) {
            Toast.makeText(requireContext(), "Nenhum dado disponível para gerar relatório", Toast.LENGTH_SHORT).show()
            return
        }

        val options = arrayOf("Relatório por Acerto", "Relatório Anual")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Tipo de Relatório")
            .setItems(options) { _, which ->
                // Gerar dados dos gráficos em background
                lifecycleScope.launch {
                    try {
                        val chartData = viewModel.generateChartData()
                        
                        when (which) {
                            0 -> {
                                // Relatório por acerto
                                val numeroAcerto = acertoSelecionado?.numero ?: 1
                                val dialog = ClosureReportDialog.newInstance(
                                    anoSelecionado,
                                    numeroAcerto,
                                    resumo,
                                    detalhes,
                                    totalMesas,
                                    false,
                                    chartData
                                )
                                dialog.show(parentFragmentManager, "ClosureReportDialog")
                            }
                            1 -> {
                                // Relatório anual
                                val dialog = ClosureReportDialog.newInstance(
                                    anoSelecionado,
                                    0,
                                    resumo,
                                    detalhes,
                                    totalMesas,
                                    true,
                                    chartData
                                )
                                dialog.show(parentFragmentManager, "ClosureReportDialog")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ClosureReportFragment", "Erro ao gerar dados dos gráficos: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao gerar gráficos: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
            val despesasRotas = resumo.despesasRotas
            val despesasGlobais = resumo.despesasGlobais
            val comissaoMotorista = resumo.comissaoMotorista
            val comissaoIltair = resumo.comissaoIltair
            val totalDescontos = resumo.totalDescontos
            
            val faturamentoPorMesa = if (totalMesas > 0) faturamentoTotal / totalMesas else 0.0
            val lucroPorMesa = if (totalMesas > 0) lucro / totalMesas else 0.0
            val despesaPorMesa = if (totalMesas > 0) despesasTotal / totalMesas else 0.0
            val margem = if (faturamentoTotal > 0) (lucro / faturamentoTotal) * 100.0 else 0.0

            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Resumo do Acerto")
                .setMessage(
                    "Total de mesas: $totalMesas\n" +
                    "Faturamento por mesa: ${moeda.format(faturamentoPorMesa)}\n" +
                    "Lucro por mesa: ${moeda.format(lucroPorMesa)}\n" +
                    "Despesa por mesa: ${moeda.format(despesaPorMesa)}\n" +
                    "Margem líquida: ${String.format(Locale("pt", "BR"), "%.1f%%", margem)}\n" +
                    "Total de Descontos: ${moeda.format(totalDescontos)}\n\n" +
                    "Composição das Despesas:\n" +
                    "• Despesas Rota: ${moeda.format(despesasRotas)}\n" +
                    "• Despesas Globais: ${moeda.format(despesasGlobais)}\n" +
                    "• Comissão Motorista (3%): ${moeda.format(comissaoMotorista)}\n" +
                    "• Comissão Iltair (2%): ${moeda.format(comissaoIltair)}\n" +
                    "• Total Despesas: ${moeda.format(despesasTotal)}\n\n" +
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


