package com.example.gestaobilhares.ui.expenses.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.Despesa
import com.example.gestaobilhares.databinding.ItemGlobalExpenseBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter para listagem de despesas globais
 * Exibe despesas com informações de ciclo, categoria e tipo
 */
class GlobalExpensesAdapter(
    private val onItemClick: (Despesa) -> Unit = {},
    private val onItemLongClick: (Despesa) -> Unit = {}
) : ListAdapter<Despesa, GlobalExpensesAdapter.GlobalExpenseViewHolder>(GlobalExpenseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GlobalExpenseViewHolder {
        val binding = ItemGlobalExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GlobalExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GlobalExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GlobalExpenseViewHolder(
        private val binding: ItemGlobalExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

        fun bind(expense: Despesa) {
            // Valor da despesa
            binding.tvExpenseValue.text = currencyFormat.format(expense.valor)

            // Data da despesa
            try {
                binding.tvExpenseDate.text = expense.dataHora.toString()
            } catch (e: Exception) {
                binding.tvExpenseDate.text = "Data inválida"
            }

            // Informações do ciclo
            val cycleInfo = buildString {
                if (expense.cicloNumero != null && expense.cicloAno != null) {
                    append("${expense.cicloNumero}º Acerto - ${expense.cicloAno}")
                } else {
                    append("Sem ciclo definido")
                }
            }
            binding.tvCycleInfo.text = cycleInfo

            // Descrição
            binding.tvExpenseDescription.text = expense.descricao

            // Categoria
            binding.tvExpenseCategory.text = expense.categoria

            // Tipo de despesa
            binding.tvExpenseType.text = expense.tipoDespesa

            // Observações (se houver)
            if (expense.observacoes.isNotEmpty()) {
                binding.tvExpenseObservations.text = expense.observacoes
                binding.tvExpenseObservations.visibility = View.VISIBLE
            } else {
                binding.tvExpenseObservations.visibility = View.GONE
            }

            // Criado por
            binding.tvCreatedBy.text = expense.criadoPor

            // Click listeners
            binding.root.setOnClickListener {
                onItemClick(expense)
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(expense)
                true
            }
        }
    }

    private class GlobalExpenseDiffCallback : DiffUtil.ItemCallback<Despesa>() {
        override fun areItemsTheSame(oldItem: Despesa, newItem: Despesa): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Despesa, newItem: Despesa): Boolean {
            return oldItem == newItem
        }
    }
}
