package com.example.gestaobilhares.ui.inventory.vehicles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.databinding.ItemMaintenanceHistoryBinding
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Adapter para exibir histórico de manutenção de veículos
 */
class MaintenanceHistoryAdapter(
    private val onMaintenanceClick: ((MaintenanceRecord) -> Unit)? = null
) : ListAdapter<MaintenanceRecord, MaintenanceHistoryAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMaintenanceHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMaintenanceHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(maintenance: MaintenanceRecord) {
            // Data
            binding.tvMaintenanceDate.text = maintenance.date.format(dateFormatter)

            // Tipo
            binding.tvMaintenanceType.text = maintenance.type

            // Descrição
            binding.tvMaintenanceDescription.text = maintenance.description

            // Valor
            binding.tvMaintenanceValue.text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", maintenance.value)}"

            // Quilometragem
            binding.tvMaintenanceMileage.text = "${String.format(Locale("pt", "BR"), "%.0f", maintenance.mileage)} km"

            // Click listener
            binding.root.setOnClickListener {
                onMaintenanceClick?.invoke(maintenance)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MaintenanceRecord>() {
        override fun areItemsTheSame(oldItem: MaintenanceRecord, newItem: MaintenanceRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MaintenanceRecord, newItem: MaintenanceRecord): Boolean {
            return oldItem == newItem
        }
    }
}

