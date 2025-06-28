package com.example.gestaobilhares.ui.mesas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.databinding.ItemMesaClienteBinding

class MesasDepositoAdapter(
    private val onMesaClick: (Mesa) -> Unit
) : ListAdapter<Mesa, MesasDepositoAdapter.MesaViewHolder>(MesaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesaViewHolder {
        val binding = ItemMesaClienteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MesaViewHolder(binding, onMesaClick)
    }

    override fun onBindViewHolder(holder: MesaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MesaViewHolder(
        private val binding: ItemMesaClienteBinding,
        private val onMesaClick: (Mesa) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mesa: Mesa) {
            binding.tvNumeroMesa.text = "Mesa ${mesa.numero}"
            binding.btnRetirarMesa.visibility = ViewGroup.GONE // Não exibe botão de retirar no depósito
            binding.root.setOnClickListener { onMesaClick(mesa) }
            // Exibir estado de conservação, tamanho, etc, se desejar
        }
    }

    class MesaDiffCallback : DiffUtil.ItemCallback<Mesa>() {
        override fun areItemsTheSame(oldItem: Mesa, newItem: Mesa): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Mesa, newItem: Mesa): Boolean = oldItem == newItem
    }
} 