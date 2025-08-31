package com.example.gestaobilhares.ui.expenses.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.databinding.ItemExpenseCategoryBinding
import com.example.gestaobilhares.data.entities.CategoriaDespesa

class ExpenseCategoryAdapter(
    private val categories: List<CategoriaDespesa>,
    private val onEditClick: (CategoriaDespesa) -> Unit,
    private val onDeleteClick: (CategoriaDespesa) -> Unit
) : RecyclerView.Adapter<ExpenseCategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(
        private val binding: ItemExpenseCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: CategoriaDespesa) {
            binding.tvCategoryName.text = category.nome
            binding.tvTypeCount.text = "Categoria ativa"

            binding.btnEdit.setOnClickListener {
                onEditClick(category)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(category)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemExpenseCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    fun moveItem(fromPosition: Int, toPosition: Int) {
        // TODO: Implementar reordenação no banco de dados
        notifyItemMoved(fromPosition, toPosition)
    }
}
