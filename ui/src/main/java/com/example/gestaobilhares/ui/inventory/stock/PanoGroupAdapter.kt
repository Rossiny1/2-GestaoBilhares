package com.example.gestaobilhares.ui.inventory.stock
import com.example.gestaobilhares.ui.R

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.PanoEstoque

/**
 * Adapter para exibir grupos de panos na RecyclerView
 */
class PanoGroupAdapter(
    private val onGroupClick: (PanoGroup) -> Unit = {}
) : ListAdapter<PanoGroup, PanoGroupAdapter.PanoGroupViewHolder>(PanoGroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanoGroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(com.example.gestaobilhares.ui.R.layout.item_pano_group, parent, false)
        return PanoGroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: PanoGroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PanoGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPanoInfo: TextView = itemView.findViewById(com.example.gestaobilhares.ui.R.id.tvPanoInfo)
        private val tvPanoNumeracao: TextView = itemView.findViewById(com.example.gestaobilhares.ui.R.id.tvPanoNumeracao)
        private val tvQuantidade: TextView = itemView.findViewById(com.example.gestaobilhares.ui.R.id.tvQuantidade)
        private val tvObservacoes: TextView = itemView.findViewById(com.example.gestaobilhares.ui.R.id.tvObservacoes)

        fun bind(panoGroup: PanoGroup) {
            // Informações principais
            tvPanoInfo.text = "Panos ${panoGroup.cor} - ${panoGroup.tamanho} - ${panoGroup.material}"
            
            // Numeração
            if (panoGroup.panos.size == 1) {
                tvPanoNumeracao.text = panoGroup.numeroInicial
            } else {
                tvPanoNumeracao.text = "${panoGroup.numeroInicial} a ${panoGroup.numeroFinal}"
            }
            
            // Quantidade disponível
            tvQuantidade.text = "${panoGroup.quantidadeDisponivel} disponíveis"
            
            // Observações
            if (!panoGroup.observacoes.isNullOrEmpty()) {
                tvObservacoes.text = "Observações: ${panoGroup.observacoes}"
                tvObservacoes.visibility = View.VISIBLE
            } else {
                tvObservacoes.visibility = View.GONE
            }
            
            // Click listener
            itemView.setOnClickListener {
                onGroupClick(panoGroup)
            }
        }
    }

    class PanoGroupDiffCallback : DiffUtil.ItemCallback<PanoGroup>() {
        override fun areItemsTheSame(oldItem: PanoGroup, newItem: PanoGroup): Boolean {
            return oldItem.cor == newItem.cor && 
                   oldItem.tamanho == newItem.tamanho && 
                   oldItem.material == newItem.material
        }

        override fun areContentsTheSame(oldItem: PanoGroup, newItem: PanoGroup): Boolean {
            return oldItem == newItem
        }
    }
}

