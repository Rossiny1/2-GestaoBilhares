package com.example.gestaobilhares.ui.reports.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.databinding.ItemColaboradorPerformanceBinding
import com.example.gestaobilhares.ui.reports.viewmodel.ColaboradorPerformance
import com.example.gestaobilhares.ui.reports.viewmodel.StatusPerformance

/**
 * Adapter para listar performance dos colaboradores.
 */
class ColaboradorPerformanceAdapter(
    private val onItemClick: (ColaboradorPerformance) -> Unit
) : ListAdapter<ColaboradorPerformance, ColaboradorPerformanceAdapter.ViewHolder>(ColaboradorPerformanceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemColaboradorPerformanceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemColaboradorPerformanceBinding,
        private val onItemClick: (ColaboradorPerformance) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ColaboradorPerformance) {
            binding.apply {
                // Nome do colaborador
                txtNomeColaborador.text = item.colaborador.nome

                // Rotas responsáveis (mock por enquanto)
                txtRotasResponsaveis.text = "Rotas: Zona Sul, Zona Norte"

                // Métricas
                txtFaturamento.text = formatarMoeda(item.faturamentoRealizado)
                txtClientesAcertados.text = "${item.clientesAcertadosRealizado.toInt()}/${item.clientesAcertadosMeta.toInt()}"
                txtMesasLocadas.text = "${item.mesasLocadasRealizado}/${item.mesasLocadasMeta}"
                txtTicketMedio.text = formatarMoeda(item.ticketMedioRealizado)

                // Percentuais
                txtPercentualClientes.text = "${String.format("%.1f", item.percentualClientesAcertados)}%"
                txtPercentualMesas.text = "${String.format("%.1f", item.percentualMesasLocadas)}%"

                // Status de performance
                txtStatusPerformance.text = item.statusGeral.name
                txtStatusPerformance.setTextColor(getColorStatus(item.statusGeral))

                // Progresso das metas
                progressBarFaturamento.progress = item.percentualFaturamento.toInt()
                progressBarClientes.progress = item.percentualClientesAcertados.toInt()
                progressBarMesas.progress = item.percentualMesasLocadas.toInt()

                // Click listener
                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }

        private fun formatarMoeda(valor: Double): String {
            return "R$ ${String.format("%.2f", valor).replace(".", ",")}"
        }

        private fun getColorStatus(status: StatusPerformance): Int {
            return when (status) {
                StatusPerformance.EXCELENTE -> android.graphics.Color.parseColor("#4CAF50") // Verde
                StatusPerformance.BOM -> android.graphics.Color.parseColor("#2196F3") // Azul
                StatusPerformance.REGULAR -> android.graphics.Color.parseColor("#FF9800") // Laranja
                StatusPerformance.RUIM -> android.graphics.Color.parseColor("#F44336") // Vermelho
                StatusPerformance.PENDENTE -> android.graphics.Color.parseColor("#9E9E9E") // Cinza
            }
        }
    }
}

/**
 * DiffUtil para otimizar atualizações da lista.
 */
class ColaboradorPerformanceDiffCallback : DiffUtil.ItemCallback<ColaboradorPerformance>() {
    override fun areItemsTheSame(oldItem: ColaboradorPerformance, newItem: ColaboradorPerformance): Boolean {
        return oldItem.colaborador.id == newItem.colaborador.id
    }

    override fun areContentsTheSame(oldItem: ColaboradorPerformance, newItem: ColaboradorPerformance): Boolean {
        return oldItem == newItem
    }
}
