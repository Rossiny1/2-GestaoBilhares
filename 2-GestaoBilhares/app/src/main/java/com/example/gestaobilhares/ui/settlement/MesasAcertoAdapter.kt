package com.example.gestaobilhares.ui.settlement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.databinding.ItemMesaAcertoBinding

class MesasAcertoAdapter : ListAdapter<Mesa, MesasAcertoAdapter.MesaViewHolder>(MesaDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesaViewHolder {
        val binding = ItemMesaAcertoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MesaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MesaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MesaViewHolder(private val binding: ItemMesaAcertoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mesa: Mesa) {
            binding.tvNumeroMesa.text = "Mesa ${mesa.numero}"
            binding.tvTipoMesa.text = mesa.tipoMesa.name
            // Por enquanto, não há tipo de acerto ou valor fixo na entidade Mesa
            // Esconder ambos os layouts até integração futura
            binding.layoutFichas.visibility = View.GONE
            binding.layoutValorFixo.visibility = View.GONE
        }
    }

    class MesaDiffCallback : DiffUtil.ItemCallback<Mesa>() {
        override fun areItemsTheSame(oldItem: Mesa, newItem: Mesa) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Mesa, newItem: Mesa) = oldItem == newItem
    }
} 