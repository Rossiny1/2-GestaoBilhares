package com.example.gestaobilhares.ui.expenses

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.CategoriaDespesa
import com.example.gestaobilhares.databinding.ItemCategorySelectionBinding

/**
 * Adapter para seleção de categorias de despesas.
 */
class CategorySelectionAdapter(
    private val onCategoryClick: (CategoriaDespesa) -> Unit
) : ListAdapter<CategoriaDespesa, CategorySelectionAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategorySelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategorySelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCategoryClick(getItem(position))
                }
            }
        }

        fun bind(categoria: CategoriaDespesa) {
            binding.tvCategoryName.text = categoria.nome
            binding.rbCategory.isChecked = false // Sempre começa desmarcado
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<CategoriaDespesa>() {
        override fun areItemsTheSame(oldItem: CategoriaDespesa, newItem: CategoriaDespesa): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoriaDespesa, newItem: CategoriaDespesa): Boolean {
            return oldItem == newItem
        }
    }
} 