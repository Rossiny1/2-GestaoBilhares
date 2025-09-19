package com.example.gestaobilhares.ui.clients.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.example.gestaobilhares.databinding.ItemCycleHistoryBinding
import com.example.gestaobilhares.ui.clients.CycleHistoryItem
import java.text.SimpleDateFormat
import java.util.Locale
import android.view.View

/**
 * Adapter para lista de ciclos de acerto no histórico
 * ✅ FASE 9C: ADAPTER PARA HISTÓRICO DE CICLOS
 */
class CycleHistoryAdapter(
    private val onCicloClick: (CycleHistoryItem) -> Unit
) : ListAdapter<CycleHistoryItem, CycleHistoryAdapter.CycleViewHolder>(CycleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CycleViewHolder {
        val binding = ItemCycleHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CycleViewHolder(binding, onCicloClick)
    }

    override fun onBindViewHolder(holder: CycleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CycleViewHolder(
        private val binding: ItemCycleHistoryBinding,
        private val onCicloClick: (CycleHistoryItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        private val currencyFormatter = java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        fun bind(ciclo: CycleHistoryItem) {
            binding.apply {
                // Título do ciclo
                tvCycleTitle.text = ciclo.titulo
                
                // Datas
                tvCycleDate.text = "${dateFormatter.format(ciclo.dataInicio)} - ${dateFormatter.format(ciclo.dataFim)}"
                
                // Valores financeiros
                tvRevenue.text = currencyFormatter.format(ciclo.valorTotalAcertado)
                tvExpenses.text = currencyFormatter.format(ciclo.valorTotalDespesas)
                tvDiscounts.text = currencyFormatter.format(ciclo.totalDescontos)
                tvProfit.text = currencyFormatter.format(ciclo.lucroLiquido)
                
                // Débito total
                tvDebitTotal.text = currencyFormatter.format(ciclo.debitoTotal)
                
                // Status
                when (ciclo.status) {
                    com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO -> {
                        tvStatus.text = "Em Andamento"
                        tvStatus.setTextColor(root.context.getColor(R.color.white))
                        tvStatus.background.setTint(root.context.getColor(R.color.orange_600))
                        tvStatus.visibility = View.VISIBLE
                    }
                    com.example.gestaobilhares.data.entities.StatusCicloAcerto.FINALIZADO -> {
                        tvStatus.text = "Finalizado"
                        tvStatus.setTextColor(root.context.getColor(R.color.white))
                        tvStatus.background.setTint(root.context.getColor(R.color.green_600))
                        tvStatus.visibility = View.VISIBLE
                    }
                    com.example.gestaobilhares.data.entities.StatusCicloAcerto.CANCELADO -> {
                        tvStatus.text = "Cancelado"
                        tvStatus.setTextColor(root.context.getColor(R.color.white))
                        tvStatus.background.setTint(root.context.getColor(R.color.red_600))
                        tvStatus.visibility = View.VISIBLE
                    }
                }
                // Cor do lucro
                val profitColor = if (ciclo.lucroLiquido >= 0) {
                    R.color.green_600
                } else {
                    R.color.red_600
                }
                tvProfit.setTextColor(root.context.getColor(profitColor))
                // Progresso real
                tvProgress.text = "${ciclo.clientesAcertados}/${ciclo.totalClientes} clientes"
                val progressPercent = if (ciclo.totalClientes > 0) {
                    (ciclo.clientesAcertados * 100) / ciclo.totalClientes
                } else {
                    0
                }
                tvProgressPercent.text = "$progressPercent%"
                // Click listener
                root.setOnClickListener {
                    onCicloClick(ciclo)
                }
            }
        }
    }

    private class CycleDiffCallback : DiffUtil.ItemCallback<CycleHistoryItem>() {
        override fun areItemsTheSame(oldItem: CycleHistoryItem, newItem: CycleHistoryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CycleHistoryItem, newItem: CycleHistoryItem): Boolean {
            return oldItem == newItem
        }
    }
} 