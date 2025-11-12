package com.example.gestaobilhares.ui.mesas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import com.example.gestaobilhares.databinding.ItemHistoricoManutencaoBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter para a lista de histórico de manutenção de uma mesa.
 */
class HistoricoManutencaoMesaAdapter : ListAdapter<HistoricoManutencaoMesa, HistoricoManutencaoMesaAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoricoManutencaoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemHistoricoManutencaoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(historico: HistoricoManutencaoMesa) {
            // Tipo de manutenção com emoji
            val tipoText = when (historico.tipoManutencao) {
                TipoManutencao.PINTURA -> "🎨 Pintura"
                TipoManutencao.TROCA_PANO -> "🟢 Troca de Pano"
                TipoManutencao.TROCA_TABELA -> "🪑 Troca de Tabela"
                TipoManutencao.REPARO_ESTRUTURAL -> "🔧 Reparo Estrutural"
                TipoManutencao.LIMPEZA -> "🧽 Limpeza"
                TipoManutencao.OUTROS -> "🔧 Outros"
            }
            binding.tvTipoManutencao.text = tipoText

            // Data da manutenção
            binding.tvDataManutencao.text = dateFormat.format(historico.dataManutencao)

            // Descrição
            if (!historico.descricao.isNullOrBlank()) {
                binding.tvDescricao.text = historico.descricao
                binding.tvDescricao.visibility = View.VISIBLE
            } else {
                binding.tvDescricao.visibility = View.GONE
            }

            // Responsável
            if (!historico.responsavel.isNullOrBlank()) {
                binding.tvResponsavel.text = historico.responsavel
                binding.tvResponsavel.visibility = View.VISIBLE
            } else {
                binding.tvResponsavel.visibility = View.GONE
            }

            // Custo
            val custo = historico.custo
            if (custo != null && custo > 0) {
                binding.tvCusto.text = "R$ ${String.format("%.2f", custo)}"
                binding.layoutCusto.visibility = View.VISIBLE
            } else {
                binding.layoutCusto.visibility = View.GONE
            }

            // Observações
            if (!historico.observacoes.isNullOrBlank()) {
                binding.tvObservacoes.text = "Observações: ${historico.observacoes}"
                binding.tvObservacoes.visibility = View.VISIBLE
            } else {
                binding.tvObservacoes.visibility = View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<HistoricoManutencaoMesa>() {
        override fun areItemsTheSame(oldItem: HistoricoManutencaoMesa, newItem: HistoricoManutencaoMesa): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HistoricoManutencaoMesa, newItem: HistoricoManutencaoMesa): Boolean {
            return oldItem == newItem
        }
    }
}
