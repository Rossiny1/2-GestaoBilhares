package com.example.gestaobilhares.ui.expenses
import com.example.gestaobilhares.ui.R

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.DespesaResumo
import com.example.gestaobilhares.data.entities.CategoriaDespesaEnum
import com.example.gestaobilhares.ui.databinding.ItemExpenseBinding
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Adapter para exibir lista de despesas no RecyclerView.
 * Utiliza DiffUtil para otimizar atualizações da lista.
 */
class ExpenseAdapter(
    private val onExpenseClick: (DespesaResumo) -> Unit
) : ListAdapter<DespesaResumo, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    // Formatador de moeda brasileiro
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    
    // Formatador de data
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder para itens de despesa.
     */
    inner class ExpenseViewHolder(
        private val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Configura clique no item
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onExpenseClick(getItem(position))
                }
            }
        }

        /**
         * Vincula dados da despesa à view.
         */
        fun bind(despesaResumo: DespesaResumo) {
            val despesa = despesaResumo.despesa
            
            with(binding) {
                // Descrição da despesa
                expenseDescription.text = despesa.descricao
                
                // Categoria e rota
                categoryAndRoute.text = "${despesa.categoria} • ${despesaResumo.nomeRota}"
                
                // Valor formatado
                expenseValue.text = currencyFormatter.format(despesa.valor)
                
                // Data relativa (ex: "Há 2 dias")
                expenseDateTime.text = formatRelativeTime(despesa.dataHora)
                
                // Ícone da categoria
                categoryIcon.setImageResource(getCategoryIcon(despesa.categoria))
                
                // Observações (mostrar apenas se não estiver vazia)
                if (despesa.observacoes.isNotBlank()) {
                    expenseObservations.text = despesa.observacoes
                    expenseObservations.visibility = View.VISIBLE
                } else {
                    expenseObservations.visibility = View.GONE
                }
                
                // Criado por (mostrar apenas se não estiver vazio)
                if (despesa.criadoPor.isNotBlank()) {
                    createdBy.text = "Por: ${despesa.criadoPor}"
                    createdBy.visibility = View.VISIBLE
                } else {
                    createdBy.visibility = View.GONE
                }
            }
        }

        /**
         * Formata o tempo relativo (ex: "Há 2 dias").
         */
        private fun formatRelativeTime(dateTime: LocalDateTime): String {
            val now = LocalDateTime.now()
            val days = ChronoUnit.DAYS.between(dateTime, now)
            val hours = ChronoUnit.HOURS.between(dateTime, now)
            val minutes = ChronoUnit.MINUTES.between(dateTime, now)

            return when {
                days > 0 -> if (days == 1L) "Há 1 dia" else "Há $days dias"
                hours > 0 -> if (hours == 1L) "Há 1 hora" else "Há $hours horas"
                minutes > 0 -> if (minutes == 1L) "Há 1 minuto" else "Há $minutes minutos"
                else -> "Agora"
            }
        }

        /**
         * Retorna o ícone apropriado para cada categoria.
         */
        private fun getCategoryIcon(categoria: String): Int {
            return when (categoria) {
                CategoriaDespesaEnum.COMBUSTIVEL.displayName -> com.example.gestaobilhares.ui.R.drawable.ic_local_gas_station
                CategoriaDespesaEnum.ALIMENTACAO.displayName -> com.example.gestaobilhares.ui.R.drawable.ic_restaurant
                CategoriaDespesaEnum.TRANSPORTE.displayName -> com.example.gestaobilhares.ui.R.drawable.ic_directions_bus
                CategoriaDespesaEnum.MANUTENCAO.displayName -> com.example.gestaobilhares.ui.R.drawable.ic_build
                CategoriaDespesaEnum.MATERIAIS.displayName -> com.example.gestaobilhares.ui.R.drawable.ic_inventory
                else -> com.example.gestaobilhares.ui.R.drawable.ic_receipt
            }
        }
    }

    /**
     * DiffCallback para comparar itens de despesas.
     * Otimiza as atualizações da lista.
     */
    class ExpenseDiffCallback : DiffUtil.ItemCallback<DespesaResumo>() {
        
        override fun areItemsTheSame(oldItem: DespesaResumo, newItem: DespesaResumo): Boolean {
            return oldItem.despesa.id == newItem.despesa.id
        }

        override fun areContentsTheSame(oldItem: DespesaResumo, newItem: DespesaResumo): Boolean {
            return oldItem == newItem
        }
    }
} 

