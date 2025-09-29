package com.example.gestaobilhares.ui.metas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.MetaRotaResumo
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.example.gestaobilhares.data.entities.TipoMeta
import com.example.gestaobilhares.databinding.ItemMetaDetalheBinding
import com.example.gestaobilhares.databinding.ItemMetaRotaBinding
import java.text.NumberFormat
import java.util.Locale

class MetasAdapter(private val onDetailsClick: (MetaRotaResumo) -> Unit) :
    ListAdapter<MetaRotaResumo, MetasAdapter.MetaRotaViewHolder>(MetaRotaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetaRotaViewHolder {
        val binding = ItemMetaRotaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MetaRotaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MetaRotaViewHolder, position: Int) {
        val metaRota = getItem(position)
        holder.bind(metaRota)
    }

    inner class MetaRotaViewHolder(private val binding: ItemMetaRotaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(metaRota: MetaRotaResumo) {
            binding.tvRotaNome.text = metaRota.rota.nome
            binding.tvColaboradorResponsavel.text = "${metaRota.colaboradorResponsavel?.nome ?: "N/A"}"
            binding.tvPeriodoCiclo.text = "Ciclo ${metaRota.cicloAtual} - ${metaRota.anoCiclo}"
            binding.tvProgressoGeral.text = String.format(Locale.getDefault(), "%.0f%%", metaRota.progressoGeral)

            // RecyclerView aninhado para metas
            val detalheAdapter = MetaDetalheAdapter()
            binding.rvMetas.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = detalheAdapter
                isNestedScrollingEnabled = false
            }
            detalheAdapter.submitList(metaRota.metas)

            binding.btnDetalhes.setOnClickListener {
                onDetailsClick(metaRota)
            }
        }
    }

    private class MetaRotaDiffCallback : DiffUtil.ItemCallback<MetaRotaResumo>() {
        override fun areItemsTheSame(oldItem: MetaRotaResumo, newItem: MetaRotaResumo): Boolean {
            return oldItem.rota.id == newItem.rota.id &&
                   oldItem.cicloAtual == newItem.cicloAtual &&
                   oldItem.anoCiclo == newItem.anoCiclo
        }

        override fun areContentsTheSame(oldItem: MetaRotaResumo, newItem: MetaRotaResumo): Boolean {
            return oldItem == newItem
        }
    }
}

class MetaDetalheAdapter :
    ListAdapter<MetaColaborador, MetaDetalheAdapter.MetaDetalheViewHolder>(MetaDetalheDiffCallback()) {

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
            binding.tvMetaTitle.text = when (meta.tipoMeta) {
                TipoMeta.FATURAMENTO -> "Faturamento"
                TipoMeta.CLIENTES_ACERTADOS -> "Clientes Acertados"
                TipoMeta.MESAS_LOCADAS -> "Mesas Locadas"
                TipoMeta.TICKET_MEDIO -> "Ticket Médio"
            }
            binding.tvMetaValue.text = formatMetaValue(meta.tipoMeta, meta.valorMeta)
            binding.tvCurrentValue.text = formatMetaValue(meta.tipoMeta, meta.valorAtual)

            val progress = if (meta.valorMeta > 0) (meta.valorAtual / meta.valorMeta * 100).coerceAtMost(100.0) else 0.0
            binding.progressBar.progress = progress.toInt()
            binding.tvProgressPercentage.text = String.format(Locale.getDefault(), "%.0f%%", progress)

            val context = binding.root.context
            val progressColor = when {
                progress >= 100.0 -> ContextCompat.getColor(context, R.color.progress_success)
                progress >= 80.0 -> ContextCompat.getColor(context, R.color.progress_warning)
                else -> ContextCompat.getColor(context, R.color.accent_teal)
            }
            binding.progressBar.progressTintList = ContextCompat.getColorStateList(context, progressColor)

            val statusText: String
            val statusBackground: Int
            val statusTextColor: Int

            when {
                progress >= 100.0 -> {
                    statusText = "ATINGIDA"
                    statusBackground = R.drawable.bg_success_chip
                    statusTextColor = ContextCompat.getColor(context, R.color.white)
                }
                progress >= 80.0 -> {
                    statusText = "PRÓXIMA"
                    statusBackground = R.drawable.bg_warning_chip
                    statusTextColor = ContextCompat.getColor(context, R.color.white)
                }
                else -> {
                    statusText = "EM ANDAMENTO"
                    statusBackground = R.drawable.bg_status_chip
                    statusTextColor = ContextCompat.getColor(context, R.color.text_primary)
                }
            }
            binding.tvStatusChip.text = statusText
            binding.tvStatusChip.setBackgroundResource(statusBackground)
            binding.tvStatusChip.setTextColor(statusTextColor)
        }

        private fun formatMetaValue(tipoMeta: TipoMeta, value: Double): String {
            return when (tipoMeta) {
                TipoMeta.FATURAMENTO, TipoMeta.TICKET_MEDIO -> NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
                TipoMeta.CLIENTES_ACERTADOS, TipoMeta.MESAS_LOCADAS -> String.format(Locale.getDefault(), "%.0f", value)
            }
        }
    }

    private class MetaDetalheDiffCallback : DiffUtil.ItemCallback<MetaColaborador>() {
        override fun areItemsTheSame(oldItem: MetaColaborador, newItem: MetaColaborador): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MetaColaborador, newItem: MetaColaborador): Boolean {
            return oldItem == newItem
        }
    }
}