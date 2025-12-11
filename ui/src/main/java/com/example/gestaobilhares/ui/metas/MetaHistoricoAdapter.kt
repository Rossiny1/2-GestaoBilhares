package com.example.gestaobilhares.ui.metas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.TipoMeta
import com.example.gestaobilhares.ui.databinding.ItemMetaHistoricoBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter para exibir lista de metas no histórico
 */
class MetaHistoricoAdapter(
    initialList: List<MetaHistoricoFragment.MetaColaboradorComProgresso>
) : ListAdapter<MetaHistoricoFragment.MetaColaboradorComProgresso, MetaHistoricoAdapter.MetaHistoricoViewHolder>(
    MetaHistoricoDiffCallback()
) {

    init {
        submitList(initialList)
    }

    fun atualizarMetas(metas: List<MetaHistoricoFragment.MetaColaboradorComProgresso>) {
        submitList(metas)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetaHistoricoViewHolder {
        val binding = ItemMetaHistoricoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MetaHistoricoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MetaHistoricoViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class MetaHistoricoViewHolder(
        private val binding: ItemMetaHistoricoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MetaHistoricoFragment.MetaColaboradorComProgresso) {
            val meta = item.meta

            // Título da meta
            binding.tvMetaTitle.text = getTipoMetaFormatado(meta.tipoMeta)

            // ✅ NOVO: Determinar status da meta baseado em se foi batida ou não
            // Se a meta está inativa (finalizada), verificar se foi batida
            val statusTexto: String
            val statusCor: Int
            
            if (!meta.ativo) {
                // Meta finalizada - verificar se foi batida
                if (item.valorAtual >= meta.valorMeta) {
                    statusTexto = "BATIDA"
                    statusCor = android.graphics.Color.parseColor("#4CAF50") // Verde
                } else {
                    statusTexto = "NÃO BATIDA"
                    statusCor = android.graphics.Color.parseColor("#F44336") // Vermelho
                }
            } else {
                // Meta ainda em andamento
                statusTexto = "EM ANDAMENTO"
                statusCor = android.graphics.Color.parseColor("#2196F3") // Azul
            }
            
            binding.tvStatusChip.text = statusTexto
            binding.tvStatusChip.setTextColor(statusCor)
            binding.tvStatusChip.visibility = android.view.View.VISIBLE

            // Valores
            binding.tvMetaValue.text = formatarValor(meta.valorMeta)
            binding.tvCurrentValue.text = formatarValor(item.valorAtual)

            // Progresso (pode ultrapassar 100% se foi batida)
            val progressoInt = if (item.progresso >= 100.0) {
                100 // Limitar visualmente a 100%
            } else {
                item.progresso.toInt().coerceIn(0, 100)
            }
            binding.progressBar.progress = progressoInt
            
            // Mostrar porcentagem, mas se foi batida, mostrar "100%+" ou valor real
            val porcentagemTexto = if (item.valorAtual >= meta.valorMeta && meta.valorMeta > 0) {
                "${((item.valorAtual / meta.valorMeta) * 100).toInt()}%"
            } else {
                "${progressoInt}%"
            }
            binding.tvProgressPercentage.text = porcentagemTexto
        }

        private fun getTipoMetaFormatado(tipoMeta: TipoMeta): String {
            return when (tipoMeta) {
                TipoMeta.FATURAMENTO -> "Faturamento"
                TipoMeta.CLIENTES_ACERTADOS -> "Clientes Acertados"
                TipoMeta.MESAS_LOCADAS -> "Mesas Locadas"
                TipoMeta.TICKET_MEDIO -> "Ticket Médio"
            }
        }

        private fun formatarValor(valor: Double): String {
            return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)
        }
    }

    private class MetaHistoricoDiffCallback :
        DiffUtil.ItemCallback<MetaHistoricoFragment.MetaColaboradorComProgresso>() {
        override fun areItemsTheSame(
            oldItem: MetaHistoricoFragment.MetaColaboradorComProgresso,
            newItem: MetaHistoricoFragment.MetaColaboradorComProgresso
        ): Boolean {
            return oldItem.meta.id == newItem.meta.id
        }

        override fun areContentsTheSame(
            oldItem: MetaHistoricoFragment.MetaColaboradorComProgresso,
            newItem: MetaHistoricoFragment.MetaColaboradorComProgresso
        ): Boolean {
            return oldItem == newItem
        }
    }
}

