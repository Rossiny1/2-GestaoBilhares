package com.example.gestaobilhares.ui.expenses.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.databinding.ItemExpenseTypeBinding
import com.example.gestaobilhares.data.entities.TipoDespesaComCategoria

class ExpenseTypeAdapter(
    private val types: List<TipoDespesaComCategoria>,
    private val onEditClick: (TipoDespesaComCategoria) -> Unit,
    private val onDeleteClick: (TipoDespesaComCategoria) -> Unit
) : RecyclerView.Adapter<ExpenseTypeAdapter.TypeViewHolder>() {

    inner class TypeViewHolder(
        private val binding: ItemExpenseTypeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(type: TipoDespesaComCategoria) {
            binding.tvTypeName.text = type.nome
            binding.tvCategoryName.text = "Categoria: ${type.categoriaNome}"

            binding.btnEdit.setOnClickListener {
                onEditClick(type)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(type)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TypeViewHolder {
        val binding = ItemExpenseTypeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TypeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TypeViewHolder, position: Int) {
        holder.bind(types[position])
    }

    override fun getItemCount(): Int = types.size

    fun moveItem(fromPosition: Int, toPosition: Int) {
        // TODO: Implementar reordenação no banco de dados
        notifyItemMoved(fromPosition, toPosition)
    }
}
