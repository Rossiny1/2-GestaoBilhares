package com.example.gestaobilhares.ui.inventory.others

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.databinding.ItemOtherInventoryBinding

class OthersInventoryAdapter(
    private val onOtherItemClick: (OtherItem) -> Unit
) : ListAdapter<OtherItem, OthersInventoryAdapter.OtherItemViewHolder>(OtherItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OtherItemViewHolder {
        val binding = ItemOtherInventoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OtherItemViewHolder(binding, onOtherItemClick)
    }

    override fun onBindViewHolder(holder: OtherItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OtherItemViewHolder(
        private val binding: ItemOtherInventoryBinding,
        private val onOtherItemClick: (OtherItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(otherItem: OtherItem) {
            binding.apply {
                tvItemName.text = otherItem.name
                tvItemDescription.text = otherItem.description
                tvItemQuantity.text = "Qtd: ${otherItem.quantity}"
                tvItemLocation.text = otherItem.location
                
                root.setOnClickListener {
                    onOtherItemClick(otherItem)
                }
            }
        }
    }

    class OtherItemDiffCallback : DiffUtil.ItemCallback<OtherItem>() {
        override fun areItemsTheSame(oldItem: OtherItem, newItem: OtherItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OtherItem, newItem: OtherItem): Boolean {
            return oldItem == newItem
        }
    }
}
