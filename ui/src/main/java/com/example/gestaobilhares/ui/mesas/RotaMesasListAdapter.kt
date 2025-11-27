package com.example.gestaobilhares.ui.mesas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.ui.databinding.ItemMesaClienteBinding

class RotaMesasListAdapter(
    private var mesas: List<Mesa>,
    private val onMesaClick: (Mesa) -> Unit
) : RecyclerView.Adapter<RotaMesasListAdapter.MesaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesaViewHolder {
        val binding = ItemMesaClienteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MesaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MesaViewHolder, position: Int) {
        holder.bind(mesas[position])
    }

    override fun getItemCount(): Int = mesas.size

    fun updateData(newMesas: List<Mesa>) {
        mesas = newMesas
        notifyDataSetChanged()
    }

    inner class MesaViewHolder(
        private val binding: ItemMesaClienteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mesa: Mesa) {
            binding.tvNumeroMesa.text = mesa.numero

            // Ocultar o botão de retirar mesa (não é necessário na tela de gerenciamento)
            binding.btnRetirarMesa.visibility = View.GONE

            binding.root.setOnClickListener {
                onMesaClick(mesa)
            }
        }
    }
}
