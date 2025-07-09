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
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter para lista de ciclos de acerto no histórico
 * ✅ FASE 9C: ADAPTER PARA HISTÓRICO DE CICLOS
 */
class CycleHistoryAdapter(
    private val onCicloClick: (CicloAcertoEntity) -> Unit
) : ListAdapter<CicloAcertoEntity, CycleHistoryAdapter.CycleViewHolder>(CycleDiffCallback()) {

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
        private val onCicloClick: (CicloAcertoEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        private val currencyFormatter = java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        fun bind(ciclo: CicloAcertoEntity) {
            binding.apply {
                // Título do ciclo
                tvCycleTitle.text = ciclo.titulo
                
                // Datas
                tvCycleDate.text = "${dateFormatter.format(ciclo.dataInicio)} - ${dateFormatter.format(ciclo.dataFim)}"
                
                // Valores financeiros
                tvRevenue.text = currencyFormatter.format(ciclo.valorTotalAcertado)
                tvExpenses.text = currencyFormatter.format(ciclo.valorTotalDespesas)
                tvProfit.text = currencyFormatter.format(ciclo.lucroLiquido)
                
                // Status
                tvStatus.text = when (ciclo.status) {
                    StatusCicloAcerto.EM_ANDAMENTO -> "Em Andamento"
                    StatusCicloAcerto.FINALIZADO -> "Finalizado"
                    StatusCicloAcerto.CANCELADO -> "Cancelado"
                }
                
                // Cor do status
                val statusColor = when (ciclo.status) {
                    StatusCicloAcerto.EM_ANDAMENTO -> R.color.orange_600
                    StatusCicloAcerto.FINALIZADO -> R.color.green_600
                    StatusCicloAcerto.CANCELADO -> R.color.red_600
                }
                tvStatus.setTextColor(root.context.getColor(statusColor))
                
                // Cor do lucro
                val profitColor = if (ciclo.lucroLiquido >= 0) {
                    R.color.green_600
                } else {
                    R.color.red_600
                }
                tvProfit.setTextColor(root.context.getColor(profitColor))
                
                // Progresso
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

    private class CycleDiffCallback : DiffUtil.ItemCallback<CicloAcertoEntity>() {
        override fun areItemsTheSame(oldItem: CicloAcertoEntity, newItem: CicloAcertoEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CicloAcertoEntity, newItem: CicloAcertoEntity): Boolean {
            return oldItem == newItem
        }
    }
} 