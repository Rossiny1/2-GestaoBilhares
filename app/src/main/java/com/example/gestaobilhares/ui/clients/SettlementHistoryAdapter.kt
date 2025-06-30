package com.example.gestaobilhares.ui.clients

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.ItemSettlementHistoryBinding
// Using data class from ClientDetailViewModel
import java.text.NumberFormat
import java.util.*

/**
 * Adapter para histórico de acertos do cliente
 * FASE 4A - Implementação crítica
 */
class SettlementHistoryAdapter(
    private val onItemClick: (AcertoResumo) -> Unit
) : ListAdapter<AcertoResumo, SettlementHistoryAdapter.SettlementViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettlementViewHolder {
        val binding = ItemSettlementHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SettlementViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: SettlementViewHolder, position: Int) {
        val item = getItem(position)
        android.util.Log.d("SettlementHistoryAdapter", "Binding item $position: ID=${item.id}, Data=${item.data}, Valor=${item.valor}")
        holder.bind(item)
    }

    class SettlementViewHolder(
        private val binding: ItemSettlementHistoryBinding,
        private val onItemClick: (AcertoResumo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(settlement: AcertoResumo) {
            binding.apply {
                // Data do acerto
                tvSettlementDate.text = settlement.data
                
                // Valor formatado em Real
                val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
                tvSettlementValue.text = formatter.format(settlement.valor)
                
                // Status com cores
                tvSettlementStatus.text = settlement.status.uppercase()
                val statusColor = when (settlement.status.lowercase()) {
                    "finalizado", "pago" -> R.color.green_600
                    "pendente" -> R.color.orange_600
                    "atrasado" -> R.color.red_600
                    else -> R.color.gray_600
                }
                tvSettlementStatus.setTextColor(ContextCompat.getColor(binding.root.context, statusColor))
                
                // Mesas acertadas
                tvTablesCount.text = "${settlement.mesasAcertadas} mesa${if (settlement.mesasAcertadas != 1) "s" else ""}"
                
                // ID do acerto
                tvSettlementId.text = "#${settlement.id.toString().padStart(4, '0')}"
                
                // Click listener
                root.setOnClickListener {
                    onItemClick(settlement)
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<AcertoResumo>() {
        override fun areItemsTheSame(oldItem: AcertoResumo, newItem: AcertoResumo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AcertoResumo, newItem: AcertoResumo): Boolean {
            return oldItem == newItem
        }
    }
} 
