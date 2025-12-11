package com.example.gestaobilhares.ui.metas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.MetaRotaResumo
import com.example.gestaobilhares.data.entities.TipoMeta
import com.example.gestaobilhares.ui.databinding.ItemMetaHistoricoRotaBinding

/**
 * Adapter para mostrar cards de histórico de metas por rota
 * ✅ REFATORADO: Agora mostra UM CARD POR ROTA com todas as metas daquela rota
 */
class MetaHistoricoAdapter(
    private var metasPorRota: List<MetaRotaResumo>
) : RecyclerView.Adapter<MetaHistoricoAdapter.MetaRotaViewHolder>() {

    fun atualizarMetas(novasMetas: List<MetaRotaResumo>) {
        metasPorRota = novasMetas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetaRotaViewHolder {
        val binding = ItemMetaHistoricoRotaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MetaRotaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MetaRotaViewHolder, position: Int) {
        holder.bind(metasPorRota[position])
    }

    override fun getItemCount(): Int = metasPorRota.size

    inner class MetaRotaViewHolder(
        private val binding: ItemMetaHistoricoRotaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(metaRota: MetaRotaResumo) {
            // Nome da rota e informações do ciclo
            binding.tvRotaNome.text = metaRota.rota.nome
            binding.tvCicloInfo.text = "${metaRota.cicloAtual}º Acerto ${metaRota.rota.nome} - ${metaRota.anoCiclo}"
            
            // Status do ciclo
            binding.tvStatusCiclo.text = metaRota.getStatusCicloFormatado()
            
            // Período do ciclo
            binding.tvPeriodoCiclo.text = metaRota.getPeriodoCicloFormatado()
            
            // Lista de metas com indicadores
            setupMetasList(metaRota)
        }

        private fun setupMetasList(metaRota: MetaRotaResumo) {
            val metasAdapter = MetaDetalheHistoricoAdapter(metaRota.metas)
            binding.rvMetas.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = metasAdapter
                isNestedScrollingEnabled = false
            }
        }
    }

    /**
     * Adapter interno para exibir detalhes de cada meta individual
     */
    inner class MetaDetalheHistoricoAdapter(
        private val metas: List<com.example.gestaobilhares.data.entities.MetaColaborador>
    ) : RecyclerView.Adapter<MetaDetalheHistoricoAdapter.MetaDetalheViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetaDetalheViewHolder {
            val binding = com.example.gestaobilhares.ui.databinding.ItemMetaDetalheHistoricoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return MetaDetalheViewHolder(binding)
        }

        override fun onBindViewHolder(holder: MetaDetalheViewHolder, position: Int) {
            holder.bind(metas[position])
        }

        override fun getItemCount(): Int = metas.size

        inner class MetaDetalheViewHolder(
            private val binding: com.example.gestaobilhares.ui.databinding.ItemMetaDetalheHistoricoBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(meta: com.example.gestaobilhares.data.entities.MetaColaborador) {
                // Tipo da meta
                binding.tvTipoMeta.text = getTipoMetaFormatado(meta.tipoMeta)
                
                // Valor da meta
                binding.tvValorMeta.text = formatarValor(meta.tipoMeta, meta.valorMeta)
                
                // Valor atingido
                binding.tvValorAtingido.text = formatarValor(meta.tipoMeta, meta.valorAtual)
                
                // ✅ ATUALIZADO: Usar emoji em vez de ImageView
                val metaBatida = meta.valorAtual >= meta.valorMeta
                binding.tvEmojiIndicador.text = if (metaBatida) "✅" else "❌"
                
                // Percentual de progresso
                val progresso = if (meta.valorMeta > 0) {
                    (meta.valorAtual / meta.valorMeta) * 100
                } else {
                    0.0
                }
                binding.tvProgresso.text = String.format("%.1f%%", progresso)
            }

            private fun getTipoMetaFormatado(tipoMeta: TipoMeta): String {
                return when (tipoMeta) {
                    TipoMeta.FATURAMENTO -> "Faturamento"
                    TipoMeta.CLIENTES_ACERTADOS -> "Clientes Acertados"
                    TipoMeta.MESAS_LOCADAS -> "Mesas Locadas"
                    TipoMeta.TICKET_MEDIO -> "Ticket Médio"
                }
            }

            private fun formatarValor(tipoMeta: TipoMeta, valor: Double): String {
                return when (tipoMeta) {
                    TipoMeta.FATURAMENTO, TipoMeta.TICKET_MEDIO -> 
                        String.format("R$ %.2f", valor)
                    TipoMeta.CLIENTES_ACERTADOS, TipoMeta.MESAS_LOCADAS -> 
                        valor.toInt().toString()
                }
            }
        }
    }
}
