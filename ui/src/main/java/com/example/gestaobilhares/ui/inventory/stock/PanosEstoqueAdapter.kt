package com.example.gestaobilhares.ui.inventory.stock
import com.example.gestaobilhares.ui.R

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.PanoEstoque

/**
 * Adapter para exibir panos do estoque na RecyclerView
 */
class PanosEstoqueAdapter(
    private val onPanoClick: (PanoEstoque) -> Unit = {}
) : ListAdapter<PanoEstoque, PanosEstoqueAdapter.PanoViewHolder>(PanoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(com.example.gestaobilhares.ui.R.layout.item_pano_estoque, parent, false)
        return PanoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PanoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PanoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumeroPano: TextView = itemView.findViewById(com.example.gestaobilhares.ui.R.id.tvNumeroPano)
        private val tvInfoPano: TextView = itemView.findViewById(com.example.gestaobilhares.ui.R.id.tvInfoPano)
        private val tvStatusPano: TextView = itemView.findViewById(com.example.gestaobilhares.ui.R.id.tvStatusPano)
        private val tvObservacoes: TextView = itemView.findViewById(com.example.gestaobilhares.ui.R.id.tvObservacoes)

        fun bind(pano: PanoEstoque) {
            tvNumeroPano.text = pano.numero
            tvInfoPano.text = "${pano.cor} - ${pano.tamanho} - ${pano.material}"
            
            if (pano.disponivel) {
                tvStatusPano.text = "Disponível"
                val successColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorPrimary)
                tvStatusPano.setTextColor(successColor)
            } else {
                tvStatusPano.text = "Em Uso"
                val errorColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorError)
                tvStatusPano.setTextColor(errorColor)
            }
            
            if (!pano.observacoes.isNullOrEmpty()) {
                tvObservacoes.text = pano.observacoes
                tvObservacoes.visibility = View.VISIBLE
            } else {
                tvObservacoes.visibility = View.GONE
            }
            
            itemView.setOnClickListener {
                onPanoClick(pano)
            }
        }
    }

    class PanoDiffCallback : DiffUtil.ItemCallback<PanoEstoque>() {
        override fun areItemsTheSame(oldItem: PanoEstoque, newItem: PanoEstoque): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PanoEstoque, newItem: PanoEstoque): Boolean {
            return oldItem == newItem
        }
    }
}

