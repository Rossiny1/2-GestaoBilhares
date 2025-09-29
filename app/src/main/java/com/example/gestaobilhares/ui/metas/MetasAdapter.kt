package com.example.gestaobilhares.ui.metas

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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

            // ✅ NOVO: Exibir metas dinamicamente
            exibirMetasDinamicamente(metaRota.metas)

            binding.btnDetalhes.setOnClickListener {
                onDetailsClick(metaRota)
            }
        }

        /**
         * Exibe as metas dinamicamente no layout
         */
        private fun exibirMetasDinamicamente(metas: List<MetaColaborador>) {
            val layoutMetas = binding.root.findViewById<LinearLayout>(R.id.layoutMetas)
            layoutMetas?.removeAllViews()

            if (metas.isNotEmpty()) {
                for (meta in metas) {
                    val metaView = criarViewMeta(meta)
                    layoutMetas?.addView(metaView)
                }
            }
        }

        /**
         * Cria uma view para uma meta específica
         */
        private fun criarViewMeta(meta: MetaColaborador): LinearLayout {
            val context = binding.root.context
            val metaLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 0, 16)
            }

            // Linha com nome da meta e valores
            val linhaMeta = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val tvNomeMeta = TextView(context).apply {
                text = getTipoMetaFormatado(meta.tipoMeta)
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvValoresMeta = TextView(context).apply {
                text = formatarValoresMeta(meta)
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }

            linhaMeta.addView(tvNomeMeta)
            linhaMeta.addView(tvValoresMeta)

            // Barra de progresso
            val progressBar = com.google.android.material.progressindicator.LinearProgressIndicator(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4
                }
                progress = calcularProgresso(meta).toInt()
                setIndicatorColor(ContextCompat.getColor(context, R.color.accent_teal))
                trackColor = ContextCompat.getColor(context, R.color.progress_track)
                trackThickness = 8
            }

            metaLayout.addView(linhaMeta)
            metaLayout.addView(progressBar)

            return metaLayout
        }

        /**
         * Formata os valores da meta
         */
        private fun formatarValoresMeta(meta: MetaColaborador): String {
            return when (meta.tipoMeta) {
                TipoMeta.FATURAMENTO, TipoMeta.TICKET_MEDIO -> {
                    val formatador = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
                    "${formatador.format(meta.valorAtual)} / ${formatador.format(meta.valorMeta)}"
                }
                TipoMeta.CLIENTES_ACERTADOS, TipoMeta.MESAS_LOCADAS -> {
                    "${String.format("%.0f", meta.valorAtual)} / ${String.format("%.0f", meta.valorMeta)}"
                }
            }
        }

        /**
         * Calcula o progresso da meta
         */
        private fun calcularProgresso(meta: MetaColaborador): Float {
            return if (meta.valorMeta > 0) {
                ((meta.valorAtual / meta.valorMeta) * 100.0).coerceAtMost(100.0).toFloat()
            } else 0f
        }

        /**
         * Formata o tipo de meta para exibição
         */
        private fun getTipoMetaFormatado(tipoMeta: TipoMeta): String {
            return when (tipoMeta) {
                TipoMeta.FATURAMENTO -> "Faturamento"
                TipoMeta.CLIENTES_ACERTADOS -> "Clientes Acertados"
                TipoMeta.MESAS_LOCADAS -> "Mesas Locadas"
                TipoMeta.TICKET_MEDIO -> "Ticket Médio"
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