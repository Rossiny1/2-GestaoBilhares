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
import android.util.Log

class MesasAcertoAdapter : ListAdapter<Mesa, MesasAcertoAdapter.MesaViewHolder>(MesaDiffCallback()) {
    override fun submitList(list: List<Mesa>?) {
        Log.d("MesasAcertoAdapter", "submitList: ${list?.map { it.numero }}")
        super.submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesaViewHolder {
        val binding = ItemMesaAcertoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MesaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MesaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MesaViewHolder(private val binding: ItemMesaAcertoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mesa: Mesa) {
            Log.d("MesasAcertoAdapter", "bind: mesa.numero=${mesa.numero}, mesa.id=${mesa.id}")
            binding.tvNumeroMesa.text = "Mesa ${mesa.numero}"
            binding.tvTipoMesa.text = mesa.tipoMesa.name
            // Exibir layout de fichas
            binding.layoutFichas.visibility = View.VISIBLE
            binding.etRelogioInicial.setText(mesa.fichasInicial.toString())
            // O campo final e o checkbox continuam editáveis pelo usuário
        }
    }

    class MesaDiffCallback : DiffUtil.ItemCallback<Mesa>() {
        override fun areItemsTheSame(oldItem: Mesa, newItem: Mesa) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Mesa, newItem: Mesa) = oldItem == newItem
    }
} 