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
    private val onFuelClick: (FuelRecord) -> Unit,
    private var vehicleInitialMileage: Double = 0.0
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

    fun updateVehicleInitialMileage(newMileage: Double) {
        this.vehicleInitialMileage = newMileage
        notifyDataSetChanged()
    }

    inner class FuelViewHolder(
        private val binding: ItemFuelHistoryBinding,
        private val onFuelClick: (FuelRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(fuel: FuelRecord) {
            binding.apply {
                tvFuelDate.text = fuel.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                tvFuelGasStation.text = fuel.gasStation
                tvFuelLiters.text = "${String.format("%.1f", fuel.liters)} L"
                tvFuelValue.text = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(fuel.value)
                tvFuelKm.text = "${String.format("%.0f", fuel.km)} km"
                
                // ✅ CORREÇÃO: Calcular média km/l correta para este abastecimento
                val kmPerLiter = calculateCorrectKmPerLiter(fuel)
                tvFuelKmPerLiter.text = "${String.format("%.1f", kmPerLiter)} km/l"
                
                // Calcular preço por litro
                val pricePerLiter = if (fuel.liters > 0) fuel.value / fuel.liters else 0.0
                tvFuelPricePerLiter.text = "R$ ${String.format("%.2f", pricePerLiter)}/L"
                
                root.setOnClickListener {
                    onFuelClick(fuel)
                }
            }
        }
        
        /**
         * Calcula o km/l correto para este abastecimento.
         * Primeiro: (kmAtual - kmInicial) / litros
         * Demais: (kmAtual - kmAnterior) / litros
         */
        private fun calculateCorrectKmPerLiter(fuel: FuelRecord): Double {
            if (fuel.liters <= 0) return 0.0

            // Lista atual do adapter, ordenada por hodômetro (km) crescente
            val sortedFuels = currentList.sortedBy { it.km }
            val currentIndex = sortedFuels.indexOfFirst { it.id == fuel.id }
            if (currentIndex == -1) return 0.0

            return if (currentIndex == 0) {
                // Primeiro abastecimento: subtrair km inicial do veículo
                val kmRealRodado = fuel.km - this@FuelHistoryAdapter.vehicleInitialMileage
                if (kmRealRodado > 0) kmRealRodado / fuel.liters else 0.0
            } else {
                // Demais: diferença para o abastecimento anterior
                val abastecimentoAnterior = sortedFuels[currentIndex - 1]
                val kmRealRodado = fuel.km - abastecimentoAnterior.km
                if (kmRealRodado > 0) kmRealRodado / fuel.liters else 0.0
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
