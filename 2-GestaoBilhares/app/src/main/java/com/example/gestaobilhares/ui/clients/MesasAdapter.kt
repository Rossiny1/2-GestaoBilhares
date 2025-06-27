package com.example.gestaobilhares.ui.clients

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.databinding.ItemMesaClienteBinding

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

    class MesaViewHolder(
        private val binding: ItemMesaClienteBinding,
        private val onRetirarMesa: (Mesa) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mesa: Mesa) {
            binding.tvNumeroMesa.text = "Mesa ${mesa.numero}"
            binding.tvTipoMesa.text = mesa.tipoMesa.name
            binding.btnRetirarMesa.setOnClickListener {
                onRetirarMesa(mesa)
            }
        }
    }

    class MesaDiffCallback : DiffUtil.ItemCallback<Mesa>() {
        override fun areItemsTheSame(oldItem: Mesa, newItem: Mesa): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Mesa, newItem: Mesa): Boolean = oldItem == newItem
    }
} 