package com.example.gestaobilhares.ui.inventory.vehicles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.Veiculo
import com.example.gestaobilhares.ui.databinding.ItemVehicleBinding

class VehiclesAdapter(
    private val onClick: (Veiculo) -> Unit
) : ListAdapter<Veiculo, VehiclesAdapter.VehicleViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val binding = ItemVehicleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VehicleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VehicleViewHolder(private val binding: ItemVehicleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Veiculo) {
            binding.tvTitle.text = item.nome.ifEmpty { "${item.marca} ${item.modelo}" }
            binding.tvSubtitle.text = "Placa: ${item.placa} | Ano: ${item.anoModelo}"
            binding.tvKm.text = "KM: ${item.kmAtual}"
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Veiculo>() {
            override fun areItemsTheSame(oldItem: Veiculo, newItem: Veiculo): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Veiculo, newItem: Veiculo): Boolean = oldItem == newItem
        }
    }
}

