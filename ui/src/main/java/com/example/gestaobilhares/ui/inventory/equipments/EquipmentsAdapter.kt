package com.example.gestaobilhares.ui.inventory.equipments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.ui.databinding.ItemEquipmentBinding

class EquipmentsAdapter(
    private val onEquipmentClick: (Equipment) -> Unit,
    private val onEditClick: (Equipment) -> Unit
) : ListAdapter<Equipment, EquipmentsAdapter.EquipmentViewHolder>(EquipmentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipmentViewHolder {
        val binding = ItemEquipmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EquipmentViewHolder(binding, onEquipmentClick, onEditClick)
    }

    override fun onBindViewHolder(holder: EquipmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EquipmentViewHolder(
        private val binding: ItemEquipmentBinding,
        private val onEquipmentClick: (Equipment) -> Unit,
        private val onEditClick: (Equipment) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(equipment: Equipment) {
            binding.apply {
                tvEquipmentName.text = equipment.name
                tvEquipmentDescription.text = equipment.description.ifEmpty { "Sem descrição" }
                tvEquipmentQuantity.text = "Qtd: ${equipment.quantity}"
                tvEquipmentLocation.text = equipment.location.ifEmpty { "Sem localização" }
                
                root.setOnClickListener {
                    onEquipmentClick(equipment)
                }
                
                btnEditEquipment.setOnClickListener {
                    onEditClick(equipment)
                }
            }
        }
    }

    class EquipmentDiffCallback : DiffUtil.ItemCallback<Equipment>() {
        override fun areItemsTheSame(oldItem: Equipment, newItem: Equipment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Equipment, newItem: Equipment): Boolean {
            return oldItem == newItem
        }
    }
}
