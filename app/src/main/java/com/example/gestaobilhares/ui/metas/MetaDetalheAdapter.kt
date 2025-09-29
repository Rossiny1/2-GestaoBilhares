package com.example.gestaobilhares.ui.metas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.TipoMeta
import com.example.gestaobilhares.databinding.ItemMetaDetalheBinding
import java.text.NumberFormat
import java.util.Locale

class MetaDetalheAdapter : ListAdapter<MetaColaborador, MetaDetalheAdapter.MetaDetalheViewHolder>(MetaDetalheDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetaDetalheViewHolder {
        val binding = ItemMetaDetalheBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MetaDetalheViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MetaDetalheViewHolder, position: Int) {
        val meta = getItem(position)
        holder.bind(meta)
    }

    inner class MetaDetalheViewHolder(private val binding: ItemMetaDetalheBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(meta: MetaColaborador) {
            // Tipo da meta
            binding.tvMetaTitle.text = getTipoMetaFormatado(meta.tipoMeta)
            
            // Valor da meta
            val valorFormatado = when (meta.tipoMeta) {
                TipoMeta.FATURAMENTO, TipoMeta.TICKET_MEDIO -> NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(meta.valorMeta)
                TipoMeta.CLIENTES_ACERTADOS, TipoMeta.MESAS_LOCADAS -> String.format(Locale.getDefault(), "%.0f", meta.valorMeta)
            }
            binding.tvMetaValue.text = valorFormatado
            
            // Valor atual
            val valorAtualFormatado = when (meta.tipoMeta) {
                TipoMeta.FATURAMENTO, TipoMeta.TICKET_MEDIO -> NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(meta.valorAtual)
                TipoMeta.CLIENTES_ACERTADOS, TipoMeta.MESAS_LOCADAS -> String.format(Locale.getDefault(), "%.0f", meta.valorAtual)
            }
            binding.tvCurrentValue.text = valorAtualFormatado
            
            // Progresso da meta
            val progresso = (meta.valorAtual / meta.valorMeta * 100).coerceAtMost(100.0)
            binding.tvProgressPercentage.text = String.format(Locale.getDefault(), "%.0f%%", progresso)
            binding.progressBar.progress = progresso.toInt()
            
            // Status da meta
            when {
                progresso >= 100 -> {
                    binding.tvStatusChip.text = "ATINGIDA"
                    binding.tvStatusChip.setTextColor(binding.root.context.getColor(com.example.gestaobilhares.R.color.white))
                    binding.tvStatusChip.setBackgroundResource(com.example.gestaobilhares.R.drawable.bg_success_chip)
                }
                progresso >= 80 -> {
                    binding.tvStatusChip.text = "PRÓXIMA"
                    binding.tvStatusChip.setTextColor(binding.root.context.getColor(com.example.gestaobilhares.R.color.white))
                    binding.tvStatusChip.setBackgroundResource(com.example.gestaobilhares.R.drawable.bg_warning_chip)
                }
                else -> {
                    binding.tvStatusChip.text = "EM ANDAMENTO"
                    binding.tvStatusChip.setTextColor(binding.root.context.getColor(com.example.gestaobilhares.R.color.text_primary))
                    binding.tvStatusChip.setBackgroundResource(com.example.gestaobilhares.R.drawable.bg_status_chip)
                }
            }
        }
    }

    private fun getTipoMetaFormatado(tipo: TipoMeta): String {
        return when (tipo) {
            TipoMeta.FATURAMENTO -> "Faturamento"
            TipoMeta.CLIENTES_ACERTADOS -> "Clientes Acertados"
            TipoMeta.MESAS_LOCADAS -> "Mesas Locadas"
            TipoMeta.TICKET_MEDIO -> "Ticket Médio"
        }
    }
}

class MetaDetalheDiffCallback : DiffUtil.ItemCallback<MetaColaborador>() {
    override fun areItemsTheSame(oldItem: MetaColaborador, newItem: MetaColaborador): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MetaColaborador, newItem: MetaColaborador): Boolean {
        return oldItem == newItem
    }
}
