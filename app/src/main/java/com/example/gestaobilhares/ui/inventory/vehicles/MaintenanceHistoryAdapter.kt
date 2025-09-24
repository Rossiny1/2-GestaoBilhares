package com.example.gestaobilhares.ui.inventory.vehicles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.databinding.ItemMaintenanceHistoryBinding
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

class MaintenanceHistoryAdapter(
    private val onMaintenanceClick: (MaintenanceRecord) -> Unit
) : ListAdapter<MaintenanceRecord, MaintenanceHistoryAdapter.MaintenanceViewHolder>(MaintenanceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaintenanceViewHolder {
        val binding = ItemMaintenanceHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MaintenanceViewHolder(binding, onMaintenanceClick)
    }

    override fun onBindViewHolder(holder: MaintenanceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MaintenanceViewHolder(
        private val binding: ItemMaintenanceHistoryBinding,
        private val onMaintenanceClick: (MaintenanceRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(maintenance: MaintenanceRecord) {
            binding.apply {
                tvMaintenanceDate.text = maintenance.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                tvMaintenanceDescription.text = maintenance.description
                tvMaintenanceValue.text = "R$ ${NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(maintenance.value)}"
                tvMaintenanceMileage.text = "${String.format("%.0f", maintenance.mileage)} km"
                tvMaintenanceType.text = maintenance.type
                
                // Configurar cor do tipo
                when (maintenance.type.lowercase()) {
                    "preventiva" -> {
                        tvMaintenanceType.setBackgroundResource(android.R.color.holo_green_light)
                        tvMaintenanceType.setTextColor(android.graphics.Color.WHITE)
                    }
                    "corretiva" -> {
                        tvMaintenanceType.setBackgroundResource(android.R.color.holo_red_light)
                        tvMaintenanceType.setTextColor(android.graphics.Color.WHITE)
                    }
                    "emergencial" -> {
                        tvMaintenanceType.setBackgroundResource(android.R.color.holo_orange_light)
                        tvMaintenanceType.setTextColor(android.graphics.Color.WHITE)
                    }
                }
                
                root.setOnClickListener {
                    onMaintenanceClick(maintenance)
                }
            }
        }
    }

    class MaintenanceDiffCallback : DiffUtil.ItemCallback<MaintenanceRecord>() {
        override fun areItemsTheSame(oldItem: MaintenanceRecord, newItem: MaintenanceRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MaintenanceRecord, newItem: MaintenanceRecord): Boolean {
            return oldItem == newItem
        }
    }
}
