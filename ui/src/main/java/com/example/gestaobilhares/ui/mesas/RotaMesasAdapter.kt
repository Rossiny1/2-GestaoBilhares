package com.example.gestaobilhares.ui.mesas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gestaobilhares.ui.databinding.ItemRotaMesasBinding

class RotaMesasAdapter(
    private val onRotaClick: (RotaComMesas) -> Unit
) : RecyclerView.Adapter<RotaMesasAdapter.RotaViewHolder>() {

    private var rotasComMesas = listOf<RotaComMesas>()

    fun updateData(rotas: List<RotaComMesas>) {
        rotasComMesas = rotas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RotaViewHolder {
        val binding = ItemRotaMesasBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RotaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RotaViewHolder, position: Int) {
        holder.bind(rotasComMesas[position])
    }

    override fun getItemCount(): Int = rotasComMesas.size

    inner class RotaViewHolder(
        private val binding: ItemRotaMesasBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(rotaComMesas: RotaComMesas) {
            binding.tvNomeRota.text = rotaComMesas.rota.nome
            binding.tvRotaSinuca.text = "S: ${rotaComMesas.sinuca}"
            binding.tvRotaJukebox.text = "J: ${rotaComMesas.jukebox}"
            binding.tvRotaPembolim.text = "P: ${rotaComMesas.pembolim}"

            binding.root.setOnClickListener {
                onRotaClick(rotaComMesas)
            }
        }
    }
}
