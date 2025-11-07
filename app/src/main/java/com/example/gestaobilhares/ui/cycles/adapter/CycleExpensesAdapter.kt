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
    private val onExpenseDelete: (CycleExpenseItem) -> Unit,
    private val onViewPhoto: (CycleExpenseItem) -> Unit
) : ListAdapter<CycleExpenseItem, CycleExpensesAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemCycleExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding, onExpenseClick, onExpenseDelete, onViewPhoto)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(
        private val binding: ItemCycleExpenseBinding,
        private val onExpenseClick: (CycleExpenseItem) -> Unit,
        private val onExpenseDelete: (CycleExpenseItem) -> Unit,
        private val onViewPhoto: (CycleExpenseItem) -> Unit
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

                // ✅ NOVO: Botão de visualizar foto - mostrar apenas se houver foto
                if (!despesa.fotoComprovante.isNullOrEmpty()) {
                    btnViewPhoto.visibility = android.view.View.VISIBLE
                    btnViewPhoto.setOnClickListener {
                        onViewPhoto(despesa)
                    }
                } else {
                    btnViewPhoto.visibility = android.view.View.GONE
                }

                // ✅ NOVA LÓGICA: Sempre permitir clique, mas com feedback visual diferente
                root.setOnClickListener {
                    if (!isCicloFinalizado) {
                        onExpenseClick(despesa)
                    }
                }

                // Botão de exclusão sempre visível e clicável
                btnDeleteExpense.setOnClickListener {
                    onExpenseDelete(despesa)
                }
                
                // ✅ NOVO: Feedback visual para ciclos finalizados
                if (isCicloFinalizado) {
                    // Ciclo finalizado - botão com aparência desabilitada mas ainda clicável
                    btnDeleteExpense.alpha = 0.5f
                    btnDeleteExpense.contentDescription = "Exclusão não permitida - Ciclo finalizado"
                    // Manter o ícone de exclusão mas com cor mais suave
                    btnDeleteExpense.setColorFilter(android.graphics.Color.GRAY)
                } else {
                    // Ciclo em andamento - botão normal
                    btnDeleteExpense.alpha = 1.0f
                    btnDeleteExpense.contentDescription = "Excluir despesa"
                    btnDeleteExpense.clearColorFilter()
                }
                
                btnDeleteExpense.visibility = android.view.View.VISIBLE
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