package com.example.gestaobilhares.ui.inventory.vehicles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.databinding.ItemFuelHistoryBinding
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

class FuelHistoryAdapter(
    private val onFuelClick: (FuelRecord) -> Unit
) : ListAdapter<FuelRecord, FuelHistoryAdapter.FuelViewHolder>(FuelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FuelViewHolder {
        val binding = ItemFuelHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FuelViewHolder(binding, onFuelClick)
    }

    override fun onBindViewHolder(holder: FuelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FuelViewHolder(
        private val binding: ItemFuelHistoryBinding,
        private val onFuelClick: (FuelRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(fuel: FuelRecord) {
            binding.apply {
                tvFuelDate.text = fuel.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                tvFuelGasStation.text = fuel.gasStation
                tvFuelLiters.text = "${String.format("%.1f", fuel.liters)} L"
                tvFuelValue.text = "R$ ${NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(fuel.value)}"
                tvFuelKm.text = "${String.format("%.0f", fuel.km)} km"
                
                // Calcular média km/l para este abastecimento
                val kmPerLiter = if (fuel.liters > 0) fuel.km / fuel.liters else 0.0
                tvFuelKmPerLiter.text = "${String.format("%.1f", kmPerLiter)} km/l"
                
                // Calcular preço por litro
                val pricePerLiter = if (fuel.liters > 0) fuel.value / fuel.liters else 0.0
                tvFuelPricePerLiter.text = "R$ ${String.format("%.2f", pricePerLiter)}/L"
                
                root.setOnClickListener {
                    onFuelClick(fuel)
                }
            }
        }
    }

    class FuelDiffCallback : DiffUtil.ItemCallback<FuelRecord>() {
        override fun areItemsTheSame(oldItem: FuelRecord, newItem: FuelRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FuelRecord, newItem: FuelRecord): Boolean {
            return oldItem == newItem
        }
    }
}
