package com.example.gestaobilhares.ui.cycles.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.ItemCycleExpenseBinding
import com.example.gestaobilhares.ui.cycles.CycleExpenseItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter para lista de despesas do ciclo
 */
class CycleExpensesAdapter(
    private val isCicloFinalizado: Boolean,
    private val onExpenseClick: (CycleExpenseItem) -> Unit,
    private val onExpenseDelete: (CycleExpenseItem) -> Unit
) : ListAdapter<CycleExpenseItem, CycleExpensesAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemCycleExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding, onExpenseClick, onExpenseDelete)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(
        private val binding: ItemCycleExpenseBinding,
        private val onExpenseClick: (CycleExpenseItem) -> Unit,
        private val onExpenseDelete: (CycleExpenseItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        fun bind(despesa: CycleExpenseItem) {
            binding.apply {
                tvExpenseDescription.text = despesa.descricao
                tvExpenseCategory.text = despesa.categoria
                tvExpenseValue.text = currencyFormatter.format(despesa.valor)
                tvExpenseDate.text = dateFormatter.format(despesa.data)
                
                // Observações (opcional)
                if (despesa.observacoes != null && despesa.observacoes.isNotEmpty()) {
                    tvExpenseObservations.text = despesa.observacoes
                    tvExpenseObservations.visibility = android.view.View.VISIBLE
                } else {
                    tvExpenseObservations.visibility = android.view.View.GONE
                }

                // Click listeners (apenas para ciclos em andamento)
                if (!isCicloFinalizado) {
                    root.setOnClickListener {
                        onExpenseClick(despesa)
                    }

                    btnDeleteExpense.setOnClickListener {
                        onExpenseDelete(despesa)
                    }
                    
                    btnDeleteExpense.visibility = android.view.View.VISIBLE
                } else {
                    root.setOnClickListener(null)
                    btnDeleteExpense.setOnClickListener(null)
                    btnDeleteExpense.visibility = android.view.View.GONE
                }
            }
        }
    }

    private class ExpenseDiffCallback : DiffUtil.ItemCallback<CycleExpenseItem>() {
        override fun areItemsTheSame(oldItem: CycleExpenseItem, newItem: CycleExpenseItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CycleExpenseItem, newItem: CycleExpenseItem): Boolean {
            return oldItem == newItem
        }
    }
}