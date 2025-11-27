package com.example.gestaobilhares.ui.clients

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.getDisplayName
import com.example.gestaobilhares.ui.databinding.ItemMesaClienteBinding

/**
 * Adapter para exibir as mesas vinculadas a um cliente
 * Exibe número da mesa e botão para retirar/desvincular
 */
class MesasAdapter(
    private val onRetirarMesa: (Mesa) -> Unit
) : ListAdapter<Mesa, MesasAdapter.MesaViewHolder>(MesaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesaViewHolder {
        val binding = ItemMesaClienteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MesaViewHolder(binding, onRetirarMesa)
    }

    override fun onBindViewHolder(holder: MesaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MesaViewHolder(
        private val binding: ItemMesaClienteBinding,
        private val onRetirarMesa: (Mesa) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mesa: Mesa) {
            // ✅ NOVO: Usar o tipo da mesa como título do card
            binding.tvMesaLabel.text = mesa.tipoMesa.getDisplayName()
            binding.tvNumeroMesa.text = mesa.numero
            
            if (mesa.clienteId != null) {
                binding.btnRetirarMesa.visibility = View.VISIBLE
                binding.btnRetirarMesa.setOnClickListener { onRetirarMesa(mesa) }
            } else {
                binding.btnRetirarMesa.visibility = View.GONE
            }
        }
    }

    class MesaDiffCallback : DiffUtil.ItemCallback<Mesa>() {
        override fun areItemsTheSame(oldItem: Mesa, newItem: Mesa): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Mesa, newItem: Mesa): Boolean = oldItem == newItem
    }
} 
