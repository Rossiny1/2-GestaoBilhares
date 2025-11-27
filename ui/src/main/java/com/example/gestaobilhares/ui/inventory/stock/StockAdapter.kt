package com.example.gestaobilhares.ui.inventory.stock

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.ui.databinding.ItemStockBinding
import java.text.NumberFormat
import java.util.*

class StockAdapter(
    private val onStockItemClick: (StockItem) -> Unit
) : ListAdapter<StockItem, StockAdapter.StockItemViewHolder>(StockItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockItemViewHolder {
        val binding = ItemStockBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StockItemViewHolder(binding, onStockItemClick)
    }

    override fun onBindViewHolder(holder: StockItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class StockItemViewHolder(
        private val binding: ItemStockBinding,
        private val onStockItemClick: (StockItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stockItem: StockItem) {
            binding.apply {
                tvItemName.text = stockItem.name
                tvItemCategory.text = stockItem.category
                tvItemQuantity.text = "Qtd: ${stockItem.quantity}"
                tvItemPrice.text = "R$ ${NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(stockItem.unitPrice)}"
                tvItemSupplier.text = stockItem.supplier
                
                root.setOnClickListener {
                    onStockItemClick(stockItem)
                }
            }
        }
    }

    class StockItemDiffCallback : DiffUtil.ItemCallback<StockItem>() {
        override fun areItemsTheSame(oldItem: StockItem, newItem: StockItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StockItem, newItem: StockItem): Boolean {
            return oldItem == newItem
        }
    }
}
