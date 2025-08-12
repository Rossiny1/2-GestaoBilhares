package com.example.gestaobilhares.ui.reports.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.databinding.ItemDetalhamentoRotaBinding
import com.example.gestaobilhares.ui.reports.viewmodel.DetalhamentoRota

/**
 * Adapter para listar detalhamento de rotas no relatório consolidado.
 */
class DetalhamentoRotasAdapter : ListAdapter<DetalhamentoRota, DetalhamentoRotasAdapter.ViewHolder>(DetalhamentoRotaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetalhamentoRotaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemDetalhamentoRotaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DetalhamentoRota) {
            binding.apply {
                // Nome da rota
                txtNomeRota.text = item.rota.nome

                // Faturamento
                txtFaturamentoAtual.text = formatarMoeda(item.faturamentoAtual)
                txtFaturamentoComparacao.text = formatarMoeda(item.faturamentoComparacao)
                txtVariacaoFaturamento.text = formatarVariacao(item.variacaoFaturamento)
                txtVariacaoFaturamento.setTextColor(getColorVariacao(item.variacaoFaturamento))

                // Clientes
                txtClientesAtual.text = item.clientesAtual.toString()
                txtClientesComparacao.text = item.clientesComparacao.toString()
                txtVariacaoClientes.text = formatarVariacao(item.variacaoClientes)
                txtVariacaoClientes.setTextColor(getColorVariacao(item.variacaoClientes))

                // Mesas
                txtMesasAtual.text = item.mesasAtual.toString()
                txtMesasComparacao.text = item.mesasComparacao.toString()
                txtVariacaoMesas.text = formatarVariacao(item.variacaoMesas)
                txtVariacaoMesas.setTextColor(getColorVariacao(item.variacaoMesas))
            }
        }

        private fun formatarMoeda(valor: Double): String {
            return "R$ ${String.format("%.2f", valor).replace(".", ",")}"
        }

        private fun formatarVariacao(variacao: Double): String {
            val sinal = if (variacao >= 0) "+" else ""
            return "$sinal${String.format("%.1f", variacao)}%"
        }

        private fun getColorVariacao(variacao: Double): Int {
            return if (variacao >= 0) {
                android.graphics.Color.parseColor("#4CAF50") // Verde
            } else {
                android.graphics.Color.parseColor("#F44336") // Vermelho
            }
        }
    }
}

/**
 * DiffUtil para otimizar atualizações da lista.
 */
class DetalhamentoRotaDiffCallback : DiffUtil.ItemCallback<DetalhamentoRota>() {
    override fun areItemsTheSame(oldItem: DetalhamentoRota, newItem: DetalhamentoRota): Boolean {
        return oldItem.rota.id == newItem.rota.id
    }

    override fun areContentsTheSame(oldItem: DetalhamentoRota, newItem: DetalhamentoRota): Boolean {
        return oldItem == newItem
    }
}
