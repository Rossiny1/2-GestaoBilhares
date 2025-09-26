package com.example.gestaobilhares.ui.inventory.stock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.PanoEstoque
import com.google.android.material.color.MaterialColors

/**
 * Adapter para exibir detalhes individuais dos panos
 */
class PanoDetailAdapter(
    private val onPanoClick: (PanoEstoque) -> Unit = {}
) : ListAdapter<PanoEstoque, PanoDetailAdapter.PanoDetailViewHolder>(PanoDetailDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanoDetailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pano_detail, parent, false)
        return PanoDetailViewHolder(view)
    }

    override fun onBindViewHolder(holder: PanoDetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PanoDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumeroPano: TextView = itemView.findViewById(R.id.tvPanoNumero)
        private val tvInfoPano: TextView = itemView.findViewById(R.id.tvPanoInfo)
        private val tvStatusPano: TextView = itemView.findViewById(R.id.tvPanoStatus)

        fun bind(pano: PanoEstoque) {
            tvNumeroPano.text = pano.numero
            tvInfoPano.text = "${pano.cor} - ${pano.tamanho} - ${pano.material}"
            
            if (pano.disponivel) {
                tvStatusPano.text = "Disponível"
                tvStatusPano.setTextColor(MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorPrimary))
            } else {
                tvStatusPano.text = "Em Uso"
                tvStatusPano.setTextColor(MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorError))
            }
            
            itemView.setOnClickListener {
                onPanoClick(pano)
            }
        }
    }

    class PanoDetailDiffCallback : DiffUtil.ItemCallback<PanoEstoque>() {
        override fun areItemsTheSame(oldItem: PanoEstoque, newItem: PanoEstoque): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PanoEstoque, newItem: PanoEstoque): Boolean {
            return oldItem == newItem
        }
    }
}
