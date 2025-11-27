package com.example.gestaobilhares.ui.inventory.vehicles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.ui.databinding.ItemFuelHistoryBinding
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Adapter para exibir histórico de abastecimento de veículos
 */
class FuelHistoryAdapter(
    private val onFuelClick: ((FuelRecord) -> Unit)? = null
) : ListAdapter<FuelRecord, FuelHistoryAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
    private var vehicleInitialMileage: Double = 0.0

    fun updateVehicleInitialMileage(mileage: Double) {
        vehicleInitialMileage = mileage
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFuelHistoryBinding.inflate(
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
        private val binding: ItemFuelHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(fuel: FuelRecord) {
            // Data
            binding.tvFuelDate.text = fuel.date.format(dateFormatter)

            // Posto
            binding.tvFuelGasStation.text = fuel.gasStation

            // Litros
            binding.tvFuelLiters.text = "${String.format(Locale("pt", "BR"), "%.1f", fuel.liters)} L"

            // Valor
            binding.tvFuelValue.text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", fuel.value)}"

            // KM
            binding.tvFuelKm.text = "${String.format(Locale("pt", "BR"), "%.0f", fuel.km)} km"

            // Calcular km/l usando km rodado desde o abastecimento anterior
            val kmPerLiter = if (fuel.kmRodado > 0 && fuel.liters > 0) {
                fuel.kmRodado / fuel.liters
            } else {
                0.0
            }
            binding.tvFuelKmPerLiter.text = if (kmPerLiter > 0) {
                "${String.format(Locale("pt", "BR"), "%.1f", kmPerLiter)} km/l"
            } else {
                "N/A"
            }

            // Preço por litro
            val pricePerLiter = if (fuel.liters > 0) {
                fuel.value / fuel.liters
            } else {
                0.0
            }
            binding.tvFuelPricePerLiter.text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", pricePerLiter)}/L"

            // Click listener
            binding.root.setOnClickListener {
                onFuelClick?.invoke(fuel)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FuelRecord>() {
        override fun areItemsTheSame(oldItem: FuelRecord, newItem: FuelRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FuelRecord, newItem: FuelRecord): Boolean {
            return oldItem == newItem
        }
    }
}

