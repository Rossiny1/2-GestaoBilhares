package com.example.gestaobilhares.ui.expenses.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.databinding.ItemCycleSelectionBinding

/**
 * Adapter para seleção de ciclo de acerto
 * Usado no dialog de seleção de ciclo
 */
class CycleSelectionAdapter(
    private val onCycleClick: (CicloAcertoEntity) -> Unit
) : ListAdapter<CicloAcertoEntity, CycleSelectionAdapter.CycleSelectionViewHolder>(CycleSelectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CycleSelectionViewHolder {
        val binding = ItemCycleSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CycleSelectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CycleSelectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CycleSelectionViewHolder(
        private val binding: ItemCycleSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cycle: CicloAcertoEntity) {
            // Número do ciclo
            binding.tvCycleNumber.text = "${cycle.numeroCiclo}º Acerto"

            // Ano do ciclo
            binding.tvCycleYear.text = cycle.ano.toString()

            // Status do ciclo
            val statusText = "Finalizado" // Ciclos finalizados
            val statusColor = R.color.success_color
            binding.tvCycleStatus.text = statusText
            binding.tvCycleStatus.setTextColor(
                binding.root.context.getColor(statusColor)
            )

            // Descrição do ciclo
            binding.tvCycleDescription.text = "Ciclo de acerto para o período"

            // Click listener
            binding.root.setOnClickListener {
                onCycleClick(cycle)
            }
        }
    }

    private class CycleSelectionDiffCallback : DiffUtil.ItemCallback<CicloAcertoEntity>() {
        override fun areItemsTheSame(oldItem: CicloAcertoEntity, newItem: CicloAcertoEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CicloAcertoEntity, newItem: CicloAcertoEntity): Boolean {
            return oldItem == newItem
        }
    }
}
