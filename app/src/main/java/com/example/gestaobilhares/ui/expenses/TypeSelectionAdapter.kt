package com.example.gestaobilhares.ui.expenses

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.TipoDespesa
import com.example.gestaobilhares.databinding.ItemCategorySelectionBinding

/**
 * Adapter para seleção de tipos de despesas.
 */
class TypeSelectionAdapter(
    private val onTypeClick: (TipoDespesa) -> Unit
) : ListAdapter<TipoDespesa, TypeSelectionAdapter.TypeViewHolder>(TypeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TypeViewHolder {
        val binding = ItemCategorySelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TypeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TypeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TypeViewHolder(
        private val binding: ItemCategorySelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTypeClick(getItem(position))
                }
            }
        }

        fun bind(tipo: TipoDespesa) {
            binding.tvCategoryName.text = tipo.nome
            binding.rbCategory.isChecked = false // Sempre começa desmarcado
        }
    }

    private class TypeDiffCallback : DiffUtil.ItemCallback<TipoDespesa>() {
        override fun areItemsTheSame(oldItem: TipoDespesa, newItem: TipoDespesa): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TipoDespesa, newItem: TipoDespesa): Boolean {
            return oldItem == newItem
        }
    }
} 