package com.example.gestaobilhares.ui.cycles.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.ItemCycleClientBinding
import com.example.gestaobilhares.ui.cycles.CycleClientItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter para lista de clientes do ciclo
 */
class CycleClientsAdapter(
    private val onClientClick: (CycleClientItem) -> Unit
) : ListAdapter<CycleClientItem, CycleClientsAdapter.ClientViewHolder>(ClientDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val binding = ItemCycleClientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClientViewHolder(binding, onClientClick)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ClientViewHolder(
        private val binding: ItemCycleClientBinding,
        private val onClientClick: (CycleClientItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        fun bind(cliente: CycleClientItem) {
            binding.apply {
                tvClientName.text = cliente.nome
                tvClientValue.text = currencyFormatter.format(cliente.valorAcertado)
                tvClientDate.text = dateFormatter.format(cliente.dataAcerto)
                
                // Observações (opcional)
                if (cliente.observacoes != null && cliente.observacoes.isNotEmpty()) {
                    tvClientObservations.text = cliente.observacoes
                    tvClientObservations.visibility = android.view.View.VISIBLE
                } else {
                    tvClientObservations.visibility = android.view.View.GONE
                }

                // Click listener
                root.setOnClickListener {
                    onClientClick(cliente)
                }
            }
        }
    }

    private class ClientDiffCallback : DiffUtil.ItemCallback<CycleClientItem>() {
        override fun areItemsTheSame(oldItem: CycleClientItem, newItem: CycleClientItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CycleClientItem, newItem: CycleClientItem): Boolean {
            return oldItem == newItem
        }
    }
} 